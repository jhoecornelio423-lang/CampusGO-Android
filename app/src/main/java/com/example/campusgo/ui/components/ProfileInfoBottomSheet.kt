package com.example.campusgo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.campusgo.R
import com.example.campusgo.core.util.SupportEmailHelper

enum class ProfileInfoType {
    NONE,
    TERMS,
    PRIVACY,
    HELP
}

enum class SupportEmailFeedbackState {
    NONE,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoBottomSheet(
    type: ProfileInfoType,
    onDismiss: () -> Unit
) {
    if (type == ProfileInfoType.NONE) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    var pendingEmailReturnCheck by remember { mutableStateOf(false) }
    var emailLaunchTimestamp by remember { mutableStateOf(0L) }
    var feedbackState by remember { mutableStateOf(SupportEmailFeedbackState.NONE) }
    var lastSentSuccess by remember { mutableStateOf<Boolean?>(null) }
    var errorMessageDetail by remember { mutableStateOf<String?>(null) }

    val handleEmailReturn: (Int?) -> Unit = { resultCode ->
        if (pendingEmailReturnCheck) {
            pendingEmailReturnCheck = false
            val durationMs = System.currentTimeMillis() - emailLaunchTimestamp
            val wasSent = (resultCode == android.app.Activity.RESULT_OK) || (durationMs >= 2500L)
            if (wasSent) {
                lastSentSuccess = true
                feedbackState = SupportEmailFeedbackState.SUCCESS
                Toast.makeText(
                    context,
                    "Correo enviado correctamente a ${SupportEmailHelper.SUPPORT_EMAIL}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                lastSentSuccess = false
                errorMessageDetail = "El envío no se completó en la aplicación de correo."
                feedbackState = SupportEmailFeedbackState.ERROR
                Toast.makeText(
                    context,
                    "Error al enviar correo electrónico",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val emailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleEmailReturn(result.resultCode)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingEmailReturnCheck) {
                handleEmailReturn(null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val triggerSendEmail: () -> Unit = {
        try {
            val intent = SupportEmailHelper.createSupportEmailIntent()
            pendingEmailReturnCheck = true
            emailLaunchTimestamp = System.currentTimeMillis()
            Toast.makeText(
                context,
                "Abriendo aplicación de correo para ${SupportEmailHelper.SUPPORT_EMAIL}...",
                Toast.LENGTH_SHORT
            ).show()
            emailLauncher.launch(intent)
        } catch (_: Exception) {
            pendingEmailReturnCheck = false
            lastSentSuccess = false
            errorMessageDetail = "No se encontró una aplicación de correo electrónico instalada o configurada en tu dispositivo."
            feedbackState = SupportEmailFeedbackState.ERROR
            Toast.makeText(
                context,
                "Error al enviar correo electrónico",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Color(0xFFCBD5E1)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Cabecera del modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val (iconRes, badgeBg, badgeTint) = when (type) {
                        ProfileInfoType.TERMS -> Triple(R.drawable.ic_terms_custom, Color(0xFFE6F7F3), Color(0xFF00A884))
                        ProfileInfoType.PRIVACY -> Triple(R.drawable.ic_privacy_custom, Color(0xFFEFF6FF), Color(0xFF2563EB))
                        ProfileInfoType.HELP -> Triple(R.drawable.ic_help_headset_custom, Color(0xFFFEF3C7), Color(0xFFD97706))
                        ProfileInfoType.NONE -> Triple(R.drawable.ic_help_headset_custom, Color(0xFFF1F5F9), Color(0xFF64748B))
                    }

                    Surface(
                        shape = CircleShape,
                        color = badgeBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = badgeTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = when (type) {
                                ProfileInfoType.TERMS -> "Términos y Condiciones"
                                ProfileInfoType.PRIVACY -> "Políticas de Privacidad"
                                ProfileInfoType.HELP -> "Centro de Ayuda"
                                ProfileInfoType.NONE -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F)
                        )
                        Text(
                            text = when (type) {
                                ProfileInfoType.TERMS -> "CampusGO • Normas de la comunidad"
                                ProfileInfoType.PRIVACY -> "Protección y seguridad de tus datos"
                                ProfileInfoType.HELP -> "Preguntas frecuentes y soporte"
                                ProfileInfoType.NONE -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Contenido con scroll (altura máxima acotada sin .weight para evitar vibración de layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight - 90.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (type) {
                    ProfileInfoType.TERMS -> {
                        InfoCardItem(
                            title = "1. Uso Responsable en Campus",
                            description = "CampusGO es una plataforma exclusiva para conectar a la comunidad universitaria con vendedores y puestos autorizados del campus. Al usar la app, aceptas convivir en un marco de respeto, puntualidad y honestidad mutua."
                        )
                        InfoCardItem(
                            title = "2. Compromiso de Pedidos",
                            description = "Al confirmar un pedido, asumes el compromiso vinculante de retirarlo y abonarlo en el punto de encuentro y horario acordado. Los pedidos falsos o cancelaciones arbitrarias perjudican gravemente a los emprendedores universitarios."
                        )
                        InfoCardItem(
                            title = "3. Pagos Directos",
                            description = "Los pagos se efectúan directamente al vendedor mediante Yape, Plin o Efectivo contra entrega. CampusGO no cobra comisiones a compradores ni custodia fondos financieros ni saldo bancario."
                        )
                        InfoCardItem(
                            title = "4. Normas de Convivencia y Calificación",
                            description = "El acoso, lenguaje ofensivo en el chat o ausencias reiteradas conllevan advertencias en el perfil y la suspensión definitiva de la cuenta. Las calificaciones deben reflejar honestamente la experiencia de compra."
                        )
                        InfoCardItem(
                            title = "5. Reclamos y Reportes de Incidencias",
                            description = "Si un puesto no entrega a tiempo o existe un inconveniente con el producto, puedes reportar la incidencia con el botón de advertencia para la mediación del equipo de moderación del campus."
                        )
                        InfoCardItem(
                            title = "6. Política de No-Show y Strikes",
                            description = "Si un comprador no acude a recoger su pedido o un vendedor no entrega lo pactado, se registra una falta oficial. Acumular 5 advertencias (strikes) genera la suspensión automática de compras o ventas."
                        )
                        InfoCardItem(
                            title = "7. Puntos de Encuentro Autorizados",
                            description = "Todas las entregas y transacciones deben realizarse obligatoriamente dentro de las zonas y puntos de encuentro autorizados del campus para garantizar la seguridad de toda la comunidad."
                        )
                        InfoCardItem(
                            title = "8. Calidad y Responsabilidad de Productos",
                            description = "Los vendedores son responsables exclusivos de la frescura, salubridad y calidad de los alimentos y artículos que comercializan, debiendo cumplir con las normas sanitarias institucionales."
                        )
                    }
                    ProfileInfoType.PRIVACY -> {
                        InfoCardItem(
                            title = "1. Información que Recopilamos",
                            description = "Recopilamos únicamente tu nombre completo, número de teléfono para coordinar pedidos, correo electrónico institucional o regular, campus universitario y foto de perfil si decides subirla."
                        )
                        InfoCardItem(
                            title = "2. Finalidad del Uso de Datos",
                            description = "Tus datos personales se utilizan exclusivamente para la gestión operativa de tus órdenes, coordinación en tiempo real en los puntos de entrega y validación de membresía de la comunidad universitaria."
                        )
                        InfoCardItem(
                            title = "3. Confidencialidad y Terceros",
                            description = "No vendemos, alquilamos ni compartimos tus datos personales con empresas de telemarketing, agencias publicitarias ni entidades comerciales externas ajenas a la operación de CampusGO."
                        )
                        InfoCardItem(
                            title = "4. Seguridad y Cifrado",
                            description = "Tu información y contraseñas se almacenan con altos estándares de seguridad en la nube con cifrado criptográfico robusto y control de acceso por roles para protegerla de cualquier vulnerabilidad."
                        )
                        InfoCardItem(
                            title = "5. Gestión y Eliminación de tu Cuenta",
                            description = "Puedes modificar tu información en cualquier momento desde tu Perfil o solicitar la baja y supresión total de tus datos personales contactando directamente a nuestro soporte: soporte@kodexti.com."
                        )
                        InfoCardItem(
                            title = "6. Derechos ARCO",
                            description = "Puedes ejercer en cualquier momento tus derechos de Acceso, Rectificación, Cancelación y Oposición sobre tus datos personales enviando una solicitud formal a soporte@kodexti.com."
                        )
                    }
                    ProfileInfoType.HELP -> {
                        Text(
                            text = "PREGUNTAS FRECUENTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        InfoCardItem(
                            title = "¿Cómo realizo y recojo un pedido?",
                            description = "Navega por los puestos de tu campus, añade productos a tu carrito, elige el punto de encuentro y horario de recogida, y desliza la barra para confirmar."
                        )
                        InfoCardItem(
                            title = "¿Qué hago si el vendedor no se presenta?",
                            description = "Escríbele por el chat de seguimiento de tu orden. Si pasados 10 minutos no responde ni entrega el pedido, puedes generar un reporte con el botón de advertencia para que soporte intervenga."
                        )
                        InfoCardItem(
                            title = "¿Cómo pago con Yape o Plin?",
                            description = "Al momento de la entrega o por el chat coordinado, el vendedor te mostrará su código QR o número registrado para transferir el monto exacto de tu compra."
                        )
                        InfoCardItem(
                            title = "¿Cómo calificar mi compra?",
                            description = "Una vez que el vendedor marque tu pedido como entregado, la app te solicitará calificar con estrellas tu experiencia para orientar a la comunidad universitaria."
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tarjeta de contacto con soporte
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "¿Necesitas ayuda personalizada?",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF166534)
                                    )
                                }
                                Text(
                                    text = "Escríbenos a nuestro correo de soporte estudiantil para resolver cualquier duda o incidencia con tu cuenta o puesto:\n📧 ${SupportEmailHelper.SUPPORT_EMAIL}\n⏰ Horario: Lun - Sáb 8:00 AM a 8:00 PM\n📍 Sede oficial: UCV - Lima Norte",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF15803D),
                                    lineHeight = 18.sp
                                )

                                if (lastSentSuccess != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (lastSentSuccess == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        border = BorderStroke(1.dp, if (lastSentSuccess == true) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (lastSentSuccess == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (lastSentSuccess == true) Color(0xFF16A34A) else Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (lastSentSuccess == true)
                                                    "Se envió correctamente el correo a ${SupportEmailHelper.SUPPORT_EMAIL}"
                                                else
                                                    "Error al enviar correo electrónico",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (lastSentSuccess == true) Color(0xFF166534) else Color(0xFF991B1B)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val copied = SupportEmailHelper.copySupportEmailToClipboard(context)
                                            if (copied) {
                                                Toast.makeText(context, "Correo de soporte copiado (${SupportEmailHelper.SUPPORT_EMAIL})", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF166534)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copiar Correo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = triggerSendEmail,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enviar Email", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    ProfileInfoType.NONE -> {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (type == ProfileInfoType.HELP) "Cerrar" else "Entendido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Alertas automáticas según el resultado del envío al regresar a la aplicación
    when (feedbackState) {
        SupportEmailFeedbackState.SUCCESS -> {
            AlertDialog(
                onDismissRequest = { feedbackState = SupportEmailFeedbackState.NONE },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE6F7F3),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Correo enviado correctamente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF16324F)
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Se envió correctamente el correo a la casilla oficial de soporte:",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE6F7F3),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = SupportEmailHelper.SUPPORT_EMAIL,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                        Text(
                            text = "El equipo de soporte de Campus GO revisará tu caso y responderá a tu correo a la brevedad posible.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { feedbackState = SupportEmailFeedbackState.NONE },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }
        SupportEmailFeedbackState.ERROR -> {
            AlertDialog(
                onDismissRequest = { feedbackState = SupportEmailFeedbackState.NONE },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Error al enviar correo electrónico",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF16324F)
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessageDetail ?: "No se pudo completar el envío del correo electrónico a la dirección de soporte.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Buzón oficial: ${SupportEmailHelper.SUPPORT_EMAIL}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                            )
                        }
                        Text(
                            text = "Por favor verifica tu conexión o copia el correo de soporte para redactar tu consulta manualmente.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                feedbackState = SupportEmailFeedbackState.NONE
                                triggerSendEmail()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar Envío de Correo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                SupportEmailHelper.copySupportEmailToClipboard(context)
                                Toast.makeText(context, "Correo de soporte copiado (${SupportEmailHelper.SUPPORT_EMAIL})", Toast.LENGTH_SHORT).show()
                                feedbackState = SupportEmailFeedbackState.NONE
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF166534)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Correo Oficial", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { feedbackState = SupportEmailFeedbackState.NONE },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                }
            )
        }
        SupportEmailFeedbackState.NONE -> {}
    }
}

@Composable
private fun InfoCardItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16324F)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                lineHeight = 18.sp
            )
        }
    }
}

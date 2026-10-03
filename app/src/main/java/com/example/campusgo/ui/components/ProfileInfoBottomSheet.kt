package com.example.campusgo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R

enum class ProfileInfoType {
    NONE,
    TERMS,
    PRIVACY,
    HELP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoBottomSheet(
    type: ProfileInfoType,
    onDismiss: () -> Unit
) {
    if (type == ProfileInfoType.NONE) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

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
                                    text = "Escríbenos a nuestro correo de soporte estudiantil para resolver cualquier duda o incidencia con tu cuenta o puesto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF15803D)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Soporte CampusGO", "soporte@kodexti.com")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Correo de soporte copiado", Toast.LENGTH_SHORT).show()
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
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:soporte@kodexti.com")
                                                    putExtra(Intent.EXTRA_SUBJECT, "Consulta Soporte CampusGO")
                                                }
                                                context.startActivity(intent)
                                                Toast.makeText(context, "Abriendo tu app de correo para enviar mensaje a soporte@kodexti.com", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Escribe a soporte@kodexti.com", Toast.LENGTH_LONG).show()
                                            }
                                        },
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

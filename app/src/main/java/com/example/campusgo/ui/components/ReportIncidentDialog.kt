package com.example.campusgo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.campusgo.R
import com.example.campusgo.core.util.ImageCompressor
import kotlinx.coroutines.launch

enum class IncidentContextType {
    SELLER,
    BUYER,
    ORDER
}

data class IncidentReasonOption(
    val key: String,
    val label: String,
    val description: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentDialog(
    title: String,
    subtitle: String? = null,
    contextType: IncidentContextType,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reasonKey: String, reasonLabel: String, details: String) -> Unit
) {
    ReportIncidentDialog(
        title = title,
        subtitle = subtitle,
        contextType = contextType,
        isSubmitting = isSubmitting,
        onDismiss = onDismiss,
        onSubmit = { key, label, details, _ -> onSubmit(key, label, details) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentDialog(
    title: String,
    subtitle: String? = null,
    contextType: IncidentContextType,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reasonKey: String, reasonLabel: String, details: String, evidenceBytes: ByteArray?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(contextType) {
        when (contextType) {
            IncidentContextType.SELLER -> listOf(
                IncidentReasonOption(
                    "WRONG_DAMAGED_PRODUCT",
                    "Producto vencido, antihigiénico o en mal estado",
                    "El puesto entregó alimentos o productos no aptos para consumo"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Precios engañosos o cobro mayor al publicado",
                    "Se exigió un precio superior al confirmado en la app"
                ),
                IncidentReasonOption(
                    "NO_SHOW_SELLER",
                    "Vendedor no se presentó al punto acordado",
                    "No acudió a la entrega en el horario pactado"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Conducta inapropiada o falta de respeto",
                    "Trato inadecuado que vulnera el reglamento del campus"
                ),
                IncidentReasonOption(
                    "STORE_UNAVAILABLE",
                    "Puesto inactivo o no atiende pedidos",
                    "Figura abierto pero no responde ni despacha"
                ),
                IncidentReasonOption(
                    "SCAM_SUSPICION",
                    "Sospecha de fraude o suplantación",
                    "Puesto sospechoso o cuenta no autorizada"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro motivo o irregularidad",
                    "Especifica el motivo en los detalles"
                )
            )
            IncidentContextType.BUYER -> listOf(
                IncidentReasonOption(
                    "NO_SHOW_BUYER",
                    "Comprador no se presentó al punto (No-Show)",
                    "El alumno no recogió su pedido tras esperar en el punto acordado"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Negativa de pago del monto pactado",
                    "El comprador no pagó el total o intentó pagar menos"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Conducta irrespetuosa o disturbio",
                    "Trato indebido en el punto de encuentro universitario"
                ),
                IncidentReasonOption(
                    "CANCELADO_VENDEDOR",
                    "Cancelación injustificada en el lugar de entrega",
                    "Canceló cuando el producto ya estaba preparado y listo"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro motivo con el comprador",
                    "Describe la situación en los detalles"
                )
            )
            IncidentContextType.ORDER -> listOf(
                IncidentReasonOption(
                    "NO_SHOW_SELLER",
                    "El vendedor no asistió al punto de encuentro",
                    "Esperé en el horario y lugar indicado sin que se presente"
                ),
                IncidentReasonOption(
                    "WRONG_DAMAGED_PRODUCT",
                    "Producto incompleto, vencido o defectuoso",
                    "El pedido no correspondía con lo comprado o estaba en mal estado"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Cobro indebido o solicitó más dinero en persona",
                    "Alteración del monto acordado al momento del pago"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Mala atención durante la entrega",
                    "Falta de respeto o trato descortés en el campus"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro inconveniente con la compra",
                    "Describe lo sucedido detalladamente"
                )
            )
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Control de flujo en 2 pasos
    var currentStep by remember { mutableStateOf(1) } // 1: Seleccionar motivo, 2: Detalles y evidencia
    var selectedKey by remember { mutableStateOf(options.first().key) }
    var detailsText by remember { mutableStateOf("") }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCompressingImage by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Procesar compresión de imagen seleccionada
    val processImageUri: (Uri) -> Unit = { uri ->
        isCompressingImage = true
        coroutineScope.launch {
            val result = ImageCompressor.compressImageFromUri(context, uri)
            if (result.isSuccess) {
                val bytes = result.getOrThrow()
                selectedImageBytes = bytes
                selectedBitmap?.recycle()
                selectedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                validationError = null
            } else {
                validationError = "No se pudo optimizar la imagen seleccionada."
            }
            isCompressingImage = false
        }
    }

    // Selector moderno PhotoPicker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) processImageUri(uri)
    }

    // Fallback de selector de archivos para dispositivos sin Google Play Services
    val fallbackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) processImageUri(uri)
    }

    val openPickerSafely: () -> Unit = {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: Exception) {
            fallbackPickerLauncher.launch("image/*")
        }
    }

    // Permiso de acceso a galería según versión de Android (no intrusivo)
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openPickerSafely()
        } else {
            Toast.makeText(
                context,
                "Permiso denegado. Puedes continuar con el reporte de texto sin imagen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val handleAttachImageClick: () -> Unit = {
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            mediaPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted) {
            openPickerSafely()
        } else {
            // Se solicita el permiso al usuario; si acepta, se abre la galería y el sistema lo recuerda
            permissionLauncher.launch(mediaPermission)
        }
    }

    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.88f).dp
    val selectedOption = remember(selectedKey, options) {
        options.firstOrNull { it.key == selectedKey } ?: options.first()
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.5.dp)
                        .background(Color(0xFFCBD5E1), CircleShape)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Cabecera adaptable según el paso actual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentStep == 2) {
                        IconButton(
                            onClick = { if (!isSubmitting) currentStep = 1 },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver al paso 1",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = if (currentStep == 1) title else "¿Deseas agregar algo más?",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (currentStep == 1) {
                                subtitle ?: "Paso 1 de 2: Selecciona el motivo principal"
                            } else {
                                "Paso 2 de 2: Detalles y evidencia (Opcional)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFE2E8F0).copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp)
            )

            // Contenedor dinámico según el paso
            if (currentStep == 1) {
                // ==================== PASO 1: SELECCIONAR MOTIVO ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Banner de protección institucional
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Este reporte llegará directamente al Panel del Administrador del Campus para su investigación y seguimiento confidencial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Text(
                        text = "SELECCIONA EL MOTIVO PRINCIPAL",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    // Tarjetas seleccionables con diseño premium estilo Rappi / iOS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            val isSelected = selectedKey == option.key
                            Surface(
                                color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.8.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSubmitting) {
                                        selectedKey = option.key
                                        validationError = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFFCBD5E1), CircleShape)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                        )
                                        option.description?.let { desc ->
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Barra inferior fija para Paso 1
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                validationError = null
                                currentStep = 2
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Continuar (Paso 1 de 2) →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // ==================== PASO 2: DESCRIPCIÓN Y EVIDENCIA ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Resumen del motivo seleccionado con opción de cambiar
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Motivo seleccionado:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = selectedOption.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                            TextButton(
                                onClick = { currentStep = 1 },
                                enabled = !isSubmitting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cambiar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }

                    // Mensaje explicativo
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Este paso es opcional. Puedes añadir una explicación o adjuntar una captura o foto como evidencia (por ejemplo un comprobante de pago o foto del producto) para agilizar la resolución.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Campo de descripción
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (selectedKey == "OTHER") "DETALLES DE LO OCURRIDO *" else "DETALLES DE LO OCURRIDO (OPCIONAL)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = detailsText,
                            onValueChange = {
                                if (it.length <= 400) {
                                    detailsText = it
                                    validationError = null
                                }
                            },
                            placeholder = {
                                Text(
                                    text = if (selectedKey == "OTHER") {
                                        "Por favor especifica brevemente qué sucedió..."
                                    } else {
                                        "Describe qué ocurrió, acuerdos no cumplidos, hora aproximada... (Opcional)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (validationError != null) {
                                Text(
                                    text = validationError ?: "",
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text(
                                text = "${detailsText.length}/400",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Sección de Evidencia Fotográfica (Opcional)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "EVIDENCIA FOTOGRÁFICA (OPCIONAL)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        if (selectedBitmap != null) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Evidencia seleccionada",
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Fotografía adjunta",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Optimizada para tu dispositivo",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedImageBytes = null
                                            selectedBitmap?.recycle()
                                            selectedBitmap = null
                                        },
                                        enabled = !isSubmitting,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFEE2E2), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar foto",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else if (isCompressingImage) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text(
                                        text = "Optimizando imagen de forma segura...",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSubmitting) {
                                        handleAttachImageClick()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Adjuntar Foto o Captura de Prueba",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "Comprobante Yape/Plin, captura de chat o entrega",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Barra inferior fija para Paso 2
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val trimmed = detailsText.trim()
                                if (selectedKey == "OTHER" && trimmed.length < 5) {
                                    validationError = "Por favor detalla brevemente el motivo del reporte."
                                    return@Button
                                }
                                onSubmit(
                                    selectedOption.key,
                                    selectedOption.label,
                                    trimmed,
                                    selectedImageBytes
                                )
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                disabledContainerColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviando reporte...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enviar Reporte al Campus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = { currentStep = 1 },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = "← Volver a cambiar motivo",
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

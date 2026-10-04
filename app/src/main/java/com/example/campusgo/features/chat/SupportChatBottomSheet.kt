package com.example.campusgo.features.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.campusgo.core.notification.CampusGoNotificationHelper
import com.example.campusgo.core.util.ImageCompressor
import com.example.campusgo.domain.model.SupportMessage
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.model.formatIncidentType
import com.example.campusgo.domain.repository.SupportRepository
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla completa de Chat Institucional de Soporte y Mediación entre Usuario y Administrador.
 * Presenta una identidad distintiva (Azul Institucional #003366, dorado y escudo de mediación)
 * que lo diferencia claramente de los chats cotidianos de coordinación de pedidos.
 */
@Composable
fun SupportChatBottomSheet(
    ticket: SupportTicket,
    currentUserId: String,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onResolveTicket: (() -> Unit)? = null,
    supportRepository: SupportRepository = koinInject()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var currentTicket by remember(ticket) { mutableStateOf(ticket) }
    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Estado de selección y compresión de imagen
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCompressingImage by remember { mutableStateOf(false) }

    // Visor de foto ampliada
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    // Registrar en memoria para silenciar notificaciones locales redundantes mientras el usuario está viendo el chat
    DisposableEffect(ticket.id) {
        ActiveChatSessionManager.activeTicketId = ticket.id
        if (ActiveChatSessionManager.isAppInForeground) {
            CampusGoNotificationHelper.cancelChatNotifications(context)
        }
        onDispose {
            ActiveChatSessionManager.activeTicketId = null
        }
    }

    // Observar mensajes reactivamente y estado de resolución del caso
    LaunchedEffect(ticket.id) {
        launch {
            supportRepository.markMessagesAsRead(ticket.id, isAdmin)
            supportRepository.observeMessages(ticket.id).collect { newMessages ->
                messages = newMessages
                if (newMessages.isNotEmpty()) {
                    listState.animateScrollToItem(newMessages.size - 1)
                    if (newMessages.any { it.isAdmin != isAdmin && !it.isRead }) {
                        supportRepository.markMessagesAsRead(ticket.id, isAdmin)
                    }
                }
            }
        }
        launch {
            while (true) {
                delay(3000)
                if (!currentTicket.incidentId.isNullOrBlank()) {
                    val ref = supportRepository.getTicketForIncident(currentTicket.incidentId!!)
                    ref.getOrNull()?.let { currentTicket = it }
                } else {
                    val ref = supportRepository.getTicketById(currentTicket.id)
                    ref.getOrNull()?.let { currentTicket = it }
                }
            }
        }
    }

    val processImageUri: (Uri) -> Unit = { uri ->
        isCompressingImage = true
        coroutineScope.launch {
            val result = ImageCompressor.compressImageFromUri(context, uri)
            if (result.isSuccess) {
                val bytes = result.getOrThrow()
                selectedImageBytes = bytes
                selectedBitmap?.recycle()
                selectedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                Toast.makeText(context, "No se pudo optimizar la imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
            isCompressingImage = false
        }
    }

    // Photo picker compatible con Xiaomi / Samsung / ZTE / Motorola
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) processImageUri(uri)
    }

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
            Toast.makeText(context, "Permiso denegado para acceder a la galería", Toast.LENGTH_SHORT).show()
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
            permissionLauncher.launch(mediaPermission)
        }
    }

    val isResolved = !currentTicket.isOpen || 
        currentTicket.status.equals("RESUELTO", ignoreCase = true) || 
        currentTicket.status.equals("SANCIONADO", ignoreCase = true) || 
        currentTicket.status.equals("CERRADO", ignoreCase = true)

    // Si el caso ya fue resuelto y no es administrador, cerrar inmediatamente para proteger privacidad
    LaunchedEffect(isResolved) {
        if (!isAdmin && isResolved) {
            Toast.makeText(context, "El caso de mediación ha concluido y fue resuelto por la administración.", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Cabecera Institucional Distintiva (Azul Marino CampusGO #003366)
            Surface(
                color = Color(0xFF003366),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = Color.White
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E3A8A),
                            border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Mesa de Mediación",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isResolved) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFFBBF24).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, if (isResolved) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFFBBF24).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = if (isResolved) "RESUELTO" else "EN MEDIACIÓN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isResolved) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatIncidentType(currentTicket.subject),
                                fontSize = 11.5.sp,
                                color = Color(0xFFBAE6FD),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (isAdmin && onResolveTicket != null && !isResolved) {
                            Button(
                                onClick = onResolveTicket,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Resolver", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else if (isResolved) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Resuelto", tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // 2. Banner Institucional de Moderación y Normativa
            Surface(
                color = Color(0xFFEFF6FF),
                border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚖️ Este canal es moderado por la administración universitaria. Los mensajes y evidencias se auditan permanentemente para la resolución justa del caso.",
                        fontSize = 11.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 15.sp
                    )
                }
            }

            // 3. Lista de Mensajes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Canal de Diálogo Oficial Abierto",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Puedes enviar detalles, aclaraciones o evidencias sobre este caso.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == currentUserId

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                // Etiqueta distintiva si es Administrador
                                if (msg.isAdmin) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF003366),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Administración CampusGO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF003366)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    ),
                                    color = when {
                                        isMe -> Color(0xFF003366)
                                        msg.isAdmin -> Color(0xFFEFF6FF)
                                        else -> Color.White
                                    },
                                    border = when {
                                        isMe -> null
                                        msg.isAdmin -> BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        else -> BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    },
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Si tiene foto adjunta
                                        if (!msg.attachmentUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = msg.attachmentUrl,
                                                contentDescription = "Evidencia adjunta",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { fullScreenPhotoUrl = msg.attachmentUrl },
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        if (msg.message.isNotBlank()) {
                                            Text(
                                                text = msg.message,
                                                fontSize = 14.sp,
                                                color = when {
                                                    isMe -> Color.White
                                                    msg.isAdmin -> Color(0xFF003366)
                                                    else -> Color(0xFF1E293B)
                                                },
                                                lineHeight = 19.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatTime(msg.createdAt.orEmpty()),
                                            fontSize = 10.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.75f) else Color(0xFF64748B),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Previsualización de imagen seleccionada antes de enviar
            AnimatedVisibility(visible = selectedBitmap != null && !isResolved) {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedBitmap?.let { bmp ->
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Foto adjunta para este mensaje", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text("Se enviará al presionar el botón de envío", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        IconButton(onClick = {
                            selectedImageBytes = null
                            selectedBitmap?.recycle()
                            selectedBitmap = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // 5. Barra inferior: Si el caso está RESUELTO, BLOQUEAR envío y mostrar tarjeta informativa
            if (isResolved) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Caso Resuelto y Cerrado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "El proceso de mediación ha concluido. El canal permanece en modo solo lectura para fines de auditoría.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Barra de entrada activa que cubre completamente el área del navigation bar con fondo blanco
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Adjuntar Foto
                            IconButton(
                                onClick = { handleAttachImageClick() },
                                enabled = !isSending && !isCompressingImage,
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                            ) {
                                if (isCompressingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF003366))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Adjuntar foto",
                                        tint = Color(0xFF003366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Campo de texto
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Escribe un mensaje de respuesta...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF003366),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            // Botón Enviar
                            val canSend = (inputText.isNotBlank() || selectedImageBytes != null) && !isSending && !isCompressingImage
                            IconButton(
                                onClick = {
                                    if (!canSend) return@IconButton
                                    isSending = true
                                    val textToSend = inputText.trim()
                                    val imageBytesToSend = selectedImageBytes

                                    coroutineScope.launch {
                                        var uploadedImageUrl: String? = null
                                        if (imageBytesToSend != null) {
                                            val uploadResult = supportRepository.uploadEvidenceImage(imageBytesToSend)
                                            uploadedImageUrl = uploadResult.getOrNull()
                                        }

                                        val result = supportRepository.sendMessage(
                                            ticketId = currentTicket.id,
                                            senderId = currentUserId,
                                            message = textToSend,
                                            isAdmin = isAdmin,
                                            attachmentUrl = uploadedImageUrl
                                        )

                                        if (result.isSuccess) {
                                            inputText = ""
                                            selectedImageBytes = null
                                            selectedBitmap?.recycle()
                                            selectedBitmap = null
                                        } else {
                                            Toast.makeText(context, "Error al enviar mensaje: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isSending = false
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(if (canSend) Color(0xFF003366) else Color(0xFFE2E8F0), CircleShape)
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = if (canSend) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para ver foto ampliada
    fullScreenPhotoUrl?.let { url ->
        EnlargedPhotoViewerDialog(
            photoUrl = url,
            name = "Evidencia Fotográfica",
            roleDescription = "Soporte Institucional CampusGO",
            title = "Evidencia Adjunta",
            onDismiss = { fullScreenPhotoUrl = null }
        )
    }
}

private fun formatTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val normalized = isoString.trim().replace(" ", "T")
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        var date: Date? = null
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                date = sdf.parse(normalized)
                if (date != null) break
            } catch (_: Exception) {}
        }
        if (date != null) {
            val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outFormat.format(date)
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}


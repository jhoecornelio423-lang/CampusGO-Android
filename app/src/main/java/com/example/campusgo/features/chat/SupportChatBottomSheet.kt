package com.example.campusgo.features.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.campusgo.core.util.ImageCompressor
import com.example.campusgo.domain.model.SupportMessage
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.repository.SupportRepository
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Estado de selección y compresión de imagen
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCompressingImage by remember { mutableStateOf(false) }

    // Visor de foto ampliada
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.90f).dp

    // Observar mensajes reactivamente
    LaunchedEffect(ticket.id) {
        supportRepository.observeMessages(ticket.id).collect { newMessages ->
            messages = newMessages
            if (newMessages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(newMessages.size - 1)
                }
            }
        }
    }

    // Photo picker compatible con Xiaomi / Samsung
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
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
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
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
            // Cabecera Institucional
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF003366),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Mesa de Diálogo Oficial",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF003366)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = "CASO #${ticket.ticketNumber.takeIf { it > 0 } ?: ticket.id.take(4).uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = ticket.subject.ifBlank { "Soporte y Mediación CampusGO" },
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin && onResolveTicket != null && ticket.isOpen) {
                            Button(
                                onClick = onResolveTicket,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resolver", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Aviso de Privacidad y Normativa
            Surface(
                color = Color(0xFFFFFBEB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚖️ Este canal es moderado por la administración universitaria. Los mensajes y evidencias se auditan para la resolución justa del reclamo.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 15.sp
                    )
                }
            }

            // Lista de Mensajes
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inicia el diálogo con la administración",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Puedes enviar detalles o preguntas adicionales sobre este caso.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
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
                                // Etiqueta de remitente si es Admin
                                if (msg.isAdmin) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF003366),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Administración CampusGO",
                                            fontSize = 10.5.sp,
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
                                    modifier = Modifier.widthIn(max = 290.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Si tiene foto adjunta
                                        if (!msg.attachmentUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = msg.attachmentUrl,
                                                contentDescription = "Evidencia adjunta",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { fullScreenPhotoUrl = msg.attachmentUrl },
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        if (msg.message.isNotBlank()) {
                                            Text(
                                                text = msg.message,
                                                fontSize = 13.5.sp,
                                                color = when {
                                                    isMe -> Color.White
                                                    msg.isAdmin -> Color(0xFF1E3A8A)
                                                    else -> Color(0xFF1E293B)
                                                },
                                                lineHeight = 18.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.createdAt?.takeLast(13)?.take(5) ?: "",
                                            fontSize = 9.5.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Previsualización de imagen seleccionada antes de enviar
            AnimatedVisibility(visible = selectedBitmap != null) {
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
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Foto adjunta para este mensaje", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Se enviará al presionar enviar", fontSize = 11.sp, color = Color(0xFF64748B))
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

            // Barra inferior de entrada
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Adjuntar Foto
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
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
                                    ticketId = ticket.id,
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

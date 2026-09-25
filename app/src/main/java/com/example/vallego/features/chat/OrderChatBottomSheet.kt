package com.example.vallego.features.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import com.example.vallego.ui.components.formatAccountCreationDate
import com.example.vallego.core.notification.ValleGoNotificationHelper
import com.example.vallego.theme.DarkBlue
import com.example.vallego.theme.TurquoiseGreen
import com.example.vallego.theme.WarmYellow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.ChatMessage
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoUserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Color institucional Campus Go para cabecera de chat (Estilo WhatsApp institucional)
private val CampusBlue = Color(0xFF00A884)

/**
 * Pantalla completa de Chat Temporal de Coordinación (Estilo WhatsApp).
 * Ocupa el 100% de la pantalla sin dejar entrever el fondo de la app ni la barra inferior.
 * Conecta con ActiveChatSessionManager para silenciar notificaciones del interlocutor actual.
 */
@Composable
fun OrderChatBottomSheet(
    viewModel: OrderChatViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showFullScreenProfile by remember { mutableStateOf(false) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoName by remember { mutableStateOf("") }
    var enlargedPhotoRole by remember { mutableStateOf("Campus Go") }
    var isEnlargedBanner by remember { mutableStateOf(false) }

    // Asegurar iconos blancos en la barra de estado mientras el chat esté abierto
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(window) {
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevLightStatus = insetsController?.isAppearanceLightStatusBars ?: true
        insetsController?.isAppearanceLightStatusBars = false
        onDispose {
            insetsController?.isAppearanceLightStatusBars = prevLightStatus
        }
    }

    val context = LocalContext.current

    // Cancelar notificaciones de inmediato al montar la pantalla
    LaunchedEffect(Unit) {
        ValleGoNotificationHelper.cancelChatNotifications(context, uiState.subOrderId)
    }

    // Registrar en memoria la conversación activa para que ValleGoPushService
    // suprima las notificaciones locales emergentes de ESTA misma conversación,
    // y cancelar inmediatamente cualquier notificación pendiente en la barra de estado.
    DisposableEffect(uiState.subOrderId, uiState.otherUserId) {
        if (uiState.subOrderId.isNotBlank()) {
            ActiveChatSessionManager.activeSubOrderId = uiState.subOrderId
            ValleGoNotificationHelper.cancelChatNotifications(context, uiState.subOrderId)
        }
        if (uiState.otherUserId.isNotBlank()) {
            ActiveChatSessionManager.activeOtherUserId = uiState.otherUserId
        }
        onDispose {
            ActiveChatSessionManager.activeSubOrderId = null
            ActiveChatSessionManager.activeOtherUserId = null
        }
    }

    // Auto-scroll al último mensaje y asegurar que no queden notificaciones en la barra
    LaunchedEffect(uiState.messages.size) {
        if (uiState.subOrderId.isNotBlank()) {
            ValleGoNotificationHelper.cancelChatNotifications(context, uiState.subOrderId)
        }
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Si está en la pantalla completa de perfil, el botón Atrás regresa al chat
    if (showFullScreenProfile) {
        ChatUserProfileFullScreen(
            otherProfile = uiState.otherUserProfile,
            otherUserName = uiState.otherUserName,
            otherUserAvatarUrl = uiState.otherUserAvatarUrl,
            meetingPoint = uiState.meetingPoint,
            onBack = { showFullScreenProfile = false },
            onOpenEnlargedPhoto = { url, name, role, isBanner ->
                enlargedPhotoUrl = url
                enlargedPhotoName = name
                enlargedPhotoRole = role
                isEnlargedBanner = isBanner
                showEnlargedPhoto = true
            }
        )
        if (showEnlargedPhoto) {
            EnlargedPhotoViewerDialog(
                photoUrl = enlargedPhotoUrl,
                name = enlargedPhotoName,
                roleDescription = enlargedPhotoRole,
                isBanner = isEnlargedBanner,
                onDismiss = { showEnlargedPhoto = false }
            )
        }
        return
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. FONDO DE CHAT FIJO E INMÓVIL (Cubre 100% de la pantalla, no se mueve al abrir el teclado)
            Image(
                painter = painterResource(id = R.drawable.fondo_de_chat),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2. CAPA DE INTERFAZ DEL CHAT (Se ajusta dinámicamente con el teclado sobre el fondo fijo)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                // A. TOP BAR ESTILO CAMPUS GO (Azul amigable y luminoso, no oscuro)
                CampusGoTopBar(
                    otherUserName = uiState.otherUserName,
                    otherUserAvatarUrl = uiState.otherUserAvatarUrl,
                    meetingPoint = uiState.meetingPoint,
                    onBack = onDismiss,
                    onOpenProfile = { showFullScreenProfile = true }
                )

                // B. AVISO DE PRIVACIDAD EFÍMERO (Flota directamente sobre el fondo de chat, sin fondo blanco)
                ChatPrivacyCard(isFinished = uiState.isFinished)

                // C. ZONA PRINCIPAL DE MENSAJES (Transparente para mostrar el wallpaper fijo)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.isLoading && uiState.messages.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            color = TurquoiseGreen,
                            strokeWidth = 3.dp
                        )
                    } else if (uiState.messages.isEmpty()) {
                        EmptyChatState(
                            otherUserName = uiState.otherUserName,
                            otherUserAvatarUrl = uiState.otherUserAvatarUrl,
                            modifier = Modifier.align(Alignment.Center),
                            onOpenProfile = { showFullScreenProfile = true }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                CampusGoMessageBubble(message = message)
                            }
                        }
                    }
                }

                // D. ACCIONES RÁPIDAS (Píldoras flotantes directamente sobre el wallpaper)
                if (!uiState.isFinished) {
                    QuickRepliesRow(
                        onReplySelected = { text ->
                            viewModel.sendQuickMessage(text)
                        }
                    )
                }

                // E. BARRA DE ENTRADA DE TEXTO (Flota al ras del teclado o la barra de navegación)
                if (!uiState.isFinished) {
                    CampusGoInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::onInputTextChanged,
                        onSend = viewModel::sendMessage,
                        isSending = uiState.isSending
                    )
                } else {
                    FinishedOrderChatNotice()
                }
            }
        }


        // 6. VISOR DE FOTOGRAFÍA EN ALTA RESOLUCIÓN
        if (showEnlargedPhoto) {
            EnlargedPhotoViewerDialog(
                photoUrl = enlargedPhotoUrl,
                name = enlargedPhotoName,
                roleDescription = enlargedPhotoRole,
                isBanner = isEnlargedBanner,
                onDismiss = { showEnlargedPhoto = false },
                onOpenProfile = {
                    showEnlargedPhoto = false
                    showFullScreenProfile = true
                }
            )
        }
    }
}

@Composable
private fun CampusGoTopBar(
    otherUserName: String,
    otherUserAvatarUrl: String?,
    meetingPoint: String,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Surface(
        color = CampusBlue, // Azul amigable, fresco y luminoso (no oscuro)
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenProfile() }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ValleGoUserAvatar(
                        avatarUrl = otherUserAvatarUrl,
                        name = otherUserName,
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = otherUserName.ifBlank { "Contacto de Pedido" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = WarmYellow, // Amarillo cálido oficial (#F4B942)
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = meetingPoint.ifBlank { "Punto por convenir" },
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(onClick = onOpenProfile) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info_custom),
                        contentDescription = "Ver Perfil",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPrivacyCard(isFinished: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isFinished) Color(0xFFF1F5F9) else Color(0xFFFEF3C7),
            border = BorderStroke(0.5.dp, if (isFinished) Color(0xFFCBD5E1) else WarmYellow.copy(alpha = 0.5f)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isFinished) Color(0xFF64748B) else Color(0xFFB45309),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFinished) {
                        "Pedido finalizado. Mensajes eliminados de la base de datos."
                    } else {
                        "Chat temporal: los mensajes se eliminan al entregar el pedido."
                    },
                    fontSize = 11.sp,
                    color = if (isFinished) Color(0xFF475569) else Color(0xFF78350F),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CampusGoMessageBubble(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val bubbleColor = if (isFromMe) Color(0xFFD7F5EE) else Color.White
    val textColor = DarkBlue
    val timeColor = Color(0xFF64748B)

    val shape = if (isFromMe) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 3.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    } else {
        RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            border = if (isFromMe) BorderStroke(0.5.dp, TurquoiseGreen.copy(alpha = 0.25f)) else BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.5.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 11.dp, end = 11.dp, top = 7.dp, bottom = 5.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.5.sp,
                    color = textColor,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        fontSize = 10.sp,
                        color = timeColor
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (message.isRead) "Leído" else "Enviado",
                            tint = if (message.isRead) TurquoiseGreen else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickRepliesRow(onReplySelected: (String) -> Unit) {
    val quickOptions = listOf(
        "📍 Ya estoy en el punto",
        "⏳ Llego en 2 min",
        "👋 ¿En qué parte estás?",
        "👕 Visto polo/polera negra",
        "📦 Tu pedido está listo"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickOptions.forEach { option ->
            Surface(
                onClick = { onReplySelected(option) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, TurquoiseGreen.copy(alpha = 0.4f)),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = option,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CampusGoInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    // Flota directamente sobre el fondo del chat sin barra ni franja blanca rectangular
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text("Escribe un mensaje...", fontSize = 14.sp, color = Color(0xFF94A3B8))
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TurquoiseGreen,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .padding(end = 8.dp),
            maxLines = 4
        )

        Surface(
            shape = CircleShape,
            color = if (text.isNotBlank() && !isSending) TurquoiseGreen else Color(0xFFE2E8F0),
            shadowElevation = 2.dp
        ) {
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSending,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.size(46.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensaje",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedOrderChatNotice() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9).copy(alpha = 0.95f),
            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
            shadowElevation = 2.dp
        ) {
            Text(
                text = "El pedido ha sido completado. El chat temporal concluyó y el historial ha sido eliminado por privacidad.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun EmptyChatState(
    otherUserName: String,
    otherUserAvatarUrl: String?,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 340.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.clickable { onOpenProfile() },
                contentAlignment = Alignment.BottomEnd
            ) {
                ValleGoUserAvatar(
                    avatarUrl = otherUserAvatarUrl,
                    name = otherUserName,
                    size = 72.dp
                )
                Surface(
                    shape = CircleShape,
                    color = TurquoiseGreen,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Text(
                text = "Coordinación con $otherUserName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Escribe aquí para acordar los detalles de entrega en el punto de encuentro. Los mensajes se eliminarán al finalizar el pedido.",
                fontSize = 12.5.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            OutlinedButton(
                onClick = onOpenProfile,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TurquoiseGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseGreen),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ver perfil de usuario",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChatUserProfileFullScreen(
    otherProfile: UserProfile?,
    otherUserName: String,
    otherUserAvatarUrl: String?,
    meetingPoint: String,
    onBack: () -> Unit,
    onOpenEnlargedPhoto: (url: String?, name: String, role: String, isBanner: Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    val isSeller = otherProfile?.role == UserRole.EMPRENDEDOR

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Barra superior de navegación nativa (Full Screen TopBar)
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al chat",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSeller) "Perfil del Emprendedor" else "Perfil del Estudiante",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // 2. Contenido scrollable de la pantalla completa
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (otherProfile == null) {
                    // Fallback visual mientras carga el perfil desde la base de datos
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onOpenEnlargedPhoto(
                                            otherUserAvatarUrl,
                                            otherUserName,
                                            "Usuario Campus Go",
                                            false
                                        )
                                    }
                            ) {
                                ValleGoUserAvatar(
                                    avatarUrl = otherUserAvatarUrl,
                                    name = otherUserName,
                                    size = 96.dp
                                )
                            }

                            Text(
                                text = otherUserName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0F172A)
                            )

                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Usuario Campus Go",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                } else if (isSeller) {
                    // PERFIL DEL VENDEDOR / TIENDA (Visto exclusivamente por el Comprador)

                    // Tarjeta Principal del Emprendimiento (Banner + Logo + Nombre) estilo Facebook
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cabecera estilo Facebook: Portada con Avatar superpuesto
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(185.dp)
                            ) {
                                // Portada del Puesto (tocable para ampliar foto)
                                ValleGoBusinessBanner(
                                    bannerUrl = otherProfile.bannerUrl,
                                    storeName = otherProfile.displayStoreName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                        .clickable {
                                            onOpenEnlargedPhoto(
                                                otherProfile.bannerUrl,
                                                otherProfile.displayStoreName,
                                                "Portada del Puesto",
                                                true
                                            )
                                        }
                                )

                                // Avatar superpuesto (Estilo Facebook: centrado y traslapado sobre la portada)
                                Box(
                                    contentAlignment = Alignment.BottomEnd,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .clip(CircleShape)
                                        .clickable {
                                            onOpenEnlargedPhoto(
                                                otherProfile.avatarUrl ?: otherUserAvatarUrl,
                                                otherProfile.displayStoreName,
                                                "Logo del Emprendimiento",
                                                false
                                            )
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        border = BorderStroke(3.5.dp, Color.White),
                                        shadowElevation = 4.dp
                                    ) {
                                        ValleGoBusinessAvatar(
                                            avatarUrl = otherProfile.avatarUrl ?: otherUserAvatarUrl,
                                            storeName = otherProfile.displayStoreName,
                                            size = 90.dp
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = TurquoiseGreen,
                                        border = BorderStroke(2.dp, Color.White),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Ampliar foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Datos del Puesto
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = otherProfile.displayStoreName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    color = Color(0xFFE6F6F3),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                            contentDescription = null,
                                            tint = Color(0xFF0D5C4C),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .offset(y = 1.dp)
                                        )
                                        Text(
                                            text = "Emprendedor Autorizado",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D5C4C)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta de Información Detallada del Puesto (Sin teléfonos)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Información del Puesto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF003366)
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            ProfileDetailRow(
                                icon = Icons.Default.Person,
                                label = "Responsable",
                                value = otherProfile.fullName
                            )

                            if (!otherProfile.businessLocation.isNullOrBlank()) {
                                ProfileDetailRow(
                                    iconPainter = painterResource(id = R.drawable.ic_location_custom),
                                    label = "Ubicación del Puesto",
                                    value = otherProfile.businessLocation
                                )
                            }

                            if (!otherProfile.openTime.isNullOrBlank() || !otherProfile.closeTime.isNullOrBlank()) {
                                ProfileDetailRow(
                                    iconPainter = painterResource(id = R.drawable.ic_alarm_custom),
                                    label = "Horario de Atención",
                                    value = "${otherProfile.openTime ?: "08:00"} - ${otherProfile.closeTime ?: "18:00"}"
                                 )
                            }

                            if (otherProfile.effectivePaymentMethods.isNotEmpty()) {
                                ProfileDetailRow(
                                    icon = Icons.Default.Payments,
                                    label = "Métodos de Pago Aceptados",
                                    value = otherProfile.effectivePaymentMethods.joinToString(", ")
                                 )
                            }

                            val businessDesc = otherProfile.displayBusinessDescription
                            if (!businessDesc.isNullOrBlank()) {
                                ProfileDetailRow(
                                    iconPainter = painterResource(id = R.drawable.ic_info_custom),
                                    label = "Descripción del Negocio",
                                    value = businessDesc
                                )
                            }

                            ProfileDetailRow(
                                icon = Icons.Default.Star,
                                label = "Calificación",
                                value = "%.1f ⭐".format(otherProfile.ratingAverage)
                            )
                        }
                    }
                } else {
                    // PERFIL DEL COMPRADOR / ESTUDIANTE (Visto exclusivamente por el Vendedor)

                    // Tarjeta Principal del Estudiante (Avatar + Nombre + Rol)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Avatar del alumno con indicador de zoom (tocable directamente para ampliar foto)
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onOpenEnlargedPhoto(
                                            otherProfile.avatarUrl ?: otherUserAvatarUrl,
                                            otherProfile.fullName,
                                            "Estudiante Campus-Go",
                                            false
                                        )
                                    }
                            ) {
                                ValleGoUserAvatar(
                                    avatarUrl = otherProfile.avatarUrl ?: otherUserAvatarUrl,
                                    name = otherProfile.fullName,
                                    size = 96.dp
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF003366),
                                    border = BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Ampliar foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = otherProfile.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "🎓 Estudiante / Comprador",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Tarjeta de Información Universitaria (Sin teléfonos)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Información Universitaria",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF003366)
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            ProfileDetailRow(
                                iconPainter = painterResource(id = R.drawable.ic_user_circle_custom),
                                label = "Nombre Completo",
                                value = otherProfile.fullName.ifBlank { "No registrado" }
                            )

                            ProfileDetailRow(
                                iconPainter = painterResource(id = R.drawable.ic_location_custom),
                                label = "Campus Universitario",
                                value = otherProfile.campus
                            )

                            ProfileDetailRow(
                                iconPainter = painterResource(id = R.drawable.ic_account_created_custom),
                                label = "Fecha de Creación de Cuenta",
                                value = formatAccountCreationDate(otherProfile.createdAt)
                            )

                            ProfileDetailRow(
                                icon = Icons.Default.Star,
                                label = "Calificación",
                                value = "%.1f ⭐".format(otherProfile.ratingAverage)
                            )
                        }
                    }
                }

                // Tarjeta del Punto de Entrega Acordado (Contexto del pedido actual)
                if (meetingPoint.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE6F6F3),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_location_custom),
                                        contentDescription = null,
                                        tint = TurquoiseGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Punto de entrega coordinado",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = meetingPoint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = Color(0xFF003366),
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = 1.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF003366),
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

private fun formatTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        var date: Date? = null
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                date = sdf.parse(isoString)
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

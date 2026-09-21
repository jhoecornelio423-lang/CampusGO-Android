package com.example.vallego.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.ChatMessage
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import com.example.vallego.domain.repository.ChatRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderChatUiState(
    val subOrderId: String = "",
    val currentUserId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatarUrl: String? = null,
    val otherUserProfile: UserProfile? = null,
    val currentUserProfile: UserProfile? = null,
    val meetingPoint: String = "",
    val subOrderStatus: SubOrderStatus = SubOrderStatus.PENDIENTE,
    val isFinished: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingProfile: Boolean = false
)

class OrderChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderChatUiState())
    val uiState: StateFlow<OrderChatUiState> = _uiState.asStateFlow()

    private var messageObservationJob: Job? = null

    fun initChat(
        subOrderId: String,
        currentUserId: String,
        otherUserId: String,
        otherUserName: String,
        meetingPoint: String,
        subOrderStatus: SubOrderStatus,
        otherUserAvatarUrl: String? = null
    ) {
        // Registrar de inmediato la conversación activa para silenciar notificaciones locales
        ActiveChatSessionManager.activeSubOrderId = subOrderId
        ActiveChatSessionManager.activeOtherUserId = otherUserId

        val isFinished = subOrderStatus.isFinal
        _uiState.update {
            it.copy(
                subOrderId = subOrderId,
                currentUserId = currentUserId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserAvatarUrl = otherUserAvatarUrl,
                otherUserProfile = null,
                currentUserProfile = null,
                meetingPoint = meetingPoint,
                subOrderStatus = subOrderStatus,
                isFinished = isFinished,
                isLoading = true,
                isLoadingProfile = true
            )
        }

        // Marcar mensajes como leídos de inmediato en base de datos para no dejar notificaciones pendientes
        if (subOrderId.isNotBlank() && currentUserId.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.markMessagesAsRead(subOrderId, currentUserId)
            }
        }

        // Cargar perfil del interlocutor (avatar real y datos completos para visor de perfil)
        if (otherUserId.isNotBlank()) {
            viewModelScope.launch {
                val res = authRepository.getUserProfile(otherUserId)
                res.onSuccess { prof ->
                    _uiState.update { state ->
                        val updatedName = if (prof.role == UserRole.EMPRENDEDOR) {
                            prof.displayStoreName
                        } else {
                            prof.fullName.ifBlank { state.otherUserName }
                        }
                        state.copy(
                            otherUserProfile = prof,
                            otherUserName = updatedName,
                            otherUserAvatarUrl = prof.avatarUrl ?: state.otherUserAvatarUrl,
                            isLoadingProfile = false
                        )
                    }
                }.onFailure {
                    _uiState.update { state -> state.copy(isLoadingProfile = false) }
                }
            }
        }

        // Cargar perfil propio (para que el usuario también pueda consultar su propio perfil desde el chat)
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                val res = authRepository.getUserProfile(currentUserId)
                res.onSuccess { prof ->
                    _uiState.update { state ->
                        state.copy(currentUserProfile = prof)
                    }
                }
            }
        }

        messageObservationJob?.cancel()
        messageObservationJob = viewModelScope.launch {
            chatRepository.observeMessages(subOrderId, currentUserId).collect { messageList ->
                _uiState.update { state ->
                    // Preservar mensajes optimistas locales aún no confirmados remotamente
                    val unconfirmedLocals = state.messages.filter { it.isFromMe && messageList.none { r -> r.content == it.content } }
                    state.copy(
                        messages = (messageList + unconfirmedLocals).distinctBy { it.id }.sortedBy { it.createdAt },
                        isLoading = false
                    )
                }
                // Marcar como leídos si hay mensajes no leídos del otro usuario
                chatRepository.markMessagesAsRead(subOrderId, currentUserId)
            }
        }
    }

    fun updateSubOrderStatus(status: SubOrderStatus) {
        val isFinal = status.isFinal
        _uiState.update {
            it.copy(
                subOrderStatus = status,
                isFinished = isFinal
            )
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        if (text.isBlank() || currentState.isSending || currentState.isFinished) return

        val tempId = UUID.randomUUID().toString()
        val optimisticMsg = ChatMessage(
            id = tempId,
            subOrderId = currentState.subOrderId,
            senderId = currentState.currentUserId,
            receiverId = currentState.otherUserId,
            content = text,
            createdAt = java.time.Instant.now().toString(),
            isRead = false,
            isFromMe = true
        )

        // 1. Mostrar de forma INMEDIATA (0 milisegundos) en la UI
        _uiState.update {
            it.copy(
                inputText = "",
                messages = it.messages + optimisticMsg
            )
        }

        // 2. Enviar a Supabase en segundo plano
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                subOrderId = currentState.subOrderId,
                senderId = currentState.currentUserId,
                receiverId = currentState.otherUserId,
                content = text
            )
            result.onSuccess { confirmedMsg ->
                _uiState.update { state ->
                    val updated = state.messages.map { if (it.id == tempId) confirmedMsg else it }
                    state.copy(messages = updated)
                }
            }
        }
    }

    fun sendQuickMessage(text: String) {
        val currentState = _uiState.value
        if (currentState.isSending || currentState.isFinished) return

        val tempId = UUID.randomUUID().toString()
        val optimisticMsg = ChatMessage(
            id = tempId,
            subOrderId = currentState.subOrderId,
            senderId = currentState.currentUserId,
            receiverId = currentState.otherUserId,
            content = text,
            createdAt = java.time.Instant.now().toString(),
            isRead = false,
            isFromMe = true
        )

        // 1. Inmediato en pantalla
        _uiState.update {
            it.copy(
                messages = it.messages + optimisticMsg
            )
        }

        // 2. Transmisión en red en segundo plano
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                subOrderId = currentState.subOrderId,
                senderId = currentState.currentUserId,
                receiverId = currentState.otherUserId,
                content = text
            )
            result.onSuccess { confirmedMsg ->
                _uiState.update { state ->
                    val updated = state.messages.map { if (it.id == tempId) confirmedMsg else it }
                    state.copy(messages = updated)
                }
            }
        }
    }

    fun clearChat() {
        messageObservationJob?.cancel()
        ActiveChatSessionManager.activeSubOrderId = null
        ActiveChatSessionManager.activeOtherUserId = null
        _uiState.value = OrderChatUiState()
    }

    override fun onCleared() {
        super.onCleared()
        messageObservationJob?.cancel()
    }
}

package com.example.vallego.domain.repository

import com.example.vallego.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * Observa los mensajes en tiempo real asociados a un subpedido específico.
     */
    fun observeMessages(subOrderId: String, currentUserId: String): Flow<List<ChatMessage>>

    /**
     * Envía un nuevo mensaje al destinatario dentro del contexto del subpedido.
     */
    suspend fun sendMessage(
        subOrderId: String,
        senderId: String,
        receiverId: String,
        content: String
    ): Result<ChatMessage>

    /**
     * Marca como leídos los mensajes recibidos para el usuario actual.
     */
    suspend fun markMessagesAsRead(subOrderId: String, currentUserId: String): Result<Unit>

    /**
     * Eliminación de respaldo del chat en cliente cuando el subpedido finaliza.
     */
    suspend fun deleteMessagesForSubOrder(subOrderId: String): Result<Unit>
}

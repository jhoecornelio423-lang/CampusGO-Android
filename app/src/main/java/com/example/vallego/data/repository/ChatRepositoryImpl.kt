package com.example.vallego.data.repository

import android.util.Log
import com.example.vallego.domain.model.ChatMessage
import com.example.vallego.domain.repository.ChatRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RemoteOrderMessageDto(
    val id: String,
    @SerialName("sub_order_id") val subOrderId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_read") val isRead: Boolean = false
)

@Serializable
data class InsertOrderMessageDto(
    @SerialName("sub_order_id") val subOrderId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String
)

@Serializable
data class UpdateMessageReadDto(
    @SerialName("is_read") val isRead: Boolean = true
)

class ChatRepositoryImpl(
    private val postgrest: Postgrest,
    private val realtime: Realtime? = null
) : ChatRepository {

    private val localMessagesCache = ConcurrentHashMap<String, MutableList<ChatMessage>>()

    override fun observeMessages(subOrderId: String, currentUserId: String): Flow<List<ChatMessage>> = flow {
        val cached = localMessagesCache.getOrPut(subOrderId) { mutableListOf() }
        // Emisión inmediata (0ms) de la caché local para evitar spinner bloqueado
        emit(cached.toList())

        // Bucle reactivo de alta frecuencia (1.8s) para chat en campus + emisión inmediata por firma
        var previousSignature: String? = if (cached.isNotEmpty()) {
            cached.joinToString(",") { "${it.id}_${it.isRead}" }
        } else {
            null
        }
        while (true) {
            try {
                if (isValidUUID(subOrderId)) {
                    val remote = postgrest["order_messages"]
                        .select {
                            filter {
                                eq("sub_order_id", subOrderId)
                            }
                            order("created_at", Order.ASCENDING)
                        }
                        .decodeList<RemoteOrderMessageDto>()

                    val domainList = remote.map { it.toDomain(currentUserId) }
                    val list = localMessagesCache.getOrPut(subOrderId) { mutableListOf() }
                    synchronized(list) {
                        list.clear()
                        list.addAll(domainList)
                    }

                    val currentSignature = domainList.joinToString(",") { "${it.id}_${it.isRead}" }
                    if (currentSignature != previousSignature) {
                        previousSignature = currentSignature
                        emit(domainList)
                    }
                } else {
                    // Si no es UUID remoto, emitir lo que haya en caché local
                    emit(cached.toList())
                }
            } catch (e: Exception) {
                Log.d("ChatRepositoryImpl", "Polling messages error: ${e.message}")
                if (previousSignature == null) {
                    previousSignature = "__error__"
                    emit(cached.toList())
                }
            }
            delay(1800L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMessage(
        subOrderId: String,
        senderId: String,
        receiverId: String,
        content: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El mensaje no puede estar vacío."))
        }

        // Mensaje optimista local
        val tempId = UUID.randomUUID().toString()
        val optimisticMsg = ChatMessage(
            id = tempId,
            subOrderId = subOrderId,
            senderId = senderId,
            receiverId = receiverId,
            content = trimmed,
            createdAt = java.time.Instant.now().toString(),
            isRead = false,
            isFromMe = true
        )

        val list = localMessagesCache.getOrPut(subOrderId) { mutableListOf() }
        synchronized(list) {
            list.add(optimisticMsg)
        }

        try {
            if (isValidUUID(subOrderId) && isValidUUID(senderId) && isValidUUID(receiverId)) {
                val dto = InsertOrderMessageDto(
                    subOrderId = subOrderId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = trimmed
                )

                val inserted = postgrest["order_messages"]
                    .insert(dto) {
                        select()
                    }
                    .decodeSingle<RemoteOrderMessageDto>()

                val finalDomain = inserted.toDomain(senderId)
                synchronized(list) {
                    val idx = list.indexOfFirst { it.id == tempId }
                    if (idx >= 0) {
                        list[idx] = finalDomain
                    } else if (list.none { it.id == finalDomain.id }) {
                        list.add(finalDomain)
                    }
                }
                Result.success(finalDomain)
            } else {
                Result.success(optimisticMsg)
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Error sending message to Supabase", e)
            Result.success(optimisticMsg) // Preservar mensaje optimista local para que el usuario no pierda el texto
        }
    }

    override suspend fun markMessagesAsRead(subOrderId: String, currentUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Actualización inmediata en caché local en memoria
            val list = localMessagesCache[subOrderId]
            if (list != null) {
                synchronized(list) {
                    for (i in list.indices) {
                        if (!list[i].isFromMe) {
                            list[i] = list[i].copy(isRead = true)
                        }
                    }
                }
            }

            // 2. Persistencia en Supabase
            if (isValidUUID(subOrderId)) {
                try {
                    postgrest.from("order_messages").update(
                        mapOf("is_read" to true)
                    ) {
                        filter {
                            eq("sub_order_id", subOrderId)
                            eq("is_read", false)
                        }
                    }
                } catch (e1: Exception) {
                    Log.w("ChatRepositoryImpl", "Update con mapOf falló: ${e1.message}, intentando con DTO")
                    try {
                        postgrest.from("order_messages").update(
                            UpdateMessageReadDto(isRead = true)
                        ) {
                            filter {
                                eq("sub_order_id", subOrderId)
                                eq("is_read", false)
                            }
                        }
                    } catch (e2: Exception) {
                        Log.e("ChatRepositoryImpl", "Update con DTO también falló: ${e2.message}")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Error en markMessagesAsRead para subOrderId $subOrderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeUnreadCount(userId: String): Flow<Int> = flow {
        while (true) {
            try {
                if (isValidUUID(userId)) {
                    val unread = postgrest["order_messages"]
                        .select {
                            filter {
                                eq("receiver_id", userId)
                                eq("is_read", false)
                            }
                        }
                        .decodeList<RemoteOrderMessageDto>()
                    emit(unread.size)
                } else {
                    emit(0)
                }
            } catch (e: Exception) {
                emit(0)
            }
            delay(3500L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteMessagesForSubOrder(subOrderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localMessagesCache.remove(subOrderId)
            if (isValidUUID(subOrderId)) {
                postgrest["order_messages"]
                    .delete {
                        filter {
                            eq("sub_order_id", subOrderId)
                        }
                    }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("ChatRepositoryImpl", "Client fallback delete messages: ${e.message}")
            Result.success(Unit)
        }
    }

    private fun RemoteOrderMessageDto.toDomain(currentUserId: String): ChatMessage = ChatMessage(
        id = id,
        subOrderId = subOrderId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        createdAt = createdAt ?: "",
        isRead = isRead,
        isFromMe = senderId == currentUserId
    )

    private fun isValidUUID(value: String): Boolean = try {
        UUID.fromString(value)
        true
    } catch (_: Exception) {
        false
    }
}

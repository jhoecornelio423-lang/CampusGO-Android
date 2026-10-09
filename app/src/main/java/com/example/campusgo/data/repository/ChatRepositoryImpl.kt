package com.example.campusgo.data.repository

import android.util.Log
import com.example.campusgo.domain.model.ChatMessage
import com.example.campusgo.domain.repository.ChatRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    override fun observeMessages(subOrderId: String, currentUserId: String): Flow<List<ChatMessage>> = channelFlow {
        val cached = localMessagesCache.getOrPut(subOrderId) { mutableListOf() }
        // Emisión inmediata (0ms) de la caché local para evitar spinner bloqueado
        send(cached.toList())

        suspend fun fetchAndSendMessages() {
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
                    send(domainList)
                } else {
                    send(cached.toList())
                }
            } catch (e: Exception) {
                Log.d("ChatRepositoryImpl", "Fetch messages error: ${e.message}")
            }
        }

        fetchAndSendMessages()

        if (realtime != null && isValidUUID(subOrderId)) {
            try {
                val chatChannel = realtime.channel("chat_$subOrderId")
                val changeFlow = chatChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "order_messages"
                }
                chatChannel.subscribe()

                launch {
                    changeFlow.collect {
                        fetchAndSendMessages()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatRepositoryImpl", "Realtime channel subscription error: ${e.message}")
            }
        }

        while (isActive) {
            delay(10000L)
            fetchAndSendMessages()
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
            var effectiveReceiverId = receiverId
            if (!isValidUUID(effectiveReceiverId) && isValidUUID(subOrderId)) {
                val cachedMsg = list.find { it.senderId != senderId && isValidUUID(it.senderId) }
                if (cachedMsg != null) {
                    effectiveReceiverId = cachedMsg.senderId
                } else {
                    try {
                        val subDto = postgrest["sub_orders"]
                            .select { filter { eq("id", subOrderId) } }
                            .decodeSingleOrNull<RemoteSubOrderDto>()
                        if (subDto != null) {
                            effectiveReceiverId = if (subDto.sellerId == senderId) {
                                subDto.buyerId ?: ""
                            } else {
                                subDto.sellerId
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            if (isValidUUID(subOrderId) && isValidUUID(senderId) && isValidUUID(effectiveReceiverId)) {
                val dto = InsertOrderMessageDto(
                    subOrderId = subOrderId,
                    senderId = senderId,
                    receiverId = effectiveReceiverId,
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

            // 2. Persistencia en Supabase: ÚNICAMENTE marcar leídos aquellos mensajes recibidos por el usuario actual
            if (isValidUUID(subOrderId) && isValidUUID(currentUserId)) {
                try {
                    postgrest.from("order_messages").update(
                        mapOf("is_read" to true)
                    ) {
                        filter {
                            eq("sub_order_id", subOrderId)
                            eq("receiver_id", currentUserId)
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
                                eq("receiver_id", currentUserId)
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

    override fun observeUnreadCount(userId: String): Flow<Int> = channelFlow {
        suspend fun computeAndSend() {
            val localUnread = localMessagesCache.values.sumOf { list ->
                synchronized(list) {
                    list.count { it.receiverId == userId && !it.isRead && !it.isFromMe }
                }
            }
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
                    send(maxOf(unread.size, localUnread))
                } else {
                    send(localUnread)
                }
            } catch (e: Exception) {
                Log.d("ChatRepositoryImpl", "observeUnreadCount fallback local: ${e.message}")
                send(localUnread)
            }
        }

        computeAndSend()

        if (realtime != null && isValidUUID(userId)) {
            try {
                val unreadChannel = realtime.channel("unread_$userId")
                val changeFlow = unreadChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "order_messages"
                }
                unreadChannel.subscribe()

                launch {
                    changeFlow.collect {
                        computeAndSend()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatRepositoryImpl", "Realtime unread subscription error: ${e.message}")
            }
        }

        while (isActive) {
            delay(15000L)
            computeAndSend()
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

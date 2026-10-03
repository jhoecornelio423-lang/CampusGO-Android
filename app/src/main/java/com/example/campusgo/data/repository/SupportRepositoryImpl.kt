package com.example.campusgo.data.repository

import android.util.Log
import com.example.campusgo.domain.model.SupportMessage
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.repository.SupportRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SupportRepositoryImpl(
    private val postgrest: Postgrest,
    private val storage: Storage
) : SupportRepository {

    private val localMessagesCache = ConcurrentHashMap<String, MutableList<SupportMessage>>()

    override suspend fun getOrCreateTicketForIncident(
        userId: String,
        incidentId: String,
        subject: String
    ): Result<SupportTicket> = withContext(Dispatchers.IO) {
        try {
            val existing = postgrest.from("support_tickets").select {
                filter {
                    eq("incident_id", incidentId)
                    eq("user_id", userId)
                }
                limit(1)
            }.decodeList<SupportTicket>().firstOrNull()

            if (existing != null) {
                return@withContext Result.success(existing)
            }

            val newId = UUID.randomUUID().toString()
            postgrest.from("support_tickets").insert(
                buildJsonObject {
                    put("id", newId)
                    put("user_id", userId)
                    put("incident_id", incidentId)
                    put("subject", subject.ifBlank { "Incidencia CampusGO" })
                    put("status", "ABIERTO")
                }
            )

            val created = postgrest.from("support_tickets").select {
                filter { eq("id", newId) }
            }.decodeSingle<SupportTicket>()

            Result.success(created)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al obtener o crear ticket: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveTicketForUser(userId: String): Result<SupportTicket?> = withContext(Dispatchers.IO) {
        try {
            val tickets = postgrest.from("support_tickets").select {
                filter {
                    eq("user_id", userId)
                    neq("status", "RESUELTO")
                    neq("status", "CERRADO")
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<SupportTicket>()

            Result.success(tickets.firstOrNull())
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al consultar ticket activo de usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTicketForIncident(incidentId: String): Result<SupportTicket?> = withContext(Dispatchers.IO) {
        try {
            val tickets = postgrest.from("support_tickets").select {
                filter { eq("incident_id", incidentId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<SupportTicket>()

            Result.success(tickets.firstOrNull())
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al consultar ticket por incidencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun observeMessages(ticketId: String): Flow<List<SupportMessage>> = flow {
        val cached = localMessagesCache.getOrPut(ticketId) { mutableListOf() }
        emit(cached.toList())

        var previousSignature: String? = if (cached.isNotEmpty()) {
            cached.joinToString(",") { it.id }
        } else null

        while (true) {
            try {
                val remote = postgrest.from("support_messages").select {
                    filter { eq("ticket_id", ticketId) }
                    order("created_at", Order.ASCENDING)
                }.decodeList<SupportMessage>()

                val list = localMessagesCache.getOrPut(ticketId) { mutableListOf() }
                synchronized(list) {
                    list.clear()
                    list.addAll(remote)
                }

                val currentSignature = remote.joinToString(",") { it.id }
                if (currentSignature != previousSignature) {
                    previousSignature = currentSignature
                    emit(remote)
                }
            } catch (e: Exception) {
                Log.d("SupportRepo", "Error polling support messages: ${e.message}")
            }
            delay(2000L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMessage(
        ticketId: String,
        senderId: String,
        message: String,
        isAdmin: Boolean,
        attachmentUrl: String?
    ): Result<SupportMessage> = withContext(Dispatchers.IO) {
        try {
            val messageId = UUID.randomUUID().toString()
            val insertPayload = buildJsonObject {
                put("id", messageId)
                put("ticket_id", ticketId)
                put("sender_id", senderId)
                put("message", message.trim())
                put("is_admin", isAdmin)
                attachmentUrl?.let { put("attachment_url", it) }
            }

            postgrest.from("support_messages").insert(insertPayload)

            // Actualizar timestamp del ticket y pasarlo a EN_PROCESO si estaba ABIERTO
            postgrest.from("support_tickets").update(
                buildJsonObject {
                    put("status", "EN_PROCESO")
                }
            ) {
                filter {
                    eq("id", ticketId)
                    eq("status", "ABIERTO")
                }
            }

            val savedMessage = SupportMessage(
                id = messageId,
                ticketId = ticketId,
                senderId = senderId,
                message = message.trim(),
                isAdmin = isAdmin,
                attachmentUrl = attachmentUrl
            )

            // Cache local inmediata
            localMessagesCache[ticketId]?.let { list ->
                synchronized(list) {
                    if (list.none { it.id == messageId }) {
                        list.add(savedMessage)
                    }
                }
            }

            Result.success(savedMessage)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al enviar mensaje de soporte: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun resolveTicket(
        ticketId: String,
        adminNotes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("support_tickets").update(
                buildJsonObject {
                    put("status", "RESUELTO")
                    adminNotes?.let { put("admin_notes", it) }
                }
            ) {
                filter { eq("id", ticketId) }
            }
            // Limpiar caché local
            localMessagesCache.remove(ticketId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al resolver ticket de soporte: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadEvidenceImage(
        imageBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "evidence_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}.jpg"
            val bucket = storage.from("support-evidences")
            bucket.upload(path = fileName, data = imageBytes) {
                upsert = true
            }
            val publicUrl = bucket.publicUrl(path = fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al subir evidencia a Storage: ${e.message}", e)
            Result.failure(e)
        }
    }
}

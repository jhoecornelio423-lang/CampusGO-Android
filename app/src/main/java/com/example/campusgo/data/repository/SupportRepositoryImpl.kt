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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
                    eq("is_open", true)
                    neq("status", "RESUELTO")
                    neq("status", "CERRADO")
                    neq("status", "SANCIONADO")
                    neq("status", "DESCARTADO")
                }
                order("created_at", Order.DESCENDING)
                limit(5)
            }.decodeList<SupportTicket>()

            for (candidate in tickets) {
                if (!candidate.incidentId.isNullOrBlank()) {
                    val incStatus = runCatching {
                        postgrest.from("order_incidents").select {
                            filter { eq("id", candidate.incidentId!!) }
                        }.decodeList<JsonObject>().firstOrNull()?.get("status")?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    if (incStatus != null && !incStatus.equals("PENDIENTE", ignoreCase = true)) {
                        continue
                    }
                }

                // REGLA: El chat de reporte NO debe ser visible por el reportador desde un inicio,
                // sino después de que el administrador envíe el primer mensaje (is_admin = true).
                val hasAdminMessage = runCatching {
                    val adminMsgs = postgrest.from("support_messages").select {
                        filter {
                            eq("ticket_id", candidate.id)
                            eq("is_admin", true)
                        }
                        limit(1)
                    }.decodeList<JsonObject>()
                    adminMsgs.isNotEmpty()
                }.getOrDefault(false)

                if (hasAdminMessage) {
                    return@withContext Result.success(candidate)
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al consultar ticket activo de usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTicketById(ticketId: String): Result<SupportTicket?> = withContext(Dispatchers.IO) {
        try {
            val tickets = postgrest.from("support_tickets").select {
                filter { eq("id", ticketId) }
                limit(1)
            }.decodeList<SupportTicket>()

            var ticket = tickets.firstOrNull()
            if (ticket != null && !ticket.incidentId.isNullOrBlank()) {
                val incStatus = runCatching {
                    postgrest.from("order_incidents").select {
                        filter { eq("id", ticket.incidentId!!) }
                    }.decodeList<JsonObject>().firstOrNull()?.get("status")?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (incStatus != null && !incStatus.equals("PENDIENTE", ignoreCase = true)) {
                    ticket = ticket.copy(status = incStatus)
                }
            }

            Result.success(ticket)
        } catch (e: Exception) {
            Log.e("SupportRepo", "Error al consultar ticket por ID: ${e.message}", e)
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

            var ticket = tickets.firstOrNull()
            if (ticket != null) {
                val incStatus = runCatching {
                    postgrest.from("order_incidents").select {
                        filter { eq("id", incidentId) }
                    }.decodeList<JsonObject>().firstOrNull()?.get("status")?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (incStatus != null && !incStatus.equals("PENDIENTE", ignoreCase = true)) {
                    ticket = ticket.copy(status = incStatus)
                }
            }

            Result.success(ticket)
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
            val ticket = runCatching {
                postgrest.from("support_tickets").select {
                    filter { eq("id", ticketId) }
                }.decodeList<SupportTicket>().firstOrNull()
            }.getOrNull()

            val isTicketClosed = ticket == null ||
                !ticket.isOpen ||
                ticket.status.equals("RESUELTO", ignoreCase = true) ||
                ticket.status.equals("CERRADO", ignoreCase = true) ||
                ticket.status.equals("SANCIONADO", ignoreCase = true) ||
                ticket.status.equals("DESCARTADO", ignoreCase = true)

            if (isTicketClosed) {
                return@withContext Result.failure(IllegalStateException("El caso ya se encuentra resuelto y cerrado."))
            }

            if (!ticket.incidentId.isNullOrBlank()) {
                val incStatus = runCatching {
                    postgrest.from("order_incidents").select {
                        filter { eq("id", ticket.incidentId!!) }
                    }.decodeList<JsonObject>().firstOrNull()?.get("status")?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (incStatus != null && !incStatus.equals("PENDIENTE", ignoreCase = true)) {
                    return@withContext Result.failure(IllegalStateException("El caso ya se encuentra resuelto y cerrado."))
                }
            }

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

            // Actualizar timestamp del ticket y pasarlo a EN_PROCESO si estaba ABIERTO (solo admin tiene permiso de update)
            if (isAdmin) {
                try {
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
                } catch (statusEx: Exception) {
                    Log.w("SupportRepo", "No se pudo actualizar estado a EN_PROCESO: ${statusEx.message}")
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
            val ticket = runCatching {
                postgrest.from("support_tickets").select {
                    filter { eq("id", ticketId) }
                }.decodeList<SupportTicket>().firstOrNull()
            }.getOrNull()

            postgrest.from("support_tickets").update(
                buildJsonObject {
                    put("status", "RESUELTO")
                    put("is_open", false)
                    adminNotes?.let { put("admin_notes", it) }
                }
            ) {
                filter { eq("id", ticketId) }
            }

            if (!ticket?.incidentId.isNullOrBlank()) {
                try {
                    postgrest.from("order_incidents").update(
                        buildJsonObject {
                            put("status", "RESUELTO")
                            put("resolution_action", "CHAT_MEDIATION")
                            adminNotes?.let { put("admin_notes", it) }
                        }
                    ) {
                        filter { eq("id", ticket!!.incidentId!!) }
                    }
                } catch (incEx: Exception) {
                    Log.w("SupportRepo", "Aviso al sincronizar order_incidents desde resolveTicket: ${incEx.message}")
                }
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

    override suspend fun markMessagesAsRead(
        ticketId: String,
        isCurrentUserAdmin: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("support_messages").update(
                buildJsonObject {
                    put("is_read", true)
                }
            ) {
                filter {
                    eq("ticket_id", ticketId)
                    eq("is_admin", !isCurrentUserAdmin)
                    eq("is_read", false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("SupportRepo", "Error al marcar mensajes como leídos: ${e.message}")
            Result.failure(e)
        }
    }
}

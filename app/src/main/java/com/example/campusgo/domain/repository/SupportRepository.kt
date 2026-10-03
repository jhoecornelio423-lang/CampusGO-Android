package com.example.campusgo.domain.repository

import com.example.campusgo.domain.model.SupportMessage
import com.example.campusgo.domain.model.SupportTicket
import kotlinx.coroutines.flow.Flow

interface SupportRepository {
    suspend fun getOrCreateTicketForIncident(
        userId: String,
        incidentId: String,
        subject: String
    ): Result<SupportTicket>

    suspend fun getActiveTicketForUser(userId: String): Result<SupportTicket?>

    suspend fun getTicketForIncident(incidentId: String): Result<SupportTicket?>

    suspend fun observeMessages(ticketId: String): Flow<List<SupportMessage>>

    suspend fun sendMessage(
        ticketId: String,
        senderId: String,
        message: String,
        isAdmin: Boolean,
        attachmentUrl: String? = null
    ): Result<SupportMessage>

    suspend fun resolveTicket(
        ticketId: String,
        adminNotes: String? = null
    ): Result<Unit>

    suspend fun uploadEvidenceImage(
        imageBytes: ByteArray
    ): Result<String>
}

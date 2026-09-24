package com.example.vallego.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ApplicationStatusSerializer : KSerializer<ApplicationStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ApplicationStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ApplicationStatus) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): ApplicationStatus {
        val str = runCatching { decoder.decodeString().trim().lowercase() }.getOrDefault("")
        return when (str) {
            "aprobada", "aprobado", "approved" -> ApplicationStatus.APROBADA
            "rechazada", "rechazado", "rejected" -> ApplicationStatus.RECHAZADA
            else -> ApplicationStatus.PENDIENTE
        }
    }
}

@Serializable(with = ApplicationStatusSerializer::class)
enum class ApplicationStatus {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}

@Serializable
data class SellerApplication(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val applicantName: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("business_name") val storeName: String = "",
    @SerialName("business_category") val category: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("proposed_location") val proposedLocation: String? = null,
    @SerialName("status") val status: ApplicationStatus = ApplicationStatus.PENDIENTE,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val studentEmail: String get() = ""
}
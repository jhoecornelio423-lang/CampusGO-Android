package com.example.vallego.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.vallego.domain.model.UserProfile
import java.util.concurrent.ConcurrentHashMap

/**
 * Almacenamiento local persistente y en memoria para los métodos de pago
 * configurados por los vendedores universitarios.
 *
 * Garantiza sincronización inmediata en la nube multi-dispositivo (a través de
 * metadatos en descripción y columnas nativas) y resiliencia local con SharedPreferences.
 */
object SellerPaymentMethodsStorage {
    private val memoryCache = ConcurrentHashMap<String, List<String>>()
    @Volatile
    private var appContext: Context? = null

    private val PM_TAG_REGEX = Regex("<!--PM:([a-zA-Z0-9_,\\s]+)-->")

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences("vallego_seller_payment_methods", Context.MODE_PRIVATE)
    }

    fun parseMethodsFromDescription(description: String?): List<String>? {
        if (description.isNullOrBlank()) return null
        val match = PM_TAG_REGEX.find(description) ?: return null
        val raw = match.groupValues.getOrNull(1) ?: return null
        val list = raw.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        return if (list.isNotEmpty()) list else null
    }

    fun cleanDescription(description: String?): String? {
        if (description.isNullOrBlank()) return description
        val cleaned = description.replace(PM_TAG_REGEX, "").trim()
        return cleaned.ifBlank { null }
    }

    fun embedMethodsInDescription(description: String?, methods: List<String>): String? {
        val base = cleanDescription(description) ?: ""
        if (methods.isEmpty()) return base.ifBlank { null }
        val tag = "<!--PM:${methods.joinToString(",").uppercase()}-->"
        return if (base.isBlank()) tag else "$base\n$tag"
    }

    fun getMethods(sellerId: String): List<String>? {
        if (sellerId.isBlank()) return null
        val inMem = memoryCache[sellerId]
        if (!inMem.isNullOrEmpty()) return inMem

        val prefs = getPrefs() ?: return null
        val raw = prefs.getString("methods_$sellerId", null) ?: return null
        val list = raw.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        if (list.isNotEmpty()) {
            memoryCache[sellerId] = list
            return list
        }
        return null
    }

    fun saveMethods(sellerId: String, methods: List<String>) {
        if (sellerId.isBlank() || methods.isEmpty()) return
        val normalized = methods.map { it.trim().uppercase() }.filter { it.isNotBlank() }
        if (normalized.isEmpty()) return
        memoryCache[sellerId] = normalized
        val prefs = getPrefs() ?: return
        prefs.edit().putString("methods_$sellerId", normalized.joinToString(",")).apply()
    }

    fun enrichProfile(profile: UserProfile): UserProfile {
        val cleanedDesc = cleanDescription(profile.businessDescription)
        return if (profile.supportedPaymentMethods.isNotEmpty()) {
            saveMethods(profile.id, profile.supportedPaymentMethods)
            profile.copy(businessDescription = cleanedDesc)
        } else {
            val fromDesc = parseMethodsFromDescription(profile.businessDescription)
            if (!fromDesc.isNullOrEmpty()) {
                saveMethods(profile.id, fromDesc)
                profile.copy(
                    supportedPaymentMethods = fromDesc,
                    businessDescription = cleanedDesc
                )
            } else {
                val cached = getMethods(profile.id)
                if (!cached.isNullOrEmpty()) {
                    profile.copy(
                        supportedPaymentMethods = cached,
                        businessDescription = cleanedDesc
                    )
                } else {
                    profile.copy(businessDescription = cleanedDesc)
                }
            }
        }
    }
}

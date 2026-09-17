package com.example.vallego.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.vallego.domain.model.UserProfile
import java.util.concurrent.ConcurrentHashMap

/**
 * Almacenamiento local persistente y en memoria para los métodos de pago
 * configurados por los vendedores universitarios.
 *
 * Garantiza resiliencia frente a caídas de red, migraciones de base de datos
 * pendientes en Supabase y asegura que la selección del vendedor nunca se revierta.
 */
object SellerPaymentMethodsStorage {
    private val memoryCache = ConcurrentHashMap<String, List<String>>()
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences("vallego_seller_payment_methods", Context.MODE_PRIVATE)
    }

    fun getMethods(sellerId: String): List<String>? {
        if (sellerId.isBlank()) return null
        val inMem = memoryCache[sellerId]
        if (!inMem.isNullOrEmpty()) return inMem

        val prefs = getPrefs() ?: return null
        val raw = prefs.getString("methods_$sellerId", null) ?: return null
        val list = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (list.isNotEmpty()) {
            memoryCache[sellerId] = list
            return list
        }
        return null
    }

    fun saveMethods(sellerId: String, methods: List<String>) {
        if (sellerId.isBlank() || methods.isEmpty()) return
        memoryCache[sellerId] = methods
        val prefs = getPrefs() ?: return
        prefs.edit().putString("methods_$sellerId", methods.joinToString(",")).apply()
    }

    fun enrichProfile(profile: UserProfile): UserProfile {
        return if (profile.supportedPaymentMethods.isNotEmpty()) {
            saveMethods(profile.id, profile.supportedPaymentMethods)
            profile
        } else {
            val cached = getMethods(profile.id)
            if (!cached.isNullOrEmpty()) {
                profile.copy(supportedPaymentMethods = cached)
            } else {
                profile
            }
        }
    }
}

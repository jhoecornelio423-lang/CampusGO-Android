package com.example.campusgo.core.notification

import android.content.Context
import android.util.Log
import com.example.campusgo.features.chat.ActiveChatSessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Servicio de Firebase Cloud Messaging (FCM) para recepción nativa de notificaciones push en CampusGO.
 * Despierta el dispositivo de forma instantánea incluso en reposo profundo (Doze Mode)
 * o con la aplicación completamente cerrada, sin necesidad de bucles HTTP locales.
 */
class CampusGoFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val auth: Auth by inject()
    private val postgrest: Postgrest by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nuevo FCM Token recibido: $token")

        // Persistir localmente en preferencias para sincronizar con Supabase
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        // Sincronizar con Supabase si hay una sesión activa
        serviceScope.launch {
            syncTokenWithSupabase(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Notificación FCM recibida de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "CampusGO"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""
        val type = data["type"] ?: "order"
        val orderId = data["order_id"]
        val subOrderId = data["sub_order_id"]
        val senderId = data["sender_id"]
        val senderName = data["sender_name"] ?: title

        Log.i(TAG, "FCM Payload: type=$type, orderId=$orderId, subOrderId=$subOrderId, title=$title, body=$body")

        if (type.equals("chat", ignoreCase = true) ||
            type.equals("chat_message", ignoreCase = true) ||
            type.equals("message", ignoreCase = true)) {
            // Si el usuario ya está conversando activamente con el remitente en primer plano, no duplicar alerta
            val isChatOpen = ActiveChatSessionManager.isChatActiveWith(
                subOrderId = subOrderId ?: "",
                senderId = senderId ?: ""
            )

            if (isChatOpen) {
                Log.i(TAG, "Chat activo en pantalla en primer plano. Silenciando notificación FCM.")
                return
            }

            val resolvedNotifId = if (!subOrderId.isNullOrBlank()) {
                Math.abs(subOrderId.hashCode())
            } else {
                Math.abs((senderId ?: body).hashCode())
            }

            CampusGoNotificationHelper.showChatNotification(
                context = applicationContext,
                notificationId = resolvedNotifId,
                senderName = senderName,
                message = body,
                subOrderId = subOrderId
            )
        } else {
            // Notificación de nuevo pedido o cambio de estado
            val notifId = (subOrderId ?: orderId ?: body).hashCode()
            CampusGoNotificationHelper.showOrderNotification(
                context = applicationContext,
                notificationId = notifId,
                title = title,
                message = body,
                orderId = orderId
            )
        }
    }

    private suspend fun syncTokenWithSupabase(token: String) {
        try {
            val user = auth.currentUserOrNull() ?: return

            // 1. Persistir en profiles
            kotlin.runCatching {
                postgrest.from("profiles").update(
                    mapOf("fcm_token" to token)
                ) {
                    filter { eq("id", user.id) }
                }
            }

            // 2. Limpiar tokens obsoletos en push_tokens para evitar fallos 404 de Firebase
            kotlin.runCatching {
                postgrest.from("push_tokens").delete {
                    filter {
                        eq("user_id", user.id)
                        neq("token", token)
                    }
                }
            }

            // 3. Registrar o actualizar token activo en push_tokens para la Edge Function send-push
            kotlin.runCatching {
                postgrest.from("push_tokens").upsert(
                    mapOf(
                        "user_id" to user.id,
                        "token" to token,
                        "platform" to "android"
                    )
                )
            }
            Log.i(TAG, "FCM Token sincronizado exitosamente con profiles y push_tokens para usuario ${user.id}")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso sincronizando FCM Token con Supabase: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CampusGoFCM"
        const val PREFS_NAME = "campusgo_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"

        /**
         * Permite sincronizar manualmente el token guardado tras el inicio de sesión.
         */
        suspend fun syncCurrentTokenWithSupabase(context: Context, auth: Auth, postgrest: Postgrest) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_FCM_TOKEN, null) ?: return
            val user = auth.currentUserOrNull() ?: return

            try {
                // 1. Actualizar profiles
                kotlin.runCatching {
                    postgrest.from("profiles").update(
                        mapOf("fcm_token" to token)
                    ) {
                        filter { eq("id", user.id) }
                    }
                }

                // 2. Limpiar tokens obsoletos de este usuario
                kotlin.runCatching {
                    postgrest.from("push_tokens").delete {
                        filter {
                            eq("user_id", user.id)
                            neq("token", token)
                        }
                    }
                }

                // 3. Registrar token activo en push_tokens
                kotlin.runCatching {
                    postgrest.from("push_tokens").upsert(
                        mapOf(
                            "user_id" to user.id,
                            "token" to token,
                            "platform" to "android"
                        )
                    )
                }
                Log.i(TAG, "FCM Token actualizado en profiles y push_tokens tras login para ${user.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando FCM Token en Supabase: ${e.message}")
            }
        }
    }
}

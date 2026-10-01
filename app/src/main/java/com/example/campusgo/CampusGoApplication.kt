package com.example.campusgo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.campusgo.core.di.networkModule
import com.example.campusgo.core.di.repositoryModule
import com.example.campusgo.core.di.uiModule
import com.example.campusgo.core.notification.CampusGoNotificationHelper
import com.example.campusgo.core.notification.PushWatchdogReceiver
import com.example.campusgo.data.repository.SellerPaymentMethodsStorage
import com.example.campusgo.features.chat.ActiveChatSessionManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class CampusGoApplication : Application(), ImageLoaderFactory, KoinComponent {

    private val auth: Auth by inject()
    private val postgrest: Postgrest by inject()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: android.content.Context) {
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO or
                (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
        val context = base.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CampusGoApplication)
            modules(networkModule, repositoryModule, uiModule)
        }
        CampusGoNotificationHelper.createNotificationChannels(this)
        SellerPaymentMethodsStorage.initialize(this)

        // Monitorear si la app está en primer o segundo plano para controlar sonido de notificaciones
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                ActiveChatSessionManager.isAppInForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    ActiveChatSessionManager.isAppInForeground = false
                }
            }

            override fun onActivityResumed(activity: Activity) {
                ActiveChatSessionManager.isAppInForeground = true
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        try {
            PushWatchdogReceiver.cancelWatchdog(this)
        } catch (_: Exception) {}

        // Iniciar escucha reactiva en segundo plano para mensajes de chat mientras el proceso de la app esté activo
        startBackgroundChatObserver()
    }

    private fun startBackgroundChatObserver() {
        appScope.launch {
            android.util.Log.d("CampusGoChatObserver", "Observador de mensajes de chat iniciado.")
            while (isActive) {
                try {
                    val user = auth.currentUserOrNull()
                    if (user != null) {
                        val unread = postgrest.from("order_messages")
                            .select {
                                filter {
                                    eq("receiver_id", user.id)
                                    eq("is_read", false)
                                }
                                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                                limit(10)
                            }
                            .decodeList<com.example.campusgo.data.repository.RemoteOrderMessageDto>()

                        val prefs = getSharedPreferences("campusgo_notifs_cache", Context.MODE_PRIVATE)

                        for (msg in unread) {
                            val eventKey = "chat_msg_${msg.id}"
                            if (prefs.getBoolean(eventKey, false)) continue

                            val isChatActive = ActiveChatSessionManager.isChatActiveWith(
                                subOrderId = msg.subOrderId,
                                senderId = msg.senderId
                            )

                            if (isChatActive) {
                                prefs.edit().putBoolean(eventKey, true).apply()
                                CampusGoNotificationHelper.cancelChatNotifications(this@CampusGoApplication, msg.subOrderId)
                                continue
                            }

                            // Obtener nombre del remitente
                            val senderName = runCatching {
                                val prof = postgrest.from("profiles")
                                    .select { filter { eq("id", msg.senderId) } }
                                    .decodeSingleOrNull<com.example.campusgo.data.repository.ProfileBasicDto>()
                                prof?.fullName?.takeIf { it.isNotBlank() }
                            }.getOrNull() ?: "Mensaje de CampusGO"

                            prefs.edit().putBoolean(eventKey, true).apply()

                            android.util.Log.d("CampusGoChatObserver", "Mostrando notificación sonora para mensaje ${msg.id} de $senderName")
                            CampusGoNotificationHelper.showChatNotification(
                                context = this@CampusGoApplication,
                                notificationId = Math.abs(msg.id.hashCode()),
                                senderName = senderName,
                                message = msg.content,
                                subOrderId = msg.subOrderId
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.d("CampusGoChatObserver", "Aviso en polling de chat: ${e.message}")
                }
                delay(2000L)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("campusgo_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // Límite máximo de seguridad: 250 MB
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // Servir siempre desde el almacenamiento local si ya fue descargada
            .crossfade(true)
            .build()
    }
}

package com.example.campusgo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.campusgo.core.notification.AppNotificationRouter
import com.example.campusgo.core.notification.CampusGoFirebaseMessagingService
import com.example.campusgo.core.notification.CampusGoPushService
import com.example.campusgo.theme.CampusGOTheme
import com.example.campusgo.theme.ThemeManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val auth: Auth by inject()
    private val postgrest: Postgrest by inject()
    private val pendingSubOrderIdState = mutableStateOf<String?>(null)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CampusGoPushService.start(this)
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val isDark = ThemeManager.isDarkModeSync(newBase)
        val configuration = android.content.res.Configuration(newBase.resources.configuration)
        val targetNightMode = if (isDark) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
        configuration.uiMode = targetNightMode or
                (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ThemeManager.init(this)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
            window.isNavigationBarContrastEnforced = false
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // Solicitar permiso de notificaciones para Android 13+ (API 33+)
        checkAndRequestNotificationPermission()

        // Obtener y almacenar token FCM
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.util.Log.d("CampusGoFCM", "FCM Registration Token actual: $token")
                    val prefs = getSharedPreferences(
                        CampusGoFirebaseMessagingService.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE
                    )
                    prefs.edit().putString(
                        CampusGoFirebaseMessagingService.KEY_FCM_TOKEN,
                        token
                    ).apply()

                    lifecycleScope.launch(Dispatchers.IO) {
                        CampusGoFirebaseMessagingService.syncCurrentTokenWithSupabase(
                            context = applicationContext,
                            auth = auth,
                            postgrest = postgrest
                        )
                    }
                } else {
                    android.util.Log.w("CampusGoFCM", "No se pudo obtener el FCM token", task.exception)
                }
            }

        // Iniciar servicio en segundo plano para garantizar alertas sonoras de mensajes de chat y pedidos
        try {
            CampusGoPushService.start(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error iniciando CampusGoPushService", e)
        }

        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Procesar cualquier intent de notificación recibido al abrir
        AppNotificationRouter.onNotificationIntent(intent)

        setContent {
            val pendingRoute = AppNotificationRouter.pendingRoute.collectAsState().value
            val isDarkMode by ThemeManager.isDarkMode.collectAsState()

            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkMode
                    isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            CampusGOTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        pendingRoute = pendingRoute,
                        onClearPendingRoute = { AppNotificationRouter.clearRoute() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppNotificationRouter.onNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestNotificationPermission()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}

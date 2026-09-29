package com.example.campusgo

import android.Manifest
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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.campusgo.core.notification.CampusGoPushService
import com.example.campusgo.theme.CampusGOTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CampusGoPushService.start(this)
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = android.content.res.Configuration(newBase.resources.configuration)
        configuration.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO or
                (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                        com.example.campusgo.core.notification.CampusGoFirebaseMessagingService.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE
                    )
                    prefs.edit().putString(
                        com.example.campusgo.core.notification.CampusGoFirebaseMessagingService.KEY_FCM_TOKEN,
                        token
                    ).apply()
                } else {
                    android.util.Log.w("CampusGoFCM", "No se pudo obtener el FCM token", task.exception)
                }
            }

        // Iniciar el servicio en segundo plano de sincronización de pedidos
        try {
            CampusGoPushService.start(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error iniciando CampusGoPushService", e)
        }

        setContent {
            CampusGOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.White
                ) {
                    MainNavigation()
                }
            }
        }
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

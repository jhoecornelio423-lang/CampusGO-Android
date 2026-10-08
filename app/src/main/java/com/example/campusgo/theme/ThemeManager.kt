package com.example.campusgo.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor reactivo y persistente del tema visual (Modo Claro / Modo Oscuro) en CampusGO.
 * Almacena la preferencia del usuario en SharedPreferences y la expone vía StateFlow
 * para que toda la jerarquía de Compose se actualice instantáneamente.
 */
object ThemeManager {
    private const val PREFS_NAME = "campusgo_theme_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    /**
     * Inicializa el estado del tema al arrancar la aplicación leyendo la preferencia persistida.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getBoolean(KEY_DARK_MODE, false)
        _isDarkMode.value = saved
    }

    /**
     * Lectura síncrona de la preferencia de tema (útil para attachBaseContext).
     */
    fun isDarkModeSync(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    /**
     * Establece el estado de Modo Oscuro, lo persiste en disco y emite la actualización reactiva.
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    /**
     * Alterna entre Modo Claro y Modo Oscuro.
     */
    fun toggleDarkMode(context: Context) {
        setDarkMode(context, !_isDarkMode.value)
    }
}

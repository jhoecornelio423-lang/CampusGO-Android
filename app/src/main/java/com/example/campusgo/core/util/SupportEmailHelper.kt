package com.example.campusgo.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Utilidades para la gestión unificada del correo oficial de soporte técnico
 * y atención al usuario en Campus GO (soporte@kodexti.com).
 */
object SupportEmailHelper {
    const val SUPPORT_EMAIL = "soporte@kodexti.com"
    const val DEFAULT_SUBJECT = "Consulta de Ayuda - Campus GO"
    const val DEFAULT_BODY = "Hola equipo de Soporte Campus GO,\n\nEscribo desde la aplicación para realizar la siguiente consulta:\n\n[Describe aquí tu consulta o inconveniente]\n\n---\nUsuario de Campus GO"

    /**
     * Valida que una dirección de correo corresponda exactamente al buzón oficial de soporte.
     */
    fun isValidSupportEmail(email: String): Boolean {
        return email.trim().equals(SUPPORT_EMAIL, ignoreCase = true)
    }

    /**
     * Construye un Intent configurado específicamente para abrir gestores de correo (ACTION_SENDTO)
     * con la dirección oficial, asunto y plantilla de cuerpo preestablecidos.
     */
    fun createSupportEmailIntent(
        subject: String = DEFAULT_SUBJECT,
        body: String = DEFAULT_BODY,
        recipient: String = SUPPORT_EMAIL
    ): Intent {
        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return Intent.createChooser(mailIntent, "Enviar correo a Soporte ($recipient)")
    }

    /**
     * Copia la dirección oficial de soporte al portapapeles del dispositivo.
     */
    fun copySupportEmailToClipboard(context: Context): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Soporte Campus GO", SUPPORT_EMAIL)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}

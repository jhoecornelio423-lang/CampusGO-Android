package com.example.campusgo.features.chat

/**
 * Gestor en memoria para la sesión activa de chat en pantalla.
 * Permite que CampusGoPushService sepa si el usuario está actualmente conversando
 * con un comprador/vendedor específico para silenciar notificaciones locales redundantes,
 * pero seguir notificando si otros usuarios envían mensajes.
 */
object ActiveChatSessionManager {

    @Volatile
    var activeSubOrderId: String? = null

    @Volatile
    var activeOtherUserId: String? = null

    @Volatile
    var isAppInForeground: Boolean = false

    /**
     * Retorna verdadero si el usuario actualmente tiene abierta y visible en pantalla
     * la conversación para el subpedido o interlocutor indicado.
     * Si la aplicación está en segundo plano o el dispositivo está fuera de la app,
     * SIEMPRE retorna false para que la notificación se muestre con sonido y vibración.
     */
    fun isChatActiveWith(subOrderId: String?, senderId: String?): Boolean {
        if (!isAppInForeground) {
            return false
        }

        val curSub = activeSubOrderId
        val curOther = activeOtherUserId

        if (!subOrderId.isNullOrBlank() && !curSub.isNullOrBlank() && curSub.equals(subOrderId, ignoreCase = true)) {
            return true
        }

        if (!senderId.isNullOrBlank() && !curOther.isNullOrBlank() && curOther.equals(senderId, ignoreCase = true)) {
            return true
        }

        return false
    }

    fun clear() {
        activeSubOrderId = null
        activeOtherUserId = null
    }
}

package com.example.vallego.features.chat

/**
 * Gestor en memoria para la sesión activa de chat en pantalla.
 * Permite que ValleGoPushService sepa si el usuario está actualmente conversando
 * con un comprador/vendedor específico para silenciar notificaciones locales redundantes,
 * pero seguir notificando si otros usuarios envían mensajes.
 */
object ActiveChatSessionManager {

    @Volatile
    var activeSubOrderId: String? = null

    @Volatile
    var activeOtherUserId: String? = null

    /**
     * Retorna verdadero si el usuario actualmente tiene abierta la pantalla de chat
     * para el subpedido o contraparte dada.
     */
    fun isChatActiveWith(subOrderId: String?, senderId: String?): Boolean {
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
}

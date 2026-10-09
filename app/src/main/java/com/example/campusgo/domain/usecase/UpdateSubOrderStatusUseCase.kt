package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.repository.OrderRepository

class UpdateSubOrderStatusUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        subOrderId: String,
        targetStatus: SubOrderStatus,
        currentStatus: SubOrderStatus? = null,
        rejectionReason: String? = null
    ): Result<SubOrder> {
        // Validación de transición de negocio:
        // Si el subpedido ya no está en PENDIENTE y se rechaza, es una cancelación formal.
        val effectiveStatus = if (targetStatus == SubOrderStatus.RECHAZADO && currentStatus != null && currentStatus != SubOrderStatus.PENDIENTE) {
            SubOrderStatus.CANCELADO
        } else {
            targetStatus
        }

        return orderRepository.updateSubOrderStatus(
            subOrderId = subOrderId,
            newStatus = effectiveStatus,
            rejectionReason = rejectionReason
        )
    }
}

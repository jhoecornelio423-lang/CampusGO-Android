package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.repository.ProductRepository

class ManageProductStockUseCase(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, newStock: Int): Result<Unit> {
        if (newStock < 0) {
            return Result.failure(IllegalArgumentException("El stock no puede ser negativo."))
        }
        return productRepository.updateProductStock(productId, newStock)
    }
}

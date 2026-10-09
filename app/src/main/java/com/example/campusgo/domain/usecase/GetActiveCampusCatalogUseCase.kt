package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.model.Category
import com.example.campusgo.domain.model.StoreCatalogGroup
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.domain.repository.ProductRepository

data class CampusCatalogResult(
    val stores: List<StoreCatalogGroup>,
    val categories: List<Category>
)

class GetActiveCampusCatalogUseCase(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(campus: String): Result<CampusCatalogResult> {
        return try {
            val prodsResult = productRepository.getActiveProducts()
            val sellersResult = productRepository.getSellerProfiles()
            val catsResult = productRepository.getCategories()

            val products = prodsResult.getOrDefault(emptyList())
            val sellers = sellersResult.getOrDefault(emptyList()).associateBy { it.id }
            val categoriesList = catsResult.getOrDefault(emptyList())

            val groupedStores = if (products.isNotEmpty()) {
                products.groupBy { it.sellerId }.mapNotNull { (sellerId, sellerProds) ->
                    val seller = sellers[sellerId] ?: return@mapNotNull null

                    // Solo mostrar el puesto si existe y su rol es EMPRENDEDOR
                    if (seller.role != UserRole.EMPRENDEDOR) {
                        return@mapNotNull null
                    }
                    // Si el vendedor tiene el puesto cerrado físicamente y no acepta pedidos, se oculta
                    if (!seller.acceptingOrders && seller.businessStatus.equals("CERRADO", ignoreCase = true)) {
                        return@mapNotNull null
                    }

                    val bName = seller.businessName?.trim().orEmpty()
                    val fName = seller.fullName.trim()
                    val storeTitle = when {
                        bName.isNotBlank() && fName.isNotBlank() && !bName.equals(fName, ignoreCase = true) -> "$bName - $fName"
                        bName.isNotBlank() -> bName
                        fName.isNotBlank() -> fName
                        else -> "Emprendimiento CampusGO"
                    }
                    val loc = seller.businessLocation?.trim()?.takeIf { it.isNotBlank() } ?: "Campus $campus"

                    StoreCatalogGroup(
                        sellerId = sellerId,
                        sellerName = storeTitle,
                        location = loc,
                        bannerUrl = seller.bannerUrl,
                        avatarUrl = seller.avatarUrl,
                        businessStatus = seller.businessStatus,
                        openTime = seller.openTime,
                        closeTime = seller.closeTime,
                        description = seller.businessDescription,
                        acceptingOrders = seller.acceptingOrders,
                        products = sellerProds,
                        sellerProfile = seller,
                        businessCategory = seller.businessCategory,
                        phone = seller.phone,
                        ratingAverage = seller.ratingAverage,
                        supportedMeetingPoints = seller.supportedMeetingPoints,
                        supportedPaymentMethods = seller.effectivePaymentMethods
                    )
                }
            } else {
                emptyList()
            }

            Result.success(CampusCatalogResult(stores = groupedStores, categories = categoriesList))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

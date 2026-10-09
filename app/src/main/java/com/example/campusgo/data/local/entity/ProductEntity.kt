package com.example.campusgo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.campusgo.domain.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val categoryId: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val pickupLocation: String? = null,
    val createdAt: String? = null
) {
    fun toDomain(): Product = Product(
        id = id,
        sellerId = sellerId,
        categoryId = categoryId,
        name = name,
        description = description,
        price = price,
        stock = stock,
        imageUrl = imageUrl,
        isActive = isActive,
        pickupLocation = pickupLocation,
        createdAt = createdAt
    )
}

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    sellerId = sellerId,
    categoryId = categoryId,
    name = name,
    description = description,
    price = price,
    stock = stock,
    imageUrl = imageUrl,
    isActive = isActive,
    pickupLocation = pickupLocation,
    createdAt = createdAt
)

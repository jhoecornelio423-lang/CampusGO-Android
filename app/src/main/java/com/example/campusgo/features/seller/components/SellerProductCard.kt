package com.example.campusgo.features.seller.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusgo.R
import com.example.campusgo.domain.model.Product
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.CampusGoProductImage
import com.example.campusgo.ui.components.resolveCategoryVisualTheme

@Composable
fun ProductCard(
    product: Product,
    categoryName: String?,
    onToggleActive: (Boolean) -> Unit,
    onEditStock: () -> Unit,
    onEditProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isDark = LocalDarkTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto con fallback elegante de categoría
            CampusGoProductImage(
                imageUrl = product.imageUrl,
                categoryName = categoryName,
                productName = product.name,
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onEditProduct)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditProduct)
            ) {
                if (!categoryName.isNullOrBlank()) {
                    val catTheme = resolveCategoryVisualTheme(categoryName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = catTheme.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF003366),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        onClick = onEditStock,
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOutOfStock) (if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFFEBEE)) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Stock: ${product.stock}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit_product_custom),
                                contentDescription = "Editar Stock",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditProduct,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit_product_custom),
                        contentDescription = "Editar producto completo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val statusText = when {
                    isOutOfStock -> "Sin stock"
                    product.isActive -> "Activo"
                    else -> "Pausado"
                }
                val statusColor = when {
                    isOutOfStock -> MaterialTheme.colorScheme.error
                    product.isActive -> if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                    else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = product.isActive && !isOutOfStock,
                    onCheckedChange = { desired ->
                        if (isOutOfStock) {
                            onToggleActive(false)
                        } else {
                            onToggleActive(desired)
                        }
                    },
                    enabled = !isOutOfStock
                )
            }
        }
    }
}

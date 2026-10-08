package com.example.campusgo.features.buyer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.campusgo.theme.LocalDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.Product
import com.example.campusgo.features.buyer.StoreCatalogGroup
import com.example.campusgo.ui.components.CampusGoProductImage

@Composable
fun BuyerProductGridCard(
    product: Product,
    store: StoreCatalogGroup,
    categoryName: String?,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onStoreClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    showStoreTag: Boolean = true,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val isStoreAvail = store.acceptingOrders &&
            !store.businessStatus.equals("PAUSADO", ignoreCase = true) &&
            !store.businessStatus.equals("CERRADO", ignoreCase = true)
    val isAvailable = isStoreAvail && product.stock > 0

    Surface(
        onClick = { if (isAvailable) onClick() },
        enabled = isAvailable,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else if (isAvailable) Color(0xFFEEF2F6) else Color(0xFFE2E8F0)),
        shadowElevation = if (isAvailable) 1.5.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.55f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                CampusGoProductImage(
                    imageUrl = product.imageUrl,
                    categoryName = categoryName,
                    productName = product.name,
                    emojiSize = 36,
                    modifier = Modifier.fillMaxSize()
                )

                // Badge opcional (ej: "3x pedidos" o "Top pedido")
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Overlay si no está disponible
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x990F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xEEBE123C),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "No disponible",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Botón de Favorito en la esquina superior derecha
                if (onToggleFavorite != null) {
                    Surface(
                        onClick = onToggleFavorite,
                        shape = CircleShape,
                        color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.90f),
                        border = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (isFavorite) Color(0xFFEF4444) else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Información del Producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Etiqueta del puesto (oculta si ya se muestra la tienda arriba)
                if (showStoreTag) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .then(
                                if (onStoreClick != null) Modifier.clickable { onStoreClick() }
                                else Modifier
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store_custom),
                            contentDescription = null,
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = store.sellerName,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Nombre del producto
                Text(
                    text = product.name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                // Fila de Precio y Botón rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A)
                    )

                    Surface(
                        onClick = {
                            if (isAvailable) onQuickAdd()
                        },
                        enabled = isAvailable,
                        shape = CircleShape,
                        color = if (isAvailable) Color(0xFF00A884) else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE2E8F0)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isAvailable) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_to_cart_custom),
                                    contentDescription = "Agregar al carrito",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "No disponible",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

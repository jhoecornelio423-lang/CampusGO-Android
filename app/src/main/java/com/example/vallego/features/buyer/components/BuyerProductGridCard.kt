package com.example.vallego.features.buyer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.Product
import com.example.vallego.features.buyer.StoreCatalogGroup
import com.example.vallego.ui.components.ValleGoProductImage

@Composable
fun BuyerProductGridCard(
    product: Product,
    store: StoreCatalogGroup,
    categoryName: String?,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onStoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isStoreAvail = store.acceptingOrders &&
            !store.businessStatus.equals("PAUSADO", ignoreCase = true) &&
            !store.businessStatus.equals("CERRADO", ignoreCase = true)
    val isAvailable = isStoreAvail && product.stock > 0

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
        shadowElevation = 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                ValleGoProductImage(
                    imageUrl = product.imageUrl,
                    categoryName = categoryName,
                    productName = product.name,
                    emojiSize = 42,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay si no está disponible
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xDD000000),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.stock <= 0) "Agotado" else "Puesto Cerrado",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Información del Producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Etiqueta del puesto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = store.sellerName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A884),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Nombre del producto
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16324F),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Fila de Precio y Botón rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        onClick = {
                            if (isAvailable) onQuickAdd() else onClick()
                        },
                        shape = CircleShape,
                        color = if (isAvailable) Color(0xFF00A884) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isAvailable) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_to_cart_custom),
                                    contentDescription = "Agregar al carrito",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "No disponible",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

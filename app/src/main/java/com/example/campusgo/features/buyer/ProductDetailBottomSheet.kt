package com.example.campusgo.features.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.campusgo.theme.LocalDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.campusgo.R
import com.example.campusgo.domain.model.Product
import com.example.campusgo.ui.components.CampusGoProductImage
import com.example.campusgo.ui.components.resolveCategoryVisualTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailBottomSheet(
    product: Product,
    storeName: String,
    categoryName: String?,
    isStoreAvailable: Boolean = true,
    storeStatus: String = "ABIERTO",
    onDismiss: () -> Unit,
    onStoreClick: (() -> Unit)? = null,
    onAddToCart: (product: Product, quantity: Int, specialInstructions: String?) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableIntStateOf(if (product.stock > 0) 1 else 0) }
    var specialInstructions by remember { mutableStateOf("") }
    val maxStock = remember(product.stock) { maxOf(0, product.stock) }

    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Imagen Hero Grande del Producto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    CampusGoProductImage(
                        imageUrl = product.imageUrl,
                        categoryName = categoryName,
                        productName = product.name,
                        emojiSize = 52,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Tienda y Categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (onStoreClick != null) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onStoreClick() }
                                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE6F7F3))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                } else Modifier
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store_custom),
                            contentDescription = null,
                            tint = if (onStoreClick != null) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (onStoreClick != null) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onStoreClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Ver Puesto",
                                tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (!categoryName.isNullOrBlank()) {
                        val catTheme = resolveCategoryVisualTheme(categoryName)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = catTheme.iconResId),
                                    contentDescription = null,
                                    tint = catTheme.contentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Título y Precio
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "S/ %.2f".format(product.price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)
                        )
                        val isLowStock = product.stock <= 3
                        Surface(
                            color = if (isDark) {
                                if (isLowStock) Color(0xFF450A0A) else Color(0xFF064E3B)
                            } else {
                                if (isLowStock) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isLowStock) "¡Solo quedan ${product.stock}!" else "Stock disponible: ${product.stock}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) {
                                    if (isLowStock) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                } else {
                                    if (isLowStock) Color(0xFFC8102E) else Color(0xFF2E7D32)
                                }
                            )
                        }
                    }
                }

                // Descripción
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // Advertencia si el puesto está cerrado o en pausa
                if (!isStoreAvailable) {
                    Surface(
                        color = if (isDark) Color(0xFF431407) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning_custom),
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFB923C) else Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (storeStatus.equals("PAUSADO", ignoreCase = true))
                                    "Este puesto se encuentra en pausa temporal y no está aceptando pedidos por el momento."
                                else
                                    "Este puesto se encuentra cerrado actualmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFFDBA74) else Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Notas e instrucciones especiales para el vendedor
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Instrucciones especiales para el puesto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "¿Deseas sin cremas, calentito, con cubiertos descartables? Indícaselo al emprendedor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        placeholder = { Text("Ej. Sin mayonesa, por favor / Salsa tártara aparte") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        enabled = isStoreAvailable && product.stock > 0
                    )
                }

                HorizontalDivider()

                // Selector de cantidad interactivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cantidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = isStoreAvailable && product.stock > 0 && quantity > 1,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar")
                        }

                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (product.stock > 0) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)) else Color(0xFF9E9E9E)
                        )

                        FilledTonalIconButton(
                            onClick = { if (quantity < maxStock) quantity++ },
                            enabled = isStoreAvailable && product.stock > 0 && quantity < maxStock,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Sumar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Sticky de Agregar al Carrito (Deshabilitado si está en Pausa o Cerrado)
            val subtotal = product.price * quantity
            val canAdd = isStoreAvailable && product.stock > 0 && quantity > 0
            val buttonLabel = when {
                !isStoreAvailable && storeStatus.equals("PAUSADO", ignoreCase = true) -> "Puesto en Pausa"
                !isStoreAvailable -> "Puesto Cerrado"
                product.stock <= 0 -> "Producto Agotado"
                else -> "Agregar al carrito"
            }

            Button(
                onClick = {
                    val instructions = specialInstructions.trim().takeIf { it.isNotBlank() }
                    onAddToCart(product, quantity, instructions)
                    onDismiss()
                },
                enabled = canAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884),
                    disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEEEEEE),
                    disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF9E9E9E)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canAdd) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add_to_cart_custom),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    if (canAdd) {
                        Text(
                            text = "S/ %.2f".format(subtotal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

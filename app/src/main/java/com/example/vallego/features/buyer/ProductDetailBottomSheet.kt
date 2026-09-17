package com.example.vallego.features.buyer

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import com.example.vallego.domain.model.Product
import com.example.vallego.ui.components.ValleGoProductImage

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
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableIntStateOf(1) }
    var specialInstructions by remember { mutableStateOf("") }
    val maxStock = remember(product.stock) { maxOf(1, product.stock) }

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
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Imagen Hero Grande del Producto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ValleGoProductImage(
                        imageUrl = product.imageUrl,
                        categoryName = categoryName,
                        productName = product.name,
                        emojiSize = 56,
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
                                        .background(Color(0xFFE6F7F3))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                } else Modifier
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store_modern),
                            contentDescription = null,
                            tint = if (onStoreClick != null) Color(0xFF00A884) else Color(0xFF16324F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (onStoreClick != null) Color(0xFF00A884) else Color(0xFF16324F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onStoreClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Ver Puesto",
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (!categoryName.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = categoryName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            color = Color(0xFF00A884)
                        )
                        Surface(
                            color = if (product.stock <= 3) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (product.stock <= 3) "¡Solo quedan ${product.stock}!" else "Stock disponible: ${product.stock}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (product.stock <= 3) Color(0xFFC8102E) else Color(0xFF2E7D32)
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
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (storeStatus.equals("PAUSADO", ignoreCase = true))
                                    "Este puesto se encuentra en pausa temporal y no está aceptando pedidos por el momento."
                                else
                                    "Este puesto se encuentra cerrado actualmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100),
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
                        color = Color(0xFF16324F)
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
                        enabled = isStoreAvailable
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
                        color = Color(0xFF16324F)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = isStoreAvailable && quantity > 1,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar")
                        }

                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884)
                        )

                        FilledTonalIconButton(
                            onClick = { if (quantity < maxStock) quantity++ },
                            enabled = isStoreAvailable && quantity < maxStock,
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
            val canAdd = isStoreAvailable && product.stock > 0
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
                    containerColor = Color(0xFF00A884),
                    disabledContainerColor = Color(0xFFEEEEEE),
                    disabledContentColor = Color(0xFF9E9E9E)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canAdd) Icons.Default.ShoppingBag else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
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

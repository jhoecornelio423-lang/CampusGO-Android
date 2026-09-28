package com.example.campusgo.features.buyer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.Category
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.Product
import com.example.campusgo.features.buyer.StoreCatalogGroup
import com.example.campusgo.ui.components.ValleGoBusinessAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerFavoritesView(
    favoriteProductIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    allProducts: List<Pair<Product, StoreCatalogGroup>>,
    categoriesList: List<Category>,
    storesList: List<StoreCatalogGroup>,
    buyerOrders: List<Order>,
    onProductClick: (Product, StoreCatalogGroup) -> Unit,
    onAddToCart: (Product, StoreCatalogGroup) -> Unit,
    onStoreClick: (StoreCatalogGroup) -> Unit,
    onExploreCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Productos Favoritos guardados
    val favoriteItems = remember(allProducts, favoriteProductIds) {
        allProducts.filter { (product, _) ->
            favoriteProductIds.contains(product.id)
        }
    }

    // 2. Cálculo de "Lo más pedido por mí" según el historial de compras del usuario
    val mostOrderedItems = remember(buyerOrders, allProducts) {
        val countMap = mutableMapOf<String, Int>()
        buyerOrders.forEach { order ->
            order.subOrders.forEach { sub ->
                sub.items.forEach { item ->
                    countMap[item.productId] = (countMap[item.productId] ?: 0) + item.quantity
                }
            }
        }
        allProducts
            .filter { (prod, _) -> (countMap[prod.id] ?: 0) > 0 }
            .map { (prod, store) -> Triple(prod, store, countMap[prod.id] ?: 1) }
            .sortedByDescending { it.third }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        spotColor = Color(0x1F16324F),
                        ambientColor = Color(0x2816324F),
                        clip = false
                    ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Mis Favoritos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "${favoriteItems.size} guardados • ${storesList.size} puestos del campus",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(
                    bottom = 96.dp + WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ==========================================
            // SECCIÓN 1: PRODUCTOS FAVORITOS GUARDADOS
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PRODUCTOS FAVORITOS (${favoriteItems.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (favoriteItems.isNotEmpty()) {
                        Text(
                            text = "Explorar más",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884),
                            modifier = Modifier.clickable { onExploreCatalog() }
                        )
                    }
                }

                if (favoriteItems.isEmpty()) {
                    // Tarjeta compacta informativa cuando aún no hay productos en favoritos
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guarda tus productos preferidos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF16324F)
                                )
                                Text(
                                    text = "Toca el corazón ❤️ en cualquier producto del menú para encontrarlo aquí al instante.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    // Cuadrícula 2 columnas de favoritos
                    favoriteItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val (prod1, store1) = rowItems[0]
                            val cat1 = categoriesList.find { it.id == prod1.categoryId }?.name
                            Box(modifier = Modifier.weight(1f)) {
                                BuyerProductGridCard(
                                    product = prod1,
                                    store = store1,
                                    categoryName = cat1,
                                    onClick = { onProductClick(prod1, store1) },
                                    onQuickAdd = { onAddToCart(prod1, store1) },
                                    isFavorite = true,
                                    onToggleFavorite = { onToggleFavorite(prod1.id) },
                                    onStoreClick = { onStoreClick(store1) }
                                )
                            }

                            if (rowItems.size > 1) {
                                val (prod2, store2) = rowItems[1]
                                val cat2 = categoriesList.find { it.id == prod2.categoryId }?.name
                                Box(modifier = Modifier.weight(1f)) {
                                    BuyerProductGridCard(
                                        product = prod2,
                                        store = store2,
                                        categoryName = cat2,
                                        onClick = { onProductClick(prod2, store2) },
                                        onQuickAdd = { onAddToCart(prod2, store2) },
                                        isFavorite = true,
                                        onToggleFavorite = { onToggleFavorite(prod2.id) },
                                        onStoreClick = { onStoreClick(store2) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 2: PUESTOS DEL CAMPUS (EN EL MEDIO)
            // ==========================================
            if (storesList.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_store_custom),
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PUESTOS DEL CAMPUS (${storesList.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "Toca para ver puesto",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Carrusel horizontal de Puestos de Campus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        storesList.forEach { store ->
                            val isOpen = store.acceptingOrders &&
                                    !store.businessStatus.equals("CERRADO", ignoreCase = true) &&
                                    !store.businessStatus.equals("PAUSADO", ignoreCase = true)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onStoreClick(store) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        border = BorderStroke(
                                            width = 2.dp,
                                            color = if (isOpen) Color(0xFF00A884) else Color(0xFFCBD5E1)
                                        ),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        ValleGoBusinessAvatar(
                                            avatarUrl = store.avatarUrl,
                                            storeName = store.sellerName,
                                            size = 56.dp
                                        )
                                    }

                                    // Indicador de disponibilidad
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    if (isOpen) Color(0xFF16A34A)
                                                    else if (store.businessStatus.equals("PAUSADO", ignoreCase = true)) Color(0xFFEAB308)
                                                    else Color(0xFF94A3B8)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = store.sellerName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = if (isOpen) "Abierto"
                                    else if (store.businessStatus.equals("PAUSADO", ignoreCase = true)) "En Pausa"
                                    else "Cerrado",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOpen) Color(0xFF16A34A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            // ==========================================
            // SECCIÓN 3: LO MÁS PEDIDO POR MÍ (DEBAJO)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LO MÁS PEDIDO POR MÍ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (mostOrderedItems.isNotEmpty()) {
                        Text(
                            text = "${mostOrderedItems.size} frecuente${if (mostOrderedItems.size > 1) "s" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                if (mostOrderedItems.isEmpty()) {
                    // Tarjeta cuando aún no hay historial de compras
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF0F9FF),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tus compras frecuentes aquí",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF16324F)
                                    )
                                    Text(
                                        text = "Cuando realices pedidos en los puestos del campus, aquí verás tus productos favoritos repetidos para volver a pedirlos con un solo toque.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Button(
                                onClick = onExploreCatalog,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Explorar Menú de Campus", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Productos más pedidos con insignia de cantidad pedida
                    mostOrderedItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val (prod1, store1, count1) = rowItems[0]
                            val cat1 = categoriesList.find { it.id == prod1.categoryId }?.name
                            val isFav1 = favoriteProductIds.contains(prod1.id)
                            Box(modifier = Modifier.weight(1f)) {
                                BuyerProductGridCard(
                                    product = prod1,
                                    store = store1,
                                    categoryName = cat1,
                                    onClick = { onProductClick(prod1, store1) },
                                    onQuickAdd = { onAddToCart(prod1, store1) },
                                    isFavorite = isFav1,
                                    onToggleFavorite = { onToggleFavorite(prod1.id) },
                                    onStoreClick = { onStoreClick(store1) },
                                    badgeText = "Pedido ${count1}x"
                                )
                            }

                            if (rowItems.size > 1) {
                                val (prod2, store2, count2) = rowItems[1]
                                val cat2 = categoriesList.find { it.id == prod2.categoryId }?.name
                                val isFav2 = favoriteProductIds.contains(prod2.id)
                                Box(modifier = Modifier.weight(1f)) {
                                    BuyerProductGridCard(
                                        product = prod2,
                                        store = store2,
                                        categoryName = cat2,
                                        onClick = { onProductClick(prod2, store2) },
                                        onQuickAdd = { onAddToCart(prod2, store2) },
                                        isFavorite = isFav2,
                                        onToggleFavorite = { onToggleFavorite(prod2.id) },
                                        onStoreClick = { onStoreClick(store2) },
                                        badgeText = "Pedido ${count2}x"
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

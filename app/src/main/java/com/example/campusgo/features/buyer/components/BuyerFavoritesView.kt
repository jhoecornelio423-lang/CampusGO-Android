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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import com.example.campusgo.ui.components.CampusGoBusinessAvatar

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
    val isDark = LocalDarkTheme.current
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val handleToggleWithUndo: (String, String) -> Unit = { prodId, prodName ->
        val wasFav = favoriteProductIds.contains(prodId)
        onToggleFavorite(prodId)
        if (wasFav) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "\"$prodName\" eliminado de favoritos",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onToggleFavorite(prodId)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 0.dp else 4.dp,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.dp
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Mis Favoritos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${favoriteItems.size} guardados • ${storesList.size} puestos del campus",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (favoriteItems.isNotEmpty()) {
                        Text(
                            text = "Explorar más",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onExploreCatalog() }
                        )
                    }
                }

                if (favoriteItems.isEmpty()) {
                    // Tarjeta compacta informativa cuando aún no hay productos en favoritos
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isDark) 0.dp else 1.dp,
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
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guarda tus productos preferidos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toca el corazón ❤️ en cualquier producto del menú para encontrarlo aquí al instante.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    onToggleFavorite = { handleToggleWithUndo(prod1.id, prod1.name) },
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
                                        onToggleFavorite = { handleToggleWithUndo(prod2.id, prod2.name) },
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PUESTOS DEL CAMPUS (${storesList.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "Toca para ver puesto",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            color = if (isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shadowElevation = if (isDark) 0.dp else 2.dp,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        CampusGoBusinessAvatar(
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
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(2.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    if (isOpen) MaterialTheme.extendedColors.success
                                                    else if (store.businessStatus.equals("PAUSADO", ignoreCase = true)) MaterialTheme.colorScheme.tertiary
                                                    else MaterialTheme.colorScheme.outline
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = store.sellerName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                    color = if (isOpen) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LO MÁS PEDIDO POR MÍ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (mostOrderedItems.isNotEmpty()) {
                        Text(
                            text = "${mostOrderedItems.size} frecuente${if (mostOrderedItems.size > 1) "s" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (mostOrderedItems.isEmpty()) {
                    // Tarjeta cuando aún no hay historial de compras
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isDark) 0.dp else 1.dp,
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
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tus compras frecuentes aquí",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Cuando realices pedidos en los puestos del campus, aquí verás tus productos favoritos repetidos para volver a pedirlos con un solo toque.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Button(
                                onClick = onExploreCatalog,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
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
                                    onToggleFavorite = { handleToggleWithUndo(prod1.id, prod1.name) },
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
                                        onToggleFavorite = { handleToggleWithUndo(prod2.id, prod2.name) },
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

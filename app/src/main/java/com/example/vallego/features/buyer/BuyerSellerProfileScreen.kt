package com.example.vallego.features.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.ui.components.PaymentMethodLogoByName
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.Product
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerSellerProfileScreen(
    store: StoreCatalogGroup,
    meetingPoints: List<CampusMeetingPoint> = emptyList(),
    cartCalculation: CartCalculationResult,
    onNavigateBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sellerSupportedPoints = remember(store.supportedMeetingPoints, meetingPoints) {
        val supportedSet = store.supportedMeetingPoints.filter { it.isNotBlank() }.toSet()
        meetingPoints.filter { it.isActive && supportedSet.contains(it.id) }
    }

    val isOpen = store.acceptingOrders && !store.businessStatus.equals("CERRADO", ignoreCase = true)
    val isSaturated = store.businessStatus.equals("SATURADO", ignoreCase = true)
    val isPaused = store.businessStatus.equals("PAUSADO", ignoreCase = true)

    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoTitle by remember { mutableStateOf("") }
    var enlargedPhotoRole by remember { mutableStateOf("") }
    var isEnlargedBanner by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = store.sellerName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF003366)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (cartCalculation.totalItemCount > 0) {
                Surface(
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${cartCalculation.totalItemCount} ítems agregados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: S/ %.2f".format(cartCalculation.grandTotal),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF003366)
                            )
                        }
                        Button(
                            onClick = onNavigateToCart,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cart_custom),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver Carrito", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Cabecera Hero: Portada y Logotipo superpuesto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        ValleGoBusinessBanner(
                            bannerUrl = store.bannerUrl,
                            storeName = store.sellerName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    enlargedPhotoUrl = store.bannerUrl
                                    enlargedPhotoTitle = store.sellerName
                                    enlargedPhotoRole = "Banner del Puesto"
                                    isEnlargedBanner = true
                                    showEnlargedPhoto = true
                                },
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )

                        // Avatar superpuesto estilo Facebook
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp)
                                .offset(y = 38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    enlargedPhotoUrl = store.avatarUrl
                                    enlargedPhotoTitle = store.sellerName
                                    enlargedPhotoRole = "Puesto • " + (store.businessCategory ?: "Campus")
                                    isEnlargedBanner = false
                                    showEnlargedPhoto = true
                                }
                        ) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(3.5.dp, Color.White),
                                shadowElevation = 5.dp
                            ) {
                                ValleGoBusinessAvatar(
                                    avatarUrl = store.avatarUrl,
                                    storeName = store.sellerName,
                                    size = 76.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Datos del Puesto
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = store.sellerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF003366),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StoreStatusBadge(status = store.businessStatus, acceptingOrders = store.acceptingOrders)
                        }

                        // Reputación, Categoría y Emprendedor Autorizado
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "%.1f".format(store.ratingAverage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }

                            val category = store.businessCategory?.takeIf { it.isNotBlank() } ?: "Emprendimiento UCV"
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFFE6F6F3),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF0D5C4C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Emprendedor Autorizado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0D5C4C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Ubicación y Horario en bloque claro y estructurado
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UBICACIÓN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = store.location ?: "Campus Universitario",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!store.openTime.isNullOrBlank() && !store.closeTime.isNullOrBlank()) {
                                HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ATENCIÓN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "${store.openTime} - ${store.closeTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (store.supportedPaymentMethods.isNotEmpty()) {
                                HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MEDIOS DE PAGO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        store.supportedPaymentMethods.forEach { method ->
                                            val (pillBg, pillTint, pillText) = when (method) {
                                                "YAPE" -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                                                "PLIN" -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                                                "EFECTIVO" -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                                                else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), method)
                                            }
                                            Surface(
                                                color = pillBg,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    PaymentMethodLogoByName(name = method, size = 11.dp)
                                                    Text(
                                                        text = pillText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pillTint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Alerta si el puesto está Saturado o Cerrado
                        if (isSaturated) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                 Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info_custom),
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Alta afluencia de pedidos. Tu pedido podría tardar unos minutos adicionales en ser preparado.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        } else if (!isOpen || isPaused) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPaused) "Este puesto está pausado temporalmente. Podrás explorar sus productos pero no realizar pedidos por ahora."
                                        else "Este puesto se encuentra cerrado en este momento. Revisa sus horarios habituales.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sección: Acerca del Puesto (Descripción)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store_custom),
                            contentDescription = null,
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Acerca del Puesto",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF003366)
                        )
                    }
                    Text(
                        text = store.description?.takeIf { it.isNotBlank() }
                            ?: "Emprendimiento estudiantil de Campus Go. Ofrece productos preparados y seleccionados especialmente para los estudiantes y docentes del campus universitario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )
                }
            }

            // 3. Sección: Puntos de Encuentro Autorizados por este Vendedor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location_custom),
                            contentDescription = null,
                            tint = Color(0xFFC8102E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Puntos de Entrega en Campus",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF003366)
                        )
                    }

                    Text(
                        text = "Lugares autorizados donde este vendedor realiza entregas dentro o en los accesos del campus:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (sellerSupportedPoints.isEmpty()) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "El vendedor coordina la entrega en los accesos principales del campus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        sellerSupportedPoints.forEach { point ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = point.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1E293B)
                                        )
                                        val details = listOfNotNull(point.pavilion, point.description).filter { it.isNotBlank() }.joinToString(" • ")
                                        if (details.isNotBlank()) {
                                            Text(
                                                text = details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isExterior = point.zoneType.equals("EXTERIOR", ignoreCase = true)
                                    Surface(
                                        color = if (isExterior) Color(0xFFE6F6F3) else Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isExterior) "Exterior" else "Interior",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isExterior) Color(0xFF16A085) else Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Sección: Catálogo de Productos del Puesto
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRODUCTOS DEL PUESTO (${store.products.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                }

                if (store.products.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Este puesto no tiene productos disponibles en este momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val productPairs = store.products.chunked(2)
                    productPairs.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val prod1 = pair[0]
                            SellerProductItemCard(
                                product = prod1,
                                isStoreAvailable = isOpen,
                                onClick = { onProductClick(prod1) },
                                onAddToCart = { onAddToCart(prod1) },
                                modifier = Modifier.weight(1f)
                            )

                            if (pair.size > 1) {
                                val prod2 = pair[1]
                                SellerProductItemCard(
                                    product = prod2,
                                    isStoreAvailable = isOpen,
                                    onClick = { onProductClick(prod2) },
                                    onAddToCart = { onAddToCart(prod2) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = enlargedPhotoUrl,
            name = enlargedPhotoTitle,
            roleDescription = enlargedPhotoRole,
            isBanner = isEnlargedBanner,
            onDismiss = { showEnlargedPhoto = false }
        )
    }
}

@Composable
private fun SellerProductItemCard(
    product: Product,
    isStoreAvailable: Boolean,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = isStoreAvailable && product.stock > 0

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
        shadowElevation = 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                ValleGoProductImage(
                    imageUrl = product.imageUrl,
                    categoryName = null,
                    productName = product.name,
                    emojiSize = 42,
                    modifier = Modifier.fillMaxSize()
                )

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
                                text = if (product.stock <= 0) "Agotado" else "No disponible",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFF003366)
                    )

                    FilledTonalButton(
                        onClick = onAddToCart,
                        enabled = isAvailable,
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE6F7F3),
                            contentColor = Color(0xFF00A884),
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_to_cart_custom),
                            contentDescription = "Agregar al Carrito",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

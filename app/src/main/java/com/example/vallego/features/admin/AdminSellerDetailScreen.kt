package com.example.vallego.features.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.ProfileWarning
import com.example.vallego.domain.model.SellerDashboardStats
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.PaymentMethodLogoByName
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.StrikeBadge
import com.example.vallego.ui.components.StrikeManagementCard
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage

/**
 * Pantalla completa dedicada para el seguimiento y auditoría detallada de un puesto por el Administrador.
 * No es un diálogo ni ventana emergente: ocupa la pantalla completa con navegación directa de retorno.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminSellerDetailScreen(
    seller: UserProfile,
    products: List<Product>,
    isLoadingProducts: Boolean,
    sellerStats: SellerDashboardStats?,
    onBack: () -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit,
    onIssueWarning: ((UserProfile) -> Unit)? = null,
    warnings: List<ProfileWarning> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSuspended = seller.role == UserRole.SUSPENDED

    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoTitle by remember { mutableStateOf("") }
    var enlargedPhotoRole by remember { mutableStateOf("") }
    var isEnlargedBanner by remember { mutableStateOf(false) }

    BackHandler(enabled = showEnlargedPhoto) {
        showEnlargedPhoto = false
    }

    BackHandler(enabled = !showEnlargedPhoto) {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Seguimiento de Puesto",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = seller.displayStoreName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al listado",
                            tint = Color(0xFF003366)
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        StrikeBadge(strikes = warnings.size, showAutoSuspensionLabel = true)
                        StoreStatusBadge(
                            status = seller.businessStatus,
                            acceptingOrders = seller.acceptingOrders
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO BANNER Y AVATAR
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Banner principal
                    ValleGoBusinessBanner(
                        bannerUrl = seller.bannerUrl,
                        storeName = seller.displayStoreName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable {
                                enlargedPhotoUrl = seller.bannerUrl?.takeIf { it.isNotBlank() }
                                enlargedPhotoTitle = seller.displayStoreName
                                enlargedPhotoRole = "Banner del Puesto"
                                isEnlargedBanner = true
                                showEnlargedPhoto = true
                            },
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )

                    // Avatar flotante sobre el borde inferior del banner
                    Box(
                        modifier = Modifier
                            .padding(top = 115.dp, start = 20.dp)
                            .clip(CircleShape)
                            .clickable {
                                enlargedPhotoUrl = seller.avatarUrl?.takeIf { it.isNotBlank() }
                                enlargedPhotoTitle = seller.displayStoreName
                                enlargedPhotoRole = "Emprendedor Universitario • Campus ${seller.campus}"
                                isEnlargedBanner = false
                                showEnlargedPhoto = true
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(3.dp, Color.White),
                            shadowElevation = 4.dp
                        ) {
                            ValleGoBusinessAvatar(
                                avatarUrl = seller.avatarUrl,
                                storeName = seller.displayStoreName,
                                size = 80.dp
                            )
                        }
                    }
                }
            }

            // 2. ENCABEZADO DE IDENTIFICACIÓN DE LA TIENDA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = seller.displayStoreName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSuspended) Color(0xFFC8102E) else Color(0xFF003366)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Titular: ${seller.fullName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Calificación
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "★",
                                    color = Color(0xFFFFA000),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(seller.ratingAverage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips de Categoría y Estado
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        seller.businessCategory?.takeIf { it.isNotBlank() }?.let { cat ->
                            Surface(
                                color = Color(0xFF00A884).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Rubro: $cat",
                                    color = Color(0xFF00897B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (isSuspended) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isSuspended) "CUENTA SUSPENDIDA" else if (seller.acceptingOrders) "RECIBIENDO PEDIDOS" else "PAUSADO / CERRADO",
                                color = if (isSuspended) Color(0xFFC8102E) else Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. TARJETA DE ALERTA DE SUSPENSIÓN (Si está suspendido)
            if (isSuspended) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F2)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC8102E),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Puesto Suspendido por Administración",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFC8102E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = seller.suspensionReason?.takeIf { it.isNotBlank() }
                                        ?: "El puesto no cumple con normativas vigentes o tiene quejas reiteradas.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }
            }

            // 4. TELEMETRÍA Y RENDIMIENTO HISTÓRICO DEL PUESTO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rendimiento y Ventas del Puesto",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF003366)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailMetricMiniCard(
                            title = "Ventas Totales",
                            value = "S/ %.2f".format(sellerStats?.totalEarnings ?: 0.0),
                            color = Color(0xFF003366),
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetricMiniCard(
                            title = "Pedidos Entregados",
                            value = "${sellerStats?.completedCount ?: 0}",
                            color = Color(0xFF00A884),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailMetricMiniCard(
                            title = "Ticket Promedio",
                            value = "S/ %.2f".format(sellerStats?.averageTicket ?: 0.0),
                            color = Color(0xFF1976D2),
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetricMiniCard(
                            title = "Cancelados / Fallidos",
                            value = "${sellerStats?.cancelledCount ?: 0}",
                            color = if ((sellerStats?.cancelledCount ?: 0) > 0) Color(0xFFE65100) else Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. DATOS COMPLETOS DE LA TIENDA Y CONTACTO
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Información del Emprendedor y Punto de Venta",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF003366)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Titular y Código
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF003366),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Titular Responsable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = seller.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Código UCV
                        if (!seller.studentCode.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFF003366),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Código de Estudiante", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = seller.studentCode, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Ubicación física de entrega en campus
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = Color(0xFF003366),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Ubicación / Pabellón", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = seller.businessLocation?.takeIf { it.isNotBlank() } ?: "Campus ${seller.campus}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Horario de atención
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF003366),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Horario de Atención", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val scheduleText = if (!seller.openTime.isNullOrBlank() && !seller.closeTime.isNullOrBlank()) {
                                    "${seller.openTime} a ${seller.closeTime}"
                                } else {
                                    "Horario flexible / Por turno"
                                }
                                Text(text = scheduleText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Teléfono con botones de acción directa
                        if (seller.phone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF003366),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Teléfono de Contacto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = seller.phone, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${seller.phone}"))
                                            context.startActivity(dialIntent)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Llamar", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val cleanPhone = seller.phone.filter { it.isDigit() }
                                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51$cleanPhone"))
                                            context.startActivity(waIntent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Métodos de pago aceptados
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Métodos de Pago Habilitados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                seller.effectivePaymentMethods.forEach { method ->
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PaymentMethodLogoByName(name = method, size = 16.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = method.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Descripción de la tienda
                        seller.displayBusinessDescription?.let { desc ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Descripción del Puesto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    color = Color.White.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. CATÁLOGO DE PRODUCTOS DEL PUESTO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catálogo de Productos (${products.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF003366)
                        )
                        if (isLoadingProducts) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }

                    if (products.isEmpty() && !isLoadingProducts) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_store_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Este puesto aún no tiene productos registrados en su catálogo.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Listado de productos
            if (!isLoadingProducts && products.isNotEmpty()) {
                items(products, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ValleGoProductImage(
                                imageUrl = product.imageUrl,
                                categoryName = product.categoryId,
                                productName = product.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "S/ %.2f".format(product.price),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00A884),
                                    fontSize = 13.sp
                                )
                                product.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = if (product.stock > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (product.stock > 0) "${product.stock} disp." else "Agotado",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.stock > 0) Color(0xFF2E7D32) else Color(0xFFC8102E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    color = if (product.isActive) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (product.isActive) "Publicado" else "Pausado",
                                        fontSize = 10.sp,
                                        color = if (product.isActive) Color(0xFF0284C7) else Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. HISTORIAL DE STRIKES Y MEDIDAS DISCIPLINARIAS
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    StrikeManagementCard(
                        strikes = warnings.size,
                        warnings = warnings,
                        isSeller = true,
                        isSuspended = isSuspended
                    )
                }
            }

            // 8. BOTONES DE ACCIÓN ADMINISTRATIVA (LLAMAR ATENCIÓN & SUSPENDER/REACTIVAR)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onIssueWarning != null && warnings.size < 5) {
                        Button(
                            onClick = { onIssueWarning(seller) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Llamar la Atención (+1 Strike)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (isSuspended) {
                        Button(
                            onClick = { onReactivate(seller) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reactivar Puesto de Venta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSuspend(seller) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC8102E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Suspender Puesto de Venta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
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
private fun DetailMetricMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

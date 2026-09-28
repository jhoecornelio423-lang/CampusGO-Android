package com.example.campusgo.features.buyer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.CartCalculationResult
import com.example.campusgo.domain.model.Product
import com.example.campusgo.domain.repository.AuthRepository
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.IncidentContextType
import com.example.campusgo.ui.components.PaymentMethodLogoByName
import com.example.campusgo.ui.components.ReportIncidentDialog
import com.example.campusgo.ui.components.StoreStatusBadge
import com.example.campusgo.ui.components.ValleGoBusinessAvatar
import com.example.campusgo.ui.components.ValleGoBusinessBanner
import com.example.campusgo.ui.components.ValleGoProductImage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    modifier: Modifier = Modifier,
    orderRepository: OrderRepository = koinInject(),
    authRepository: AuthRepository = koinInject()
) {
    val currentBuyerProfile by authRepository.currentProfile.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReportDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var isMeetingPointsExpanded by remember { mutableStateOf(false) }

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
    val isAnyModalOpen = showReportDialog || showEnlargedPhoto
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "buyer_seller_profile_blur"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Contenido desplazable estilo Rappi
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Portada Hero Superior a Pantalla Completa (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
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
                        shape = RoundedCornerShape(0.dp)
                    )

                    // Gradiente sutil superior e inferior para legibilidad perfecta
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x55000000),
                                        Color.Transparent,
                                        Color(0x33000000)
                                    )
                                )
                            )
                    )
                }

                // 2. Tarjeta Principal de Información del Puesto (Superpuesta sobre el banner estilo Rappi)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fila de Avatar + Estado del Puesto
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    border = BorderStroke(3.5.dp, Color.White),
                                    shadowElevation = 5.dp,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clickable {
                                            enlargedPhotoUrl = store.avatarUrl
                                            enlargedPhotoTitle = store.sellerName
                                            enlargedPhotoRole = "Puesto • " + (store.businessCategory ?: "Campus")
                                            isEnlargedBanner = false
                                            showEnlargedPhoto = true
                                        }
                                ) {
                                    ValleGoBusinessAvatar(
                                        avatarUrl = store.avatarUrl,
                                        storeName = store.sellerName,
                                        size = 76.dp
                                    )
                                }

                                StoreStatusBadge(
                                    status = store.businessStatus,
                                    acceptingOrders = store.acceptingOrders
                                )
                            }

                            // Nombre del Puesto
                            Text(
                                text = store.sellerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )

                            // Badges: Estrellas / Calificación, Categoría y Emprendedor Autorizado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "%.1f".format(store.ratingAverage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }

                                val category = store.businessCategory?.takeIf { it.isNotBlank() } ?: "Campus UCV"
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFFE6F6F3),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                            contentDescription = null,
                                            tint = Color(0xFF0D5C4C),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Autorizado UCV",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0D5C4C),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Bloque informativo moderno horizontal: Ubicación, Horario y Medios de Pago
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Ubicación
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_location_custom),
                                                contentDescription = null,
                                                tint = Color(0xFF00A884),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "UBICACIÓN",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8),
                                                letterSpacing = 0.4.sp
                                            )
                                        }
                                        Text(
                                            text = store.location?.takeIf { it.isNotBlank() } ?: "Campus UCV",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Divisor vertical sutil
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    // 2. Horario
                                    val hasSchedule = !store.openTime.isNullOrBlank() && !store.closeTime.isNullOrBlank()
                                    val scheduleText = if (hasSchedule) "${store.openTime} - ${store.closeTime}" else "Flexible"
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_clock_modern),
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "HORARIO",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8),
                                                letterSpacing = 0.4.sp
                                            )
                                        }
                                        Text(
                                            text = scheduleText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Divisor vertical sutil
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    // 3. Medios de Pago
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "PAGOS",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8),
                                                letterSpacing = 0.4.sp
                                            )
                                        }

                                        if (store.supportedPaymentMethods.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                store.supportedPaymentMethods.take(3).forEachIndexed { index, method ->
                                                    if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                                                    PaymentMethodLogoByName(name = method, size = 16.dp)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Efectivo",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Descripción breve
                            if (!store.description.isNullOrBlank()) {
                                Text(
                                    text = store.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569),
                                    lineHeight = 20.sp
                                )
                            }

                            // Alerta si el puesto está Saturado, Pausado o Cerrado
                            if (isSaturated) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    shape = RoundedCornerShape(10.dp),
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
                                    shape = RoundedCornerShape(10.dp),
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

                    // 3. Sección: Puntos de Entrega en Campus (Desplegable / Accordion)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            // Cabecera clickeable del desplegable
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMeetingPointsExpanded = !isMeetingPointsExpanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFEE2E2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Puntos de Entrega",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF16324F)
                                        )
                                        Text(
                                            text = if (sellerSupportedPoints.isNotEmpty())
                                                "${sellerSupportedPoints.size} puntos autorizados en campus"
                                            else
                                                "Entregas en accesos del campus",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isMeetingPointsExpanded) "Ocultar" else "Ver puntos",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003366),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isMeetingPointsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isMeetingPointsExpanded) "Colapsar" else "Expandir",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Lista expandible de puntos de encuentro
                            AnimatedVisibility(visible = isMeetingPointsExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(2.dp))

                                    if (sellerSupportedPoints.isEmpty()) {
                                        Surface(
                                            color = Color(0xFFFFFBEB),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "El vendedor coordina la entrega en los accesos principales del campus.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    } else {
                                        sellerSupportedPoints.forEach { point ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
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
                                                        val details = listOfNotNull(point.pavilion, point.description)
                                                            .filter { it.isNotBlank() }
                                                            .joinToString(" • ")
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
                        }
                    }

                    // 4. Sección: Catálogo de Productos
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PRODUCTOS DEL PUESTO (${store.products.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        if (store.products.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 5. Controles Flotantes Superiores estilo Rappi (Botón Volver y Botón Reportar con fondo circular blanco)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flecha de volver con círculo blanco detrás
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 5.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF16324F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botón de reportar con círculo blanco detrás
                Surface(
                    onClick = { showReportDialog = true },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 5.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                            contentDescription = "Reportar Puesto",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
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

    if (showReportDialog) {
        ReportIncidentDialog(
            title = "Reportar Puesto Comercial",
            subtitle = store.sellerName,
            contextType = IncidentContextType.SELLER,
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonKey, reasonLabel, details ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val reporterId = currentBuyerProfile?.id
                    val result = orderRepository.reportIncident(
                        subOrderId = null,
                        reporterId = reporterId,
                        reportedUserId = store.sellerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel }
                    )
                    isSubmittingReport = false
                    showReportDialog = false
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("Reporte enviado con éxito al Administrador del Campus.")
                    } else {
                        snackbarHostState.showSnackbar("Error al enviar reporte: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
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
        onClick = {
            if (isAvailable) {
                onClick()
            }
        },
        enabled = isAvailable,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isAvailable) Color(0xFFEEF2F6) else Color(0xFFE2E8F0)),
        shadowElevation = if (isAvailable) 1.5.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.55f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                ValleGoProductImage(
                    imageUrl = product.imageUrl,
                    categoryName = null,
                    productName = product.name,
                    emojiSize = 36,
                    modifier = Modifier.fillMaxSize()
                )

                // Si no hay stock o la tienda no está disponible, difuminar con capa y tarjetita 'No disponible'
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x990F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xEEBE123C), // Rojo oscuro elegante
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
                                    text = if (product.stock <= 0) "No disponible" else "No disponible",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = if (isAvailable) Color(0xFF1E293B) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (isAvailable) Color(0xFF003366) else Color(0xFF94A3B8)
                    )

                    FilledTonalButton(
                        onClick = { if (isAvailable) onAddToCart() },
                        enabled = isAvailable,
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE6F7F3),
                            contentColor = Color(0xFF00A884),
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledContentColor = Color(0xFFCBD5E1)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_to_cart_custom),
                            contentDescription = "Agregar al Carrito",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

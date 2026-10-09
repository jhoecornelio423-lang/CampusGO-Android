package com.example.campusgo.features.seller.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.features.seller.SellerDashboardUiState
import com.example.campusgo.features.seller.SellerOrderFilter
import com.example.campusgo.features.seller.components.MetricSummaryCard
import com.example.campusgo.features.seller.components.SellerSubOrderCard
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.OfficialWarningBanner
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SellerOrdersTab(
    uiState: SellerDashboardUiState,
    showWarningBanner: Boolean,
    timerProgress: Float,
    onWarningBannerClick: () -> Unit,
    onResetToToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onFilterSelected: (SellerOrderFilter) -> Unit,
    onAcceptSubOrder: (String) -> Unit,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onOpenDelivery: (SubOrder) -> Unit,
    onOpenRejection: (SubOrder) -> Unit,
    onOpenNoShow: (SubOrder) -> Unit,
    onSubOrderExpired: (String) -> Unit,
    onOpenDetail: (SubOrder) -> Unit,
    onOpenChat: (SubOrder) -> Unit,
    onOpenBuyerProfile: (SubOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalDarkTheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedVisibility(
            visible = showWarningBanner && uiState.warnings.isNotEmpty(),
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(400)) + shrinkVertically(tween(400))
        ) {
            OfficialWarningBanner(
                warnings = uiState.warnings,
                isSeller = true,
                timerProgress = timerProgress,
                onBannerClick = onWarningBannerClick
            )
        }

        val formattedSelectedDate = remember(uiState.selectedDate) {
            val dayName = uiState.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                .replaceFirstChar { it.uppercase() }
            val monthName = uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
            "$dayName, ${uiState.selectedDate.dayOfMonth} de $monthName"
        }

        // Indicador de Jornada de Hoy o Historial por Fecha con Botón de Calendario
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alarm_custom),
                    contentDescription = null,
                    tint = if (uiState.isViewingToday) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isViewingToday) "Jornada de Hoy • $formattedSelectedDate" else "Historial • $formattedSelectedDate",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isViewingToday) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!uiState.isViewingToday) {
                    Surface(
                        onClick = onResetToToday,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Volver a hoy",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Hoy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Botón de Calendario
                Surface(
                    onClick = onOpenDatePicker,
                    shape = RoundedCornerShape(12.dp),
                    color = if (!uiState.isViewingToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (!uiState.isViewingToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shadowElevation = if (!uiState.isViewingToday) 2.dp else 1.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Seleccionar fecha de historial",
                            tint = if (!uiState.isViewingToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Resumen de Métricas / KPIs del Día
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricSummaryCard(
                title = if (uiState.isViewingToday) "Ganancias Hoy" else "Ganancias",
                value = "S/ %.2f".format(uiState.displayEarnings),
                color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF003366),
                modifier = Modifier.weight(1.3f)
            )
            MetricSummaryCard(
                title = "Pendientes",
                value = "${uiState.displayPendingCount}",
                color = if (isDarkMode) Color(0xFFFB923C) else Color(0xFFF57C00),
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "En Preparación",
                value = "${uiState.displayInPrepCount}",
                color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF1976D2),
                modifier = Modifier.weight(1.25f)
            )
            MetricSummaryCard(
                title = "Listos",
                value = "${uiState.displayReadyCount}",
                color = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        // Filtros de Estado en Chips Horizontales
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SellerOrderFilter.values().forEach { filter ->
                val isSelected = uiState.selectedFilter == filter
                val label = when (filter) {
                    SellerOrderFilter.TODOS -> if (uiState.isViewingToday) "Hoy (${uiState.displayTotalOrders})" else "Todos (${uiState.displayTotalOrders})"
                    SellerOrderFilter.PENDIENTES -> "Pendientes (${uiState.displayPendingCount})"
                    SellerOrderFilter.EN_PREPARACION -> "En Preparación (${uiState.displayInPrepCount})"
                    SellerOrderFilter.LISTOS -> "Listos (${uiState.displayReadyCount})"
                    SellerOrderFilter.COMPLETADOS -> if (uiState.isViewingToday) "Entregados Hoy (${uiState.displayCompletedCount})" else "Entregados (${uiState.displayCompletedCount})"
                    SellerOrderFilter.RECHAZADOS -> "Rechazados (${uiState.displayRejectedCount})"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Lista de Subpedidos de la Fecha Seleccionada
        val displayOrders = remember(uiState.filteredDisplayOrders) {
            uiState.filteredDisplayOrders
        }
        var sellerHistoryLimit by remember(uiState.isViewingToday, uiState.selectedDate) { mutableIntStateOf(20) }
        val visibleOrders = remember(displayOrders, uiState.isViewingToday, sellerHistoryLimit) {
            if (!uiState.isViewingToday) {
                displayOrders.take(sellerHistoryLimit)
            } else {
                displayOrders
            }
        }

        if (displayOrders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_store_custom),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val emptyMsg = if (!uiState.isViewingToday) {
                        "No se registraron pedidos en esta fecha ($formattedSelectedDate)"
                    } else {
                        when (uiState.selectedFilter) {
                            SellerOrderFilter.TODOS -> "No hay subpedidos registrados aún"
                            SellerOrderFilter.PENDIENTES -> "No hay pedidos pendientes por responder"
                            SellerOrderFilter.EN_PREPARACION -> "No tienes pedidos en preparación actualmente"
                            SellerOrderFilter.LISTOS -> "No hay pedidos esperando entrega en este momento"
                            SellerOrderFilter.COMPLETADOS -> "No hay pedidos entregados registrados"
                            SellerOrderFilter.RECHAZADOS -> "No hay pedidos rechazados o cancelados"
                        }
                    }
                    Text(
                        text = emptyMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.isViewingToday) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onResetToToday,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver Pedidos de Hoy")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                item(key = "header_orders") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (uiState.isViewingToday) Color(0xFF2E7D32) else Color(0xFF0284C7), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isViewingToday) "Pedidos de Hoy" else "Historial del Día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${displayOrders.size} pedido${if (displayOrders.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(visibleOrders, key = { it.id }) { subOrder ->
                    SellerSubOrderCard(
                        subOrder = subOrder,
                        isProcessing = uiState.processingSubOrderIds.contains(subOrder.id),
                        onAccept = { onAcceptSubOrder(subOrder.id) },
                        onStartPrep = { onStartPrep(subOrder.id) },
                        onMarkReady = { onMarkReady(subOrder.id) },
                        onOpenDelivery = { onOpenDelivery(subOrder) },
                        onOpenRejection = { onOpenRejection(subOrder) },
                        onOpenNoShow = { onOpenNoShow(subOrder) },
                        onExpired = { onSubOrderExpired(subOrder.id) },
                        onOpenDetail = { onOpenDetail(subOrder) },
                        buyerStrikes = subOrder.buyerId?.let { uiState.buyerStrikes[it] } ?: 0,
                        onOpenChat = { onOpenChat(subOrder) },
                        onOpenBuyerProfile = { onOpenBuyerProfile(subOrder) }
                    )
                }

                if (!uiState.isViewingToday) {
                    if (displayOrders.size > sellerHistoryLimit) {
                        item(key = "load_more_seller_history") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Mostrando ${visibleOrders.size} de ${displayOrders.size} pedidos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Button(
                                        onClick = { sellerHistoryLimit += 20 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Cargar más",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (displayOrders.size > 20) {
                        item(key = "all_seller_history_loaded") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Mostrando todos los pedidos del historial (${displayOrders.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.vallego.features.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.HourlyDemandStat
import com.example.vallego.domain.model.MeetingPointStat
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SellerDashboardStats
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.TopProductStat
import com.example.vallego.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class StatsTimeRange {
    HOY,
    SEMANA,
    TODO
}

@Composable
fun SellerStatisticsScreen(
    sellerProfile: UserProfile,
    subOrders: List<SubOrder>,
    products: List<Product>,
    statsData: SellerDashboardStats? = null,
    isLoadingStats: Boolean = false,
    onRangeChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(StatsTimeRange.TODO) }

    // Si el backend ya computó statsData, se usa directamente (alta escalabilidad)
    // De lo contrario, se usa el cálculo reactivo local como fallback
    val fallbackStats = remember(subOrders, selectedRange) {
        computeLocalStats(subOrders, selectedRange)
    }

    val activeStats = if (statsData != null && (statsData.totalOrdersCount > 0 || subOrders.isEmpty())) {
        statsData
    } else {
        fallbackStats
    }

    val totalEarnings = activeStats.totalEarnings
    val completedCount = activeStats.completedCount
    val cancelledCount = activeStats.cancelledCount
    val inProgressCount = activeStats.inProgressCount
    val totalOrdersCount = activeStats.totalOrdersCount
    val averageTicket = activeStats.averageTicket

    val successRate = if (totalOrdersCount > 0) {
        ((completedCount.toDouble() / totalOrdersCount.toDouble()) * 100).toInt()
    } else 100

    val topProducts = activeStats.topProducts
    val hourlyStats = activeStats.hourlyDistribution
    val meetingPointStats = activeStats.topMeetingPoints

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector de periodo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Periodo:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                StatsTimeRange.values().forEach { range ->
                    val isSelected = selectedRange == range
                    val (label, param) = when (range) {
                        StatsTimeRange.HOY -> Pair("Hoy", "today")
                        StatsTimeRange.SEMANA -> Pair("Últimos 7 Días", "week")
                        StatsTimeRange.TODO -> Pair("Histórico Total", "all")
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedRange = range
                            onRangeChanged?.invoke(param)
                        },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003366),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (isLoadingStats) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFF003366),
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }

        // 1. Tarjetas Principales de Ingresos y Ticket Promedio
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Ventas Totales",
                    value = "S/ %.2f".format(totalEarnings),
                    subtitle = "$completedCount pedidos entregados",
                    icon = Icons.Default.AttachMoney,
                    cardColor = Color(0xFFE6F7F3),
                    accentColor = Color(0xFF00A884),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Ticket Promedio",
                    value = "S/ %.2f".format(averageTicket),
                    subtitle = "por pedido completado",
                    icon = Icons.Default.AttachMoney,
                    cardColor = Color(0xFFEBF5FF),
                    accentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Tarjetas Secundarias: Calificación y Efectividad
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Calificación",
                    value = "⭐ %.1f".format(sellerProfile.ratingAverage),
                    subtitle = "Reputación en campus",
                    icon = Icons.Default.Star,
                    cardColor = Color(0xFFFEF3C7),
                    accentColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Efectividad",
                    value = "$successRate%",
                    subtitle = "$cancelledCount cancelados/rechazados",
                    icon = Icons.Default.CheckCircle,
                    cardColor = Color(0xFFF3E8FF),
                    accentColor = Color(0xFF9333EA),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Desglose Operativo de Pedidos
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DESGLOSE DE PEDIDOS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366),
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    OrderStatusProgressBar(
                        label = "Entregados y Cobrados",
                        count = completedCount,
                        total = totalOrdersCount,
                        color = Color(0xFF00A884)
                    )
                    OrderStatusProgressBar(
                        label = "En Proceso / Espera",
                        count = inProgressCount,
                        total = totalOrdersCount,
                        color = Color(0xFF0284C7)
                    )
                    OrderStatusProgressBar(
                        label = "Rechazados o Cancelados",
                        count = cancelledCount,
                        total = totalOrdersCount,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // 4. Ranking de Productos Más Vendidos
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PRODUCTOS MÁS VENDIDOS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366),
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (topProducts.isEmpty()) {
                        Text(
                            text = "Aún no se han completado ventas en este periodo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val maxQty = topProducts.maxOfOrNull { it.unitsSold }?.takeIf { it > 0 } ?: 1
                        topProducts.forEachIndexed { index, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when (index) {
                                                0 -> Color(0xFFFEF3C7)
                                                1 -> Color(0xFFE2E8F0)
                                                2 -> Color(0xFFFFEDD5)
                                                else -> Color(0xFFF1F5F9)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (index) {
                                                        0 -> Color(0xFFB45309)
                                                        1 -> Color(0xFF475569)
                                                        2 -> Color(0xFF9A3412)
                                                        else -> Color(0xFF64748B)
                                                    }
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.unitsSold} uds.",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003366)
                                        )
                                        Text(
                                            text = "S/ %.2f".format(item.totalAmount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF00A884)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { (item.unitsSold.toFloat() / maxQty.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00A884),
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Horarios de Mayor Afluencia
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HORARIOS DE MAYOR DEMANDA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366),
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    val slotLabels = mapOf(
                        "morning" to Pair("Mañana (08:00 - 12:00)", Color(0xFF0284C7)),
                        "lunch" to Pair("Almuerzo (12:00 - 15:00)", Color(0xFF00A884)),
                        "afternoon" to Pair("Tarde (15:00 - 18:00)", Color(0xFFF59E0B)),
                        "night" to Pair("Noche (18:00 en ad.)", Color(0xFF8B5CF6))
                    )

                    val maxDemand = hourlyStats.maxOfOrNull { it.orderCount }?.takeIf { it > 0 } ?: 1
                    hourlyStats.forEach { stat ->
                        val (slotLabel, color) = slotLabels[stat.slot] ?: Pair(stat.slot, Color(0xFF003366))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = slotLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "${stat.orderCount} pedidos",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (stat.orderCount.toFloat() / maxDemand.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }

        // 6. Puntos de Entrega Más Concurridos
        if (meetingPointStats.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PUNTOS DE ENTREGA MÁS FRECUENTES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366),
                            letterSpacing = 0.5.sp
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        val maxPoints = meetingPointStats.maxOfOrNull { it.deliveryCount }?.takeIf { it > 0 } ?: 1
                        meetingPointStats.forEach { stat ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stat.pointName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${stat.deliveryCount} entregas",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003366)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (stat.deliveryCount.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF0284C7),
                                    trackColor = Color(0xFFF1F5F9)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    cardColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun OrderStatusProgressBar(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = if (total > 0) (count.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF334155)
            )
            Text(
                text = "$count (${(fraction * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

private fun computeLocalStats(subOrders: List<SubOrder>, range: StatsTimeRange): SellerDashboardStats {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis
    val startOfWeek = startOfToday - (6L * 24 * 60 * 60 * 1000)

    val filtered = when (range) {
        StatsTimeRange.HOY -> subOrders.filter { parseOrderTimestamp(it.createdAt) >= startOfToday }
        StatsTimeRange.SEMANA -> subOrders.filter { parseOrderTimestamp(it.createdAt) >= startOfWeek }
        StatsTimeRange.TODO -> subOrders
    }

    val completed = filtered.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
    val cancelled = filtered.filter {
        it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO
    }
    val inProgress = filtered.filter {
        it.status in setOf(
            SubOrderStatus.PENDIENTE, SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION,
            SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA
        )
    }

    val totalEarnings = completed.sumOf { it.subtotalAmount }
    val completedCount = completed.size
    val avgTicket = if (completedCount > 0) totalEarnings / completedCount else 0.0

    val productCounts = mutableMapOf<String, Pair<Int, Double>>()
    completed.forEach { order ->
        order.items.forEach { item ->
            val cur = productCounts.getOrDefault(item.productName, Pair(0, 0.0))
            productCounts[item.productName] = Pair(cur.first + item.quantity, cur.second + (item.unitPrice * item.quantity))
        }
    }
    val topProducts = productCounts.entries
        .map { (name, stats) -> TopProductStat(name, stats.first, stats.second) }
        .sortedByDescending { it.totalAmount }
        .take(5)

    val pointCounts = mutableMapOf<String, Int>()
    filtered.forEach { order ->
        val pointName = order.meetingPointName?.trim().takeIf { !it.isNullOrBlank() } ?: "Punto por acordar"
        pointCounts[pointName] = pointCounts.getOrDefault(pointName, 0) + 1
    }
    val meetingPointStats = pointCounts.entries
        .map { MeetingPointStat(it.key, it.value) }
        .sortedByDescending { it.deliveryCount }
        .take(4)

    var manana = 0
    var mediodia = 0
    var tarde = 0
    var noche = 0
    filtered.forEach { order ->
        val hour = getOrderHourOfDay(order.createdAt)
        when {
            hour in 8..11 -> manana++
            hour in 12..14 -> mediodia++
            hour in 15..17 -> tarde++
            else -> noche++
        }
    }
    val hourlyStats = listOf(
        HourlyDemandStat("morning", manana),
        HourlyDemandStat("lunch", mediodia),
        HourlyDemandStat("afternoon", tarde),
        HourlyDemandStat("night", noche)
    )

    return SellerDashboardStats(
        totalEarnings = totalEarnings,
        completedCount = completedCount,
        cancelledCount = cancelled.size,
        inProgressCount = inProgress.size,
        totalOrdersCount = filtered.size,
        averageTicket = avgTicket,
        topProducts = topProducts,
        hourlyDistribution = hourlyStats,
        topMeetingPoints = meetingPointStats
    )
}

private fun parseOrderTimestamp(isoDate: String?): Long {
    if (isoDate.isNullOrBlank()) return 0L
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoDate)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return 0L
}

private fun getOrderHourOfDay(isoDate: String?): Int {
    val timestamp = parseOrderTimestamp(isoDate)
    if (timestamp <= 0L) return 12
    val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
        timeInMillis = timestamp
    }
    return cal.get(Calendar.HOUR_OF_DAY)
}

package com.example.campusgo.features.seller.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.features.seller.DailyOrderGroup
import com.example.campusgo.theme.LocalDarkTheme

@Composable
fun SellerPastDayCard(
    group: DailyOrderGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAccept: (String) -> Unit,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onOpenDelivery: (SubOrder) -> Unit,
    onOpenRejection: (SubOrder) -> Unit,
    onOpenNoShow: (SubOrder) -> Unit,
    onExpired: (String) -> Unit,
    onOpenDetail: (SubOrder) -> Unit = {},
    onOpenChat: ((SubOrder) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${group.completedCount} entregados • ${group.orders.size} pedidos totales" +
                                if (group.cancelledCount > 0) " • ${group.cancelledCount} cancelados" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "S/ %.2f".format(group.totalEarnings),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    group.orders.forEach { subOrder ->
                        SellerSubOrderCard(
                            subOrder = subOrder,
                            isProcessing = false,
                            onAccept = { onAccept(subOrder.id) },
                            onStartPrep = { onStartPrep(subOrder.id) },
                            onMarkReady = { onMarkReady(subOrder.id) },
                            onOpenDelivery = { onOpenDelivery(subOrder) },
                            onOpenRejection = { onOpenRejection(subOrder) },
                            onOpenNoShow = { onOpenNoShow(subOrder) },
                            onExpired = { onExpired(subOrder.id) },
                            onOpenDetail = { onOpenDetail(subOrder) },
                            onOpenChat = if (onOpenChat != null && !subOrder.status.isFinal) { { onOpenChat(subOrder) } } else null
                        )
                    }
                }
            }
        }
    }
}

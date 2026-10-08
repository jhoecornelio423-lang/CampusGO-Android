package com.example.campusgo.features.seller.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.features.seller.SellerTab

@Composable
fun SellerBottomNavBar(
    selectedTab: SellerTab,
    onTabSelected: (SellerTab) -> Unit,
    pendingOrdersCount: Int = 0,
    unreadChatCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF00A884)
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = Color(0x1F16324F),
                    ambientColor = Color(0x2816324F),
                    clip = false
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 1. Pedidos
                SellerNavItem(
                    selected = selectedTab == SellerTab.PEDIDOS,
                    onClick = { onTabSelected(SellerTab.PEDIDOS) },
                    iconRes = R.drawable.ic_orders_bag,
                    label = "Pedidos",
                    badgeCount = pendingOrdersCount,
                    badgeColor = Color(0xFFF59E0B), // Ámbar para alertar pedidos pendientes
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 2. Menú
                SellerNavItem(
                    selected = selectedTab == SellerTab.PRODUCTOS,
                    onClick = { onTabSelected(SellerTab.PRODUCTOS) },
                    iconRes = R.drawable.ic_store_custom,
                    label = "Menú",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 3. Estadísticas
                SellerNavItem(
                    selected = selectedTab == SellerTab.ESTADISTICAS,
                    onClick = { onTabSelected(SellerTab.ESTADISTICAS) },
                    iconRes = R.drawable.ic_nav_stats_custom,
                    label = "Estadísticas",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 4. Chats
                SellerNavItem(
                    selected = selectedTab == SellerTab.CHATS,
                    onClick = { onTabSelected(SellerTab.CHATS) },
                    iconRes = R.drawable.ic_nav_chat_custom,
                    label = "Chats",
                    badgeCount = unreadChatCount,
                    badgeColor = Color(0xFFEF4444), // Rojo para mensajes no leídos
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 5. Perfil
                SellerNavItem(
                    selected = selectedTab == SellerTab.PERFIL,
                    onClick = { onTabSelected(SellerTab.PERFIL) },
                    iconRes = R.drawable.ic_nav_profile_custom,
                    label = "Perfil",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
}

@Composable
private fun SellerNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeColor: Color = Color(0xFFEF4444),
    activeColor: Color = Color(0xFF00A884),
    inactiveColor: Color = Color(0xFF64748B)
) {
    val tintColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "seller_nav_tint"
    )
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFE6F7F3) else Color.Transparent,
        animationSpec = tween(200),
        label = "seller_nav_pill"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillBgColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = badgeColor,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (badgeCount > 9) "+9" else "$badgeCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tintColor,
            maxLines = 1
        )
    }
}

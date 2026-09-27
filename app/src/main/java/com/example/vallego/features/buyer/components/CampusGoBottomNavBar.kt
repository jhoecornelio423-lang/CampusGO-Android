package com.example.vallego.features.buyer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R

enum class BuyerBottomNavTab {
    INICIO,
    FAVORITOS,
    PEDIDOS,
    CHATS,
    PERFIL
}

@Composable
fun CampusGoBottomNavBar(
    selectedTab: BuyerBottomNavTab,
    onTabSelected: (BuyerBottomNavTab) -> Unit,
    unreadChatCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF00A884)
    val inactiveColor = Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Barra blanca curvada con bordes redondeados superiores y difuminado/sombra suave
        // El único contorno visible es el redondeado, el exterior es 100% transparente
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp) // Espacio de 14dp para que el círculo de Pedidos sobresalga hacia arriba
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = Color(0x1F16324F),
                    ambientColor = Color(0x2816324F),
                    clip = false
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                        .height(68.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 1. Inicio
                BottomNavItem(
                    selected = selectedTab == BuyerBottomNavTab.INICIO,
                    onClick = { onTabSelected(BuyerBottomNavTab.INICIO) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_home_custom),
                            contentDescription = "Inicio",
                            tint = if (selectedTab == BuyerBottomNavTab.INICIO) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = "Inicio",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 2. Favoritos
                BottomNavItem(
                    selected = selectedTab == BuyerBottomNavTab.FAVORITOS,
                    onClick = { onTabSelected(BuyerBottomNavTab.FAVORITOS) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_favorites_custom),
                            contentDescription = "Favoritos",
                            tint = if (selectedTab == BuyerBottomNavTab.FAVORITOS) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = "Favoritos",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 3. Espacio central para Pedidos (su etiqueta se posiciona en la parte inferior con holgura)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(BuyerBottomNavTab.PEDIDOS) }
                        .padding(bottom = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "Pedidos",
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedTab == BuyerBottomNavTab.PEDIDOS) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == BuyerBottomNavTab.PEDIDOS) activeColor else inactiveColor
                    )
                }

                // 4. Chats
                BottomNavItem(
                    selected = selectedTab == BuyerBottomNavTab.CHATS,
                    onClick = { onTabSelected(BuyerBottomNavTab.CHATS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadChatCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadChatCount > 9) "+9" else "$unreadChatCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_chat_custom),
                                contentDescription = "Chats",
                                tint = if (selectedTab == BuyerBottomNavTab.CHATS) activeColor else inactiveColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = "Chats",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )

                // 5. Perfil
                BottomNavItem(
                    selected = selectedTab == BuyerBottomNavTab.PERFIL,
                    onClick = { onTabSelected(BuyerBottomNavTab.PERFIL) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_profile_custom),
                            contentDescription = "Perfil",
                            tint = if (selectedTab == BuyerBottomNavTab.PERFIL) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = "Perfil",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

        // Botón central flotante de Pedidos (círculo verde elevado con sombra)
        Surface(
            onClick = { onTabSelected(BuyerBottomNavTab.PEDIDOS) },
            shape = CircleShape,
            color = Color(0xFF00A884),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(52.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_orders_bag),
                    contentDescription = "Pedidos",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        icon()
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else inactiveColor
        )
    }
}

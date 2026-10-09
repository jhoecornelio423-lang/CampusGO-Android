package com.example.campusgo.features.admin.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.features.admin.AdminTab
import com.example.campusgo.theme.ThemeManager

private data class AdminDockItemData(
    val tab: AdminTab,
    val label: String,
    val iconResId: Int? = null,
    val iconVector: ImageVector? = null
)

@Composable
fun AdminLiquidGlassDock(
    selectedTab: AdminTab,
    pendingApplicationsCount: Int = 0,
    onSelectTab: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            AdminDockItemData(
                tab = AdminTab.MEETING_POINTS,
                label = "Puntos",
                iconResId = R.drawable.ic_location_custom
            ),
            AdminDockItemData(
                tab = AdminTab.SELLER_APPLICATIONS,
                label = "Solicitudes",
                iconVector = Icons.Default.VerifiedUser
            ),
            AdminDockItemData(
                tab = AdminTab.SELLERS_DIRECTORY,
                label = "Puestos",
                iconResId = R.drawable.ic_store_custom
            ),
            AdminDockItemData(
                tab = AdminTab.BUYERS_DIRECTORY,
                label = "Alumnos",
                iconVector = Icons.Default.Group
            ),
            AdminDockItemData(
                tab = AdminTab.CAMPUS_METRICS,
                label = "Métricas",
                iconVector = Icons.AutoMirrored.Filled.TrendingUp
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Panel del Dock con efecto Liquid Glass translúcido / blur
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x33003366),
                    ambientColor = Color(0x1F000000)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (ThemeManager.isDarkMode.collectAsState().value) {
                            listOf(
                                Color(0xE61E293B),
                                Color(0xD90F172A)
                            )
                        } else {
                            listOf(
                                Color(0xCCFFFFFF),
                                Color(0xAAFFFFFF)
                            )
                        }
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.verticalGradient(
                        if (ThemeManager.isDarkMode.collectAsState().value) {
                            listOf(
                                Color(0x8064748B),
                                Color(0x40475569),
                                Color(0x20334155)
                            )
                        } else {
                            listOf(
                                Color(0xF0FFFFFF),
                                Color(0x80FFFFFF),
                                Color(0x30FFFFFF)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            // Capa de brillo satinado del cristal líquido
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            if (ThemeManager.isDarkMode.collectAsState().value) {
                                listOf(
                                    Color(0x33CBD5E1),
                                    Color(0x0F94A3B8),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0x66FFFFFF),
                                    Color(0x1AFFFFFF),
                                    Color.Transparent
                                )
                            }
                        )
                    )
            )

            // Fila de ítems del Dock con espaciado elástico
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    AdminDockItem(
                        item = item,
                        isSelected = selectedTab == item.tab,
                        badgeCount = if (item.tab == AdminTab.SELLER_APPLICATIONS) pendingApplicationsCount else 0,
                        onClick = { onSelectTab(item.tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDockItem(
    item: AdminDockItemData,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dock_item_scale"
    )

    val itemWidth by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dock_item_width"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(itemWidth)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x330284C7),
                            Color(0x1A003366)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        if (ThemeManager.isDarkMode.collectAsState().value) {
                            listOf(Color(0x20FFFFFF), Color(0x0CFFFFFF))
                        } else {
                            listOf(Color(0x14000000), Color(0x08000000))
                        }
                    )
                }
            )
            .border(
                width = if (isSelected) 1.3.dp else 0.8.dp,
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF0284C7), Color(0x66003366))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0x40FFFFFF), Color(0x15FFFFFF))
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        ) {
            val isDarkMode = ThemeManager.isDarkMode.collectAsState().value
            val iconTint = if (isSelected) {
                if (isDarkMode) Color(0xFF70F7D7) else Color(0xFF003366)
            } else {
                if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
            }
            val iconModifier = Modifier.size(20.dp)

            Box(contentAlignment = Alignment.Center) {
                if (item.iconResId != null) {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = iconModifier
                    )
                } else if (item.iconVector != null) {
                    Icon(
                        imageVector = item.iconVector,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = iconModifier
                    )
                }

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFC8102E), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = iconTint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

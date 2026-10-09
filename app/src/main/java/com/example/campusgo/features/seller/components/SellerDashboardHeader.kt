package com.example.campusgo.features.seller.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.CampusGoUserAvatar

@Composable
fun SellerDashboardHeader(
    profile: UserProfile,
    isAcceptingOrders: Boolean,
    warnings: List<ProfileWarning>,
    unreadWarningsCount: Int,
    onStoreClick: () -> Unit,
    onToggleAcceptingOrders: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                spotColor = Color(0x1F16324F),
                ambientColor = Color(0x2816324F),
                clip = false
            ),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tienda / Emprendimiento (Al pulsar lleva a la pestaña de Perfil)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onStoreClick)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                CampusGoUserAvatar(
                    avatarUrl = profile.avatarUrl,
                    name = profile.businessName ?: profile.fullName,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    val storeDisplayName = (profile.businessName?.takeIf { it.isNotBlank() } ?: profile.fullName).ifBlank { "Mi Puesto" }
                    Text(
                        text = storeDisplayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = profile.businessLocation?.takeIf { it.isNotBlank() } ?: "Campus ${profile.campus}"
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Controles fijos superiores: Toggle Abrir/Cerrar Puesto + Botón de Notificaciones/Strikes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDark = LocalDarkTheme.current
                val thumbOffset by animateDpAsState(
                    targetValue = if (isAcceptingOrders) 14.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "stall_toggle"
                )
                val toggleBg = if (isDark) {
                    if (isAcceptingOrders) Color(0xFF004D3D).copy(alpha = 0.45f) else Color(0xFF450A0A).copy(alpha = 0.45f)
                } else {
                    if (isAcceptingOrders) Color(0xFFE6F7F3) else Color(0xFFFEE2E2)
                }
                val toggleBorder = if (isDark) {
                    if (isAcceptingOrders) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFFF87171).copy(alpha = 0.4f)
                } else {
                    if (isAcceptingOrders) Color(0xFF00A884).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
                }
                val toggleText = if (isDark) {
                    if (isAcceptingOrders) Color(0xFF34D399) else Color(0xFFF87171)
                } else {
                    if (isAcceptingOrders) Color(0xFF007A60) else Color(0xFFB91C1C)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(toggleBg)
                        .border(1.dp, toggleBorder, RoundedCornerShape(20.dp))
                        .clickable { onToggleAcceptingOrders(!isAcceptingOrders) }
                        .padding(start = 9.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(toggleText, CircleShape)
                    )
                    Text(
                        text = if (isAcceptingOrders) "Abierto" else "Cerrado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = toggleText
                    )
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(18.dp)
                            .background(
                                if (isAcceptingOrders) (if (isDark) Color(0xFF00B589) else Color(0xFF00A884)) else (if (isDark) Color(0xFF30363D) else Color(0xFFCBD5E1)),
                                RoundedCornerShape(9.dp)
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = thumbOffset)
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                // Botón de Avisos y Moderación
                if (warnings.isNotEmpty()) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.size(38.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadWarningsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = "$unreadWarningsCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Avisos y Moderación",
                                tint = if (unreadWarningsCount > 0) MaterialTheme.colorScheme.error else Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

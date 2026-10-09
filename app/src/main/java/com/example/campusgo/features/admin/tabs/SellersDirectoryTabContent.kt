package com.example.campusgo.features.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.campusgo.theme.ThemeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import com.example.campusgo.ui.components.StrikeBadge

@Composable
fun SellersDirectoryTabContent(
    sellers: List<UserProfile>,
    strikesMap: Map<String, Int> = emptyMap(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectSeller: (UserProfile) -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Puestos del Campus (${sellers.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Gestión, auditoría y seguimiento detallado a pantalla completa de puestos universitarios", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Barra de búsqueda con icono y botón para limpiar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por puesto, titular o rubro...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (sellers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_store_custom),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron puestos que coincidan con '$searchQuery'" else "No hay emprendedores registrados en este campus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(sellers, key = { it.id }) { seller ->
                    SellerDirectoryCard(
                        seller = seller,
                        strikes = strikesMap[seller.id] ?: 0,
                        onSelectSeller = { onSelectSeller(seller) },
                        onSuspend = { onSuspend(seller) },
                        onReactivate = { onReactivate(seller) },
                        onIssueWarning = { onIssueWarning(seller) }
                    )
                }
            }
        }
    }
}

@Composable
fun SellerDirectoryCard(
    seller: UserProfile,
    strikes: Int = 0,
    onSelectSeller: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onIssueWarning: () -> Unit
) {
    val isSuspended = seller.role == UserRole.SUSPENDED
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuspended) {
                if (isDarkMode) Color(0xFF450A0A) else Color(0xFFFEF2F2)
            } else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSuspended) {
                if (isDarkMode) Color(0xFF991B1B) else Color(0xFFFECACA)
            } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectSeller)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampusGoBusinessAvatar(
                    avatarUrl = seller.avatarUrl,
                    storeName = seller.displayStoreName,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seller.displayStoreName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Titular: ${seller.fullName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    seller.businessCategory?.takeIf { it.isNotBlank() }?.let { cat ->
                        Text(
                            text = "Rubro: $cat",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF0D9488),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = if (isSuspended) MaterialTheme.colorScheme.error else if (seller.acceptingOrders) (if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669)) else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuspended) "SUSPENDIDO" else if (seller.acceptingOrders) "ABIERTO" else "CERRADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    StrikeBadge(strikes = strikes, showAutoSuspensionLabel = true)
                }
            }

            if (isSuspended && !seller.suspensionReason.isNullOrBlank()) {
                Surface(
                    color = if (isDarkMode) Color(0xFF2D1214) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Motivo de suspensión: ${seller.suspensionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_custom),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = seller.businessLocation ?: seller.campus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (strikes < 5) {
                        IconButton(
                            onClick = onIssueWarning,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Llamar la atención",
                                tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSelectSeller,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Ver Detalle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }

                    if (isSuspended) {
                        Button(
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reactivar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSuspend,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Suspender", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

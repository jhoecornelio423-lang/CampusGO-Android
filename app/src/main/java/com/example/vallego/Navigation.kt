package com.example.vallego

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import com.example.vallego.features.admin.AdminHomeScreen
import com.example.vallego.features.auth.AuthRoute
import com.example.vallego.features.buyer.BuyerHomeScreen
import com.example.vallego.features.seller.SellerDashboardScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainNavigation(
    authRepository: AuthRepository = koinInject(),
    cartRepository: com.example.vallego.domain.repository.CartRepository = koinInject(),
    orderRepository: com.example.vallego.domain.repository.OrderRepository = koinInject()
) {
    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val currentProfile by authRepository.currentProfile.collectAsState()
    val isSessionChecking by authRepository.isSessionChecking.collectAsState()
    val scope = rememberCoroutineScope()

    if (isSessionChecking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.campus_logo_full),
                    contentDescription = "Logo CampusGO",
                    modifier = Modifier.size(170.dp),
                    contentScale = ContentScale.Fit
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF16A085),
                    strokeWidth = 3.dp
                )
            }
        }
    } else if (!isAuthenticated || currentProfile == null) {
        AuthRoute(
            onAuthSuccess = { /* State triggers automatic recomposition */ },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val profile = currentProfile!!
        val onSignOut: () -> Unit = {
            scope.launch {
                cartRepository.clearCart()
                orderRepository.clearCache()
                authRepository.signOut()
            }
        }

        if (profile.isSellerPendingApproval) {
            com.example.vallego.features.auth.SellerPendingApprovalFullScreen(
                profile = profile,
                onSignOut = onSignOut,
                modifier = Modifier.safeDrawingPadding()
            )
        } else {
            when (profile.role) {
                UserRole.COMPRADOR -> {
                    BuyerHomeScreen(
                        profile = profile,
                        onSignOut = onSignOut,
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                UserRole.EMPRENDEDOR -> {
                    SellerDashboardScreen(
                        profile = profile,
                        onSignOut = onSignOut,
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                UserRole.ADMIN -> {
                    AdminHomeScreen(
                        profile = profile,
                        onSignOut = onSignOut,
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                UserRole.SUSPENDED, UserRole.SUSPENDED_BUYER -> {
                    SuspendedAccountScreen(
                        profile = profile,
                        onSignOut = onSignOut,
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
            }
        }
    }
}

@Composable
fun SuspendedAccountScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icono de advertencia / suspensión
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Acceso Suspendido",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Cuenta Suspendida",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tu acceso a CampusGO ha sido suspendido temporalmente por la administración debido a infracciones o reportes acumulados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Resumen de la cuenta afectada
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Datos de la Cuenta",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Usuario:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(profile.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sede:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(profile.campus, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estado:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text("Suspendido", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }

            // Botones de acción (Contacto con soporte y Cerrar Sesión)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri("mailto:soporte@kodexti.com?subject=Consulta%20Cuenta%20Suspendida%20CampusGO%20-%20${profile.fullName}")
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16324F))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar a Soporte", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

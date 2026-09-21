package com.example.vallego.features.cart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.ui.components.SlideCommit
import org.koin.androidx.compose.koinViewModel

enum class CartCheckoutStep {
    PRODUCTS,
    DELIVERY,
    PAYMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    buyerProfile: UserProfile,
    onNavigateBack: () -> Unit,
    onNavigateToTracking: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshMeetingPoints()
    }

    var currentStep by remember { mutableStateOf(CartCheckoutStep.PRODUCTS) }

    // Si el carrito queda vacío, volver automáticamente al paso 1
    LaunchedEffect(uiState.isEmpty) {
        if (uiState.isEmpty) {
            currentStep = CartCheckoutStep.PRODUCTS
        }
    }

    // Manejo del botón Atrás de Android
    BackHandler(enabled = currentStep != CartCheckoutStep.PRODUCTS) {
        currentStep = when (currentStep) {
            CartCheckoutStep.PAYMENT -> CartCheckoutStep.DELIVERY
            CartCheckoutStep.DELIVERY -> CartCheckoutStep.PRODUCTS
            CartCheckoutStep.PRODUCTS -> CartCheckoutStep.PRODUCTS
        }
    }

    val isOrderConfirmed = uiState.placedOrder != null
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isOrderConfirmed) 20.dp else 0.dp,
        label = "cart_dialog_blur"
    )

    var showClearCartDialog by remember { mutableStateOf(false) }

    if (showClearCartDialog) {
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFFC8102E),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Vaciar el carrito?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar todos los productos seleccionados?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart()
                        showClearCartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Sí, vaciar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCartDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Pantalla Completa de Resumen / Boleta cuando se genera la orden y sus subpedidos
    if (uiState.placedOrder != null) {
        val order = uiState.placedOrder!!
        OrderSummaryReceiptScreen(
            order = order,
            onNavigateToTracking = {
                viewModel.clearPlacedOrder()
                onNavigateToTracking()
            },
            onOpenChat = {
                viewModel.clearPlacedOrder()
                onNavigateToTracking()
            },
            onNavigateBack = {
                viewModel.clearPlacedOrder()
                onNavigateBack()
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentStep) {
                                CartCheckoutStep.PRODUCTS -> "Mi Carrito"
                                CartCheckoutStep.DELIVERY -> "Punto de Entrega"
                                CartCheckoutStep.PAYMENT -> "Método de Pago"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                when (currentStep) {
                                    CartCheckoutStep.PAYMENT -> currentStep = CartCheckoutStep.DELIVERY
                                    CartCheckoutStep.DELIVERY -> currentStep = CartCheckoutStep.PRODUCTS
                                    CartCheckoutStep.PRODUCTS -> onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                )
            },
            modifier = if (backgroundBlurRadius > 0.dp) Modifier.fillMaxSize().blur(backgroundBlurRadius) else Modifier.fillMaxSize()
        ) { innerPadding ->
            if (uiState.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tu carrito está vacío",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Explora los puestos de tu campus y agrega tus antojos favoritos en una sola compra.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                        ) {
                            Text("Explorar Puestos")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val canProceedToPayment = uiState.selectedMeetingPoint != null && uiState.meetingPointWarning == null

                    // Barra de progreso interactiva por secciones
                    CartStepIndicator(
                        currentStep = currentStep,
                        canProceedToPayment = canProceedToPayment,
                        onStepClick = { step ->
                            when (step) {
                                CartCheckoutStep.PRODUCTS -> currentStep = CartCheckoutStep.PRODUCTS
                                CartCheckoutStep.DELIVERY -> currentStep = CartCheckoutStep.DELIVERY
                                CartCheckoutStep.PAYMENT -> {
                                    if (canProceedToPayment) {
                                        currentStep = CartCheckoutStep.PAYMENT
                                    }
                                }
                            }
                        }
                    )

                    Crossfade(
                        targetState = currentStep,
                        label = "cart_step_crossfade",
                        modifier = Modifier.weight(1f)
                    ) { step ->
                        when (step) {
                            CartCheckoutStep.PRODUCTS -> {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Lista scrolleable de productos
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Cabecera de lista con opción rápida de vaciar carrito
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Productos seleccionados (${uiState.calculation.totalItemCount})",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF003366)
                                            )
                                            TextButton(
                                                onClick = { showClearCartDialog = true },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC8102E))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Vaciar carrito",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        // Grupos por Emprendimiento
                                        uiState.calculation.storeGroups.forEach { group ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // Cabecera del puesto
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_store_modern),
                                                                contentDescription = null,
                                                                tint = Color(0xFF003366),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = group.sellerName,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF003366)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Subtotal: S/ %.2f".format(group.subtotal),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFC8102E)
                                                        )
                                                    }

                                                    HorizontalDivider()

                                                    // Lista de productos del puesto
                                                    group.items.forEach { cartItem ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = cartItem.product.name,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                Text(
                                                                    text = "S/ %.2f c/u".format(cartItem.product.price),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }

                                                            // Stepper cantidad (- 1 +)
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                IconButton(
                                                                    onClick = { viewModel.decrementItem(cartItem.product.id) },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                                                        contentDescription = "Disminuir",
                                                                        tint = if (cartItem.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }

                                                                Text(
                                                                    text = "${cartItem.quantity}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                                )

                                                                IconButton(
                                                                    onClick = { viewModel.incrementItem(cartItem.product.id) },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Add,
                                                                        contentDescription = "Aumentar"
                                                                    )
                                                                }

                                                                Text(
                                                                    text = "S/ %.2f".format(cartItem.subtotal),
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.width(64.dp),
                                                                    textAlign = TextAlign.End
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Barra inferior FIJA en la parte de abajo con el Subtotal, Precio y Botón Continuar
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 6.dp,
                                        shadowElevation = 10.dp,
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Subtotal (${uiState.calculation.totalItemCount} productos):",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${uiState.calculation.storeGroups.size} puesto(s) comercial(es)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "S/ %.2f".format(uiState.calculation.grandTotal),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF003366)
                                                )
                                            }

                                            Button(
                                                onClick = { currentStep = CartCheckoutStep.DELIVERY },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                            ) {
                                                Text(
                                                    text = "Continuar a Entrega",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Paso 2: Entrega
                            CartCheckoutStep.DELIVERY -> {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Contenido scrolleable
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Resumen rápido de productos con acceso a modificarlos
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1D4ED8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "${uiState.calculation.totalItemCount} producto(s) • Total: S/ %.2f".format(uiState.calculation.grandTotal),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1E3A8A)
                                                        )
                                                        Text(
                                                            text = "${uiState.calculation.storeGroups.size} puesto(s) seleccionado(s)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF3B82F6)
                                                        )
                                                    }
                                                }
                                                TextButton(
                                                    onClick = { currentStep = CartCheckoutStep.PRODUCTS },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1D4ED8))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Modificar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        }

                                        // Sección: Punto de Encuentro en Campus
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = Color(0xFFC8102E)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Punto de Encuentro en Campus",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Aviso si no hay intersección de puntos de entrega
                                                if (uiState.meetingPointWarning != null) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info,
                                                                contentDescription = null,
                                                                tint = Color(0xFFD97706),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = uiState.meetingPointWarning!!,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF92400E)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Dropdown de Puntos de Encuentro
                                                var expanded by remember { mutableStateOf(false) }
                                                ExposedDropdownMenuBox(
                                                    expanded = expanded,
                                                    onExpandedChange = { expanded = !expanded }
                                                ) {
                                                    val selectedText = uiState.selectedMeetingPoint?.let { pt ->
                                                        val zonePart = if (pt.zoneType.equals("EXTERIOR", ignoreCase = true)) " • Exterior" else " • Interior"
                                                        "${pt.name}$zonePart"
                                                    } ?: if (uiState.meetingPoints.isEmpty()) "Sin puntos de entrega disponibles" else "Selecciona un punto de entrega"

                                                    OutlinedTextField(
                                                        value = selectedText,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .fillMaxWidth()
                                                    )
                                                    ExposedDropdownMenu(
                                                        expanded = expanded,
                                                        onDismissRequest = { expanded = false }
                                                    ) {
                                                        if (uiState.meetingPoints.isEmpty()) {
                                                            DropdownMenuItem(
                                                                text = { Text("No hay puntos de entrega compatibles") },
                                                                onClick = { expanded = false },
                                                                enabled = false
                                                            )
                                                        } else {
                                                            uiState.meetingPoints.forEach { point ->
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                Text(
                                                                                    text = point.name,
                                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                                    fontWeight = FontWeight.SemiBold,
                                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                                Surface(
                                                                                    color = if (point.zoneType.equals("EXTERIOR", ignoreCase = true)) Color(0xFFE6F6F3) else Color(0xFFEFF6FF),
                                                                                    shape = RoundedCornerShape(4.dp)
                                                                                ) {
                                                                                    Text(
                                                                                        text = if (point.zoneType.equals("EXTERIOR", ignoreCase = true)) "Exterior" else "Interior",
                                                                                        style = MaterialTheme.typography.labelSmall,
                                                                                        color = if (point.zoneType.equals("EXTERIOR", ignoreCase = true)) Color(0xFF16A085) else Color(0xFF1D4ED8),
                                                                                        fontWeight = FontWeight.Bold,
                                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                                    )
                                                                                }
                                                                            }
                                                                            if (!point.description.isNullOrBlank()) {
                                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                                Text(
                                                                                    text = point.description,
                                                                                    style = MaterialTheme.typography.bodySmall,
                                                                                    color = Color(0xFF64748B)
                                                                                )
                                                                            }
                                                                        }
                                                                    },
                                                                    onClick = {
                                                                        viewModel.selectMeetingPoint(point)
                                                                        expanded = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Horario de Entrega (Intervalos de 30 min)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = Color(0xFF003366)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Horario de Encuentro en Campus",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Banner informativo sobre el horario y disponibilidad
                                                Surface(
                                                    color = if (uiState.isCampusClosedNow) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = if (uiState.isCampusClosedNow) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = uiState.deliveryScheduleNote,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (uiState.isCampusClosedNow) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    uiState.availableTimeSlots.forEach { slot ->
                                                        FilterChip(
                                                            selected = uiState.selectedTimeSlot == slot,
                                                            onClick = { viewModel.selectTimeSlot(slot) },
                                                            label = { Text(slot) }
                                                        )
                                                    }
                                                }

                                                // Indicaciones opcionales para la entrega
                                                OutlinedTextField(
                                                    value = uiState.orderNotes,
                                                    onValueChange = viewModel::onNotesChange,
                                                    label = { Text("Notas para la entrega (opcional)") },
                                                    placeholder = { Text("Ej: Estoy con casaca azul cerca a la puerta") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    // Barra inferior FIJA en la parte de abajo con el Subtotal, Precio y Botón Continuar (igual que en Producto)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 6.dp,
                                        shadowElevation = 10.dp,
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "Subtotal (${uiState.calculation.totalItemCount} productos):",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = uiState.selectedMeetingPoint?.let { "Entrega: ${it.name}" } ?: "Selecciona punto de entrega",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (canProceedToPayment) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "S/ %.2f".format(uiState.calculation.grandTotal),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF003366)
                                                )
                                            }

                                            Button(
                                                onClick = { currentStep = CartCheckoutStep.PAYMENT },
                                                enabled = canProceedToPayment,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF003366),
                                                    disabledContainerColor = Color(0xFF64748B).copy(alpha = 0.4f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                            ) {
                                                Text(
                                                    text = "Continuar a Pago",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Paso 3: Pago
                            CartCheckoutStep.PAYMENT -> {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Contenido scrolleable
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Resumen de la Entrega seleccionada
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = Color(0xFFC8102E),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Datos de Entrega",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF003366)
                                                        )
                                                    }
                                                    TextButton(
                                                        onClick = { currentStep = CartCheckoutStep.DELIVERY },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1D4ED8))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Cambiar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }

                                                HorizontalDivider()

                                                // Punto de encuentro
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "Punto:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.width(64.dp)
                                                    )
                                                    Text(
                                                        text = uiState.selectedMeetingPoint?.let { pt ->
                                                            val zone = if (pt.zoneType.equals("EXTERIOR", ignoreCase = true)) " (Exterior)" else " (Interior)"
                                                            "${pt.name}$zone"
                                                        } ?: "No seleccionado",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                // Horario
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Horario:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.width(64.dp)
                                                    )
                                                    Text(
                                                        text = uiState.selectedTimeSlot,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF003366)
                                                    )
                                                }

                                                // Notas si existen
                                                if (uiState.orderNotes.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Text(
                                                            text = "Notas:",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.width(64.dp)
                                                        )
                                                        Text(
                                                            text = uiState.orderNotes,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Resumen rápido de productos con acceso a modificarlos
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1D4ED8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "${uiState.calculation.totalItemCount} producto(s) • Total: S/ %.2f".format(uiState.calculation.grandTotal),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1E3A8A)
                                                        )
                                                        Text(
                                                            text = "${uiState.calculation.storeGroups.size} puesto(s) seleccionado(s)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF3B82F6)
                                                        )
                                                    }
                                                }
                                                TextButton(
                                                    onClick = { currentStep = CartCheckoutStep.PRODUCTS },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1D4ED8))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Modificar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        }

                                        // Sección: Método de Pago (Contra entrega)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF003366),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Método de Pago (Contra entrega)",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF003366)
                                                    )
                                                }

                                                if (uiState.paymentMethodWarning != null) {
                                                    Surface(
                                                        color = Color(0xFFFEF2F2),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info,
                                                                contentDescription = null,
                                                                tint = Color(0xFFDC2626),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = uiState.paymentMethodWarning!!,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF991B1B)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (uiState.availablePaymentMethods.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        uiState.availablePaymentMethods.forEach { method ->
                                                            val (label, containerColor) = when (method) {
                                                                PaymentMethod.YAPE -> "Yape" to Color(0xFF6A1B9A)
                                                                PaymentMethod.PLIN -> "Plin" to Color(0xFF00796B)
                                                                PaymentMethod.EFECTIVO -> "Efectivo" to Color(0xFF003366)
                                                                PaymentMethod.TRANSFERENCIA -> "Transferencia" to Color(0xFF0284C7)
                                                                PaymentMethod.OTRO -> "Otro" to Color(0xFF475569)
                                                            }
                                                            val isSelected = uiState.selectedPaymentMethod == method
                                                            FilterChip(
                                                                selected = isSelected,
                                                                onClick = { viewModel.selectPaymentMethod(method) },
                                                                label = {
                                                                    Text(
                                                                        label,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                    )
                                                                },
                                                                leadingIcon = if (isSelected) {
                                                                    {
                                                                        Icon(
                                                                            Icons.Default.CheckCircle,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                } else null,
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = containerColor,
                                                                    selectedLabelColor = Color.White,
                                                                    selectedLeadingIconColor = Color.White
                                                                ),
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    color = Color(0xFFF8FAFC),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = when (uiState.selectedPaymentMethod) {
                                                            PaymentMethod.YAPE, PaymentMethod.PLIN -> "Pagas al vendedor mediante código QR o número de celular al momento de la entrega en el campus."
                                                            PaymentMethod.EFECTIVO -> "Pagas en efectivo exacto al vendedor al recibir tus productos."
                                                            else -> "Coordinas el pago directamente con el vendedor al recibir tu entrega."
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF475569),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Mensaje de Error si existiera
                                        if (uiState.errorMessage != null) {
                                            Surface(
                                                color = Color(0xFFFEF2F2),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = uiState.errorMessage!!,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF991B1B)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Barra inferior FIJA en la parte de abajo con el Total y Botón Confirmar (igual que en Producto)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 6.dp,
                                        shadowElevation = 10.dp,
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "Total General (${uiState.calculation.totalItemCount} productos):",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Pago: ${uiState.selectedPaymentMethod.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "S/ %.2f".format(uiState.calculation.grandTotal),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF003366)
                                                )
                                            }

                                            SlideCommit(
                                                onConfirm = { viewModel.confirmOrder(buyerProfile) },
                                                enabled = uiState.canCheckout,
                                                isSubmitting = uiState.isSubmitting,
                                                hasError = uiState.errorMessage != null,
                                                isDone = uiState.placedOrder != null,
                                                label = "Desliza para confirmar pedido",
                                                doneLabel = "¡Pedido confirmado!",
                                                errorLabel = "Error al procesar pedido",
                                                trackColor = Color(0xFF003366),
                                                handleColor = Color(0xFFF8FAFC),
                                                successColor = Color(0xFF16A085),
                                                dangerColor = Color(0xFFDC2626),
                                                height = 54.dp,
                                                modifier = Modifier.fillMaxWidth()
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

        // Overlay elegante desenfocado / scrim para el diálogo emergente
        AnimatedVisibility(
            visible = isOrderConfirmed,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF001A33).copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun CartStepIndicator(
    currentStep: CartCheckoutStep,
    canProceedToPayment: Boolean,
    onStepClick: (CartCheckoutStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Paso 1: Producto
            val isStep1Active = currentStep == CartCheckoutStep.PRODUCTS
            val isStep1Done = currentStep == CartCheckoutStep.DELIVERY || currentStep == CartCheckoutStep.PAYMENT

            CartStepChip(
                stepNumber = 1,
                label = "Producto",
                isActive = isStep1Active,
                isDone = isStep1Done,
                onClick = { onStepClick(CartCheckoutStep.PRODUCTS) }
            )

            // Conector 1 -> 2
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .padding(horizontal = 2.dp)
                    .background(
                        if (currentStep == CartCheckoutStep.DELIVERY || currentStep == CartCheckoutStep.PAYMENT) Color(0xFF16A085)
                        else Color(0xFFCBD5E1)
                    )
            )

            // Paso 2: Entrega
            val isStep2Active = currentStep == CartCheckoutStep.DELIVERY
            val isStep2Done = currentStep == CartCheckoutStep.PAYMENT

            CartStepChip(
                stepNumber = 2,
                label = "Entrega",
                isActive = isStep2Active,
                isDone = isStep2Done,
                onClick = { onStepClick(CartCheckoutStep.DELIVERY) }
            )

            // Conector 2 -> 3
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .padding(horizontal = 2.dp)
                    .background(
                        if (currentStep == CartCheckoutStep.PAYMENT) Color(0xFF16A085)
                        else Color(0xFFCBD5E1)
                    )
            )

            // Paso 3: Pago
            val isStep3Active = currentStep == CartCheckoutStep.PAYMENT

            CartStepChip(
                stepNumber = 3,
                label = "Pago",
                isActive = isStep3Active,
                isDone = false,
                onClick = {
                    if (canProceedToPayment) {
                        onStepClick(CartCheckoutStep.PAYMENT)
                    }
                }
            )
        }
    }
}

@Composable
private fun CartStepChip(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isActive -> Color(0xFF003366)
            isDone -> Color(0xFFE2E8F0)
            else -> Color(0xFFF1F5F9)
        },
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isActive -> Color.White
                    isDone -> Color(0xFF16A085)
                    else -> Color(0xFF94A3B8)
                },
                modifier = Modifier.size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            color = if (isActive) Color(0xFF003366) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isActive -> Color.White
                    isDone -> Color(0xFF16A085)
                    else -> Color(0xFF64748B)
                },
                fontSize = 12.sp
            )
        }
    }
}

package com.example.campusgo.features.cart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campusgo.R
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.cart.components.CartCheckoutStep
import com.example.campusgo.features.cart.components.CartStepIndicator
import com.example.campusgo.features.cart.components.ClearCartDialog
import com.example.campusgo.features.cart.steps.CartDeliveryStep
import com.example.campusgo.features.cart.steps.CartPaymentStep
import com.example.campusgo.features.cart.steps.CartProductsStep
import com.example.campusgo.theme.LocalDarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    buyerProfile: UserProfile,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToTracking: () -> Unit = { onNavigateBack?.invoke() },
    onExploreStalls: (() -> Unit)? = null,
    onOpenChatForOrder: ((Order, SubOrder) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = koinViewModel()
) {
    val isDark = LocalDarkTheme.current
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

    LaunchedEffect(currentStep) {
        viewModel.clearError()
    }

    // Manejo del botón Atrás de Android
    BackHandler(enabled = currentStep != CartCheckoutStep.PRODUCTS) {
        currentStep = when (currentStep) {
            CartCheckoutStep.PAYMENT -> CartCheckoutStep.DELIVERY
            CartCheckoutStep.DELIVERY -> CartCheckoutStep.PRODUCTS
            CartCheckoutStep.PRODUCTS -> CartCheckoutStep.PRODUCTS
        }
    }

    var showClearCartDialog by remember { mutableStateOf(false) }
    val isAnyModalOpen = showClearCartDialog
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "cart_dialog_blur"
    )

    if (showClearCartDialog) {
        ClearCartDialog(
            onConfirm = { viewModel.clearCart() },
            onDismiss = { showClearCartDialog = false }
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
            onOpenChat = { subOrder ->
                val ord = order
                viewModel.clearPlacedOrder()
                if (onOpenChatForOrder != null) {
                    onOpenChatForOrder(ord, subOrder)
                } else {
                    onNavigateToTracking()
                }
            },
            onNavigateBack = {
                viewModel.clearPlacedOrder()
                onNavigateBack?.invoke() ?: onNavigateToTracking()
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = when (currentStep) {
                                CartCheckoutStep.PRODUCTS -> "Mi Carrito"
                                CartCheckoutStep.DELIVERY -> "Punto de Entrega"
                                CartCheckoutStep.PAYMENT -> "Método de Pago"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (currentStep != CartCheckoutStep.PRODUCTS || onNavigateBack != null) {
                            IconButton(
                                onClick = {
                                    when (currentStep) {
                                        CartCheckoutStep.PAYMENT -> currentStep = CartCheckoutStep.DELIVERY
                                        CartCheckoutStep.DELIVERY -> currentStep = CartCheckoutStep.PRODUCTS
                                        CartCheckoutStep.PRODUCTS -> onNavigateBack?.invoke()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
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
                        .padding(24.dp)
                        .padding(bottom = if (onNavigateBack == null) (80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cart_custom),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tu carrito está vacío",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Explora los puestos de tu campus y agrega tus antojos favoritos en una sola compra.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                onExploreStalls?.invoke() ?: onNavigateBack?.invoke() ?: onNavigateToTracking()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Explorar Puestos")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    val canProceedToPayment = uiState.canProceedToPayment

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

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            val isForward = targetState.ordinal > initialState.ordinal
                            if (isForward) {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
                                ) + fadeIn(animationSpec = tween(durationMillis = 350)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                            animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
                                        ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                    )
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
                                ) + fadeIn(animationSpec = tween(durationMillis = 350)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
                                        ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                    )
                            }
                        },
                        label = "cart_step_transition",
                        modifier = Modifier.weight(1f)
                    ) { step ->
                        when (step) {
                            CartCheckoutStep.PRODUCTS -> {
                                CartProductsStep(
                                    calculation = uiState.calculation,
                                    onClearCartClick = { showClearCartDialog = true },
                                    onIncrementItem = { id -> viewModel.incrementItem(id) },
                                    onDecrementItem = { id -> viewModel.decrementItem(id) },
                                    onContinueToDelivery = { currentStep = CartCheckoutStep.DELIVERY },
                                    onNavigateBack = onNavigateBack
                                )
                            }
                            CartCheckoutStep.DELIVERY -> {
                                CartDeliveryStep(
                                    uiState = uiState,
                                    onModifyProducts = { currentStep = CartCheckoutStep.PRODUCTS },
                                    onSetSplitDeliveryMode = { split -> viewModel.setSplitDeliveryMode(split) },
                                    onSelectMeetingPoint = { pt -> viewModel.selectMeetingPoint(pt) },
                                    onSelectSellerMeetingPoint = { sellerId, pt -> viewModel.selectSellerMeetingPoint(sellerId, pt) },
                                    onSelectTimeSlot = { slot -> viewModel.selectTimeSlot(slot) },
                                    onNotesChange = { notes -> viewModel.onNotesChange(notes) },
                                    onContinueToPayment = { currentStep = CartCheckoutStep.PAYMENT },
                                    onNavigateBack = onNavigateBack
                                )
                            }
                            CartCheckoutStep.PAYMENT -> {
                                CartPaymentStep(
                                    uiState = uiState,
                                    onModifyDelivery = { currentStep = CartCheckoutStep.DELIVERY },
                                    onModifyProducts = { currentStep = CartCheckoutStep.PRODUCTS },
                                    onSetSplitPaymentMode = { split -> viewModel.setSplitPaymentMode(split) },
                                    onSelectPaymentMethod = { method -> viewModel.selectPaymentMethod(method) },
                                    onSelectSellerPaymentMethod = { sellerId, method -> viewModel.selectSellerPaymentMethod(sellerId, method) },
                                    onConfirmOrder = { viewModel.confirmOrder(buyerProfile) },
                                    onClearError = { viewModel.clearError() },
                                    onNavigateBack = onNavigateBack
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay elegante desenfocado / scrim para el diálogo emergente
        AnimatedVisibility(
            visible = isAnyModalOpen,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
    }
}

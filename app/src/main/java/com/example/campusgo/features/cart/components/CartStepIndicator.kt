package com.example.campusgo.features.cart.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.theme.LocalDarkTheme

enum class CartCheckoutStep {
    PRODUCTS,
    DELIVERY,
    PAYMENT
}

@Composable
fun CartStepIndicator(
    currentStep: CartCheckoutStep,
    canProceedToPayment: Boolean,
    onStepClick: (CartCheckoutStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        .weight(1f, fill = false)
                        .widthIn(min = 14.dp, max = 28.dp)
                        .height(2.5.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            if (currentStep == CartCheckoutStep.DELIVERY || currentStep == CartCheckoutStep.PAYMENT) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
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
                        .weight(1f, fill = false)
                        .widthIn(min = 14.dp, max = 28.dp)
                        .height(2.5.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            if (currentStep == CartCheckoutStep.PAYMENT) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
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
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun CartStepChip(
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
            isActive -> MaterialTheme.colorScheme.primary
            isDone -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.onPrimary
                            isDone -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 11.sp
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    isActive -> MaterialTheme.colorScheme.onPrimary
                    isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CartModeToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isDark) 0.dp else (if (selected) 2.5.dp else 0.5.dp),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

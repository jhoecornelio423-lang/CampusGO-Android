package com.example.campusgo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class CodeSlotStatus {
    IDLE,
    ERROR,
    SUCCESS
}

/**
 * Componente CodeSlots en Jetpack Compose adaptado del diseño de React Bits.
 *
 * Características:
 * - Ranuras (*slots*) estilizadas con animación de caída/rebote al entrar dígitos.
 * - Cursor (*caret*) parpadeante en la ranura activa.
 * - Animación de sacudida (*shake*) ante código erróneo (ERROR).
 * - Fusión en color de éxito y check animado al completar el código correcto (SUCCESS).
 * - Teclado numérico nativo automático y accesible.
 */
@Composable
fun CodeSlots(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    status: CodeSlotStatus = CodeSlotStatus.IDLE,
    autoFocus: Boolean = false,
    slotSize: Dp = 56.dp,
    height: Dp = 64.dp,
    gap: Dp = 10.dp,
    radius: Dp = 14.dp,
    accentColor: Color = Color(0xFF00A884), // Verde CampusGO
    slotColor: Color = Color(0xFFF8FAFC),
    activeSlotColor: Color = Color(0xFFE6F7F3),
    digitColor: Color = Color(0xFF0F172A),
    dangerColor: Color = Color(0xFFEF4444),
    successColor: Color = Color(0xFF16A34A),
    onComplete: ((String) -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Animación de sacudida (shake) en caso de error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == CodeSlotStatus.ERROR) {
            for (i in 0..2) {
                shakeOffset.animateTo(12f, animationSpec = tween(50))
                shakeOffset.animateTo(-12f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    // Efecto de cursor parpadeante (caret)
    val infiniteTransition = rememberInfiniteTransition(label = "CodeSlotsCaret")
    val caretAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CaretBlink"
    )

    // Solicitar foco automático solo si se especifica explícitamente
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val activeIndex = value.length.coerceAtMost(length - 1)

    Box(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        contentAlignment = Alignment.Center
    ) {
        // Campo de texto transparente para gestionar eventos de teclado y accesibilidad
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(length)
                onValueChange(digitsOnly)
                if (digitsOnly.length == length) {
                    keyboardController?.hide()
                    onComplete?.invoke(digitsOnly)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
        )

        // Fila de Slots visuales
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until length) {
                val char = value.getOrNull(i)?.toString() ?: ""
                val isActive = i == activeIndex && status != CodeSlotStatus.SUCCESS && value.length < length
                val isFilled = char.isNotEmpty()

                CodeSlotItem(
                    index = i,
                    char = char,
                    isActive = isActive,
                    isFilled = isFilled,
                    status = status,
                    slotSize = slotSize,
                    height = height,
                    radius = radius,
                    slotColor = slotColor,
                    activeSlotColor = activeSlotColor,
                    accentColor = accentColor,
                    digitColor = digitColor,
                    dangerColor = dangerColor,
                    successColor = successColor,
                    caretAlpha = caretAlpha
                )
            }
        }
    }
}

@Composable
private fun CodeSlotItem(
    index: Int,
    char: String,
    isActive: Boolean,
    isFilled: Boolean,
    status: CodeSlotStatus,
    slotSize: Dp,
    height: Dp,
    radius: Dp,
    slotColor: Color,
    activeSlotColor: Color,
    accentColor: Color,
    digitColor: Color,
    dangerColor: Color,
    successColor: Color,
    caretAlpha: Float
) {
    val slotBgColor by animateColorAsState(
        targetValue = when (status) {
            CodeSlotStatus.SUCCESS -> Color(0xFFDCFCE7)
            CodeSlotStatus.ERROR -> Color(0xFFFEE2E2)
            CodeSlotStatus.IDLE -> when {
                isActive -> activeSlotColor
                isFilled -> Color.White
                else -> slotColor
            }
        },
        animationSpec = tween(200),
        label = "SlotBgColor_$index"
    )

    val slotBorderColor by animateColorAsState(
        targetValue = when (status) {
            CodeSlotStatus.SUCCESS -> successColor
            CodeSlotStatus.ERROR -> dangerColor
            CodeSlotStatus.IDLE -> when {
                isActive -> accentColor
                isFilled -> accentColor.copy(alpha = 0.5f)
                else -> Color(0xFFE2E8F0)
            }
        },
        animationSpec = tween(200),
        label = "SlotBorderColor_$index"
    )

    val borderWidth = if (isActive || status != CodeSlotStatus.IDLE || isFilled) 2.dp else 1.2.dp

    Box(
        modifier = Modifier
            .size(width = slotSize, height = height)
            .clip(RoundedCornerShape(radius))
            .background(slotBgColor)
            .border(
                BorderStroke(borderWidth, slotBorderColor),
                RoundedCornerShape(radius)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Contenido del dígito con animación de aparición
        androidx.compose.animation.AnimatedVisibility(
            visible = isFilled && status != CodeSlotStatus.SUCCESS,
            enter = scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.4f) + fadeOut()
        ) {
            Text(
                text = char,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (status == CodeSlotStatus.ERROR) dangerColor else digitColor
            )
        }

        // Ícono de éxito cuando el estado es SUCCESS
        androidx.compose.animation.AnimatedVisibility(
            visible = status == CodeSlotStatus.SUCCESS,
            enter = scaleIn(
                initialScale = 0.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(successColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verificado",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Caret (cursor) parpadeante en el slot activo cuando está vacío
        if (isActive && !isFilled && status == CodeSlotStatus.IDLE) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(accentColor.copy(alpha = caretAlpha))
            )
        }
    }
}

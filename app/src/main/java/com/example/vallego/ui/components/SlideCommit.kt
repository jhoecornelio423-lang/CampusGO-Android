package com.example.vallego.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Jetpack Compose translation of React Bits <SlideCommit /> component.
 *
 * Provides a responsive, physics-based horizontal slide-to-confirm button with:
 * - Pixel-perfect vertical centering for handle and direction arrow.
 * - Dynamic color palette: sleek deep midnight navy in idle state, vibrant gradient trail
 *   (Tech Blue -> Emerald) while dragging, and rich emerald green upon completion.
 * - Text label with smooth fade-out during travel.
 * - Physics spring bounce-back upon release before threshold.
 * - Haptic tactile feedback on commit threshold.
 */
@Composable
fun SlideCommit(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Desliza para confirmar pedido",
    doneLabel: String = "¡Pedido confirmado!",
    errorLabel: String = "Error al procesar pedido",
    onDone: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    isSubmitting: Boolean = false,
    hasError: Boolean = false,
    isDone: Boolean = false,
    enabled: Boolean = true,
    trackColor: Color = Color(0xFF0F1E36),
    handleColor: Color = Color(0xFFFFFFFF),
    successColor: Color = Color(0xFF059669),
    dangerColor: Color = Color(0xFFDC2626),
    width: Dp? = null,
    height: Dp = 54.dp,
    radius: Dp = 27.dp,
    returnBounce: Float = 0.38f,
    landingDip: Float = 0.026f,
    holdMs: Long = 1500L
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val trackPadding = 4.dp
    val handleSize = height - (trackPadding * 2)

    val offsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val trackShape = RoundedCornerShape(radius)
    val handleShape = CircleShape

    // Subtle pulsating glow for directional chevrons in idle state
    val infiniteTransition = rememberInfiniteTransition(label = "slideHintTransition")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    // Landing dip scale animation when dragging
    val handleScale by animateFloatAsState(
        targetValue = if (isDragging) (1f - landingDip * 2.2f).coerceAtLeast(0.94f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "handleScale"
    )

    val baseModifier = if (width != null) {
        modifier.width(width).height(height)
    } else {
        modifier.fillMaxWidth().height(height)
    }

    // Dynamic background brush according to current state
    val baseTrackBrush = when {
        isDone -> Brush.horizontalGradient(
            listOf(Color(0xFF047857), Color(0xFF10B981))
        )
        hasError -> Brush.horizontalGradient(
            listOf(Color(0xFF991B1B), Color(0xFFDC2626))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF0F1E36), Color(0xFF091424))
        )
    }

    val trackBorderColor by animateColorAsState(
        targetValue = when {
            isDone -> Color(0xFF34D399).copy(alpha = 0.8f)
            hasError -> Color(0xFFF87171).copy(alpha = 0.8f)
            isDragging -> Color(0xFF38BDF8).copy(alpha = 0.6f)
            else -> Color(0xFF1E3A5F).copy(alpha = 0.7f)
        },
        animationSpec = tween(300),
        label = "trackBorderColor"
    )

    BoxWithConstraints(
        modifier = baseModifier
            .clip(trackShape)
            .background(baseTrackBrush)
            .border(BorderStroke(1.dp, trackBorderColor), trackShape)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val handleSizePx = with(density) { handleSize.toPx() }
        val paddingPx = with(density) { trackPadding.toPx() }
        val maxTravelPx = (containerWidthPx - handleSizePx - (paddingPx * 2)).coerceAtLeast(1f)

        // React to isSubmitting: snap to end
        LaunchedEffect(isSubmitting) {
            if (isSubmitting) {
                offsetX.animateTo(
                    targetValue = maxTravelPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        // React to isDone: snap to end and notify onDone
        LaunchedEffect(isDone) {
            if (isDone) {
                offsetX.snapTo(maxTravelPx)
                onDone?.invoke()
            }
        }

        // React to hasError: pause for holdMs then bounce back to 0
        LaunchedEffect(hasError) {
            if (hasError) {
                onError?.invoke(errorLabel)
                delay(holdMs)
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = returnBounce.coerceIn(0.2f, 0.8f),
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // React to enabled: if disabled and not submitting, reset
        LaunchedEffect(enabled) {
            if (!enabled && !isSubmitting && !isDone) {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = returnBounce.coerceIn(0.2f, 0.8f),
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        val progress = if (maxTravelPx > 0f) (offsetX.value / maxTravelPx).coerceIn(0f, 1f) else 0f

        // Energetic glowing trail following the handle as it slides
        if (progress > 0.01f && !isDone && !hasError) {
            val activeFillWidthDp = with(density) {
                (paddingPx * 2 + handleSizePx + offsetX.value).toDp()
            }
            Box(
                modifier = Modifier
                    .width(activeFillWidthDp)
                    .fillMaxHeight()
                    .clip(trackShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0284C7).copy(alpha = 0.45f),
                                Color(0xFF0EA5E9).copy(alpha = 0.75f),
                                Color(0xFF10B981).copy(alpha = 0.90f)
                            )
                        )
                    )
            )
        }

        // Centered text label that smoothly fades out as the handle slides forward
        val textAlpha = when {
            isDone || hasError || isSubmitting -> 1f
            else -> (1f - progress * 2.2f).coerceIn(0f, 1f)
        }

        val displayText = when {
            isDone -> doneLabel
            hasError -> errorLabel
            isSubmitting -> "Confirmando pedido..."
            else -> label
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = handleSize + 12.dp, end = 16.dp)
                .alpha(textAlpha),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                color = Color(0xFFF1F5F9),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic direction hint chevrons when idle
            if (!isSubmitting && !isDone && !hasError && progress < 0.15f) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "›››",
                    color = Color(0xFF38BDF8).copy(alpha = hintAlpha),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Draggable Handle / Thumb (Centered vertically with pixel precision: y = 0)
        val canDrag = enabled && !isSubmitting && !isDone && !hasError

        val handleBorderColor by animateColorAsState(
            targetValue = when {
                isDone || progress >= 0.78f -> Color(0xFF10B981)
                isDragging -> Color(0xFF38BDF8)
                else -> Color(0xFFE2E8F0)
            },
            animationSpec = tween(200),
            label = "handleBorderColor"
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = (paddingPx + offsetX.value).roundToInt(),
                        y = 0 // Exactly centered vertically with equal top/bottom padding!
                    )
                }
                .size(handleSize)
                .scale(handleScale)
                .shadow(
                    elevation = 6.dp,
                    shape = handleShape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(handleShape)
                .background(handleColor)
                .border(BorderStroke(1.5.dp, handleBorderColor), handleShape)
                .pointerInput(canDrag, maxTravelPx) {
                    if (!canDrag) return@pointerInput

                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            coroutineScope.launch {
                                val currentOffset = offsetX.value
                                val commitThreshold = maxTravelPx * 0.80f

                                if (currentOffset >= commitThreshold) {
                                    // Committed! Haptic vibration + smooth snap to end + trigger onConfirm
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(
                                        targetValue = maxTravelPx,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onConfirm()
                                } else {
                                    // Released before threshold: bouncy return to 0
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = returnBounce.coerceIn(0.2f, 0.8f),
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = returnBounce.coerceIn(0.2f, 0.8f),
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val nextVal = (offsetX.value + dragAmount).coerceIn(0f, maxTravelPx)
                            coroutineScope.launch {
                                offsetX.snapTo(nextVal)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val arrowTint by animateColorAsState(
                targetValue = when {
                    progress >= 0.78f -> Color(0xFF059669) // turns green when nearing commit threshold
                    isDragging -> Color(0xFF0284C7)        // tech blue while sliding
                    else -> Color(0xFF0F1E36)              // deep navy when idle
                },
                animationSpec = tween(150),
                label = "arrowTint"
            )

            when {
                isSubmitting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF0284C7)
                    )
                }
                isDone -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmado",
                        tint = successColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                hasError -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning_custom),
                        contentDescription = "Error",
                        tint = dangerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Deslizar para confirmar",
                        tint = arrowTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

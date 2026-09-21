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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * - Rounded pill track
 * - Draggable thumb/handle with return bounce animation (customizable via returnBounce)
 * - Text label with fade-out during travel
 * - Loading indicator during isSubmitting
 * - Done and Error feedback states
 * - Haptic tactile feedback on commit threshold
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
    trackColor: Color = Color(0xFF003366),
    handleColor: Color = Color(0xFFF8FAFC),
    successColor: Color = Color(0xFF16A085),
    dangerColor: Color = Color(0xFFE5484D),
    width: Dp? = null,
    height: Dp = 56.dp,
    radius: Dp = 28.dp,
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

    // Dynamic track color based on state
    val targetTrackColor = when {
        isDone -> successColor
        hasError -> dangerColor
        else -> trackColor
    }
    val animatedTrackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = tween(350),
        label = "animatedTrackColor"
    )

    // Gentle shimmer effect for guiding arrows when idle
    val infiniteTransition = rememberInfiniteTransition(label = "slideHint")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    // Landing dip scale animation when dragging
    val handleScale by animateFloatAsState(
        targetValue = if (isDragging) (1f - landingDip * 2f).coerceAtLeast(0.92f) else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "handleScale"
    )

    val baseModifier = if (width != null) {
        modifier.width(width).height(height)
    } else {
        modifier.fillMaxWidth().height(height)
    }

    BoxWithConstraints(
        modifier = baseModifier
            .clip(trackShape)
            .background(animatedTrackColor)
            .border(
                BorderStroke(
                    1.dp,
                    if (isDone) successColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                ),
                trackShape
            )
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

        // React to isDone: animate to end and notify onDone
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

        // Highlight trail following the handle
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
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
        )

        // Centered text label that fades out as the handle slides
        val textAlpha = when {
            isDone || hasError || isSubmitting -> 1f
            else -> (1f - progress * 1.8f).coerceIn(0f, 1f)
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
                .padding(horizontal = handleSize + 8.dp)
                .alpha(textAlpha),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Idle direction hints (chevrons)
            if (!isSubmitting && !isDone && !hasError && progress < 0.2f) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = hintAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Draggable Handle / Thumb
        val canDrag = enabled && !isSubmitting && !isDone && !hasError

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (paddingPx + offsetX.value).roundToInt(),
                        y = paddingPx.roundToInt()
                    )
                }
                .size(handleSize)
                .scale(handleScale)
                .shadow(elevation = 4.dp, shape = handleShape)
                .clip(handleShape)
                .background(handleColor)
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
                                val commitThreshold = maxTravelPx * 0.82f

                                if (currentOffset >= commitThreshold) {
                                    // Committed! Haptic vibration + snap to end + trigger confirm
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
                                    // Release before threshold: bounce back to 0
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
            when {
                isSubmitting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = trackColor
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = dangerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Deslizar",
                        tint = trackColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

package com.example.vallego.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Jetpack Compose translation of React Bits <PeekRating /> component.
 *
 * Provides a playful, animated rating control featuring:
 * - Dynamic glyph lift when previewed/hovered
 * - Magnification on active glyph under touch
 * - Floating tip bubble with semantic labels that smoothly tracks the pointer
 * - Pop bounce animation upon commit
 * - Responsive touch drag gesture tracking with haptic clicks
 */
@Composable
fun PeekRating(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 5,
    labels: List<String> = listOf("Muy malo", "Regular", "Bueno", "Muy bueno", "¡Excelente!"),
    activeColor: Color = Color(0xFFF5B400),
    idleColor: Color = Color(0xFFCBD5E1),
    tipColor: Color = Color(0xFF27272A),
    tipTextColor: Color = Color(0xFFF5F5F5),
    size: Dp = 36.dp,
    lift: Dp = 8.dp,
    magnify: Float = 1.20f,
    riseDuration: Int = 280,
    popScale: Float = 1.35f,
    showTip: Boolean = true,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // Animatable for the pop animation on commit
    val popAnimatables = remember(count) {
        List(count) { Animatable(1f) }
    }

    val triggerPop: (Int) -> Unit = { index ->
        if (index in 0 until count) {
            coroutineScope.launch {
                popAnimatables[index].animateTo(
                    targetValue = popScale,
                    animationSpec = tween(120, easing = FastOutSlowInEasing)
                )
                popAnimatables[index].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    val tipRoom = if (showTip) lift + 28.dp else lift + 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(size + tipRoom)
                .onGloballyPositioned { coordinates ->
                    totalWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(enabled, count, totalWidthPx) {
                    if (!enabled || totalWidthPx <= 0f) return@pointerInput

                    val slotWidthPx = totalWidthPx / count.toFloat()

                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = (offset.x / slotWidthPx).toInt().coerceIn(0, count - 1)
                            if (hoverIndex != idx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hoverIndex = idx
                            }
                        },
                        onDragEnd = {
                            hoverIndex?.let { finalIdx ->
                                val finalValue = finalIdx + 1
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                triggerPop(finalIdx)
                                onChange(finalValue)
                            }
                            hoverIndex = null
                        },
                        onDragCancel = {
                            hoverIndex = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = (change.position.x / slotWidthPx).toInt().coerceIn(0, count - 1)
                            if (hoverIndex != idx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hoverIndex = idx
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Floating Tip Bubble
            if (showTip && hoverIndex != null && totalWidthPx > 0f) {
                val currentHover = hoverIndex!!
                val slotWidthPx = totalWidthPx / count.toFloat()
                val targetCenterXPx = (currentHover + 0.5f) * slotWidthPx

                val animatedTipXPx by animateFloatAsState(
                    targetValue = targetCenterXPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tipX"
                )

                val labelText = labels.getOrElse(currentHover) { "${currentHover + 1}" }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                x = (animatedTipXPx - 50.dp.toPx()).roundToInt(),
                                y = 0
                            )
                        }
                        .size(width = 100.dp, height = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = tipColor,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = labelText,
                            color = tipTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Star Glyphs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 0 until count) {
                    val isPreviewing = hoverIndex != null
                    val isLit = if (isPreviewing) i <= hoverIndex!! else i < value
                    val isHoveredStar = hoverIndex == i

                    val targetLift = if (isPreviewing && i <= hoverIndex!!) -lift else 0.dp
                    val animatedLift by animateDpAsState(
                        targetValue = targetLift,
                        animationSpec = tween(durationMillis = riseDuration, easing = FastOutSlowInEasing),
                        label = "starLift"
                    )

                    val dynamicScale = if (isHoveredStar) magnify else popAnimatables[i].value

                    val starColor = if (isLit) activeColor else idleColor

                    Box(
                        modifier = Modifier
                            .offset(y = animatedLift)
                            .scale(dynamicScale)
                            .size(size + 12.dp)
                            .clip(CircleShape)
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val nextVal = i + 1
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                triggerPop(i)
                                onChange(nextVal)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Estrella ${i + 1}",
                            tint = starColor,
                            modifier = Modifier.size(size)
                        )
                    }
                }
            }
        }
    }
}

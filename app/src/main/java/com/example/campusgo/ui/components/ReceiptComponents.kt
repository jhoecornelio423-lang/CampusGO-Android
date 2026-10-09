package com.example.campusgo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.PaymentMethod
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Divisor visual con efecto de línea discontinua / punteada estilo boleta digital (Fintech Ticket).
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFCBD5E1),
    thickness: Dp = 1.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}

/**
 * Representación estética de código de barras simulado para el ticket fintech.
 */
@Composable
fun StylizedBarcode(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth(0.70f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pattern = listOf(3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 4, 1, 2, 3, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 4, 1)
        pattern.forEach { barWidth ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(barWidth.dp)
                    .background(Color(0xFF334155).copy(alpha = 0.85f))
            )
        }
    }
}

/**
 * Insignia visual moderna para el método de pago empleado en la transacción.
 */
@Composable
fun PaymentMethodPill(
    method: PaymentMethod?,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor) = when (method) {
        PaymentMethod.YAPE -> Triple("Yape", Color(0xFFEDE9FE), Color(0xFF6D28D9))
        PaymentMethod.PLIN -> Triple("Plin", Color(0xFFE0F2FE), Color(0xFF0284C7))
        PaymentMethod.EFECTIVO -> Triple("Efectivo", Color(0xFFECFDF5), Color(0xFF059669))
        else -> Triple(method?.name ?: "Efectivo", Color(0xFFF1F5F9), Color(0xFF334155))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PaymentMethodLogo(method = method, size = 18.dp)
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Formateador de fecha y hora local con fallback seguro para comprobantes digitales.
 */
fun formatReceiptOrderDate(createdAtIso: String?): String {
    val localePe = Locale.forLanguageTag("es-PE")
    if (createdAtIso.isNullOrBlank()) {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a", localePe)
        return now.format(formatter)
    }
    return try {
        val instant = Instant.parse(createdAtIso)
        val zone = ZoneId.systemDefault()
        val dt = instant.atZone(zone)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a", localePe)
        dt.format(formatter).replace(". ", " ").replace(".", "").uppercase()
    } catch (e: Exception) {
        createdAtIso.take(16).replace("T", " ")
    }
}

package com.example.campusgo.features.buyer.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.campusgo.R
import kotlinx.coroutines.delay

/**
 * Modelo para cada Flyer / Anuncio / Publicidad en el carrusel de inicio.
 *
 * MEDIDAS RECOMENDADAS PARA DISEÑO DE IMÁGENES / FLYERS:
 * - Resolución recomendada (estándar): 1080 x 480 px (Relación 9:4 / ~2.25:1)
 * - Resolución de alta calidad usada: 1881 x 836 px (Relación exacta 2.25:1)
 */
data class CampusFlyer(
    val id: String,
    val tag: String = "",
    val title: String = "",
    val subtitle: String = "",
    @param:DrawableRes val imageRes: Int? = null,
    val imageUrl: String? = null,
    val gradientColors: List<Color> = listOf(Color(0xFF00A884), Color(0xFF0F766E)),
    val actionText: String = "Ver más",
    val showOverlayText: Boolean = false,
    val onClick: (() -> Unit)? = null
)

val defaultCampusFlyers = listOf(
    CampusFlyer(
        id = "flyer_vendedor",
        tag = "EMPRENDEDORES",
        title = "¡Súmate como Vendedor!",
        subtitle = "Ofrece tus productos y llega a más estudiantes",
        imageRes = R.drawable.flyer1,
        gradientColors = listOf(Color(0xFF00A884), Color(0xFF0F766E)),
        actionText = "Unirme",
        showOverlayText = false
    ),
    CampusFlyer(
        id = "flyer_rifa",
        tag = "SORTEO",
        title = "¡Rifa de Bienvenida!",
        subtitle = "Participa y gana increíbles premios",
        imageRes = R.drawable.flyer2,
        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        actionText = "Participar",
        showOverlayText = false
    ),
    CampusFlyer(
        id = "flyer_kodex",
        tag = "KODEX TI",
        title = "Tu aliado tecnológico",
        subtitle = "Desarrollamos aplicativos y sistemas web",
        imageRes = R.drawable.flyer3,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF4338CA)),
        actionText = "Saber más",
        showOverlayText = false
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CampusFlyerCarousel(
    modifier: Modifier = Modifier,
    flyers: List<CampusFlyer> = defaultCampusFlyers,
    onFlyerClick: ((CampusFlyer) -> Unit)? = null
) {
    if (flyers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { flyers.size })

    // Auto-avance suave del carrusel cada 5 segundos si el usuario no está arrastrando
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && flyers.size > 1) {
            while (true) {
                delay(5000L)
                val nextPage = (pagerState.currentPage + 1) % flyers.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 650)
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Carrusel Horizontal de Flyers que se desliza hasta el límite de la pantalla
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 14.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val flyer = flyers[page]
            FlyerCard(
                flyer = flyer,
                onClick = {
                    flyer.onClick?.invoke()
                    onFlyerClick?.invoke(flyer)
                }
            )
        }

        // Indicador de Puntos (Dots Indicator) animado
        if (flyers.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(flyers.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 6.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF00A884)
                                else Color(0xFFCBD5E1)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FlyerCard(
    flyer: CampusFlyer,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.05f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Imagen Local (Drawable), Remota (URL) o Gradiente de Respaldo
            when {
                flyer.imageRes != null -> {
                    Image(
                        painter = painterResource(id = flyer.imageRes),
                        contentDescription = flyer.title.ifBlank { flyer.tag },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                !flyer.imageUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(flyer.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = flyer.title.ifBlank { flyer.tag },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // Fondo con gradiente moderno y formas decorativas sutiles
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(flyer.gradientColors))
                    ) {
                        // Círculo decorativo transparente
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .offset(x = 190.dp, y = (-20).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }

            // 2. Overlay de texto (Solo si showOverlayText está activo o si no hay imagen de flyer)
            if (flyer.showOverlayText || (flyer.imageRes == null && flyer.imageUrl.isNullOrBlank())) {
                if (flyer.imageRes != null || !flyer.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.70f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Contenido textual del Flyer
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (flyer.tag.isNotBlank()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = flyer.tag,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    if (flyer.title.isNotBlank() || flyer.subtitle.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (flyer.title.isNotBlank()) {
                                Text(
                                    text = flyer.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (flyer.subtitle.isNotBlank()) {
                                Text(
                                    text = flyer.subtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (flyer.actionText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = flyer.actionText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

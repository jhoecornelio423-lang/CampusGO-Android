package com.example.vallego.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.ui.components.SetDarkScreenStatusBar

@Composable
fun WelcomeScreen(
    onStartRegister: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetDarkScreenStatusBar(isDark = true)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF022B34), // Verde azulado oscuro superior
                        Color(0xFF044550), // Verde azulado medio
                        Color(0xFF09726D)  // Esmeralda inferior
                    )
                )
            )
    ) {
        // =========================================================================
        // CÍRCULOS TRANSLÚCIDOS DE FONDO CON RELLENO (SIN CONTORNOS BLANCOS)
        // =========================================================================

        // 1. Círculo grande superior derecho (relleno oscuro translúcido que pasa detrás del birrete)
        Box(
            modifier = Modifier
                .size(460.dp)
                .align(Alignment.TopEnd)
                .offset(x = 85.dp, y = 0.dp)
                .clip(CircleShape)
                .background(Color(0x44000000))
        )

        // 2. Círculo lateral izquierdo (relleno translúcido detrás de alas y mochila)
        Box(
            modifier = Modifier
                .size(390.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-130).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color(0x30000000))
        )

        // =========================================================================
        // CONTENIDO PRINCIPAL: MASCOTA FLOTANTE Y TARJETA INFERIOR
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // SECCIÓN SUPERIOR: Mascota Campus Go centrada armónicamente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mascota_campusgo),
                    contentDescription = "Mascota Campus Go",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(290.dp)
                        .padding(horizontal = 8.dp)
                )
            }

            // SECCIÓN INFERIOR: Tarjeta blanca flotante con bordes redondeados
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(34.dp),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título y Subtítulo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "¿Listo para empezar?",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF102A43)
                        )
                        Text(
                            text = "Crea tu cuenta en pocos pasos o ingresa si ya eres parte de Campus Go.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF627D98),
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Botón Principal: Comenzar -> (lleva al registro)
                    Button(
                        onClick = onStartRegister,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A884),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = Color(0xFF00A884),
                                ambientColor = Color(0x3300A884)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Comenzar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Enlace secundario: ¿Ya tienes una cuenta? Iniciar sesión
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Ya tienes una cuenta? ",
                            fontSize = 13.sp,
                            color = Color(0xFF627D98)
                        )
                        Text(
                            text = "Iniciar sesión",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884),
                            modifier = Modifier.clickable { onLogin() }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Respaldo KODEX y versión beta
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CON EL RESPALDO DE  ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Image(
                                painter = painterResource(id = R.drawable.kodex_logo),
                                contentDescription = "Logo Kodex",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "KODEX",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF102A43),
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = "Versión Beta v${com.example.vallego.BuildConfig.VERSION_NAME.removeSuffix("-beta")}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0AEC0)
                        )
                    }
                }
            }
        }
    }
}

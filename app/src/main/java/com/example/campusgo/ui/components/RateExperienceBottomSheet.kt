package com.example.campusgo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.campusgo.theme.LocalDarkTheme

@Composable
fun RateExperienceBottomSheet(
    title: String = "Calificar Experiencia",
    subtitle: String = "",
    targetName: String,
    targetAvatarUrl: String? = null,
    targetRoleLabel: String = "",
    isStore: Boolean = false,
    promptText: String = "¿Cómo estuvo tu experiencia?",
    commentPlaceholder: String = "¿Algún comentario adicional? (Opcional)",
    submitButtonText: String = "Cerrar y Calificar ?",
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String?) -> Unit
) {
    val isDark = LocalDarkTheme.current
    var selectedStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Cabecera superior moderna con botón de regreso y cerrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { if (!isSubmitting) onDismiss() },
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Contenido interactivo centrado y con scroll nativo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Tarjeta central de la persona o puesto a calificar
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isStore) {
                            CampusGoBusinessAvatar(
                                avatarUrl = targetAvatarUrl,
                                storeName = targetName,
                                size = 52.dp,
                                shape = CircleShape
                            )
                        } else {
                            CampusGoUserAvatar(
                                avatarUrl = targetAvatarUrl,
                                name = targetName,
                                size = 52.dp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = targetName.ifBlank { "Campus GO" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16324F),
                                textAlign = TextAlign.Center
                            )
                            if (targetRoleLabel.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFE6F7F3),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = targetRoleLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A884),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = promptText,
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        // Fila animada interactiva PeekRating (1 a 5 estrellas)
                        Box(
                            modifier = Modifier.padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PeekRating(
                                value = selectedStars,
                                onChange = { selectedStars = it },
                                size = 36.dp,
                                lift = 7.dp,
                                magnify = 1.2f,
                                popScale = 1.35f,
                                activeColor = Color(0xFFF59E0B),
                                idleColor = Color(0xFFE2E8F0),
                                enabled = !isSubmitting
                            )
                        }

                        // Badge dinámico según estrellas
                        val (reactionText, reactionColor, reactionBg) = when (selectedStars) {
                            5 -> Triple("¡Excelente experiencia! ✨", Color(0xFFB45309), Color(0xFFFEF3C7))
                            4 -> Triple("Muy buena atención 👍", Color(0xFF15803D), Color(0xFFDCFCE7))
                            3 -> Triple("Buena atención 👌", Color(0xFF0369A1), Color(0xFFE0F2FE))
                            2 -> Triple("Atención regular 😐", Color(0xFFC2410C), Color(0xFFFFEDD5))
                            else -> Triple("Mala experiencia 🙁", Color(0xFFB91C1C), Color(0xFFFEE2E2))
                        }

                        Surface(
                            color = reactionBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = reactionText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = reactionColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Campo de comentario opcional (limpio, directo y estilizado)
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = {
                        Text(
                            text = commentPlaceholder,
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF00A884),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Botón Principal de Enviar Calificación
                Button(
                    onClick = { onSubmit(selectedStars, comment.takeIf { it.isNotBlank() }) },
                    enabled = selectedStars in 1..5 && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A884),
                        disabledContainerColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = submitButtonText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
}

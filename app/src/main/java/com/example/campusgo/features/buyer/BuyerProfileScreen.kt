package com.example.campusgo.features.buyer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.BuildConfig
import com.example.campusgo.R
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.OfficialWarningBanner
import com.example.campusgo.ui.components.ProfileInfoBottomSheet
import com.example.campusgo.ui.components.ProfileInfoType
import com.example.campusgo.ui.components.ValleGoUserAvatar
import com.example.campusgo.ui.components.compressImageUri
import com.example.campusgo.ui.components.formatAccountCreationDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerProfileScreen(
    profile: UserProfile,
    warnings: List<ProfileWarning> = emptyList(),
    onNavigateBack: (() -> Unit)? = null,
    onSaveProfile: (UserProfile) -> Unit,
    onUploadAvatar: (ByteArray, (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var studentCode by remember(profile) { mutableStateOf(profile.studentCode.orEmpty()) }
    var campus by remember(profile) { mutableStateOf(profile.campus) }
    var avatarUrl by remember(profile) { mutableStateOf(profile.avatarUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var selectedInfoType by remember { mutableStateOf(ProfileInfoType.NONE) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = compressImageUri(context, uri)
            if (bytes != null) {
                isUploading = true
                onUploadAvatar(bytes) { newUrl ->
                    isUploading = false
                    avatarUrl = newUrl
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        spotColor = Color(0x1F16324F),
                        ambientColor = Color(0x2816324F),
                        clip = false
                    ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) "Editar Mi Perfil" else "Mi Perfil",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F)
                        )
                    },
                    navigationIcon = {
                        if (isEditMode) {
                            IconButton(onClick = {
                                fullName = profile.fullName
                                phone = profile.phone
                                studentCode = profile.studentCode.orEmpty()
                                campus = profile.campus
                                avatarUrl = profile.avatarUrl
                                isEditMode = false
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás",
                                    tint = Color(0xFF16324F)
                                )
                            }
                        } else if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás",
                                    tint = Color(0xFF16324F)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isEditMode) {
                            FilledTonalButton(
                                onClick = { isEditMode = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE6F7F3),
                                    contentColor = Color(0xFF00A884)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_user_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OfficialWarningBanner(
                warnings = warnings,
                isSeller = false
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val hasAvatarPhoto = !avatarUrl.isNullOrBlank()
                        Box(
                            modifier = if (!isEditMode && hasAvatarPhoto) Modifier.clip(CircleShape).clickable { showEnlargedPhoto = true } else Modifier
                        ) {
                            ValleGoUserAvatar(
                                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() },
                                name = fullName,
                                size = 96.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00A884),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Cambiar foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isEditMode) {
                        Text(
                            text = "Toca la cámara para cambiar tu foto de perfil",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F)
                        )

                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_school_cap),
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estudiante / Comprador CampusGO",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "INFORMACIÓN DE LA CUENTA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    if (!isEditMode) {
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_user_circle_custom),
                            label = "Nombre Completo",
                            value = fullName.ifBlank { "No registrado" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_phone_custom),
                            label = "Teléfono",
                            value = phone.ifBlank { "No registrado" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_location_custom),
                            label = "Campus Universitario",
                            value = "Campus $campus"
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_account_created_custom),
                            label = "Fecha de Creación de Cuenta",
                            value = formatAccountCreationDate(profile.createdAt)
                        )
                    } else {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nombre Completo") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_user_circle_custom),
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_phone_custom),
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = campus,
                            onValueChange = { campus = it },
                            label = { Text("Campus") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Sección: MÁS INFORMACIÓN
            if (!isEditMode) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "MÁS INFORMACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        // 1. Términos y condiciones
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_terms_custom),
                            iconTint = Color(0xFF00A884),
                            iconBg = Color(0xFFE6F7F3),
                            title = "Términos y condiciones",
                            subtitle = "Normas de uso del servicio y pedidos",
                            onClick = { selectedInfoType = ProfileInfoType.TERMS }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // 2. Políticas de privacidad
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_privacy_custom),
                            iconTint = Color(0xFF2563EB),
                            iconBg = Color(0xFFEFF6FF),
                            title = "Políticas de privacidad",
                            subtitle = "Tratamiento y protección de tus datos",
                            onClick = { selectedInfoType = ProfileInfoType.PRIVACY }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // 3. Botón de Ayuda
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_help_headset_custom),
                            iconTint = Color(0xFFD97706),
                            iconBg = Color(0xFFFEF3C7),
                            title = "Ayuda",
                            subtitle = "Preguntas frecuentes y soporte",
                            onClick = { selectedInfoType = ProfileInfoType.HELP }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // 4. Quiero ser aliado Campus GO
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_store_custom),
                            iconTint = Color(0xFF7C3AED),
                            iconBg = Color(0xFFF5F3FF),
                            title = "Quiero ser aliado Campus GO",
                            subtitle = "Vende tus productos en la comunidad universitaria",
                            onClick = {
                                Toast.makeText(context, "Por el momento no está disponible", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Botones de acción en la parte inferior del perfil
            if (isEditMode) {
                Button(
                    onClick = {
                        val updated = profile.copy(
                            fullName = fullName.trim().ifBlank { profile.fullName },
                            phone = phone.trim(),
                            studentCode = profile.studentCode,
                            campus = campus.trim().ifBlank { profile.campus },
                            avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() }
                        )
                        onSaveProfile(updated)
                        isEditMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = {
                        fullName = profile.fullName
                        phone = profile.phone
                        studentCode = profile.studentCode.orEmpty()
                        campus = profile.campus
                        avatarUrl = profile.avatarUrl
                        isEditMode = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Cancelar Edición", fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout_custom),
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Versión de la app y créditos de autoría
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CampusGO • Versión ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Hecho con 💚 por Jhoe Cornelio y Aldo Torres",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }
    }

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = avatarUrl,
            name = fullName,
            roleDescription = "Estudiante / Comprador • Campus $campus",
            onDismiss = { showEnlargedPhoto = false }
        )
    }

    if (selectedInfoType != ProfileInfoType.NONE) {
        ProfileInfoBottomSheet(
            type = selectedInfoType,
            onDismiss = { selectedInfoType = ProfileInfoType.NONE }
        )
    }
}

@Composable
private fun ProfileInfoNavigationRow(
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16324F)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = Color(0xFF00A884),
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 1.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00A884),
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

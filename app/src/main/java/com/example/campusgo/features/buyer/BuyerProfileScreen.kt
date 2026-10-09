package com.example.campusgo.features.buyer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.ThemeManager
import com.example.campusgo.theme.extendedColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.core.util.FormValidators
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import com.example.campusgo.BuildConfig
import com.example.campusgo.R
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.seller.components.SellerNotificationsBottomSheet
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.OfficialWarningBanner
import com.example.campusgo.ui.components.ProfileInfoBottomSheet
import com.example.campusgo.ui.components.ProfileInfoType
import com.example.campusgo.ui.components.StrikeBadge
import com.example.campusgo.ui.components.CampusGoUserAvatar
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
    var showStrikesSheet by remember { mutableStateOf(false) }
    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var studentCode by remember(profile) { mutableStateOf(profile.studentCode.orEmpty()) }
    var campus by remember(profile) { mutableStateOf(profile.campus) }
    var avatarUrl by remember(profile) { mutableStateOf(profile.avatarUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var selectedInfoType by remember { mutableStateOf(ProfileInfoType.NONE) }
    val context = LocalContext.current

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var campusError by remember { mutableStateOf<String?>(null) }
    var showValidationErrors by remember { mutableStateOf(false) }

    fun resetEditFields() {
        fullName = profile.fullName
        phone = profile.phone
        studentCode = profile.studentCode.orEmpty()
        campus = profile.campus
        avatarUrl = profile.avatarUrl
        fullNameError = null
        phoneError = null
        campusError = null
        showValidationErrors = false
        isEditMode = false
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
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
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) "Editar Mi Perfil" else "Mi Perfil",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (isEditMode) {
                            IconButton(onClick = { resetEditFields() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isEditMode) {
                            FilledTonalButton(
                                onClick = { isEditMode = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
        containerColor = MaterialTheme.colorScheme.background
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            CampusGoUserAvatar(
                                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() },
                                name = fullName,
                                size = 96.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Cambiar foto",
                                            tint = MaterialTheme.colorScheme.onPrimary,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val badgeBg = MaterialTheme.extendedColors.successContainer
                        val badgeText = MaterialTheme.extendedColors.onSuccessContainer
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(0.75.dp, badgeText.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_school_cap),
                                    contentDescription = null,
                                    tint = badgeText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estudiante / Comprador CampusGO",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    if (!isEditMode) {
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_phone_custom),
                            label = "Teléfono",
                            value = phone.ifBlank { "No registrado" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_location_custom),
                            label = "Campus Universitario",
                            value = "Campus $campus"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileDetailRow(
                            iconPainter = painterResource(id = R.drawable.ic_account_created_custom),
                            label = "Fecha de Creación de Cuenta",
                            value = formatAccountCreationDate(profile.createdAt)
                        )
                    } else {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                if (showValidationErrors) fullNameError = FormValidators.validateFullName(it)
                            },
                            label = { Text("Nombre Completo *") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_user_circle_custom),
                                    contentDescription = null,
                                    tint = if (fullNameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            isError = fullNameError != null,
                            supportingText = fullNameError?.let { msg -> { Text(text = msg, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(9)
                                phone = digits
                                if (showValidationErrors) phoneError = FormValidators.validatePhone(digits)
                            },
                            label = { Text("Teléfono (9 dígitos) *") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_phone_custom),
                                    contentDescription = null,
                                    tint = if (phoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            isError = phoneError != null,
                            supportingText = phoneError?.let { msg -> { Text(text = msg, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = campus,
                            onValueChange = {
                                campus = it
                                if (showValidationErrors) campusError = FormValidators.validateCampus(it)
                            },
                            label = { Text("Campus Universitario *") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                    contentDescription = null,
                                    tint = if (campusError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            isError = campusError != null,
                            supportingText = campusError?.let { msg -> { Text(text = msg, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Sección: MÁS INFORMACIÓN
            if (!isEditMode) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            text = "PREFERENCIAS Y AJUSTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        // 0. Modo Oscuro
                        val isDarkMode by ThemeManager.isDarkMode.collectAsState()
                        ProfileInfoNavigationRow(
                            icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            iconTint = if (isDarkMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDarkMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            title = "Modo Oscuro",
                            subtitle = if (isDarkMode) "Activado • Tema nocturno visual" else "Desactivado • Tema claro visual",
                            showChevron = false,
                            trailing = {
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { ThemeManager.setDarkMode(context, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            },
                            onClick = { ThemeManager.toggleDarkMode(context) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "MÁS INFORMACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        // 1. Términos y condiciones
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_terms_custom),
                            iconTint = MaterialTheme.extendedColors.success,
                            iconBg = MaterialTheme.extendedColors.successContainer,
                            title = "Términos y condiciones",
                            subtitle = "Normas de uso del servicio y pedidos",
                            onClick = { selectedInfoType = ProfileInfoType.TERMS }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 2. Políticas de privacidad
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_privacy_custom),
                            iconTint = MaterialTheme.extendedColors.info,
                            iconBg = MaterialTheme.extendedColors.infoContainer,
                            title = "Políticas de privacidad",
                            subtitle = "Tratamiento y protección de tus datos",
                            onClick = { selectedInfoType = ProfileInfoType.PRIVACY }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 3. Botón de Ayuda
                        ProfileInfoNavigationRow(
                            iconPainter = painterResource(id = R.drawable.ic_help_headset_custom),
                            iconTint = MaterialTheme.extendedColors.warning,
                            iconBg = MaterialTheme.extendedColors.warningContainer,
                            title = "Ayuda",
                            subtitle = "Preguntas frecuentes y soporte",
                            onClick = { selectedInfoType = ProfileInfoType.HELP }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 4. Sistema de Strikes / Avisos y Moderación
                        val strikeCount = warnings.size
                        val (strikeIconBg, strikeIconTint) = when {
                            strikeCount >= 5 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                            strikeCount in 1..4 -> MaterialTheme.extendedColors.warningContainer to MaterialTheme.extendedColors.warning
                            else -> MaterialTheme.extendedColors.successContainer to MaterialTheme.extendedColors.success
                        }
                        ProfileInfoNavigationRow(
                            icon = if (strikeCount >= 5) Icons.Default.Dangerous else if (strikeCount in 1..4) Icons.Default.WarningAmber else Icons.Default.Shield,
                            iconTint = strikeIconTint,
                            iconBg = strikeIconBg,
                            title = "Avisos y Moderación (Strikes)",
                            subtitle = if (strikeCount == 0) "0 strikes • Sin infracciones registradas" else "$strikeCount aviso${if (strikeCount != 1) "s" else ""} activo${if (strikeCount != 1) "s" else ""} del campus",
                            trailing = {
                                StrikeBadge(strikes = strikeCount)
                            },
                            onClick = { showStrikesSheet = true }
                        )
                    }
                }
            }

            // Botones de acción en la parte inferior del perfil
            if (isEditMode) {
                Button(
                    onClick = {
                        showValidationErrors = true
                        val fnErr = FormValidators.validateFullName(fullName)
                        val phErr = FormValidators.validatePhone(phone)
                        val cpErr = FormValidators.validateCampus(campus)

                        fullNameError = fnErr
                        phoneError = phErr
                        campusError = cpErr

                        if (fnErr != null || phErr != null || cpErr != null) {
                            val firstErr = fnErr ?: phErr ?: cpErr ?: "Corrige los errores antes de guardar"
                            Toast.makeText(context, firstErr, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val updated = profile.copy(
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            studentCode = profile.studentCode,
                            campus = campus.trim(),
                            avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() }
                        )
                        onSaveProfile(updated)
                        isEditMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                    onClick = { resetEditFields() },
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout_custom),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Versión de la app
                Text(
                    text = "CampusGO - version 0.6.2-beta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
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

    if (showStrikesSheet) {
        SellerNotificationsBottomSheet(
            warnings = warnings,
            isSeller = false,
            onDismiss = { showStrikesSheet = false }
        )
    }
}

@Composable
private fun ProfileInfoNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val effectiveBg = if (isDark) iconTint.copy(alpha = 0.18f) else iconBg
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
            color = effectiveBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 1.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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

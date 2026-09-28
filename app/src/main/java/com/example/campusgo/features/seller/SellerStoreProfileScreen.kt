package com.example.campusgo.features.seller

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.BuildConfig
import com.example.campusgo.R
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.seller.components.SellerNotificationsBottomSheet
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.PaymentMethodLogoByName
import com.example.campusgo.ui.components.ProfileInfoBottomSheet
import com.example.campusgo.ui.components.ProfileInfoType
import com.example.campusgo.ui.components.StoreStatusBadge
import com.example.campusgo.ui.components.StrikeBadge
import com.example.campusgo.ui.components.ValleGoBusinessAvatar
import com.example.campusgo.ui.components.ValleGoBusinessBanner
import com.example.campusgo.ui.components.compressImageUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerStoreProfileScreen(
    profile: UserProfile,
    sellerProfile: UserProfile?,
    warnings: List<ProfileWarning> = emptyList(),
    availableMeetingPoints: List<CampusMeetingPoint> = emptyList(),
    isSaving: Boolean,
    isUploading: Boolean = false,
    onNavigateBack: () -> Unit,
    onUploadAsset: ((bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) -> Unit)? = null,
    onSave: (
        businessName: String,
        businessStatus: String,
        businessDescription: String?,
        businessCategory: String?,
        businessLocation: String?,
        openTime: String?,
        closeTime: String?,
        bannerUrl: String?,
        avatarUrl: String?,
        acceptingOrders: Boolean,
        supportedMeetingPoints: List<String>,
        supportedPaymentMethods: List<String>
    ) -> Unit,
    onToggleAcceptingOrders: ((Boolean) -> Unit)? = null,
    onSignOut: () -> Unit = {},
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = sellerProfile ?: profile
    var isEditMode by remember { mutableStateOf(false) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoTitle by remember { mutableStateOf("") }
    var enlargedPhotoRole by remember { mutableStateOf("") }
    var isEnlargedBanner by remember { mutableStateOf(false) }

    var selectedInfoType by remember { mutableStateOf(ProfileInfoType.NONE) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    // Manejo de retroceso nativo de Android en el perfil del vendedor
    BackHandler(enabled = showEnlargedPhoto) {
        showEnlargedPhoto = false
    }
    BackHandler(enabled = !showEnlargedPhoto && isEditMode) {
        isEditMode = false
    }
    BackHandler(enabled = !showEnlargedPhoto && !isEditMode && showHeader) {
        onNavigateBack()
    }

    var selectedMeetingPoints by remember(activeProfile.id, activeProfile.supportedMeetingPoints) {
        mutableStateOf(activeProfile.supportedMeetingPoints.toSet())
    }
    var selectedPaymentMethods by remember(activeProfile.id, activeProfile.supportedPaymentMethods) {
        mutableStateOf(activeProfile.effectivePaymentMethods.toSet())
    }

    var businessName by remember(activeProfile.id, activeProfile.businessName) {
        mutableStateOf(activeProfile.businessName ?: activeProfile.fullName)
    }
    var description by remember(activeProfile.id, activeProfile.businessDescription) {
        mutableStateOf(activeProfile.businessDescription.orEmpty())
    }
    var category by remember(activeProfile.id, activeProfile.businessCategory) {
        mutableStateOf(activeProfile.businessCategory.orEmpty())
    }
    var location by remember(activeProfile.id, activeProfile.businessLocation) {
        mutableStateOf(activeProfile.businessLocation ?: activeProfile.campus)
    }
    var openTime by remember(activeProfile.id, activeProfile.openTime) {
        mutableStateOf(activeProfile.openTime ?: "08:00")
    }
    var closeTime by remember(activeProfile.id, activeProfile.closeTime) {
        mutableStateOf(activeProfile.closeTime ?: "18:00")
    }
    var bannerUrl by remember(activeProfile.id, activeProfile.bannerUrl) {
        mutableStateOf(activeProfile.bannerUrl.orEmpty())
    }
    var avatarUrl by remember(activeProfile.id, activeProfile.avatarUrl) {
        mutableStateOf(activeProfile.avatarUrl.orEmpty())
    }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var businessStatus by remember(activeProfile.id, activeProfile.businessStatus) {
        mutableStateOf(activeProfile.businessStatus.ifBlank { "ABIERTO" })
    }
    var acceptingOrders by remember(activeProfile.id, activeProfile.acceptingOrders) {
        mutableStateOf(activeProfile.acceptingOrders)
    }

    LaunchedEffect(activeProfile.acceptingOrders) {
        acceptingOrders = activeProfile.acceptingOrders
        businessStatus = activeProfile.businessStatus.ifBlank { if (activeProfile.acceptingOrders) "ABIERTO" else "CERRADO" }
    }

    fun resetFields() {
        businessName = activeProfile.businessName ?: activeProfile.fullName
        description = activeProfile.businessDescription.orEmpty()
        category = activeProfile.businessCategory.orEmpty()
        location = activeProfile.businessLocation ?: activeProfile.campus
        openTime = activeProfile.openTime ?: "08:00"
        closeTime = activeProfile.closeTime ?: "18:00"
        bannerUrl = activeProfile.bannerUrl.orEmpty()
        avatarUrl = activeProfile.avatarUrl.orEmpty()
        businessStatus = activeProfile.businessStatus.ifBlank { "ABIERTO" }
        acceptingOrders = activeProfile.acceptingOrders
        selectedMeetingPoints = activeProfile.supportedMeetingPoints.toSet()
        selectedPaymentMethods = activeProfile.effectivePaymentMethods.toSet()
        isEditMode = false
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 1200, quality = 82)
            if (bytes != null && onUploadAsset != null) {
                isUploadingBanner = true
                val path = "banners/banner_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    bannerUrl = uploadedUrl
                    isUploadingBanner = false
                }
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 512, quality = 85)
            if (bytes != null && onUploadAsset != null) {
                isUploadingAvatar = true
                val path = "avatars/avatar_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    avatarUrl = uploadedUrl
                    isUploadingAvatar = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showHeader) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) "Editar Mi Puesto" else "Mi Puesto Comercial",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isEditMode) {
                                resetFields()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color(0xFF003366)
                            )
                        }
                    },
                    actions = {
                        if (!isEditMode) {
                            FilledTonalButton(
                                onClick = { isEditMode = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE3F2FD),
                                    contentColor = Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_edit_store_custom), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (isEditMode || showHeader) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEditMode) {
                            Button(
                                onClick = {
                                    onSave(
                                        businessName,
                                        businessStatus,
                                        description,
                                        category,
                                        location,
                                        openTime,
                                        closeTime,
                                        bannerUrl,
                                        avatarUrl,
                                        acceptingOrders,
                                        selectedMeetingPoints.toList(),
                                        selectedPaymentMethods.toList()
                                    )
                                    isEditMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isSaving && !isUploadingBanner && !isUploadingAvatar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardando...")
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { resetFields() },
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
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Portada Completa Arriba (Full Width Hero Banner)
            val bannerShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .clip(bannerShape)
                    .then(
                        if (isEditMode) Modifier.clickable { bannerPickerLauncher.launch("image/*") }
                        else Modifier.clickable {
                            enlargedPhotoUrl = bannerUrl.takeIf { it.isNotBlank() }
                            enlargedPhotoTitle = businessName.ifBlank { activeProfile.fullName }
                            enlargedPhotoRole = "Banner del Puesto"
                            isEnlargedBanner = true
                            showEnlargedPhoto = true
                        }
                    )
            ) {
                ValleGoBusinessBanner(
                    bannerUrl = bannerUrl.takeIf { it.isNotBlank() },
                    storeName = businessName,
                    shape = bannerShape,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay sutil para legibilidad de botones superiores
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                )

                // Botones superiores sobre la portada (elevado para aprovechar el espacio superior)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (showHeader) {
                        IconButton(
                            onClick = {
                                if (isEditMode) resetFields() else onNavigateBack()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    if (!isEditMode) {
                        Surface(
                            onClick = { isEditMode = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_store_custom),
                                    contentDescription = null,
                                    tint = Color(0xFF003366),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Editar Puesto",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF003366)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = { bannerPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isUploadingBanner) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                    Text("Subiendo...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Cambiar Portada", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Contenedor de tarjetas superpuesto suavemente sobre la portada
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-44).dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de Cabecera: Perfil Circular y Datos del Puesto (Blanco Nítido)
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tarjeta de fondo blanco
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 44.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Fila superior en la tarjeta: StoreStatusBadge a la derecha
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StoreStatusBadge(status = businessStatus, acceptingOrders = acceptingOrders)
                            }

                            // Información de nombre y responsable
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = businessName.ifBlank { "Nombre del Puesto" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F)
                                )
                                Text(
                                    text = "Responsable: ${activeProfile.fullName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Badges: Emprendedor Autorizado + Calificación
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFE6F6F3),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                                contentDescription = null,
                                                tint = Color(0xFF0D5C4C),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Emprendedor Autorizado",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF0D5C4C),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "%.1f".format(activeProfile.ratingAverage),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Etiquetas: Categoría + Métodos de Pago
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Etiqueta de la categoría
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_category_custom),
                                                contentDescription = null,
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = category.ifBlank { "Comidas / Varios" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }

                                    // Métodos de pago al costado
                                    val availableMethods = listOf("EFECTIVO", "YAPE", "PLIN")
                                    selectedPaymentMethods.sortedBy { availableMethods.indexOf(it) }.forEach { method ->
                                        val (bg, tint, label) = when (method) {
                                            "YAPE" -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                                            "PLIN" -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                                            "EFECTIVO" -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                                            else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), method)
                                        }
                                        Surface(
                                            color = bg,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                PaymentMethodLogoByName(name = method, size = 12.dp)
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tint
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Avatar Circular Prominente (Sobresale como círculo completo sobre la portada y la tarjeta)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp)
                            .size(92.dp)
                            .then(
                                if (isEditMode) Modifier.clickable { avatarPickerLauncher.launch("image/*") }
                                else Modifier.clip(CircleShape).clickable {
                                    enlargedPhotoUrl = avatarUrl.takeIf { it.isNotBlank() }
                                    enlargedPhotoTitle = businessName.ifBlank { activeProfile.fullName }
                                    enlargedPhotoRole = "Emprendedor Universitario • Campus ${activeProfile.campus}"
                                    isEnlargedBanner = false
                                    showEnlargedPhoto = true
                                }
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(3.5.dp, Color.White),
                            shadowElevation = 6.dp,
                            color = Color.White,
                            modifier = Modifier.size(92.dp)
                        ) {
                            ValleGoBusinessAvatar(
                                avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                                storeName = businessName,
                                size = 92.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                color = Color(0xFF003366),
                                shape = CircleShape,
                                shadowElevation = 3.dp,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploadingAvatar) {
                                        CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar logo", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isEditMode) {
                    // MODO LECTURA
                    if (businessStatus == "SATURADO") {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Modo Saturado activo: Tus clientes ven un aviso de alta demanda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else if (businessStatus == "PAUSADO") {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Modo Pausado: Las compras están deshabilitadas temporalmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else if (businessStatus == "CERRADO") {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Puesto Cerrado: No visible para pedidos en catálogo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC8102E),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // Información del Puesto (Blanco Nítido con Toggle Abierto/Cerrado)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                        shadowElevation = 1.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INFORMACIÓN DEL PUESTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )

                                // Toggle interactivo de Abrir/Cerrar Puesto
                                val thumbOffset by animateDpAsState(
                                    targetValue = if (acceptingOrders) 14.dp else 0.dp,
                                    animationSpec = tween(200),
                                    label = "stall_toggle_profile"
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (acceptingOrders) Color(0xFFE6F7F3) else Color(0xFFFEE2E2))
                                        .border(
                                            1.dp,
                                            if (acceptingOrders) Color(0xFF00A884).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            val newAccepting = !acceptingOrders
                                            acceptingOrders = newAccepting
                                            businessStatus = if (newAccepting) "ABIERTO" else "CERRADO"
                                            onToggleAcceptingOrders?.invoke(newAccepting)
                                        }
                                        .padding(start = 9.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                if (acceptingOrders) Color(0xFF00A884) else Color(0xFFEF4444),
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (acceptingOrders) "Abierto" else "Cerrado",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (acceptingOrders) Color(0xFF007A60) else Color(0xFFB91C1C)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(30.dp)
                                            .height(18.dp)
                                            .background(
                                                if (acceptingOrders) Color(0xFF00A884) else Color(0xFFCBD5E1),
                                                RoundedCornerShape(9.dp)
                                            )
                                            .padding(2.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = thumbOffset)
                                                .size(14.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }

                            SellerProfileDetailRow(
                                label = "Descripción",
                                value = description.ifBlank { "Sin descripción detallada registrada." }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            SellerProfileDetailRow(
                                label = "Calificación promedio",
                                value = "⭐ %.1f de 5.0 estrellas".format(activeProfile.ratingAverage)
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            SellerProfileDetailRow(
                                label = "Ubicación en campus",
                                value = location.ifBlank { "Campus ${activeProfile.campus}" }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            SellerProfileDetailRow(
                                label = "Horario de atención",
                                value = "$openTime - $closeTime"
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            SellerProfileDetailRow(
                                label = "Recepción de pedidos",
                                value = if (acceptingOrders) "Abierto • Aceptando pedidos activamente" else "Cerrado • Pedidos desactivados",
                                valueColor = if (acceptingOrders) Color(0xFF007A60) else Color(0xFFB91C1C)
                            )
                        }
                    }

                    // Puntos de Entrega Habilitados (Modo Lectura - Blanco Nítido)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                        shadowElevation = 1.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PUNTOS DE ENTREGA HABILITADOS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${selectedMeetingPoints.size} seleccionados",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884)
                                )
                            }

                            if (selectedMeetingPoints.isEmpty()) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No has seleccionado puntos de entrega. Tus compradores no podrán programar entregas hasta que habilites al menos un punto.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedMeetingPoints.forEach { pointId ->
                                        val pt = availableMeetingPoints.find { it.id == pointId }
                                        val label = pt?.name ?: pointId
                                        val isExt = pt?.zoneType == "EXTERIOR"
                                        Surface(
                                            color = if (isExt) Color(0xFFE8F5E9) else Color(0xFFEDE7F6),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isExt) Color(0xFF81C784) else Color(0xFFB39DDB))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                                    contentDescription = null,
                                                    tint = if (isExt) Color(0xFF2E7D32) else Color(0xFF512DA8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isExt) Color(0xFF1B5E20) else Color(0xFF311B92)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sección: MÁS INFORMACIÓN (Términos, Privacidad, Ayuda y Strikes)
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
                                subtitle = "Normas de uso del servicio y ventas",
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
                                subtitle = "Preguntas frecuentes y soporte al vendedor",
                                onClick = { selectedInfoType = ProfileInfoType.HELP }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 4. Sistema de Strikes / Avisos y Moderación
                            val strikeCount = warnings.size
                            val (strikeIconBg, strikeIconTint) = when {
                                strikeCount >= 5 -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
                                strikeCount in 1..4 -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                else -> Color(0xFFE6F7F3) to Color(0xFF00A884)
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
                                onClick = { showNotificationsSheet = true }
                            )
                        }
                    }
                } else {
                // MODO EDICIÓN
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                    shadowElevation = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ESTADO OPERATIVO DEL PUESTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val states = listOf(
                                "ABIERTO" to "Abierto",
                                "SATURADO" to "Saturado",
                                "PAUSADO" to "Pausado",
                                "CERRADO" to "Cerrado"
                            )
                            states.forEach { (statusKey, label) ->
                                val isSelected = businessStatus.equals(statusKey, ignoreCase = true)
                                OutlinedButton(
                                    onClick = {
                                        businessStatus = statusKey
                                        if (statusKey == "CERRADO") acceptingOrders = false
                                        if (statusKey == "ABIERTO" || statusKey == "SATURADO") acceptingOrders = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color(0xFF003366) else Color.Transparent,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        if (businessStatus == "SATURADO") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Modo Saturado: Los compradores verán un aviso de alta demanda indicando que su pedido puede tardar un poco más.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Nombre Comercial del Puesto *") },
                            placeholder = { Text("Ej. El Rincón del Sabor Universitario") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción del Negocio") },
                            placeholder = { Text("Ej. Hamburguesas artesanales, triples y jugos") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Giro / Categoría") },
                                placeholder = { Text("Comidas / Snacks") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Ubicación") },
                                placeholder = { Text("Pabellón A / Cafetería") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = openTime,
                                onValueChange = { openTime = it },
                                label = { Text("Apertura") },
                                placeholder = { Text("08:00 AM") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = closeTime,
                                onValueChange = { closeTime = it },
                                label = { Text("Cierre") },
                                placeholder = { Text("06:00 PM") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Puntos de Entrega (Modo Edición con Selección)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                    shadowElevation = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PUNTOS DE ENTREGA AUTORIZADOS *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Marca únicamente los puntos donde realmente puedes entregar pedidos. Solo estos puntos aparecerán al comprador al realizar su pedido:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        if (availableMeetingPoints.isEmpty()) {
                            Text(
                                text = "Cargando puntos de encuentro...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableMeetingPoints.forEach { point ->
                                    val isChecked = selectedMeetingPoints.contains(point.id)
                                    val isExterior = point.zoneType == "EXTERIOR"
                                    Surface(
                                        color = if (isChecked) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChecked) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMeetingPoints = if (isChecked) {
                                                    selectedMeetingPoints - point.id
                                                } else {
                                                    selectedMeetingPoints + point.id
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedMeetingPoints = if (checked) {
                                                        selectedMeetingPoints + point.id
                                                    } else {
                                                        selectedMeetingPoints - point.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF003366))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = point.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Surface(
                                                        color = if (isExterior) Color(0xFFDCFCE7) else Color(0xFFF3E8FF),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isExterior) "Exterior" else "Interior",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isExterior) Color(0xFF15803D) else Color(0xFF7E22CE),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                val ptDesc = point.description
                                                if (!ptDesc.isNullOrBlank()) {
                                                    Text(
                                                        text = ptDesc,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Métodos de Pago Aceptados (Modo Edición con Selección)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                    shadowElevation = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MÉTODOS DE PAGO ACEPTADOS *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Selecciona qué formas de pago aceptas de tus clientes (debes mantener al menos una activa):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        val paymentOptions = listOf(
                            Triple("EFECTIVO", "Efectivo", "Pago contra entrega al momento de recibir el pedido"),
                            Triple("YAPE", "Yape", "Transferencia móvil directa BCP"),
                            Triple("PLIN", "Plin", "Transferencia interbancaria (BBVA, Interbank, etc.)")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            paymentOptions.forEach { (code, label, desc) ->
                                val isChecked = selectedPaymentMethods.contains(code)
                                val (bgTint, textTint) = when (code) {
                                    "YAPE" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
                                    "PLIN" -> Color(0xFFE0F2F1) to Color(0xFF00796B)
                                    else -> Color(0xFFF1F5F9) to Color(0xFF003366)
                                }

                                Surface(
                                    color = if (isChecked) bgTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChecked) textTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPaymentMethods = if (isChecked) {
                                                if (selectedPaymentMethods.size > 1) selectedPaymentMethods - code else selectedPaymentMethods
                                            } else {
                                                selectedPaymentMethods + code
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedPaymentMethods = if (checked) {
                                                    selectedPaymentMethods + code
                                                } else {
                                                    if (selectedPaymentMethods.size > 1) selectedPaymentMethods - code else selectedPaymentMethods
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = textTint)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                PaymentMethodLogoByName(name = code, size = 16.dp)
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textTint
                                                )
                                            }
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!showHeader && !isEditMode) {
                Spacer(modifier = Modifier.height(4.dp))
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

                Spacer(modifier = Modifier.height(96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }
}

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = enlargedPhotoUrl,
            name = enlargedPhotoTitle,
            roleDescription = enlargedPhotoRole,
            isBanner = isEnlargedBanner,
            onDismiss = { showEnlargedPhoto = false }
        )
    }

    if (selectedInfoType != ProfileInfoType.NONE) {
        ProfileInfoBottomSheet(
            type = selectedInfoType,
            onDismiss = { selectedInfoType = ProfileInfoType.NONE }
        )
    }

    if (showNotificationsSheet) {
        SellerNotificationsBottomSheet(
            warnings = warnings,
            onDismiss = { showNotificationsSheet = false }
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
    trailing: (@Composable () -> Unit)? = null,
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
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
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
                color = Color(0xFF16324F)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
        if (trailing != null) {
            trailing()
            Spacer(modifier = Modifier.width(6.dp))
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
private fun SellerProfileDetailRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

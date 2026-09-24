package com.example.vallego.features.seller

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.ProfileWarning
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.OfficialWarningBanner
import com.example.vallego.ui.components.PaymentMethodLogoByName
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.compressImageUri

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
    onSignOut: () -> Unit = {},
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

    // Manejo de retroceso nativo de Android en el perfil del vendedor
    BackHandler(enabled = showEnlargedPhoto) {
        showEnlargedPhoto = false
    }
    BackHandler(enabled = !showEnlargedPhoto && isEditMode) {
        isEditMode = false
    }
    BackHandler(enabled = !showEnlargedPhoto && !isEditMode) {
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
        },
        bottomBar = {
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
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera: Portada y Avatar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
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
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isEditMode) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isUploadingBanner) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Subiendo...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text("Cambiar portada", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Logo / Avatar
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 80.dp)
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
                            shadowElevation = 4.dp
                        ) {
                            ValleGoBusinessAvatar(
                                avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                                storeName = businessName,
                                size = 76.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                color = Color(0xFF003366),
                                shape = CircleShape,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploadingAvatar) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar logo", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessName.ifBlank { "Nombre del Puesto" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                            Text(
                                text = "Responsable: ${activeProfile.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Badge Emprendedor Autorizado
                            Surface(
                                color = Color(0xFFE6F6F3),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF0D5C4C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Emprendedor Autorizado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0D5C4C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Calificación promedio del vendedor
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                            fontSize = 12.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                                Text(
                                    text = "Calificación de clientes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        StoreStatusBadge(status = businessStatus, acceptingOrders = acceptingOrders)
                    }
                }
            }

            if (!isEditMode) {
                // Advertencias Oficiales de Moderación
                OfficialWarningBanner(
                    warnings = warnings,
                    isSeller = true
                )

                // MODO LECTURA
                if (businessStatus == "SATURADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Modo Saturado activo: Tus clientes ven un aviso de alta demanda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (businessStatus == "PAUSADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Modo Pausado: Las compras están deshabilitadas temporalmente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (businessStatus == "CERRADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Puesto Cerrado: No visible para pedidos en catálogo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC8102E),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "INFORMACIÓN DEL PUESTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

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
                            label = "Giro comercial / Categoría",
                            value = category.ifBlank { "Comidas / Varios" }
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
                            value = if (acceptingOrders) "Aceptando pedidos activamente" else "Pedidos desactivados"
                        )
                    }
                }

                // Puntos de Entrega Habilitados (Modo Lectura)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

                // Métodos de Pago Aceptados (Modo Lectura)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MÉTODOS DE PAGO ACEPTADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${selectedPaymentMethods.size} activos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        PaymentMethodLogoByName(name = method, size = 14.dp)
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = tint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MODO EDICIÓN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
}

@Composable
private fun SellerProfileDetailRow(
    label: String,
    value: String,
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
            color = Color(0xFF1E293B)
        )
    }
}

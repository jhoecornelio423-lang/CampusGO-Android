package com.example.vallego.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.ui.components.SetDarkScreenStatusBar
import com.example.vallego.ui.components.ValleGoDialogContainerColor
import com.example.vallego.ui.components.ValleGoDialogShape
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.valleGoDialogStyle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthRoute(
    onAuthSuccess: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWelcome by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        if (uiState.infoMessage != null || uiState.errorMessage != null) {
            showWelcome = false
        }
    }

    BackHandler(enabled = !showWelcome || uiState.screenMode != AuthScreenMode.LOGIN) {
        when (uiState.screenMode) {
            AuthScreenMode.VERIFY_OTP,
            AuthScreenMode.FORGOT_PASSWORD_EMAIL,
            AuthScreenMode.FORGOT_PASSWORD_OTP,
            AuthScreenMode.SELLER_PENDING_APPROVAL -> {
                viewModel.backToLogin()
            }
            AuthScreenMode.REGISTER -> {
                viewModel.setLoginMode(true)
            }
            AuthScreenMode.LOGIN -> {
                showWelcome = true
            }
        }
    }

    LaunchedEffect(uiState.isSuccess, uiState.profile) {
        val profile = uiState.profile
        if (uiState.isSuccess && (profile != null)) {
            onAuthSuccess(profile)
        }
    }

    when (uiState.screenMode) {
        AuthScreenMode.SELLER_PENDING_APPROVAL -> {
            SellerPendingApprovalFullScreen(
                profile = uiState.profile ?: UserProfile(
                    id = "",
                    fullName = uiState.fullName,
                    phone = uiState.phone,
                    role = UserRole.EMPRENDEDOR,
                    campus = uiState.campus,
                    businessName = uiState.storeName,
                    businessStatus = "PENDIENTE",
                    supportedMeetingPoints = listOf(uiState.selectedMeetingPoint)
                ),
                onSignOut = viewModel::signOutFromPending,
                modifier = modifier
            )
        }
        AuthScreenMode.VERIFY_OTP -> {
            OtpVerificationView(
                uiState = uiState,
                onOtpChange = viewModel::onOtpCodeChange,
                onVerify = viewModel::verifyOtp,
                onResend = viewModel::resendOtp,
                onBack = { viewModel.setScreenMode(AuthScreenMode.REGISTER) },
                onDismissError = viewModel::clearError,
                modifier = modifier
            )
        }
        AuthScreenMode.FORGOT_PASSWORD_EMAIL -> {
            ForgotPasswordEmailView(
                uiState = uiState,
                onEmailChange = viewModel::onEmailChange,
                onSubmit = viewModel::sendForgotPasswordEmail,
                onBackToLogin = viewModel::backToLogin,
                onDismissError = viewModel::clearError,
                modifier = modifier
            )
        }
        AuthScreenMode.FORGOT_PASSWORD_OTP -> {
            ForgotPasswordOtpView(
                uiState = uiState,
                onOtpChange = viewModel::onOtpCodeChange,
                onNewPasswordChange = viewModel::onNewPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmNewPasswordChange,
                onSubmit = viewModel::resetPasswordWithOtp,
                onResend = viewModel::sendForgotPasswordEmail,
                onBackToLogin = viewModel::backToLogin,
                onDismissError = viewModel::clearError,
                modifier = modifier
            )
        }
        AuthScreenMode.LOGIN, AuthScreenMode.REGISTER -> {
            if (showWelcome && uiState.infoMessage == null) {
                WelcomeScreen(
                    onStartRegister = {
                        viewModel.setLoginMode(false)
                        showWelcome = false
                    },
                    onLogin = {
                        viewModel.setLoginMode(true)
                        showWelcome = false
                    },
                    modifier = modifier
                )
            } else {
                AuthScreen(
                    uiState = uiState,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onFullNameChange = viewModel::onFullNameChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onRoleChange = viewModel::onRoleChange,
                    onCampusChange = viewModel::onCampusChange,
                    onStoreNameChange = viewModel::onStoreNameChange,
                    onStoreCategoryChange = viewModel::onStoreCategoryChange,
                    onStoreDescriptionChange = viewModel::onStoreDescriptionChange,
                    onMeetingPointChange = viewModel::onMeetingPointChange,
                    onForgotPasswordClick = viewModel::openForgotPassword,
                    onTabSelected = viewModel::setLoginMode,
                    onSubmit = viewModel::submit,
                    onDismissError = viewModel::clearError,
                    onDismissInfo = viewModel::clearInfoMessage,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onCampusChange: (String) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onStoreCategoryChange: (String) -> Unit,
    onStoreDescriptionChange: (String) -> Unit,
    onMeetingPointChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onTabSelected: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    onDismissInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetDarkScreenStatusBar(isDark = true)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    var showSupportDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.isLoginMode) {
        scrollState.scrollTo(0)
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    // Separación de Nombre y Apellidos para la vista de registro
    var firstName by remember(uiState.fullName) {
        val parts = uiState.fullName.split(" ", limit = 2)
        mutableStateOf(parts.firstOrNull().orEmpty())
    }
    var lastName by remember(uiState.fullName) {
        val parts = uiState.fullName.split(" ", limit = 2)
        mutableStateOf(parts.getOrNull(1).orEmpty())
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var meetingPointDropdownExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(240.dp) }

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (showSupportDialog) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "auth_dialog_blur"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val totalScreenHeight = maxHeight

        Image(
            painter = painterResource(id = R.drawable.fondo_login_register),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier)
                .imePadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECCIÓN SUPERIOR: HEADER CON LOGO OFICIAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)
                    .onGloballyPositioned { coordinates ->
                        val hDp = with(density) { coordinates.size.height.toDp() }
                        if (hDp > 0.dp) {
                            headerHeightDp = hDp
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.campus_logo),
                        contentDescription = "Logo Campus Go",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(58.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.letras_logo_white),
                        contentDescription = "Campus Go",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(24.dp)
                    )
                }

                if (uiState.isLoginMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TU CAMPUS, EN MOVIMIENTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6DE2D2),
                            letterSpacing = 1.4.sp
                        )

                        Text(
                            text = "Todo lo que necesitas, en\nun solo lugar.",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 34.sp
                        )
                    }
                }
            }

            // SECCIÓN INFERIOR: TARJETA BLANCA BORDE A BORDE
            val cardMinHeight = (totalScreenHeight - headerHeightDp).coerceAtLeast(0.dp)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = cardMinHeight),
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp
                ),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = cardMinHeight)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado según el Modo
                    if (uiState.isLoginMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Bienvenido",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp
                                ),
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Ingresa con tu cuenta Campus Go",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "Registro de Vendedor" else "Crea tu cuenta",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp
                                ),
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = if (uiState.selectedRole == UserRole.EMPRENDEDOR)
                                    "Registra tu emprendimiento y vende en el campus"
                                else
                                    "Únete para comprar fácil y rápido en el campus",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Banner de Información si existe
                    AnimatedVisibility(
                        visible = uiState.infoMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.infoMessage.orEmpty(),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissInfo,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = Color(0xFF166534),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Banner de Error
                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissError,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = Color(0xFF991B1B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // MODO REGISTRO: SELECTOR DE ROL AL INICIO (Cliente vs Vendedor)
                    if (!uiState.isLoginMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "¿Cómo usarás Campus Go?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16324F)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RoleCardButton(
                                    title = "Cliente",
                                    subtitle = "Comprar productos",
                                    icon = Icons.Default.ShoppingBag,
                                    isSelected = uiState.selectedRole == UserRole.COMPRADOR,
                                    onClick = { onRoleChange(UserRole.COMPRADOR) },
                                    modifier = Modifier.weight(1f)
                                )
                                RoleCardButton(
                                    title = "Vendedor",
                                    subtitle = "Vender y emprender",
                                    iconPainter = painterResource(id = R.drawable.ic_store_custom),
                                    isSelected = uiState.selectedRole == UserRole.EMPRENDEDOR,
                                    onClick = { onRoleChange(UserRole.EMPRENDEDOR) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Banner explicativo y visual del rol actualmente activo
                            Surface(
                                color = if (uiState.selectedRole == UserRole.EMPRENDEDOR) Color(0xFFFFFBEB) else Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (uiState.selectedRole == UserRole.EMPRENDEDOR) Color(0xFFFDE68A) else Color(0xFFBFDBFE)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.selectedRole == UserRole.EMPRENDEDOR) Icons.Default.Storefront else Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = if (uiState.selectedRole == UserRole.EMPRENDEDOR) Color(0xFFD97706) else Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (uiState.selectedRole == UserRole.EMPRENDEDOR)
                                            "Modo Vendedor seleccionado: Al verificar tu correo, tu solicitud requerirá aprobación del administrador antes de que puedas acceder."
                                        else
                                            "Modo Cliente seleccionado: Podrás navegar y realizar compras en los puestos oficiales del campus en cuanto verifiques tu correo.",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (uiState.selectedRole == UserRole.EMPRENDEDOR) Color(0xFF92400E) else Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }

                        // Sede Universitaria de Lanzamiento (Informativo y limpio)
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sede de lanzamiento:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "UCV - Lima Norte (Sede oficial)",
                                        fontSize = 13.sp,
                                        color = Color(0xFF16324F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Nombres & Apellidos en 2 columnas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "Nombres titular" else "Nombres",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = {
                                        firstName = it
                                        onFullNameChange("$firstName $lastName".trim())
                                    },
                                    placeholder = { Text("Tus nombres", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF4F6F8),
                                        focusedBorderColor = Color(0xFF00A884),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "Apellidos titular" else "Apellidos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = {
                                        lastName = it
                                        onFullNameChange("$firstName $lastName".trim())
                                    },
                                    placeholder = { Text("Tus apellidos", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF4F6F8),
                                        focusedBorderColor = Color(0xFF00A884),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Teléfono WhatsApp
                        Column {
                            Text(
                                text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "WhatsApp de contacto" else "Número de teléfono",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16324F),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = uiState.phone,
                                onValueChange = onPhoneChange,
                                placeholder = { Text("+51 987 654 321", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF4F6F8),
                                    focusedBorderColor = Color(0xFF00A884),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // CAMPOS EXCLUSIVOS DE VENDEDOR
                        if (uiState.selectedRole == UserRole.EMPRENDEDOR) {
                            // Nombre de la Tienda
                            Column {
                                Text(
                                    text = "Nombre de la Tienda / Negocio",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.storeName,
                                    onValueChange = onStoreNameChange,
                                    placeholder = { Text("Ej. Jugos y Snacks Doña Luz", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Storefront,
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF4F6F8),
                                        focusedBorderColor = Color(0xFF00A884),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Categoría de la Tienda (Dropdown)
                            Column {
                                Text(
                                    text = "Categoría del Emprendimiento",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = categoryDropdownExpanded,
                                    onExpandedChange = { expanded ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        categoryDropdownExpanded = expanded
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.storeCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF4F6F8),
                                            focusedBorderColor = Color(0xFF00A884),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedTextColor = Color(0xFF16324F),
                                            unfocusedTextColor = Color(0xFF16324F)
                                        ),
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false },
                                        modifier = Modifier
                                            .exposedDropdownSize()
                                            .background(Color.White)
                                    ) {
                                        AuthUiState.STORE_CATEGORIES.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category, fontSize = 13.5.sp, color = Color(0xFF16324F)) },
                                                onClick = {
                                                    onStoreCategoryChange(category)
                                                    categoryDropdownExpanded = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Descripción del Negocio
                            Column {
                                Text(
                                    text = "Descripción de tus productos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.storeDescription,
                                    onValueChange = onStoreDescriptionChange,
                                    placeholder = { Text("Ej. Venta de jugos naturales, sánguches frescos y postres caseros...", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF4F6F8),
                                        focusedBorderColor = Color(0xFF00A884),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Punto de Entrega Preferido (Puntos Oficiales Aprobados)
                            Column {
                                Text(
                                    text = "Punto de entrega preferido (Oficial)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = meetingPointDropdownExpanded,
                                    onExpandedChange = { expanded ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        meetingPointDropdownExpanded = expanded
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.selectedMeetingPoint.ifBlank { "Selecciona un punto oficial..." },
                                        onValueChange = {},
                                        readOnly = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Place,
                                                contentDescription = null,
                                                tint = Color(0xFF00A884),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = meetingPointDropdownExpanded)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF4F6F8),
                                            focusedBorderColor = Color(0xFF00A884),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedTextColor = Color(0xFF16324F),
                                            unfocusedTextColor = Color(0xFF16324F)
                                        ),
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = meetingPointDropdownExpanded,
                                        onDismissRequest = { meetingPointDropdownExpanded = false },
                                        modifier = Modifier
                                            .exposedDropdownSize()
                                            .background(Color.White)
                                    ) {
                                        uiState.availableMeetingPoints.forEach { point ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(point.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16324F))
                                                        val desc = point.description
                                                        if (!desc.isNullOrBlank()) {
                                                            Text(desc, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    onMeetingPointChange(point.name)
                                                    meetingPointDropdownExpanded = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Campo Correo Electrónico (Cualquier dominio válido)
                    Column {
                        Text(
                            text = "Correo Electrónico",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = onEmailChange,
                            placeholder = { Text("tu.correo@ejemplo.com", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFB),
                                focusedBorderColor = Color(0xFF00A884),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF16324F),
                                unfocusedTextColor = Color(0xFF16324F)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Campo Contraseña
                    Column {
                        Text(
                            text = "Contraseña",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChange,
                            placeholder = {
                                Text(
                                    text = if (uiState.isLoginMode) "••••••••" else "Mínimo 6 caracteres",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                        tint = Color(0xFF829AB1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (uiState.isLoginMode) ImeAction.Done else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (uiState.canSubmit && (uiState.isLoginMode || termsAccepted)) onSubmit()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFB),
                                focusedBorderColor = Color(0xFF00A884),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF16324F),
                                unfocusedTextColor = Color(0xFF16324F)
                            ),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Fila inferior de contraseña para MODO LOGIN
                    if (uiState.isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00A884))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Conexión segura",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { onForgotPasswordClick() }
                            )
                        }
                    }

                    // MODO REGISTRO: Checkbox Términos
                    if (!uiState.isLoginMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { termsAccepted = !termsAccepted }
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF00A884),
                                    uncheckedColor = Color(0xFFCBD5E1)
                                )
                            )
                            Text(
                                text = "Acepto los términos y la política de privacidad",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botón Principal
                    val isButtonEnabled = uiState.canSubmit && (uiState.isLoginMode || termsAccepted)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                        enabled = isButtonEnabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A884),
                            disabledContainerColor = Color(0xFF80D3C5),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when {
                                        uiState.isLoginMode -> "Iniciar sesión"
                                        uiState.selectedRole == UserRole.EMPRENDEDOR -> "Solicitar Registro de Vendedor"
                                        else -> "Crear Cuenta de Cliente"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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
                    }

                    // Links del Pie de Página
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        if (uiState.isLoginMode) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¿No tienes una cuenta? ",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Regístrate ahora",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884),
                                    modifier = Modifier.clickable { onTabSelected(false) }
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¿Ya tienes una cuenta? ",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Inicia sesión",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884),
                                    modifier = Modifier.clickable { onTabSelected(true) }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "¿Necesitas ayuda o reportar un problema? ",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Centro de ayuda y reportes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { showSupportDialog = true }
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Powered by",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Image(
                                painter = painterResource(id = R.drawable.kodex_logo),
                                contentDescription = "Logo Kodex",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KODEX",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF102A43),
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Espacio generoso para que el teclado no obstruya el botón ni los campos
                        Spacer(modifier = Modifier.height(180.dp))
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text(
                    text = "Centro de Soporte y Reportes Campus Go",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16324F)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Tienes inconvenientes con tu cuenta, verificación, reportes o solicitud de vendedor? Comunícate con nuestro canal oficial de Campus Go:",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "📧 Correo: soporte@kodexti.com\n⏰ Horario: Lun - Sáb 8:00 AM a 8:00 PM\n📍 Sede oficial: UCV - Lima Norte",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            uriHandler.openUri("mailto:soporte@kodexti.com?subject=Soporte%20Campus%20Go")
                        } catch (_: Exception) {}
                    }
                ) {
                    Text("Enviar Correo", fontWeight = FontWeight.Bold, color = Color(0xFF00A884))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun RoleCardButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF00A884) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00897B) else Color(0xFFCBD5E1)
        ),
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        modifier = modifier.height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconPainter != null) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (isSelected) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.88f) else Color(0xFF64748B)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
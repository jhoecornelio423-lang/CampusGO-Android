package com.example.campusgo.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.material.icons.outlined.HeadsetMic
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.ui.components.SetDarkScreenStatusBar
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.campusGoDialogStyle
import com.example.campusgo.core.util.FormValidators
import com.example.campusgo.ui.components.ProfileInfoBottomSheet
import com.example.campusgo.ui.components.ProfileInfoType
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthRoute(
    onAuthSuccess: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resetCredentials()
    }

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
                    onFirstNameChange = viewModel::onFirstNameChange,
                    onLastNameChange = viewModel::onLastNameChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onRoleChange = viewModel::onRoleChange,
                    onCampusChange = viewModel::onCampusChange,
                    onStoreNameChange = viewModel::onStoreNameChange,
                    onStoreCategoryChange = viewModel::onStoreCategoryChange,
                    onCustomCategoryChange = viewModel::onCustomCategoryChange,
                    onStoreDescriptionChange = viewModel::onStoreDescriptionChange,
                    onMeetingPointChange = viewModel::onMeetingPointChange,
                    onForgotPasswordClick = viewModel::openForgotPassword,
                    onTabSelected = viewModel::setLoginMode,
                    onSubmit = viewModel::submit,
                    onGoogleSignIn = { viewModel.signInWithGoogle(context) },
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
    onFirstNameChange: (String) -> Unit = {},
    onLastNameChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onCampusChange: (String) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onStoreCategoryChange: (String) -> Unit,
    onCustomCategoryChange: (String) -> Unit = {},
    onStoreDescriptionChange: (String) -> Unit,
    onMeetingPointChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onTabSelected: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onDismissError: () -> Unit,
    onDismissInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetDarkScreenStatusBar(isDark = true)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    var showSupportDialog by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
    var showFieldErrors by remember { mutableStateOf(false) }

    var firstNameTouched by remember { mutableStateOf(false) }
    var lastNameTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var storeNameTouched by remember { mutableStateOf(false) }
    var customCategoryTouched by remember { mutableStateOf(false) }
    var storeDescTouched by remember { mutableStateOf(false) }
    var meetingPointTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.isLoginMode) {
        showFieldErrors = false
        firstNameTouched = false
        lastNameTouched = false
        phoneTouched = false
        storeNameTouched = false
        customCategoryTouched = false
        storeDescTouched = false
        meetingPointTouched = false
        emailTouched = false
        passwordTouched = false
        scrollState.scrollTo(0)
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            scrollState.animateScrollTo(0)
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var meetingPointDropdownExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(240.dp) }

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (showSupportDialog || showTermsSheet) 20.dp else 0.dp,
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
            alignment = Alignment.TopCenter,
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
            // SECCIÓN SUPERIOR: HEADER CON LOGO OFICIAL AGRANDADO Y LIMPIO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = if (uiState.isLoginMode) 28.dp else 20.dp)
                    .onGloballyPositioned { coordinates ->
                        val hDp = with(density) { coordinates.size.height.toDp() }
                        if (hDp > 0.dp) {
                            headerHeightDp = hDp
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_login),
                    contentDescription = "Logo CampusGo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(if (uiState.isLoginMode) 94.dp else 84.dp)
                )
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
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado según el Modo (directo y sin exceso de texto)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when {
                                uiState.isLoginMode -> "Bienvenido"
                                uiState.selectedRole == UserRole.EMPRENDEDOR -> "Registro de Vendedor"
                                else -> "Crear Cuenta"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color(0xFF16324F)
                        )
                        Text(
                            text = if (uiState.isLoginMode) "Inicia sesión para continuar" else "Ingresa tus datos para empezar",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
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

                            if (uiState.selectedRole == UserRole.EMPRENDEDOR) {
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "La cuenta de vendedor requiere aprobación del administrador",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }

                        // Nombres & Apellidos en 2 columnas
                        val firstNameError = if (!uiState.isLoginMode && (showFieldErrors || firstNameTouched)) {
                            if (uiState.firstName.isBlank()) "El nombre es obligatorio"
                            else FormValidators.validateFirstName(uiState.firstName)
                        } else null

                        val lastNameError = if (!uiState.isLoginMode && (showFieldErrors || lastNameTouched)) {
                            if (uiState.lastName.isBlank()) "Los apellidos son obligatorios"
                            else FormValidators.validateLastName(uiState.lastName)
                        } else null

                        val phoneError = if (!uiState.isLoginMode && (showFieldErrors || phoneTouched)) {
                            if (uiState.phone.isBlank()) "El número de teléfono es obligatorio"
                            else FormValidators.validatePhone(uiState.phone)
                        } else null

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "Nombres titular *" else "Nombres *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.firstName,
                                    onValueChange = {
                                        firstNameTouched = true
                                        onFirstNameChange(it)
                                    },
                                    placeholder = { Text("Tus nombres", fontSize = 13.sp) },
                                    isError = firstNameError != null,
                                    supportingText = firstNameError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = if (firstNameError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) firstNameTouched = true }
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "Apellidos titular *" else "Apellidos *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.lastName,
                                    onValueChange = {
                                        lastNameTouched = true
                                        onLastNameChange(it)
                                    },
                                    placeholder = { Text("Tus apellidos", fontSize = 13.sp) },
                                    isError = lastNameError != null,
                                    supportingText = lastNameError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = if (lastNameError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) lastNameTouched = true }
                                )
                            }
                        }

                        // Teléfono WhatsApp
                        Column {
                            Text(
                                text = if (uiState.selectedRole == UserRole.EMPRENDEDOR) "WhatsApp de contacto *" else "Número de teléfono *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16324F),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = uiState.phone,
                                onValueChange = {
                                    phoneTouched = true
                                    onPhoneChange(it)
                                },
                                placeholder = { Text("Número de teléfono", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                isError = phoneError != null,
                                supportingText = phoneError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = null,
                                        tint = if (phoneError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) phoneTouched = true }
                            )
                        }

                        // CAMPOS EXCLUSIVOS DE VENDEDOR
                        if (uiState.selectedRole == UserRole.EMPRENDEDOR) {
                            val storeNameError = if (!uiState.isLoginMode && (showFieldErrors || storeNameTouched)) {
                                if (uiState.storeName.isBlank()) "El nombre del puesto es obligatorio"
                                else FormValidators.validateStoreName(uiState.storeName)
                            } else null
                            val isOtherCategory = uiState.storeCategory.equals("Otros", ignoreCase = true) || uiState.storeCategory.equals("Otro", ignoreCase = true)
                            val customCategoryError = if (!uiState.isLoginMode && isOtherCategory && (showFieldErrors || customCategoryTouched)) {
                                if (uiState.customCategory.isBlank()) "Especifica la categoría de tu puesto"
                                else FormValidators.validateCustomCategory(uiState.customCategory)
                            } else null
                            val storeDescError = if (!uiState.isLoginMode && (showFieldErrors || storeDescTouched)) {
                                if (uiState.storeDescription.isBlank()) "La descripción del puesto es obligatoria"
                                else FormValidators.validateStoreDescription(uiState.storeDescription)
                            } else null
                            val meetingPointError = if (!uiState.isLoginMode && (showFieldErrors || meetingPointTouched) && uiState.selectedMeetingPoint.isBlank()) "Selecciona un punto de entrega oficial" else null

                            // Nombre de la Tienda
                            Column {
                                Text(
                                    text = "Nombre de la Tienda / Negocio *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.storeName,
                                    onValueChange = {
                                        storeNameTouched = true
                                        onStoreNameChange(it)
                                    },
                                    placeholder = { Text("Ej. Jugos y Snacks Doña Luz", fontSize = 13.sp) },
                                    isError = storeNameError != null,
                                    supportingText = storeNameError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Storefront,
                                            contentDescription = null,
                                            tint = if (storeNameError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) storeNameTouched = true }
                                )
                            }

                            // Categoría de la Tienda (Dropdown)
                            Column {
                                Text(
                                    text = "Categoría del Emprendimiento *",
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

                            // Apartado dinámico para texto libre si se selecciona "Otros" (Bug 20)
                            if (isOtherCategory) {
                                Column {
                                    Text(
                                        text = "¿Qué categoría es tu emprendimiento? *",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16324F),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = uiState.customCategory,
                                        onValueChange = {
                                            customCategoryTouched = true
                                            onCustomCategoryChange(it)
                                        },
                                        placeholder = { Text("Ej. Artesanías, Ropa, Papelería...", fontSize = 13.sp) },
                                        isError = customCategoryError != null,
                                        supportingText = customCategoryError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF4F6F8),
                                            focusedBorderColor = Color(0xFF00A884),
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) customCategoryTouched = true }
                                    )
                                }
                            }

                            // Descripción del Negocio
                            Column {
                                Text(
                                    text = "Descripción de tus productos *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = uiState.storeDescription,
                                    onValueChange = {
                                        storeDescTouched = true
                                        onStoreDescriptionChange(it)
                                    },
                                    placeholder = { Text("Ej. Venta de jugos naturales, sánguches frescos y postres caseros...", fontSize = 13.sp) },
                                    isError = storeDescError != null,
                                    supportingText = storeDescError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = if (storeDescError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) storeDescTouched = true }
                                )
                            }

                            // Punto de Entrega Preferido (Puntos Oficiales Aprobados)
                            Column {
                                Text(
                                    text = "Punto de entrega preferido (Oficial) *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = meetingPointDropdownExpanded,
                                    onExpandedChange = { expanded ->
                                        meetingPointTouched = true
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
                                        isError = meetingPointError != null,
                                        supportingText = meetingPointError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Place,
                                                contentDescription = null,
                                                tint = if (meetingPointError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                    val emailError = if (uiState.isLoginMode) {
                        if (showFieldErrors) FormValidators.validateEmail(uiState.email) else null
                    } else {
                        if (showFieldErrors || emailTouched) {
                            if (uiState.email.isBlank()) "El correo electrónico es obligatorio"
                            else FormValidators.validateEmail(uiState.email)
                        } else null
                    }
                    Column {
                        Text(
                            text = "Correo Electrónico *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = {
                                if (!uiState.isLoginMode) emailTouched = true
                                onEmailChange(it)
                            },
                            placeholder = { Text("tu.correo@ejemplo.com", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            isError = emailError != null,
                            supportingText = emailError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = if (emailError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFB),
                                focusedBorderColor = Color(0xFF00A884),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF16324F),
                                unfocusedTextColor = Color(0xFF16324F)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (!uiState.isLoginMode && it.isFocused) emailTouched = true }
                        )
                    }

                    // Campo Contraseña
                    val passwordError = if (uiState.isLoginMode) {
                        if (showFieldErrors) FormValidators.validatePassword(uiState.password) else null
                    } else {
                        if (showFieldErrors || passwordTouched) {
                            if (uiState.password.isBlank()) "La contraseña es obligatoria"
                            else FormValidators.validatePassword(uiState.password)
                        } else null
                    }
                    Column {
                        Text(
                            text = "Contraseña *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = {
                                if (!uiState.isLoginMode) passwordTouched = true
                                onPasswordChange(it)
                            },
                            placeholder = {
                                Text(
                                    text = if (uiState.isLoginMode) "••••••••" else "Mínimo 6 caracteres",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            isError = passwordError != null,
                            supportingText = passwordError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) } },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (passwordError != null) MaterialTheme.colorScheme.error else Color(0xFF00A884),
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
                                    if (uiState.canSubmit && (uiState.isLoginMode || termsAccepted)) {
                                        onSubmit()
                                    } else {
                                        showFieldErrors = true
                                    }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (!uiState.isLoginMode && it.isFocused) passwordTouched = true }
                        )
                    }

                    // Fila inferior de contraseña para MODO LOGIN
                    if (uiState.isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { onForgotPasswordClick() }
                            )
                        }
                    }

                    // MODO REGISTRO: Checkbox Términos
                    if (!uiState.isLoginMode) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { termsAccepted = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF00A884),
                                        uncheckedColor = Color(0xFFCBD5E1)
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Acepto los ",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "términos y condiciones",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A884),
                                        modifier = Modifier.clickable { showTermsSheet = true }
                                    )
                                }
                            }
                            if (showFieldErrors && !termsAccepted) {
                                Text(
                                    text = "Debes aceptar los términos y condiciones para continuar",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Botón Principal
                    val isButtonEnabled = !uiState.isLoading
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (uiState.isLoginMode) {
                                if (uiState.canSubmit) {
                                    onSubmit()
                                } else {
                                    showFieldErrors = true
                                }
                            } else {
                                if (uiState.canSubmit && termsAccepted) {
                                    onSubmit()
                                } else {
                                    showFieldErrors = true
                                }
                            }
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
                                        else -> "Crear Cuenta"
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

                    // Separador "o"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )
                        Text(
                            text = "o",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )
                    }

                    // Botón Continuar con Google
                    Surface(
                        onClick = {
                            if (!uiState.isLoading) {
                                focusManager.clearFocus()
                                onGoogleSignIn()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00A884)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Conectando con Google...",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continuar con Google",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Links del Pie de Página ordenados y limpios
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        // Alternar entre Iniciar sesión y Registrarse
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isLoginMode) "¿No tienes una cuenta? " else "¿Ya tienes una cuenta? ",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (uiState.isLoginMode) "Regístrate aquí" else "Inicia sesión",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { onTabSelected(!uiState.isLoginMode) }
                            )
                        }

                        // Centro de Ayuda sutil y directo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSupportDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HeadsetMic,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "¿Necesitas ayuda? Centro de soporte",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Powered by KODEX sobrio y profesional
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Powered by",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Image(
                                painter = painterResource(id = R.drawable.kodex_logo),
                                contentDescription = "Logo Kodex",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "KODEX",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        ProfileInfoBottomSheet(
            type = ProfileInfoType.HELP,
            onDismiss = { showSupportDialog = false }
        )
    }

    if (showTermsSheet) {
        ProfileInfoBottomSheet(
            type = ProfileInfoType.TERMS,
            onDismiss = { showTermsSheet = false }
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
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = modifier.defaultMinSize(minHeight = 60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconPainter != null) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.5.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.88f) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
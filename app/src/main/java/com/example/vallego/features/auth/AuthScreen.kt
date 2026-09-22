package com.example.vallego.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalUriHandler
import com.example.vallego.ui.components.SetDarkScreenStatusBar
import com.example.vallego.R
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthRoute(
    onAuthSuccess: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWelcome by rememberSaveable { mutableStateOf(true) }

    // Interceptar el botón Atrás nativo de Android: Si está en login/registro, regresa a la bienvenida en vez de salir de la app
    BackHandler(enabled = !showWelcome) {
        showWelcome = true
    }

    LaunchedEffect(uiState.isSuccess, uiState.profile) {
        val profile = uiState.profile
        if (uiState.isSuccess && (profile != null)) {
            onAuthSuccess(profile)
        }
    }

    if (showWelcome) {
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
            onTabSelected = viewModel::setLoginMode,
            onSubmit = viewModel::submit,
            onDismissError = viewModel::clearError,
            modifier = modifier
        )
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
    onTabSelected: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetDarkScreenStatusBar(isDark = true)
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var showSupportDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.isLoginMode) {
        scrollState.scrollTo(0)
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    // Separación de Nombre y Apellidos para la vista de registro
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    LaunchedEffect(firstName, lastName) {
        if (!uiState.isLoginMode) {
            val combined = "$firstName $lastName".trim()
            onFullNameChange(combined)
        }
    }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(240.dp) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03333D), // Azul verdoso oscuro superior profundo
                        Color(0xFF064D57), // Verde azulado intermedio
                        Color(0xFF0A756F)  // Verde esmeralda / turquesa inferior
                    )
                )
            )
    ) {
        val totalScreenHeight = maxHeight

        // Círculo grande decorativo translúcido en la esquina superior derecha
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-70).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x22FFFFFF),
                            Color(0x0EFFFFFF)
                        )
                    )
                )
        )

        // Círculo decorativo translúcido en el costado izquierdo (detrás del card)
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .offset(x = (-140).dp, y = 260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x20FFFFFF),
                            Color(0x06FFFFFF)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECCIÓN SUPERIOR: HEADER CON LOGO OFICIAL DEL PROTOTIPO
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
                // Logo Oficial Completo con letras blancas para fondo oscuro
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.vallego.R.drawable.campus_logo),
                        contentDescription = "Logo Campus Go",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(58.dp)
                    )
                    Image(
                        painter = painterResource(id = com.example.vallego.R.drawable.letras_logo_white),
                        contentDescription = "Campus Go",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Titular Motivacional Grande si está en Login
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

                        Text(
                            text = "Pide desde la comodidad de tu Universidad",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFD4F3EE)
                        )
                    }
                }
            }

            // SECCIÓN INFERIOR: TARJETA BLANCA BORDE A BORDE CON ESQUINAS SUPERIORES CURVAS
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
                        .padding(top = 28.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado según el Modo (Login o Registro)
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
                                text = "Crea tu cuenta",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Únete a la experiencia Campus Go",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF64748B)
                            )
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

                    // FORMULARIO MODO REGISTRO: Nombres & Apellidos en 2 columnas
                    if (!uiState.isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Nombres
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nombres",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
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

                            // Apellidos
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Apellidos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
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

                        // Número de teléfono
                        Column {
                            Text(
                                text = "Número de teléfono",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16324F),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = uiState.phone,
                                onValueChange = onPhoneChange,
                                placeholder = { Text("+51 000 000 000", fontSize = 13.sp) },
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
                    }

                    // Campo Correo Electrónico
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
                            placeholder = { Text("nombre@gmail.com", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
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
                                    text = if (uiState.isLoginMode) "••••••••" else "Mínimo 8 caracteres",
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
                                imeAction = if (uiState.isLoginMode) ImeAction.Done else ImeAction.Next
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
                                text = "Regístrate ahora",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { onTabSelected(false) }
                            )
                        }
                    }

                    // MODO REGISTRO: Rol y Términos
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
                                    icon = Icons.Default.ShoppingBag,
                                    isSelected = uiState.selectedRole == UserRole.COMPRADOR,
                                    onClick = { onRoleChange(UserRole.COMPRADOR) },
                                    modifier = Modifier.weight(1f)
                                )
                                RoleCardButton(
                                    title = "Vendedor",
                                    iconPainter = painterResource(id = R.drawable.ic_store_custom),
                                    isSelected = uiState.selectedRole == UserRole.EMPRENDEDOR,
                                    onClick = { onRoleChange(UserRole.EMPRENDEDOR) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Checkbox Términos
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

                    // Botón Principal Verde Esmeralda (#00A884)
                    val isButtonEnabled = uiState.canSubmit && (uiState.isLoginMode || termsAccepted)
                    val shadowElevation by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isButtonEnabled) 6.dp else 0.dp,
                        label = "buttonShadowElevation"
                    )
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
                            .shadow(
                                elevation = shadowElevation,
                                shape = RoundedCornerShape(16.dp),
                                clip = false,
                                spotColor = Color(0xFF00A884),
                                ambientColor = Color(0x3300A884)
                            )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (uiState.isLoginMode) "Iniciar sesión" else "Crear mi cuenta",
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
                                    text = "¿Necesitas ayuda? ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Centro de soporte",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884),
                                    modifier = Modifier.clickable { showSupportDialog = true }
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¿Ya tienes una cuenta? ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Inicia sesión",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884),
                                    modifier = Modifier.clickable { onTabSelected(true) }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Firma de Marca KODEX usando kodex_logo.png
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
                                painter = painterResource(id = com.example.vallego.R.drawable.kodex_logo),
                                contentDescription = "Logo Kodex",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(20.dp)
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
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = {
                Text(
                    text = "Centro de Soporte ValleGO",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16324F)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Tienes inconvenientes para iniciar sesión o registrarte? Comunícate con nuestro canal de atención universitaria:",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "📧 Correo: soporte@vallego.app\n💬 WhatsApp: +51 987 654 321\n⏰ Horario: Lun - Sáb 8:00 AM a 8:00 PM",
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
                            uriHandler.openUri("mailto:soporte@vallego.app?subject=Soporte%20ValleGO")
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
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun RoleCardButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFE6F6F3) else Color(0xFFFAFAFA),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF00A884) else Color(0xFFE2E8F0)
        ),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF00A884) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF00A884) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color(0xFF00A884) else Color(0xFF334155)
            )
        }
    }
}
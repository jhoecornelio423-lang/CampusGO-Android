package com.example.vallego.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AdminRepository
import com.example.vallego.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadApprovedMeetingPoints()
    }

    private fun loadApprovedMeetingPoints() {
        val defaultPoints = listOf(
            CampusMeetingPoint(id = "mp-1", name = "Biblioteca Central - Puerta Principal", campus = "UCV - Lima Norte", isActive = true),
            CampusMeetingPoint(id = "mp-2", name = "Pabellón A - Zona de Bancas", campus = "UCV - Lima Norte", isActive = true),
            CampusMeetingPoint(id = "mp-3", name = "Pabellón C - Explanada Central", campus = "UCV - Lima Norte", isActive = true),
            CampusMeetingPoint(id = "mp-4", name = "Cafetería Campus - Terraza", campus = "UCV - Lima Norte", isActive = true),
            CampusMeetingPoint(id = "mp-5", name = "Puerta Principal - Acceso Exterior", campus = "UCV - Lima Norte", isActive = true),
            CampusMeetingPoint(id = "mp-6", name = "Puerta 2 - Reja Auxiliar", campus = "UCV - Lima Norte", isActive = true)
        )

        _uiState.update {
            it.copy(
                availableMeetingPoints = defaultPoints,
                selectedMeetingPoint = defaultPoints.first().name
            )
        }

        viewModelScope.launch {
            adminRepository?.observeMeetingPoints()?.collect { points ->
                val active = points.filter { it.isActive }
                if (active.isNotEmpty()) {
                    _uiState.update { current ->
                        current.copy(
                            availableMeetingPoints = active,
                            selectedMeetingPoint = if (current.selectedMeetingPoint.isBlank() || active.none { it.name == current.selectedMeetingPoint }) {
                                active.first().name
                            } else {
                                current.selectedMeetingPoint
                            }
                        )
                    }
                }
            }
        }
    }

    fun setScreenMode(mode: AuthScreenMode) {
        _uiState.update {
            it.copy(
                screenMode = mode,
                isLoginMode = (mode == AuthScreenMode.LOGIN),
                errorMessage = null
            )
        }
    }

    fun setLoginMode(isLogin: Boolean) {
        setScreenMode(if (isLogin) AuthScreenMode.LOGIN else AuthScreenMode.REGISTER)
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun onCampusChange(campus: String) {
        _uiState.update { it.copy(campus = campus, errorMessage = null) }
    }

    fun onStoreNameChange(storeName: String) {
        _uiState.update { it.copy(storeName = storeName, errorMessage = null) }
    }

    fun onStoreCategoryChange(category: String) {
        _uiState.update { it.copy(storeCategory = category, errorMessage = null) }
    }

    fun onStoreDescriptionChange(description: String) {
        _uiState.update { it.copy(storeDescription = description, errorMessage = null) }
    }

    fun onMeetingPointChange(pointName: String) {
        _uiState.update { it.copy(selectedMeetingPoint = pointName, errorMessage = null) }
    }

    fun onOtpCodeChange(otp: String) {
        if (otp.length <= 8 && otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otpCode = otp, errorMessage = null) }
        }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onConfirmNewPasswordChange(password: String) {
        _uiState.update { it.copy(confirmNewPassword = password, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            if (current.isLoginMode) {
                authRepository.signIn(current.email, current.password)
                    .onSuccess { profile ->
                        if (profile.isSellerPendingApproval) {
                            viewModelScope.launch {
                                runCatching { authRepository.signOut() }
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    screenMode = AuthScreenMode.LOGIN,
                                    isLoginMode = true,
                                    errorMessage = "Tu cuenta está en revisión. El administrador aún debe aprobar tu solicitud para que puedas acceder.",
                                    infoMessage = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, isSuccess = true, profile = profile)
                            }
                        }
                    }
                    .onFailure { exception ->
                        val msg = exception.message.orEmpty()
                        if (msg.contains("email_not_confirmed", ignoreCase = true)) {
                            // Ofrecer ir directamente a verificar OTP
                            startResendCountdown(60)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    screenMode = AuthScreenMode.VERIFY_OTP,
                                    infoMessage = "Tu correo aún no ha sido verificado. Ingresa el código que te enviamos."
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = parseAuthErrorMessage(exception, isLoginMode = true)
                                )
                            }
                        }
                    }
            } else {
                authRepository.signUp(
                    email = current.email,
                    password = current.password,
                    fullName = current.fullName,
                    phone = current.phone,
                    role = current.selectedRole,
                    campus = current.campus,
                    storeName = current.storeName.takeIf { current.selectedRole == UserRole.EMPRENDEDOR },
                    category = current.storeCategory.takeIf { current.selectedRole == UserRole.EMPRENDEDOR },
                    description = current.storeDescription.takeIf { current.selectedRole == UserRole.EMPRENDEDOR },
                    meetingPoint = current.selectedMeetingPoint.takeIf { current.selectedRole == UserRole.EMPRENDEDOR }
                )
                    .onSuccess { profile ->
                        if (profile != null) {
                            // Sesión inmediata (si la confirmación de correo estuviese desactivada)
                            viewModelScope.launch {
                                runCatching { authRepository.signOut() }
                            }
                            val isSeller = profile.isSellerPendingApproval || current.selectedRole == UserRole.EMPRENDEDOR
                            val msg = if (isSeller) {
                                "Tu solicitud de registro fue enviada con éxito. Ahora el administrador debe revisar tu cuenta; en caso de que sea aprobada, podrás acceder a la app."
                            } else {
                                "Registro completado con éxito. Ya puedes iniciar sesión con tu cuenta."
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuccess = false,
                                    screenMode = AuthScreenMode.LOGIN,
                                    isLoginMode = true,
                                    infoMessage = msg,
                                    errorMessage = null,
                                    password = ""
                                )
                            }
                        } else {
                            // Se envió el correo con código OTP
                            startResendCountdown(60)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    screenMode = AuthScreenMode.VERIFY_OTP,
                                    otpCode = "",
                                    infoMessage = "Hemos enviado un código de verificación a ${current.email}"
                                )
                            }
                        }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = parseAuthErrorMessage(exception, isLoginMode = false)
                            )
                        }
                    }
            }
        }
    }

    fun verifyOtp() {
        val current = _uiState.value
        if (!current.canVerifyOtp) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.verifyEmailOtp(
                email = current.email,
                token = current.otpCode,
                isSeller = (current.selectedRole == UserRole.EMPRENDEDOR)
            )
                .onSuccess { profile ->
                    viewModelScope.launch {
                        runCatching { authRepository.signOut() }
                    }
                    val isSeller = profile.isSellerPendingApproval || current.selectedRole == UserRole.EMPRENDEDOR
                    val msg = if (isSeller) {
                        "Tu correo fue verificado con éxito. Ahora el administrador debe revisar tu cuenta; en caso de que sea aprobada, podrás acceder a la app."
                    } else {
                        "Tu correo fue verificado con éxito. Ya puedes iniciar sesión con tu cuenta."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            screenMode = AuthScreenMode.LOGIN,
                            isLoginMode = true,
                            infoMessage = msg,
                            errorMessage = null,
                            otpCode = "",
                            password = ""
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = parseAuthErrorMessage(exception, isLoginMode = false)
                        )
                    }
                }
        }
    }

    fun resendOtp() {
        val current = _uiState.value
        if (current.countdownSeconds > 0) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.resendOtp(current.email)
                .onSuccess {
                    startResendCountdown(60)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Nuevo código enviado a ${current.email}"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = parseAuthErrorMessage(exception, isLoginMode = false)
                        )
                    }
                }
        }
    }

    fun openForgotPassword() {
        _uiState.update {
            it.copy(
                screenMode = AuthScreenMode.FORGOT_PASSWORD_EMAIL,
                errorMessage = null,
                infoMessage = null,
                otpCode = "",
                newPassword = "",
                confirmNewPassword = ""
            )
        }
    }

    fun sendForgotPasswordEmail() {
        val current = _uiState.value
        if (!current.canSubmitForgotPasswordEmail) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.sendPasswordResetOtp(current.email)
                .onSuccess {
                    startResendCountdown(60)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screenMode = AuthScreenMode.FORGOT_PASSWORD_OTP,
                            infoMessage = "Código de recuperación enviado a ${current.email}"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = parseAuthErrorMessage(exception, isLoginMode = true)
                        )
                    }
                }
        }
    }

    fun resetPasswordWithOtp() {
        val current = _uiState.value
        if (!current.canResetPassword) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.resetPasswordWithOtp(
                email = current.email,
                token = current.otpCode,
                newPassword = current.newPassword
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screenMode = AuthScreenMode.LOGIN,
                            isLoginMode = true,
                            infoMessage = "Contraseña restablecida con éxito. Inicia sesión con tu nueva contraseña.",
                            password = ""
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = parseAuthErrorMessage(exception, isLoginMode = false)
                        )
                    }
                }
        }
    }

    fun backToLogin() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                screenMode = AuthScreenMode.LOGIN,
                isLoginMode = true,
                errorMessage = null,
                infoMessage = null,
                otpCode = "",
                newPassword = "",
                confirmNewPassword = ""
            )
        }
    }

    fun signOutFromPending() {
        viewModelScope.launch {
            authRepository.signOut()
            backToLogin()
        }
    }

    private fun startResendCountdown(seconds: Int = 60) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 0) {
                _uiState.update { it.copy(countdownSeconds = i) }
                delay(1000)
            }
        }
    }

    private fun parseAuthErrorMessage(exception: Throwable, isLoginMode: Boolean): String {
        val msg = exception.message.orEmpty()
        return when {
            msg.contains("user_already_exists", ignoreCase = true) ||
                    msg.contains("User already registered", ignoreCase = true) -> {
                "Este correo electrónico ya está registrado. Por favor, inicia sesión."
            }
            msg.contains("invalid_credentials", ignoreCase = true) ||
                    msg.contains("Invalid login credentials", ignoreCase = true) -> {
                "Correo o contraseña incorrectos. Por favor, verifica tus datos."
            }
            msg.contains("email_not_confirmed", ignoreCase = true) -> {
                "Tu correo electrónico no ha sido confirmado aún. Revisa el código en tu bandeja."
            }
            msg.contains("token has expired", ignoreCase = true) ||
                    msg.contains("otp_expired", ignoreCase = true) -> {
                "El código ha expirado. Por favor, solicita uno nuevo."
            }
            msg.contains("invalid token", ignoreCase = true) ||
                    msg.contains("otp_invalid", ignoreCase = true) ||
                    msg.contains("Token is invalid", ignoreCase = true) -> {
                "El código de verificación es incorrecto. Verifica el correo e inténtalo de nuevo."
            }
            msg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    msg.contains("rate limit", ignoreCase = true) -> {
                "Has realizado demasiados intentos. Por favor, espera un momento e inténtalo de nuevo."
            }
            msg.contains("weak_password", ignoreCase = true) ||
                    msg.contains("Password should be at least", ignoreCase = true) -> {
                "La contraseña es muy débil. Debe tener al menos 6 caracteres."
            }
            msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("timed out", ignoreCase = true) -> {
                "El servidor tardó en responder al enviar el código de verificación. Por favor, verifica la conexión o inténtalo de nuevo."
            }
            msg.contains("Database error saving new user", ignoreCase = true) ||
                    msg.contains("database error", ignoreCase = true) -> {
                "Error al procesar el registro en el servidor. Por favor, inténtalo más tarde."
            }
            msg.contains("Error sending confirmation email", ignoreCase = true) -> {
                "No se pudo enviar el correo de verificación. Por favor, verifica que tu correo sea válido o inténtalo más tarde."
            }
            (exception is java.net.UnknownHostException) ||
                    (exception is java.net.SocketTimeoutException) ||
                    (exception is java.net.ConnectException) -> {
                "No se pudo conectar con el servidor. Verifica tu conexión a internet."
            }
            msg.contains("revisión", ignoreCase = true) || msg.contains("revision", ignoreCase = true) -> {
                "Tu cuenta está en revisión. El administrador aún debe aprobar tu solicitud para que puedas acceder."
            }
            (exception is IllegalArgumentException || exception is IllegalStateException) && !exception.message.isNullOrBlank() -> {
                exception.message!!
            }
            else -> {
                if (isLoginMode) {
                    "Error al iniciar sesión. Por favor, verifica tus datos e inténtalo de nuevo."
                } else {
                    "No se pudo completar el registro. Por favor, verifica tus datos e inténtalo de nuevo."
                }
            }
        }
    }
}
package com.example.vallego.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setLoginMode(isLogin: Boolean) {
        _uiState.update { it.copy(isLoginMode = isLogin, errorMessage = null) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            if (current.isLoginMode) {
                authRepository.signIn(current.email, current.password)
                    .onSuccess { profile ->
                        _uiState.update { it.copy(isLoading = false, isSuccess = true, profile = profile) }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = parseAuthErrorMessage(exception, isLoginMode = true),
                            )
                        }
                    }
            } else {
                authRepository.signUp(
                    current.email,
                    current.password,
                    current.fullName,
                    current.phone,
                    current.selectedRole,
                )
                    .onSuccess { profile ->
                        _uiState.update { it.copy(isLoading = false, isSuccess = true, profile = profile) }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = parseAuthErrorMessage(exception, isLoginMode = false),
                            )
                        }
                    }
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
                "Tu correo electrónico no ha sido confirmado aún."
            }
            msg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    msg.contains("rate limit", ignoreCase = true) -> {
                "Has realizado demasiados intentos. Por favor, espera unos minutos e inténtalo de nuevo."
            }
            msg.contains("weak_password", ignoreCase = true) ||
                    msg.contains("Password should be at least", ignoreCase = true) -> {
                "La contraseña es muy débil. Debe tener al menos 6 caracteres."
            }
            (exception is java.net.UnknownHostException) ||
                    (exception is java.net.SocketTimeoutException) ||
                    (exception is java.net.ConnectException) -> {
                "No se pudo conectar con el servidor. Verifica tu conexión a internet."
            }
            (exception is IllegalArgumentException) && !exception.message.isNullOrBlank() -> {
                exception.message!!
            }
            else -> {
                if (isLoginMode) {
                    "Error al iniciar sesión. Por favor, verifica tus datos e inténtalo de nuevo."
                } else {
                    "Error al registrar cuenta. Por favor, inténtalo de nuevo."
                }
            }
        }
    }
}
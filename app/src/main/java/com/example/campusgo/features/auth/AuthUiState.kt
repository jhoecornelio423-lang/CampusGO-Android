package com.example.campusgo.features.auth

import com.example.campusgo.core.util.FormValidators
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole

enum class AuthScreenMode {
    LOGIN,
    REGISTER,
    VERIFY_OTP,
    FORGOT_PASSWORD_EMAIL,
    FORGOT_PASSWORD_OTP,
    SELLER_PENDING_APPROVAL
}

data class AuthUiState(
    val screenMode: AuthScreenMode = AuthScreenMode.LOGIN,
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val selectedRole: UserRole = UserRole.COMPRADOR,
    val campus: String = "UCV - Lima Norte",
    val storeName: String = "",
    val storeCategory: String = "Comidas y Menús",
    val customCategory: String = "",
    val storeDescription: String = "",
    val selectedMeetingPoint: String = "",
    val availableMeetingPoints: List<CampusMeetingPoint> = emptyList(),
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val countdownSeconds: Int = 0,
    val infoMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val profile: UserProfile? = null,
    val isSellerPendingApproval: Boolean = false
) {
    val fullName: String get() = "$firstName $lastName".trim()

    val isEmailValid: Boolean get() = FormValidators.validateEmail(email) == null

    val isInstitutionalEmailValid: Boolean get() = isEmailValid

    val effectiveCategory: String
        get() = if (storeCategory.equals("Otros", ignoreCase = true) || storeCategory.equals("Otro", ignoreCase = true)) {
            customCategory.trim()
        } else {
            storeCategory.trim()
        }

    val canSubmit: Boolean get() {
        if (!isEmailValid || FormValidators.validatePassword(password) != null || isLoading) return false
        if (isLoginMode) return true
        if (FormValidators.validateFirstName(firstName) != null ||
            FormValidators.validateLastName(lastName) != null ||
            FormValidators.validatePhone(phone) != null) return false
        if (selectedRole == UserRole.EMPRENDEDOR) {
            val isCustomCategory = storeCategory.equals("Otros", ignoreCase = true) || storeCategory.equals("Otro", ignoreCase = true)
            val categoryValid = if (isCustomCategory) {
                FormValidators.validateCustomCategory(customCategory) == null
            } else {
                storeCategory.isNotBlank()
            }
            return FormValidators.validateStoreName(storeName) == null &&
                    categoryValid &&
                    FormValidators.validateStoreDescription(storeDescription) == null &&
                    selectedMeetingPoint.isNotBlank()
        }
        return true
    }

    val canVerifyOtp: Boolean get() {
        val len = otpCode.trim().length
        return (len == 6 || len == 8) && !isLoading
    }

    val canSubmitForgotPasswordEmail: Boolean get() {
        return isEmailValid && !isLoading
    }

    val canResetPassword: Boolean get() {
        val len = otpCode.trim().length
        return (len == 6 || len == 8) &&
                newPassword.length >= 6 &&
                newPassword == confirmNewPassword &&
                !isLoading
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val STORE_CATEGORIES = listOf(
            "Comidas y Menús",
            "Snacks y Antojos",
            "Bebidas y Jugos",
            "Postres y Repostería",
            "Librería y Útiles",
            "Tecnología y Accesorios",
            "Otros"
        )
    }
}
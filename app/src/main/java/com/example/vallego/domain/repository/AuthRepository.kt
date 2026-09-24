package com.example.vallego.domain.repository

import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentProfile: StateFlow<UserProfile?>
    val isAuthenticated: StateFlow<Boolean>
    val isSessionChecking: StateFlow<Boolean>

    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: UserRole = UserRole.COMPRADOR,
        campus: String = "UCV - Lima Norte",
        storeName: String? = null,
        category: String? = null,
        description: String? = null,
        meetingPoint: String? = null
    ): Result<UserProfile?>

    suspend fun verifyEmailOtp(
        email: String,
        token: String,
        isSeller: Boolean = false
    ): Result<UserProfile>

    suspend fun resendOtp(email: String): Result<Unit>
    suspend fun sendPasswordResetOtp(email: String): Result<Unit>
    suspend fun resetPasswordWithOtp(email: String, token: String, newPassword: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
    suspend fun refreshProfile(): Result<UserProfile?>
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    fun isValidEmail(email: String): Boolean
    fun isValidInstitutionalEmail(email: String): Boolean = isValidEmail(email)
}
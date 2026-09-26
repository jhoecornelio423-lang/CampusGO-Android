package com.example.vallego.data.repository

import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

class AuthRepositoryImpl(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    override val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isSessionChecking = MutableStateFlow(true)
    override val isSessionChecking: StateFlow<Boolean> = _isSessionChecking.asStateFlow()

    private val isSuppressingSessionBroadcast = java.util.concurrent.atomic.AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (isSuppressingSessionBroadcast.get()) {
                    return@collect
                }
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = auth.currentUserOrNull()
                        if (user != null) {
                            val meta = user.userMetadata
                            val metaName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: "Estudiante Universitario"
                            val metaRoleStr = meta?.get("role")?.jsonPrimitive?.contentOrNull ?: "comprador"
                            val metaStoreName = meta?.get("business_name")?.jsonPrimitive?.contentOrNull

                            val profile = try {
                                fetchProfile(user.id)
                            } catch (_: Exception) {
                                val role = when (metaRoleStr.lowercase()) {
                                    "admin" -> UserRole.ADMIN
                                    "emprendedor" -> UserRole.EMPRENDEDOR
                                    else -> UserRole.COMPRADOR
                                }
                                val metaStatus = meta?.get("business_status")?.jsonPrimitive?.contentOrNull ?: if (role == UserRole.EMPRENDEDOR) "PENDIENTE" else "ABIERTO"
                                UserProfile(
                                    id = user.id,
                                    fullName = metaName,
                                    role = role,
                                    businessName = metaStoreName,
                                    businessStatus = metaStatus
                                )
                            }

                            val isSeller = profile.role == UserRole.EMPRENDEDOR ||
                                    !profile.businessName.isNullOrBlank() ||
                                    metaRoleStr.equals("emprendedor", ignoreCase = true) ||
                                    !metaStoreName.isNullOrBlank()

                            if (isSeller) {
                                val isApproved = try {
                                    val apps = postgrest.from("seller_applications").select {
                                        filter { eq("user_id", user.id) }
                                    }.decodeList<SellerApplication>()
                                    apps.any { it.status == ApplicationStatus.APROBADA }
                                } catch (_: Exception) { false }

                                if (!isApproved || profile.businessStatus.equals("SUSPENDIDO", ignoreCase = true) || profile.businessStatus.equals("RECHAZADO", ignoreCase = true)) {
                                    try {
                                        auth.signOut()
                                    } catch (_: Exception) {}
                                    _currentProfile.value = null
                                    _isAuthenticated.value = false
                                } else {
                                    _currentProfile.value = profile
                                    _isAuthenticated.value = true
                                }
                            } else {
                                _currentProfile.value = profile
                                _isAuthenticated.value = true
                            }
                        } else {
                            _currentProfile.value = null
                            _isAuthenticated.value = false
                        }
                        _isSessionChecking.value = false
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentProfile.value = null
                        _isAuthenticated.value = false
                        _isSessionChecking.value = false
                    }
                    else -> Unit
                }
            }
        }
        scope.launch {
            // Límite de seguridad para no quedar bloqueado en carga inicial si la red demora
            kotlinx.coroutines.delay(2500)
            if (_isSessionChecking.value) {
                _isSessionChecking.value = false
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    override fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
    }

    override fun isValidInstitutionalEmail(email: String): Boolean {
        return isValidEmail(email)
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(
                IllegalArgumentException("Ingresa un correo electrónico válido")
            )
        }

        return try {
            auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }
            val user = auth.currentUserOrNull()
                ?: throw IllegalStateException("Sesión no iniciada correctamente")
            val meta = user.userMetadata
            val metaName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: "Usuario CampusGO"
            val metaRoleStr = meta?.get("role")?.jsonPrimitive?.contentOrNull ?: "comprador"
            val metaStoreName = meta?.get("business_name")?.jsonPrimitive?.contentOrNull
            val metaStatus = meta?.get("business_status")?.jsonPrimitive?.contentOrNull
            val metaCampus = meta?.get("campus")?.jsonPrimitive?.contentOrNull ?: "UCV - Lima Norte"
            val metaPhone = meta?.get("phone")?.jsonPrimitive?.contentOrNull ?: ""

            var profile = try {
                fetchProfile(user.id)
            } catch (_: Exception) {
                val role = when (metaRoleStr.lowercase()) {
                    "admin" -> UserRole.ADMIN
                    "emprendedor" -> UserRole.EMPRENDEDOR
                    else -> UserRole.COMPRADOR
                }
                UserProfile(
                    id = user.id,
                    fullName = metaName,
                    phone = metaPhone,
                    campus = metaCampus,
                    role = role,
                    businessName = metaStoreName,
                    businessStatus = metaStatus ?: if (role == UserRole.EMPRENDEDOR) "PENDIENTE" else "ABIERTO"
                )
            }

            // 1. Validar si la cuenta está suspendida
            if (profile.role.isSuspended || profile.businessStatus.equals("SUSPENDIDO", ignoreCase = true)) {
                try {
                    auth.signOut()
                } catch (_: Exception) {}
                _currentProfile.value = null
                _isAuthenticated.value = false
                val reasonText = profile.suspensionReason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                return Result.failure(
                    IllegalStateException("Tu cuenta se encuentra suspendida por la administración$reasonText")
                )
            }

            // 2. Validar si la solicitud fue rechazada
            if (profile.businessStatus.equals("RECHAZADO", ignoreCase = true)) {
                try {
                    auth.signOut()
                } catch (_: Exception) {}
                _currentProfile.value = null
                _isAuthenticated.value = false
                val reasonText = profile.suspensionReason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                return Result.failure(
                    IllegalStateException("Tu solicitud de vendedor fue rechazada por la administración$reasonText")
                )
            }

            // 3. Validar si es un emprendedor: TODO VENDEDOR DEBE ESTAR APROBADO POR ADMIN
            val isSeller = profile.role == UserRole.EMPRENDEDOR ||
                    !profile.businessName.isNullOrBlank() ||
                    metaRoleStr.equals("emprendedor", ignoreCase = true) ||
                    !metaStoreName.isNullOrBlank()

            if (isSeller) {
                val latestApp = try {
                    postgrest.from("seller_applications").select {
                        filter { eq("user_id", user.id) }
                    }.decodeList<SellerApplication>().maxByOrNull { it.createdAt ?: "" }
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepositoryImpl", "Error al consultar seller_applications: ${e.message}")
                    null
                }

                if (latestApp != null && latestApp.status == ApplicationStatus.APROBADA) {
                    // ¡El administrador ya aprobó la solicitud! Actualizamos el perfil en Supabase
                    try {
                        postgrest.from("profiles").update(
                            buildJsonObject {
                                put("business_status", "ABIERTO")
                                put("accepting_orders", true)
                                if (latestApp.storeName.isNotBlank()) put("business_name", latestApp.storeName)
                            }
                        ) {
                            filter { eq("id", user.id) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepositoryImpl", "Error al sincronizar estado aprobado: ${e.message}")
                    }
                    profile = profile.copy(businessStatus = "ABIERTO", acceptingOrders = true)
                } else if (latestApp != null && latestApp.status == ApplicationStatus.RECHAZADA) {
                    try {
                        postgrest.from("profiles").update(
                            buildJsonObject {
                                put("business_status", "RECHAZADO")
                                put("accepting_orders", false)
                                latestApp.rejectionReason?.let { put("suspension_reason", it) }
                            }
                        ) {
                            filter { eq("id", user.id) }
                        }
                    } catch (_: Exception) {}
                    try { auth.signOut() } catch (_: Exception) {}
                    _currentProfile.value = null
                    _isAuthenticated.value = false
                    val reason = latestApp.rejectionReason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                    return Result.failure(
                        IllegalStateException("Tu solicitud de vendedor fue rechazada por la administración$reason")
                    )
                } else {
                    // Estado PENDIENTE o sin postulación aún: bloquear acceso y registrar si faltaba
                    if (latestApp == null) {
                        try {
                            postgrest.from("seller_applications").insert(
                                buildJsonObject {
                                    put("id", UUID.randomUUID().toString())
                                    put("user_id", user.id)
                                    put("dni", "")
                                    put("open_time", "08:00:00")
                                    put("close_time", "20:00:00")
                                    put("full_name", profile.fullName.ifBlank { metaName })
                                    put("phone", profile.phone.ifBlank { metaPhone })
                                    put("business_name", profile.businessName ?: metaStoreName ?: "Mi Tienda")
                                    put("business_category", profile.businessCategory ?: meta?.get("business_category")?.jsonPrimitive?.contentOrNull ?: "Varios")
                                    put("description", profile.businessDescription ?: meta?.get("business_description")?.jsonPrimitive?.contentOrNull ?: "")
                                    (profile.supportedMeetingPoints.firstOrNull() ?: meta?.get("meeting_point")?.jsonPrimitive?.contentOrNull)?.let { put("proposed_location", it) }
                                    put("status", "pending")
                                }
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("AuthRepositoryImpl", "Error al asegurar solicitud en signIn: ${e.message}")
                        }
                    }

                    try {
                        postgrest.from("profiles").update(
                            buildJsonObject {
                                put("business_status", "PENDIENTE")
                                put("accepting_orders", false)
                                (profile.businessName ?: metaStoreName)?.let { put("business_name", it) }
                            }
                        ) {
                            filter { eq("id", user.id) }
                        }
                    } catch (_: Exception) {}

                    try { auth.signOut() } catch (_: Exception) {}
                    _currentProfile.value = null
                    _isAuthenticated.value = false
                    return Result.failure(
                        IllegalStateException("Tu cuenta está en revisión. El administrador aún debe aprobar tu solicitud para que puedas acceder.")
                    )
                }
            }

            _currentProfile.value = profile
            _isAuthenticated.value = true
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: UserRole,
        campus: String,
        storeName: String?,
        category: String?,
        description: String?,
        meetingPoint: String?
    ): Result<UserProfile?> {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(
                IllegalArgumentException("Ingresa un correo electrónico válido")
            )
        }

        isSuppressingSessionBroadcast.set(true)
        return try {
            auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", fullName.trim())
                    put("phone", phone.trim())
                    put("role", role.name.lowercase())
                    put("campus", campus.trim())
                    if (role == UserRole.EMPRENDEDOR) {
                        storeName?.let { put("business_name", it.trim()) }
                        category?.let { put("business_category", it.trim()) }
                        description?.let { put("business_description", it.trim()) }
                        meetingPoint?.let { put("meeting_point", it.trim()) }
                        put("business_status", "PENDIENTE")
                    }
                }
            }

            val user = auth.currentUserOrNull()
            val profile = if (user != null) {
                // Si la sesión es inmediata (ej. confirmación por correo desactivada en Supabase)
                if (role == UserRole.EMPRENDEDOR) {
                    try {
                        postgrest.from("seller_applications").insert(
                            buildJsonObject {
                                put("id", UUID.randomUUID().toString())
                                put("user_id", user.id)
                                put("dni", "")
                                put("open_time", "08:00:00")
                                put("close_time", "20:00:00")
                                put("full_name", fullName.trim())
                                put("phone", phone.trim())
                                put("business_name", storeName?.trim() ?: "Mi Tienda")
                                put("business_category", category?.trim() ?: "Varios")
                                put("description", description?.trim() ?: "")
                                meetingPoint?.let { put("proposed_location", it.trim()) }
                                put("status", "pending")
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepositoryImpl", "Error al crear seller_application en signUp: ${e.message}")
                    }

                    try {
                        postgrest.from("profiles").update(
                            buildJsonObject {
                                put("role", "emprendedor")
                                put("campus", campus.trim())
                                put("business_name", storeName?.trim() ?: "Mi Tienda")
                                put("business_category", category?.trim())
                                put("business_description", description?.trim())
                                put("business_status", "PENDIENTE")
                                put("accepting_orders", false)
                                meetingPoint?.let {
                                    put("supported_meeting_points", buildJsonArray { add(it.trim()) })
                                }
                            }
                        ) {
                            filter { eq("id", user.id) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepositoryImpl", "Error al actualizar profile en signUp: ${e.message}")
                    }
                }

                try {
                    fetchProfile(user.id)
                } catch (_: Exception) {
                    UserProfile(
                        id = user.id,
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        role = role,
                        campus = campus,
                        businessName = storeName?.trim(),
                        businessCategory = category?.trim(),
                        businessDescription = description?.trim(),
                        businessStatus = if (role == UserRole.EMPRENDEDOR) "PENDIENTE" else "ABIERTO",
                        acceptingOrders = (role != UserRole.EMPRENDEDOR),
                        supportedMeetingPoints = meetingPoint?.let { listOf(it.trim()) } ?: emptyList()
                    )
                }
            } else {
                null
            }

            // Siempre cerramos la sesión para evitar auto-login tras registrarse
            if (profile != null) {
                try {
                    auth.signOut()
                } catch (_: Exception) {}
                _currentProfile.value = null
                _isAuthenticated.value = false
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            kotlinx.coroutines.delay(100)
            isSuppressingSessionBroadcast.set(false)
            _currentProfile.value = null
            _isAuthenticated.value = false
        }
    }

    override suspend fun verifyEmailOtp(
        email: String,
        token: String,
        isSeller: Boolean
    ): Result<UserProfile> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedToken = token.trim()

        isSuppressingSessionBroadcast.set(true)
        return try {
            auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = trimmedEmail,
                token = trimmedToken
            )

            val user = auth.currentUserOrNull()
                ?: throw IllegalStateException("No se pudo iniciar sesión tras verificar el código")

            val meta = user.userMetadata
            val metaName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: "Usuario CampusGO"
            val metaPhone = meta?.get("phone")?.jsonPrimitive?.contentOrNull ?: ""
            val metaCampus = meta?.get("campus")?.jsonPrimitive?.contentOrNull ?: "UCV - Lima Norte"
            val metaStoreName = meta?.get("business_name")?.jsonPrimitive?.contentOrNull
            val metaCategory = meta?.get("business_category")?.jsonPrimitive?.contentOrNull
            val metaDescription = meta?.get("business_description")?.jsonPrimitive?.contentOrNull
            val metaMeetingPoint = meta?.get("meeting_point")?.jsonPrimitive?.contentOrNull

            val effectiveIsSeller = isSeller || !metaStoreName.isNullOrBlank()

            if (effectiveIsSeller) {
                try {
                    postgrest.from("seller_applications").insert(
                        buildJsonObject {
                            put("id", UUID.randomUUID().toString())
                            put("user_id", user.id)
                            put("dni", "")
                            put("open_time", "08:00:00")
                            put("close_time", "20:00:00")
                            put("full_name", metaName)
                            put("phone", metaPhone)
                            put("business_name", metaStoreName ?: "Mi Tienda")
                            put("business_category", metaCategory ?: "Varios")
                            put("description", metaDescription ?: "")
                            metaMeetingPoint?.let { put("proposed_location", it) }
                            put("status", "pending")
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepositoryImpl", "Error en seller_applications insert: ${e.message}")
                }

                try {
                    postgrest.from("profiles").update(
                        buildJsonObject {
                            put("role", "emprendedor")
                            put("campus", metaCampus)
                            put("business_name", metaStoreName ?: "Mi Tienda")
                            put("business_category", metaCategory)
                            put("business_description", metaDescription)
                            put("business_status", "PENDIENTE")
                            put("accepting_orders", false)
                            metaMeetingPoint?.let {
                                put("supported_meeting_points", buildJsonArray { add(it) })
                            }
                        }
                    ) {
                        filter { eq("id", user.id) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepositoryImpl", "Error en profiles update: ${e.message}")
                }
            }

            val profile = try {
                fetchProfile(user.id)
            } catch (_: Exception) {
                UserProfile(
                    id = user.id,
                    fullName = metaName,
                    phone = metaPhone,
                    campus = metaCampus,
                    role = if (effectiveIsSeller) UserRole.EMPRENDEDOR else UserRole.COMPRADOR,
                    businessName = metaStoreName,
                    businessCategory = metaCategory,
                    businessDescription = metaDescription,
                    businessStatus = if (effectiveIsSeller) "PENDIENTE" else "ABIERTO",
                    acceptingOrders = !effectiveIsSeller,
                    supportedMeetingPoints = metaMeetingPoint?.let { listOf(it) } ?: emptyList()
                )
            }

            // Cerramos sesión explícitamente para todos (vendedor y comprador)
            // para que no haya auto-login y ambos deban pasar por el Login manual
            try {
                auth.signOut()
            } catch (_: Exception) {}
            _currentProfile.value = null
            _isAuthenticated.value = false
            Result.success(profile)
        } catch (e: Exception) {
            try {
                auth.signOut()
            } catch (_: Exception) {}
            _currentProfile.value = null
            _isAuthenticated.value = false
            Result.failure(e)
        } finally {
            kotlinx.coroutines.delay(100)
            isSuppressingSessionBroadcast.set(false)
            _currentProfile.value = null
            _isAuthenticated.value = false
        }
    }

    override suspend fun resendOtp(email: String): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        return try {
            auth.resendEmail(
                type = OtpType.Email.SIGNUP,
                email = trimmedEmail
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetOtp(email: String): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        return try {
            auth.resetPasswordForEmail(trimmedEmail)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPasswordWithOtp(
        email: String,
        token: String,
        newPassword: String
    ): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedToken = token.trim()
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))
        }
        return try {
            auth.verifyEmailOtp(
                type = OtpType.Email.RECOVERY,
                email = trimmedEmail,
                token = trimmedToken
            )
            auth.updateUser {
                this.password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            _currentProfile.value = null
            _isAuthenticated.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshProfile(): Result<UserProfile?> {
        val user = auth.currentUserOrNull()
            ?: return Result.success(null)

        return try {
            val profile = fetchProfile(user.id)
            _currentProfile.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val profile = fetchProfile(userId)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchProfile(userId: String): UserProfile {
        val profile = postgrest.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<UserProfile>()
        return SellerPaymentMethodsStorage.enrichProfile(profile)
    }
}

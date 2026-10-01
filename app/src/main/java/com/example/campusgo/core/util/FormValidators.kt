package com.example.campusgo.core.util

/**
 * Validaciones generales y reutilizables para formularios y edición de perfiles
 * aplicables a todos los roles (Comprador, Vendedor, Administrador).
 */
object FormValidators {

    /**
     * Valida el nombre completo:
     * - Obligatorio
     * - Mínimo 3 caracteres, máximo 60
     * - Solo letras, espacios, acentos y caracteres de nombre válidos
     */
    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return "El nombre completo es obligatorio"
        }
        if (trimmed.length < 3) {
            return "El nombre debe tener al menos 3 caracteres"
        }
        if (trimmed.length > 60) {
            return "El nombre no puede exceder 60 caracteres"
        }
        val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s.'-]+$")
        if (!nameRegex.matches(trimmed)) {
            return "Ingresa un nombre válido (solo letras y espacios)"
        }
        return null
    }

    /**
     * Valida número telefónico:
     * - Obligatorio
     * - Estándar peruano: 9 dígitos numéricos iniciando con 9
     */
    fun validatePhone(phone: String): String? {
        val cleaned = phone.trim().replace(" ", "").replace("-", "")
        if (cleaned.isBlank()) {
            return "El número de teléfono es obligatorio"
        }
        if (!cleaned.all { it.isDigit() }) {
            return "El teléfono solo debe contener números"
        }
        if (cleaned.length != 9 || !cleaned.startsWith("9")) {
            return "Debe tener 9 dígitos y empezar con 9 (ej. 987654321)"
        }
        return null
    }

    /**
     * Valida campus universitario:
     * - Obligatorio
     * - Mínimo 3 caracteres
     */
    fun validateCampus(campus: String): String? {
        val trimmed = campus.trim()
        if (trimmed.isBlank()) {
            return "El campus universitario es obligatorio"
        }
        if (trimmed.length < 3) {
            return "El nombre del campus debe tener al menos 3 caracteres"
        }
        return null
    }

    /**
     * Valida nombre del puesto / emprendimiento:
     * - Obligatorio
     * - Entre 3 y 40 caracteres
     */
    fun validateStoreName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return "El nombre del puesto es obligatorio"
        }
        if (trimmed.length < 3) {
            return "El nombre debe tener al menos 3 caracteres"
        }
        if (trimmed.length > 40) {
            return "El nombre no puede exceder 40 caracteres"
        }
        return null
    }

    /**
     * Valida categoría comercial del puesto:
     * - Obligatorio
     */
    fun validateStoreCategory(category: String?): String? {
        if (category.isNullOrBlank()) {
            return "Debes seleccionar una categoría"
        }
        return null
    }

    /**
     * Valida descripción del puesto:
     * - Obligatorio
     * - Entre 10 y 300 caracteres
     */
    fun validateStoreDescription(description: String?): String? {
        val trimmed = description?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return "La descripción del puesto es obligatoria"
        }
        if (trimmed.length < 10) {
            return "Describe qué productos ofreces (mínimo 10 caracteres)"
        }
        if (trimmed.length > 300) {
            return "La descripción no puede exceder 300 caracteres"
        }
        return null
    }

    /**
     * Valida ubicación física del puesto:
     * - Obligatorio
     */
    fun validateStoreLocation(location: String?): String? {
        val trimmed = location?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return "La ubicación del puesto es obligatoria"
        }
        if (trimmed.length < 3) {
            return "Ingresa una ubicación válida dentro del campus"
        }
        return null
    }

    /**
     * Valida que al menos un punto de entrega esté seleccionado
     */
    fun validateMeetingPoints(points: Set<String>): String? {
        if (points.isEmpty()) {
            return "Debes seleccionar al menos un punto de entrega"
        }
        return null
    }

    /**
     * Valida que al menos un método de pago esté seleccionado
     */
    fun validatePaymentMethods(methods: Set<String>): String? {
        if (methods.isEmpty()) {
            return "Debes seleccionar al menos un método de pago"
        }
        return null
    }

    /**
     * Valida correo electrónico institucional o regular
     */
    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return "El correo electrónico es obligatorio"
        }
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(trimmed)) {
            return "Ingresa un formato de correo válido (ej. usuario@ucvvirtual.edu.pe)"
        }
        return null
    }

    /**
     * Valida contraseña
     */
    fun validatePassword(password: String): String? {
        if (password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres"
        }
        return null
    }
}

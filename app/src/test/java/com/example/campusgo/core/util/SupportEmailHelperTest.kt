package com.example.campusgo.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportEmailHelperTest {

    @Test
    fun testOfficialSupportEmailAddress() {
        assertEquals("soporte@kodexti.com", SupportEmailHelper.SUPPORT_EMAIL)
    }

    @Test
    fun testValidSupportEmailValidation() {
        assertTrue(SupportEmailHelper.isValidSupportEmail("soporte@kodexti.com"))
        assertTrue(SupportEmailHelper.isValidSupportEmail("SOPORTE@KODEXTI.COM"))
        assertTrue(SupportEmailHelper.isValidSupportEmail("  soporte@kodexti.com  "))
    }

    @Test
    fun testInvalidSupportEmailValidation() {
        assertFalse(SupportEmailHelper.isValidSupportEmail("otro@kodexti.com"))
        assertFalse(SupportEmailHelper.isValidSupportEmail("soporte@gmail.com"))
        assertFalse(SupportEmailHelper.isValidSupportEmail(""))
        assertFalse(SupportEmailHelper.isValidSupportEmail("admin@ucv.edu.pe"))
    }
}

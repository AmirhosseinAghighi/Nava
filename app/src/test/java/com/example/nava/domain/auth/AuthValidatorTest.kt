package com.example.nava.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test fun accepts_valid_email_and_password() {
        assertTrue(AuthValidator.isValid("listener@nava.app", "secret1"))
    }

    @Test fun rejects_invalid_credentials() {
        assertFalse(AuthValidator.isValid("listener", "short"))
    }
}

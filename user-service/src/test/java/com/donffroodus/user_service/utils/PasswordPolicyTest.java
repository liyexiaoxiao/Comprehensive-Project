package com.donffroodus.user_service.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordWithLettersAndDigits() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("abc12345"));
    }

    @Test
    void rejectsShortPassword() {
        RuntimeException error = assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("abc123"));
        assertEquals("密码至少 8 位，且需同时包含字母和数字", error.getMessage());
    }

    @Test
    void rejectsDigitsOnlyPassword() {
        assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("12345678"));
    }

    @Test
    void rejectsLettersOnlyPassword() {
        assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("abcdefgh"));
    }
}

package com.donffroodus.user_service.utils;

/**
 * 注册与改密时的密码强度校验。
 */
public final class PasswordPolicy {

    private static final String WEAK_PASSWORD_MESSAGE = "密码至少 8 位，且需同时包含字母和数字";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException(WEAK_PASSWORD_MESSAGE);
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) {
                hasLetter = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new RuntimeException(WEAK_PASSWORD_MESSAGE);
        }
    }
}

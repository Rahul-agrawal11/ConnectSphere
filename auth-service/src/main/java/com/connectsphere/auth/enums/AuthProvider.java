package com.connectsphere.auth.enums;

/**
 * Identifies how the user authenticated.
 * LOCAL = email/password, GOOGLE/GITHUB = OAuth2 social login.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    GITHUB
}
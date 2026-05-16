package com.connectsphere.auth.enums;

/**
 * Account lifecycle status.
 * SUSPENDED accounts cannot log in but data is retained.
 * DEACTIVATED is user-initiated soft delete.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
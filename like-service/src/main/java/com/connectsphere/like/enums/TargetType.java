package com.connectsphere.like.enums;

/**
 * Identifies what entity a reaction is applied to.
 *
 * POST    → reaction on a post     (calls post-service counter)
 * COMMENT → reaction on a comment  (calls comment-service counter)
 */
public enum TargetType {
    POST,
    COMMENT
}
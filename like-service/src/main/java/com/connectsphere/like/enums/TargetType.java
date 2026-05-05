package com.connectsphere.like.enums;

/**
 * Identifies what entity a reaction is applied to.
 *
 * POST    → reaction on a post     (calls post-service counter)
 * COMMENT → reaction on a comment  (calls comment-service counter)
 * STORY   → reaction on a story    (no counter propagation — stories track views, not likes)
 */
public enum TargetType {
    POST,
    COMMENT,
    STORY
}
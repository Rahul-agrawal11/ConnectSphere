package com.connectsphere.post.enums;

/**
 * Controls who can see a post.
 *
 * PUBLIC         — visible to everyone including guests
 * FOLLOWERS_ONLY — visible only to approved followers
 * PRIVATE        — visible only to the author
 */
public enum PostVisibility {
    PUBLIC,
    FOLLOWERS_ONLY,
    PRIVATE
}
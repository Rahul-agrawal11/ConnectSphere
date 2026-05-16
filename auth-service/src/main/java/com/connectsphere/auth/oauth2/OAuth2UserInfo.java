package com.connectsphere.auth.oauth2;

import java.util.Map;

/**
 * Abstract base class for OAuth2 provider user info extraction.
 * Each provider (Google, GitHub) has different attribute key names.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getEmail();
    public abstract String getImageUrl();
}
package com.connectsphere.auth.oauth2;

import java.util.Map;

/**
 * Extracts user info from GitHub OAuth2 token attributes.
 */
public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        if (name == null || name.toString().isBlank()) {
            name = attributes.get("login");
        }
        return name != null ? name.toString() : null;
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getImageUrl() {
        Object avatarUrl = attributes.get("avatar_url");
        return avatarUrl != null ? avatarUrl.toString() : null;
    }
}
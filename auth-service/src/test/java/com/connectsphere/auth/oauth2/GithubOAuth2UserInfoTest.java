package com.connectsphere.auth.oauth2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GithubOAuth2UserInfoTest {

    @Test
    void getId_shouldReturnIdAsString() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(
                Map.of("id", 98765, "login", "rahul", "email", "rahul@gmail.com"));

        assertThat(info.getId()).isEqualTo("98765");
    }

    @Test
    void getId_whenNull_shouldReturnNull() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of());

        assertThat(info.getId()).isNull();
    }

    @Test
    void getName_shouldReturnNameWhenPresent() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(
                Map.of("id", 1, "name", "Rahul Agrawal", "login", "rahul"));

        assertThat(info.getName()).isEqualTo("Rahul Agrawal");
    }

    @Test
    void getName_whenNameBlank_shouldFallBackToLogin() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", 1);
        attrs.put("name", "");
        attrs.put("login", "rahul_dev");

        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(attrs);

        assertThat(info.getName()).isEqualTo("rahul_dev");
    }

    @Test
    void getName_whenNameNull_shouldFallBackToLogin() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", 1);
        attrs.put("name", null);
        attrs.put("login", "rahul_dev");

        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(attrs);

        assertThat(info.getName()).isEqualTo("rahul_dev");
    }

    @Test
    void getEmail_shouldReturnEmail() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(
                Map.of("id", 1, "email", "rahul@gmail.com"));

        assertThat(info.getEmail()).isEqualTo("rahul@gmail.com");
    }

    @Test
    void getEmail_whenNull_shouldReturnNull() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of("id", 1));

        assertThat(info.getEmail()).isNull();
    }

    @Test
    void getImageUrl_shouldReturnAvatarUrl() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(
                Map.of("id", 1, "avatar_url", "http://avatars.github.com/u/1"));

        assertThat(info.getImageUrl()).isEqualTo("http://avatars.github.com/u/1");
    }

    @Test
    void getImageUrl_whenNull_shouldReturnNull() {
        GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of("id", 1));

        assertThat(info.getImageUrl()).isNull();
    }
}
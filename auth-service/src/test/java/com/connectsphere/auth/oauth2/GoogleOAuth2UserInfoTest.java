package com.connectsphere.auth.oauth2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuth2UserInfoTest {

    @Test
    void getId_shouldReturnSub() {
        GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(
                Map.of("sub", "google-123", "name", "Rahul",
                        "email", "rahul@gmail.com", "picture", "http://pic.url"));

        assertThat(info.getId()).isEqualTo("google-123");
    }

    @Test
    void getName_shouldReturnName() {
        GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(
                Map.of("sub", "google-123", "name", "Rahul Agrawal",
                        "email", "rahul@gmail.com", "picture", "http://pic.url"));

        assertThat(info.getName()).isEqualTo("Rahul Agrawal");
    }

    @Test
    void getEmail_shouldReturnEmail() {
        GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(
                Map.of("sub", "google-123", "name", "Rahul",
                        "email", "rahul@gmail.com", "picture", "http://pic.url"));

        assertThat(info.getEmail()).isEqualTo("rahul@gmail.com");
    }

    @Test
    void getImageUrl_shouldReturnPicture() {
        GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(
                Map.of("sub", "google-123", "name", "Rahul",
                        "email", "rahul@gmail.com", "picture", "http://pic.url"));

        assertThat(info.getImageUrl()).isEqualTo("http://pic.url");
    }

    @Test
    void getMissingFields_shouldReturnNull() {
        GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(Map.of());

        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getEmail()).isNull();
        assertThat(info.getImageUrl()).isNull();
    }
}
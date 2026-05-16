package com.connectsphere.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ConfigTest {

    private OAuth2Config oAuth2Config;

    @BeforeEach
    void setUp() {
        oAuth2Config = new OAuth2Config();
        ReflectionTestUtils.setField(oAuth2Config, "googleClientId", "google-client-id");
        ReflectionTestUtils.setField(oAuth2Config, "googleClientSecret", "google-client-secret");
        ReflectionTestUtils.setField(oAuth2Config, "googleRedirectUri",
                "http://localhost:8081/login/oauth2/code/google");
        ReflectionTestUtils.setField(oAuth2Config, "githubClientId", "github-client-id");
        ReflectionTestUtils.setField(oAuth2Config, "githubClientSecret", "github-client-secret");
        ReflectionTestUtils.setField(oAuth2Config, "githubRedirectUri",
                "http://localhost:8081/login/oauth2/code/github");
    }

    @Test
    void clientRegistrationRepository_shouldContainGoogleRegistration() {
        ClientRegistrationRepository repo = oAuth2Config.clientRegistrationRepository();

        ClientRegistration google = repo.findByRegistrationId("google");
        assertThat(google).isNotNull();
        assertThat(google.getClientId()).isEqualTo("google-client-id");
        assertThat(google.getClientName()).isEqualTo("Google");
        assertThat(google.getScopes()).contains("email", "profile", "openid");
    }

    @Test
    void clientRegistrationRepository_shouldContainGithubRegistration() {
        ClientRegistrationRepository repo = oAuth2Config.clientRegistrationRepository();

        ClientRegistration github = repo.findByRegistrationId("github");
        assertThat(github).isNotNull();
        assertThat(github.getClientId()).isEqualTo("github-client-id");
        assertThat(github.getClientName()).isEqualTo("GitHub");
        assertThat(github.getScopes()).contains("user:email", "read:user");
    }

    @Test
    void googleRegistration_shouldUseCorrectEndpoints() {
        ClientRegistrationRepository repo = oAuth2Config.clientRegistrationRepository();
        ClientRegistration google = repo.findByRegistrationId("google");

        assertThat(google.getProviderDetails().getAuthorizationUri())
                .contains("accounts.google.com");
        assertThat(google.getProviderDetails().getTokenUri())
                .contains("googleapis.com");
        assertThat(google.getRedirectUri())
                .isEqualTo("http://localhost:8081/login/oauth2/code/google");
    }

    @Test
    void githubRegistration_shouldUseCorrectEndpoints() {
        ClientRegistrationRepository repo = oAuth2Config.clientRegistrationRepository();
        ClientRegistration github = repo.findByRegistrationId("github");

        assertThat(github.getProviderDetails().getAuthorizationUri())
                .contains("github.com");
        assertThat(github.getProviderDetails().getTokenUri())
                .contains("github.com");
        assertThat(github.getRedirectUri())
                .isEqualTo("http://localhost:8081/login/oauth2/code/github");
    }
}
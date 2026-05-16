package com.connectsphere.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthenticationFailureHandlerTest {

    private OAuth2AuthenticationFailureHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "redirectUri",
                "http://localhost:3000/oauth2/redirect");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void onAuthenticationFailure_shouldRedirectToLoginWithErrorParam() throws Exception {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied"), "Access denied");

        handler.onAuthenticationFailure(request, response, ex);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).contains("/login");
        assertThat(redirectUrl).contains("error=oauth2");
    }

    @Test
    void onAuthenticationFailure_withLoginSuccessUri_shouldStripCorrectly() throws Exception {
        ReflectionTestUtils.setField(handler, "redirectUri",
                "http://localhost:3000/login-success");

        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(
                        new OAuth2Error("server_error"), "Server error"));

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).contains("error=oauth2");
        assertThat(redirectUrl).doesNotContain("login-success");
    }
}
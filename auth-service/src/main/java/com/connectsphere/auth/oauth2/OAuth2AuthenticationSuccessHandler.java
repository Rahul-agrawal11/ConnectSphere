package com.connectsphere.auth.oauth2;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * After successful OAuth2 login, generate a JWT and redirect the user
 * to the frontend with the token as a query parameter.
 *
 * The frontend (connectsphere-web) extracts the token and stores it
 * in session/cookie for subsequent API calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found after OAuth2 login: " + email));

        String token = jwtUtil.generateToken(user);

//        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
//                .queryParam("token", token)
//                .queryParam("userId", user.getId())
//                .build().toUriString();
//
//        log.info("OAuth2 success — redirecting user {} to frontend", user.getEmail());
//
//        getRedirectStrategy().sendRedirect(request, response, targetUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "OAuth2 login successful");
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("email", user.getEmail());
        result.put("provider", user.getProvider());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));

        log.info("OAuth2 success for user {}", user.getEmail());
    }
}
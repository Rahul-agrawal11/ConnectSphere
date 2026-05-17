package com.connectsphere.auth.oauth2;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .username(email.split("@")[0])
                                .fullName(name != null ? name : email.split("@")[0])
                                .profilePicUrl(picture)
                                .provider(AuthProvider.GOOGLE)
                                .providerId(providerId)
                                .role(Role.USER)
                                .status(AccountStatus.ACTIVE)
                                .build()
                ));

        String token = jwtUtil.generateToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("userId", user.getId())
                .build()
                .toUriString();

        log.info("OAuth2 success — redirecting user {} to frontend", user.getEmail());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

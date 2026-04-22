//package com.connectsphere.auth.oauth2;
//
//import com.connectsphere.auth.entity.User;
//import com.connectsphere.auth.enums.AccountStatus;
//import com.connectsphere.auth.enums.AuthProvider;
//import com.connectsphere.auth.enums.Role;
//import com.connectsphere.auth.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
///**
// * Custom OAuth2 user service.
// * On first OAuth2 login: creates a new user account.
// * On subsequent logins: updates profile info from the provider.
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CustomOAuth2UserService extends DefaultOAuth2UserService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    @Transactional
//    public OAuth2User loadUser(OAuth2UserRequest userRequest)
//            throws OAuth2AuthenticationException {
//
//        OAuth2User oAuth2User = super.loadUser(userRequest);
//        String registrationId = userRequest.getClientRegistration().getRegistrationId();
//
//        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User);
//
//        Optional<User> existingUser = userRepository
//                .findByProviderAndProviderId(
//                        AuthProvider.valueOf(registrationId.toUpperCase()),
//                        userInfo.getId());
//
//        User user;
//        user = existingUser.map(value -> updateExistingUser(value, userInfo)).orElseGet(() -> registerNewUser(registrationId, userInfo));
//
//        log.info("OAuth2 login successful for user: {} via {}",
//                user.getEmail(), registrationId);
//
//        return oAuth2User;
//    }
//
//    private OAuth2UserInfo resolveUserInfo(String registrationId,
//                                           OAuth2User oAuth2User) {
//        return switch (registrationId.toLowerCase()) {
//            case "google" -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
//            case "github" -> new GithubOAuth2UserInfo(oAuth2User.getAttributes());
//            default -> throw new OAuth2AuthenticationException(
//                    "Unsupported OAuth2 provider: " + registrationId);
//        };
//    }
//
//    private User registerNewUser(String registrationId, OAuth2UserInfo userInfo) {
//        // Generate a unique username from the provider's name
//        String baseUsername = userInfo.getName() != null
//                ? userInfo.getName().replaceAll("\\s+", "").toLowerCase()
//                : "user";
//        String username = ensureUniqueUsername(baseUsername);
//
//        User user = User.builder()
//                .username(username)
//                .email(userInfo.getEmail())
//                .fullName(userInfo.getName())
//                .profilePicUrl(userInfo.getImageUrl())
//                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
//                .providerId(userInfo.getId())
//                .role(Role.USER)
//                .status(AccountStatus.ACTIVE)
//                .build();
//
//        return userRepository.save(user);
//    }
//
//    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
//        user.setFullName(userInfo.getName());
//        user.setProfilePicUrl(userInfo.getImageUrl());
//        return userRepository.save(user);
//    }
//
//    private String ensureUniqueUsername(String base) {
//        String candidate = base;
//        int suffix = 1;
//        while (userRepository.existsByUsername(candidate)) {
//            candidate = base + suffix++;
//        }
//        return candidate;
//    }
//}

package com.connectsphere.auth.oauth2;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toLowerCase();

        OAuth2User effectiveOAuth2User = oAuth2User;

        // Only for GitHub: fetch email manually if missing
        if ("github".equals(registrationId)) {
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

            String email = attributes.get("email") != null ? attributes.get("email").toString() : null;

            if (email == null || email.isBlank()) {
                email = fetchGithubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
                attributes.put("email", email);
            }

            effectiveOAuth2User = new DefaultOAuth2User(
                    oAuth2User.getAuthorities(),
                    attributes,
                    "id"   // GitHub uses id
            );
        }

        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, effectiveOAuth2User);

        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not found from OAuth provider"
            );
        }

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(
                AuthProvider.valueOf(registrationId.toUpperCase()),
                userInfo.getId()
        );

        User user = existingUser
                .map(existing -> updateExistingUser(existing, userInfo))
                .orElseGet(() -> registerNewUser(registrationId, userInfo));

        log.info("OAuth2 login successful for user: {} via {}", user.getEmail(), registrationId);

        return effectiveOAuth2User;
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            case "github" -> new GithubOAuth2UserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "Unsupported OAuth2 provider: " + registrationId
            );
        };
    }

    private User registerNewUser(String registrationId, OAuth2UserInfo userInfo) {
        String baseUsername = userInfo.getName() != null && !userInfo.getName().isBlank()
                ? userInfo.getName().replaceAll("\\s+", "").toLowerCase()
                : "user";

        String username = ensureUniqueUsername(baseUsername);

        User user = User.builder()
                .username(username)
                .email(userInfo.getEmail())
                .fullName(userInfo.getName())
                .profilePicUrl(userInfo.getImageUrl())
                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(userInfo.getId())
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setFullName(userInfo.getName());
        user.setProfilePicUrl(userInfo.getImageUrl());

        if ((user.getEmail() == null || user.getEmail().isBlank())
                && userInfo.getEmail() != null
                && !userInfo.getEmail().isBlank()) {
            user.setEmail(userInfo.getEmail());
        }

        return userRepository.save(user);
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String fetchGithubPrimaryEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                List.class
        );

        List<Map<String, Object>> emails = response.getBody();

        if (emails != null) {
            for (Map<String, Object> emailObj : emails) {
                Boolean primary = (Boolean) emailObj.get("primary");
                Boolean verified = (Boolean) emailObj.get("verified");
                String email = (String) emailObj.get("email");

                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)
                        && email != null && !email.isBlank()) {
                    return email;
                }
            }

            for (Map<String, Object> emailObj : emails) {
                Boolean verified = (Boolean) emailObj.get("verified");
                String email = (String) emailObj.get("email");

                if (Boolean.TRUE.equals(verified) && email != null && !email.isBlank()) {
                    return email;
                }
            }
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "Email not found from GitHub"
        );
    }
}
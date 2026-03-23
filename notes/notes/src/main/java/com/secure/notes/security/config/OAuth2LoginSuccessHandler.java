package com.secure.notes.security.config;

import com.secure.notes.entity.AppRole;
import com.secure.notes.entity.Role;
import com.secure.notes.entity.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.security.jwt.JwtUtils;
import com.secure.notes.security.services.UserDetailsImpl;
import com.secure.notes.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private final UserService userService;

    @Autowired
    private final JwtUtils jwtUtils;

    @Autowired
    RoleRepository roleRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        // Declare all variables upfront — accessible throughout the method
        String username = "";
        String idAttributeKey = "id";
        String email = "";

        if ("github".equals(provider) || "google".equals(provider)) {
            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();

            // Step 1: Set username and idAttributeKey FIRST
            if ("github".equals(provider)) {
                username = attributes.getOrDefault("login", "").toString();
                idAttributeKey = "id";
            } else {
                // google — username will be derived from email below
                idAttributeKey = "sub";
            }

            // Step 2: Safely resolve email — handles explicit null from GitHub
            Object emailObj = attributes.get("email");
            email = (emailObj != null) ? emailObj.toString() : username + "@github.com";

            // Step 3: For Google, derive username from email
            if ("google".equals(provider)) {
                username = email.split("@")[0];
            }

            String name = attributes.getOrDefault("name", "").toString();
            System.out.println("HELLO OAUTH: " + email + " : " + name + " : " + username);

            // Need effectively final copies for lambda
            final String finalEmail = email;
            final String finalUsername = username;
            final String finalIdAttributeKey = idAttributeKey;

            userService.findByEmail(finalEmail)
                    .ifPresentOrElse(user -> {
                        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                                attributes,
                                finalIdAttributeKey
                        );
                        Authentication securityAuth = new OAuth2AuthenticationToken(
                                oauthUser,
                                List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                                provider
                        );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    }, () -> {
                        User newUser = new User();
                        Optional<Role> userRole = roleRepository.findByRoleName(AppRole.ROLE_USER);
                        if (userRole.isPresent()) {
                            newUser.setRole(userRole.get());
                        } else {
                            throw new RuntimeException("Default role not found");
                        }
                        newUser.setEmail(finalEmail);
                        newUser.setUserName(finalUsername);
                        newUser.setSignUpMethod(provider);
                        userService.registerUser(newUser);
                        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                attributes,
                                finalIdAttributeKey
                        );
                        Authentication securityAuth = new OAuth2AuthenticationToken(
                                oauthUser,
                                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                provider
                        );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    });
        }

        // JWT TOKEN LOGIC — uses email and username resolved above, no redeclaration
        DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
        System.out.println("OAuth2LoginSuccessHandler: " + username + " : " + email);

        Set<SimpleGrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toList()));

        User user = userService.findByEmail(email).orElseThrow( () -> new RuntimeException("User not found"));
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName().name()));

        UserDetailsImpl userDetails = new UserDetailsImpl(
                null,
                username,
                email,
                null,
                false,
                authorities
        );

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", jwtToken)
                .build().toUriString();

        this.setAlwaysUseDefaultTargetUrl(true);
        this.setDefaultTargetUrl(targetUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
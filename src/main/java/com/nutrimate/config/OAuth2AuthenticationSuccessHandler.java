package com.nutrimate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final String frontendUrl;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2AuthenticationSuccessHandler(
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.frontendUrl = frontendUrl;
        this.authorizedClientService = authorizedClientService;
        setDefaultTargetUrl(frontendUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) {
        String jwtToken = resolveJwtToken(authentication);
        if (jwtToken == null || jwtToken.isBlank()) {
            return frontendUrl;
        }
        String encoded = urlEncode(jwtToken);
        String base = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
        return base + "?token=" + encoded;
    }

    private String resolveJwtToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        String principalName = authentication.getName();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("cognito", principalName);
        if (client != null && client.getAccessToken() != null) {
            return client.getAccessToken().getTokenValue();
        }
        if (authentication.getPrincipal() instanceof OidcUser) {
            var idToken = ((OidcUser) authentication.getPrincipal()).getIdToken();
            if (idToken != null) return idToken.getTokenValue();
        }
        return null;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}

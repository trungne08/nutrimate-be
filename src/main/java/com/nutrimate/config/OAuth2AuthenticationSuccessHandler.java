package com.nutrimate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    public OAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
        setDefaultTargetUrl("http://localhost:5173");
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
        
        String targetUrl = "http://localhost:5173";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(targetUrl);
        
        // Lấy token từ authentication
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            
            // Lấy ID token
            if (oidcUser.getIdToken() != null) {
                String idToken = oidcUser.getIdToken().getTokenValue();
                builder.queryParam("token", idToken);
                builder.queryParam("token_type", "Bearer");
            }
            
            // Lấy access token từ OAuth2AuthorizedClientService
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                "cognito", 
                oidcUser.getName()
            );
            
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                builder.queryParam("access_token", accessToken);
            }
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            
            // Lấy access token
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                "cognito", 
                oauth2User.getName()
            );
            
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                builder.queryParam("access_token", accessToken);
                builder.queryParam("token_type", "Bearer");
            }
        }
        
        return builder.toUriString();
    }
}

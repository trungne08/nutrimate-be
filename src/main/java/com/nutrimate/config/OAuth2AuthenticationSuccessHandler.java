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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendUrl;
    
    public OAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
                                             @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.authorizedClientService = authorizedClientService;
        this.frontendUrl = frontendUrl;
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
        // Không gửi bất kỳ token nào qua URL nữa.
        // FE sau khi được redirect tới frontendUrl sẽ tự gọi API /api/auth/token
        // để lấy access_token nếu cần.
        return frontendUrl;
    }
}

package com.nutrimate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final String frontendUrl;
    
    public OAuth2AuthenticationSuccessHandler(@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
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

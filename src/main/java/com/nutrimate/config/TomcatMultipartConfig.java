package com.nutrimate.config;

import org.apache.catalina.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * Fix Tomcat multipart parsing limits (Spring Boot 4 / Tomcat 10.1+).
 *
 * Lỗi thường gặp trên Railway:
 * - MultipartException: Failed to parse multipart servlet request
 * - FileCountLimitExceededException
 */
@Configuration
public class TomcatMultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartCustomizer(
            @Value("${app.upload.max-part-count:200}") int maxPartCount
    ) {
        return (TomcatServletWebServerFactory factory) ->
                factory.addContextCustomizers((Context context) -> {
                    // Tomcat version khác nhau có/không có setter này.
                    // Dùng reflection để tránh lỗi compile-time và vẫn set được khi runtime support.
                    try {
                        Method m = context.getClass().getMethod("setMaxPartCount", int.class);
                        m.invoke(context, maxPartCount);
                    } catch (NoSuchMethodException ignored) {
                        // Tomcat không support maxPartCount → bỏ qua
                    } catch (Exception e) {
                        // Không fail startup vì config upload
                        e.printStackTrace();
                    }
                });
    }
}


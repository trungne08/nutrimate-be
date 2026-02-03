package com.nutrimate.config;

import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;

/**
 * Thêm ROLE_xxx từ DB vào authorities của Jwt để hasRole() hoạt động
 * với Bearer token từ Cognito (vì token không có scope/role EXPERT).
 */
@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 1. Lấy authorities mặc định từ scope (SCOPE_xxx)
        Collection<GrantedAuthority> authorities =
                new HashSet<>(defaultGrantedAuthoritiesConverter.convert(jwt));

        // 2. Tra DB để gán thêm ROLE_xxx
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("cognito:username");
        if (email == null) email = jwt.getClaimAsString("preferred_username");

        if (email != null) {
            userRepository.findByEmail(email).ifPresent(user -> addRoleAuthority(authorities, user));
        }

        // Principal name: sub (cognito id) hoặc email đều được, không quá quan trọng ở đây
        String principalName = jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private void addRoleAuthority(Collection<GrantedAuthority> authorities, User user) {
        if (user.getRole() == null) {
            return;
        }
        String roleName = "ROLE_" + user.getRole().name(); // ví dụ ROLE_EXPERT
        authorities.add(new SimpleGrantedAuthority(roleName));
    }
}


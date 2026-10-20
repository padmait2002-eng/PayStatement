package com.paystatement.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@Profile("!local")
@EnableMethodSecurity
public class JwtSecurityConfig {
    @Bean
    SecurityFilterChain jwtSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())))
                .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> {
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
            Collection<?> scopeAuthorities = scopes.convert(jwt);
            if (scopeAuthorities != null) {
                scopeAuthorities.forEach(authority -> authorities.add(new SimpleGrantedAuthority(authority.toString())));
            }
            Object claim = jwt.getClaims().get("roles");
            if (claim instanceof Collection<?> roles) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString())));
            }
            String subject = jwt.getClaimAsString("employeeId");
            if (subject == null || subject.isBlank()) subject = jwt.getClaimAsString("preferred_username");
            if (subject == null || subject.isBlank()) subject = jwt.getSubject();
            return new JwtAuthenticationToken(jwt, authorities, subject);
        };
    }
}

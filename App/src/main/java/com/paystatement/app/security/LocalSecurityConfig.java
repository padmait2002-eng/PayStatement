package com.paystatement.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Configuration
@Profile("local")
@EnableMethodSecurity
public class LocalSecurityConfig {
    private static final Set<String> ROLES = Set.of("EMPLOYEE", "ADMIN", "PAYMENT_SPECIALIST", "SYSTEM");

    @Bean
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new LocalIdentityFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
                .build();
    }

    private static class LocalIdentityFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String employeeId = request.getHeader("X-Employee-Id");
                String role = request.getHeader("X-User-Role");
                if (employeeId == null || employeeId.isBlank()) employeeId = "local-employee";
                if (role == null || role.isBlank()) role = "EMPLOYEE";
                if (!ROLES.contains(role)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid local role");
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(employeeId, "local",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);
        }
    }
}

package com.project2.BookStore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.http.HttpMethod;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                    log.info("Configuring CORS");
                    cors.configurationSource(corsConfigurationSource());
                })
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .xssProtection(xss -> {})
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'self'; form-action 'self'"))
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/api/bookStore/user/login?expired")
                )
                .authorizeHttpRequests(auth -> {
                    log.info("Configuring authorization rules");
                    auth
                            // Public APIs
                            .requestMatchers(
                                    "/api/bookStore/user/login",
                                    "/api/bookStore/user/register",
                                    "/api/bookStore/book/simple",
                                    "/api/bookStore/book/paged",
                                    "/api/bookStore/category/**"
                            ).permitAll()
                            // Chỉ ADMIN mới được truy cập
                            .requestMatchers(
                                    "/api/bookStore/book/add",
                                    "/api/bookStore/book/update",
                                    "/api/bookStore/book/delete/*",
                                    "/api/bookStore/book/image",
                                    "/api/bookStore/user",
                                    "/api/bookStore/user/paged",
                                    "/api/bookStore/user/delete/*",
                                    "/api/bookStore/statistics/*",
                                    "/api/orders/*/status",
                                    "/api/order-items/*"
                            ).hasRole("ADMIN")
                            // APIs yêu cầu xác thực (không cần role ADMIN)
                            .requestMatchers(
                                    "/api/bookStore/user/account",
                                    "/api/bookStore/user/logout",
                                    "/api/bookStore/user/update",
                                    "/api/bookStore/user/avatar/upload",
                                    "/api/bookStore/user/upload-avatar",
                                    "/api/bookStore/cart/**",
                                    "/api/orders",
                                    "/api/orders/*",
                                    "/api/orders/user",
                                    "/api/orders/*/cancel"
                            ).authenticated()
                            // Các API còn lại yêu cầu xác thực
                            .anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> {
                    log.info("Configuring OAuth2 resource server");
                    oauth2.jwt(jwt -> {
                        log.info("Configuring JWT");
                        jwt.decoder(jwtDecoder())
                           .jwtAuthenticationConverter(jwtAuthenticationConverter());
                    });
                })
                .exceptionHandling(exception -> {
                    log.info("Configuring exception handling");
                    exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                log.warn("Authentication failed: {}", authException.getMessage());
                                log.debug("Request URI: {}", request.getRequestURI());
                                log.debug("Request method: {}", request.getMethod());
                                log.debug("Authorization header: {}", request.getHeader("Authorization"));
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(401);
                                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                            })
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                log.warn("Access denied: {}", accessDeniedException.getMessage());
                                log.debug("Request URI: {}", request.getRequestURI());
                                log.debug("Request method: {}", request.getMethod());
                                log.debug("Authorization header: {}", request.getHeader("Authorization"));
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(403);
                                response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Access denied\"}");
                            });
                });

        log.info("Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.info("Configuring JWT authentication converter");
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            log.info("Processing JWT token for role mapping");
            log.info("JWT claims: {}", jwt.getClaims());
            log.info("Role from token: {}", jwt.getClaimAsString("role"));
            
            var authorities = grantedAuthoritiesConverter.convert(jwt);
            log.info("Converted authorities: {}", authorities);
            
            return authorities;
        });
        
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with secret key");
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS configuration");
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:5173");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
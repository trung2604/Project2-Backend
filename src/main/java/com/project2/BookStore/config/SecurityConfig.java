package com.project2.BookStore.config;

import com.project2.BookStore.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
import jakarta.servlet.MultipartConfigElement;
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

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        return factory.createMultipartConfig();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
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
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
                            "/api/bookStore/book/{id}",
                            "/api/bookStore/category",
                            "/api/bookStore/category/{id}",
                            "/api/bookStore/category/paged"
                        ).permitAll()
                        
                        // VNPay IPN callback (public endpoint)
                        .requestMatchers("/api/bookStore/payments/ipn").permitAll()
                        
                        // Public Review APIs (không cần authentication) - Sử dụng antMatchers
                        .requestMatchers(HttpMethod.GET, 
                            "/api/bookStore/reviews/book/*/rating",
                            "/api/bookStore/reviews/book/*/summary",
                            "/api/bookStore/reviews/book/*",
                            "/api/bookStore/reviews/*"
                        ).permitAll()
                        
                        // Admin APIs
                        .requestMatchers(
                            "/api/bookStore/book/add",
                            "/api/bookStore/book/update",
                            "/api/bookStore/book/delete/*",
                            "/api/bookStore/book/image",
                            "/api/bookStore/category/add",
                            "/api/bookStore/category/update/*",
                            "/api/bookStore/category/delete/*",
                            "/api/bookStore/user",
                            "/api/bookStore/user/create",
                            "/api/bookStore/user/paged",
                            "/api/bookStore/user/delete/*",
                            "/api/bookStore/statistics/*",
                            "/api/bookStore/orders/*/status",
                            "/api/bookStore/order-items/*",
                            "/api/bookStore/reports/**",
                            "/api/bookStore/orders/*/shipping",
                            "/api/bookStore/orders/*/delivered",
                            "/api/bookStore/orders/*/confirm",
                            "/api/bookStore/reviews/admin",
                            "/api/bookStore/reviews/admin/*/status"
                        ).hasRole("ADMIN")
                        
                        // APIs requiring authentication (no ADMIN role needed)
                        .requestMatchers(
                            "/api/bookStore/user/account",
                            "/api/bookStore/user/logout",
                            "/api/bookStore/user/update",
                            "/api/bookStore/user/avatar/upload",
                            "/api/bookStore/user/upload-avatar",
                            "/api/bookStore/cart/**",
                            "/api/bookStore/orders",
                            "/api/bookStore/orders/*",
                            "/api/bookStore/orders/user",
                            "/api/bookStore/orders/*/cancel",
                            "/api/bookStore/reviews",
                            "/api/bookStore/reviews/user",
                            "/api/bookStore/payments/vnpay",
                            "/api/bookStore/payments/status/*"
                        ).authenticated()
                        
                        // Review CRUD operations requiring authentication
                        .requestMatchers(HttpMethod.PUT, "/api/bookStore/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/bookStore/reviews/*").authenticated()
                        
                        // All other APIs require authentication
                        .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> {
                    log.info("Configuring exception handling");
                    exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                log.warn("Authentication failed for {}: {}", request.getRequestURI(), authException.getMessage());
                                log.debug("Request URI: {}", request.getRequestURI());
                                log.debug("Request method: {}", request.getMethod());
                                log.debug("Authorization header: {}", request.getHeader("Authorization"));
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(401);
                                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                            })
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                log.warn("Access denied for {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
                                log.debug("Request URI: {}", request.getRequestURI());
                                log.debug("Request method: {}", request.getMethod());
                                log.debug("Authorization header: {}", request.getHeader("Authorization"));
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(403);
                                response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Access denied\"}");
                            });
                });
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.info("Configuring JWT authentication converter");
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter::convert);
        
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
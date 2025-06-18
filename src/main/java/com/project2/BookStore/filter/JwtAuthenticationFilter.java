package com.project2.BookStore.filter;

import com.project2.BookStore.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("Processing request: {} {}", method, path);

        try {
            String authHeader = request.getHeader("Authorization");
            log.debug("Authorization header: {}", authHeader != null ? "Bearer ***" : "null");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No valid Authorization header found for {} {}", method, path);
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            log.debug("Processing token for {} {}", method, path);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                log.debug("Token is valid for user: {} with role: {}", email, role);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set for user: {} with authorities: {}", 
                        email, userDetails.getAuthorities());
                }
            } else {
                log.warn("Invalid token for {} {}", method, path);
            }
        } catch (Exception e) {
            log.error("Error processing authentication for {} {}: {}", 
                method, path, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Public endpoints that don't need authentication
        boolean isPublicEndpoint = 
            // User endpoints
            path.equals("/api/bookStore/user/login") ||
            path.equals("/api/bookStore/user/register") ||
            // Book endpoints
            path.equals("/api/bookStore/book/simple") ||
            path.equals("/api/bookStore/book/paged") ||
            path.matches("/api/bookStore/book/\\d+") ||
            // Category endpoints
            path.equals("/api/bookStore/category") ||
            path.matches("/api/bookStore/category/\\d+") ||
            path.equals("/api/bookStore/category/paged");

        if (isPublicEndpoint) {
            log.debug("Skipping authentication for public endpoint: {} {}", method, path);
            return true;
        }

        log.debug("Authentication required for: {} {}", method, path);
        return false;
    }
} 
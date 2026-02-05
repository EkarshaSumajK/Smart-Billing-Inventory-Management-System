package com.smartretail.backend.security;

import com.smartretail.backend.models.User;
import com.smartretail.backend.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String email = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(jwt);
                email = claims.getSubject();
                logger.debug("[JWT] Extracted email from token: {}", email);
            } catch (Exception e) {
                logger.error("[JWT] Token validation failed: {}", e.getMessage());
            }
        } else {
            logger.debug("[JWT] No Bearer token found in Authorization header");
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> optionalUser = Optional.ofNullable(authService.getUserByEmail(email));
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                // FIX: Use getRole() instead of getRoles()
                String role = user.getRole();
                String springRole = "ROLE_" + role;

                var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(springRole)
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user, // Pass the full User object as principal
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("[JWT] Authentication set for user: {} with role: {}", email, springRole);
            } else {
                logger.warn("[JWT] User not found for email: {}", email);
            }
        }

        filterChain.doFilter(request, response);
    }
}
package com.boundless.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:YOUR_JWT_SECRET_KEY_MUST_BE_AT_LEAST_256_BITS_LONG_CHANGE_ME!}")
    private String jwtSecret;

    // Endpoints that DON'T require a JWT token
    private final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/public/",
            "/api/network/swipe",
            "/api/companies/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow open endpoints through without any token
        if (OPEN_ENDPOINTS.stream().anyMatch(path::contains)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the Bearer token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            String token = authHeader.substring(7);

            // Decode and validate the JWT using the SAME secret as card-identity-service
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            String userId = claims.get("userId", String.class);

            // Inject trusted headers so downstream services know WHO is calling
            HttpServletRequestWrapper mutatedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if (name.equalsIgnoreCase("X-Trusted-User-Email")) return email;
                    if (name.equalsIgnoreCase("X-Trusted-User-Role")) return role;
                    if (name.equalsIgnoreCase("X-Trusted-User-Id")) return userId;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (name.equalsIgnoreCase("X-Trusted-User-Email")) return Collections.enumeration(Collections.singletonList(email));
                    if (name.equalsIgnoreCase("X-Trusted-User-Role")) return Collections.enumeration(Collections.singletonList(role));
                    if (name.equalsIgnoreCase("X-Trusted-User-Id")) return Collections.enumeration(Collections.singletonList(userId));
                    return super.getHeaders(name);
                }
            };

            filterChain.doFilter(mutatedRequest, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or expired JWT token\"}");
        }
    }
}

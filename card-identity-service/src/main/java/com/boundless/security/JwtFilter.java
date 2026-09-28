package com.boundless.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            final String authHeader = request.getHeader("Authorization");

            // 1. If there's no Bearer token, let it pass to be rejected by Spring Security
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Extract the token
            final String jwt = authHeader.substring(7);
            
            // 3. Cryptographically verify it
            if (jwtUtils.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                Claims claims = jwtUtils.extractAllClaims(jwt);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                
                // 4. Inject the Role into the Security Context!
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // If anything goes wrong with the JWT, we silently fail and let Spring Security handle the 403 
            // if the endpoint was secure, or allow it if it was permitAll().
        }
        
        filterChain.doFilter(request, response);
    }
}

package com.example.drivebackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String secret;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(String secret, UserDetailsService userDetailsService) {
        this.secret = secret;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        // We use Spring's WebUtils to find the cookie by name easily
        Cookie cookie = WebUtils.getCookie(request, "accessToken");

        if (cookie != null) {
            token = cookie.getValue();
        }

        // If no token found in cookie, skip validation and continue chain
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 2. Create Key from Secret
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            // 3. Validate Token & Parse Claims
            Claims claims = Jwts.parser()
                    .verifyWith(key) // verify signature with secret
                    .build()
                    .parseSignedClaims(token) // parse the JWT
                    .getPayload(); // extract the body (claims)

            String username = claims.getSubject();

            // 4. If valid and not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details from memory
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Set Authentication in Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            // Token is invalid, expired, or tampered with.
            // We clear context just to be safe.
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}

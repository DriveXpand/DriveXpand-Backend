package com.example.drivebackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders; // Import for headers
import org.springframework.http.ResponseCookie; // Import for cookie building
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final String secret;
    private final boolean isProduction;
    private static final long EXPIRATION_MS = 86400000; // 1 day

    public AuthController(AuthenticationManager authenticationManager,
                          @Value("${app.auth.secret}") String secret,
                          @Value("${app.is-production:true}") boolean isProduction) {
        this.authenticationManager = authenticationManager;
        this.secret = secret;
        this.isProduction = isProduction;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 1. Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            // 2. Generate Token
            String token = generateToken(authentication.getName());

            // 3. Create the HttpOnly Cookie
            ResponseCookie jwtCookie = ResponseCookie.from("accessToken", token)
                    .httpOnly(true)                // Crucial: JS cannot read this
                    .secure(isProduction)          // True in Prod (HTTPS), False in Dev (HTTP)
                    .path("/")                     // Available for the whole app
                    .maxAge(EXPIRATION_MS / 1000)  // Expiration in seconds
                    .sameSite("Lax")               // allows navigation from external links
                    .build();

            // 4. Return Response with Set-Cookie Header
            // We return the user info in the body so the frontend can update UI immediately
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(new UserInfo(authentication.getName(), "User logged in successfully"));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    // New Endpoint: frontend calls this on page load to see if the user is logged in
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
             return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserInfo(authentication.getName(), "Active Session"));
    }

    // New Endpoint: Clears the cookie
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out");
    }

    private String generateToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    // Records
    public record LoginRequest(String username, String password) {}
    public record UserInfo(String username, String message) {} 
}

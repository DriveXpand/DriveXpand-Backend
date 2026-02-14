package com.example.drivebackend.controller;

import org.springframework.beans.factory.annotation.Value;
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

    // Constructor Injection
    public AuthController(AuthenticationManager authenticationManager,
            @Value("${app.auth.secret}") String secret) {
        this.authenticationManager = authenticationManager;
        this.secret = secret;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            // 1. Authenticate the user (checks user/pass against SecurityConfig)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            // 2. If successful, generate a JWT using your secret
            String token = generateToken(authentication.getName());

            return ResponseEntity.ok(new AuthResponse(token));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    private String generateToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    // DTOs (Data Transfer Objects) using Java Records
    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(String token) {
    }
}

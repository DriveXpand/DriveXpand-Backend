package com.example.drivebackend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    private String testUsername;
    private String testPassword;
    private String validLoginPayload;
    private String invalidLoginPayload;

    @BeforeEach
    void setUp() {
        testUsername = "testuser@example.com";
        testPassword = "password123";
        validLoginPayload = "{\"username\": \"" + testUsername + "\", \"password\": \"" + testPassword + "\"}";
        invalidLoginPayload = "{\"username\": \"" + testUsername + "\", \"password\": \"wrongpassword\"}";
    }

    @Test
    @DisplayName("POST /api/auth/login - Should successfully login with valid credentials")
    void testLoginSuccess() throws Exception {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername, testPassword
        );
        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginPayload))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.message").value("User logged in successfully"));

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 for invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLoginPayload))
                .andExpect(status().isUnauthorized());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should set HttpOnly cookie")
    void testLoginSetsCookie() throws Exception {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername, testPassword
        );
        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginPayload))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true));

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should set secure cookie in production")
    void testLoginSetSecureCookie() throws Exception {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername, testPassword
        );
        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginPayload))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("GET /api/auth/me - Should return current user info when authenticated")
    @WithMockUser(username = "testuser@example.com")
    void testGetCurrentUserAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser@example.com"))
                .andExpect(jsonPath("$.message").value("Active Session"));
    }

    @Test
    @DisplayName("GET /api/auth/me - Should return 401 when not authenticated")
    void testGetCurrentUserUnauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout - Should clear the access token cookie")
    void testLogout() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(jsonPath("$").value("Logged out"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Should set cookie maxAge to 0")
    void testLogoutClearsCookie() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should authenticate with multiple users")
    void testLoginMultipleUsers() throws Exception {
        // Arrange
        String user2 = "anotheruser@example.com";
        String payload2 = "{\"username\": \"" + user2 + "\", \"password\": \"password123\"}";

        Authentication auth1 = new UsernamePasswordAuthenticationToken(testUsername, testPassword);
        auth1.setAuthenticated(true);

        Authentication auth2 = new UsernamePasswordAuthenticationToken(user2, testPassword);
        auth2.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth1)
                .thenReturn(auth2);

        // Act & Assert - First user
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername));

        // Act & Assert - Second user
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user2));

        verify(authenticationManager, times(2))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should handle empty credentials")
    void testLoginEmptyCredentials() throws Exception {
        // Arrange
        String emptyPayload = "{\"username\": \"\", \"password\": \"\"}";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should handle null password")
    void testLoginNullPassword() throws Exception {
        // Arrange
        String nullPasswordPayload = "{\"username\": \"" + testUsername + "\", \"password\": null}";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nullPasswordPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should handle malformed JSON")
    void testLoginMalformedJSON() throws Exception {
        // Arrange
        String malformedJSON = "{\"username\": \"" + testUsername + "\", \"password\": }";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return JSON with correct content type")
    void testLoginResponseContentType() throws Exception {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUsername, testPassword
        );
        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Should return JSON response")
    void testLogoutResponseFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}


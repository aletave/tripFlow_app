package com.tripflow.user_auth_service.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tripflow.user_auth_service.dto.request.ChangePasswordRequest;
import com.tripflow.user_auth_service.dto.request.LoginRequest;
import com.tripflow.user_auth_service.dto.request.RefreshTokenRequest;
import com.tripflow.user_auth_service.dto.request.RegisterRequest;
import com.tripflow.user_auth_service.dto.request.UpdateProfileRequest;
import com.tripflow.user_auth_service.dto.request.UserLookupRequest;
import com.tripflow.user_auth_service.dto.response.AuthResponse;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(authService.getMe(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        authService.deleteMe(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<PublicUserResponse> getPublicUser(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getPublicUserById(id));
    }

    @PostMapping("/users/lookup")
    public ResponseEntity<List<PublicUserResponse>> lookupUsers(@Valid @RequestBody UserLookupRequest request) {
        return ResponseEntity.ok(authService.lookupByEmails(request.emails()));
    }
}

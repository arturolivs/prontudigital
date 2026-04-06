package com.prontudigital.auth_service.controller;

import com.prontudigital.auth_service.dto.*;
import com.prontudigital.auth_service.exception.InvalidTokenException;
import com.prontudigital.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            RefreshTokenResponseDTO response = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (InvalidTokenException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        String username = userDetails.getUsername();
        UserInfoDTO userInfo = authService.getUserInfo(username);
        return ResponseEntity.ok(userInfo);
    }
}
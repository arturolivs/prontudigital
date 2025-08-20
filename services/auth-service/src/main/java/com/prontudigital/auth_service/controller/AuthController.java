package com.prontudigital.auth_service.controller;

import com.prontudigital.auth_service.dto.LoginRequestDTO;
import com.prontudigital.auth_service.dto.RegisterRequestDTO;
import com.prontudigital.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid
            @RequestBody
            RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticate(
            @Valid
            @RequestBody
            LoginRequestDTO request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logout(
            HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("Logout successful");
    }
}
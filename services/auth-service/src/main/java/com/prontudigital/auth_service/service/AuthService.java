package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.dto.JwtResponseDTO;
import com.prontudigital.auth_service.dto.LoginRequestDTO;
import com.prontudigital.auth_service.dto.RegisterRequestDTO;
import com.prontudigital.auth_service.dto.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    UserResponseDTO register(RegisterRequestDTO request);
    JwtResponseDTO authenticate(LoginRequestDTO request);
    void logout(HttpServletRequest request);
}

package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.dto.JwtResponseDTO;
import com.prontudigital.auth_service.dto.LoginRequestDTO;
import com.prontudigital.auth_service.dto.RegisterRequestDTO;
import com.prontudigital.auth_service.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    UserDTO register(RegisterRequestDTO request);
    JwtResponseDTO authenticate(LoginRequestDTO request);
    void logout(HttpServletRequest request);
}

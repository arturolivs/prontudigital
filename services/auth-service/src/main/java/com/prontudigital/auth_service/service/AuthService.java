package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    UserResponseDTO register(RegisterRequestDTO request);
    JwtResponseDTO authenticate(LoginRequestDTO request);
    void logout(String refreshToken);
    RefreshTokenResponseDTO refreshToken(String refreshToken);
    UserInfoDTO getUserInfo(String username);
}

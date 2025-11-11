package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.entity.RefreshToken;
import com.prontudigital.auth_service.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    String generateRefreshToken(String username);
    Optional<RefreshToken> findByToken(String token);
    boolean isRefreshTokenValid(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserRefreshTokens(User user);
    String rotateRefreshToken(String oldToken);
}

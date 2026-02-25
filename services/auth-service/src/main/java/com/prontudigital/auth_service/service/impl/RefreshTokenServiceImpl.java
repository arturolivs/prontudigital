package com.prontudigital.auth_service.service.impl;

import com.prontudigital.auth_service.exception.InvalidTokenException;
import com.prontudigital.auth_service.entity.RefreshToken;
import com.prontudigital.auth_service.entity.User;
import com.prontudigital.auth_service.repository.RefreshTokenRepository;
import com.prontudigital.auth_service.repository.UserRepository;
import com.prontudigital.auth_service.security.JwtTokenProvider;
import com.prontudigital.auth_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private Long refreshTokenExpirationMs;

    public String generateRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        revokeAllUserRefreshTokens(user);

        String token = jwtTokenProvider.generateRefreshToken(username);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isRefreshTokenRevokedOrExpired(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(refreshToken -> refreshToken.isRevoked() ||
                        refreshToken.getExpiryDate().isBefore(Instant.now()))
                .isPresent();
    }

    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }

    public void revokeAllUserRefreshTokens(User user) {
        List<RefreshToken> validUserTokens = refreshTokenRepository.findAllByUser(user)
                .stream()
                .filter(token -> !token.isRevoked() && token.getExpiryDate().isAfter(Instant.now()))
                .collect(Collectors.toList());

        if (!validUserTokens.isEmpty()) {
            validUserTokens.forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(validUserTokens);
        }
    }

    public String rotateRefreshToken(String oldToken) {
        RefreshToken refreshToken = findByToken(oldToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token não encontrado"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token já revogado");
        }

        revokeRefreshToken(oldToken);
        return generateRefreshToken(refreshToken.getUser().getUsername());
    }
}
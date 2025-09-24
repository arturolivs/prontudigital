package com.prontudigital.auth_service.service.impl;

import com.prontudigital.auth_service.dto.*;
import com.prontudigital.auth_service.exception.InvalidTokenException;
import com.prontudigital.auth_service.security.JwtTokenProvider;
import com.prontudigital.auth_service.security.UserDetailsImpl;
import com.prontudigital.auth_service.service.AuthService;
import com.prontudigital.auth_service.service.RefreshTokenService;
import com.prontudigital.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    public UserResponseDTO register(RegisterRequestDTO request) {
        var userDTO = UserResponseDTO.builder()
                .email(request.email())
                .username(request.username())
                .fullName(request.fullName())
                .isActive(true)
                .build();

        return userService.create(userDTO, request.password());
    }

    public JwtResponseDTO authenticate(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.generateRefreshToken(userPrincipal.getUsername());

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(accessToken, refreshToken, userPrincipal.getUsername(), roles);
    }

    public RefreshTokenResponseDTO refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Refresh token inválido");
        }

        if (!refreshTokenService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException("Refresh token revogado");
        }

        String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "Bearer", 900L);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }
}
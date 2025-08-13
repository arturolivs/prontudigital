package com.prontudigital.auth_service.service.impl;

import com.prontudigital.auth_service.dto.JwtResponseDTO;
import com.prontudigital.auth_service.dto.LoginRequestDTO;
import com.prontudigital.auth_service.dto.RegisterRequestDTO;
import com.prontudigital.auth_service.dto.UserDTO;
import com.prontudigital.auth_service.repository.UserRepository;
import com.prontudigital.auth_service.security.JwtTokenProvider;
import com.prontudigital.auth_service.security.UserDetailsImpl;
import com.prontudigital.auth_service.service.AuthService;
import com.prontudigital.auth_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public JwtResponseDTO register(RegisterRequestDTO request) {

        var userDTO = UserDTO.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .fullName(request.getFullName())
                .isActive(true)
                .build();

        userService.create(userDTO, request.getPassword());

        return authenticate(new LoginRequestDTO(request.getEmail(), request.getPassword()));
    }

    public JwtResponseDTO authenticate(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(jwt, userDetails.getUsername(), roles);
    }

    public void logout(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);
        if (token != null) {
            tokenProvider.invalidateToken(token);
        }
        SecurityContextHolder.clearContext();
    }
}
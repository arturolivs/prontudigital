package com.prontudigital.backend.autenticacao.servicos.impl;


import com.prontudigital.backend.autenticacao.dto.*;
import com.prontudigital.backend.autenticacao.excecoes.TokenInvalidoException;
import com.prontudigital.backend.autenticacao.seguranca.JwtTokenProvider;
import com.prontudigital.backend.autenticacao.seguranca.UserDetailsImpl;
import com.prontudigital.backend.autenticacao.servicos.AutenticacaoService;
import com.prontudigital.backend.autenticacao.servicos.RefreshTokenService;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
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
public class AutenticacaoServiceImpl implements AutenticacaoService {

    private final UsuarioService usuarioService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    public UsuarioDTO registrar(RegistrarRequestDTO request) {
        UsuarioDTO dto = UsuarioDTO.builder()
                .email(request.email())
                .username(request.username())
                .nomeCompleto(request.nomeCompleto())
                .telefone(request.telefone())
                .ativo(true)
                .perfis(request.perfis())
                .build();

        return usuarioService.criar(dto, request.senha());
    }

    @Override
    public JwtResponseDTO autenticar(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.senha()));

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.gerarRefreshToken(principal.getUsername());

        List<String> perfis = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(accessToken, refreshToken, principal.getUsername(), perfis);
    }

    @Override
    public RefreshTokenResponseDTO renovarToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new TokenInvalidoException("Refresh token invalido");
        }
        if (refreshTokenService.estaRevogadoOuExpirado(refreshToken)) {
            throw new TokenInvalidoException("Refresh token revogado ou expirado");
        }

        String novoRefreshToken = refreshTokenService.rotacionarRefreshToken(refreshToken);

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String novoAccessToken = tokenProvider.generateAccessToken(authentication);

        return new RefreshTokenResponseDTO(novoAccessToken, novoRefreshToken, "Bearer", 900L);
    }

    @Override
    public void encerrarSessao(String refreshToken) {
        refreshTokenService.revogarRefreshToken(refreshToken);
    }
}
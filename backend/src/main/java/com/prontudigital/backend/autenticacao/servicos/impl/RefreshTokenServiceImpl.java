package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.entidades.RefreshToken;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.TokenInvalidoException;
import com.prontudigital.backend.autenticacao.repositorios.RefreshTokenRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.seguranca.JwtTokenProvider;
import com.prontudigital.backend.autenticacao.servicos.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private Long refreshTokenExpirationMs;

    @Override
    public String gerarRefreshToken(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + username));

        revogarTodosTokensDoUsuario(usuario);

        String token = jwtTokenProvider.generateRefreshToken(username);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .usuario(usuario)
                .expiraEm(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revogado(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    public Optional<RefreshToken> buscarPorToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public boolean estaRevogadoOuExpirado(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> rt.isRevogado() || rt.getExpiraEm().isBefore(Instant.now()))
                .isPresent();
    }

    @Override
    public void revogarRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevogado(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    public void revogarTodosTokensDoUsuario(Usuario usuario) {
        List<RefreshToken> tokensAtivos = refreshTokenRepository.findAllByUsuario(usuario)
                .stream()
                .filter(rt -> !rt.isRevogado() && rt.getExpiraEm().isAfter(Instant.now()))
                .collect(Collectors.toList());

        if (!tokensAtivos.isEmpty()) {
            tokensAtivos.forEach(rt -> rt.setRevogado(true));
            refreshTokenRepository.saveAll(tokensAtivos);
        }
    }

    @Override
    public String rotacionarRefreshToken(String tokenAntigo) {
        RefreshToken refreshToken = buscarPorToken(tokenAntigo)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token nao encontrado"));

        if (refreshToken.isRevogado()) {
            throw new TokenInvalidoException("Refresh token ja revogado");
        }

        revogarRefreshToken(tokenAntigo);
        return gerarRefreshToken(refreshToken.getUsuario().getUsername());
    }
}
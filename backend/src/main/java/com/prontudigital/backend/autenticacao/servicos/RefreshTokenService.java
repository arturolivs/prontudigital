package com.prontudigital.backend.autenticacao.servicos;

import com.prontudigital.backend.autenticacao.entidades.RefreshToken;
import com.prontudigital.backend.autenticacao.entidades.Usuario;

import java.util.Optional;

public interface RefreshTokenService {
    String gerarRefreshToken(String username);
    Optional<RefreshToken> buscarPorToken(String token);
    boolean estaRevogadoOuExpirado(String token);
    void revogarRefreshToken(String token);
    void revogarTodosTokensDoUsuario(Usuario usuario);
    String rotacionarRefreshToken(String tokenAntigo);
}

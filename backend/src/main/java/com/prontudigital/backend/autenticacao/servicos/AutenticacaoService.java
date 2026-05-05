package com.prontudigital.backend.autenticacao.servicos;

import com.prontudigital.backend.autenticacao.dto.*;

public interface AutenticacaoService {
    UsuarioDTO registrar(RegisterRequestDTO request);
    JwtResponseDTO autenticar(LoginRequestDTO request);
    RefreshTokenResponseDTO renovarToken(String refreshToken);
    void encerrarSessao(String refreshToken);
    UsuarioDTO getUsuarioAtual();

}

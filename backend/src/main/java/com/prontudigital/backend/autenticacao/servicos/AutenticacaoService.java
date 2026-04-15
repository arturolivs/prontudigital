package com.prontudigital.backend.autenticacao.servicos;

import com.prontudigital.backend.autenticacao.dto.JwtResponseDTO;
import com.prontudigital.backend.autenticacao.dto.LoginRequestDTO;
import com.prontudigital.backend.autenticacao.dto.RefreshTokenResponseDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;

public interface AutenticacaoService {
    JwtResponseDTO autenticar(LoginRequestDTO request);
    RefreshTokenResponseDTO renovarToken(String refreshToken);
    void encerrarSessao(String refreshToken);
    UsuarioDTO getInfoUsuario(String username);

}

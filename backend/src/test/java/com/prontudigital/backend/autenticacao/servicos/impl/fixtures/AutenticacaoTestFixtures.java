// fixtures/AutenticacaoTestFixtures.java
package com.prontudigital.backend.autenticacao.servicos.impl.fixtures;

import com.prontudigital.backend.autenticacao.dto.*;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.RefreshToken;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.entidades.UsuarioPerfil;
import com.prontudigital.backend.autenticacao.entidades.UsuarioPerfilId;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AutenticacaoTestFixtures {

    public static final Long   USUARIO_ID   = 1L;
    public static final UUID   USUARIO_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String USERNAME     = "joaosilva";
    public static final String EMAIL        = "joao@email.com";
    public static final String SENHA_RAW    = "senha12345";
    public static final String SENHA_HASH   = "$2a$10$hashfakeparatestes";
    public static final String NOME         = "Joao Silva";

    public static final String TOKEN_VALIDO    = "eyJhbGciOiJIUzI1NiIs.valido.token";
    public static final String TOKEN_REVOGADO  = "eyJhbGciOiJIUzI1NiIs.revogado.token";
    public static final String TOKEN_EXPIRADO  = "eyJhbGciOiJIUzI1NiIs.expirado.token";

    private AutenticacaoTestFixtures() {}

    public static UsuarioDTO usuarioDTO() {
        return UsuarioDTO.builder()
                .id(USUARIO_ID)
                .uuid(USUARIO_UUID)
                .username(USERNAME)
                .email(EMAIL)
                .nomeCompleto(NOME)
                .ativo(true)
                .perfis(Set.of("PACIENTE"))
                .build();
    }

    public static UsuarioDTO usuarioDTOSemAtivo() {
        return UsuarioDTO.builder()
                .username(USERNAME)
                .email(EMAIL)
                .nomeCompleto(NOME)
                .perfis(Set.of("PACIENTE"))
                .build();
    }

    public static Perfil perfilPaciente() {
        return Perfil.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .nome("PACIENTE")
                .descricao("Paciente do sistema")
                .build();
    }

    public static Usuario usuarioComPerfil() {
        Usuario u = Usuario.builder()
                .id(USUARIO_ID)
                .uuid(USUARIO_UUID)
                .username(USERNAME)
                .email(EMAIL)
                .nomeCompleto(NOME)
                .senhaHash(SENHA_HASH)
                .ativo(true)
                .usuarioPerfis(new HashSet<>())
                .build();
        Perfil perfil = perfilPaciente();
        UsuarioPerfil up = UsuarioPerfil.builder()
                .id(new UsuarioPerfilId(USUARIO_ID, perfil.getId()))
                .usuario(u)
                .perfil(perfil)
                .build();
        u.getUsuarioPerfis().add(up);
        return u;
    }

    public static RegistrarRequestDTO registrarRequest() {
        return new RegistrarRequestDTO(NOME, EMAIL, USERNAME, SENHA_RAW, null, Set.of("PACIENTE"));
    }

    public static LoginRequestDTO loginRequest() {
        return new LoginRequestDTO(USERNAME, SENHA_RAW);
    }

    public static RefreshToken refreshTokenAtivo() {
        return RefreshToken.builder()
                .id(1L)
                .token(TOKEN_VALIDO)
                .usuario(usuarioComPerfil())
                .expiraEm(Instant.now().plusSeconds(3600))
                .revogado(false)
                .build();
    }

    public static RefreshToken refreshTokenRevogado() {
        RefreshToken rt = refreshTokenAtivo();
        rt.setToken(TOKEN_REVOGADO);
        rt.setRevogado(true);
        return rt;
    }

    public static RefreshToken refreshTokenExpirado() {
        RefreshToken rt = refreshTokenAtivo();
        rt.setToken(TOKEN_EXPIRADO);
        rt.setExpiraEm(Instant.now().minusSeconds(3600));
        return rt;
    }
}
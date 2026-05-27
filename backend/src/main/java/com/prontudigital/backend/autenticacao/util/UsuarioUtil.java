package com.prontudigital.backend.autenticacao.util;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;

import java.util.stream.Collectors;

public class UsuarioUtil {
    public static UsuarioDTO converterUsuarioParaDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .uuid(usuario.getUuid())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nomeCompleto(usuario.getNomeCompleto())
                .ativo(usuario.getAtivo())
                .perfis(usuario.getPerfis().stream()
                        .map(Perfil::getNome)
                        .collect(Collectors.toSet()))
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }
}

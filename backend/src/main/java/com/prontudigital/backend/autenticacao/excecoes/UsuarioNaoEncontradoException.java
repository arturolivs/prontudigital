package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends ExcecaoBase {
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }
    public UsuarioNaoEncontradoException(UUID uuid) {
        super("Usuário não encontrado com UUID: " + uuid);
    }
}

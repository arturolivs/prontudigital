package com.prontudigital.backend.autenticacao.excecoes;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }
    public UsuarioNaoEncontradoException(UUID uuid) {
        super("Usuário não encontrado com UUID: " + uuid);
    }
}

package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends ExcecaoBase {
    public UsuarioNaoEncontradoException(Long id) {
        super(Mensagens.get("usuario.nao-encontrado.id", id));
    }
    public UsuarioNaoEncontradoException(UUID uuid) {
        super(Mensagens.get("usuario.nao-encontrado.uuid", uuid));
    }
    public UsuarioNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

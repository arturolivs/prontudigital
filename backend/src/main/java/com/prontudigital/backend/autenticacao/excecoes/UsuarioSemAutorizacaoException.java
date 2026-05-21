package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class UsuarioSemAutorizacaoException extends ExcecaoBase {
    public UsuarioSemAutorizacaoException(String message) {
        super(message);
    }
}

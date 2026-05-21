package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class TokenInvalidoException extends ExcecaoBase {
    public TokenInvalidoException(String message) {
        super(message);
    }
}

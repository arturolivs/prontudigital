package com.prontudigital.backend.autenticacao.excecoes;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String message) {
        super(message);
    }
}

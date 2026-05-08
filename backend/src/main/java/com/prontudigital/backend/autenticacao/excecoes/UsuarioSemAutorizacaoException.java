package com.prontudigital.backend.autenticacao.excecoes;

public class UsuarioSemAutorizacaoException extends RuntimeException {
    public UsuarioSemAutorizacaoException(String message) {
        super(message);
    }
}

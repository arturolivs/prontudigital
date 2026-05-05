package com.prontudigital.backend.agendamento.excecoes;

public class UsuarioSemAutorizacaoException extends RuntimeException {
    public UsuarioSemAutorizacaoException(String message) {
        super(message);
    }
}

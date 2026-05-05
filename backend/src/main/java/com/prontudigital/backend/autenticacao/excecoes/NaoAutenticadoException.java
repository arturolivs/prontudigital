package com.prontudigital.backend.autenticacao.excecoes;

public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String message) {
        super(message);
    }
}

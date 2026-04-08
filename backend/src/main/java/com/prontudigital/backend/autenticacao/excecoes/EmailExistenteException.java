package com.prontudigital.backend.autenticacao.excecoes;

public class EmailExistenteException extends RuntimeException {
    public EmailExistenteException(String email) {
        super("Email '"+ email + "' já está em uso.");
    }
}

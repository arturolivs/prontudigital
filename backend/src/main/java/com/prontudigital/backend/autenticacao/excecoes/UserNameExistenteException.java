package com.prontudigital.backend.autenticacao.excecoes;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserNameExistenteException extends RuntimeException {
    public UserNameExistenteException(String username) {
        super("Usuário '" + username + "' já existe.");
    }
}

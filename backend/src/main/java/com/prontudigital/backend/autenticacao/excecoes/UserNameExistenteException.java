package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserNameExistenteException extends ExcecaoBase {
    public UserNameExistenteException(String username) {
        super("Usuário '" + username + "' já existe.");
    }
}

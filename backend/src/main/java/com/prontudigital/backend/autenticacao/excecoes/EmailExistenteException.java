package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class EmailExistenteException extends ExcecaoBase {
    public EmailExistenteException(String email) {
        super("Email '"+ email + "' já está em uso.");
    }
}

package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class EmailExistenteException extends ExcecaoBase {
    public EmailExistenteException(String email) {
        super(Mensagens.get("usuario.email-existente", email));
    }
}

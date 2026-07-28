package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class SenhaAtualInvalidaException extends ExcecaoBase {
    public SenhaAtualInvalidaException() {
        super(Mensagens.get("auth.senha-atual-invalida"));
    }
}

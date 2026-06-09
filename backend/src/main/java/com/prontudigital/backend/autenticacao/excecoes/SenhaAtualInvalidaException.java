package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class SenhaAtualInvalidaException extends ExcecaoBase {
    public SenhaAtualInvalidaException() {
        super("Senha atual incorreta.");
    }
}

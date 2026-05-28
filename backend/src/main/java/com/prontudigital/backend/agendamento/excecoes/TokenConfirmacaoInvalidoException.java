package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class TokenConfirmacaoInvalidoException extends ExcecaoBase {
    public TokenConfirmacaoInvalidoException(String mensagem) {
        super(mensagem);
    }
}

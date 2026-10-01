package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoNaoEncontradoException extends ExcecaoBase {
    public AgendamentoNaoEncontradoException(String message) {
        super(message);
    }
}

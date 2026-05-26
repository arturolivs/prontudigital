package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoStatusInvalidoException extends ExcecaoBase {
    public AgendamentoStatusInvalidoException(String message) {
        super(message);
    }
}

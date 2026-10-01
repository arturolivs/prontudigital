package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoInvalidoException extends ExcecaoBase {
    public AgendamentoInvalidoException(String message) {
        super(message);
    }
}

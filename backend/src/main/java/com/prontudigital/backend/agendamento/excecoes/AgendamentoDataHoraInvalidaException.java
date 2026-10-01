package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoDataHoraInvalidaException extends ExcecaoBase {
    public AgendamentoDataHoraInvalidaException(String message) {
        super(message);
    }
}

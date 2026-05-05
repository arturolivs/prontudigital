package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoDataHoraInvalidaException extends RuntimeException {
    public AgendamentoDataHoraInvalidaException(String message) {
        super(message);
    }
}

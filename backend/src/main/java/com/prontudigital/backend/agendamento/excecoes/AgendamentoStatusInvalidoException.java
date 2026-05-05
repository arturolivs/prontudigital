package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoStatusInvalidoException extends RuntimeException {
    public AgendamentoStatusInvalidoException(String message) {
        super(message);
    }
}

package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoInvalidoException extends RuntimeException {
    public AgendamentoInvalidoException(String message) {
        super(message);
    }
}

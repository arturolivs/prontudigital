package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoCanceladoException extends RuntimeException {
    public AgendamentoCanceladoException(String message) {
        super(message);
    }
}

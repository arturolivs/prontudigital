package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoJaCanseladoException extends RuntimeException {
    public AgendamentoJaCanseladoException(String message) {
        super(message);
    }
}

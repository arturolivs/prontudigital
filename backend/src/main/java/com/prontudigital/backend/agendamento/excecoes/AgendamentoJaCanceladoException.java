package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoJaCanceladoException extends RuntimeException {
    public AgendamentoJaCanceladoException(String message) {
        super(message);
    }
}

package com.prontudigital.backend.agendamento.excecoes;

public class AgendamentoJaConcluidoException extends RuntimeException {
    public AgendamentoJaConcluidoException(String message) {
        super(message);
    }
}

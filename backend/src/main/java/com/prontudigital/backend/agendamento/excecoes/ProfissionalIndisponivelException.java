package com.prontudigital.backend.agendamento.excecoes;

public class ProfissionalIndisponivelException extends RuntimeException {
    public ProfissionalIndisponivelException(String message) {
        super(message);
    }
}

package com.prontudigital.backend.agendamento.excecoes;

public class PacienteIndisponivelException extends RuntimeException {
    public PacienteIndisponivelException(String message) {
        super(message);
    }
}

package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class PacienteIndisponivelException extends ExcecaoBase {
    public PacienteIndisponivelException(String message) {
        super(message);
    }
}

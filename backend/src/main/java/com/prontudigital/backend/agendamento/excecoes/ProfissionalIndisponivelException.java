package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class ProfissionalIndisponivelException extends ExcecaoBase {
    public ProfissionalIndisponivelException(String message) {
        super(message);
    }
}

package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class ProcedimentoEmUsoException extends ExcecaoBase {
    public ProcedimentoEmUsoException(String message) {
        super(message);
    }
}

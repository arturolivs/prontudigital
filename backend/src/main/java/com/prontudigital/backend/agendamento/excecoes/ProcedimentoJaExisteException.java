package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class ProcedimentoJaExisteException extends ExcecaoBase {
    public ProcedimentoJaExisteException(String message) {
        super(message);
    }
}

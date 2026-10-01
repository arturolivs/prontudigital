package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class ProcedimentoNaoEncontradoException extends ExcecaoBase {
    public ProcedimentoNaoEncontradoException(String message) {
        super(message);
    }
}

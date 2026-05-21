package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoJaConcluidoException extends ExcecaoBase {
    public AgendamentoJaConcluidoException(String message) {
        super(message);
    }
}

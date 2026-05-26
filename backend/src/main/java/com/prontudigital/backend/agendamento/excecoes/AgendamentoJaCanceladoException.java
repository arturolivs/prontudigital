package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AgendamentoJaCanceladoException extends ExcecaoBase {
    public AgendamentoJaCanceladoException(String message) {
        super(message);
    }
}

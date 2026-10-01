package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AvaliacaoNaoEncontradaException extends ExcecaoBase {
    public AvaliacaoNaoEncontradaException(String message) {
        super(message);
    }
}

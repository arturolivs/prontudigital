package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class TipoVisualizacaoInvalidoException extends ExcecaoBase {
    public TipoVisualizacaoInvalidoException(String message) {
        super(message);
    }
}

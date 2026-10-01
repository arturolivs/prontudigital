package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AnamneseNaoEncontradaException extends ExcecaoBase {
    public AnamneseNaoEncontradaException(String message) {
        super(message);
    }
}

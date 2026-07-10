package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class PrescricaoNaoEncontradaException extends ExcecaoBase {
    public PrescricaoNaoEncontradaException(String message) {
        super(message);
    }
}

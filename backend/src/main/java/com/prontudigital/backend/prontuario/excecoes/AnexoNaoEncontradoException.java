package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AnexoNaoEncontradoException extends ExcecaoBase {
    public AnexoNaoEncontradoException(String message) {
        super(message);
    }
}

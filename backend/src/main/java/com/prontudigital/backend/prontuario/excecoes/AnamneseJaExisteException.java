package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AnamneseJaExisteException extends ExcecaoBase {
    public AnamneseJaExisteException(String message) {
        super(message);
    }
}

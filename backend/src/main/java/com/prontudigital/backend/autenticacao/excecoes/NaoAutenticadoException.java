package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class NaoAutenticadoException extends ExcecaoBase {
    public NaoAutenticadoException(String message) {
        super(message);
    }
}

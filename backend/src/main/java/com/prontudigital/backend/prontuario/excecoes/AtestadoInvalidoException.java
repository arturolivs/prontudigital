package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AtestadoInvalidoException extends ExcecaoBase {
    public AtestadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}

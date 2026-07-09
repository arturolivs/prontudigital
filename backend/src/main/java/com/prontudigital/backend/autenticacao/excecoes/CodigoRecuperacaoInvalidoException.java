package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class CodigoRecuperacaoInvalidoException extends ExcecaoBase {
    public CodigoRecuperacaoInvalidoException(String mensagem) {
        super(mensagem);
    }
}

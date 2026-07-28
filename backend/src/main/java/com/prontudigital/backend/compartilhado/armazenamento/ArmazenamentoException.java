package com.prontudigital.backend.compartilhado.armazenamento;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/** Falha de infraestrutura ao gravar, ler ou remover um arquivo. */
public class ArmazenamentoException extends ExcecaoBase {
    public ArmazenamentoException(String mensagem) {
        super(mensagem);
    }
}

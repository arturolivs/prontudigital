package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

public class AcessoNaoAtivadoException extends ExcecaoBase {
    public AcessoNaoAtivadoException() {
        super("Acesso não ativado. Configure seu e-mail e senha para entrar no sistema.");
    }
}

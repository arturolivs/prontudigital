package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class AcessoNaoAtivadoException extends ExcecaoBase {
    public AcessoNaoAtivadoException() {
        super(Mensagens.get("auth.acesso-nao-ativado"));
    }
}

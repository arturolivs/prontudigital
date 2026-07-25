package com.prontudigital.backend.prontuario.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class AtestadoNaoEncontradoException extends ExcecaoBase {
    public AtestadoNaoEncontradoException() {
        super(Mensagens.get("atestado.nao-encontrado"));
    }
}

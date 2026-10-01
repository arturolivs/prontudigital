package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class CpfExistenteException extends ExcecaoBase {
    public CpfExistenteException(String cpf) {
        super(Mensagens.get("usuario.cpf-existente", cpf));
    }
}

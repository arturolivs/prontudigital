package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;

public class CorenExistenteException extends ExcecaoBase {
    public CorenExistenteException(String coren) {
        super(Mensagens.get("usuario.coren-existente", coren));
    }
}

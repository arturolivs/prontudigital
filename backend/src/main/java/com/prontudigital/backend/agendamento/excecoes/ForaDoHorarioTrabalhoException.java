package com.prontudigital.backend.agendamento.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/** RF05 — agendamento fora do expediente cadastrado do profissional. */
public class ForaDoHorarioTrabalhoException extends ExcecaoBase {
    public ForaDoHorarioTrabalhoException(String mensagem) {
        super(mensagem);
    }
}

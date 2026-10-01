package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/**
 * Upload de avatar recusado: arquivo vazio, tipo nao suportado, acima do
 * limite de tamanho, ou download pedido de um usuario que nao tem avatar.
 */
public class AvatarInvalidoException extends ExcecaoBase {
    public AvatarInvalidoException(String mensagem) {
        super(mensagem);
    }
}

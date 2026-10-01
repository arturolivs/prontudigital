package com.prontudigital.backend.autenticacao.excecoes;

import com.prontudigital.backend.compartilhado.excecoes.ExcecaoBase;

/**
 * Lancada quando a recuperacao de senha e solicitada com o canal de envio
 * desligado ({@code app.notificacoes.habilitadas=false}).
 *
 * <p>O codigo de recuperacao so trafega por WhatsApp — nao ha fallback por
 * e-mail. Sem o canal, gerar o codigo seria inutil: ninguem o receberia. Em vez
 * de estourar um 500 generico a cada tentativa, a API responde 503 com uma
 * mensagem que orienta o usuario a procurar o administrador.
 */
public class RecuperacaoSenhaIndisponivelException extends ExcecaoBase {
    public RecuperacaoSenhaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}

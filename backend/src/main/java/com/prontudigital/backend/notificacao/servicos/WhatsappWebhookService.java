package com.prontudigital.backend.notificacao.servicos;

/**
 * Recebe os callbacks do WhatsApp Cloud API.
 *
 * <p>O envio responde HTTP 200 assim que a Meta aceita a mensagem; a entrega
 * real (ou a falha, como o 131047 de janela de 24h fechada) só chega depois,
 * por aqui.
 */
public interface WhatsappWebhookService {

    /** Handshake de verificação da URL, feito uma vez pelo painel da Meta. */
    boolean verificacaoValida(String modo, String token);

    /**
     * Confere o {@code X-Hub-Signature-256} contra o corpo bruto da requisição.
     * Sem o app secret configurado a verificação é pulada (dev).
     */
    boolean assinaturaValida(String corpoBruto, String assinatura);

    /** Aplica os status de entrega recebidos ao log de notificações. */
    void processar(String corpoBruto);
}

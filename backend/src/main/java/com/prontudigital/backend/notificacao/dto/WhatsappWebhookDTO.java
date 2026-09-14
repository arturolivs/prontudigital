package com.prontudigital.backend.notificacao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Recorte do payload que a Meta envia no webhook do WhatsApp Cloud API.
 *
 * <p>Só os campos que a aplicação usa estão mapeados — a Meta acrescenta campos
 * novos sem aviso, então tudo o mais é ignorado de propósito.
 *
 * <p>Formato de referência:
 * <pre>
 * { "object": "whatsapp_business_account",
 *   "entry": [ { "changes": [ { "field": "messages", "value": {
 *       "statuses": [ { "id": "wamid…", "status": "delivered|read|sent|failed",
 *                       "timestamp": "1717000000", "recipient_id": "5581…",
 *                       "errors": [ { "code": 131047, "title": "…" } ] } ],
 *       "messages": [ { "from": "5581…", "type": "text" } ] } } ] } ] }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsappWebhookDTO(

        String object,
        List<Entrada> entry
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entrada(String id, List<Mudanca> changes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mudanca(String field, Conteudo value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Conteudo(
            List<StatusMensagem> statuses,
            List<MensagemRecebida> messages) {}

    /** Callback de entrega de uma mensagem que NÓS enviamos. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusMensagem(
            String id,
            String status,
            String timestamp,
            @JsonProperty("recipient_id") String recipientId,
            List<Erro> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Erro(
            Integer code,
            String title,
            String message,
            @JsonProperty("error_data") DetalheErro errorData) {

        /** Mensagem mais útil disponível, na ordem em que a Meta costuma preenchê-las. */
        public String descricao() {
            if (errorData != null && errorData.details() != null && !errorData.details().isBlank()) {
                return errorData.details();
            }
            if (message != null && !message.isBlank()) {
                return message;
            }
            return title;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetalheErro(String details) {}

    /**
     * Mensagem que o PACIENTE enviou. Não é persistida: interessa porque é ela
     * que abre a janela de 24h em que o texto livre pode ser entregue.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MensagemRecebida(String from, String id, String type) {}
}

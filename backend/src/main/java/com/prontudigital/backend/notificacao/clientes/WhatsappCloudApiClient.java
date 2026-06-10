package com.prontudigital.backend.notificacao.clientes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class WhatsappCloudApiClient {

    private final RestClient restClient;
    private final String phoneNumberId;
    private final boolean configurado;

    public WhatsappCloudApiClient(
            @Value("${app.whatsapp.cloud-api.base-url}") String baseUrl,
            @Value("${app.whatsapp.cloud-api.api-version}") String apiVersion,
            @Value("${app.whatsapp.cloud-api.phone-number-id:}") String phoneNumberId,
            @Value("${app.whatsapp.cloud-api.access-token:}") String accessToken) {
        this.phoneNumberId = phoneNumberId;
        this.configurado = !phoneNumberId.isBlank() && !accessToken.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/" + apiVersion)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    /**
     * Envia uma mensagem de texto livre via WhatsApp Cloud API (Meta).
     * Requer que o destinatário esteja dentro da janela de atendimento de 24h
     * ou que a mensagem corresponda a um template aprovado.
     */
    public void enviarMensagemTexto(String telefone, String mensagem) {
        if (!configurado) {
            throw new IllegalStateException(
                    "Integração com WhatsApp Cloud API não configurada (defina "
                            + "app.whatsapp.cloud-api.access-token e app.whatsapp.cloud-api.phone-number-id)");
        }

        Map<String, Object> corpo = Map.of(
                "messaging_product", "whatsapp",
                "to", normalizarTelefone(telefone),
                "type", "text",
                "text", Map.of("body", mensagem, "preview_url", false));

        ResponseEntity<String> resposta = restClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(String.class);

        log.debug("WhatsApp Cloud API respondeu {}: {}", resposta.getStatusCode(), resposta.getBody());
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 11) {
            digitos = "55" + digitos;
        }
        return digitos;
    }
}

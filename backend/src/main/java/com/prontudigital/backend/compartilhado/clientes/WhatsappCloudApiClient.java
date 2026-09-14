package com.prontudigital.backend.compartilhado.clientes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@Slf4j
public class WhatsappCloudApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String phoneNumberId;
    private final boolean configurado;

    public WhatsappCloudApiClient(
            ObjectMapper objectMapper,
            @Value("${app.whatsapp.cloud-api.base-url}") String baseUrl,
            @Value("${app.whatsapp.cloud-api.api-version}") String apiVersion,
            @Value("${app.whatsapp.cloud-api.phone-number-id:}") String phoneNumberId,
            @Value("${app.whatsapp.cloud-api.access-token:}") String accessToken) {
        this.objectMapper = objectMapper;
        this.phoneNumberId = phoneNumberId;
        this.configurado = !phoneNumberId.isBlank() && !accessToken.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/" + apiVersion)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        if (configurado) {
            log.info("[WHATSAPP] Cliente Cloud API pronto: endpoint={}/{}, phoneNumberId={}",
                    baseUrl, apiVersion, phoneNumberId);
        } else {
            log.warn("[WHATSAPP] Cliente Cloud API NAO configurado (phone-number-id e/ou access-token ausentes). "
                    + "Todos os envios serao recusados ate que as credenciais sejam informadas.");
        }
    }

    /**
     * Envia uma mensagem de texto livre via WhatsApp Cloud API (Meta).
     * Requer que o destinatário esteja dentro da janela de atendimento de 24h
     * ou que a mensagem corresponda a um template aprovado.
     *
     * <p>O retorno HTTP 200 significa apenas que a Meta <b>aceitou</b> a mensagem.
     * A entrega — ou a falha, como o 131047 de janela fechada — chega depois pelo
     * webhook de status, correlacionada pelo id devolvido aqui.
     *
     * @return o {@code wamid} da mensagem aceita, ou {@code null} se a resposta
     *         vier sem id (não impede o envio, só o rastreio da entrega)
     */
    public String enviarMensagemTexto(String telefone, String mensagem) {
        String destino = normalizarTelefone(telefone);
        String destinoLog = mascararTelefone(destino);

        if (!configurado) {
            log.error("[WHATSAPP] Envio para {} abortado: credenciais da Cloud API ausentes", destinoLog);
            throw new IllegalStateException(Mensagens.get("whatsapp.nao-configurado"));
        }

        log.info("[WHATSAPP] Enviando mensagem de texto para {} ({} caracteres)", destinoLog, mensagem.length());
        log.debug("[WHATSAPP] Conteudo da mensagem destinada a {}: {}", destinoLog, mensagem);

        Map<String, Object> corpo = Map.of(
                "messaging_product", "whatsapp",
                "to", destino,
                "type", "text",
                "text", Map.of("body", mensagem, "preview_url", false));

        long inicio = System.currentTimeMillis();
        try {
            ResponseEntity<String> resposta = restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toEntity(String.class);

            String mensagemId = extrairMensagemId(resposta.getBody());
            log.info("[WHATSAPP] Cloud API aceitou a mensagem para {}: HTTP {} em {} ms, wamid={}",
                    destinoLog, resposta.getStatusCode().value(), System.currentTimeMillis() - inicio,
                    mensagemId);
            log.debug("[WHATSAPP] Resposta da Cloud API para {}: {}", destinoLog, resposta.getBody());
            return mensagemId;
        } catch (RestClientResponseException e) {
            log.error("[WHATSAPP] Cloud API recusou a mensagem para {}: HTTP {} em {} ms. Corpo: {}",
                    destinoLog, e.getStatusCode().value(), System.currentTimeMillis() - inicio,
                    e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            log.error("[WHATSAPP] Falha de comunicacao com a Cloud API ao enviar para {} apos {} ms: {}",
                    destinoLog, System.currentTimeMillis() - inicio, e.getMessage(), e);
            throw e;
        }
    }

    /** Extrai {@code messages[0].id} da resposta de envio, sem quebrar se o formato mudar. */
    private String extrairMensagemId(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return null;
        }
        try {
            String id = objectMapper.readTree(corpo).path("messages").path(0).path("id").asText(null);
            if (id == null || id.isBlank()) {
                log.warn("[WHATSAPP] Resposta da Cloud API sem wamid — a entrega desta mensagem "
                        + "nao podera ser rastreada pelo webhook. Corpo: {}", corpo);
                return null;
            }
            return id;
        } catch (Exception e) {
            log.warn("[WHATSAPP] Nao foi possivel ler o wamid da resposta da Cloud API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Oculta o miolo do telefone para que os logs nao exponham o numero completo
     * do paciente. Ex.: {@code 5561999998888} vira {@code 55*******8888}.
     */
    public static String mascararTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return "<sem telefone>";
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 6) {
            return "*".repeat(digitos.length());
        }
        return digitos.substring(0, 2)
                + "*".repeat(digitos.length() - 6)
                + digitos.substring(digitos.length() - 4);
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 11) {
            digitos = "55" + digitos;
        }
        return digitos;
    }
}

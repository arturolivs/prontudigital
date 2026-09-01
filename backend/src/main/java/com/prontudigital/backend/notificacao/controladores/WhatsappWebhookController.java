package com.prontudigital.backend.notificacao.controladores;

import com.prontudigital.backend.notificacao.servicos.WhatsappWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do WhatsApp Cloud API.
 *
 * <p>Público por definição — quem chama é a Meta, sem JWT. A autenticidade vem
 * do {@code X-Hub-Signature-256} (HMAC do corpo com o app secret), validado no
 * serviço.
 */
@RestController
@RequestMapping("/api/whatsapp/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook do WhatsApp",
     description = "Callbacks da Meta com o status de entrega das mensagens enviadas")
public class WhatsappWebhookController {

    private final WhatsappWebhookService webhookService;

    /**
     * Handshake que a Meta faz ao cadastrar a URL no painel: ela devolve o
     * {@code hub.challenge} recebido, em texto puro, se o verify token bater.
     */
    @Operation(summary = "Verificação da URL do webhook",
               description = "Chamado uma vez pela Meta ao cadastrar a URL. Devolve o hub.challenge em texto puro.")
    @SecurityRequirements
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificar(
            @RequestParam("hub.mode") String modo,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String desafio) {

        log.info("[WHATSAPP][WEBHOOK] GET de verificacao recebido (modo={})", modo);

        if (!webhookService.verificacaoValida(modo, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(desafio);
    }

    /**
     * Recebe os status de entrega. Responde 200 mesmo quando o processamento
     * falha: um erro nosso não deve fazer a Meta reenviar o mesmo callback em
     * loop — o motivo fica no log.
     */
    @Operation(summary = "Recebimento de status de entrega",
               description = "Callback da Meta com sent/delivered/read/failed das mensagens enviadas.")
    @SecurityRequirements
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receber(
            @RequestBody String corpo,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String assinatura) {

        log.debug("[WHATSAPP][WEBHOOK] POST recebido ({} bytes)", corpo == null ? 0 : corpo.length());

        if (!webhookService.assinaturaValida(corpo, assinatura)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            webhookService.processar(corpo);
        } catch (Exception e) {
            log.error("[WHATSAPP][WEBHOOK] Erro ao processar o callback: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}

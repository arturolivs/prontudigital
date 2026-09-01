package com.prontudigital.backend.notificacao.servicos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
import com.prontudigital.backend.notificacao.dto.WhatsappWebhookDTO;
import com.prontudigital.backend.notificacao.entidades.LogNotificacaoWhatsapp;
import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.repositorios.LogNotificacaoWhatsappRepository;
import com.prontudigital.backend.notificacao.servicos.WhatsappWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappWebhookServiceImpl implements WhatsappWebhookService {

    private static final String ALGORITMO_ASSINATURA = "HmacSHA256";
    private static final String PREFIXO_ASSINATURA = "sha256=";

    private final LogNotificacaoWhatsappRepository logRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.whatsapp.webhook.verify-token:}")
    private String verifyToken;

    @Value("${app.whatsapp.webhook.app-secret:}")
    private String appSecret;

    @Override
    public boolean verificacaoValida(String modo, String token) {
        if (verifyToken.isBlank()) {
            log.error("[WHATSAPP][WEBHOOK] Verificacao recusada: app.whatsapp.webhook.verify-token "
                    + "nao configurado (WHATSAPP_WEBHOOK_VERIFY_TOKEN)");
            return false;
        }
        boolean valido = "subscribe".equals(modo) && verifyToken.equals(token);
        if (valido) {
            log.info("[WHATSAPP][WEBHOOK] Handshake de verificacao aceito");
        } else {
            log.warn("[WHATSAPP][WEBHOOK] Handshake recusado: modo={} e token {}",
                    modo, verifyToken.equals(token) ? "correto" : "divergente");
        }
        return valido;
    }

    @Override
    public boolean assinaturaValida(String corpoBruto, String assinatura) {
        if (appSecret.isBlank()) {
            log.warn("[WHATSAPP][WEBHOOK] app.whatsapp.webhook.app-secret nao configurado — "
                    + "callback aceito SEM validar a assinatura. Nao use assim em producao.");
            return true;
        }
        if (assinatura == null || !assinatura.startsWith(PREFIXO_ASSINATURA)) {
            log.warn("[WHATSAPP][WEBHOOK] Callback sem o cabecalho X-Hub-Signature-256 — descartado");
            return false;
        }

        try {
            Mac mac = Mac.getInstance(ALGORITMO_ASSINATURA);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITMO_ASSINATURA));
            String esperado = HexFormat.of()
                    .formatHex(mac.doFinal(corpoBruto.getBytes(StandardCharsets.UTF_8)));

            boolean valido = MessageDigest.isEqual(
                    esperado.getBytes(StandardCharsets.UTF_8),
                    assinatura.substring(PREFIXO_ASSINATURA.length()).getBytes(StandardCharsets.UTF_8));

            if (!valido) {
                log.warn("[WHATSAPP][WEBHOOK] Assinatura invalida — callback descartado. "
                        + "Confira o app secret configurado.");
            }
            return valido;
        } catch (Exception e) {
            log.error("[WHATSAPP][WEBHOOK] Falha ao validar a assinatura do callback: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void processar(String corpoBruto) {
        WhatsappWebhookDTO payload;
        try {
            payload = objectMapper.readValue(corpoBruto, WhatsappWebhookDTO.class);
        } catch (Exception e) {
            log.error("[WHATSAPP][WEBHOOK] Payload ilegivel, descartado: {}. Corpo: {}", e.getMessage(), corpoBruto);
            return;
        }

        if (payload.entry() == null) {
            log.debug("[WHATSAPP][WEBHOOK] Callback sem 'entry' — nada a processar");
            return;
        }

        for (WhatsappWebhookDTO.Entrada entrada : payload.entry()) {
            if (entrada == null || entrada.changes() == null) continue;

            for (WhatsappWebhookDTO.Mudanca mudanca : entrada.changes()) {
                if (mudanca == null || mudanca.value() == null) continue;

                aplicarStatus(mudanca.value().statuses());
                registrarMensagensRecebidas(mudanca.value().messages());
            }
        }
    }

    private void aplicarStatus(List<WhatsappWebhookDTO.StatusMensagem> statuses) {
        if (statuses == null) return;

        for (WhatsappWebhookDTO.StatusMensagem status : statuses) {
            if (status == null || status.id() == null) continue;

            Optional<LogNotificacaoWhatsapp> registro = logRepository.findByMensagemId(status.id());
            if (registro.isEmpty()) {
                // Mensagem enviada por fora da aplicação (o botão do painel da Meta,
                // um curl de teste) ou anterior ao rastreio por wamid.
                log.debug("[WHATSAPP][WEBHOOK] Status '{}' para o wamid {}, que nao esta no log de notificacoes",
                        status.status(), status.id());
                continue;
            }

            atualizar(registro.get(), status);
        }
    }

    private void atualizar(LogNotificacaoWhatsapp registro, WhatsappWebhookDTO.StatusMensagem status) {
        LocalDateTime momento = converterTimestamp(status.timestamp());
        String situacao = status.status() == null ? "" : status.status().toLowerCase();
        String telefoneLog = WhatsappCloudApiClient.mascararTelefone(registro.getTelefone());

        switch (situacao) {
            case "sent" -> {
                if (registro.getEnviadoEm() == null) registro.setEnviadoEm(momento);
                log.debug("[WHATSAPP][WEBHOOK] Agendamento {}: mensagem entregue aos servidores da Meta",
                        registro.getAgendamentoId());
            }
            case "delivered" -> {
                registro.setEntregueEm(momento);
                log.info("[WHATSAPP][WEBHOOK] Agendamento {}: mensagem ENTREGUE no aparelho de {} em {}",
                        registro.getAgendamentoId(), telefoneLog, momento);
            }
            case "read" -> {
                if (registro.getEntregueEm() == null) registro.setEntregueEm(momento);
                registro.setLidoEm(momento);
                log.info("[WHATSAPP][WEBHOOK] Agendamento {}: mensagem LIDA por {} em {}",
                        registro.getAgendamentoId(), telefoneLog, momento);
            }
            case "failed" -> registrarFalha(registro, status, telefoneLog);
            default -> log.debug("[WHATSAPP][WEBHOOK] Agendamento {}: status '{}' ignorado",
                    registro.getAgendamentoId(), status.status());
        }

        logRepository.save(registro);
    }

    private void registrarFalha(LogNotificacaoWhatsapp registro,
                                WhatsappWebhookDTO.StatusMensagem status,
                                String telefoneLog) {
        WhatsappWebhookDTO.Erro erro = (status.errors() == null || status.errors().isEmpty())
                ? null
                : status.errors().get(0);

        String codigo = erro == null || erro.code() == null ? null : String.valueOf(erro.code());
        String detalhe = erro == null ? "sem detalhe informado pela Meta" : erro.descricao();

        registro.setErroCodigo(codigo);
        registro.setErroDetalhe(detalhe);

        // CONFIRMADO/RECUSADO/EXPIRADO já refletem uma decisão posterior do
        // paciente — o resultado da entrega não pode sobrescrever isso.
        if (registro.getStatus() == StatusNotificacao.PENDENTE
                || registro.getStatus() == StatusNotificacao.ENVIADO) {
            registro.setStatus(StatusNotificacao.FALHA);
        }

        log.error("[WHATSAPP][WEBHOOK] Agendamento {}: a Meta NAO entregou a mensagem para {} — "
                        + "codigo {}, motivo: {}{}",
                registro.getAgendamentoId(), telefoneLog, codigo, detalhe,
                "131047".equals(codigo)
                        ? ". A janela de 24h esta fechada: o paciente precisa enviar uma mensagem "
                          + "para o numero da clinica, ou o envio precisa usar um template aprovado."
                        : "");
    }

    private void registrarMensagensRecebidas(List<WhatsappWebhookDTO.MensagemRecebida> mensagens) {
        if (mensagens == null || mensagens.isEmpty()) return;

        for (WhatsappWebhookDTO.MensagemRecebida mensagem : mensagens) {
            if (mensagem == null) continue;
            // Não é persistida: o que importa é que a partir daqui abre a janela
            // de 24h em que a Meta entrega texto livre para este número.
            log.info("[WHATSAPP][WEBHOOK] Mensagem recebida de {} (tipo {}) — janela de 24h aberta",
                    WhatsappCloudApiClient.mascararTelefone(mensagem.from()), mensagem.type());
        }
    }

    /** A Meta manda epoch em segundos; sem valor utilizável, cai no relógio da aplicação. */
    private LocalDateTime converterTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.now(clock);
        }
        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(timestamp.trim())), clock.getZone());
        } catch (NumberFormatException e) {
            log.warn("[WHATSAPP][WEBHOOK] Timestamp '{}' fora do formato esperado — usando o horario atual",
                    timestamp);
            return LocalDateTime.now(clock);
        }
    }
}

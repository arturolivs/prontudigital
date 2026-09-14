package com.prontudigital.backend.notificacao.servicos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.backend.notificacao.entidades.LogNotificacaoWhatsapp;
import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.enums.TipoNotificacao;
import com.prontudigital.backend.notificacao.repositorios.LogNotificacaoWhatsappRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsappWebhookServiceImpl")
class WhatsappWebhookServiceImplTest {

    private static final String WAMID        = "wamid.HBgMNTU4MTc5MTI1MzIzFQIAERgS";
    private static final String VERIFY_TOKEN = "token-combinado-com-a-meta";
    private static final String APP_SECRET   = "segredo-do-app";
    private static final UUID PACIENTE_UUID  = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Mock private LogNotificacaoWhatsappRepository logRepository;

    private WhatsappWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new WhatsappWebhookServiceImpl(logRepository, new ObjectMapper(), clock);

        ReflectionTestUtils.setField(service, "verifyToken", VERIFY_TOKEN);
        ReflectionTestUtils.setField(service, "appSecret", APP_SECRET);
    }

    // =========================================================
    // Fixtures locais
    // =========================================================
    private LogNotificacaoWhatsapp registro(StatusNotificacao status) {
        return LogNotificacaoWhatsapp.builder()
                .id(10L)
                .agendamentoId(1L)
                .pacienteUuid(PACIENTE_UUID)
                .tipo(TipoNotificacao.CONFIRMACAO_24H)
                .status(status)
                .telefone("+5581979125323")
                .mensagemId(WAMID)
                .enviadoEm(AGORA.minusMinutes(5))
                .tentativas(1)
                .build();
    }

    private String payloadStatus(String situacao) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"899","changes":[{"field":"messages",
                 "value":{"messaging_product":"whatsapp","statuses":[
                   {"id":"%s","status":"%s","timestamp":"%d","recipient_id":"558179125323"}]}}]}]}
                """.formatted(WAMID, situacao, AGORA.toEpochSecond(ZoneOffset.UTC));
    }

    private String payloadFalha() {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"899","changes":[{"field":"messages",
                 "value":{"messaging_product":"whatsapp","statuses":[
                   {"id":"%s","status":"failed","timestamp":"%d","recipient_id":"558179125323",
                    "errors":[{"code":131047,"title":"Re-engagement message",
                               "error_data":{"details":"Message failed to send because more than 24 hours have passed"}}]}]}}]}]}
                """.formatted(WAMID, AGORA.toEpochSecond(ZoneOffset.UTC));
    }

    private String assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private LogNotificacaoWhatsapp salvo() {
        ArgumentCaptor<LogNotificacaoWhatsapp> captor = ArgumentCaptor.forClass(LogNotificacaoWhatsapp.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    // =========================================================
    @Nested
    @DisplayName("verificacaoValida")
    class Verificacao {

        @Test
        @DisplayName("aceita o handshake quando modo e token conferem")
        void aceita() {
            assertTrue(service.verificacaoValida("subscribe", VERIFY_TOKEN));
        }

        @Test
        @DisplayName("recusa token divergente")
        void recusaTokenErrado() {
            assertFalse(service.verificacaoValida("subscribe", "outro-token"));
        }

        @Test
        @DisplayName("recusa quando o verify token nao esta configurado")
        void recusaSemConfiguracao() {
            ReflectionTestUtils.setField(service, "verifyToken", "");
            assertFalse(service.verificacaoValida("subscribe", ""));
        }
    }

    @Nested
    @DisplayName("assinaturaValida")
    class Assinatura {

        @Test
        @DisplayName("aceita o HMAC correto do corpo")
        void aceitaHmacCorreto() {
            String corpo = payloadStatus("delivered");
            assertTrue(service.assinaturaValida(corpo, assinar(corpo)));
        }

        @Test
        @DisplayName("recusa HMAC de outro corpo")
        void recusaHmacDeOutroCorpo() {
            assertFalse(service.assinaturaValida(payloadStatus("delivered"), assinar("outro corpo")));
        }

        @Test
        @DisplayName("recusa callback sem o cabecalho de assinatura")
        void recusaSemCabecalho() {
            assertFalse(service.assinaturaValida(payloadStatus("delivered"), null));
        }

        @Test
        @DisplayName("sem app secret configurado, aceita sem conferir (dev)")
        void aceitaSemAppSecret() {
            ReflectionTestUtils.setField(service, "appSecret", "");
            assertTrue(service.assinaturaValida(payloadStatus("delivered"), null));
        }
    }

    @Nested
    @DisplayName("processar")
    class Processar {

        @Test
        @DisplayName("'delivered' marca a data de entrega sem mexer no status")
        void marcaEntrega() {
            when(logRepository.findByMensagemId(WAMID)).thenReturn(Optional.of(registro(StatusNotificacao.ENVIADO)));

            service.processar(payloadStatus("delivered"));

            LogNotificacaoWhatsapp resultado = salvo();
            assertEquals(AGORA, resultado.getEntregueEm());
            assertNull(resultado.getLidoEm());
            assertEquals(StatusNotificacao.ENVIADO, resultado.getStatus());
        }

        @Test
        @DisplayName("'read' marca leitura e preenche a entrega que nao tinha chegado")
        void marcaLeitura() {
            when(logRepository.findByMensagemId(WAMID)).thenReturn(Optional.of(registro(StatusNotificacao.ENVIADO)));

            service.processar(payloadStatus("read"));

            LogNotificacaoWhatsapp resultado = salvo();
            assertEquals(AGORA, resultado.getLidoEm());
            assertEquals(AGORA, resultado.getEntregueEm());
        }

        @Test
        @DisplayName("'failed' grava FALHA com o codigo e o motivo da Meta")
        void marcaFalha() {
            when(logRepository.findByMensagemId(WAMID)).thenReturn(Optional.of(registro(StatusNotificacao.ENVIADO)));

            service.processar(payloadFalha());

            LogNotificacaoWhatsapp resultado = salvo();
            assertEquals(StatusNotificacao.FALHA, resultado.getStatus());
            assertEquals("131047", resultado.getErroCodigo());
            assertTrue(resultado.getErroDetalhe().contains("24 hours"));
        }

        @Test
        @DisplayName("'failed' nao sobrescreve uma resposta ja dada pelo paciente")
        void naoSobrescreveConfirmado() {
            when(logRepository.findByMensagemId(WAMID)).thenReturn(Optional.of(registro(StatusNotificacao.CONFIRMADO)));

            service.processar(payloadFalha());

            LogNotificacaoWhatsapp resultado = salvo();
            assertEquals(StatusNotificacao.CONFIRMADO, resultado.getStatus());
            assertEquals("131047", resultado.getErroCodigo());
        }

        @Test
        @DisplayName("ignora status de wamid que nao esta no log")
        void ignoraWamidDesconhecido() {
            when(logRepository.findByMensagemId(WAMID)).thenReturn(Optional.empty());

            service.processar(payloadStatus("delivered"));

            verify(logRepository, never()).save(any());
        }

        @Test
        @DisplayName("payload ilegivel nao propaga excecao")
        void payloadInvalido() {
            assertDoesNotThrow(() -> service.processar("{isso nao e json"));
            verify(logRepository, never()).save(any());
        }

        @Test
        @DisplayName("callback so com mensagem recebida nao altera nada")
        void mensagemRecebida() {
            String corpo = """
                    {"object":"whatsapp_business_account","entry":[{"id":"899","changes":[{"field":"messages",
                     "value":{"messaging_product":"whatsapp","messages":[
                       {"from":"558179125323","id":"wamid.INBOUND","type":"text"}]}}]}]}
                    """;

            assertDoesNotThrow(() -> service.processar(corpo));
            verify(logRepository, never()).save(any());
        }
    }
}

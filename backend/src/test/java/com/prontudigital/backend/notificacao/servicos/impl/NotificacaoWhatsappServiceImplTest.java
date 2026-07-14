package com.prontudigital.backend.notificacao.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.FilaEsperaRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
import com.prontudigital.backend.notificacao.entidades.LogNotificacaoWhatsapp;
import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.enums.TipoNotificacao;
import com.prontudigital.backend.notificacao.excecoes.TokenConfirmacaoInvalidoException;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoWhatsappServiceImpl")
class NotificacaoWhatsappServiceImplTest {

    private static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TOKEN             = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final String TELEFONE   = "+5511999999999";
    private static final String URL_BASE   = "https://app.local/confirmacao";
    private static final LocalDateTime AGORA  = LocalDateTime.of(2026, 6, 1, 12, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 6, 1, 14, 0);

    @Mock private LogNotificacaoWhatsappRepository logRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private FilaEsperaRepository filaEsperaRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private WhatsappCloudApiClient whatsappCliente;

    private NotificacaoWhatsappServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new NotificacaoWhatsappServiceImpl(
                logRepository, agendamentoRepository, filaEsperaRepository,
                usuarioService, whatsappCliente, clock);

        ReflectionTestUtils.setField(service, "urlBaseConfirmacao", URL_BASE);
    }

    // =========================================================
    // Fixtures locais
    // =========================================================
    private Agendamento agendamento() {
        return Agendamento.builder()
                .id(1L)
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(INICIO)
                .fimEm(INICIO.plusHours(1))
                .status(StatusAgendamento.AGENDADO)
                .tipo(TipoAgendamento.AVALIACAO)
                .build();
    }

    private UsuarioDTO paciente() {
        return UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Maria Paciente").telefone(TELEFONE).build();
    }

    private UsuarioDTO profissional() {
        return UsuarioDTO.builder().uuid(PROFISSIONAL_UUID).nomeCompleto("Dr. Joao").telefone("+5511888888888").build();
    }

    private LogNotificacaoWhatsapp logConfirmacao(StatusNotificacao status, LocalDateTime expiraEm) {
        return LogNotificacaoWhatsapp.builder()
                .id(1L)
                .agendamentoId(1L)
                .pacienteUuid(PACIENTE_UUID)
                .tipo(TipoNotificacao.CONFIRMACAO_24H)
                .status(status)
                .tokenConfirmacao(TOKEN)
                .tokenExpiraEm(expiraEm)
                .tentativas(1)
                .build();
    }

    private void mockUsuarios() {
        when(usuarioService.buscarPorUuid(PACIENTE_UUID)).thenReturn(paciente());
        when(usuarioService.buscarPorUuid(PROFISSIONAL_UUID)).thenReturn(profissional());
    }

    private LogNotificacaoWhatsapp capturarLogSalvo() {
        ArgumentCaptor<LogNotificacaoWhatsapp> captor = ArgumentCaptor.forClass(LogNotificacaoWhatsapp.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    // =========================================================
    // enviarLembrete48h()
    // =========================================================
    @Nested
    @DisplayName("enviarLembrete48h()")
    class EnviarLembrete48h {

        @Test
        @DisplayName("nao reenvia quando ja existe lembrete para o agendamento")
        void naoReenviaSeJaExiste() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.LEMBRETE_48H))
                    .thenReturn(true);

            service.enviarLembrete48h(agendamento());

            verify(logRepository, never()).save(any());
            verifyNoInteractions(whatsappCliente, usuarioService);
        }

        @Test
        @DisplayName("envia e registra ENVIADO em caso de sucesso")
        void enviaComSucesso() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.LEMBRETE_48H))
                    .thenReturn(false);
            mockUsuarios();

            service.enviarLembrete48h(agendamento());

            verify(whatsappCliente).enviarMensagemTexto(eq(TELEFONE), anyString());
            LogNotificacaoWhatsapp salvo = capturarLogSalvo();
            assertEquals(StatusNotificacao.ENVIADO, salvo.getStatus());
            assertEquals(TipoNotificacao.LEMBRETE_48H, salvo.getTipo());
            assertEquals(1, salvo.getTentativas());
            assertEquals(AGORA, salvo.getEnviadoEm());
        }

        @Test
        @DisplayName("registra FALHA e nao propaga quando o cliente WhatsApp falha")
        void registraFalhaSemPropagar() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.LEMBRETE_48H))
                    .thenReturn(false);
            mockUsuarios();
            doThrow(new RuntimeException("API fora do ar"))
                    .when(whatsappCliente).enviarMensagemTexto(anyString(), anyString());

            assertDoesNotThrow(() -> service.enviarLembrete48h(agendamento()));

            LogNotificacaoWhatsapp salvo = capturarLogSalvo();
            assertEquals(StatusNotificacao.FALHA, salvo.getStatus());
            assertEquals(1, salvo.getTentativas());
            assertNull(salvo.getEnviadoEm());
        }
    }

    // =========================================================
    // enviarSolicitacaoConfirmacao24h()
    // =========================================================
    @Nested
    @DisplayName("enviarSolicitacaoConfirmacao24h()")
    class EnviarConfirmacao24h {

        @Test
        @DisplayName("nao reenvia quando ja existe solicitacao para o agendamento")
        void naoReenviaSeJaExiste() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.CONFIRMACAO_24H))
                    .thenReturn(true);

            service.enviarSolicitacaoConfirmacao24h(agendamento());

            verify(logRepository, never()).save(any());
            verifyNoInteractions(whatsappCliente, usuarioService);
        }

        @Test
        @DisplayName("gera token, links e registra ENVIADO em caso de sucesso")
        void enviaComSucesso() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.CONFIRMACAO_24H))
                    .thenReturn(false);
            mockUsuarios();

            service.enviarSolicitacaoConfirmacao24h(agendamento());

            ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
            verify(whatsappCliente).enviarMensagemTexto(eq(TELEFONE), mensagem.capture());

            LogNotificacaoWhatsapp salvo = capturarLogSalvo();
            assertEquals(StatusNotificacao.ENVIADO, salvo.getStatus());
            assertEquals(TipoNotificacao.CONFIRMACAO_24H, salvo.getTipo());
            assertNotNull(salvo.getTokenConfirmacao());

            assertEquals(INICIO.minusHours(2), salvo.getTokenExpiraEm());

            String token = salvo.getTokenConfirmacao().toString();
            assertTrue(mensagem.getValue().contains(URL_BASE + "/" + token + "/confirmar"));
            assertTrue(mensagem.getValue().contains(URL_BASE + "/" + token + "/recusar"));
        }

        @Test
        @DisplayName("registra FALHA e nao propaga quando o cliente WhatsApp falha")
        void registraFalhaSemPropagar() {
            when(logRepository.existsByAgendamentoIdAndTipo(1L, TipoNotificacao.CONFIRMACAO_24H))
                    .thenReturn(false);
            mockUsuarios();
            doThrow(new RuntimeException("API fora do ar"))
                    .when(whatsappCliente).enviarMensagemTexto(anyString(), anyString());

            assertDoesNotThrow(() -> service.enviarSolicitacaoConfirmacao24h(agendamento()));

            LogNotificacaoWhatsapp salvo = capturarLogSalvo();
            assertEquals(StatusNotificacao.FALHA, salvo.getStatus());
        }
    }

    // =========================================================
    // confirmarViaToken()
    // =========================================================
    @Nested
    @DisplayName("confirmarViaToken()")
    class ConfirmarViaToken {

        @Test
        @DisplayName("token inexistente lanca TokenConfirmacaoInvalidoException")
        void tokenInexistente() {
            when(logRepository.findByTokenConfirmacao(TOKEN)).thenReturn(Optional.empty());

            assertThrows(TokenConfirmacaoInvalidoException.class,
                    () -> service.confirmarViaToken(TOKEN));
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("token ja utilizado (CONFIRMADO) lanca excecao")
        void tokenJaUtilizado() {
            when(logRepository.findByTokenConfirmacao(TOKEN))
                    .thenReturn(Optional.of(logConfirmacao(StatusNotificacao.CONFIRMADO, AGORA.plusHours(1))));

            assertThrows(TokenConfirmacaoInvalidoException.class,
                    () -> service.confirmarViaToken(TOKEN));
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("token expirado marca EXPIRADO e lanca excecao")
        void tokenExpirado() {
            LogNotificacaoWhatsapp entrada =
                    logConfirmacao(StatusNotificacao.ENVIADO, AGORA.minusHours(1));
            when(logRepository.findByTokenConfirmacao(TOKEN)).thenReturn(Optional.of(entrada));

            assertThrows(TokenConfirmacaoInvalidoException.class,
                    () -> service.confirmarViaToken(TOKEN));

            assertEquals(StatusNotificacao.EXPIRADO, entrada.getStatus());
            verify(logRepository).save(entrada);
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("agendamento inexistente lanca excecao")
        void agendamentoInexistente() {
            when(logRepository.findByTokenConfirmacao(TOKEN))
                    .thenReturn(Optional.of(logConfirmacao(StatusNotificacao.ENVIADO, AGORA.plusHours(1))));
            when(agendamentoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(TokenConfirmacaoInvalidoException.class,
                    () -> service.confirmarViaToken(TOKEN));
        }

        @Test
        @DisplayName("confirma o agendamento e a entrada em caso de sucesso")
        void confirmaComSucesso() {
            LogNotificacaoWhatsapp entrada =
                    logConfirmacao(StatusNotificacao.ENVIADO, AGORA.plusHours(1));
            Agendamento agendamento = agendamento();
            when(logRepository.findByTokenConfirmacao(TOKEN)).thenReturn(Optional.of(entrada));
            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

            service.confirmarViaToken(TOKEN);

            assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
            assertEquals(StatusNotificacao.CONFIRMADO, entrada.getStatus());
            assertEquals(AGORA, entrada.getRespondidoEm());
            verify(agendamentoRepository).save(agendamento);
            verify(logRepository).save(entrada);
            verify(filaEsperaRepository, never()).save(any());
        }
    }

    // =========================================================
    // recusarViaToken()
    // =========================================================
    @Nested
    @DisplayName("recusarViaToken()")
    class RecusarViaToken {

        @Test
        @DisplayName("recusa: cancela o agendamento, gera fila de espera e marca RECUSADO")
        void recusaComSucesso() {
            LogNotificacaoWhatsapp entrada =
                    logConfirmacao(StatusNotificacao.ENVIADO, AGORA.plusHours(1));
            Agendamento agendamento = agendamento();
            when(logRepository.findByTokenConfirmacao(TOKEN)).thenReturn(Optional.of(entrada));
            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

            service.recusarViaToken(TOKEN);

            assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
            assertEquals(StatusNotificacao.RECUSADO, entrada.getStatus());
            assertEquals(AGORA, entrada.getRespondidoEm());
            verify(agendamentoRepository).save(agendamento);
            verify(filaEsperaRepository).save(any(FilaEspera.class));
            verify(logRepository).save(entrada);
        }

        @Test
        @DisplayName("token expirado marca EXPIRADO e nao cancela o agendamento")
        void tokenExpirado() {
            LogNotificacaoWhatsapp entrada =
                    logConfirmacao(StatusNotificacao.ENVIADO, AGORA.minusHours(1));
            when(logRepository.findByTokenConfirmacao(TOKEN)).thenReturn(Optional.of(entrada));

            assertThrows(TokenConfirmacaoInvalidoException.class,
                    () -> service.recusarViaToken(TOKEN));

            assertEquals(StatusNotificacao.EXPIRADO, entrada.getStatus());
            verify(agendamentoRepository, never()).save(any());
            verify(filaEsperaRepository, never()).save(any());
        }
    }

    // =========================================================
    // processarNaoConfirmados()
    // =========================================================
    @Nested
    @DisplayName("processarNaoConfirmados()")
    class ProcessarNaoConfirmados {

        @Test
        @DisplayName("libera vaga e expira o log de agendamentos com confirmacao enviada e sem resposta")
        void liberaNaoConfirmado() {
            Agendamento agendamento = agendamento();
            LogNotificacaoWhatsapp entrada =
                    logConfirmacao(StatusNotificacao.ENVIADO, AGORA.plusHours(1));

            when(agendamentoRepository.findByStatusInAndInicioEmBetween(any(), any(), any()))
                    .thenReturn(List.of(agendamento));
            when(logRepository.existsByAgendamentoIdAndTipoAndStatus(
                    1L, TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO))
                    .thenReturn(true);
            when(logRepository.findByAgendamentoIdAndTipoAndStatus(
                    1L, TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO))
                    .thenReturn(Optional.of(entrada));

            service.processarNaoConfirmados();

            assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
            assertEquals(StatusNotificacao.EXPIRADO, entrada.getStatus());
            verify(agendamentoRepository).save(agendamento);
            verify(filaEsperaRepository).save(any(FilaEspera.class));
            verify(logRepository).save(entrada);
        }

        @Test
        @DisplayName("ignora agendamentos sem solicitacao de confirmacao enviada")
        void ignoraSemConfirmacaoEnviada() {
            when(agendamentoRepository.findByStatusInAndInicioEmBetween(any(), any(), any()))
                    .thenReturn(List.of(agendamento()));
            when(logRepository.existsByAgendamentoIdAndTipoAndStatus(
                    1L, TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO))
                    .thenReturn(false);

            service.processarNaoConfirmados();

            verify(agendamentoRepository, never()).save(any());
            verify(filaEsperaRepository, never()).save(any());
            verify(logRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao faz nada quando nao ha candidatos")
        void semCandidatos() {
            when(agendamentoRepository.findByStatusInAndInicioEmBetween(any(), any(), any()))
                    .thenReturn(List.of());

            service.processarNaoConfirmados();

            verifyNoInteractions(filaEsperaRepository);
            verify(agendamentoRepository, never()).save(any());
        }
    }
}

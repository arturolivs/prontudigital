package com.prontudigital.backend.relatorio.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.HorarioTrabalhoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RelatorioServiceImpl (RF19/RF20)")
class RelatorioServiceImplTest {

    private static final LocalDate INICIO_PERIODO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIM_PERIODO = LocalDate.of(2026, 6, 7);

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private HorarioTrabalhoRepository horarioTrabalhoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;

    private final AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    private RelatorioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RelatorioServiceImpl(
                agendamentoRepository, horarioTrabalhoRepository,
                usuarioService, usuarioContexto, permissaoPolicy);

        when(usuarioService.buscarPorUuid(any()))
                .thenReturn(UsuarioDTO.builder().nomeCompleto("Fulano").build());
    }

    /** Agendamento de 1h no dia informado. */
    private Agendamento agendamento(int dia, int hora, StatusAgendamento status) {
        return Agendamento.builder()
                .id((long) (dia * 100 + hora))
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(LocalDateTime.of(2026, 6, dia, hora, 0))
                .fimEm(LocalDateTime.of(2026, 6, dia, hora + 1, 0))
                .tipo(TipoAgendamento.TRATAMENTO)
                .status(status)
                .build();
    }

    private void mockAgendamentos(List<Agendamento> agendamentos) {
        when(agendamentoRepository.buscarParaRelatorio(any(), any(), any(), any(), any()))
                .thenReturn(agendamentos);
    }

    // =========================================================
    // atendimentos() - RF19
    // =========================================================
    @Nested
    @DisplayName("atendimentos() (RF19)")
    class Atendimentos {

        @Test
        @DisplayName("admin gera relatorio da clinica inteira e agrupa por status e tipo")
        void deveAgruparTotais() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(
                    agendamento(1, 8, StatusAgendamento.REALIZADO),
                    agendamento(1, 10, StatusAgendamento.REALIZADO),
                    agendamento(2, 8, StatusAgendamento.CANCELADO)));

            RelatorioAtendimentosDTO r =
                    service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null, null, null);

            assertEquals(3, r.total());
            assertEquals(3, r.itens().size());
            assertEquals(2L, r.totalPorStatus().get("REALIZADO"));
            assertEquals(1L, r.totalPorStatus().get("CANCELADO"));
            assertEquals(3L, r.totalPorTipo().get("TRATAMENTO"));
            // sem filtro de profissional, o relatorio nao nomeia ninguem
            assertNull(r.nomeProfissional());
        }

        @Test
        @DisplayName("calcula a duracao de cada atendimento em minutos")
        void deveCalcularDuracao() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(agendamento(1, 8, StatusAgendamento.REALIZADO)));

            RelatorioAtendimentosDTO r =
                    service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null, null, null);

            assertEquals(60, r.itens().get(0).duracaoMinutos());
        }

        @Test
        @DisplayName("profissional tem o filtro forcado para a propria agenda")
        void deveForcarProprioProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            mockAgendamentos(List.of());

            service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null, null, null);

            verify(agendamentoRepository).buscarParaRelatorio(
                    any(), any(), eq(PROFISSIONAL_UUID), any(), any());
        }

        @Test
        @DisplayName("profissional nao consulta a agenda de outro")
        void deveRejeitarAgendaAlheia() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            UUID outro = UUID.randomUUID();

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.atendimentos(INICIO_PERIODO, FIM_PERIODO, outro, null, null));
            verify(agendamentoRepository, never())
                    .buscarParaRelatorio(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("paciente nao acessa relatorio")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null, null, null));
        }

        @Test
        @DisplayName("rejeita periodo invertido")
        void deveRejeitarPeriodoInvertido() {
            assertThrows(AgendamentoInvalidoException.class,
                    () -> service.atendimentos(FIM_PERIODO, INICIO_PERIODO, null, null, null));
        }

        @Test
        @DisplayName("repassa os filtros de status e tipo ao repositorio")
        void deveRepassarFiltros() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of());

            service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null,
                    StatusAgendamento.REALIZADO, TipoAgendamento.AVALIACAO);

            verify(agendamentoRepository).buscarParaRelatorio(
                    eq(INICIO_PERIODO.atStartOfDay()),
                    eq(FIM_PERIODO.atTime(LocalTime.MAX)),
                    eq(null),
                    eq(StatusAgendamento.REALIZADO),
                    eq(TipoAgendamento.AVALIACAO));
        }

        @Test
        @DisplayName("resolve o nome de cada usuario uma unica vez")
        void deveCachearNomes() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(
                    agendamento(1, 8, StatusAgendamento.REALIZADO),
                    agendamento(1, 10, StatusAgendamento.REALIZADO),
                    agendamento(2, 8, StatusAgendamento.REALIZADO)));

            service.atendimentos(INICIO_PERIODO, FIM_PERIODO, null, null, null);

            // 3 agendamentos do mesmo par paciente/profissional = 2 consultas, nao 6
            verify(usuarioService, times(1)).buscarPorUuid(PACIENTE_UUID);
            verify(usuarioService, times(1)).buscarPorUuid(PROFISSIONAL_UUID);
        }
    }

    // =========================================================
    // ocupacao() - RF20
    // =========================================================
    @Nested
    @DisplayName("ocupacao() (RF20)")
    class Ocupacao {

        @Test
        @DisplayName("conta cada status e calcula as taxas")
        void deveCalcularTaxas() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(
                    agendamento(1, 8, StatusAgendamento.REALIZADO),
                    agendamento(1, 10, StatusAgendamento.REALIZADO),
                    agendamento(1, 12, StatusAgendamento.REALIZADO),
                    agendamento(2, 8, StatusAgendamento.NAO_COMPARECEU),
                    agendamento(2, 10, StatusAgendamento.CANCELADO)));

            RelatorioOcupacaoDTO r = service.ocupacao(INICIO_PERIODO, FIM_PERIODO, null);

            assertEquals(5, r.total());
            assertEquals(3, r.realizados());
            assertEquals(1, r.naoCompareceram());
            assertEquals(1, r.cancelados());
            // comparecimento e absenteismo sobre realizados + faltas = 4
            assertEquals(75.0, r.taxaComparecimento());
            assertEquals(25.0, r.taxaAbsenteismo());
            // cancelamento sobre o total = 5
            assertEquals(20.0, r.taxaCancelamento());
        }

        @Test
        @DisplayName("cancelado nao ocupa a agenda; falta ocupa")
        void deveContarHorasOcupadas() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(
                    agendamento(1, 8, StatusAgendamento.REALIZADO),
                    agendamento(1, 10, StatusAgendamento.NAO_COMPARECEU),
                    agendamento(1, 12, StatusAgendamento.CANCELADO),
                    agendamento(1, 14, StatusAgendamento.REMARCADO)));

            RelatorioOcupacaoDTO r = service.ocupacao(INICIO_PERIODO, FIM_PERIODO, null);

            // só realizado + falta = 2h
            assertEquals(2.0, r.horasAgendadas());
        }

        @Test
        @DisplayName("periodo sem agendamentos devolve taxas nulas em vez de zero")
        void deveDevolverNuloSemBase() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of());

            RelatorioOcupacaoDTO r = service.ocupacao(INICIO_PERIODO, FIM_PERIODO, null);

            assertEquals(0, r.total());
            assertNull(r.taxaComparecimento());
            assertNull(r.taxaCancelamento());
            assertNull(r.taxaAbsenteismo());
        }

        @Test
        @DisplayName("sem profissional filtrado nao ha capacidade conhecida")
        void deveOmitirOcupacaoSemProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(agendamento(1, 8, StatusAgendamento.REALIZADO)));

            RelatorioOcupacaoDTO r = service.ocupacao(INICIO_PERIODO, FIM_PERIODO, null);

            assertNull(r.horasDisponiveis());
            assertNull(r.taxaOcupacao());
            verifyNoInteractions(horarioTrabalhoRepository);
        }

        @Test
        @DisplayName("profissional sem expediente cadastrado nao tem taxa de ocupacao")
        void deveOmitirOcupacaoSemExpediente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(agendamento(1, 8, StatusAgendamento.REALIZADO)));
            when(horarioTrabalhoRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of());

            RelatorioOcupacaoDTO r =
                    service.ocupacao(INICIO_PERIODO, FIM_PERIODO, PROFISSIONAL_UUID);

            assertNull(r.horasDisponiveis());
            assertNull(r.taxaOcupacao());
        }

        @Test
        @DisplayName("calcula ocupacao a partir do expediente do RF05")
        void deveCalcularOcupacao() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            mockAgendamentos(List.of(
                    agendamento(1, 8, StatusAgendamento.REALIZADO),
                    agendamento(2, 8, StatusAgendamento.REALIZADO)));

            // Segunda a sexta, 8h-12h = 4h/dia. O periodo 01/06 a 07/06/2026
            // (segunda a domingo) tem 5 dias uteis = 20h de expediente.
            List<HorarioTrabalho> janelas = List.of(1, 2, 3, 4, 5).stream()
                    .map(dia -> HorarioTrabalho.builder()
                            .profissionalUuid(PROFISSIONAL_UUID)
                            .diaSemana(dia)
                            .horaInicio(LocalTime.of(8, 0))
                            .horaFim(LocalTime.of(12, 0))
                            .ativo(true)
                            .build())
                    .toList();
            when(horarioTrabalhoRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(janelas);

            RelatorioOcupacaoDTO r =
                    service.ocupacao(INICIO_PERIODO, FIM_PERIODO, PROFISSIONAL_UUID);

            assertEquals(20.0, r.horasDisponiveis());
            assertEquals(2.0, r.horasAgendadas());
            assertEquals(10.0, r.taxaOcupacao());
        }

        @Test
        @DisplayName("paciente nao acessa ocupacao")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.ocupacao(INICIO_PERIODO, FIM_PERIODO, null));
        }
    }
}

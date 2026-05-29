package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.*;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.HistoricoAgendamento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import com.prontudigital.backend.agendamento.enums.TipoVisualizacaoAgenda;
import com.prontudigital.backend.agendamento.eventos.*;
import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.agendamento.repositorios.*;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.utils.AgendamentoUtil;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgendamentoServiceImpl")
class AgendamentoServiceImplTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private BloqueioHorarioRepository bloqueioHorarioRepository;
    @Mock private HistoricoAgendamentoRepository historicoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;
    @Mock private AgendamentoUtil agendamentoUtil;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    private final Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 5, 1, 10, 0)
                    .atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private AgendamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgendamentoServiceImpl(
                agendamentoRepository, bloqueioHorarioRepository, historicoRepository,
                usuarioService, usuarioContexto, permissaoPolicy,
                agendamentoUtil, eventPublisher, clock);
    }

    private void mockSemConflitos() {
        when(bloqueioHorarioRepository.findConflitos(any(), any(), any()))
                .thenReturn(List.of());
        when(agendamentoRepository.findConflitosParaProfissionalComLock(any(), any(), any()))
                .thenReturn(List.of());
        when(agendamentoRepository.findConflitosParaPacienteComLock(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("agendar()")
    class Agendar {

        @Test
        @DisplayName("paciente cria avaliação para si mesmo com sucesso")
        void deveCriarComSucesso() {
            AgendamentoRequestDTO request = requestAvaliacao();
            Agendamento salvo = agendamentoAgendado();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(agendamentoRepository.save(any())).thenReturn(salvo);
            when(agendamentoUtil.convertToResponseDTO(salvo))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            AgendamentoResponseDTO resultado = service.agendar(request);

            assertNotNull(resultado);
            verify(agendamentoRepository).save(any(Agendamento.class));
            verify(eventPublisher).publishEvent(any(AgendamentoCriadoEvento.class));
        }

        @Test
        @DisplayName("paciente não pode criar agendamento para outro paciente")
        void deveRejeitarPacienteAgendandoParaOutro() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    OUTRO_UUID, PROFISSIONAL_UUID, null, INICIO, FIM,
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.agendar(request));

            verify(agendamentoRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("rejeita data no passado")
        void deveRejeitarDataPassada() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    PACIENTE_UUID, PROFISSIONAL_UUID, null,
                    LocalDateTime.of(2020, 1, 1, 10, 0),
                    LocalDateTime.of(2020, 1, 1, 11, 0),
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoDataHoraInvalidaException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("rejeita duração menor que 15 minutos")
        void deveRejeitarDuracaoMuitoCurta() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    PACIENTE_UUID, PROFISSIONAL_UUID, null,
                    INICIO, INICIO.plusMinutes(10),
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            AgendamentoInvalidoException ex = assertThrows(
                    AgendamentoInvalidoException.class,
                    () -> service.agendar(request));
            assertTrue(ex.getMessage().contains("Duracão minima"));
        }

        @Test
        @DisplayName("rejeita quando profissional tem bloqueio")
        void deveRejeitarBloqueio() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(bloqueioHorarioRepository.findConflitos(any(), any(), any()))
                    .thenReturn(List.of(bloqueio()));

            assertThrows(HorarioIndisponivelException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("rejeita conflito de horário do profissional")
        void deveRejeitarConflitoProfissional() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(bloqueioHorarioRepository.findConflitos(any(), any(), any()))
                    .thenReturn(List.of());
            when(agendamentoRepository.findConflitosParaProfissionalComLock(any(), any(), any()))
                    .thenReturn(List.of(agendamentoAgendado()));

            assertThrows(ProfissionalIndisponivelException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("tratamento sem avaliacaoId eh rejeitado")
        void deveRejeitarTratamentoSemAvaliacao() {
            AgendamentoRequestDTO request = requestTratamento(null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();

            assertThrows(AgendamentoInvalidoException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("tratamento valido vincula a avaliacao concluida")
        void deveVincularAvaliacaoAoTratamento() {
            AgendamentoRequestDTO request = requestTratamento(99L);
            Agendamento avaliacao = avaliacaoConcluida();
            Agendamento referencia = avaliacaoConcluida();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(agendamentoRepository.findById(99L)).thenReturn(Optional.of(avaliacao));
            when(agendamentoRepository.getReferenceById(99L)).thenReturn(referencia);
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            service.agendar(request);

            ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
            verify(agendamentoRepository).save(captor.capture());
            assertEquals(referencia, captor.getValue().getAvaliacao());
        }
    }

    @Nested
    @DisplayName("cancelar()")
    class Cancelar {

        @Test
        @DisplayName("paciente cancela proprio agendamento com sucesso")
        void deveCancelarComSucesso() {
            Agendamento agendamento = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            service.cancelar(1L);

            assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
            verify(agendamentoRepository).save(agendamento);
            verify(eventPublisher).publishEvent(any(AgendamentoCanceladoEvento.class));
        }

        @Test
        @DisplayName("rejeita cancelamento por usuario sem permissao")
        void deveRejeitarSemPermissao() {
            Agendamento agendamento = agendamentoAgendado();
            agendamento.setPacienteUuid(OUTRO_UUID);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.cancelar(1L));

            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita cancelamento se agendamento ja cancelado")
        void deveRejeitarSeJaCancelado() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.CANCELADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoJaCanceladoException.class,
                    () -> service.cancelar(1L));
        }

        @Test
        @DisplayName("agendamento inexistente lanca excecao")
        void deveLancarSeNaoEncontrado() {
            when(agendamentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class,
                    () -> service.cancelar(999L));
        }
    }

    @Nested
    @DisplayName("reagendar()")
    class Reagendar {

        @Test
        @DisplayName("reagenda com sucesso, cria historico e dispara evento")
        void deveReagendarComSucesso() {
            Agendamento agendamento = agendamentoAgendado();
            ReagendarRequestDTO request = reagendarRequest();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            service.reagendar(1L, request);

            verify(historicoRepository).save(any(HistoricoAgendamento.class));
            verify(eventPublisher).publishEvent(any(AgendamentoReagendadoEvento.class));
            assertEquals(request.novoInicioEm(), agendamento.getInicioEm());
            assertEquals(request.novoFimEm(), agendamento.getFimEm());
        }

        @Test
        @DisplayName("conflito ignora o proprio agendamento")
        void naoConflitaComOProprio() {
            Agendamento agendamento = agendamentoAgendado();
            ReagendarRequestDTO request = reagendarRequest();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(bloqueioHorarioRepository.findConflitos(any(), any(), any()))
                    .thenReturn(List.of());
            // O proprio agendamento aparece nos resultados — deve ser filtrado
            when(agendamentoRepository.findConflitosParaProfissionalComLock(any(), any(), any()))
                    .thenReturn(List.of(agendamento));
            when(agendamentoRepository.findConflitosParaPacienteComLock(any(), any(), any()))
                    .thenReturn(List.of(agendamento));
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            assertDoesNotThrow(() -> service.reagendar(1L, request));
            verify(historicoRepository).save(any());
        }

        @Test
        @DisplayName("rejeita reagendamento de agendamento concluido")
        void deveRejeitarSeConcluido() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.REALIZADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.reagendar(1L, reagendarRequest()));
        }
    }

    // =========================================================
    // concluir()
    // =========================================================
    @Nested
    @DisplayName("concluir()")
    class Concluir {

        @Test
        @DisplayName("profissional conclui agendamento com sucesso")
        void deveConcluirComSucesso() {
            Agendamento agendamento = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            service.concluir(1L);

            assertEquals(StatusAgendamento.REALIZADO, agendamento.getStatus());
            assertNotNull(agendamento.getConcluidoEm());
            verify(agendamentoRepository).save(agendamento);
        }

        @Test
        @DisplayName("rejeita conclusao de agendamento cancelado")
        void deveRejeitarSeCancelado() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.CANCELADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.concluir(1L));
        }
    }

    // =========================================================
    // RF08 — visualizarAgenda()
    // =========================================================
    @Nested
    @DisplayName("visualizarAgenda()")
    class VisualizarAgenda {

        @Test
        @DisplayName("profissional visualiza propria agenda do dia")
        void profissionalVisualizaProprioDia() {
            LocalDate data = LocalDate.of(2026, 6, 1);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(agendamentoRepository.findByProfissionalUuidAndInicioEmBetween(
                    eq(PROFISSIONAL_UUID), any(), any()))
                    .thenReturn(List.of(agendamentoAgendado()));
            when(agendamentoUtil.convertToViewDTO(any()))
                    .thenReturn(mock(AgendamentoViewDTO.class));

            List<AgendamentoViewDTO> resultado = service.visualizarAgenda(
                    data, TipoVisualizacaoAgenda.DIA, null);

            assertEquals(1, resultado.size());
            verify(agendamentoRepository).findByProfissionalUuidAndInicioEmBetween(
                    eq(PROFISSIONAL_UUID),
                    eq(data.atStartOfDay()),
                    eq(data.atTime(LocalTime.MAX)));
        }

        @Test
        @DisplayName("paciente nao pode visualizar agenda")
        void pacienteNaoPodeVisualizar() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.visualizarAgenda(
                            LocalDate.now(), TipoVisualizacaoAgenda.DIA, null));
        }

        @Test
        @DisplayName("admin precisa informar profissionalUuid")
        void adminPrecisaInformarProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());

            assertThrows(AgendamentoInvalidoException.class,
                    () -> service.visualizarAgenda(
                            LocalDate.now(), TipoVisualizacaoAgenda.DIA, null));
        }

        @Test
        @DisplayName("visualizacao SEMANA retorna periodo de segunda a domingo")
        void visualizacaoSemana() {
            // 03/06/2026 = quarta-feira → semana = 01/06 (seg) a 07/06 (dom)
            LocalDate quarta = LocalDate.of(2026, 6, 3);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(agendamentoRepository.findByProfissionalUuidAndInicioEmBetween(
                    any(), any(), any())).thenReturn(List.of());

            service.visualizarAgenda(quarta, TipoVisualizacaoAgenda.SEMANA, null);

            ArgumentCaptor<LocalDateTime> inicioCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> fimCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(agendamentoRepository).findByProfissionalUuidAndInicioEmBetween(
                    any(), inicioCaptor.capture(), fimCaptor.capture());

            assertEquals(LocalDate.of(2026, 6, 1), inicioCaptor.getValue().toLocalDate());
            assertEquals(LocalDate.of(2026, 6, 7), fimCaptor.getValue().toLocalDate());
        }
    }
}
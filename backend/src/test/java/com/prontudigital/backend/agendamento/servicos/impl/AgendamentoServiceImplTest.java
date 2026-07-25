package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.*;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoCurativo;
import com.prontudigital.backend.agendamento.entidades.EvolucaoEnfermagem;
import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.entidades.HistoricoAgendamento;
import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacaoCurativo;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
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

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgendamentoServiceImpl")
class AgendamentoServiceImplTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private EvolucaoEnfermagemRepository evolucaoEnfermagemRepository;
    @Mock private EvolucaoCurativoRepository evolucaoCurativoRepository;
    @Mock private BloqueioHorarioRepository bloqueioHorarioRepository;
    @Mock private HorarioTrabalhoRepository horarioTrabalhoRepository;
    @Mock private HistoricoAgendamentoRepository historicoRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
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
                agendamentoRepository, evolucaoEnfermagemRepository, evolucaoCurativoRepository,
                bloqueioHorarioRepository, horarioTrabalhoRepository, historicoRepository,
                procedimentoRepository, usuarioService, usuarioContexto, permissaoPolicy,
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

    private void mockProcedimentoPodiatria() {
        when(procedimentoRepository.findByCodigo(TipoProcedimento.PODIATRIA.name()))
                .thenReturn(Optional.of(procedimentoPodiatria()));
    }

    // =========================================================
    // agendar()
    // =========================================================
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
            mockProcedimentoPodiatria();
            when(agendamentoRepository.save(any())).thenReturn(salvo);
            when(agendamentoUtil.convertToResponseDTO(salvo))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            AgendamentoResponseDTO resultado = service.agendar(request);

            assertNotNull(resultado);
            verify(agendamentoRepository).save(any(Agendamento.class));
            verify(eventPublisher).publishEvent(any(AgendamentoCriadoEvento.class));
        }

        @Test
        @DisplayName("agenda com procedimentoId da tabela de procedimentos (RF06)")
        void deveCriarComProcedimentoId() {
            AgendamentoRequestDTO request = requestAvaliacaoComProcedimentoId(5L);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(procedimentoRepository.findById(5L))
                    .thenReturn(Optional.of(procedimentoNovo()));
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            service.agendar(request);

            ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
            verify(agendamentoRepository).save(captor.capture());
            assertEquals(5L, captor.getValue().getProcedimento().getId());
            // procedimento sem codigo legado nao preenche o enum
            assertNull(captor.getValue().getTipoProcedimento());
        }

        @Test
        @DisplayName("rejeita procedimento inativo")
        void deveRejeitarProcedimentoInativo() {
            AgendamentoRequestDTO request = requestAvaliacaoComProcedimentoId(5L);
            var inativo = procedimentoNovo();
            inativo.setAtivo(false);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(procedimentoRepository.findById(5L)).thenReturn(Optional.of(inativo));

            assertThrows(AgendamentoInvalidoException.class,
                    () -> service.agendar(request));
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita procedimentoId inexistente")
        void deveRejeitarProcedimentoInexistente() {
            AgendamentoRequestDTO request = requestAvaliacaoComProcedimentoId(999L);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            when(procedimentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ProcedimentoNaoEncontradoException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("rejeita agendamento sem procedimento e sem tipoProcedimento")
        void deveRejeitarSemProcedimento() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    PACIENTE_UUID, PROFISSIONAL_UUID, INICIO, FIM,
                    TipoAgendamento.AVALIACAO, null, null,
                    LocalAtendimento.CLINICA, false, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();

            assertThrows(AgendamentoInvalidoException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("paciente não pode criar agendamento para outro paciente")
        void deveRejeitarPacienteAgendandoParaOutro() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    OUTRO_UUID, PROFISSIONAL_UUID, INICIO, FIM,
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null,
                    LocalAtendimento.CLINICA, false, null);

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
                    PACIENTE_UUID, PROFISSIONAL_UUID,
                    LocalDateTime.of(2020, 1, 1, 10, 0),
                    LocalDateTime.of(2020, 1, 1, 11, 0),
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null,
                    LocalAtendimento.CLINICA, false, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoDataHoraInvalidaException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("rejeita duração menor que 15 minutos")
        void deveRejeitarDuracaoMuitoCurta() {
            AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                    PACIENTE_UUID, PROFISSIONAL_UUID,
                    INICIO, INICIO.plusMinutes(10),
                    TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null,
                    LocalAtendimento.CLINICA, false, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            AgendamentoInvalidoException ex = assertThrows(
                    AgendamentoInvalidoException.class,
                    () -> service.agendar(request));
            assertTrue(ex.getMessage().contains("Duracao minima"));
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
            mockProcedimentoPodiatria();
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

    // =========================================================
    // buscarPorId()
    // =========================================================
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("profissional visualiza proprio agendamento")
        void deveBuscarComSucesso() {
            Agendamento agendamento = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(agendamentoUtil.convertToDetalhadoDTO(agendamento))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            AgendamentoDetalhadoDTO resultado = service.buscarPorId(1L);

            assertNotNull(resultado);
            verify(agendamentoUtil).convertToDetalhadoDTO(agendamento);
        }

        @Test
        @DisplayName("usuario sem vinculo com o agendamento eh rejeitado")
        void deveRejeitarAcessoNaoAutorizado() {
            Agendamento agendamento = agendamentoAgendado();
            agendamento.setPacienteUuid(OUTRO_UUID);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.buscarPorId(1L));
        }

        @Test
        @DisplayName("agendamento inexistente lanca excecao")
        void deveLancarSeNaoEncontrado() {
            when(agendamentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class,
                    () -> service.buscarPorId(999L));
        }
    }

    // =========================================================
    // confirmar()
    // =========================================================
    @Nested
    @DisplayName("confirmar()")
    class Confirmar {

        @Test
        @DisplayName("paciente confirma proprio agendamento com sucesso")
        void deveConfirmarComSucesso() {
            Agendamento agendamento = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            service.confirmar(1L);

            assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
            verify(agendamentoRepository).save(agendamento);
        }

        @Test
        @DisplayName("rejeita confirmacao de agendamento que nao esta AGENDADO")
        void deveRejeitarSeNaoAgendado() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.CONFIRMADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.confirmar(1L));

            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita confirmacao por usuario sem permissao")
        void deveRejeitarSemPermissao() {
            Agendamento agendamento = agendamentoAgendado();
            agendamento.setPacienteUuid(OUTRO_UUID);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.confirmar(1L));
        }
    }

    // =========================================================
    // cancelar()
    // =========================================================
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
        @DisplayName("rejeita cancelamento de agendamento concluido")
        void deveRejeitarSeConcluido() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.REALIZADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.cancelar(1L));
        }

        @Test
        @DisplayName("agendamento inexistente lanca excecao")
        void deveLancarSeNaoEncontrado() {
            when(agendamentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class,
                    () -> service.cancelar(999L));
        }

        @Test
        @DisplayName("rejeita cancelamento com menos de 24h de antecedencia")
        void deveRejeitarForaDoPrazo() {
            Agendamento agendamento = agendamentoAgendado();
            // clock fixo em 2026-05-01 10:00; inicio a 10h de distancia (< 24h)
            agendamento.setInicioEm(LocalDateTime.of(2026, 5, 1, 20, 0));

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(CancelamentoForaDoPrazoException.class,
                    () -> service.cancelar(1L));

            assertEquals(StatusAgendamento.AGENDADO, agendamento.getStatus());
            verify(agendamentoRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("admin cancela mesmo com menos de 24h de antecedencia")
        void devemitirAdminForaDoPrazo() {
            Agendamento agendamento = agendamentoAgendado();
            agendamento.setInicioEm(LocalDateTime.of(2026, 5, 1, 20, 0));

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());

            service.cancelar(1L);

            assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
            verify(agendamentoRepository).save(agendamento);
            verify(eventPublisher).publishEvent(any(AgendamentoCanceladoEvento.class));
        }
    }

    // =========================================================
    // reagendar()
    // =========================================================
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

        @Test
        @DisplayName("rejeita conclusao de agendamento ja concluido")
        void deveRejeitarSeJaConcluido() {
            Agendamento agendamento = agendamentoComStatus(StatusAgendamento.REALIZADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoJaConcluidoException.class,
                    () -> service.concluir(1L));
        }
    }

    // =========================================================
    // registrarEvolucaoEnfermagem()
    // =========================================================
    @Nested
    @DisplayName("registrarEvolucaoEnfermagem()")
    class RegistrarEvolucaoEnfermagem {

        private EvolucaoEnfermagemRequestDTO requestEnfermagem() {
            return EvolucaoEnfermagemRequestDTO.builder()
                    .dataAvaliacao(LocalDate.of(2026, 5, 1))
                    .horaAvaliacao(LocalTime.of(10, 0))
                    .diagnosticoMedico("Ulcera venosa MID")
                    .comorbDiabetes(true)
                    .localizacaoAnatomica("Perna direita, terco distal")
                    .tecidoLeito(TecidoLeito.GRANULACAO)
                    .dorEscala(5)
                    .avaliacaoEvolucao(AvaliacaoEvolucao.MELHORA)
                    .planoManterConduta(true)
                    .retornoDias(7)
                    .build();
        }

        @Test
        @DisplayName("cria nova EvolucaoEnfermagem quando nao existe registro anterior")
        void deveCriarNovaEvolucao() {
            Agendamento avaliacao = agendamentoAgendado(); // tipo = AVALIACAO

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoEnfermagemRepository.findByAgendamento(avaliacao))
                    .thenReturn(Optional.empty());
            when(evolucaoEnfermagemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoEnfermagem(1L, requestEnfermagem());

            ArgumentCaptor<EvolucaoEnfermagem> captor =
                    ArgumentCaptor.forClass(EvolucaoEnfermagem.class);
            verify(evolucaoEnfermagemRepository).save(captor.capture());

            EvolucaoEnfermagem salva = captor.getValue();
            assertEquals(avaliacao, salva.getAgendamento());
            assertEquals("Ulcera venosa MID", salva.getDiagnosticoMedico());
            assertEquals(TecidoLeito.GRANULACAO, salva.getTecidoLeito());
            assertEquals(5, salva.getDorEscala());
            assertEquals(AvaliacaoEvolucao.MELHORA, salva.getAvaliacaoEvolucao());
            assertEquals(7, salva.getRetornoDias());
        }

        @Test
        @DisplayName("atualiza EvolucaoEnfermagem existente sem criar novo registro")
        void deveAtualizarEvolucaoExistente() {
            Agendamento avaliacao = agendamentoAgendado();
            EvolucaoEnfermagem existente = EvolucaoEnfermagem.builder()
                    .id(10L)
                    .agendamento(avaliacao)
                    .diagnosticoMedico("Antigo")
                    .build();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoEnfermagemRepository.findByAgendamento(avaliacao))
                    .thenReturn(Optional.of(existente));
            when(evolucaoEnfermagemRepository.save(existente)).thenReturn(existente);
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoEnfermagem(1L, requestEnfermagem());

            verify(evolucaoEnfermagemRepository).save(existente);
            assertEquals("Ulcera venosa MID", existente.getDiagnosticoMedico());
            assertEquals(TecidoLeito.GRANULACAO, existente.getTecidoLeito());
        }

        @Test
        @DisplayName("paciente nao pode registrar ficha de enfermagem")
        void deveRejeitarPaciente() {
            Agendamento avaliacao = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrarEvolucaoEnfermagem(1L, requestEnfermagem()));

            verify(evolucaoEnfermagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita ficha de enfermagem em agendamento do tipo TRATAMENTO")
        void deveRejeitarTipoTratamento() {
            Agendamento tratamento = agendamentoTratamento();

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(tratamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.registrarEvolucaoEnfermagem(2L, requestEnfermagem()));

            verify(evolucaoEnfermagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita ficha de enfermagem em agendamento cancelado")
        void deveRejeitarAgendamentoCancelado() {
            Agendamento cancelado = agendamentoAgendado();
            cancelado.setStatus(StatusAgendamento.CANCELADO);

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(cancelado));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.registrarEvolucaoEnfermagem(1L, requestEnfermagem()));

            verify(evolucaoEnfermagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("recarrega o agendamento apos salvar para retornar DTO atualizado")
        void deveRecarregarAgendamentoAposSalvar() {
            Agendamento avaliacao = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoEnfermagemRepository.findByAgendamento(avaliacao))
                    .thenReturn(Optional.empty());
            when(evolucaoEnfermagemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoEnfermagem(1L, requestEnfermagem());

            verify(agendamentoRepository, times(2)).findById(1L);
        }
    }

    // =========================================================
    // registrarEvolucaoCurativo()
    // =========================================================
    @Nested
    @DisplayName("registrarEvolucaoCurativo()")
    class RegistrarEvolucaoCurativo {

        private EvolucaoCurativoRequestDTO requestCurativo() {
            return EvolucaoCurativoRequestDTO.builder()
                    .comprimento(new BigDecimal("4.00"))
                    .largura(new BigDecimal("2.50"))
                    .areaAproximada(new BigDecimal("10.00"))
                    .tecido(TecidoLeito.GRANULACAO)
                    .infeccaoInflamacao(InfeccaoInflamacaoCurativo.AUSENTE)
                    .odorPresente(false)
                    .dorEscala(3)
                    .limpezaIrrigacao("SF 0,9% em jato")
                    .desbridamento(TipoDesbridamento.NAO)
                    .coberturaPrimaria("Hidrofibra com prata")
                    .evolucao(AvaliacaoEvolucao.MELHORA)
                    .planoManterConduta("Manter cobertura atual")
                    .retornoPrevisto("Retorno em 3 dias")
                    .build();
        }

        @Test
        @DisplayName("cria nova EvolucaoCurativo quando nao existe registro anterior")
        void deveCriarNovaEvolucao() {
            Agendamento tratamento = agendamentoTratamento();

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(tratamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoCurativoRepository.findByAgendamento(tratamento))
                    .thenReturn(Optional.empty());
            when(evolucaoCurativoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoCurativo(2L, requestCurativo());

            ArgumentCaptor<EvolucaoCurativo> captor =
                    ArgumentCaptor.forClass(EvolucaoCurativo.class);
            verify(evolucaoCurativoRepository).save(captor.capture());

            EvolucaoCurativo salva = captor.getValue();
            assertEquals(tratamento, salva.getAgendamento());
            assertEquals(new BigDecimal("10.00"), salva.getAreaAproximada());
            assertEquals(TecidoLeito.GRANULACAO, salva.getTecido());
            assertEquals(InfeccaoInflamacaoCurativo.AUSENTE, salva.getInfeccaoInflamacao());
            assertEquals(3, salva.getDorEscala());
            assertEquals(AvaliacaoEvolucao.MELHORA, salva.getEvolucao());
            assertEquals("Retorno em 3 dias", salva.getRetornoPrevisto());
        }

        @Test
        @DisplayName("atualiza EvolucaoCurativo existente sem criar novo registro")
        void deveAtualizarEvolucaoExistente() {
            Agendamento tratamento = agendamentoTratamento();
            EvolucaoCurativo existente = EvolucaoCurativo.builder()
                    .id(20L)
                    .agendamento(tratamento)
                    .coberturaPrimaria("Antiga")
                    .build();

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(tratamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoCurativoRepository.findByAgendamento(tratamento))
                    .thenReturn(Optional.of(existente));
            when(evolucaoCurativoRepository.save(existente)).thenReturn(existente);
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoCurativo(2L, requestCurativo());

            verify(evolucaoCurativoRepository).save(existente);
            assertEquals("Hidrofibra com prata", existente.getCoberturaPrimaria());
            assertEquals(TecidoLeito.GRANULACAO, existente.getTecido());
        }

        @Test
        @DisplayName("paciente nao pode registrar ficha de curativos")
        void deveRejeitarPaciente() {
            Agendamento tratamento = agendamentoTratamento();

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(tratamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrarEvolucaoCurativo(2L, requestCurativo()));

            verify(evolucaoCurativoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita ficha de curativos em agendamento do tipo AVALIACAO")
        void deveRejeitarTipoAvaliacao() {
            Agendamento avaliacao = agendamentoAgendado();

            when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.registrarEvolucaoCurativo(1L, requestCurativo()));

            verify(evolucaoCurativoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita ficha de curativos em agendamento cancelado")
        void deveRejeitarAgendamentoCancelado() {
            Agendamento cancelado = agendamentoTratamento();
            cancelado.setStatus(StatusAgendamento.CANCELADO);

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(cancelado));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoStatusInvalidoException.class,
                    () -> service.registrarEvolucaoCurativo(2L, requestCurativo()));

            verify(evolucaoCurativoRepository, never()).save(any());
        }

        @Test
        @DisplayName("recarrega o agendamento apos salvar para retornar DTO atualizado")
        void deveRecarregarAgendamentoAposSalvar() {
            Agendamento tratamento = agendamentoTratamento();

            when(agendamentoRepository.findById(2L)).thenReturn(Optional.of(tratamento));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(evolucaoCurativoRepository.findByAgendamento(tratamento))
                    .thenReturn(Optional.empty());
            when(evolucaoCurativoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(agendamentoUtil.convertToDetalhadoDTO(any()))
                    .thenReturn(mock(AgendamentoDetalhadoDTO.class));

            service.registrarEvolucaoCurativo(2L, requestCurativo());

            verify(agendamentoRepository, times(2)).findById(2L);
        }
    }

    // =========================================================
    // obterMeusAgendamentos()
    // =========================================================
    @Nested
    @DisplayName("obterMeusAgendamentos()")
    class ObterMeusAgendamentos {

        @Test
        @DisplayName("paciente obtem lista dos proprios agendamentos")
        void deveRetornarAgendamentosDoPackiente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(PACIENTE_UUID))
                    .thenReturn(List.of(agendamentoAgendado()));
            when(agendamentoUtil.convertToViewDTO(any()))
                    .thenReturn(mock(AgendamentoViewDTO.class));

            List<AgendamentoViewDTO> resultado = service.obterMeusAgendamentos();

            assertEquals(1, resultado.size());
        }

        @Test
        @DisplayName("profissional nao pode acessar endpoint de paciente")
        void deveRejeitarNaoPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.obterMeusAgendamentos());
        }
    }

    // =========================================================
    // getTratamentosPorAvaliacao()
    // =========================================================
    @Nested
    @DisplayName("getTratamentosPorAvaliacao()")
    class GetTratamentosPorAvaliacao {

        @Test
        @DisplayName("retorna avaliacao seguida dos tratamentos vinculados")
        void deveRetornarAvaliacaoETratamentos() {
            Agendamento avaliacao = avaliacaoConcluida();
            Agendamento tratamento = agendamentoTratamento();

            when(agendamentoRepository.findById(99L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(agendamentoRepository.findByAvaliacaoId(99L)).thenReturn(List.of(tratamento));
            when(agendamentoUtil.convertToViewDTO(any())).thenReturn(mock(AgendamentoViewDTO.class));

            List<AgendamentoViewDTO> resultado = service.getTratamentosPorAvaliacao(99L);

            // avaliacao + 1 tratamento = 2 itens
            assertEquals(2, resultado.size());
            verify(agendamentoUtil, times(2)).convertToViewDTO(any());
        }

        @Test
        @DisplayName("lanca excecao se avaliacao nao encontrada")
        void deveRejeitarAvaliacaoNaoEncontrada() {
            when(agendamentoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(AvaliacaoNaoEncontradaException.class,
                    () -> service.getTratamentosPorAvaliacao(99L));
        }

        @Test
        @DisplayName("rejeita acesso de usuario sem vinculo com a avaliacao")
        void deveRejeitarAcessoNaoAutorizado() {
            Agendamento avaliacao = avaliacaoConcluida();
            avaliacao.setPacienteUuid(OUTRO_UUID);
            avaliacao.setProfissionalUuid(OUTRO_UUID);

            when(agendamentoRepository.findById(99L)).thenReturn(Optional.of(avaliacao));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.getTratamentosPorAvaliacao(99L));
        }
    }

    // =========================================================
    // visualizarAgenda()
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

    // =========================================================
    // RF05 - horario de trabalho do profissional
    // =========================================================
    @Nested
    @DisplayName("validacao de horario de trabalho (RF05)")
    class HorarioDeTrabalho {

        /** A janela de expediente que cobre INICIO..FIM (01/06/2026, 14h-15h). */
        private HorarioTrabalho janela(LocalTime inicio, LocalTime fim) {
            return HorarioTrabalho.builder()
                    .profissionalUuid(PROFISSIONAL_UUID)
                    .diaSemana(INICIO.getDayOfWeek().getValue())
                    .horaInicio(inicio)
                    .horaFim(fim)
                    .ativo(true)
                    .build();
        }

        private void mockExpediente(HorarioTrabalho... janelas) {
            when(horarioTrabalhoRepository
                    .existsByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID)).thenReturn(true);
            when(horarioTrabalhoRepository
                    .findByProfissionalUuidAndDiaSemanaAndAtivoTrue(eq(PROFISSIONAL_UUID), anyInt()))
                    .thenReturn(List.of(janelas));
        }

        @Test
        @DisplayName("profissional sem expediente cadastrado nao e validado")
        void deveIgnorarQuandoSemExpediente() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockSemConflitos();
            mockProcedimentoPodiatria();
            when(agendamentoRepository.save(any())).thenReturn(agendamentoAgendado());
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            assertNotNull(service.agendar(request));

            verify(horarioTrabalhoRepository, never())
                    .findByProfissionalUuidAndDiaSemanaAndAtivoTrue(any(), anyInt());
        }

        @Test
        @DisplayName("agenda dentro da janela de expediente")
        void deveAceitarDentroDoExpediente() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockExpediente(janela(LocalTime.of(8, 0), LocalTime.of(18, 0)));
            mockSemConflitos();
            mockProcedimentoPodiatria();
            when(agendamentoRepository.save(any())).thenReturn(agendamentoAgendado());
            when(agendamentoUtil.convertToResponseDTO(any()))
                    .thenReturn(mock(AgendamentoResponseDTO.class));

            assertNotNull(service.agendar(request));
        }

        @Test
        @DisplayName("rejeita agendamento fora da janela de expediente")
        void deveRejeitarForaDoExpediente() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            mockExpediente(janela(LocalTime.of(8, 0), LocalTime.of(12, 0)));

            assertThrows(ForaDoHorarioTrabalhoException.class,
                    () -> service.agendar(request));
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita quando o atendimento so cabe somando duas janelas")
        void deveRejeitarQuandoAtravessaDuasJanelas() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            // 13h-14h30 e 14h30-16h: o atendimento de 14h-15h cruza o intervalo
            // entre as duas e nao cabe inteiro em nenhuma delas.
            mockExpediente(
                    janela(LocalTime.of(13, 0), LocalTime.of(14, 30)),
                    janela(LocalTime.of(14, 30), LocalTime.of(16, 0)));

            assertThrows(ForaDoHorarioTrabalhoException.class,
                    () -> service.agendar(request));
        }

        @Test
        @DisplayName("rejeita quando o dia da semana nao tem expediente")
        void deveRejeitarDiaSemExpediente() {
            AgendamentoRequestDTO request = requestAvaliacao();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(horarioTrabalhoRepository
                    .existsByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID)).thenReturn(true);
            when(horarioTrabalhoRepository
                    .findByProfissionalUuidAndDiaSemanaAndAtivoTrue(eq(PROFISSIONAL_UUID), anyInt()))
                    .thenReturn(List.of());

            assertThrows(ForaDoHorarioTrabalhoException.class,
                    () -> service.agendar(request));
        }
    }
}

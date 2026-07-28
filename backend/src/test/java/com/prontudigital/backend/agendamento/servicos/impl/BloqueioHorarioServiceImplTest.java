package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.dto.BloqueioRecorrenteDTO;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.entidades.BloqueioRecorrente;
import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.repositorios.BloqueioRecorrenteRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.BloqueioHorarioTestFixtures.*;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.BloqueioHorarioTestFixtures.FIM;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.BloqueioHorarioTestFixtures.INICIO;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.BloqueioHorarioTestFixtures.PROFISSIONAL_UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BloqueioHorarioServiceImpl")
class BloqueioHorarioServiceImplTest {

    @Mock private BloqueioHorarioRepository repository;
    @Mock private BloqueioRecorrenteRepository recorrenteRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private UsuarioContexto usuarioContexto;

    private final AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    private BloqueioHorarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BloqueioHorarioServiceImpl(
                repository, recorrenteRepository, agendamentoRepository, usuarioContexto, permissaoPolicy);
    }

    // =========================================================
    // criar()
    // =========================================================
    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("profissional cria bloqueio na propria agenda com sucesso")
        void deveCriarComSucesso() {
            BloqueioHorarioDTO request = requestValido();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.save(any())).thenReturn(bloqueioSalvo());

            BloqueioHorarioDTO resultado = service.criar(request);

            assertNotNull(resultado);
            assertEquals(PROFISSIONAL_UUID, resultado.profissionalUuid());
            verify(repository).save(any(BloqueioHorario.class));
        }

        @Test
        @DisplayName("admin pode criar bloqueio para qualquer profissional")
        void adminPodeCriarParaQualquer() {
            BloqueioHorarioDTO request = requestComProfissional(OUTRO_PROFISSIONAL_UUID);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(repository.save(any())).thenReturn(bloqueioSalvo());

            assertDoesNotThrow(() -> service.criar(request));
            verify(repository).save(any());
        }

        @Test
        @DisplayName("profissional nao pode bloquear agenda de outro profissional")
        void profissionalNaoPodeBloquearOutro() {
            BloqueioHorarioDTO request = requestComProfissional(OUTRO_PROFISSIONAL_UUID);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criar(request));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("paciente nao pode criar bloqueios")
        void pacienteNaoPodeCriar() {
            BloqueioHorarioDTO request = requestValido();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criar(request));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita bloqueio com fim anterior ao inicio")
        void deveRejeitarFimAntesDoInicio() {
            BloqueioHorarioDTO request = new BloqueioHorarioDTO(
                    null, null, PROFISSIONAL_UUID,
                    FIM, INICIO,
                    "Ferias", null, null);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoInvalidoException.class, () -> service.criar(request));
            verify(repository, never()).save(any());
        }
    }

    // =========================================================
    // remover()
    // =========================================================
    @Nested
    @DisplayName("remover()")
    class Remover {

        @Test
        @DisplayName("profissional remove proprio bloqueio com sucesso")
        void deveRemoverComSucesso() {
            BloqueioHorario bloqueio = bloqueioSalvo();

            when(repository.findById(1L)).thenReturn(Optional.of(bloqueio));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            service.remover(1L);

            verify(repository).delete(bloqueio);
        }

        @Test
        @DisplayName("admin pode remover qualquer bloqueio")
        void adminPodeRemoverQualquer() {
            BloqueioHorario bloqueio = bloqueioSalvo();
            bloqueio.setProfissionalUuid(OUTRO_PROFISSIONAL_UUID);

            when(repository.findById(1L)).thenReturn(Optional.of(bloqueio));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());

            assertDoesNotThrow(() -> service.remover(1L));
            verify(repository).delete(bloqueio);
        }

        @Test
        @DisplayName("profissional nao pode remover bloqueio de outro profissional")
        void profissionalNaoPodeRemoverDeOutro() {
            BloqueioHorario bloqueio = bloqueioSalvo();
            bloqueio.setProfissionalUuid(OUTRO_PROFISSIONAL_UUID);

            when(repository.findById(1L)).thenReturn(Optional.of(bloqueio));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.remover(1L));
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("bloqueio inexistente lanca excecao")
        void deveLancarSeNaoEncontrado() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class, () -> service.remover(999L));
        }
    }

    // =========================================================
    // listarPorProfissional()
    // =========================================================
    @Nested
    @DisplayName("listarPorProfissional()")
    class Listar {

        @Test
        @DisplayName("retorna bloqueios no periodo informado")
        void deveListar() {
            when(repository.findConflitos(eq(PROFISSIONAL_UUID), any(), any()))
                    .thenReturn(List.of(bloqueioSalvo(), bloqueioSalvo()));
            when(agendamentoRepository.findOcupadosPorProfissional(eq(PROFISSIONAL_UUID), any(), any()))
                    .thenReturn(List.of());

            List<BloqueioHorarioDTO> resultado = service.listarPorProfissional(
                    PROFISSIONAL_UUID,
                    java.time.LocalDate.of(2026, 6, 1),
                    java.time.LocalDate.of(2026, 6, 30));

            assertEquals(2, resultado.size());
        }

        @Test
        @DisplayName("retorna lista vazia quando nao ha bloqueios")
        void deveRetornarVaziaQuandoSemBloqueios() {
            when(repository.findConflitos(any(), any(), any())).thenReturn(List.of());
            when(agendamentoRepository.findOcupadosPorProfissional(any(), any(), any()))
                    .thenReturn(List.of());

            List<BloqueioHorarioDTO> resultado = service.listarPorProfissional(
                    PROFISSIONAL_UUID,
                    java.time.LocalDate.of(2026, 6, 1),
                    java.time.LocalDate.of(2026, 6, 30));

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("inclui agendamentos ocupados e expande a regra recorrente no dia que casa")
        void deveIncluirOcupadosEExpandirRecorrente() {
            LocalDate dia = LocalDate.of(2026, 6, 1);

            when(repository.findConflitos(eq(PROFISSIONAL_UUID), any(), any()))
                    .thenReturn(List.of(bloqueioSalvo()));
            when(agendamentoRepository.findOcupadosPorProfissional(eq(PROFISSIONAL_UUID), any(), any()))
                    .thenReturn(List.of(agendamentoAgendado()));
            // Regra no mesmo dia da semana do periodo (1 dia) => expande exatamente 1 ocorrencia.
            when(recorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regraRecorrente(dia.getDayOfWeek().getValue())));

            List<BloqueioHorarioDTO> resultado =
                    service.listarPorProfissional(PROFISSIONAL_UUID, dia, dia);

            // 1 bloqueio + 1 agendamento ocupado + 1 recorrente expandido
            assertEquals(3, resultado.size());
            assertTrue(resultado.stream().anyMatch(b -> "Horário reservado".equals(b.motivo())
                    || b.tipo() == TipoBloqueio.INDISPONIVEL));
        }

        @Test
        @DisplayName("nao expande a regra recorrente quando o dia da semana nao casa")
        void naoExpandeRecorrenteForaDoDia() {
            LocalDate dia = LocalDate.of(2026, 6, 1);
            int outroDia = (dia.getDayOfWeek().getValue() % 7) + 1; // dia da semana diferente

            when(repository.findConflitos(any(), any(), any())).thenReturn(List.of());
            when(agendamentoRepository.findOcupadosPorProfissional(any(), any(), any()))
                    .thenReturn(List.of());
            when(recorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regraRecorrente(outroDia)));

            List<BloqueioHorarioDTO> resultado =
                    service.listarPorProfissional(PROFISSIONAL_UUID, dia, dia);

            assertTrue(resultado.isEmpty());
        }
    }

    // =========================================================
    // criarRecorrente()
    // =========================================================
    @Nested
    @DisplayName("criarRecorrente()")
    class CriarRecorrente {

        @Test
        @DisplayName("profissional cria regra na propria agenda com sucesso")
        void deveCriarComSucesso() {
            BloqueioRecorrenteDTO request = requestRecorrente(PROFISSIONAL_UUID, LocalTime.of(12, 0), LocalTime.of(13, 0));

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(recorrenteRepository.save(any())).thenReturn(regraRecorrente(1));

            BloqueioRecorrenteDTO resultado = service.criarRecorrente(request);

            assertNotNull(resultado);
            assertEquals(PROFISSIONAL_UUID, resultado.profissionalUuid());
            verify(recorrenteRepository).save(any(BloqueioRecorrente.class));
        }

        @Test
        @DisplayName("admin pode criar regra para qualquer profissional")
        void adminPodeCriarParaQualquer() {
            BloqueioRecorrenteDTO request = requestRecorrente(OUTRO_PROFISSIONAL_UUID, LocalTime.of(12, 0), LocalTime.of(13, 0));

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(recorrenteRepository.save(any())).thenReturn(regraRecorrente(1));

            assertDoesNotThrow(() -> service.criarRecorrente(request));
            verify(recorrenteRepository).save(any());
        }

        @Test
        @DisplayName("profissional nao pode criar regra na agenda de outro")
        void profissionalNaoPodeCriarParaOutro() {
            BloqueioRecorrenteDTO request = requestRecorrente(OUTRO_PROFISSIONAL_UUID, LocalTime.of(12, 0), LocalTime.of(13, 0));

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criarRecorrente(request));
            verify(recorrenteRepository, never()).save(any());
        }

        @Test
        @DisplayName("paciente nao pode criar regra recorrente")
        void pacienteNaoPodeCriar() {
            BloqueioRecorrenteDTO request = requestRecorrente(PROFISSIONAL_UUID, LocalTime.of(12, 0), LocalTime.of(13, 0));

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criarRecorrente(request));
            verify(recorrenteRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita regra com hora fim anterior ou igual a hora inicio")
        void deveRejeitarHoraFimAntesDoInicio() {
            BloqueioRecorrenteDTO request = requestRecorrente(PROFISSIONAL_UUID, LocalTime.of(13, 0), LocalTime.of(12, 0));

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(AgendamentoInvalidoException.class, () -> service.criarRecorrente(request));
            verify(recorrenteRepository, never()).save(any());
        }
    }

    // =========================================================
    // removerRecorrente()
    // =========================================================
    @Nested
    @DisplayName("removerRecorrente()")
    class RemoverRecorrente {

        @Test
        @DisplayName("profissional remove propria regra com sucesso")
        void deveRemoverComSucesso() {
            BloqueioRecorrente regra = regraRecorrente(1);

            when(recorrenteRepository.findById(1L)).thenReturn(Optional.of(regra));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            service.removerRecorrente(1L);

            verify(recorrenteRepository).delete(regra);
        }

        @Test
        @DisplayName("admin pode remover qualquer regra")
        void adminPodeRemoverQualquer() {
            BloqueioRecorrente regra = regraRecorrente(1);
            regra.setProfissionalUuid(OUTRO_PROFISSIONAL_UUID);

            when(recorrenteRepository.findById(1L)).thenReturn(Optional.of(regra));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());

            assertDoesNotThrow(() -> service.removerRecorrente(1L));
            verify(recorrenteRepository).delete(regra);
        }

        @Test
        @DisplayName("profissional nao pode remover regra de outro profissional")
        void profissionalNaoPodeRemoverDeOutro() {
            BloqueioRecorrente regra = regraRecorrente(1);
            regra.setProfissionalUuid(OUTRO_PROFISSIONAL_UUID);

            when(recorrenteRepository.findById(1L)).thenReturn(Optional.of(regra));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.removerRecorrente(1L));
            verify(recorrenteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("regra inexistente lanca excecao")
        void deveLancarSeNaoEncontrada() {
            when(recorrenteRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class, () -> service.removerRecorrente(999L));
        }
    }

    // =========================================================
    // listarRecorrentesPorProfissional()
    // =========================================================
    @Nested
    @DisplayName("listarRecorrentesPorProfissional()")
    class ListarRecorrentes {

        @Test
        @DisplayName("retorna as regras ativas convertidas para DTO")
        void deveListarRecorrentes() {
            when(recorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regraRecorrente(1), regraRecorrente(3)));

            List<BloqueioRecorrenteDTO> resultado =
                    service.listarRecorrentesPorProfissional(PROFISSIONAL_UUID);

            assertEquals(2, resultado.size());
            assertEquals(PROFISSIONAL_UUID, resultado.get(0).profissionalUuid());
        }
    }

    // ── fixtures de recorrentes ───────────────────────────────
    private static BloqueioRecorrenteDTO requestRecorrente(UUID profissionalUuid,
                                                           LocalTime horaInicio, LocalTime horaFim) {
        return new BloqueioRecorrenteDTO(
                null, null, profissionalUuid, 1, horaInicio, horaFim, "Almoco", TipoBloqueio.INDISPONIVEL);
    }

    private static BloqueioRecorrente regraRecorrente(int diaSemana) {
        return BloqueioRecorrente.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .profissionalUuid(PROFISSIONAL_UUID)
                .diaSemana(diaSemana)
                .horaInicio(LocalTime.of(12, 0))
                .horaFim(LocalTime.of(13, 0))
                .motivo("Almoco")
                .tipo(TipoBloqueio.INDISPONIVEL)
                .ativo(true)
                .build();
    }
}
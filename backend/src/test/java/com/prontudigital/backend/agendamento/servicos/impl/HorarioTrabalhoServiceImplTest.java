package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.HorarioTrabalhoDTO;
import com.prontudigital.backend.agendamento.entidades.BloqueioRecorrente;
import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.BloqueioRecorrenteRepository;
import com.prontudigital.backend.agendamento.repositorios.HorarioTrabalhoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HorarioTrabalhoServiceImpl (RF05)")
class HorarioTrabalhoServiceImplTest {

    private static final int SEGUNDA = 1;

    @Mock private HorarioTrabalhoRepository repository;
    @Mock private BloqueioRecorrenteRepository bloqueioRecorrenteRepository;
    @Mock private UsuarioContexto usuarioContexto;

    private final AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    private HorarioTrabalhoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HorarioTrabalhoServiceImpl(
                repository, bloqueioRecorrenteRepository, usuarioContexto, permissaoPolicy);
    }

    private HorarioTrabalhoDTO request(LocalTime inicio, LocalTime fim) {
        return new HorarioTrabalhoDTO(null, null, PROFISSIONAL_UUID, SEGUNDA, inicio, fim, null);
    }

    private BloqueioRecorrente regra(int diaSemana, LocalTime inicio, LocalTime fim) {
        return BloqueioRecorrente.builder()
                .id(1L)
                .profissionalUuid(PROFISSIONAL_UUID)
                .diaSemana(diaSemana)
                .horaInicio(inicio)
                .horaFim(fim)
                .tipo(TipoBloqueio.FOLGA)
                .ativo(true)
                .build();
    }

    private HorarioTrabalho entidade(LocalTime inicio, LocalTime fim) {
        return HorarioTrabalho.builder()
                .id(1L)
                .profissionalUuid(PROFISSIONAL_UUID)
                .diaSemana(SEGUNDA)
                .horaInicio(inicio)
                .horaFim(fim)
                .ativo(true)
                .build();
    }

    // =========================================================
    // criar()
    // =========================================================
    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("profissional cadastra janela na propria agenda")
        void deveCriarComSucesso() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt())).thenReturn(List.of());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            HorarioTrabalhoDTO resultado =
                    service.criar(request(LocalTime.of(8, 0), LocalTime.of(12, 0)));

            assertEquals(SEGUNDA, resultado.diaSemana());
            assertEquals(LocalTime.of(8, 0), resultado.horaInicio());
            assertTrue(resultado.ativo());
            verify(repository).save(any(HorarioTrabalho.class));
        }

        @Test
        @DisplayName("admin cadastra janela na agenda de qualquer profissional")
        void deveCriarComoAdmin() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt())).thenReturn(List.of());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertNotNull(service.criar(request(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        }

        @Test
        @DisplayName("rejeita profissional mexendo na agenda alheia")
        void deveRejeitarAgendaAlheia() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            HorarioTrabalhoDTO outro = new HorarioTrabalhoDTO(
                    null, null, UUID.randomUUID(), SEGUNDA,
                    LocalTime.of(8, 0), LocalTime.of(12, 0), null);

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criar(outro));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita paciente")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            HorarioTrabalhoDTO dto = request(LocalTime.of(8, 0), LocalTime.of(12, 0));

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.criar(dto));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita fim anterior ao inicio")
        void deveRejeitarIntervaloInvertido() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            HorarioTrabalhoDTO dto = request(LocalTime.of(12, 0), LocalTime.of(8, 0));

            assertThrows(AgendamentoInvalidoException.class, () -> service.criar(dto));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita janela sobreposta no mesmo dia")
        void deveRejeitarSobreposicao() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt()))
                    .thenReturn(List.of(entidade(LocalTime.of(8, 0), LocalTime.of(12, 0))));

            HorarioTrabalhoDTO dto = request(LocalTime.of(11, 0), LocalTime.of(15, 0));

            assertThrows(AgendamentoInvalidoException.class, () -> service.criar(dto));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("aceita janelas encostadas sem sobreposicao")
        void deveAceitarJanelasAdjacentes() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt()))
                    .thenReturn(List.of(entidade(LocalTime.of(8, 0), LocalTime.of(12, 0))));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertNotNull(service.criar(request(LocalTime.of(12, 0), LocalTime.of(18, 0))));
        }

        @Test
        @DisplayName("rejeita janela integralmente coberta por bloqueio recorrente")
        void deveRejeitarJanelaAnuladaPorBloqueio() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt())).thenReturn(List.of());
            when(bloqueioRecorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regra(SEGUNDA, LocalTime.of(8, 0), LocalTime.of(18, 0))));

            HorarioTrabalhoDTO dto = request(LocalTime.of(8, 0), LocalTime.of(18, 0));

            assertThrows(AgendamentoInvalidoException.class, () -> service.criar(dto));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("aceita janela apenas parcialmente coberta (intervalo de almoco)")
        void deveAceitarCoberturaParcial() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt())).thenReturn(List.of());
            when(bloqueioRecorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regra(SEGUNDA, LocalTime.of(12, 0), LocalTime.of(13, 0))));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertNotNull(service.criar(request(LocalTime.of(8, 0), LocalTime.of(18, 0))));
        }

        @Test
        @DisplayName("ignora bloqueio recorrente de outro dia da semana")
        void deveIgnorarBloqueioDeOutroDia() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());
            when(repository.findByProfissionalUuidAndDiaSemanaAndAtivoTrue(
                    eq(PROFISSIONAL_UUID), anyInt())).thenReturn(List.of());
            when(bloqueioRecorrenteRepository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(regra(SEGUNDA + 1, LocalTime.of(8, 0), LocalTime.of(18, 0))));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertNotNull(service.criar(request(LocalTime.of(8, 0), LocalTime.of(18, 0))));
        }
    }

    // =========================================================
    // listarPorProfissional()
    // =========================================================
    @Nested
    @DisplayName("listarPorProfissional()")
    class Listar {

        @Test
        @DisplayName("ordena por dia da semana e hora de inicio")
        void deveOrdenar() {
            HorarioTrabalho terca = HorarioTrabalho.builder()
                    .profissionalUuid(PROFISSIONAL_UUID).diaSemana(2)
                    .horaInicio(LocalTime.of(8, 0)).horaFim(LocalTime.of(12, 0))
                    .ativo(true).build();
            HorarioTrabalho segundaTarde = entidade(LocalTime.of(14, 0), LocalTime.of(18, 0));
            HorarioTrabalho segundaManha = entidade(LocalTime.of(8, 0), LocalTime.of(12, 0));

            when(repository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of(terca, segundaTarde, segundaManha));

            List<HorarioTrabalhoDTO> resultado = service.listarPorProfissional(PROFISSIONAL_UUID);

            assertEquals(3, resultado.size());
            assertEquals(SEGUNDA, resultado.get(0).diaSemana());
            assertEquals(LocalTime.of(8, 0), resultado.get(0).horaInicio());
            assertEquals(LocalTime.of(14, 0), resultado.get(1).horaInicio());
            assertEquals(2, resultado.get(2).diaSemana());
        }

        @Test
        @DisplayName("nao exige autenticacao — serve a tela publica de agendamento")
        void deveListarSemUsuarioAtual() {
            when(repository.findByProfissionalUuidAndAtivoTrue(PROFISSIONAL_UUID))
                    .thenReturn(List.of());

            assertTrue(service.listarPorProfissional(PROFISSIONAL_UUID).isEmpty());
            verifyNoInteractions(usuarioContexto);
        }
    }

    // =========================================================
    // remover()
    // =========================================================
    @Nested
    @DisplayName("remover()")
    class Remover {

        @Test
        @DisplayName("remove janela da propria agenda")
        void deveRemover() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(entidade(LocalTime.of(8, 0), LocalTime.of(12, 0))));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            service.remover(1L);

            verify(repository).delete(any(HorarioTrabalho.class));
        }

        @Test
        @DisplayName("lanca excecao quando a janela nao existe")
        void deveLancarQuandoInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class, () -> service.remover(99L));
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("rejeita remocao em agenda alheia")
        void deveRejeitarAgendaAlheia() {
            HorarioTrabalho deOutro = entidade(LocalTime.of(8, 0), LocalTime.of(12, 0));
            deOutro.setProfissionalUuid(UUID.randomUUID());

            when(repository.findById(1L)).thenReturn(Optional.of(deOutro));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.remover(1L));
            verify(repository, never()).delete(any());
        }
    }
}

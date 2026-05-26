package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.BloqueioHorarioRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.agendamento.servicos.impl.fixtures.BloqueioHorarioTestFixtures;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    @Mock private UsuarioContexto usuarioContexto;

    // Policy real — queremos testar a logica de autorizacao de fato
    @Spy
    private AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    @InjectMocks
    private BloqueioHorarioServiceImpl service;

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
                    "Ferias", null);

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

            List<BloqueioHorarioDTO> resultado = service.listarPorProfissional(
                    PROFISSIONAL_UUID,
                    java.time.LocalDate.of(2026, 6, 1),
                    java.time.LocalDate.of(2026, 6, 30));

            assertTrue(resultado.isEmpty());
        }
    }
}
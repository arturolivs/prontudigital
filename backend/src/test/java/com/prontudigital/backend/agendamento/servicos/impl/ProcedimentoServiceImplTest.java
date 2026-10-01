package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.ProcedimentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.ProcedimentoResponseDTO;
import com.prontudigital.backend.agendamento.entidades.Procedimento;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoEmUsoException;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoJaExisteException;
import com.prontudigital.backend.agendamento.excecoes.ProcedimentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.ProcedimentoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcedimentoServiceImpl")
class ProcedimentoServiceImplTest {

    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private UsuarioContexto usuarioContexto;

    private final AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    private ProcedimentoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcedimentoServiceImpl(
                procedimentoRepository, agendamentoRepository, usuarioContexto, permissaoPolicy);
    }

    private ProcedimentoRequestDTO request(String nome) {
        return new ProcedimentoRequestDTO(nome, "Descricao", 45, null);
    }

    // =========================================================
    // listar() / buscarPorId()
    // =========================================================
    @Nested
    @DisplayName("listar() / buscarPorId()")
    class Listagem {

        @Test
        @DisplayName("lista apenas ativos por padrao")
        void deveListarAtivos() {
            when(procedimentoRepository.findByAtivoTrueOrderByNomeAsc())
                    .thenReturn(List.of(procedimentoPodiatria()));

            List<ProcedimentoResponseDTO> resultado = service.listar(false);

            assertEquals(1, resultado.size());
            assertEquals("Podiatria", resultado.get(0).nome());
            verify(procedimentoRepository, never()).findAllByOrderByNomeAsc();
        }

        @Test
        @DisplayName("lista todos quando incluirInativos=true")
        void deveListarTodos() {
            when(procedimentoRepository.findAllByOrderByNomeAsc())
                    .thenReturn(List.of(procedimentoPodiatria(), procedimentoNovo()));

            List<ProcedimentoResponseDTO> resultado = service.listar(true);

            assertEquals(2, resultado.size());
            verify(procedimentoRepository, never()).findByAtivoTrueOrderByNomeAsc();
        }

        @Test
        @DisplayName("buscarPorId retorna procedimento existente")
        void deveBuscarPorId() {
            when(procedimentoRepository.findById(1L))
                    .thenReturn(Optional.of(procedimentoPodiatria()));

            ProcedimentoResponseDTO resultado = service.buscarPorId(1L);

            assertEquals("PODIATRIA", resultado.codigo());
            assertEquals("Podiatria", resultado.nome());
        }

        @Test
        @DisplayName("buscarPorId lanca excecao se nao encontrado")
        void deveLancarSeNaoEncontrado() {
            when(procedimentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ProcedimentoNaoEncontradoException.class,
                    () -> service.buscarPorId(999L));
        }
    }

    // =========================================================
    // criar()
    // =========================================================
    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("admin cria procedimento com sucesso (ativo por padrao)")
        void deveCriarComSucesso() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.existsByNomeIgnoreCase("Laserterapia")).thenReturn(false);
            when(procedimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProcedimentoResponseDTO resultado = service.criar(request("Laserterapia"));

            assertEquals("Laserterapia", resultado.nome());
            assertTrue(resultado.ativo());
            assertNull(resultado.codigo());
        }

        @Test
        @DisplayName("normaliza o nome removendo espacos nas bordas")
        void deveNormalizarNome() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.existsByNomeIgnoreCase("Laserterapia")).thenReturn(false);
            when(procedimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.criar(request("  Laserterapia  "));

            ArgumentCaptor<Procedimento> captor = ArgumentCaptor.forClass(Procedimento.class);
            verify(procedimentoRepository).save(captor.capture());
            assertEquals("Laserterapia", captor.getValue().getNome());
        }

        @Test
        @DisplayName("rejeita nome duplicado")
        void deveRejeitarNomeDuplicado() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.existsByNomeIgnoreCase("Podiatria")).thenReturn(true);

            assertThrows(ProcedimentoJaExisteException.class,
                    () -> service.criar(request("Podiatria")));

            verify(procedimentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("profissional nao pode criar procedimento")
        void deveRejeitarProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.criar(request("Laserterapia")));

            verify(procedimentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("paciente nao pode criar procedimento")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.criar(request("Laserterapia")));
        }
    }

    // =========================================================
    // atualizar()
    // =========================================================
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("admin atualiza nome, descricao, duracao e ativo")
        void deveAtualizarComSucesso() {
            Procedimento existente = procedimentoNovo();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(5L)).thenReturn(Optional.of(existente));
            when(procedimentoRepository.existsByNomeIgnoreCaseAndIdNot("Laser", 5L))
                    .thenReturn(false);
            when(procedimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProcedimentoResponseDTO resultado = service.atualizar(
                    5L, new ProcedimentoRequestDTO("Laser", "Nova descricao", 30, false));

            assertEquals("Laser", resultado.nome());
            assertEquals("Nova descricao", resultado.descricao());
            assertEquals(30, resultado.duracaoPadraoMinutos());
            assertFalse(resultado.ativo());
        }

        @Test
        @DisplayName("mantem ativo quando request nao informa o campo")
        void deveManterAtivoSeNaoInformado() {
            Procedimento existente = procedimentoNovo();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(5L)).thenReturn(Optional.of(existente));
            when(procedimentoRepository.existsByNomeIgnoreCaseAndIdNot(any(), any()))
                    .thenReturn(false);
            when(procedimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProcedimentoResponseDTO resultado = service.atualizar(5L, request("Laser"));

            assertTrue(resultado.ativo());
        }

        @Test
        @DisplayName("rejeita nome ja usado por outro procedimento")
        void deveRejeitarNomeDuplicado() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(5L))
                    .thenReturn(Optional.of(procedimentoNovo()));
            when(procedimentoRepository.existsByNomeIgnoreCaseAndIdNot("Podiatria", 5L))
                    .thenReturn(true);

            assertThrows(ProcedimentoJaExisteException.class,
                    () -> service.atualizar(5L, request("Podiatria")));

            verify(procedimentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanca excecao se procedimento nao encontrado")
        void deveLancarSeNaoEncontrado() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ProcedimentoNaoEncontradoException.class,
                    () -> service.atualizar(999L, request("Laser")));
        }

        @Test
        @DisplayName("profissional nao pode atualizar procedimento")
        void deveRejeitarProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.atualizar(5L, request("Laser")));
        }
    }

    // =========================================================
    // excluir()
    // =========================================================
    @Nested
    @DisplayName("excluir()")
    class Excluir {

        @Test
        @DisplayName("admin exclui procedimento sem vinculos")
        void deveExcluirComSucesso() {
            Procedimento existente = procedimentoNovo();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(5L)).thenReturn(Optional.of(existente));
            when(agendamentoRepository.existsByProcedimentoId(5L)).thenReturn(false);

            service.excluir(5L);

            verify(procedimentoRepository).delete(existente);
        }

        @Test
        @DisplayName("rejeita exclusao de procedimento vinculado a agendamentos")
        void deveRejeitarSeEmUso() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(1L))
                    .thenReturn(Optional.of(procedimentoPodiatria()));
            when(agendamentoRepository.existsByProcedimentoId(1L)).thenReturn(true);

            assertThrows(ProcedimentoEmUsoException.class,
                    () -> service.excluir(1L));

            verify(procedimentoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("lanca excecao se procedimento nao encontrado")
        void deveLancarSeNaoEncontrado() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(procedimentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ProcedimentoNaoEncontradoException.class,
                    () -> service.excluir(999L));
        }

        @Test
        @DisplayName("profissional nao pode excluir procedimento")
        void deveRejeitarProfissional() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioProfissional());

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.excluir(5L));

            verify(procedimentoRepository, never()).delete(any());
        }
    }
}

package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.prontuario.dto.PrescricaoRequestDTO;
import com.prontudigital.backend.prontuario.dto.PrescricaoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Prescricao;
import com.prontudigital.backend.prontuario.enums.TipoPrescricao;
import com.prontudigital.backend.prontuario.excecoes.PrescricaoNaoEncontradaException;
import com.prontudigital.backend.prontuario.repositorios.PrescricaoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescricaoServiceImpl")
class PrescricaoServiceImplTest {

    @Mock private PrescricaoRepository prescricaoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;
    @Mock private AgendamentoRepository agendamentoRepository;

    private ProntuarioPermissaoPolicy permissaoPolicy;
    private PrescricaoServiceImpl service;

    private static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OUTRO_UUID        = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PRESCRICAO_UUID   = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        permissaoPolicy = new ProntuarioPermissaoPolicy(agendamentoRepository);
        service = new PrescricaoServiceImpl(
                prescricaoRepository, usuarioService, usuarioContexto, permissaoPolicy);
    }

    private UsuarioDTO usuario(UUID uuid, String perfil) {
        return UsuarioDTO.builder().uuid(uuid).perfis(Set.of(perfil)).build();
    }

    private PrescricaoRequestDTO request() {
        return new PrescricaoRequestDTO(
                TipoPrescricao.MEDICAMENTO,
                "Sulfadiazina de prata 1%",
                "Aplicar camada fina sobre a ferida",
                "A cada 12 horas",
                "7 dias",
                "Trocar o curativo apos a aplicacao",
                null);
    }

    private Prescricao prescricaoExistente() {
        return Prescricao.builder()
                .id(1L)
                .pacienteUuid(PACIENTE_UUID)
                .tipo(TipoPrescricao.CUIDADO)
                .descricao("Antiga")
                .build();
    }

    private void mockPacienteNome() {
        when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                .thenReturn(UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Carlos").build());
    }

    // =========================================================
    // registrar()
    // =========================================================
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("profissional vinculado registra prescricao com sucesso")
        void deveRegistrar() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
            mockPacienteNome();

            PrescricaoResponseDTO resp = service.registrar(PACIENTE_UUID, request());

            assertEquals("Sulfadiazina de prata 1%", resp.descricao());
            assertEquals(TipoPrescricao.MEDICAMENTO, resp.tipo());
            assertEquals("Carlos", resp.pacienteNome());

            ArgumentCaptor<Prescricao> captor = ArgumentCaptor.forClass(Prescricao.class);
            verify(prescricaoRepository).save(captor.capture());
            assertEquals(PROFISSIONAL_UUID, captor.getValue().getRegistradoPor());
            assertEquals("7 dias", captor.getValue().getDuracao());
        }

        @Test
        @DisplayName("rejeita profissional sem vinculo com o paciente (RN03)")
        void deveRejeitarSemVinculo() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrar(PACIENTE_UUID, request()));
            verify(prescricaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita paciente tentando registrar prescricao")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PACIENTE_UUID, "PACIENTE"));

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrar(PACIENTE_UUID, request()));
            verify(prescricaoRepository, never()).save(any());
        }
    }

    // =========================================================
    // listarPorPaciente()
    // =========================================================
    @Nested
    @DisplayName("listarPorPaciente()")
    class Listar {

        @Test
        @DisplayName("paciente lista as proprias prescricoes")
        void pacienteVeProprias() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PACIENTE_UUID, "PACIENTE"));
            when(prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                    .thenReturn(List.of(prescricaoExistente()));
            mockPacienteNome();

            List<PrescricaoResponseDTO> resp = service.listarPorPaciente(PACIENTE_UUID);

            assertEquals(1, resp.size());
            assertEquals(PACIENTE_UUID, resp.get(0).pacienteUuid());
        }

        @Test
        @DisplayName("paciente nao lista prescricoes de outro paciente")
        void pacienteNaoVeDeOutro() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "PACIENTE"));

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.listarPorPaciente(PACIENTE_UUID));
            verify(prescricaoRepository, never()).findByPacienteUuidOrderByCriadoEmDesc(any());
        }
    }

    // =========================================================
    // atualizar()
    // =========================================================
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("admin atualiza prescricao existente")
        void deveAtualizar() {
            Prescricao existente = prescricaoExistente();
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(prescricaoRepository.findByUuidAndPacienteUuid(PRESCRICAO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(existente));
            when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
            mockPacienteNome();

            PrescricaoResponseDTO resp = service.atualizar(PACIENTE_UUID, PRESCRICAO_UUID, request());

            assertEquals("Sulfadiazina de prata 1%", resp.descricao());
            assertEquals(TipoPrescricao.MEDICAMENTO, existente.getTipo());
            assertEquals(OUTRO_UUID, existente.getRegistradoPor());
        }

        @Test
        @DisplayName("rejeita atualizacao quando prescricao inexistente")
        void deveRejeitarInexistente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(prescricaoRepository.findByUuidAndPacienteUuid(PRESCRICAO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(PrescricaoNaoEncontradaException.class,
                    () -> service.atualizar(PACIENTE_UUID, PRESCRICAO_UUID, request()));
            verify(prescricaoRepository, never()).save(any());
        }
    }

    // =========================================================
    // excluir()
    // =========================================================
    @Nested
    @DisplayName("excluir()")
    class Excluir {

        @Test
        @DisplayName("profissional vinculado exclui prescricao")
        void deveExcluir() {
            Prescricao existente = prescricaoExistente();
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(prescricaoRepository.findByUuidAndPacienteUuid(PRESCRICAO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(existente));

            service.excluir(PACIENTE_UUID, PRESCRICAO_UUID);

            verify(prescricaoRepository).delete(existente);
        }

        @Test
        @DisplayName("rejeita exclusao quando prescricao inexistente")
        void deveRejeitarInexistente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(prescricaoRepository.findByUuidAndPacienteUuid(PRESCRICAO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(PrescricaoNaoEncontradaException.class,
                    () -> service.excluir(PACIENTE_UUID, PRESCRICAO_UUID));
            verify(prescricaoRepository, never()).delete(any());
        }
    }
}

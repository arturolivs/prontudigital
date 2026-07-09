package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.prontuario.dto.AnamneseRequestDTO;
import com.prontudigital.backend.prontuario.dto.AnamneseResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Anamnese;
import com.prontudigital.backend.prontuario.excecoes.AnamneseJaExisteException;
import com.prontudigital.backend.prontuario.excecoes.AnamneseNaoEncontradaException;
import com.prontudigital.backend.prontuario.repositorios.AnamneseRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnamneseServiceImpl")
class AnamneseServiceImplTest {

    @Mock private AnamneseRepository anamneseRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;
    @Mock private AgendamentoRepository agendamentoRepository;

    private ProntuarioPermissaoPolicy permissaoPolicy;
    private AnamneseServiceImpl service;

    private static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OUTRO_UUID        = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        permissaoPolicy = new ProntuarioPermissaoPolicy(agendamentoRepository);
        service = new AnamneseServiceImpl(
                anamneseRepository, usuarioService, usuarioContexto, permissaoPolicy);
    }

    private UsuarioDTO usuario(UUID uuid, String perfil) {
        return UsuarioDTO.builder().uuid(uuid).perfis(Set.of(perfil)).build();
    }

    private AnamneseRequestDTO request() {
        return new AnamneseRequestDTO(
                "Dor em ferida", "Ha 3 meses", "Diabetes",
                "Penicilina", "Losartana", "Pai hipertenso", "Tabagista", "Colaborativo");
    }

    private Anamnese anamneseExistente() {
        return Anamnese.builder()
                .id(1L)
                .pacienteUuid(PACIENTE_UUID)
                .queixaPrincipal("Antiga")
                .build();
    }

    // =========================================================
    // registrar()
    // =========================================================
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("profissional vinculado registra anamnese com sucesso")
        void deveRegistrar() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(anamneseRepository.existsByPacienteUuid(PACIENTE_UUID)).thenReturn(false);
            when(anamneseRepository.save(any(Anamnese.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                    .thenReturn(UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Carlos").build());

            AnamneseResponseDTO resp = service.registrar(PACIENTE_UUID, request());

            assertEquals("Dor em ferida", resp.queixaPrincipal());
            assertEquals("Carlos", resp.pacienteNome());

            ArgumentCaptor<Anamnese> captor = ArgumentCaptor.forClass(Anamnese.class);
            verify(anamneseRepository).save(captor.capture());
            assertEquals(PROFISSIONAL_UUID, captor.getValue().getRegistradoPor());
            assertEquals("Penicilina", captor.getValue().getAlergias());
        }

        @Test
        @DisplayName("rejeita quando paciente ja possui anamnese")
        void deveRejeitarDuplicada() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(anamneseRepository.existsByPacienteUuid(PACIENTE_UUID)).thenReturn(true);

            assertThrows(AnamneseJaExisteException.class,
                    () -> service.registrar(PACIENTE_UUID, request()));
            verify(anamneseRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita profissional sem vinculo com o paciente (RN03)")
        void deveRejeitarSemVinculo() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrar(PACIENTE_UUID, request()));
            verify(anamneseRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita paciente tentando registrar anamnese")
        void deveRejeitarPaciente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PACIENTE_UUID, "PACIENTE"));

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.registrar(PACIENTE_UUID, request()));
            verify(anamneseRepository, never()).save(any());
        }
    }

    // =========================================================
    // buscarPorPaciente()
    // =========================================================
    @Nested
    @DisplayName("buscarPorPaciente()")
    class Buscar {

        @Test
        @DisplayName("paciente visualiza a propria anamnese")
        void pacienteVeProprio() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PACIENTE_UUID, "PACIENTE"));
            when(anamneseRepository.findByPacienteUuid(PACIENTE_UUID))
                    .thenReturn(Optional.of(anamneseExistente()));
            when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                    .thenReturn(UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Carlos").build());

            AnamneseResponseDTO resp = service.buscarPorPaciente(PACIENTE_UUID);

            assertEquals(PACIENTE_UUID, resp.pacienteUuid());
        }

        @Test
        @DisplayName("paciente nao acessa anamnese de outro paciente")
        void pacienteNaoVeDeOutro() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "PACIENTE"));

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.buscarPorPaciente(PACIENTE_UUID));
            verify(anamneseRepository, never()).findByPacienteUuid(any());
        }

        @Test
        @DisplayName("admin recebe 404 quando anamnese inexistente")
        void adminSemAnamnese() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(anamneseRepository.findByPacienteUuid(PACIENTE_UUID)).thenReturn(Optional.empty());

            assertThrows(AnamneseNaoEncontradaException.class,
                    () -> service.buscarPorPaciente(PACIENTE_UUID));
        }
    }

    // =========================================================
    // atualizar()
    // =========================================================
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("admin atualiza anamnese existente")
        void deveAtualizar() {
            Anamnese existente = anamneseExistente();
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(anamneseRepository.findByPacienteUuid(PACIENTE_UUID)).thenReturn(Optional.of(existente));
            when(anamneseRepository.save(any(Anamnese.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                    .thenReturn(UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Carlos").build());

            AnamneseResponseDTO resp = service.atualizar(PACIENTE_UUID, request());

            assertEquals("Dor em ferida", resp.queixaPrincipal());
            assertEquals(OUTRO_UUID, existente.getRegistradoPor());
        }

        @Test
        @DisplayName("rejeita atualizacao quando anamnese inexistente")
        void deveRejeitarInexistente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(anamneseRepository.findByPacienteUuid(PACIENTE_UUID)).thenReturn(Optional.empty());

            assertThrows(AnamneseNaoEncontradaException.class,
                    () -> service.atualizar(PACIENTE_UUID, request()));
            verify(anamneseRepository, never()).save(any());
        }
    }
}

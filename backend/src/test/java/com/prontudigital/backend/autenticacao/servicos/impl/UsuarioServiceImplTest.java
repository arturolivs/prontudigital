package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.*;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.prontudigital.backend.autenticacao.servicos.impl.fixtures.AutenticacaoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl")
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl service;

    // =========================================================
    // listarTodos()
    // =========================================================
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("retorna todos os usuarios convertidos para DTO")
        void deveListarTodos() {
            when(usuarioRepository.findAll())
                    .thenReturn(List.of(usuarioComPerfil(), usuarioComPerfil()));

            List<UsuarioDTO> resultado = service.listarTodos();

            assertEquals(2, resultado.size());
        }

        @Test
        @DisplayName("retorna lista vazia quando nao ha usuarios")
        void deveRetornarVaziaQuandoSemUsuarios() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            assertTrue(service.listarTodos().isEmpty());
        }
    }

    // =========================================================
    // buscarPorId()
    // =========================================================
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("retorna usuario quando encontrado")
        void deveRetornarUsuario() {
            when(usuarioRepository.findById(USUARIO_ID))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.buscarPorId(USUARIO_ID);

            assertEquals(USERNAME, resultado.username());
        }

        @Test
        @DisplayName("lanca excecao quando nao encontrado")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.buscarPorId(999L));
        }
    }

    // =========================================================
    // buscarPorUuid()
    // =========================================================
    @Nested
    @DisplayName("buscarPorUuid()")
    class BuscarPorUuid {

        @Test
        @DisplayName("retorna usuario quando encontrado")
        void deveRetornarUsuario() {
            when(usuarioRepository.findByUuid(USUARIO_UUID))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.buscarPorUuid(USUARIO_UUID);

            assertEquals(USUARIO_UUID, resultado.uuid());
        }

        @Test
        @DisplayName("lanca excecao quando nao encontrado")
        void deveLancarQuandoInexistente() {
            UUID uuid = UUID.randomUUID();
            when(usuarioRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.buscarPorUuid(uuid));
        }
    }

    // =========================================================
    // criar()
    // =========================================================
    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("cria usuario com sucesso e codifica senha")
        void deveCriarComSucesso() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(SENHA_RAW)).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenAnswer(inv -> {
                        Usuario u = inv.getArgument(0);
                        u.setId(USUARIO_ID);
                        u.setUuid(USUARIO_UUID);
                        return u;
                    });

            UsuarioDTO resultado = service.criar(dto, SENHA_RAW);

            assertEquals(USERNAME, resultado.username());
            verify(passwordEncoder).encode(SENHA_RAW);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertEquals(SENHA_HASH, captor.getValue().getPasswordHash());
        }

        @Test
        @DisplayName("usa ativo=true como default quando dto.ativo eh null")
        void deveUsarAtivoTruePorDefault() {
            UsuarioDTO dto = usuarioDTOSemAtivo();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome(any()))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.criar(dto, SENHA_RAW);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertTrue(captor.getValue().getAtivo());
        }

        @Test
        @DisplayName("nao chama atualizarPerfis quando perfis eh null")
        void naoAtualizaPerfisQuandoNull() {
            UsuarioDTO dto = UsuarioDTO.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(null)
                    .build();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.criar(dto, SENHA_RAW);

            verify(perfilRepository, never()).findByNome(any());
        }

        @Test
        @DisplayName("rejeita criacao quando username ja existe")
        void deveRejeitarUsernameDuplicado() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(true);

            assertThrows(UserNameExistenteException.class,
                    () -> service.criar(dto, SENHA_RAW));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita criacao quando email ja existe")
        void deveRejeitarEmailDuplicado() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThrows(EmailExistenteException.class,
                    () -> service.criar(dto, SENHA_RAW));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita criacao quando perfil nao existe")
        void deveRejeitarPerfilInexistente() {
            UsuarioDTO dto = UsuarioDTO.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of("INEXISTENTE"))
                    .build();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome("INEXISTENTE")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class,
                    () -> service.criar(dto, SENHA_RAW));
        }
    }

    // =========================================================
    // atualizar()
    // =========================================================
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("atualiza dados basicos com sucesso")
        void deveAtualizarComSucesso() {
            Usuario existente = usuarioComPerfil();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email("novo@email.com")
                    .nomeCompleto("Novo Nome")
                    .ativo(false)
                    .perfis(Set.of("PACIENTE"))
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO resultado = service.atualizar(USUARIO_ID, dto);

            assertEquals("novo@email.com", existente.getEmail());
            assertEquals("Novo Nome", existente.getNomeCompleto());
            assertFalse(existente.getAtivo());
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("lanca excecao ao atualizar usuario inexistente")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.atualizar(999L, usuarioDTO()));
        }

        @Test
        @DisplayName("limpa perfis quando dto.perfis vem vazio")
        void deveLimparPerfisQuandoVazio() {
            Usuario existente = usuarioComPerfil();
            int perfisAntes = existente.getUsuarioPerfis().size();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of())
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.atualizar(USUARIO_ID, dto);

            assertTrue(perfisAntes > 0);
            assertTrue(existente.getUsuarioPerfis().isEmpty());
            verify(perfilRepository, never()).findByNome(any());
        }

        @Test
        @DisplayName("lanca excecao quando novo perfil nao existe")
        void deveLancarQuandoPerfilInexistente() {
            Usuario existente = usuarioComPerfil();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of("FANTASMA"))
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(perfilRepository.findByNome("FANTASMA")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class,
                    () -> service.atualizar(USUARIO_ID, dto));
        }
    }

    // =========================================================
    // deletar()
    // =========================================================
    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("deleta usuario existente")
        void deveDeletar() {
            when(usuarioRepository.existsById(USUARIO_ID)).thenReturn(true);

            service.deletar(USUARIO_ID);

            verify(usuarioRepository).deleteById(USUARIO_ID);
        }

        @Test
        @DisplayName("lanca excecao ao deletar usuario inexistente")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.existsById(999L)).thenReturn(false);

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.deletar(999L));
            verify(usuarioRepository, never()).deleteById(any());
        }
    }

    // =========================================================
    // validarUsuarioExiste()
    // =========================================================
    @Nested
    @DisplayName("validarUsuarioExiste()")
    class ValidarUsuarioExiste {

        @Test
        @DisplayName("nao lanca quando usuario existe")
        void deveAceitarUsuarioExistente() {
            when(usuarioRepository.existsByUuid(USUARIO_UUID)).thenReturn(true);

            assertDoesNotThrow(() -> service.validarUsuarioExiste(USUARIO_UUID));
        }

        @Test
        @DisplayName("lanca excecao quando usuario nao existe")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.existsByUuid(USUARIO_UUID)).thenReturn(false);

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.validarUsuarioExiste(USUARIO_UUID));
        }
    }

    // =========================================================
    // getInfoUsuario()
    // =========================================================
    @Nested
    @DisplayName("getInfoUsuario()")
    class GetInfoUsuario {

        @Test
        @DisplayName("retorna usuario quando encontrado por username")
        void deveRetornarUsuario() {
            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.getInfoUsuario(USERNAME);

            assertEquals(USERNAME, resultado.username());
        }

        @Test
        @DisplayName("lanca UsernameNotFoundException quando nao encontrado")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findByUsername("inexistente"))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> service.getInfoUsuario("inexistente"));
        }
    }
}
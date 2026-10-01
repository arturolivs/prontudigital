package com.prontudigital.backend.autenticacao.config;

import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.PerfilNaoEncontradoException;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Este runner e a unica porta de entrada do primeiro ADMIN: se criar demais,
 * um restart devolve acesso a uma credencial que ja foi trocada; se criar de
 * menos, a instalacao nasce sem ninguem que consiga entrar. Os testes cobrem
 * os dois lados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapAdminRunner")
class BootstrapAdminRunnerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private BootstrapAdminProperties propriedades;
    private BootstrapAdminRunner runner;

    @BeforeEach
    void setUp() {
        propriedades = new BootstrapAdminProperties();
        propriedades.setUsername("admin.clinica");
        propriedades.setSenha("SenhaForte123");
        propriedades.setNomeCompleto("Administrador da Clinica");
        propriedades.setEmail("admin@clinica.com.br");

        runner = new BootstrapAdminRunner(
                propriedades, usuarioRepository, perfilRepository, passwordEncoder);
    }

    private Perfil perfilAdmin() {
        return Perfil.builder().id(1L).nome("ADMIN").build();
    }

    /** O save devolve a entidade com id: adicionarPerfil monta a chave composta com ele. */
    private void salvarAtribuindoId() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario u = invocacao.getArgument(0);
            if (u.getId() == null) {
                u.setId(1L);
            }
            return u;
        });
    }

    @Nested
    @DisplayName("quando o banco ainda nao tem ADMIN")
    class SemAdmin {

        @BeforeEach
        void semAdmin() {
            when(usuarioRepository.existsByUsuarioPerfis_Perfil_Nome("ADMIN")).thenReturn(false);
        }

        @Test
        @DisplayName("cria o ADMIN com senha hasheada, acesso ativado e perfil ADMIN")
        void deveCriarAdmin() {
            when(usuarioRepository.existsByUsername("admin.clinica")).thenReturn(false);
            when(usuarioRepository.existsByEmail("admin@clinica.com.br")).thenReturn(false);
            when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.of(perfilAdmin()));
            when(passwordEncoder.encode("SenhaForte123")).thenReturn("hash-bcrypt");
            salvarAtribuindoId();

            runner.run(null);

            ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository, times(2)).save(capturado.capture());

            Usuario admin = capturado.getValue();
            assertEquals("admin.clinica", admin.getUsername());
            assertEquals("hash-bcrypt", admin.getSenhaHash());
            assertTrue(admin.getAcessoAtivado(), "sem isto o ADMIN nao consegue logar");
            assertTrue(admin.getAtivo());
            assertEquals(1, admin.getPerfis().size());
            assertEquals("ADMIN", admin.getPerfis().iterator().next().getNome());
        }

        @Test
        @DisplayName("nao grava a senha em claro")
        void deveHashearSenha() {
            when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.of(perfilAdmin()));
            when(passwordEncoder.encode("SenhaForte123")).thenReturn("hash-bcrypt");
            salvarAtribuindoId();

            runner.run(null);

            verify(passwordEncoder).encode("SenhaForte123");
            ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository, atLeastOnce()).save(capturado.capture());
            assertNotEquals("SenhaForte123", capturado.getValue().getSenhaHash());
        }

        @Test
        @DisplayName("nao cria nada quando a senha nao foi informada")
        void deveIgnorarSemSenha() {
            propriedades.setSenha("   ");

            runner.run(null);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao cria nada quando o username nao foi informado")
        void deveIgnorarSemUsername() {
            propriedades.setUsername(null);

            runner.run(null);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa senha menor que o minimo aceito no registro pela API")
        void deveRecusarSenhaCurta() {
            propriedades.setSenha("1234567");

            runner.run(null);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao mexe em usuario existente com o mesmo username")
        void deveIgnorarUsernameEmUso() {
            when(usuarioRepository.existsByUsername("admin.clinica")).thenReturn(true);

            runner.run(null);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao cria quando o e-mail ja pertence a outro usuario")
        void deveIgnorarEmailEmUso() {
            when(usuarioRepository.existsByUsername("admin.clinica")).thenReturn(false);
            when(usuarioRepository.existsByEmail("admin@clinica.com.br")).thenReturn(true);

            runner.run(null);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("falha alto se os perfis de referencia sumiram do banco")
        void deveFalharSemPerfilAdmin() {
            when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class, () -> runner.run(null));
        }

        @Test
        @DisplayName("aceita ADMIN sem e-mail — a coluna e opcional")
        void deveCriarSemEmail() {
            propriedades.setEmail(null);
            when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.of(perfilAdmin()));
            when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
            salvarAtribuindoId();

            runner.run(null);

            verify(usuarioRepository, never()).existsByEmail(any());
            ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository, atLeastOnce()).save(capturado.capture());
            assertNull(capturado.getValue().getEmail());
        }
    }

    @Test
    @DisplayName("nao faz nada quando ja existe um ADMIN — restart nao ressuscita a senha do .env")
    void deveIgnorarQuandoJaHaAdmin() {
        when(usuarioRepository.existsByUsuarioPerfis_Perfil_Nome("ADMIN")).thenReturn(true);

        runner.run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(perfilRepository, passwordEncoder);
    }

    @Test
    @DisplayName("nao consulta o banco quando desabilitado por configuracao")
    void deveRespeitarDesabilitado() {
        propriedades.setHabilitado(false);

        runner.run(null);

        verifyNoInteractions(usuarioRepository, perfilRepository, passwordEncoder);
    }
}

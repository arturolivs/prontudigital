package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.entidades.RefreshToken;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.TokenInvalidoException;
import com.prontudigital.backend.autenticacao.repositorios.RefreshTokenRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.seguranca.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.prontudigital.backend.autenticacao.servicos.impl.fixtures.AutenticacaoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl")
class RefreshTokenServiceImplTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(
                refreshTokenRepository, usuarioRepository, jwtTokenProvider);
        // @Value não é resolvido em teste unitario — injetar manualmente
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", 604_800_000L);
    }

    // =========================================================
    // gerarRefreshToken()
    // =========================================================
    @Nested
    @DisplayName("gerarRefreshToken()")
    class Gerar {

        @Test
        @DisplayName("gera novo token, revoga ativos anteriores e persiste")
        void deveGerarERevogarAnteriores() {
            Usuario usuario = usuarioComPerfil();
            RefreshToken anterior = refreshTokenAtivo();

            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuario));
            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of(anterior));
            when(jwtTokenProvider.generateRefreshToken(USERNAME))
                    .thenReturn("novo-token");

            String resultado = service.gerarRefreshToken(USERNAME);

            assertEquals("novo-token", resultado);

            // anteriores foram revogados
            assertTrue(anterior.isRevogado());
            verify(refreshTokenRepository).saveAll(List.of(anterior));

            // novo token foi salvo
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken novo = captor.getValue();
            assertEquals("novo-token", novo.getToken());
            assertEquals(usuario, novo.getUsuario());
            assertFalse(novo.isRevogado());
            assertTrue(novo.getExpiraEm().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("nao chama saveAll quando nao ha tokens ativos para revogar")
        void naoChamaSaveAllSemTokensAtivos() {
            Usuario usuario = usuarioComPerfil();

            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuario));
            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of());
            when(jwtTokenProvider.generateRefreshToken(any()))
                    .thenReturn("novo-token");

            service.gerarRefreshToken(USERNAME);

            verify(refreshTokenRepository, never()).saveAll(any());
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("ignora tokens ja revogados ao limpar anteriores")
        void deveIgnorarTokensJaRevogados() {
            Usuario usuario = usuarioComPerfil();
            RefreshToken jaRevogado = refreshTokenRevogado();

            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuario));
            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of(jaRevogado));
            when(jwtTokenProvider.generateRefreshToken(any()))
                    .thenReturn("novo-token");

            service.gerarRefreshToken(USERNAME);

            // saveAll nao deve ser chamado pois nao ha tokens ativos
            verify(refreshTokenRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("ignora tokens expirados ao limpar anteriores")
        void deveIgnorarTokensExpirados() {
            Usuario usuario = usuarioComPerfil();
            RefreshToken expirado = refreshTokenExpirado();

            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuario));
            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of(expirado));
            when(jwtTokenProvider.generateRefreshToken(any()))
                    .thenReturn("novo-token");

            service.gerarRefreshToken(USERNAME);

            verify(refreshTokenRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("lanca excecao quando usuario nao existe")
        void deveLancarSeUsuarioInexistente() {
            when(usuarioRepository.findByUsername("fantasma"))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> service.gerarRefreshToken("fantasma"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // =========================================================
    // buscarPorToken()
    // =========================================================
    @Nested
    @DisplayName("buscarPorToken()")
    class BuscarPorToken {

        @Test
        @DisplayName("retorna token quando encontrado")
        void deveRetornarToken() {
            when(refreshTokenRepository.findByToken(TOKEN_VALIDO))
                    .thenReturn(Optional.of(refreshTokenAtivo()));

            Optional<RefreshToken> resultado = service.buscarPorToken(TOKEN_VALIDO);

            assertTrue(resultado.isPresent());
        }

        @Test
        @DisplayName("retorna Optional vazio quando nao encontrado")
        void deveRetornarVazio() {
            when(refreshTokenRepository.findByToken("inexistente"))
                    .thenReturn(Optional.empty());

            assertTrue(service.buscarPorToken("inexistente").isEmpty());
        }
    }

    // =========================================================
    // estaRevogadoOuExpirado()
    // =========================================================
    @Nested
    @DisplayName("estaRevogadoOuExpirado()")
    class EstaRevogadoOuExpirado {

        @Test
        @DisplayName("retorna true quando token esta revogado")
        void deveRetornarTrueQuandoRevogado() {
            when(refreshTokenRepository.findByToken(TOKEN_REVOGADO))
                    .thenReturn(Optional.of(refreshTokenRevogado()));

            assertTrue(service.estaRevogadoOuExpirado(TOKEN_REVOGADO));
        }

        @Test
        @DisplayName("retorna true quando token esta expirado")
        void deveRetornarTrueQuandoExpirado() {
            when(refreshTokenRepository.findByToken(TOKEN_EXPIRADO))
                    .thenReturn(Optional.of(refreshTokenExpirado()));

            assertTrue(service.estaRevogadoOuExpirado(TOKEN_EXPIRADO));
        }

        @Test
        @DisplayName("retorna false quando token esta ativo")
        void deveRetornarFalseQuandoAtivo() {
            when(refreshTokenRepository.findByToken(TOKEN_VALIDO))
                    .thenReturn(Optional.of(refreshTokenAtivo()));

            assertFalse(service.estaRevogadoOuExpirado(TOKEN_VALIDO));
        }

        @Test
        @DisplayName("retorna false quando token nao existe (comportamento atual)")
        void deveRetornarFalseQuandoInexistente() {
            when(refreshTokenRepository.findByToken("nao-existe"))
                    .thenReturn(Optional.empty());

            // Nota: filter().isPresent() retorna false para Optional.empty()
            assertFalse(service.estaRevogadoOuExpirado("nao-existe"));
        }
    }

    // =========================================================
    // revogarRefreshToken()
    // =========================================================
    @Nested
    @DisplayName("revogarRefreshToken()")
    class Revogar {

        @Test
        @DisplayName("revoga token existente")
        void deveRevogar() {
            RefreshToken token = refreshTokenAtivo();

            when(refreshTokenRepository.findByToken(TOKEN_VALIDO))
                    .thenReturn(Optional.of(token));

            service.revogarRefreshToken(TOKEN_VALIDO);

            assertTrue(token.isRevogado());
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("nao faz nada quando token nao existe")
        void naoFazNadaSeInexistente() {
            when(refreshTokenRepository.findByToken("nao-existe"))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.revogarRefreshToken("nao-existe"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // =========================================================
    // revogarTodosTokensDoUsuario()
    // =========================================================
    @Nested
    @DisplayName("revogarTodosTokensDoUsuario()")
    class RevogarTodos {

        @Test
        @DisplayName("revoga apenas tokens ativos do usuario")
        void deveRevogarApenasAtivos() {
            Usuario usuario = usuarioComPerfil();
            RefreshToken ativo = refreshTokenAtivo();
            RefreshToken jaRevogado = refreshTokenRevogado();
            RefreshToken expirado = refreshTokenExpirado();

            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of(ativo, jaRevogado, expirado));

            service.revogarTodosTokensDoUsuario(usuario);

            assertTrue(ativo.isRevogado());
            verify(refreshTokenRepository).saveAll(List.of(ativo));
        }

        @Test
        @DisplayName("nao chama saveAll quando todos ja inativos")
        void naoChamaSaveAllSemAtivos() {
            Usuario usuario = usuarioComPerfil();

            when(refreshTokenRepository.findAllByUsuario(usuario))
                    .thenReturn(List.of(refreshTokenRevogado(), refreshTokenExpirado()));

            service.revogarTodosTokensDoUsuario(usuario);

            verify(refreshTokenRepository, never()).saveAll(any());
        }
    }

    // =========================================================
    // rotacionarRefreshToken()
    // =========================================================
    @Nested
    @DisplayName("rotacionarRefreshToken()")
    class Rotacionar {

        @Test
        @DisplayName("rotaciona token valido com sucesso")
        void deveRotacionarComSucesso() {
            RefreshToken tokenAntigo = refreshTokenAtivo();

            when(refreshTokenRepository.findByToken(TOKEN_VALIDO))
                    .thenReturn(Optional.of(tokenAntigo));
            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(tokenAntigo.getUsuario()));
            when(refreshTokenRepository.findAllByUsuario(any()))
                    .thenReturn(List.of(tokenAntigo));
            when(jwtTokenProvider.generateRefreshToken(USERNAME))
                    .thenReturn("token-novo");

            String resultado = service.rotacionarRefreshToken(TOKEN_VALIDO);

            assertEquals("token-novo", resultado);
            assertTrue(tokenAntigo.isRevogado());
        }

        @Test
        @DisplayName("lanca excecao quando token antigo nao existe")
        void deveLancarSeInexistente() {
            when(refreshTokenRepository.findByToken("nao-existe"))
                    .thenReturn(Optional.empty());

            assertThrows(TokenInvalidoException.class,
                    () -> service.rotacionarRefreshToken("nao-existe"));
        }

        @Test
        @DisplayName("lanca excecao quando token antigo ja esta revogado")
        void deveLancarSeJaRevogado() {
            when(refreshTokenRepository.findByToken(TOKEN_REVOGADO))
                    .thenReturn(Optional.of(refreshTokenRevogado()));

            assertThrows(TokenInvalidoException.class,
                    () -> service.rotacionarRefreshToken(TOKEN_REVOGADO));
        }
    }
}
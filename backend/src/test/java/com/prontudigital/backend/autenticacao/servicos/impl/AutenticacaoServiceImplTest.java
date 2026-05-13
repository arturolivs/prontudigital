package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.*;
import com.prontudigital.backend.autenticacao.excecoes.TokenInvalidoException;
import com.prontudigital.backend.autenticacao.seguranca.JwtTokenProvider;
import com.prontudigital.backend.autenticacao.seguranca.UserDetailsImpl;
import com.prontudigital.backend.autenticacao.servicos.RefreshTokenService;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Set;

import static com.prontudigital.backend.autenticacao.servicos.impl.fixtures.AutenticacaoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoServiceImpl")
class AutenticacaoServiceImplTest {

    @Mock private UsuarioService usuarioService;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AutenticacaoServiceImpl service;

    // =========================================================
    // registrar()
    // =========================================================
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("delega a criacao ao UsuarioService com ativo=true")
        void deveDelegarCriacao() {
            RegistrarRequestDTO request = registrarRequest();
            UsuarioDTO dtoEsperado = usuarioDTO();

            when(usuarioService.criar(any(UsuarioDTO.class), any())).thenReturn(dtoEsperado);

            UsuarioDTO resultado = service.registrar(request);

            assertSame(dtoEsperado, resultado);

            ArgumentCaptor<UsuarioDTO> dtoCaptor = ArgumentCaptor.forClass(UsuarioDTO.class);
            ArgumentCaptor<String> senhaCaptor = ArgumentCaptor.forClass(String.class);
            verify(usuarioService).criar(dtoCaptor.capture(), senhaCaptor.capture());

            assertEquals(EMAIL, dtoCaptor.getValue().email());
            assertEquals(USERNAME, dtoCaptor.getValue().username());
            assertEquals(NOME, dtoCaptor.getValue().nomeCompleto());
            assertTrue(dtoCaptor.getValue().ativo());
            assertEquals(SENHA_RAW, senhaCaptor.getValue());
        }

        @Test
        @DisplayName("propaga excecao do UsuarioService")
        void devePropagarExcecaoDoUsuarioService() {
            RegistrarRequestDTO request = registrarRequest();

            when(usuarioService.criar(any(), any()))
                    .thenThrow(new RuntimeException("Erro na criacao"));

            assertThrows(RuntimeException.class, () -> service.registrar(request));
        }
    }

    // =========================================================
    // autenticar()
    // =========================================================
    @Nested
    @DisplayName("autenticar()")
    class Autenticar {

        @Test
        @DisplayName("autentica com sucesso e retorna tokens")
        void deveAutenticarComSucesso() {
            LoginRequestDTO request = loginRequest();
            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
            Authentication authentication = mock(Authentication.class);

            when(userDetails.getUsername()).thenReturn(USERNAME);
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                    .when(userDetails).getAuthorities();
            when(authentication.getPrincipal()).thenReturn(userDetails);

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(tokenProvider.generateAccessToken(authentication)).thenReturn("access-token");
            when(refreshTokenService.gerarRefreshToken(USERNAME)).thenReturn("refresh-token");

            JwtResponseDTO resultado = service.autenticar(request);

            assertEquals("access-token", resultado.accessToken());
            assertEquals("refresh-token", resultado.refreshToken());
            assertEquals(USERNAME, resultado.username());
            assertEquals(List.of("ROLE_PACIENTE"), resultado.perfis());
        }

        @Test
        @DisplayName("propaga AuthenticationException quando credenciais sao invalidas")
        void devePropagarExcecaoDoAuthManager() {
            LoginRequestDTO request = loginRequest();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new RuntimeException("Bad credentials"));

            assertThrows(RuntimeException.class, () -> service.autenticar(request));
            verify(refreshTokenService, never()).gerarRefreshToken(any());
        }

        @Test
        @DisplayName("passa username e senha corretos para o AuthenticationManager")
        void devePassarCredenciaisCorretas() {
            LoginRequestDTO request = loginRequest();
            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
            Authentication authentication = mock(Authentication.class);

            when(userDetails.getUsername()).thenReturn(USERNAME);
            doReturn(List.of()).when(userDetails).getAuthorities();
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(tokenProvider.generateAccessToken(any())).thenReturn("x");
            when(refreshTokenService.gerarRefreshToken(any())).thenReturn("y");

            service.autenticar(request);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertEquals(USERNAME, captor.getValue().getPrincipal());
            assertEquals(SENHA_RAW, captor.getValue().getCredentials());
        }
    }

    // =========================================================
    // renovarToken()
    // =========================================================
    @Nested
    @DisplayName("renovarToken()")
    class RenovarToken {

        @Test
        @DisplayName("renova token com sucesso quando valido")
        void deveRenovarComSucesso() {
            when(tokenProvider.validateToken(TOKEN_VALIDO)).thenReturn(true);
            when(refreshTokenService.estaRevogadoOuExpirado(TOKEN_VALIDO)).thenReturn(false);
            when(refreshTokenService.rotacionarRefreshToken(TOKEN_VALIDO))
                    .thenReturn("refresh-novo");
            when(tokenProvider.getUsernameFromToken(TOKEN_VALIDO)).thenReturn(USERNAME);

            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
            doReturn(Set.of()).when(userDetails).getAuthorities();
            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
            when(tokenProvider.generateAccessToken(any())).thenReturn("access-novo");

            RefreshTokenResponseDTO resultado = service.renovarToken(TOKEN_VALIDO);

            assertEquals("access-novo", resultado.accessToken());
            assertEquals("refresh-novo", resultado.refreshToken());
            assertEquals("Bearer", resultado.tokenType());
            assertEquals(900L, resultado.expiresIn());
        }

        @Test
        @DisplayName("rejeita token com assinatura invalida")
        void deveRejeitarTokenInvalido() {
            when(tokenProvider.validateToken("invalido")).thenReturn(false);

            assertThrows(TokenInvalidoException.class,
                    () -> service.renovarToken("invalido"));
            verify(refreshTokenService, never()).rotacionarRefreshToken(any());
        }

        @Test
        @DisplayName("rejeita token revogado")
        void deveRejeitarTokenRevogado() {
            when(tokenProvider.validateToken(TOKEN_REVOGADO)).thenReturn(true);
            when(refreshTokenService.estaRevogadoOuExpirado(TOKEN_REVOGADO)).thenReturn(true);

            assertThrows(TokenInvalidoException.class,
                    () -> service.renovarToken(TOKEN_REVOGADO));
            verify(refreshTokenService, never()).rotacionarRefreshToken(any());
        }
    }

    // =========================================================
    // encerrarSessao()
    // =========================================================
    @Nested
    @DisplayName("encerrarSessao()")
    class EncerrarSessao {

        @Test
        @DisplayName("delega a revogacao do refresh token")
        void deveDelegarRevogacao() {
            service.encerrarSessao(TOKEN_VALIDO);

            verify(refreshTokenService).revogarRefreshToken(TOKEN_VALIDO);
        }

        @Test
        @DisplayName("propaga excecao do RefreshTokenService")
        void devePropagarExcecao() {
            doThrow(new RuntimeException("falha"))
                    .when(refreshTokenService).revogarRefreshToken(any());

            assertThrows(RuntimeException.class,
                    () -> service.encerrarSessao(TOKEN_VALIDO));
        }
    }
}
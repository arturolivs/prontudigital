package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.*;
import com.prontudigital.backend.autenticacao.entidades.CodigoRecuperacaoSenha;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.AcessoNaoAtivadoException;
import com.prontudigital.backend.autenticacao.excecoes.CodigoRecuperacaoInvalidoException;
import com.prontudigital.backend.autenticacao.excecoes.TokenInvalidoException;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioNaoEncontradoException;
import com.prontudigital.backend.autenticacao.repositorios.CodigoRecuperacaoSenhaRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.seguranca.JwtTokenProvider;
import com.prontudigital.backend.autenticacao.seguranca.UserDetailsImpl;
import com.prontudigital.backend.autenticacao.servicos.RefreshTokenService;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.prontudigital.backend.autenticacao.servicos.impl.fixtures.AutenticacaoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoServiceImpl")
class AutenticacaoServiceImplTest {

    private static final String TELEFONE = "11999999999";
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private CodigoRecuperacaoSenhaRepository codigoRecuperacaoSenhaRepository;
    @Mock private WhatsappCloudApiClient whatsappCliente;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutenticacaoServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        ReflectionTestUtils.setField(service, "clock", clock);
        ReflectionTestUtils.setField(service, "expiracaoCodigoMinutos", 10);
    }

    private CodigoRecuperacaoSenha codigo(String valor, LocalDateTime expiraEm, int tentativas) {
        return CodigoRecuperacaoSenha.builder()
                .id(1L)
                .usuarioId(USUARIO_ID)
                .codigo(valor)
                .expiraEm(expiraEm)
                .utilizado(false)
                .tentativas(tentativas)
                .build();
    }

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
            when(usuarioRepository.findByUsername(USERNAME)).thenReturn(
                    java.util.Optional.of(Usuario.builder().username(USERNAME).acessoAtivado(true).build()));
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
        @DisplayName("rejeita usuario autenticado mas com acesso nao ativado")
        void deveRejeitarAcessoNaoAtivado() {
            LoginRequestDTO request = loginRequest();
            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
            Authentication authentication = mock(Authentication.class);

            when(userDetails.getUsername()).thenReturn(USERNAME);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(usuarioRepository.findByUsername(USERNAME)).thenReturn(
                    Optional.of(Usuario.builder().username(USERNAME).acessoAtivado(false).build()));

            assertThrows(AcessoNaoAtivadoException.class, () -> service.autenticar(request));
            verify(tokenProvider, never()).generateAccessToken(any());
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
            when(usuarioRepository.findByUsername(USERNAME)).thenReturn(
                    java.util.Optional.of(Usuario.builder().username(USERNAME).acessoAtivado(true).build()));
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

    // =========================================================
    // cadastrarPaciente()
    // =========================================================
    @Nested
    @DisplayName("cadastrarPaciente()")
    class CadastrarPaciente {

        @Test
        @DisplayName("cadastra o paciente e ja retorna tokens de sessao")
        void deveCadastrarERetornarTokens() {
            CadastrarPacienteDTO dto = new CadastrarPacienteDTO(NOME, TELEFONE);
            Usuario usuario = Usuario.builder().username(USERNAME).build();
            UserDetails userDetails = mock(UserDetails.class);

            when(usuarioService.cadastrarPaciente(dto)).thenReturn(usuario);
            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                    .when(userDetails).getAuthorities();
            when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
            when(refreshTokenService.gerarRefreshToken(USERNAME)).thenReturn("refresh-token");

            JwtResponseDTO resultado = service.cadastrarPaciente(dto);

            assertEquals("access-token", resultado.accessToken());
            assertEquals("refresh-token", resultado.refreshToken());
            assertEquals(USERNAME, resultado.username());
            assertEquals(List.of("ROLE_PACIENTE"), resultado.perfis());
        }
    }

    // =========================================================
    // ativarAcesso()
    // =========================================================
    @Nested
    @DisplayName("ativarAcesso()")
    class AtivarAcesso {

        @Test
        @DisplayName("delega a ativacao ao UsuarioService")
        void deveDelegarAtivacao() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO(TELEFONE, EMAIL, USERNAME, SENHA_RAW);
            UsuarioDTO esperado = usuarioDTO();

            when(usuarioService.ativarAcesso(dto)).thenReturn(esperado);

            assertSame(esperado, service.ativarAcesso(dto));
            verify(usuarioService).ativarAcesso(dto);
        }
    }

    // =========================================================
    // solicitarRecuperacaoSenha()
    // =========================================================
    @Nested
    @DisplayName("solicitarRecuperacaoSenha()")
    class SolicitarRecuperacaoSenha {

        @Test
        @DisplayName("invalida codigos anteriores, gera novo e envia por WhatsApp")
        void deveGerarEEnviarCodigo() {
            RecuperarSenhaSolicitarRequestDTO dto =
                    new RecuperarSenhaSolicitarRequestDTO("  " + TELEFONE + "  "); // com espacos
            Usuario usuario = Usuario.builder().id(USUARIO_ID).username(USERNAME).build();
            CodigoRecuperacaoSenha anterior = codigo("111111", AGORA.plusMinutes(5), 0);

            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository.findByUsuarioIdAndUtilizadoFalse(USUARIO_ID))
                    .thenReturn(List.of(anterior));

            service.solicitarRecuperacaoSenha(dto);

            assertTrue(anterior.getUtilizado());

            ArgumentCaptor<CodigoRecuperacaoSenha> captor =
                    ArgumentCaptor.forClass(CodigoRecuperacaoSenha.class);
            verify(codigoRecuperacaoSenhaRepository).save(captor.capture());
            assertEquals(USUARIO_ID, captor.getValue().getUsuarioId());
            assertTrue(captor.getValue().getCodigo().matches("\\d{6}"));
            assertEquals(AGORA.plusMinutes(10), captor.getValue().getExpiraEm());

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(whatsappCliente).enviarMensagemTexto(eq(TELEFONE), msg.capture());
            assertTrue(msg.getValue().contains(captor.getValue().getCodigo()));
        }

        @Test
        @DisplayName("lanca excecao quando nenhum usuario tem o telefone")
        void deveLancarSeTelefoneNaoEncontrado() {
            RecuperarSenhaSolicitarRequestDTO dto =
                    new RecuperarSenhaSolicitarRequestDTO(TELEFONE);

            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.solicitarRecuperacaoSenha(dto));
            verify(codigoRecuperacaoSenhaRepository, never()).save(any());
            verifyNoInteractions(whatsappCliente);
        }
    }

    // =========================================================
    // confirmarRecuperacaoSenha()
    // =========================================================
    @Nested
    @DisplayName("confirmarRecuperacaoSenha()")
    class ConfirmarRecuperacaoSenha {

        private RecuperarSenhaConfirmarRequestDTO dto(String codigo) {
            return new RecuperarSenhaConfirmarRequestDTO(TELEFONE, codigo, "novaSenha123");
        }

        @Test
        @DisplayName("lanca excecao quando nenhum usuario tem o telefone")
        void deveLancarSeTelefoneNaoEncontrado() {
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.confirmarRecuperacaoSenha(dto("123456")));
        }

        @Test
        @DisplayName("lanca excecao quando nao ha codigo ativo")
        void deveLancarSeSemCodigo() {
            Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThrows(CodigoRecuperacaoInvalidoException.class,
                    () -> service.confirmarRecuperacaoSenha(dto("123456")));
        }

        @Test
        @DisplayName("codigo expirado e marcado como utilizado e rejeitado")
        void deveRejeitarCodigoExpirado() {
            Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
            CodigoRecuperacaoSenha entidade = codigo("123456", AGORA.minusMinutes(1), 0);
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.of(entidade));

            assertThrows(CodigoRecuperacaoInvalidoException.class,
                    () -> service.confirmarRecuperacaoSenha(dto("123456")));

            assertTrue(entidade.getUtilizado());
            verify(codigoRecuperacaoSenhaRepository).save(entidade);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("codigo com tentativas excedidas e invalidado e rejeitado")
        void deveRejeitarTentativasExcedidas() {
            Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
            CodigoRecuperacaoSenha entidade = codigo("123456", AGORA.plusMinutes(5), 5);
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.of(entidade));

            assertThrows(CodigoRecuperacaoInvalidoException.class,
                    () -> service.confirmarRecuperacaoSenha(dto("123456")));

            assertTrue(entidade.getUtilizado());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("codigo incorreto incrementa tentativas e rejeita sem invalidar")
        void deveIncrementarTentativasSeCodigoIncorreto() {
            Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
            CodigoRecuperacaoSenha entidade = codigo("123456", AGORA.plusMinutes(5), 1);
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.of(entidade));

            assertThrows(CodigoRecuperacaoInvalidoException.class,
                    () -> service.confirmarRecuperacaoSenha(dto("999999")));

            assertEquals(2, entidade.getTentativas());
            assertFalse(entidade.getUtilizado());
            verify(codigoRecuperacaoSenhaRepository).save(entidade);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("codigo correto invalida o codigo e grava a nova senha (com hash)")
        void deveTrocarSenhaComSucesso() {
            Usuario usuario = Usuario.builder().id(USUARIO_ID).username(USERNAME).build();
            CodigoRecuperacaoSenha entidade = codigo("123456", AGORA.plusMinutes(5), 0);
            when(usuarioRepository.findByTelefone(TELEFONE)).thenReturn(Optional.of(usuario));
            when(codigoRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.of(entidade));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("HASH_NOVO");

            service.confirmarRecuperacaoSenha(dto("123456"));

            assertTrue(entidade.getUtilizado());
            assertEquals("HASH_NOVO", usuario.getSenhaHash());
            verify(codigoRecuperacaoSenhaRepository).save(entidade);
            verify(usuarioRepository).save(usuario);
        }
    }
}
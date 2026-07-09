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
import com.prontudigital.backend.autenticacao.servicos.AutenticacaoService;
import com.prontudigital.backend.autenticacao.servicos.RefreshTokenService;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutenticacaoServiceImpl implements AutenticacaoService {

    private static final int MAX_TENTATIVAS_CODIGO = 5;

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final CodigoRecuperacaoSenhaRepository codigoRecuperacaoSenhaRepository;
    private final WhatsappCloudApiClient whatsappCliente;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.recuperacao-senha.expiracao-minutos:10}")
    private int expiracaoCodigoMinutos;

    @Override
    public UsuarioDTO registrar(RegistrarRequestDTO request) {
        UsuarioDTO dto = UsuarioDTO.builder()
                .email(request.email())
                .username(request.username())
                .nomeCompleto(request.nomeCompleto())
                .telefone(request.telefone())
                .ativo(true)
                .perfis(request.perfis())
                .build();

        return usuarioService.criar(dto, request.senha());
    }

    @Override
    public JwtResponseDTO autenticar(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.senha()));

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        Usuario usuario = usuarioRepository.findByUsername(principal.getUsername())
                .orElseThrow();
        if (!usuario.getAcessoAtivado()) {
            throw new AcessoNaoAtivadoException();
        }

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.gerarRefreshToken(principal.getUsername());

        List<String> perfis = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(accessToken, refreshToken, principal.getUsername(), perfis);
    }

    @Override
    public JwtResponseDTO cadastrarPaciente(CadastrarPacienteDTO dto) {
        Usuario usuario = usuarioService.cadastrarPaciente(dto);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.gerarRefreshToken(usuario.getUsername());

        List<String> perfis = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponseDTO(accessToken, refreshToken, usuario.getUsername(), perfis);
    }

    @Override
    public UsuarioDTO ativarAcesso(AtivarAcessoRequestDTO dto) {
        return usuarioService.ativarAcesso(dto);
    }

    @Override
    public RefreshTokenResponseDTO renovarToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new TokenInvalidoException(Mensagens.get("auth.refresh-token-invalido"));
        }
        if (refreshTokenService.estaRevogadoOuExpirado(refreshToken)) {
            throw new TokenInvalidoException(Mensagens.get("auth.refresh-token-revogado"));
        }

        String novoRefreshToken = refreshTokenService.rotacionarRefreshToken(refreshToken);

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String novoAccessToken = tokenProvider.generateAccessToken(authentication);

        return new RefreshTokenResponseDTO(novoAccessToken, novoRefreshToken, "Bearer", 900L);
    }

    @Override
    public void encerrarSessao(String refreshToken) {
        refreshTokenService.revogarRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public void solicitarRecuperacaoSenha(RecuperarSenhaSolicitarRequestDTO dto) {
        String telefone = dto.telefone().trim();
        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(
                        "Nenhum usuário encontrado com o telefone: " + telefone));

        codigoRecuperacaoSenhaRepository.findByUsuarioIdAndUtilizadoFalse(usuario.getId())
                .forEach(c -> c.setUtilizado(true));

        String codigo = gerarCodigoRecuperacao();
        CodigoRecuperacaoSenha entidade = CodigoRecuperacaoSenha.builder()
                .usuarioId(usuario.getId())
                .codigo(codigo)
                .expiraEm(LocalDateTime.now(clock).plusMinutes(expiracaoCodigoMinutos))
                .build();
        codigoRecuperacaoSenhaRepository.save(entidade);

        String mensagem = String.format(
                "Seu código de recuperação de senha é: %s%nVálido por %d minutos. Não compartilhe este código com ninguém.",
                codigo, expiracaoCodigoMinutos);

        whatsappCliente.enviarMensagemTexto(telefone, mensagem);
    }

    @Override
    @Transactional
    public void confirmarRecuperacaoSenha(RecuperarSenhaConfirmarRequestDTO dto) {
        String telefone = dto.telefone().trim();
        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(
                        "Nenhum usuário encontrado com o telefone: " + telefone));

        CodigoRecuperacaoSenha entidade = codigoRecuperacaoSenhaRepository
                .findFirstByUsuarioIdAndUtilizadoFalseOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow(() -> new CodigoRecuperacaoInvalidoException("Código inválido ou expirado"));

        if (LocalDateTime.now(clock).isAfter(entidade.getExpiraEm())) {
            entidade.setUtilizado(true);
            codigoRecuperacaoSenhaRepository.save(entidade);
            throw new CodigoRecuperacaoInvalidoException(Mensagens.get("auth.codigo.expirado"));
        }

        if (entidade.getTentativas() >= MAX_TENTATIVAS_CODIGO) {
            entidade.setUtilizado(true);
            codigoRecuperacaoSenhaRepository.save(entidade);
            throw new CodigoRecuperacaoInvalidoException(Mensagens.get("auth.codigo.tentativas-excedidas"));
        }

        if (!entidade.getCodigo().equals(dto.codigo().trim())) {
            entidade.setTentativas(entidade.getTentativas() + 1);
            codigoRecuperacaoSenhaRepository.save(entidade);
            throw new CodigoRecuperacaoInvalidoException(Mensagens.get("auth.codigo.invalido"));
        }

        entidade.setUtilizado(true);
        codigoRecuperacaoSenhaRepository.save(entidade);

        usuario.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    private String gerarCodigoRecuperacao() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
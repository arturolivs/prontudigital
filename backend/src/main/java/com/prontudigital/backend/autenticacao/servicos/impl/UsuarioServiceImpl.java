package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.AlterarSenhaRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtivarAcessoRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtualizarPerfilRequestDTO;
import com.prontudigital.backend.autenticacao.dto.CadastrarPacienteDTO;
import com.prontudigital.backend.autenticacao.dto.EnderecoDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Endereco;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.CorenExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.CpfExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.EmailExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.SenhaAtualInvalidaException;
import com.prontudigital.backend.autenticacao.excecoes.PerfilNaoEncontradoException;
import com.prontudigital.backend.autenticacao.excecoes.TelefoneExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.UserNameExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioNaoEncontradoException;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.autenticacao.util.UsuarioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.prontudigital.backend.autenticacao.util.UsuarioUtil.converterUsuarioParaDTO;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioUtil::converterUsuarioParaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(UsuarioUtil::converterUsuarioParaDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioUtil::converterUsuarioParaDTO)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorUuid(UUID uuid) {
        Usuario usuario = usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(uuid));
        return converterUsuarioParaDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO criar(UsuarioDTO dto, String senhaRaw) {
        validar(dto);
        Usuario usuario = construirUsuarioDoDTO(dto, senhaRaw);
        usuario = usuarioRepository.save(usuario);
        return converterUsuarioParaDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = getUsuarioPorId(id);

        usuario.setEmail(dto.email());
        usuario.setNomeCompleto(dto.nomeCompleto());
        usuario.setTelefone(dto.telefone());
        usuario.setAtivo(dto.ativo());

        aplicarDadosPessoais(usuario, dto.cpf(), dto.dataNascimento(), dto.endereco());
        aplicarDadosProfissionais(usuario, dto.coren(), dto.especialidade());

        atualizarPerfis(usuario, dto.perfis());

        return converterUsuarioParaDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioDTO atualizarPerfil(Long id, AtualizarPerfilRequestDTO dto) {
        Usuario usuario = getUsuarioPorId(id);
        usuario.setNomeCompleto(dto.nomeCompleto());
        usuario.setEmail(dto.email());
        usuario.setTelefone(dto.telefone());

        // COREN e especialidade nao entram aqui de proposito: sao credenciais
        // profissionais, alteradas pelo ADMIN via PUT /api/usuarios/{id}.
        aplicarDadosPessoais(usuario, dto.cpf(), dto.dataNascimento(), dto.endereco());

        return converterUsuarioParaDTO(usuarioRepository.save(usuario));
    }

    /** RF04 — CPF (unico), data de nascimento e endereco. */
    private void aplicarDadosPessoais(Usuario usuario, String cpf,
                                      LocalDate dataNascimento, EnderecoDTO endereco) {
        String cpfNormalizado = UsuarioUtil.normalizarCpf(cpf);
        if (cpfNormalizado != null && cpfJaUsado(cpfNormalizado, usuario.getId())) {
            throw new CpfExistenteException(cpf);
        }
        usuario.setCpf(cpfNormalizado);
        usuario.setDataNascimento(dataNascimento);

        // Endereco ausente na requisicao preserva o que ja estava gravado.
        Endereco novoEndereco = UsuarioUtil.converterEnderecoDeDTO(endereco);
        if (novoEndereco != null) {
            usuario.setEndereco(novoEndereco);
        }
    }

    /** RF05 — COREN (unico) e especialidade. */
    private void aplicarDadosProfissionais(Usuario usuario, String coren, String especialidade) {
        String corenNormalizado = UsuarioUtil.normalizar(coren);
        if (corenNormalizado != null && corenJaUsado(corenNormalizado, usuario.getId())) {
            throw new CorenExistenteException(coren);
        }
        usuario.setCoren(corenNormalizado);
        usuario.setEspecialidade(UsuarioUtil.normalizar(especialidade));
    }

    private boolean cpfJaUsado(String cpf, Long id) {
        return id == null
                ? usuarioRepository.existsByCpf(cpf)
                : usuarioRepository.existsByCpfAndIdNot(cpf, id);
    }

    private boolean corenJaUsado(String coren, Long id) {
        return id == null
                ? usuarioRepository.existsByCoren(coren)
                : usuarioRepository.existsByCorenAndIdNot(coren, id);
    }

    @Override
    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequestDTO dto) {
        Usuario usuario = getUsuarioPorId(id);
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenhaHash())) {
            throw new SenhaAtualInvalidaException();
        }
        usuario.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNaoEncontradoException(id);
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public void validarUsuarioExiste(UUID uuid) {
        if (!usuarioRepository.existsByUuid(uuid)) {
            throw new UsuarioNaoEncontradoException(uuid);
        }
    }

    @Override
    public UsuarioDTO getInfoUsuario(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        return converterUsuarioParaDTO(usuario);
    }

    private Usuario construirUsuarioDoDTO(UsuarioDTO dto, String senhaRaw) {
        Usuario usuario = Usuario.builder()
                .email(dto.email())
                .username(dto.username())
                .senhaHash(passwordEncoder.encode(senhaRaw))
                .nomeCompleto(dto.nomeCompleto())
                .telefone(dto.telefone())
                .ativo(Objects.requireNonNullElse(dto.ativo(), true))
                .acessoAtivado(true)
                .build();

        aplicarDadosPessoais(usuario, dto.cpf(), dto.dataNascimento(), dto.endereco());
        aplicarDadosProfissionais(usuario, dto.coren(), dto.especialidade());

        if (dto.perfis() != null) {
            atualizarPerfis(usuario, dto.perfis());
        }

        return usuario;
    }

    private Usuario getUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    @Override
    @Transactional
    public Usuario cadastrarPaciente(CadastrarPacienteDTO dto) {
        String telefone = dto.telefone().trim();
        if (usuarioRepository.existsByTelefone(telefone)) {
            throw new TelefoneExistenteException(telefone);
        }

        String telefoneLimpo = telefone.replaceAll("[^0-9]", "");
        String username = gerarUsernameUnico("pac_" + telefoneLimpo);

        Usuario usuario = Usuario.builder()
                .nomeCompleto(dto.nomeCompleto().trim())
                .telefone(telefone)
                .username(username)
                .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .acessoAtivado(false)
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        Perfil perfilPaciente = perfilRepository.findByNome("PACIENTE")
                .orElseThrow(() -> new PerfilNaoEncontradoException("ROLE_PACIENTE"));
        usuario.adicionarPerfil(perfilPaciente);

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO ativarAcesso(AtivarAcessoRequestDTO dto) {
        String telefone = dto.telefone().trim();
        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Nenhum paciente encontrado com o telefone: " + telefone));

        if (usuarioRepository.existsByUsername(dto.username()) &&
                !usuario.getUsername().equals(dto.username())) {
            throw new UserNameExistenteException(dto.username());
        }
        if (dto.email() != null && usuarioRepository.existsByEmail(dto.email().trim())) {
            throw new EmailExistenteException(dto.email());
        }

        usuario.setEmail(dto.email().trim());
        usuario.setUsername(dto.username().trim());
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        usuario.setAcessoAtivado(true);

        return converterUsuarioParaDTO(usuarioRepository.save(usuario));
    }

    private String gerarUsernameUnico(String base) {
        String candidate = base;
        int suffix = 1;
        while (usuarioRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private void validar(UsuarioDTO dto) {
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new UserNameExistenteException(dto.username());
        }
        if (dto.email() != null && usuarioRepository.existsByEmail(dto.email())) {
            throw new EmailExistenteException(dto.email());
        }
    }

    private void atualizarPerfis(Usuario usuario, Set<String> perfis) {
        usuario.getUsuarioPerfis().clear();
        if (perfis != null && !perfis.isEmpty()) {
            perfis.forEach(nomePerfil -> {
                Perfil perfil = perfilRepository.findByNome(nomePerfil)
                        .orElseThrow(() -> new PerfilNaoEncontradoException(nomePerfil));
                usuario.adicionarPerfil(perfil);
            });
        }
    }
}
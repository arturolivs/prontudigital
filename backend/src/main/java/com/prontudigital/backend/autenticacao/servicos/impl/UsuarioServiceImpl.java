package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.EmailExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.UserNameExistenteException;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioNaoEncontradoException;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.RoleNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(this::converterParaDTO)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorUuid(UUID uuid) {
        Usuario usuario = usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(uuid));
        return converterParaUsuarioDTO(usuario);
    }

    /*
     * Nota: recebe UsuarioResponseDTO como parâmetro de criação por compatibilidade
     * com AuthServiceImpl. Considere criar um UsuarioCriacaoDTO dedicado no futuro.
     */
    @Override
    @Transactional
    public UsuarioDTO criar(UsuarioDTO dto, String senhaRaw) {
        validar(dto);
        Usuario usuario = construirUsuarioDoDTO(dto, senhaRaw);
        usuario = usuarioRepository.save(usuario);
        return converterParaDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = getUsuarioPorId(id);

        usuario.setEmail(dto.email());
        usuario.setNomeCompleto(dto.nomeCompleto());
        usuario.setAtivo(dto.ativo());

        atualizarPerfis(usuario, dto.roles());

        return converterParaDTO(usuarioRepository.save(usuario));
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
                .orElseThrow(() -> new UsernameNotFoundException("Usuário nao encontrado: " + username));
        return converterParaDTO(usuario);
    }

    private UsuarioDTO converterParaDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nomeCompleto(usuario.getNomeCompleto())
                .ativo(usuario.getAtivo())
                .roles(usuario.getPerfis().stream()
                        .map(Perfil::getNome)
                        .collect(Collectors.toSet()))
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }

    private UsuarioDTO converterParaUsuarioDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .uuid(usuario.getUuid())
                .nomeCompleto(usuario.getNomeCompleto())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .ativo(usuario.getAtivo())
                .roles(usuario.getPerfis().stream()
                        .map(Perfil::getNome)
                        .collect(Collectors.toSet()))
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }

    private Usuario construirUsuarioDoDTO(UsuarioDTO dto, String senhaRaw) {
        Usuario usuario = Usuario.builder()
                .email(dto.email())
                .username(dto.username())
                .passwordHash(passwordEncoder.encode(senhaRaw))
                .nomeCompleto(dto.nomeCompleto())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .build();

        if (dto.roles() != null) {
            atualizarPerfis(usuario, dto.roles());
        }

        return usuario;
    }

    private Usuario getUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    private void validar(UsuarioDTO dto) {
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new UserNameExistenteException(dto.username());
        }
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new EmailExistenteException(dto.email());
        }
    }

    private void atualizarPerfis(Usuario usuario, Set<String> roles) {
        usuario.getPerfis().clear();

        if (roles != null && !roles.isEmpty()) {
            roles.forEach(nomePerfil -> {
                Perfil perfil = null;
                try {
                    perfil = perfilRepository.findByNome(nomePerfil)
                            .orElseThrow(() -> new RoleNotFoundException(nomePerfil));
                } catch (RoleNotFoundException e) {
                    throw new RuntimeException(e);
                }
                usuario.adicionarPerfil(perfil);
            });
        }
    }
}
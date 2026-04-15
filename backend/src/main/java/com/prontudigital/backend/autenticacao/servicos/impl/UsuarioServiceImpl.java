package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import lombok.RequiredArgsConstructor;
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

    private final AutenticacaoServiceImpl usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(this::converterParaDTO)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorUuid(UUID uuid) {
        Usuario usuario = usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException(uuid));
        return converterParaUsuarioDTO(usuario);
    }

    /*
     * Nota: recebe UsuarioResponseDTO como parâmetro de criação por compatibilidade
     * com AuthServiceImpl. Considere criar um UsuarioCriacaoDTO dedicado no futuro.
     */
    @Override
    @Transactional
    public UsuarioResponseDTO criar(UsuarioResponseDTO dto, String senhaRaw) {
        validar(dto);
        Usuario usuario = construirUsuarioDoDTO(dto, senhaRaw);
        usuario = usuarioRepository.save(usuario);
        return converterParaDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioResponseDTO dto) {
        Usuario usuario = getUsuarioPorId(id);

        usuario.setEmail(dto.getEmail());
        usuario.setNomeCompleto(dto.getNomeCompleto());
        usuario.setAtivo(dto.getAtivo());

        atualizarPerfis(usuario, dto.getRoles());

        return converterParaDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public void validarUsuarioExiste(UUID uuid) {
        if (!usuarioRepository.existsByUuid(uuid)) {
            throw new UserNotFoundException(uuid);
        }
    }

    // ========== MÉTODOS PRIVADOS ==========

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nomeCompleto(usuario.getNomeCompleto())
                .ativo(usuario.getAtivo())
                .roles(usuario.getPerfis().stream()
                        .map(Perfil::getName)
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
                        .map(Perfil::getName)
                        .collect(Collectors.toSet()))
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }

    private Usuario construirUsuarioDoDTO(UsuarioResponseDTO dto, String senhaRaw) {
        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(senhaRaw))
                .nomeCompleto(dto.getNomeCompleto())
                .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
                .build();

        if (dto.getRoles() != null) {
            atualizarPerfis(usuario, dto.getRoles());
        }

        return usuario;
    }

    private Usuario getUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validar(UsuarioResponseDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new UserNameAlreadyExistsException(dto.getUsername());
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException(dto.getEmail());
        }
    }

    private void atualizarPerfis(Usuario usuario, Set<String> roles) {
        // Limpa perfis atuais e reatribui — HashSet garantido pela entidade
        usuario.getPerfis().clear();

        if (roles != null && !roles.isEmpty()) {
            roles.forEach(nomePerfil -> {
                Perfil perfil = perfilRepository.findByName(nomePerfil)
                        .orElseThrow(() -> new RoleNotFoundException(nomePerfil));
                usuario.adicionarPerfil(perfil);
            });
        }
    }
}
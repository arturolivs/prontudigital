package com.prontudigital.backend.autenticacao.servicos;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;

import java.util.List;
import java.util.UUID;

public interface UsuarioService {
    List<UsuarioDTO> listarTodos();
    UsuarioDTO buscarPorId(Long id);
    UsuarioDTO buscarPorUuid(UUID uuid);
    UsuarioDTO criar(UsuarioDTO dto, String senhaRaw);
    UsuarioDTO atualizar(Long id, UsuarioDTO dto);
    void deletar(Long id);
    void validarUsuarioExiste(UUID uuid);
    UsuarioDTO getInfoUsuario(String username);
}

package com.prontudigital.backend.autenticacao.servicos;

import com.prontudigital.backend.autenticacao.dto.AlterarSenhaRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtivarAcessoRequestDTO;
import com.prontudigital.backend.autenticacao.dto.CadastrarPacienteDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Usuario;

import java.util.List;
import java.util.UUID;

public interface UsuarioService {
    List<UsuarioDTO> listarTodos();
    UsuarioDTO buscarPorId(Long id);
    UsuarioDTO buscarPorUuid(UUID uuid);
    UsuarioDTO criar(UsuarioDTO dto, String senhaRaw);
    UsuarioDTO atualizar(Long id, UsuarioDTO dto);
    void alterarSenha(Long id, AlterarSenhaRequestDTO dto);
    void deletar(Long id);
    void validarUsuarioExiste(UUID uuid);
    UsuarioDTO getInfoUsuario(String username);
    Usuario cadastrarPaciente(CadastrarPacienteDTO dto);
    UsuarioDTO ativarAcesso(AtivarAcessoRequestDTO dto);
}

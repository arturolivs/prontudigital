package com.prontudigital.backend.agendamento.seguranca;


import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.NaoAutenticadoException;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class AgendamentoPermissaoPolicy {

    public String perfilEfetivo(UsuarioDTO usuario) {
        Set<String> perfis = usuario.perfis();
        if (perfis == null || perfis.isEmpty()) {
            throw new NaoAutenticadoException(Mensagens.get("auth.sem-perfil-atribuido"));
        }
        if (perfis.contains("ADMIN"))        return "ADMIN";
        if (perfis.contains("PROFISSIONAL")) return "PROFISSIONAL";
        if (perfis.contains("PACIENTE"))     return "PACIENTE";
        throw new NaoAutenticadoException(Mensagens.get("auth.sem-perfil-reconhecido"));
    }

    public boolean podeCriar(UsuarioDTO usuario, AgendamentoRequestDTO request) {
        String perfil = perfilEfetivo(usuario);
        UUID uuid = usuario.uuid();
        return switch (perfil) {
            case "ADMIN"        -> true;
            case "PROFISSIONAL" -> request.profissionalUuid().equals(uuid);
            case "PACIENTE"     -> request.pacienteUuid().equals(uuid);
            default             -> false;
        };
    }

    public boolean podeModificar(UsuarioDTO usuario, Agendamento agendamento) {
        String perfil = perfilEfetivo(usuario);
        UUID uuid = usuario.uuid();
        return switch (perfil) {
            case "ADMIN"        -> true;
            case "PROFISSIONAL" -> agendamento.getProfissionalUuid().equals(uuid);
            case "PACIENTE"     -> agendamento.getPacienteUuid().equals(uuid);
            default             -> false;
        };
    }

    public boolean podeVisualizar(UsuarioDTO usuario, Agendamento agendamento) {
        return podeModificar(usuario, agendamento);
    }
}
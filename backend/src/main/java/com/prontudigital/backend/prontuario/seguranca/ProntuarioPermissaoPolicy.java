package com.prontudigital.backend.prontuario.seguranca;

import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.NaoAutenticadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Regras de acesso ao prontuario do paciente.
 *
 * RN03: profissionais so podem acessar prontuarios de seus proprios pacientes,
 * ou seja, pacientes com os quais possuem ao menos um agendamento.
 */
@Component
@RequiredArgsConstructor
public class ProntuarioPermissaoPolicy {

    private final AgendamentoRepository agendamentoRepository;

    public String perfilEfetivo(UsuarioDTO usuario) {
        Set<String> perfis = usuario.perfis();
        if (perfis == null || perfis.isEmpty()) {
            throw new NaoAutenticadoException("Usuário sem perfil atribuído");
        }
        if (perfis.contains("ADMIN"))        return "ADMIN";
        if (perfis.contains("PROFISSIONAL")) return "PROFISSIONAL";
        if (perfis.contains("PACIENTE"))     return "PACIENTE";
        throw new NaoAutenticadoException("Usuário sem perfil reconhecido");
    }

    /** Pode visualizar o prontuario do paciente informado. */
    public boolean podeVisualizar(UsuarioDTO usuario, UUID pacienteUuid) {
        String perfil = perfilEfetivo(usuario);
        return switch (perfil) {
            case "ADMIN"        -> true;
            case "PROFISSIONAL" -> agendamentoRepository
                    .existsByProfissionalUuidAndPacienteUuid(usuario.uuid(), pacienteUuid);
            case "PACIENTE"     -> pacienteUuid.equals(usuario.uuid());
            default             -> false;
        };
    }

    /** Pode registrar/alterar o prontuario. Paciente nunca edita dados clinicos. */
    public boolean podeEditar(UsuarioDTO usuario, UUID pacienteUuid) {
        String perfil = perfilEfetivo(usuario);
        return switch (perfil) {
            case "ADMIN"        -> true;
            case "PROFISSIONAL" -> agendamentoRepository
                    .existsByProfissionalUuidAndPacienteUuid(usuario.uuid(), pacienteUuid);
            default             -> false;
        };
    }
}

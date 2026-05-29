package com.prontudigital.backend.agendamento.utils;


import com.prontudigital.backend.agendamento.dto.AgendamentoDetalhadoDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgendamentoUtil {

    private final UsuarioService usuarioService;

    /*
     * Método não-estático — a classe tem dependência injetada (UsuarioService),
     * portanto nenhum método deve ser estático. Consistência garantida.
     */
    public AgendamentoResponseDTO convertToResponseDTO(Agendamento agendamento) {
        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                agendamento.getObservacoes(),
                agendamento.getCriadoEm(),
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null,
                agendamento.getConcluidoEm()
        );
    }

    public AgendamentoViewDTO convertToViewDTO(Agendamento agendamento) {
        String nomePaciente;
        String nomeProfissional;

        try {
            UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
            UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

            nomePaciente = paciente != null ? paciente.nomeCompleto() : "Paciente nao encontrado";
            nomeProfissional = profissional != null ? profissional.nomeCompleto() : "Profissional nao encontrado";
        } catch (Exception e) {
            log.error("Erro ao resolver nomes para agendamento id={}", agendamento.getId(), e);
            nomePaciente = "Indisponivel";
            nomeProfissional = "Indisponivel";
        }

        return new AgendamentoViewDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                nomePaciente,
                nomeProfissional,
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null
        );
    }

    public AgendamentoDetalhadoDTO convertToDetalhadoDTO(Agendamento agendamento) {
        String nomePaciente;
        String nomeProfissional;

        try {
            UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
            UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());
            nomePaciente = paciente.nomeCompleto();
            nomeProfissional = profissional.nomeCompleto();
        } catch (Exception e) {
            log.error("Erro ao resolver nomes para agendamento id={}", agendamento.getId(), e);
            nomePaciente = "Indisponivel";
            nomeProfissional = "Indisponivel";
        }

        return new AgendamentoDetalhadoDTO(
                agendamento.getId(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getProfissionalUuid(),
                agendamento.getPacienteUuid(),
                agendamento.getTipo(),
                agendamento.getTipoProcedimento(),
                agendamento.getLocalAtendimento(),
                agendamento.getPacienteAcamado(),
                agendamento.getStatus(),
                nomePaciente,
                nomeProfissional,
                agendamento.getObservacoes(),
                agendamento.getCriadoEm(),
                agendamento.getAvaliacao() != null ? agendamento.getAvaliacao().getId() : null,
                agendamento.getConcluidoEm()
        );
    }
}
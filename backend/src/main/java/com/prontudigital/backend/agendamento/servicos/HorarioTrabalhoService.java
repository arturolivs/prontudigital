package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.HorarioTrabalhoDTO;

import java.util.List;
import java.util.UUID;

/** Horarios de trabalho do profissional (RF05). */
public interface HorarioTrabalhoService {

    HorarioTrabalhoDTO criar(HorarioTrabalhoDTO request);

    List<HorarioTrabalhoDTO> listarPorProfissional(UUID profissionalUuid);

    void remover(Long id);
}

package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import com.prontudigital.backend.agendamento.dto.BloqueioRecorrenteDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BloqueioHorarioService {
    BloqueioHorarioDTO criar(BloqueioHorarioDTO request);
    void remover(Long id);
    List<BloqueioHorarioDTO> listarPorProfissional(UUID profissionalUuid,
                                                   LocalDate inicio, LocalDate fim);

    BloqueioRecorrenteDTO criarRecorrente(BloqueioRecorrenteDTO request);
    void removerRecorrente(Long id);
    List<BloqueioRecorrenteDTO> listarRecorrentesPorProfissional(UUID profissionalUuid);
}

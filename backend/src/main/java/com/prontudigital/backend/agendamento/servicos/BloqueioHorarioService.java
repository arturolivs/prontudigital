package com.prontudigital.backend.agendamento.servicos;

import com.prontudigital.backend.agendamento.dto.BloqueioHorarioDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BloqueioHorarioService {
    BloqueioHorarioDTO criar(BloqueioHorarioDTO request);
    void remover(Long id);
    List<BloqueioHorarioDTO> listarPorProfissional(UUID profissionalUuid,
                                                   LocalDate inicio, LocalDate fim);
}

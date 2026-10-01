package com.prontudigital.backend.prontuario.servicos;

import com.prontudigital.backend.prontuario.dto.HistoricoItemDTO;

import java.util.List;
import java.util.UUID;

public interface HistoricoService {

    /** Linha do tempo consolidada do prontuario do paciente, da mais recente para a mais antiga (RF18). */
    List<HistoricoItemDTO> montarHistorico(UUID pacienteUuid);
}

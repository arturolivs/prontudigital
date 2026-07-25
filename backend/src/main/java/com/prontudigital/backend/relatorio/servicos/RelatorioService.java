package com.prontudigital.backend.relatorio.servicos;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;

import java.time.LocalDate;
import java.util.UUID;

/** Relatorios gerenciais (RF19 e RF20). */
public interface RelatorioService {

    /** RF19 — atendimentos do periodo, filtrados por profissional/status/tipo. */
    RelatorioAtendimentosDTO atendimentos(LocalDate inicio, LocalDate fim,
                                          UUID profissionalUuid,
                                          StatusAgendamento status,
                                          TipoAgendamento tipo);

    /** RF20 — comparecimento, cancelamentos e ocupacao da agenda no periodo. */
    RelatorioOcupacaoDTO ocupacao(LocalDate inicio, LocalDate fim, UUID profissionalUuid);
}

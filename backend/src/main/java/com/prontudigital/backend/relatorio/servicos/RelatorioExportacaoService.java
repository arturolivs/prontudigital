package com.prontudigital.backend.relatorio.servicos;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.compartilhado.documento.FormatoExportacao;

import java.time.LocalDate;
import java.util.UUID;

/** Exportacao dos relatorios em PDF/Excel (RF21). */
public interface RelatorioExportacaoService {

    ArquivoGerado exportarAtendimentos(LocalDate inicio, LocalDate fim,
                                       UUID profissionalUuid,
                                       StatusAgendamento status,
                                       TipoAgendamento tipo,
                                       FormatoExportacao formato);

    ArquivoGerado exportarOcupacao(LocalDate inicio, LocalDate fim,
                                   UUID profissionalUuid,
                                   FormatoExportacao formato);
}

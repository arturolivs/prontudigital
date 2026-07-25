package com.prontudigital.backend.relatorio.servicos.impl;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.compartilhado.documento.FormatoExportacao;
import com.prontudigital.backend.compartilhado.documento.PdfBuilder;
import com.prontudigital.backend.compartilhado.documento.PlanilhaBuilder;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentoItemDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;
import com.prontudigital.backend.relatorio.servicos.RelatorioExportacaoService;
import com.prontudigital.backend.relatorio.servicos.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RF21 — exporta os relatorios do RF19/RF20 em PDF e Excel.
 *
 * <p>Delega a apuracao ao {@link RelatorioService}, entao as regras de acesso e
 * as formulas das taxas ficam num lugar so: aqui e apenas formatacao.
 */
@Service
@RequiredArgsConstructor
public class RelatorioExportacaoServiceImpl implements RelatorioExportacaoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<String> COLUNAS_ATENDIMENTOS = List.of(
            "Data", "Duração (min)", "Paciente", "Profissional",
            "Tipo", "Procedimento", "Local", "Status", "Concluído em");

    private final RelatorioService relatorioService;

    @Override
    @Transactional(readOnly = true)
    public ArquivoGerado exportarAtendimentos(LocalDate inicio, LocalDate fim,
                                              UUID profissionalUuid,
                                              StatusAgendamento status,
                                              TipoAgendamento tipo,
                                              FormatoExportacao formato) {

        RelatorioAtendimentosDTO relatorio =
                relatorioService.atendimentos(inicio, fim, profissionalUuid, status, tipo);

        List<List<String>> linhas = relatorio.itens().stream()
                .map(this::linhaAtendimento)
                .toList();

        String nomeBase = "atendimentos_" + inicio + "_a_" + fim;
        byte[] conteudo = formato == FormatoExportacao.PDF
                ? pdfAtendimentos(relatorio, linhas)
                : xlsxAtendimentos(relatorio, linhas);

        return ArquivoGerado.de(nomeBase, formato, conteudo);
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoGerado exportarOcupacao(LocalDate inicio, LocalDate fim,
                                          UUID profissionalUuid,
                                          FormatoExportacao formato) {

        RelatorioOcupacaoDTO relatorio =
                relatorioService.ocupacao(inicio, fim, profissionalUuid);

        String nomeBase = "ocupacao_" + inicio + "_a_" + fim;
        byte[] conteudo = formato == FormatoExportacao.PDF
                ? pdfOcupacao(relatorio)
                : xlsxOcupacao(relatorio);

        return ArquivoGerado.de(nomeBase, formato, conteudo);
    }

    // ── RF19 em PDF/XLSX ─────────────────────────────────────────

    private byte[] pdfAtendimentos(RelatorioAtendimentosDTO r, List<List<String>> linhas) {
        // Nove colunas nao cabem em retrato.
        PdfBuilder pdf = new PdfBuilder("Relatório de atendimentos", true)
                .subtitulo(escopo(r.inicio(), r.fim(), r.nomeProfissional()));

        pdf.secao("Resumo");
        pdf.campo("Total de atendimentos", String.valueOf(r.total()));
        r.totalPorStatus().forEach((chave, valor) -> pdf.campo(chave, String.valueOf(valor)));

        pdf.secao("Atendimentos");
        if (linhas.isEmpty()) {
            pdf.paragrafo("Nenhum atendimento no período com os filtros escolhidos.");
        } else {
            pdf.tabela(COLUNAS_ATENDIMENTOS, linhas);
        }
        return pdf.gerar();
    }

    private byte[] xlsxAtendimentos(RelatorioAtendimentosDTO r, List<List<String>> linhas) {
        try (PlanilhaBuilder planilha = new PlanilhaBuilder()) {
            planilha.aba("Atendimentos", COLUNAS_ATENDIMENTOS, linhas);

            List<List<String>> resumo = new ArrayList<>();
            resumo.add(List.of("Período", r.inicio().format(DATA) + " a " + r.fim().format(DATA)));
            resumo.add(List.of("Profissional",
                    r.nomeProfissional() == null ? "Toda a clínica" : r.nomeProfissional()));
            resumo.add(List.of("Total", String.valueOf(r.total())));
            r.totalPorStatus().forEach((chave, valor) ->
                    resumo.add(List.of(chave, String.valueOf(valor))));
            r.totalPorTipo().forEach((chave, valor) ->
                    resumo.add(List.of(chave, String.valueOf(valor))));

            planilha.aba("Resumo", List.of("Indicador", "Valor"), resumo);
            return planilha.gerar();
        }
    }

    private List<String> linhaAtendimento(RelatorioAtendimentoItemDTO item) {
        return List.of(
                texto(item.inicioEm()),
                String.valueOf(item.duracaoMinutos()),
                nuloParaTraco(item.nomePaciente()),
                nuloParaTraco(item.nomeProfissional()),
                item.tipo() == null ? "—" : item.tipo().name(),
                nuloParaTraco(item.procedimentoNome()),
                item.localAtendimento() == null ? "—" : item.localAtendimento().name(),
                item.status().name(),
                texto(item.concluidoEm()));
    }

    // ── RF20 em PDF/XLSX ─────────────────────────────────────────

    private byte[] pdfOcupacao(RelatorioOcupacaoDTO r) {
        PdfBuilder pdf = new PdfBuilder("Relatório de ocupação")
                .subtitulo(escopo(r.inicio(), r.fim(), r.nomeProfissional()));

        pdf.secao("Indicadores");
        pdf.campo("Taxa de comparecimento", percentual(r.taxaComparecimento()));
        pdf.campo("Taxa de cancelamento", percentual(r.taxaCancelamento()));
        pdf.campo("Taxa de absenteísmo", percentual(r.taxaAbsenteismo()));
        pdf.campo("Taxa de ocupação da agenda", percentual(r.taxaOcupacao()));

        pdf.secao("Agendamentos");
        pdf.tabela(List.of("Situação", "Quantidade"), List.of(
                List.of("Total", String.valueOf(r.total())),
                List.of("Realizados", String.valueOf(r.realizados())),
                List.of("Cancelados", String.valueOf(r.cancelados())),
                List.of("Faltas", String.valueOf(r.naoCompareceram())),
                List.of("Remarcados", String.valueOf(r.remarcados())),
                List.of("Em aberto", String.valueOf(r.emAberto()))));

        pdf.secao("Horas");
        pdf.campo("Horas ocupadas", horas(r.horasAgendadas()));
        pdf.campo("Horas de expediente", horas(r.horasDisponiveis()));

        pdf.paragrafo("Comparecimento e absenteísmo são calculados sobre os atendimentos "
                + "que chegaram a acontecer (realizados e faltas); cancelamentos prévios "
                + "ficam fora desse denominador. A taxa de ocupação depende dos horários "
                + "de trabalho cadastrados para o profissional.");

        return pdf.gerar();
    }

    private byte[] xlsxOcupacao(RelatorioOcupacaoDTO r) {
        try (PlanilhaBuilder planilha = new PlanilhaBuilder()) {
            List<List<String>> linhas = List.of(
                    List.of("Período", r.inicio().format(DATA) + " a " + r.fim().format(DATA)),
                    List.of("Profissional",
                            r.nomeProfissional() == null ? "Toda a clínica" : r.nomeProfissional()),
                    List.of("Total de agendamentos", String.valueOf(r.total())),
                    List.of("Realizados", String.valueOf(r.realizados())),
                    List.of("Cancelados", String.valueOf(r.cancelados())),
                    List.of("Faltas", String.valueOf(r.naoCompareceram())),
                    List.of("Remarcados", String.valueOf(r.remarcados())),
                    List.of("Em aberto", String.valueOf(r.emAberto())),
                    List.of("Taxa de comparecimento", percentual(r.taxaComparecimento())),
                    List.of("Taxa de cancelamento", percentual(r.taxaCancelamento())),
                    List.of("Taxa de absenteísmo", percentual(r.taxaAbsenteismo())),
                    List.of("Horas ocupadas", horas(r.horasAgendadas())),
                    List.of("Horas de expediente", horas(r.horasDisponiveis())),
                    List.of("Taxa de ocupação", percentual(r.taxaOcupacao())));

            planilha.aba("Ocupação", List.of("Indicador", "Valor"), linhas);
            return planilha.gerar();
        }
    }

    // ── formatacao ───────────────────────────────────────────────

    private String escopo(LocalDate inicio, LocalDate fim, String nomeProfissional) {
        String periodo = inicio.format(DATA) + " a " + fim.format(DATA);
        return nomeProfissional == null
                ? periodo + " — toda a clínica"
                : periodo + " — " + nomeProfissional;
    }

    /** Traco em vez de "0%": nulo aqui significa "sem base de comparacao". */
    private String percentual(Double valor) {
        return valor == null ? "—" : String.format("%.1f%%", valor);
    }

    private String horas(Double valor) {
        return valor == null ? "—" : String.format("%.1fh", valor);
    }

    private String texto(LocalDateTime data) {
        return data == null ? "—" : data.format(DATA_HORA);
    }

    private String nuloParaTraco(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}

package com.prontudigital.backend.relatorio.servicos.impl;

import com.prontudigital.backend.compartilhado.documento.MarcaDocumento;
import com.prontudigital.backend.configuracao.servicos.MarcaDocumentoProvider;
import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.compartilhado.documento.FormatoExportacao;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentoItemDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;
import com.prontudigital.backend.relatorio.servicos.RelatorioService;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioExportacaoServiceImpl (RF21)")
class RelatorioExportacaoServiceImplTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 7, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 7, 31);

    @Mock private RelatorioService relatorioService;
    @Mock private MarcaDocumentoProvider marcaProvider;

    private RelatorioExportacaoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RelatorioExportacaoServiceImpl(relatorioService, marcaProvider);

        // Marca minima: o cabecalho agora vem da configuracao da clinica.
        // `lenient` porque so os testes de PDF a consomem — os de XLSX nao
        // passam pelo PdfBuilder e acusariam stub desnecessario.
        lenient().when(marcaProvider.obter())
                .thenReturn(MarcaDocumento.padrao("Clinica de Teste"));
    }

    private RelatorioAtendimentosDTO relatorioAtendimentos(int quantidade) {
        List<RelatorioAtendimentoItemDTO> itens = java.util.stream.IntStream
                .range(0, quantidade)
                .mapToObj(i -> RelatorioAtendimentoItemDTO.builder()
                        .agendamentoId((long) i)
                        .inicioEm(LocalDateTime.of(2026, 7, 10, 8, 0))
                        .fimEm(LocalDateTime.of(2026, 7, 10, 9, 0))
                        .duracaoMinutos(60)
                        .pacienteUuid(UUID.randomUUID())
                        .nomePaciente("Paciente " + i)
                        .profissionalUuid(UUID.randomUUID())
                        .nomeProfissional("Enf. Ana")
                        .tipo(TipoAgendamento.TRATAMENTO)
                        .procedimentoNome("Curativo")
                        .localAtendimento(LocalAtendimento.CLINICA)
                        .status(StatusAgendamento.REALIZADO)
                        .build())
                .toList();

        return RelatorioAtendimentosDTO.builder()
                .inicio(INICIO)
                .fim(FIM)
                .nomeProfissional("Enf. Ana")
                .total(itens.size())
                .totalPorStatus(Map.of("REALIZADO", (long) itens.size()))
                .totalPorTipo(Map.of("TRATAMENTO", (long) itens.size()))
                .itens(itens)
                .build();
    }

    /** Cobre o caso das taxas nulas — o formatador precisa escrever "—". */
    private RelatorioOcupacaoDTO relatorioOcupacao(boolean comTaxas) {
        return RelatorioOcupacaoDTO.builder()
                .inicio(INICIO)
                .fim(FIM)
                .nomeProfissional(comTaxas ? "Enf. Ana" : null)
                .total(comTaxas ? 4 : 0)
                .realizados(comTaxas ? 3 : 0)
                .cancelados(comTaxas ? 1 : 0)
                .naoCompareceram(0)
                .remarcados(0)
                .emAberto(0)
                .taxaComparecimento(comTaxas ? 100.0 : null)
                .taxaCancelamento(comTaxas ? 25.0 : null)
                .taxaAbsenteismo(comTaxas ? 0.0 : null)
                .horasAgendadas(comTaxas ? 3.0 : 0.0)
                .horasDisponiveis(comTaxas ? 40.0 : null)
                .taxaOcupacao(comTaxas ? 7.5 : null)
                .build();
    }

    private boolean ehPdf(byte[] conteudo) {
        return conteudo.length > 4 && new String(conteudo, 0, 4).equals("%PDF");
    }

    // =========================================================
    // exportarAtendimentos() - RF19 em arquivo
    // =========================================================
    @Nested
    @DisplayName("exportarAtendimentos()")
    class ExportarAtendimentos {

        @Test
        @DisplayName("gera PDF com nome derivado do periodo")
        void deveGerarPdf() {
            when(relatorioService.atendimentos(any(), any(), any(), any(), any()))
                    .thenReturn(relatorioAtendimentos(3));

            ArquivoGerado arquivo = service.exportarAtendimentos(
                    INICIO, FIM, null, null, null, FormatoExportacao.PDF);

            assertEquals("atendimentos_2026-07-01_a_2026-07-31.pdf", arquivo.nomeArquivo());
            assertEquals("application/pdf", arquivo.contentType());
            assertTrue(ehPdf(arquivo.conteudo()));
        }

        @Test
        @DisplayName("gera XLSX com abas de atendimentos e resumo")
        void deveGerarXlsx() throws IOException {
            when(relatorioService.atendimentos(any(), any(), any(), any(), any()))
                    .thenReturn(relatorioAtendimentos(2));

            ArquivoGerado arquivo = service.exportarAtendimentos(
                    INICIO, FIM, null, null, null, FormatoExportacao.XLSX);

            assertEquals("atendimentos_2026-07-01_a_2026-07-31.xlsx", arquivo.nomeArquivo());

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
                Sheet atendimentos = wb.getSheet("Atendimentos");
                assertNotNull(atendimentos);
                assertNotNull(wb.getSheet("Resumo"));
                // cabecalho + 2 linhas
                assertEquals(2, atendimentos.getLastRowNum());
                assertEquals("Data", atendimentos.getRow(0).getCell(0).getStringCellValue());
                assertEquals("Paciente 0",
                        atendimentos.getRow(1).getCell(2).getStringCellValue());
            }
        }

        @Test
        @DisplayName("relatorio vazio ainda gera arquivo valido")
        void deveGerarComRelatorioVazio() {
            when(relatorioService.atendimentos(any(), any(), any(), any(), any()))
                    .thenReturn(relatorioAtendimentos(0));

            ArquivoGerado pdf = service.exportarAtendimentos(
                    INICIO, FIM, null, null, null, FormatoExportacao.PDF);
            ArquivoGerado xlsx = service.exportarAtendimentos(
                    INICIO, FIM, null, null, null, FormatoExportacao.XLSX);

            assertTrue(ehPdf(pdf.conteudo()));
            assertTrue(xlsx.conteudo().length > 0);
        }

        @Test
        @DisplayName("repassa os filtros ao servico de relatorio")
        void deveRepassarFiltros() {
            UUID profissional = UUID.randomUUID();
            when(relatorioService.atendimentos(any(), any(), any(), any(), any()))
                    .thenReturn(relatorioAtendimentos(0));

            service.exportarAtendimentos(INICIO, FIM, profissional,
                    StatusAgendamento.REALIZADO, TipoAgendamento.AVALIACAO,
                    FormatoExportacao.PDF);

            verify(relatorioService).atendimentos(INICIO, FIM, profissional,
                    StatusAgendamento.REALIZADO, TipoAgendamento.AVALIACAO);
        }
    }

    // =========================================================
    // exportarOcupacao() - RF20 em arquivo
    // =========================================================
    @Nested
    @DisplayName("exportarOcupacao()")
    class ExportarOcupacao {

        @Test
        @DisplayName("gera PDF da ocupacao")
        void deveGerarPdf() {
            when(relatorioService.ocupacao(any(), any(), any()))
                    .thenReturn(relatorioOcupacao(true));

            ArquivoGerado arquivo = service.exportarOcupacao(
                    INICIO, FIM, UUID.randomUUID(), FormatoExportacao.PDF);

            assertEquals("ocupacao_2026-07-01_a_2026-07-31.pdf", arquivo.nomeArquivo());
            assertTrue(ehPdf(arquivo.conteudo()));
        }

        @Test
        @DisplayName("gera XLSX da ocupacao com os indicadores")
        void deveGerarXlsx() throws IOException {
            when(relatorioService.ocupacao(any(), any(), any()))
                    .thenReturn(relatorioOcupacao(true));

            ArquivoGerado arquivo = service.exportarOcupacao(
                    INICIO, FIM, UUID.randomUUID(), FormatoExportacao.XLSX);

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
                Sheet aba = wb.getSheet("Ocupação");
                assertNotNull(aba);
                assertEquals("Indicador", aba.getRow(0).getCell(0).getStringCellValue());
                assertEquals("Período", aba.getRow(1).getCell(0).getStringCellValue());
            }
        }

        @Test
        @DisplayName("taxa nula vira travessao, nao 0%")
        void deveEscreverTravessaoParaTaxaNula() throws IOException {
            when(relatorioService.ocupacao(any(), any(), any()))
                    .thenReturn(relatorioOcupacao(false));

            ArquivoGerado arquivo = service.exportarOcupacao(
                    INICIO, FIM, null, FormatoExportacao.XLSX);

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
                Sheet aba = wb.getSheet("Ocupação");
                boolean achouTravessao = false;
                for (int i = 1; i <= aba.getLastRowNum(); i++) {
                    if ("Taxa de comparecimento"
                            .equals(aba.getRow(i).getCell(0).getStringCellValue())) {
                        assertEquals("—", aba.getRow(i).getCell(1).getStringCellValue());
                        achouTravessao = true;
                    }
                }
                assertTrue(achouTravessao, "linha da taxa de comparecimento nao encontrada");
            }
        }

        @Test
        @DisplayName("sem profissional o escopo vira 'toda a clinica'")
        void deveGerarSemProfissional() throws IOException {
            when(relatorioService.ocupacao(any(), any(), any()))
                    .thenReturn(relatorioOcupacao(false));

            ArquivoGerado arquivo = service.exportarOcupacao(
                    INICIO, FIM, null, FormatoExportacao.XLSX);

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
                Sheet aba = wb.getSheet("Ocupação");
                assertEquals("Toda a clínica",
                        aba.getRow(2).getCell(1).getStringCellValue());
            }
        }
    }
}

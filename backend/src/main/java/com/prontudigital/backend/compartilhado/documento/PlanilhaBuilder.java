package com.prontudigital.backend.compartilhado.documento;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Envolve o Apache POI para a exportacao XLSX (RF21).
 *
 * <p>Cada chamada de {@link #aba} cria uma planilha com cabecalho em negrito,
 * colunas dimensionadas e painel congelado na primeira linha.
 */
public class PlanilhaBuilder implements AutoCloseable {

    /** Autodimensionar coluna e caro; acima disso o custo nao compensa. */
    private static final int MAX_LINHAS_PARA_AUTODIMENSIONAR = 2_000;

    private final Workbook workbook = new XSSFWorkbook();
    private final CellStyle estiloCabecalho;

    public PlanilhaBuilder() {
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        estiloCabecalho = workbook.createCellStyle();
        estiloCabecalho.setFont(fonte);
    }

    public PlanilhaBuilder aba(String nome, List<String> colunas, List<List<String>> linhas) {
        Sheet aba = workbook.createSheet(nome);

        Row cabecalho = aba.createRow(0);
        for (int i = 0; i < colunas.size(); i++) {
            Cell celula = cabecalho.createCell(i);
            celula.setCellValue(colunas.get(i));
            celula.setCellStyle(estiloCabecalho);
        }

        int numeroLinha = 1;
        for (List<String> linha : linhas) {
            Row row = aba.createRow(numeroLinha++);
            for (int i = 0; i < linha.size(); i++) {
                row.createCell(i).setCellValue(linha.get(i) == null ? "" : linha.get(i));
            }
        }

        aba.createFreezePane(0, 1);

        if (linhas.size() <= MAX_LINHAS_PARA_AUTODIMENSIONAR) {
            for (int i = 0; i < colunas.size(); i++) {
                aba.autoSizeColumn(i);
            }
        }

        return this;
    }

    public byte[] gerar() {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            workbook.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar a planilha", e);
        }
    }

    @Override
    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao fechar a planilha", e);
        }
    }
}

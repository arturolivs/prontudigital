package com.prontudigital.backend.compartilhado.documento;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envolve o OpenPDF para o formato de documento usado no sistema
 * (RF17 e RF21): cabecalho da clinica, titulo, blocos de conteudo e rodape
 * com data de emissao.
 *
 * <p>Uso encadeado, fechando com {@link #gerar()}:
 * <pre>{@code
 * byte[] pdf = new PdfBuilder("Relatorio de atendimentos")
 *         .subtitulo("01/07/2026 a 31/07/2026")
 *         .tabela(List.of("Data", "Paciente"), linhas)
 *         .gerar();
 * }</pre>
 */
public class PdfBuilder {

    private static final String NOME_CLINICA = "ProntuDigital";
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final Color COR_CABECALHO = new Color(37, 99, 235);
    private static final Color COR_LINHA_ALTERNADA = new Color(248, 250, 252);
    private static final Color COR_TEXTO_SUAVE = new Color(100, 116, 139);

    private static final Font FONTE_TITULO =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font FONTE_SUBTITULO =
            FontFactory.getFont(FontFactory.HELVETICA, 10, COR_TEXTO_SUAVE);
    private static final Font FONTE_SECAO =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font FONTE_TEXTO =
            FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONTE_TABELA_CABECALHO =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONTE_TABELA =
            FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONTE_RODAPE =
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, COR_TEXTO_SUAVE);

    private final Document documento;
    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();

    public PdfBuilder(String titulo) {
        this(titulo, false);
    }

    /** {@code paisagem} vale a pena para tabelas com muitas colunas. */
    public PdfBuilder(String titulo, boolean paisagem) {
        Rectangle pagina = paisagem ? PageSize.A4.rotate() : PageSize.A4;
        this.documento = new Document(pagina, 42, 42, 42, 36);
        PdfWriter.getInstance(documento, saida);
        documento.open();

        Paragraph cabecalho = new Paragraph(NOME_CLINICA, FONTE_SECAO);
        cabecalho.setSpacingAfter(2);
        adicionar(cabecalho);

        Paragraph paragrafoTitulo = new Paragraph(titulo, FONTE_TITULO);
        paragrafoTitulo.setSpacingAfter(4);
        adicionar(paragrafoTitulo);
    }

    public PdfBuilder subtitulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return this;
        }
        Paragraph p = new Paragraph(texto, FONTE_SUBTITULO);
        p.setSpacingAfter(14);
        return adicionar(p);
    }

    public PdfBuilder secao(String titulo) {
        Paragraph p = new Paragraph(titulo, FONTE_SECAO);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        return adicionar(p);
    }

    public PdfBuilder paragrafo(String texto) {
        Paragraph p = new Paragraph(texto == null ? "" : texto, FONTE_TEXTO);
        p.setSpacingAfter(6);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return adicionar(p);
    }

    /** Par rotulo/valor, um por linha — usado nos resumos e no atestado. */
    public PdfBuilder campo(String rotulo, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(rotulo + ": ", FONTE_SECAO));
        p.add(new Chunk(valor == null ? "—" : valor, FONTE_TEXTO));
        p.setSpacingAfter(4);
        return adicionar(p);
    }

    public PdfBuilder tabela(List<String> colunas, List<List<String>> linhas) {
        PdfPTable tabela = new PdfPTable(colunas.size());
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(8);

        for (String coluna : colunas) {
            PdfPCell celula = new PdfPCell(new Phrase(coluna, FONTE_TABELA_CABECALHO));
            celula.setBackgroundColor(COR_CABECALHO);
            celula.setPadding(6);
            celula.setBorderWidth(0);
            tabela.addCell(celula);
        }

        boolean alternada = false;
        for (List<String> linha : linhas) {
            for (String valor : linha) {
                PdfPCell celula = new PdfPCell(
                        new Phrase(valor == null ? "—" : valor, FONTE_TABELA));
                celula.setPadding(5);
                celula.setBorderWidth(0);
                celula.setBorderWidthBottom(0.5f);
                celula.setBorderColorBottom(new Color(226, 232, 240));
                if (alternada) {
                    celula.setBackgroundColor(COR_LINHA_ALTERNADA);
                }
                tabela.addCell(celula);
            }
            alternada = !alternada;
        }

        return adicionar(tabela);
    }

    /** Linha de assinatura do profissional — exigida no atestado (RF17). */
    public PdfBuilder assinatura(String nome, String registro) {
        Paragraph espaco = new Paragraph(" ");
        espaco.setSpacingBefore(40);
        adicionar(espaco);

        Paragraph linha = new Paragraph("_________________________________________",
                FONTE_TEXTO);
        linha.setAlignment(Element.ALIGN_CENTER);
        adicionar(linha);

        Paragraph identificacao = new Paragraph(
                registro == null || registro.isBlank() ? nome : nome + " — " + registro,
                FONTE_SUBTITULO);
        identificacao.setAlignment(Element.ALIGN_CENTER);
        return adicionar(identificacao);
    }

    public byte[] gerar() {
        Paragraph rodape = new Paragraph(
                "Emitido em " + LocalDateTime.now().format(DATA_HORA) + " por " + NOME_CLINICA,
                FONTE_RODAPE);
        rodape.setSpacingBefore(24);
        rodape.setAlignment(Element.ALIGN_RIGHT);
        adicionar(rodape);

        documento.close();
        return saida.toByteArray();
    }

    /**
     * O OpenPDF declara {@link DocumentException} nos {@code add}, mas aqui
     * escrevemos num buffer em memoria — falha só aconteceria por bug de
     * montagem, que nao e recuperavel em runtime.
     */
    private PdfBuilder adicionar(Element elemento) {
        try {
            documento.add(elemento);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao montar o PDF", e);
        }
        return this;
    }
}

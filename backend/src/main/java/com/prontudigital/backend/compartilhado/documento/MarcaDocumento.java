package com.prontudigital.backend.compartilhado.documento;

/**
 * Identidade da clinica aplicada ao cabecalho e ao rodape dos documentos.
 *
 * <p>Existe para desacoplar o {@link PdfBuilder} da entidade de configuracao:
 * o builder monta PDF e nao deve conhecer JPA nem armazenamento. Quem sabe
 * carregar a logo e formatar o endereco e o
 * {@code MarcaDocumentoProvider}.
 *
 * @param nome         razao social/nome fantasia no topo
 * @param linhaContato CNPJ, endereco e contato ja concatenados; {@code null}
 *                     quando nada foi configurado
 * @param rodape       texto livre do rodape (ex.: responsavel tecnico)
 * @param logo         binario da imagem, ou {@code null} — o cabecalho degrada
 *                     para so texto, que e o estado de uma instalacao nova
 */
public record MarcaDocumento(
        String nome,
        String linhaContato,
        String rodape,
        byte[] logo) {

    /**
     * Marca minima, para quando nao ha configuracao acessivel (ex.: teste
     * unitario do builder). Nunca deixa o documento sem identificacao.
     */
    public static MarcaDocumento padrao(String nome) {
        return new MarcaDocumento(nome, null, null, null);
    }

    public boolean temLogo() {
        return logo != null && logo.length > 0;
    }
}

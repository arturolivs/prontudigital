package com.prontudigital.backend.compartilhado.documento;

/**
 * Arquivo pronto para download (RF17/RF21).
 *
 * <p>Fica em memoria de proposito: relatorios e atestados sao pequenos e
 * gerados sob demanda, entao nao ha o que persistir. Anexos de paciente, que
 * sao grandes e duradouros, seguem pelo {@code ArmazenamentoService}.
 */
public record ArquivoGerado(String nomeArquivo, String contentType, byte[] conteudo) {

    public static ArquivoGerado de(String nomeBase, FormatoExportacao formato, byte[] conteudo) {
        return new ArquivoGerado(
                nomeBase + "." + formato.extensao(),
                formato.contentType(),
                conteudo);
    }
}

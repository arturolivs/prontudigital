package com.prontudigital.backend.compartilhado.documento;

/** Formatos de exportação suportados (RF21). */
public enum FormatoExportacao {

    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extensao;

    FormatoExportacao(String contentType, String extensao) {
        this.contentType = contentType;
        this.extensao = extensao;
    }

    public String contentType() {
        return contentType;
    }

    public String extensao() {
        return extensao;
    }
}

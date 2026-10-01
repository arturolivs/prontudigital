package com.prontudigital.backend.compartilhado.armazenamento;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuracao do armazenamento de anexos (prefixo {@code app.armazenamento}).
 */
@Component
@ConfigurationProperties(prefix = "app.armazenamento")
@Getter
@Setter
public class ArmazenamentoProperties {

    /** Diretorio raiz onde os arquivos sao gravados (volume Docker em producao). */
    private String diretorioBase = "dados/anexos";

    /** Tamanho maximo aceito por arquivo, em bytes (padrao 10 MB). */
    private long tamanhoMaximoBytes = 10_485_760L;

    /** Tipos MIME permitidos no upload. */
    private List<String> tiposPermitidos = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"));
}

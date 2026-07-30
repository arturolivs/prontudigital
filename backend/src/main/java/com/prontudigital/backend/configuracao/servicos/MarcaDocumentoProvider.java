package com.prontudigital.backend.configuracao.servicos;

import com.prontudigital.backend.autenticacao.entidades.Endereco;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoService;
import com.prontudigital.backend.compartilhado.documento.MarcaDocumento;
import com.prontudigital.backend.configuracao.entidades.ConfiguracaoClinica;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Traduz a {@link ConfiguracaoClinica} na {@link MarcaDocumento} que o
 * PdfBuilder consome: carrega o binario da logo e formata endereco e contato
 * em uma linha.
 *
 * <p>Fica no modulo de configuracao — e nao em {@code compartilhado.documento} —
 * para que o gerador de PDF continue sem dependencia de JPA ou armazenamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarcaDocumentoProvider {

    private final ConfiguracaoClinicaService configuracaoService;
    private final ArmazenamentoService armazenamentoService;

    @Transactional(readOnly = true)
    public MarcaDocumento obter() {
        ConfiguracaoClinica config = configuracaoService.obterConfiguracao();

        return new MarcaDocumento(
                config.getNome(),
                montarLinhaContato(config),
                config.getRodapeDocumentos(),
                carregarLogo(config));
    }

    /**
     * Falhar aqui nao pode impedir a emissao do documento: um arquivo de logo
     * ausente (volume trocado, restore parcial) degrada para cabecalho textual.
     */
    private byte[] carregarLogo(ConfiguracaoClinica config) {
        if (!config.temLogo()) {
            return null;
        }
        try (InputStream in = armazenamentoService
                .carregar(config.getLogoChave()).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.warn("Logo {} nao pode ser lida para o documento: {}",
                    config.getLogoChave(), e.getMessage());
            return null;
        }
    }

    /** CNPJ, endereco e contato em uma linha, pulando o que estiver vazio. */
    private String montarLinhaContato(ConfiguracaoClinica config) {
        List<String> partes = new ArrayList<>();

        if (preenchido(config.getCnpj())) {
            partes.add("CNPJ " + formatarCnpj(config.getCnpj()));
        }

        String endereco = formatarEndereco(config.getEndereco());
        if (preenchido(endereco)) {
            partes.add(endereco);
        }
        if (preenchido(config.getTelefone())) {
            partes.add("Tel. " + config.getTelefone());
        }
        if (preenchido(config.getEmail())) {
            partes.add(config.getEmail());
        }
        if (preenchido(config.getSite())) {
            partes.add(config.getSite());
        }

        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    private String formatarEndereco(Endereco e) {
        if (e == null || e.vazio()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (preenchido(e.getLogradouro())) {
            sb.append(e.getLogradouro());
            if (preenchido(e.getNumero())) {
                sb.append(", ").append(e.getNumero());
            }
        }
        if (preenchido(e.getBairro())) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(e.getBairro());
        }
        if (preenchido(e.getCidade())) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(e.getCidade());
            if (preenchido(e.getUf())) {
                sb.append('/').append(e.getUf());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /** O banco guarda so digitos; a mascara e de apresentacao. */
    private String formatarCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + '.' + cnpj.substring(2, 5) + '.'
                + cnpj.substring(5, 8) + '/' + cnpj.substring(8, 12) + '-'
                + cnpj.substring(12);
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}

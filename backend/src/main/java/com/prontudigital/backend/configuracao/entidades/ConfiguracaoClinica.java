package com.prontudigital.backend.configuracao.entidades;

import com.prontudigital.backend.autenticacao.entidades.Endereco;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Dados da clinica atendida por esta instalacao.
 *
 * O produto e distribuido em modelo silo — uma stack por cliente —, entao
 * existe UMA linha, fixada em id = 1 por CHECK no banco. Nao ha discriminador
 * de tenant: a instalacao inteira pertence a um cliente.
 *
 * <p>Substitui a constante {@code NOME_CLINICA} que estava no PdfBuilder:
 * cada instalacao passa a ser configurada pela tela do ADMIN, sem recompilar.
 *
 * <p>Os campos {@code templatePdf*} estao reservados para a evolucao em que o
 * admin envia o proprio layout; hoje a geracao segue programatica.
 */
@Entity
@Table(name = "configuracao_clinica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoClinica {

    /** Sempre 1 — ver ck_configuracao_clinica_linha_unica. */
    public static final Long ID_UNICO = 1L;

    @Id
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "site", length = 150)
    private String site;

    @Embedded
    private Endereco endereco;

    /**
     * Chave no {@code ArmazenamentoService} — o binario da logo nao fica no
     * banco, pelo mesmo motivo dos anexos.
     */
    @Column(name = "logo_chave", length = 255)
    private String logoChave;

    @Column(name = "logo_tipo_conteudo", length = 100)
    private String logoTipoConteudo;

    @Column(name = "rodape_documentos", columnDefinition = "TEXT")
    private String rodapeDocumentos;

    // --- Reservado para template de PDF enviavel (sem uso hoje) ---

    @Column(name = "template_pdf_chave", length = 255)
    private String templatePdfChave;

    @Column(name = "template_pdf_versao")
    private Integer templatePdfVersao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Verdadeiro quando ha logo para compor o cabecalho dos documentos. */
    public boolean temLogo() {
        return logoChave != null && !logoChave.isBlank();
    }
}

package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.AvaliacaoPulsos;
import com.prontudigital.backend.agendamento.enums.CaracteristicaBorda;
import com.prontudigital.backend.agendamento.enums.CaracteristicaPerilesional;
import com.prontudigital.backend.agendamento.enums.ClassificacaoDor;
import com.prontudigital.backend.agendamento.enums.EvolucaoFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import com.prontudigital.backend.agendamento.enums.GrauEdema;
import com.prontudigital.backend.agendamento.enums.OdorIntensidade;
import com.prontudigital.backend.agendamento.enums.SinalEvolucao;
import com.prontudigital.backend.agendamento.enums.SinalInfeccao;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "evolucoes_clinicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolucaoClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true, nullable = false)
    private Agendamento agendamento;

    // ---------------------------------------------------------
    // Dados da ferida
    // ---------------------------------------------------------
    @Column(name = "localizacao_anatomica", columnDefinition = "TEXT")
    private String localizacaoAnatomica;

    @Column(name = "etiologia", columnDefinition = "TEXT")
    private String etiologia;

    @Column(name = "tempo_evolucao")
    private String tempoEvolucao;

    // ---------------------------------------------------------
    // Mensuração
    // ---------------------------------------------------------
    @Column(name = "medida_comprimento", precision = 6, scale = 2)
    private BigDecimal medidaComprimento;

    @Column(name = "medida_largura", precision = 6, scale = 2)
    private BigDecimal medidaLargura;

    @Column(name = "medida_profundidade", precision = 6, scale = 2)
    private BigDecimal medidaProfundidade;

    @Column(name = "tunelizacao")
    private Boolean tunelizacao;

    @Column(name = "descolamento_bordas")
    private Boolean descolamentoBordas;

    // ---------------------------------------------------------
    // Leito da ferida (checkbox + percentual)
    // ---------------------------------------------------------
    @Column(name = "epitelizacao_percentual")
    private Integer epitelizacaoPercentual;

    @Column(name = "granulacao_percentual")
    private Integer granulacaoPercentual;

    @Column(name = "esfacelo_percentual")
    private Integer esfaceloPercentual;

    @Column(name = "necrose_percentual")
    private Integer necrosePercentual;

    @Column(name = "tendao_exposto")
    private Boolean tendaoExposto;

    @Column(name = "musculo_exposto")
    private Boolean musculoExposto;

    @Column(name = "osso_exposto")
    private Boolean ossoExposto;

    // ---------------------------------------------------------
    // Exsudato
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato_volume")
    private ExsudatoVolume exsudatoVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato_caracteristica")
    private ExsudatoCaracteristica exsudatoCaracteristica;

    @Enumerated(EnumType.STRING)
    @Column(name = "odor_intensidade")
    private OdorIntensidade odorIntensidade;

    // ---------------------------------------------------------
    // Bordas (múltipla escolha)
    // ---------------------------------------------------------
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "evolucao_caracteristicas_bordas",
            joinColumns = @JoinColumn(name = "evolucao_id"))
    @Column(name = "caracteristica")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<CaracteristicaBorda> caracteristicasBordas = new LinkedHashSet<>();

    // ---------------------------------------------------------
    // Pele perilesional (múltipla escolha)
    // ---------------------------------------------------------
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "evolucao_caracteristicas_perilesional",
            joinColumns = @JoinColumn(name = "evolucao_id"))
    @Column(name = "caracteristica")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<CaracteristicaPerilesional> caracteristicasPerilesional = new LinkedHashSet<>();

    // ---------------------------------------------------------
    // Sinais de infecção (múltipla escolha)
    // ---------------------------------------------------------
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "evolucao_sinais_infeccao",
            joinColumns = @JoinColumn(name = "evolucao_id"))
    @Column(name = "sinal")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<SinalInfeccao> sinaisInfeccao = new LinkedHashSet<>();

    // ---------------------------------------------------------
    // Dor
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_dor")
    private ClassificacaoDor classificacaoDor;

    // ---------------------------------------------------------
    // Avaliação vascular
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "grau_edema")
    private GrauEdema grauEdema;

    @Enumerated(EnumType.STRING)
    @Column(name = "avaliacao_pulsos")
    private AvaliacaoPulsos avaliacaoPulsos;

    // ---------------------------------------------------------
    // Evolução da ferida
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "evolucao_ferida")
    private EvolucaoFerida evolucaoFerida;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "evolucao_sinais_evolucao",
            joinColumns = @JoinColumn(name = "evolucao_id"))
    @Column(name = "sinal")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<SinalEvolucao> sinaisEvolucao = new LinkedHashSet<>();

    // ---------------------------------------------------------
    // Conduta
    // ---------------------------------------------------------
    @Column(name = "limpeza_lesao")
    private Boolean limpezaLesao;

    @Column(name = "desbridamento")
    private Boolean desbridamento;

    @Column(name = "cobertura_aplicada")
    private Boolean coberturaAplicada;

    @Column(name = "cobertura_descricao", columnDefinition = "TEXT")
    private String coberturaDescricao;

    @Column(name = "terapia_adjuvante")
    private Boolean terapiaAdjuvante;

    @Column(name = "terapia_adjuvante_descricao", columnDefinition = "TEXT")
    private String terapiaAdjuvanteDescricao;

    @Column(name = "orientacoes_fornecidas")
    private Boolean orientacoesFornecidas;

    // ---------------------------------------------------------
    // Observações
    // ---------------------------------------------------------
    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;
}

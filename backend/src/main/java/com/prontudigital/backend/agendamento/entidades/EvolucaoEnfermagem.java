package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.enums.BordasFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoTime;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacao;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import com.prontudigital.backend.agendamento.enums.TipoFerida;
import com.prontudigital.backend.agendamento.enums.TipoLimpeza;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "evolucoes_enfermagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolucaoEnfermagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true, nullable = false)
    private Agendamento agendamento;

    // ---------------------------------------------------------
    // 1. Dados da avaliacao
    // ---------------------------------------------------------
    @Column(name = "data_avaliacao")
    private LocalDate dataAvaliacao;

    @Column(name = "hora_avaliacao")
    private LocalTime horaAvaliacao;

    @Column(name = "diagnostico_medico", columnDefinition = "TEXT")
    private String diagnosticoMedico;

    @Column(name = "comorb_diabetes")
    private Boolean comorbDiabetes;

    @Column(name = "comorb_hipertensao")
    private Boolean comorbHipertensao;

    @Column(name = "comorb_doenca_vascular")
    private Boolean comorbDoencaVascular;

    @Column(name = "comorb_neuropatia")
    private Boolean comorbNeuropatia;

    @Column(name = "comorb_outras")
    private Boolean comorbOutras;

    @Column(name = "comorb_outras_detalhe", columnDefinition = "TEXT")
    private String comorbOutrasDetalhe;

    @Column(name = "medicamentos_relevantes", columnDefinition = "TEXT")
    private String medicamentosRelevantes;

    // ---------------------------------------------------------
    // 2. Avaliacao da ferida (TIME)
    // ---------------------------------------------------------
    @Column(name = "localizacao_anatomica", columnDefinition = "TEXT")
    private String localizacaoAnatomica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ferida", length = 20)
    private TipoFerida tipoFerida;

    @Column(name = "tipo_ferida_outra", columnDefinition = "TEXT")
    private String tipoFeridaOutra;

    @Column(name = "dimensoes", columnDefinition = "TEXT")
    private String dimensoes;

    @Column(name = "comprimento", precision = 6, scale = 2)
    private BigDecimal comprimento;

    @Column(name = "largura", precision = 6, scale = 2)
    private BigDecimal largura;

    @Column(name = "profundidade", precision = 6, scale = 2)
    private BigDecimal profundidade;

    @Column(name = "tunelizacao")
    private Boolean tunelizacao;

    @Column(name = "descolamento")
    private Boolean descolamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tecido_leito", length = 20)
    private TecidoLeito tecidoLeito;

    @Enumerated(EnumType.STRING)
    @Column(name = "infeccao_inflamacao", length = 30)
    private InfeccaoInflamacao infeccaoInflamacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato", length = 20)
    private ExsudatoTime exsudato;

    @Column(name = "exsudato_tipo", columnDefinition = "TEXT")
    private String exsudatoTipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "bordas", length = 20)
    private BordasFerida bordas;

    @Column(name = "pele_perilesional", columnDefinition = "TEXT")
    private String pelePerilesional;

    @Column(name = "dor_escala")
    private Integer dorEscala;

    @Column(name = "sinais_vitais", columnDefinition = "TEXT")
    private String sinaisVitais;

    @Column(name = "pa", length = 30)
    private String pa;

    @Column(name = "fc", length = 30)
    private String fc;

    @Column(name = "fr", length = 30)
    private String fr;

    @Column(name = "temp", length = 30)
    private String temp;

    // ---------------------------------------------------------
    // 3. Diagnosticos de enfermagem
    // ---------------------------------------------------------
    @Column(name = "diag_integridade_pele")
    private Boolean diagIntegridadePele;

    @Column(name = "diag_integridade_tissular")
    private Boolean diagIntegridadeTissular;

    @Column(name = "diag_risco_infeccao")
    private Boolean diagRiscoInfeccao;

    @Column(name = "diag_perfusao_ineficaz")
    private Boolean diagPerfusaoIneficaz;

    @Column(name = "diag_dor_aguda")
    private Boolean diagDorAguda;

    @Column(name = "diag_outros", columnDefinition = "TEXT")
    private String diagOutros;

    // ---------------------------------------------------------
    // 4. Conduta realizada
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "limpeza", length = 10)
    private TipoLimpeza limpeza;

    @Column(name = "limpeza_outro", columnDefinition = "TEXT")
    private String limpezaOutro;

    @Enumerated(EnumType.STRING)
    @Column(name = "desbridamento", length = 20)
    private TipoDesbridamento desbridamento;

    @Column(name = "cobertura_primaria", columnDefinition = "TEXT")
    private String coberturaPrimaria;

    @Column(name = "cobertura_secundaria", columnDefinition = "TEXT")
    private String coberturaSecundaria;

    @Column(name = "fixacao", columnDefinition = "TEXT")
    private String fixacao;

    @Column(name = "orientacoes_paciente", columnDefinition = "TEXT")
    private String orientacoesPaciente;

    // ---------------------------------------------------------
    // 5. Avaliacao da evolucao
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "avaliacao_evolucao", length = 10)
    private AvaliacaoEvolucao avaliacaoEvolucao;

    @Column(name = "reducao_area")
    private Boolean reducaoArea;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    // ---------------------------------------------------------
    // 6. Plano
    // ---------------------------------------------------------
    @Column(name = "plano_manter_conduta")
    private Boolean planoManterConduta;

    @Column(name = "plano_ajustar_cobertura")
    private Boolean planoAjustarCobertura;

    @Column(name = "plano_avaliacao_medica")
    private Boolean planoAvaliacaoMedica;

    @Column(name = "plano_solicitar_exames")
    private Boolean planoSolicitarExames;

    @Column(name = "plano_encaminhamento")
    private Boolean planoEncaminhamento;

    @Column(name = "retorno_dias")
    private Integer retornoDias;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}

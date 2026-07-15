package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.AvaliacaoEvolucao;
import com.prontudigital.backend.agendamento.enums.BordasFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoTime;
import com.prontudigital.backend.agendamento.enums.InfeccaoInflamacaoCurativo;
import com.prontudigital.backend.agendamento.enums.TecidoLeito;
import com.prontudigital.backend.agendamento.enums.TipoDesbridamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evolucoes_curativos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolucaoCurativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true, nullable = false)
    private Agendamento agendamento;

    // ---------------------------------------------------------
    // 1. Avaliacao diaria
    // ---------------------------------------------------------
    @Column(name = "comprimento", precision = 6, scale = 2)
    private BigDecimal comprimento;

    @Column(name = "largura", precision = 6, scale = 2)
    private BigDecimal largura;

    @Column(name = "profundidade", precision = 6, scale = 2)
    private BigDecimal profundidade;

    @Column(name = "area_aproximada", precision = 8, scale = 2)
    private BigDecimal areaAproximada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tecido", length = 20)
    private TecidoLeito tecido;

    @Enumerated(EnumType.STRING)
    @Column(name = "infeccao_inflamacao", length = 20)
    private InfeccaoInflamacaoCurativo infeccaoInflamacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato", length = 20)
    private ExsudatoTime exsudato;

    @Enumerated(EnumType.STRING)
    @Column(name = "bordas", length = 20)
    private BordasFerida bordas;

    @Column(name = "odor_presente")
    private Boolean odorPresente;

    @Column(name = "dor_escala")
    private Integer dorEscala;

    @Column(name = "pele_perilesional", columnDefinition = "TEXT")
    private String pelePerilesional;

    // ---------------------------------------------------------
    // 2. Intervencoes
    // ---------------------------------------------------------
    @Column(name = "limpeza_irrigacao", columnDefinition = "TEXT")
    private String limpezaIrrigacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "desbridamento", length = 20)
    private TipoDesbridamento desbridamento;

    @Column(name = "desbridamento_obs", columnDefinition = "TEXT")
    private String desbridamentoObs;

    @Column(name = "cobertura_primaria", columnDefinition = "TEXT")
    private String coberturaPrimaria;

    @Column(name = "orientacoes_paciente", columnDefinition = "TEXT")
    private String orientacoesPaciente;

    // ---------------------------------------------------------
    // 3. Avaliacao da evolucao
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "evolucao", length = 10)
    private AvaliacaoEvolucao evolucao;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    // ---------------------------------------------------------
    // 4. Plano / acoes futuras (observacao preenchida = acao marcada)
    // ---------------------------------------------------------
    @Column(name = "plano_manter_conduta", columnDefinition = "TEXT")
    private String planoManterConduta;

    @Column(name = "plano_alterar_cobertura", columnDefinition = "TEXT")
    private String planoAlterarCobertura;

    @Column(name = "plano_solicitar_exames", columnDefinition = "TEXT")
    private String planoSolicitarExames;

    @Column(name = "plano_encaminhamento", columnDefinition = "TEXT")
    private String planoEncaminhamento;

    @Column(name = "retorno_previsto", columnDefinition = "TEXT")
    private String retornoPrevisto;

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

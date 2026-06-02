package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "paciente_uuid")
    private UUID pacienteUuid;

    @Column(name = "profissional_uuid")
    private UUID profissionalUuid;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusAgendamento status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoAgendamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_procedimento")
    private TipoProcedimento tipoProcedimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "local_atendimento")
    private LocalAtendimento localAtendimento;

    @Column(name = "paciente_acamado")
    private Boolean pacienteAcamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id")
    private Agendamento avaliacao;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "localizacao_anatomica", columnDefinition = "TEXT")
    private String localizacaoAnatomica;

    @Column(name = "tipo_lesao")
    private String tipoLesao;

    @Column(name = "medida_comprimento", precision = 6, scale = 2)
    private BigDecimal medidaComprimento;

    @Column(name = "medida_largura", precision = 6, scale = 2)
    private BigDecimal medidaLargura;

    @Column(name = "medida_profundidade", precision = 6, scale = 2)
    private BigDecimal medidaProfundidade;

    @Column(name = "aspecto_leito_ferida", columnDefinition = "TEXT")
    private String aspectoLeitoFerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato_volume")
    private ExsudatoVolume exsudatoVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "exsudato_caracteristica")
    private ExsudatoCaracteristica exsudatoCaracteristica;

    @Column(name = "condicao_bordas", columnDefinition = "TEXT")
    private String condicaoBordas;

    @Column(name = "aspecto_perilesional", columnDefinition = "TEXT")
    private String aspectoPerilesional;

    @Column(name = "sinais_flogisticos")
    private Boolean sinaisFlogisticos;

    @Column(name = "presenca_odor")
    private Boolean presencaOdor;

    @Column(name = "limpeza_realizada", columnDefinition = "TEXT")
    private String limpezaRealizada;

    @Column(name = "coberturas_aplicadas", columnDefinition = "TEXT")
    private String coberturasAplicadas;

    @Column(name = "produtos_utilizados", columnDefinition = "TEXT")
    private String produtosUtilizados;

    @Column(name = "aceitacao_procedimento", columnDefinition = "TEXT")
    private String aceitacaoProcedimento;

    @Column(name = "escala_dor")
    private Integer escalaDor;

    @Column(name = "intercorrencias", columnDefinition = "TEXT")
    private String intercorrencias;

    @Column(name = "cuidados_curativo", columnDefinition = "TEXT")
    private String cuidadosCurativo;

    @Column(name = "sinais_alerta", columnDefinition = "TEXT")
    private String sinaisAlerta;

    @Column(name = "orientacao_retorno", columnDefinition = "TEXT")
    private String orientacaoRetorno;

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
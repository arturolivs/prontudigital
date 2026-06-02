package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
}

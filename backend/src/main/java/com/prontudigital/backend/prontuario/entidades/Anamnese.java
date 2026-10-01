package com.prontudigital.backend.prontuario.entidades;

import com.prontudigital.backend.prontuario.enums.RedeApoio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anamneses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anamnese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "paciente_uuid", nullable = false, unique = true)
    private UUID pacienteUuid;

    // Identificacao complementar
    @Column(name = "profissao", columnDefinition = "TEXT")
    private String profissao;

    @Column(name = "responsavel_cuidador", columnDefinition = "TEXT")
    private String responsavelCuidador;

    // Historia da ferida
    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "tempo_existencia_ferida", columnDefinition = "TEXT")
    private String tempoExistenciaFerida;

    @Column(name = "como_ferida_surgiu", columnDefinition = "TEXT")
    private String comoFeridaSurgiu;

    @Column(name = "data_inicio_aproximada")
    private LocalDate dataInicioAproximada;

    @Column(name = "tratamentos_anteriores", columnDefinition = "TEXT")
    private String tratamentosAnteriores;

    @Column(name = "curativos_previos", columnDefinition = "TEXT")
    private String curativosPrevios;

    // Historico de saude (Sim/Nao + detalhe)
    @Column(name = "diabetes_mellitus")
    private Boolean diabetesMellitus;

    @Column(name = "diabetes_mellitus_detalhe", columnDefinition = "TEXT")
    private String diabetesMellitusDetalhe;

    @Column(name = "hipertensao_arterial")
    private Boolean hipertensaoArterial;

    @Column(name = "hipertensao_arterial_detalhe", columnDefinition = "TEXT")
    private String hipertensaoArterialDetalhe;

    @Column(name = "doenca_venosa_cronica")
    private Boolean doencaVenosaCronica;

    @Column(name = "doenca_venosa_cronica_detalhe", columnDefinition = "TEXT")
    private String doencaVenosaCronicaDetalhe;

    @Column(name = "doenca_arterial_periferica")
    private Boolean doencaArterialPeriferica;

    @Column(name = "doenca_arterial_periferica_detalhe", columnDefinition = "TEXT")
    private String doencaArterialPerifericaDetalhe;

    @Column(name = "insuficiencia_renal")
    private Boolean insuficienciaRenal;

    @Column(name = "insuficiencia_renal_detalhe", columnDefinition = "TEXT")
    private String insuficienciaRenalDetalhe;

    @Column(name = "cancer")
    private Boolean cancer;

    @Column(name = "cancer_detalhe", columnDefinition = "TEXT")
    private String cancerDetalhe;

    @Column(name = "problemas_neurologicos")
    private Boolean problemasNeurologicos;

    @Column(name = "problemas_neurologicos_detalhe", columnDefinition = "TEXT")
    private String problemasNeurologicosDetalhe;

    @Column(name = "historico_cirurgias")
    private Boolean historicoCirurgias;

    @Column(name = "historico_cirurgias_detalhe", columnDefinition = "TEXT")
    private String historicoCirurgiasDetalhe;

    // Medicamentos em uso
    @Column(name = "med_antibioticos")
    private Boolean medAntibioticos;

    @Column(name = "med_anticoagulantes")
    private Boolean medAnticoagulantes;

    @Column(name = "med_corticoides")
    private Boolean medCorticoides;

    @Column(name = "med_insulina_hipoglicemiantes")
    private Boolean medInsulinaHipoglicemiantes;

    @Column(name = "med_outros_continuos")
    private Boolean medOutrosContinuos;

    // Alergias
    @Column(name = "alergia_medicamentos")
    private Boolean alergiaMedicamentos;

    @Column(name = "alergia_produtos_topicos")
    private Boolean alergiaProdutosTopicos;

    @Column(name = "alergia_curativos_adesivos")
    private Boolean alergiaCurativosAdesivos;

    // Habitos de vida
    @Column(name = "tabagismo")
    private Boolean tabagismo;

    @Column(name = "consumo_alcool")
    private Boolean consumoAlcool;

    @Column(name = "alimentacao_estado_nutricional", columnDefinition = "TEXT")
    private String alimentacaoEstadoNutricional;

    @Column(name = "ingestao_hidrica")
    private Boolean ingestaoHidrica;

    @Column(name = "ingestao_hidrica_detalhe", columnDefinition = "TEXT")
    private String ingestaoHidricaDetalhe;

    // Mobilidade
    @Column(name = "deambula_sozinho")
    private Boolean deambulaSozinho;

    @Column(name = "deambula_sozinho_detalhe", columnDefinition = "TEXT")
    private String deambulaSozinhoDetalhe;

    @Column(name = "acamado_ou_cadeirante")
    private Boolean acamadoOuCadeirante;

    @Column(name = "acamado_ou_cadeirante_detalhe", columnDefinition = "TEXT")
    private String acamadoOuCadeiranteDetalhe;

    @Column(name = "uso_dispositivos")
    private Boolean usoDispositivos;

    @Column(name = "uso_dispositivos_detalhe", columnDefinition = "TEXT")
    private String usoDispositivosDetalhe;

    @Column(name = "mudanca_posicao_leito")
    private Boolean mudancaPosicaoLeito;

    @Column(name = "mudanca_posicao_leito_detalhe", columnDefinition = "TEXT")
    private String mudancaPosicaoLeitoDetalhe;

    // Outros
    @Column(name = "exames_recentes")
    private Boolean examesRecentes;

    @Enumerated(EnumType.STRING)
    @Column(name = "rede_apoio", length = 40)
    private RedeApoio redeApoio;

    @Column(name = "acompanhamento_medico")
    private Boolean acompanhamentoMedico;

    @Column(name = "acompanhamento_medico_detalhe", columnDefinition = "TEXT")
    private String acompanhamentoMedicoDetalhe;

    @Column(name = "registrado_por")
    private UUID registradoPor;

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

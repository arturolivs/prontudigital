package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @OneToOne(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private EvolucaoClinica evolucaoClinica;

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
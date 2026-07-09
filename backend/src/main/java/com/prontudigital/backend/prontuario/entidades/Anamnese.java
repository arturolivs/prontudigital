package com.prontudigital.backend.prontuario.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "queixa_principal", columnDefinition = "TEXT")
    private String queixaPrincipal;

    @Column(name = "historico_doenca_atual", columnDefinition = "TEXT")
    private String historicoDoencaAtual;

    @Column(name = "historico_medico_pregresso", columnDefinition = "TEXT")
    private String historicoMedicoPregresso;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "medicamentos_em_uso", columnDefinition = "TEXT")
    private String medicamentosEmUso;

    @Column(name = "historico_familiar", columnDefinition = "TEXT")
    private String historicoFamiliar;

    @Column(name = "habitos", columnDefinition = "TEXT")
    private String habitos;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

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

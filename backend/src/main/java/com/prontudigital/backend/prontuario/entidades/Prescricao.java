package com.prontudigital.backend.prontuario.entidades;

import com.prontudigital.backend.prontuario.enums.TipoPrescricao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Prescricao de medicamento ou cuidado de enfermagem (RF16).
 *
 * Relacao 1:N com o paciente: um paciente possui varias prescricoes ao longo do
 * tempo. Vinculada ao paciente e, opcionalmente, a um atendimento (agendamento).
 */
@Entity
@Table(name = "prescricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "paciente_uuid", nullable = false)
    private UUID pacienteUuid;

    @Column(name = "agendamento_uuid")
    private UUID agendamentoUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPrescricao tipo;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "posologia", columnDefinition = "TEXT")
    private String posologia;

    @Column(name = "frequencia")
    private String frequencia;

    @Column(name = "duracao")
    private String duracao;

    @Column(name = "orientacoes", columnDefinition = "TEXT")
    private String orientacoes;

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

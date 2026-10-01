package com.prontudigital.backend.agendamento.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Janela semanal de atendimento do profissional (RF05).
 *
 * <p>Complementa {@link BloqueioRecorrente} pelo lado oposto: o bloqueio diz
 * quando o profissional <em>nao</em> atende dentro do expediente; aqui fica o
 * expediente em si. Um agendamento precisa caber inteiro em uma destas janelas.
 */
@Entity
@Table(name = "horarios_trabalho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "profissional_uuid", nullable = false)
    private UUID profissionalUuid;

    /** ISO-8601: 1=Segunda … 6=Sábado, 7=Domingo */
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    /** Verdadeiro quando o intervalo cabe inteiro nesta janela. */
    public boolean contem(LocalTime inicio, LocalTime fim) {
        return !inicio.isBefore(horaInicio) && !fim.isAfter(horaFim);
    }
}

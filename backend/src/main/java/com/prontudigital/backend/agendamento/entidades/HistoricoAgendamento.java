package com.prontudigital.backend.agendamento.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_agendamentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricoAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;

    @Column(name = "inicio_anterior", nullable = false)
    private LocalDateTime inicioAnterior;

    @Column(name = "fim_anterior", nullable = false)
    private LocalDateTime fimAnterior;

    @Column(name = "inicio_novo", nullable = false)
    private LocalDateTime inicioNovo;

    @Column(name = "fim_novo", nullable = false)
    private LocalDateTime fimNovo;

    @Column(length = 255)
    private String motivo;

    @Column(name = "alterado_por", nullable = false)
    private UUID alteradoPor;

    @CreationTimestamp
    @Column(name = "alterado_em", updatable = false)
    private LocalDateTime alteradoEm;
}
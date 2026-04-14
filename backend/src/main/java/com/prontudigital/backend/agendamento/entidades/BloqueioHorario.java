package com.prontudigital.backend.agendamento.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bloqueios_horario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueioHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "profissional_uuid")
    private UUID profissionalUuid;

    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @Column(name = "motivo")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoBloqueio tipo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
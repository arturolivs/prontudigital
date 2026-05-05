package com.prontudigital.backend.agendamento.entidades;

import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fila_espera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "paciente_uuid")
    private UUID pacienteUuid;

    @Column(name = "profissional_uuid")
    private UUID profissionalUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_preferido")
    private TipoAgendamento tipoPreferido;

    @Column(name = "data_preferida")
    private LocalDateTime dataPreferida;

    @Column(name = "prioridade")
    private Integer prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusFilaEspera status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
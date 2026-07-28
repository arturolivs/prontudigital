package com.prontudigital.backend.prontuario.entidades;

import com.prontudigital.backend.prontuario.enums.TipoAtestado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Atestado emitido ao paciente (RF17).
 *
 * <p>O PDF nao e persistido: e regerado sob demanda a partir destes campos,
 * entao reemitir um atestado antigo sempre reflete o registro atual.
 */
@Entity
@Table(name = "atestados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atestado {

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
    private TipoAtestado tipo;

    @Column(name = "dias_afastamento")
    private Integer diasAfastamento;

    @Column(name = "cid", length = 10)
    private String cid;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "emitido_por")
    private UUID emitidoPor;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}

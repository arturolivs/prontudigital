package com.prontudigital.backend.prontuario.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Anexo de exame ou documento do prontuario (RF15).
 *
 * Relacao 1:N com o paciente: um paciente possui varios anexos ao longo do tempo,
 * opcionalmente vinculados a um atendimento (agendamento). O conteudo binario nao
 * e persistido aqui; guarda-se apenas a {@link #chaveArmazenamento} usada pelo
 * {@code ArmazenamentoService} para recuperar o arquivo.
 */
@Entity
@Table(name = "anexos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "paciente_uuid", nullable = false)
    private UUID pacienteUuid;

    @Column(name = "agendamento_uuid")
    private UUID agendamentoUuid;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "chave_armazenamento", nullable = false, length = 512)
    private String chaveArmazenamento;

    @Column(name = "registrado_por")
    private UUID registradoPor;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}

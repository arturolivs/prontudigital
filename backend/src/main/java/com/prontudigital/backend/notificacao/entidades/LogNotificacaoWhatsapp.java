package com.prontudigital.backend.notificacao.entidades;

import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.enums.TipoNotificacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log_notificacoes_whatsapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogNotificacaoWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;

    @Column(name = "paciente_uuid", nullable = false)
    private UUID pacienteUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusNotificacao status;

    @Column(name = "token_confirmacao", unique = true)
    private UUID tokenConfirmacao;

    @Column(name = "token_expira_em")
    private LocalDateTime tokenExpiraEm;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Builder.Default
    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    /** wamid devolvido pela Cloud API; chave de correlacao do webhook de status. */
    @Column(name = "mensagem_id")
    private String mensagemId;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm;

    @Column(name = "erro_codigo")
    private String erroCodigo;

    @Column(name = "erro_detalhe", columnDefinition = "TEXT")
    private String erroDetalhe;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}

package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_recuperacao_senha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "codigo", nullable = false, length = 6)
    private String codigo;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Builder.Default
    @Column(name = "utilizado", nullable = false)
    private Boolean utilizado = false;

    @Builder.Default
    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}

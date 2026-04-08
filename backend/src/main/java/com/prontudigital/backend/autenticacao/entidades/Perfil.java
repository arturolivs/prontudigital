package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "perfis")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;

    @Column(name="nome", nullable = false, unique = true, length = 20)
    private String nome;  // Ex: "ROLE_ADMIN", "ROLE_USER" — mantém convenção Spring Security

    @Column(name = "descricao", length = 200)
    private String descricao;
}
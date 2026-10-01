package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuario_perfis")
public class UsuarioPerfil {

    @EmbeddedId
    private UsuarioPerfilId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("perfilId")
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    @Column(name = "atribuido_em")
    private Instant atribuidoEm;

    @PrePersist
    protected void onCreate() {
        atribuidoEm = Instant.now();
    }
}
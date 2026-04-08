package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioPerfilId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "perfil_id")
    private Long perfilId;
}
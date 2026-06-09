package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(length = 100, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "acesso_ativado", nullable = false)
    @Builder.Default
    private Boolean acessoAtivado = false;

    @Builder.Default
    @OneToMany(mappedBy = "usuario", fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioPerfil> usuarioPerfis = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = Instant.now();
    }

    public void adicionarPerfil(Perfil perfil) {
        UsuarioPerfil up = UsuarioPerfil.builder()
                .id(new UsuarioPerfilId(this.id, perfil.getId()))
                .usuario(this)
                .perfil(perfil)
                .build();
        this.usuarioPerfis.add(up);
    }

    public void removerPerfil(Perfil perfil) {
        this.usuarioPerfis.removeIf(up -> up.getPerfil().equals(perfil));
    }

    public Set<Perfil> getPerfis() {
        Set<Perfil> perfis = new HashSet<>();
        for (UsuarioPerfil up : usuarioPerfis) {
            perfis.add(up.getPerfil());
        }
        return perfis;
    }
}
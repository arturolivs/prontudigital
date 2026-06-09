package com.prontudigital.backend.autenticacao.repositorios;

import com.prontudigital.backend.autenticacao.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    boolean existsByUuid(UUID uuid);
    boolean existsByUsername(String username);
    boolean existsByTelefone(String telefone);
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUuid(UUID uuid);
    Optional<Usuario> findByTelefone(String telefone);
}

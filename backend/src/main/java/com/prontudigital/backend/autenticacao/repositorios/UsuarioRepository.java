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

    // RF04/RF05 — unicidade de CPF e COREN. As variantes ...AndIdNot ignoram o
    // proprio registro na edicao; as simples valem para o cadastro novo, que
    // ainda nao tem id (id <> NULL nunca casaria).
    boolean existsByCpf(String cpf);
    boolean existsByCoren(String coren);
    boolean existsByCpfAndIdNot(String cpf, Long id);
    boolean existsByCorenAndIdNot(String coren, Long id);

    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUuid(UUID uuid);
    Optional<Usuario> findByTelefone(String telefone);
}

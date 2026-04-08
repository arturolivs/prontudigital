package com.prontudigital.backend.autenticacao.repositorios;

import com.prontudigital.backend.autenticacao.entidades.RefreshToken;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
 //   Optional<RefreshToken> findByToken(String token);
  //  List<RefreshToken> findAllByUsuario(Usuario usuario);
  //  void deletarPorUsuario(Usuario usuario);
 //   void deletarPorToken(String token);
  //  boolean existsByTokenAndRevogado(String token, boolean revogado);

  //  @Modifying
  //  @Query("DELETE FROM RefreshToken rt WHERE rt.expiraEm < :agora")
  //  void deletarTodosExpiradosDesde(Instant agora);
}

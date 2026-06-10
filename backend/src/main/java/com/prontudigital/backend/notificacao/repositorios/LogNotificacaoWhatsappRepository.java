package com.prontudigital.backend.notificacao.repositorios;

import com.prontudigital.backend.notificacao.entidades.LogNotificacaoWhatsapp;
import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.enums.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LogNotificacaoWhatsappRepository extends JpaRepository<LogNotificacaoWhatsapp, Long> {

    boolean existsByAgendamentoIdAndTipo(Long agendamentoId, TipoNotificacao tipo);

    boolean existsByAgendamentoIdAndTipoAndStatus(
            Long agendamentoId, TipoNotificacao tipo, StatusNotificacao status);

    Optional<LogNotificacaoWhatsapp> findByTokenConfirmacao(UUID token);

    Optional<LogNotificacaoWhatsapp> findByAgendamentoIdAndTipoAndStatus(
            Long agendamentoId, TipoNotificacao tipo, StatusNotificacao status);
}

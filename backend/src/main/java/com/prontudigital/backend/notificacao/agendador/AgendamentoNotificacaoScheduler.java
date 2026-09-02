package com.prontudigital.backend.notificacao.agendador;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.notificacao.servicos.NotificacaoWhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgendamentoNotificacaoScheduler {

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoWhatsappService notificacaoService;
    private final Clock clock;

    @Value("${app.notificacoes.habilitadas:true}")
    private boolean notificacoesHabilitadas;


    @Scheduled(cron = "0 */15 * * * *")
    public void processarLembretes48h() {
        if (!notificacoesHabilitadas) {
            log.debug("[WHATSAPP][SCHEDULER 48h] Execucao ignorada: notificacoes desabilitadas "
                    + "(app.notificacoes.habilitadas=false)");
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime de = agora.plusHours(46);
        LocalDateTime ate = agora.plusHours(50);
        log.debug("[WHATSAPP][SCHEDULER 48h] Buscando agendamentos com inicio entre {} e {}", de, ate);

        List<Agendamento> agendamentos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                de,
                ate);

        if (agendamentos.isEmpty()) {
            log.debug("[WHATSAPP][SCHEDULER 48h] Nenhum agendamento na janela - nada a enviar");
            return;
        }

        log.info("[WHATSAPP][SCHEDULER 48h] {} agendamento(s) encontrado(s) para lembrete", agendamentos.size());
        int falhas = 0;
        for (Agendamento a : agendamentos) {
            try {
                notificacaoService.enviarLembrete48h(a);
            } catch (Exception e) {
                falhas++;
                log.error("[WHATSAPP][SCHEDULER 48h] Erro ao enviar lembrete do agendamento {}: {}",
                        a.getId(), e.getMessage(), e);
            }
        }
        log.info("[WHATSAPP][SCHEDULER 48h] Execucao concluida: {} processado(s), {} com erro",
                agendamentos.size(), falhas);
    }

    /**
     * Solicita a confirmacao de presenca na janela de 24h antes da consulta.
     * O envio duplicado e barrado pelo log de notificacoes no servico.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void processarConfirmacoes24h() {
        if (!notificacoesHabilitadas) {
            log.debug("[WHATSAPP][SCHEDULER 24h] Execucao ignorada: notificacoes desabilitadas "
                    + "(app.notificacoes.habilitadas=false)");
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime de = agora.plusHours(22);
        LocalDateTime ate = agora.plusHours(26);
        log.debug("[WHATSAPP][SCHEDULER 24h] Buscando agendamentos com inicio entre {} e {}", de, ate);

        List<Agendamento> agendamentos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                de,
                ate);

        if (agendamentos.isEmpty()) {
            log.debug("[WHATSAPP][SCHEDULER 24h] Nenhum agendamento na janela - nada a enviar");
            return;
        }

        log.info("[WHATSAPP][SCHEDULER 24h] {} agendamento(s) encontrado(s) para confirmação", agendamentos.size());
        int falhas = 0;
        for (Agendamento a : agendamentos) {
            try {
                notificacaoService.enviarSolicitacaoConfirmacao24h(a);
            } catch (Exception e) {
                falhas++;
                log.error("[WHATSAPP][SCHEDULER 24h] Erro ao enviar confirmação do agendamento {}: {}",
                        a.getId(), e.getMessage(), e);
            }
        }
        log.info("[WHATSAPP][SCHEDULER 24h] Execucao concluida: {} processado(s), {} com erro",
                agendamentos.size(), falhas);
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void processarExpiracoes() {
        if (!notificacoesHabilitadas) {
            log.debug("[WHATSAPP][SCHEDULER EXPIRACAO] Execucao ignorada: notificacoes desabilitadas "
                    + "(app.notificacoes.habilitadas=false)");
            return;
        }

        log.debug("[WHATSAPP][SCHEDULER EXPIRACAO] Verificando agendamentos não confirmados na janela de 2h");
        try {
            notificacaoService.processarNaoConfirmados();
            log.debug("[WHATSAPP][SCHEDULER EXPIRACAO] Execucao concluida");
        } catch (Exception e) {
            log.error("[WHATSAPP][SCHEDULER EXPIRACAO] Erro ao processar expirações de agendamentos: {}",
                    e.getMessage(), e);
        }
    }
}

package com.prontudigital.backend.agendamento.agendador;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.servicos.NotificacaoWhatsappService;
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
        if (!notificacoesHabilitadas) return;

        LocalDateTime agora = LocalDateTime.now(clock);
        List<Agendamento> agendamentos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                agora.plusHours(46),
                agora.plusHours(50));

        if (agendamentos.isEmpty()) return;

        log.info("Scheduler 48h: {} agendamento(s) encontrado(s) para lembrete", agendamentos.size());
        agendamentos.forEach(a -> {
            try {
                notificacaoService.enviarLembrete48h(a);
            } catch (Exception e) {
                log.error("Erro ao enviar lembrete 48h para agendamento {}: {}", a.getId(), e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void processarConfirmacoes24h() {
        if (!notificacoesHabilitadas) return;

        LocalDateTime agora = LocalDateTime.now(clock);
        List<Agendamento> agendamentos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                agora.plusHours(22),
                agora.plusHours(26));

        if (agendamentos.isEmpty()) return;

        log.info("Scheduler 24h: {} agendamento(s) encontrado(s) para confirmação", agendamentos.size());
        agendamentos.forEach(a -> {
            try {
                notificacaoService.enviarSolicitacaoConfirmacao24h(a);
            } catch (Exception e) {
                log.error("Erro ao enviar confirmação 24h para agendamento {}: {}", a.getId(), e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void processarExpiracoes() {
        if (!notificacoesHabilitadas) return;

        log.debug("Scheduler expiração: verificando agendamentos não confirmados na janela de 2h");
        try {
            notificacaoService.processarNaoConfirmados();
        } catch (Exception e) {
            log.error("Erro ao processar expirações de agendamentos: {}", e.getMessage());
        }
    }
}

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

    /** Tempo de espera, apos a criacao do agendamento, para disparar a confirmacao. */
    @Value("${app.notificacoes.confirmacao.atraso-minutos:1}")
    private long confirmacaoAtrasoMinutos;

    /**
     * Tamanho da janela retroativa varrida a cada execucao. Mantem o resultado
     * limitado e garante o reenvio de agendamentos criados enquanto a aplicacao
     * esteve fora do ar.
     */
    @Value("${app.notificacoes.confirmacao.janela-minutos:60}")
    private long confirmacaoJanelaMinutos;


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
     * Dispara a solicitação de confirmação logo após a criação do agendamento
     * (por padrão, 1 minuto depois). O envio duplicado é barrado pelo log de
     * notificações no serviço.
     */
    @Scheduled(cron = "0 * * * * *")
    public void processarConfirmacoesAgendamento() {
        if (!notificacoesHabilitadas) {
            log.debug("[WHATSAPP][SCHEDULER CONFIRMACAO] Execucao ignorada: notificacoes desabilitadas "
                    + "(app.notificacoes.habilitadas=false)");
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime criadoAte = agora.minusMinutes(confirmacaoAtrasoMinutos);
        LocalDateTime criadoDe = criadoAte.minusMinutes(confirmacaoJanelaMinutos);
        log.debug("[WHATSAPP][SCHEDULER CONFIRMACAO] Buscando agendamentos criados entre {} e {} "
                        + "(atraso={} min, janela={} min)",
                criadoDe, criadoAte, confirmacaoAtrasoMinutos, confirmacaoJanelaMinutos);

        List<Agendamento> agendamentos = agendamentoRepository.findPendentesDeConfirmacaoPorCriacao(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                criadoDe,
                criadoAte,
                agora);

        if (agendamentos.isEmpty()) {
            log.debug("[WHATSAPP][SCHEDULER CONFIRMACAO] Nenhum agendamento pendente de confirmacao na janela");
            return;
        }

        log.info("[WHATSAPP][SCHEDULER CONFIRMACAO] {} agendamento(s) criado(s) há {} min ou mais para confirmação",
                agendamentos.size(), confirmacaoAtrasoMinutos);
        int falhas = 0;
        for (Agendamento a : agendamentos) {
            try {
                notificacaoService.enviarSolicitacaoConfirmacao24h(a);
            } catch (Exception e) {
                falhas++;
                log.error("[WHATSAPP][SCHEDULER CONFIRMACAO] Erro ao enviar confirmação do agendamento {}: {}",
                        a.getId(), e.getMessage(), e);
            }
        }
        log.info("[WHATSAPP][SCHEDULER CONFIRMACAO] Execucao concluida: {} processado(s), {} com erro",
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

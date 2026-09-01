package com.prontudigital.backend.notificacao.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.FilaEsperaRepository;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.notificacao.entidades.LogNotificacaoWhatsapp;
import com.prontudigital.backend.notificacao.enums.StatusNotificacao;
import com.prontudigital.backend.notificacao.enums.TipoNotificacao;
import com.prontudigital.backend.notificacao.excecoes.TokenConfirmacaoInvalidoException;
import com.prontudigital.backend.notificacao.repositorios.LogNotificacaoWhatsappRepository;
import com.prontudigital.backend.notificacao.servicos.NotificacaoWhatsappService;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoWhatsappServiceImpl implements NotificacaoWhatsappService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final LogNotificacaoWhatsappRepository logRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final FilaEsperaRepository filaEsperaRepository;
    private final UsuarioService usuarioService;
    private final WhatsappCloudApiClient whatsappCliente;
    private final Clock clock;

    @Value("${app.notificacoes.url-base-confirmacao}")
    private String urlBaseConfirmacao;

    @Override
    @Transactional
    public void enviarLembrete48h(Agendamento agendamento) {
        log.debug("[WHATSAPP][LEMBRETE_48H] Iniciando processamento do agendamento {} (inicio em {})",
                agendamento.getId(), agendamento.getInicioEm());

        if (logRepository.existsByAgendamentoIdAndTipo(agendamento.getId(), TipoNotificacao.LEMBRETE_48H)) {
            log.debug("[WHATSAPP][LEMBRETE_48H] Agendamento {} ignorado: lembrete ja registrado anteriormente",
                    agendamento.getId());
            return;
        }

        UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
        UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

        if (paciente.telefone() == null || paciente.telefone().isBlank()) {
            log.warn("[WHATSAPP][LEMBRETE_48H] Agendamento {} sem telefone cadastrado para o paciente {} - envio cancelado",
                    agendamento.getId(), agendamento.getPacienteUuid());
            return;
        }

        String mensagem = String.format(
                "Olá, %s! Lembrete: você tem uma consulta com %s marcada para %s. " +
                "Em caso de dúvidas, entre em contato conosco.",
                paciente.nomeCompleto(),
                profissional.nomeCompleto(),
                agendamento.getInicioEm().format(FORMATTER));

        LogNotificacaoWhatsapp registro = LogNotificacaoWhatsapp.builder()
                .agendamentoId(agendamento.getId())
                .pacienteUuid(agendamento.getPacienteUuid())
                .tipo(TipoNotificacao.LEMBRETE_48H)
                .status(StatusNotificacao.PENDENTE)
                .telefone(paciente.telefone())
                .mensagem(mensagem)
                .tentativas(0)
                .build();

        try {
            enviarWhatsApp(paciente.telefone(), mensagem);
            registro.setStatus(StatusNotificacao.ENVIADO);
            registro.setEnviadoEm(LocalDateTime.now(clock));
            registro.setTentativas(1);
            log.info("[WHATSAPP][LEMBRETE_48H] Lembrete enviado para o agendamento {} (paciente {}, telefone {})",
                    agendamento.getId(), agendamento.getPacienteUuid(),
                    WhatsappCloudApiClient.mascararTelefone(paciente.telefone()));
        } catch (Exception e) {
            log.error("[WHATSAPP][LEMBRETE_48H] Falha ao enviar lembrete do agendamento {} (telefone {}): {}",
                    agendamento.getId(), WhatsappCloudApiClient.mascararTelefone(paciente.telefone()),
                    e.getMessage(), e);
            registro.setStatus(StatusNotificacao.FALHA);
            registro.setTentativas(1);
        }

        logRepository.save(registro);
        log.debug("[WHATSAPP][LEMBRETE_48H] Log de notificacao do agendamento {} salvo com status {}",
                agendamento.getId(), registro.getStatus());
    }

    @Override
    @Transactional
    public void enviarSolicitacaoConfirmacao24h(Agendamento agendamento) {
        log.debug("[WHATSAPP][CONFIRMACAO] Iniciando processamento do agendamento {} (criado em {}, inicio em {})",
                agendamento.getId(), agendamento.getCriadoEm(), agendamento.getInicioEm());

        if (logRepository.existsByAgendamentoIdAndTipo(agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H)) {
            log.debug("[WHATSAPP][CONFIRMACAO] Agendamento {} ignorado: solicitacao ja registrada anteriormente",
                    agendamento.getId());
            return;
        }

        UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
        UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

        if (paciente.telefone() == null || paciente.telefone().isBlank()) {
            log.warn("[WHATSAPP][CONFIRMACAO] Agendamento {} sem telefone cadastrado para o paciente {} - envio cancelado",
                    agendamento.getId(), agendamento.getPacienteUuid());
            return;
        }

        UUID token = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime tokenExpiraEm = agendamento.getInicioEm().minusHours(2);
        if (tokenExpiraEm.isBefore(agora)) {
            // Agendamento criado com menos de 2h de antecedência: o link vale
            // até o horário da consulta, senão nasceria expirado.
            log.info("[WHATSAPP][CONFIRMACAO] Agendamento {} criado com menos de 2h de antecedencia: "
                            + "validade do link ajustada de {} para {}",
                    agendamento.getId(), tokenExpiraEm, agendamento.getInicioEm());
            tokenExpiraEm = agendamento.getInicioEm();
        }

        String linkConfirmar = urlBaseConfirmacao + "/" + token + "/confirmar";
        String linkRecusar = urlBaseConfirmacao + "/" + token + "/recusar";
        log.debug("[WHATSAPP][CONFIRMACAO] Agendamento {}: token {} gerado, valido ate {}",
                agendamento.getId(), resumirToken(token), tokenExpiraEm);

        String mensagem = String.format(
                "Olá, %s! Sua consulta com %s está marcada para %s. " +
                "Por favor, confirme sua presença:%n" +
                "✅ CONFIRMAR: %s%n" +
                "❌ CANCELAR: %s%n" +
                "Se não responder até %s, a vaga será liberada.",
                paciente.nomeCompleto(),
                profissional.nomeCompleto(),
                agendamento.getInicioEm().format(FORMATTER),
                linkConfirmar,
                linkRecusar,
                tokenExpiraEm.format(FORMATTER));

        LogNotificacaoWhatsapp registro = LogNotificacaoWhatsapp.builder()
                .agendamentoId(agendamento.getId())
                .pacienteUuid(agendamento.getPacienteUuid())
                .tipo(TipoNotificacao.CONFIRMACAO_24H)
                .status(StatusNotificacao.PENDENTE)
                .tokenConfirmacao(token)
                .tokenExpiraEm(tokenExpiraEm)
                .telefone(paciente.telefone())
                .mensagem(mensagem)
                .tentativas(0)
                .build();

        try {
            enviarWhatsApp(paciente.telefone(), mensagem);
            registro.setStatus(StatusNotificacao.ENVIADO);
            registro.setEnviadoEm(agora);
            registro.setTentativas(1);
            log.info("[WHATSAPP][CONFIRMACAO] Solicitacao enviada para o agendamento {} "
                            + "(paciente {}, telefone {}, token {}, valido ate {})",
                    agendamento.getId(), agendamento.getPacienteUuid(),
                    WhatsappCloudApiClient.mascararTelefone(paciente.telefone()),
                    resumirToken(token), tokenExpiraEm);
        } catch (Exception e) {
            log.error("[WHATSAPP][CONFIRMACAO] Falha ao enviar solicitação de confirmação para agendamento {} (telefone {}): {}",
                    agendamento.getId(), WhatsappCloudApiClient.mascararTelefone(paciente.telefone()),
                    e.getMessage(), e);
            registro.setStatus(StatusNotificacao.FALHA);
            registro.setTentativas(1);
        }

        logRepository.save(registro);
        log.debug("[WHATSAPP][CONFIRMACAO] Log de notificacao do agendamento {} salvo com status {}",
                agendamento.getId(), registro.getStatus());
    }

    @Override
    @Transactional
    public void confirmarViaToken(UUID token) {
        log.info("[WHATSAPP][RESPOSTA] Confirmacao recebida para o token {}", resumirToken(token));

        LogNotificacaoWhatsapp entrada = logRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> {
                    log.warn("[WHATSAPP][RESPOSTA] Token {} nao encontrado no log de notificacoes",
                            resumirToken(token));
                    return new TokenConfirmacaoInvalidoException("Token inválido ou não encontrado");
                });

        validarTokenUtilizavel(entrada);

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isAfter(entrada.getTokenExpiraEm())) {
            log.warn("[WHATSAPP][RESPOSTA] Token {} do agendamento {} expirou em {} - confirmacao recusada",
                    resumirToken(token), entrada.getAgendamentoId(), entrada.getTokenExpiraEm());
            entrada.setStatus(StatusNotificacao.EXPIRADO);
            logRepository.save(entrada);
            throw new TokenConfirmacaoInvalidoException(
                    Mensagens.get("notificacao.prazo-encerrado"));
        }

        Agendamento agendamento = agendamentoRepository.findById(entrada.getAgendamentoId())
                .orElseThrow(() -> {
                    log.error("[WHATSAPP][RESPOSTA] Token {} referencia o agendamento {}, que nao existe mais",
                            resumirToken(token), entrada.getAgendamentoId());
                    return new TokenConfirmacaoInvalidoException("Agendamento não encontrado");
                });

        log.debug("[WHATSAPP][RESPOSTA] Agendamento {} muda de {} para CONFIRMADO",
                agendamento.getId(), agendamento.getStatus());
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);

        entrada.setStatus(StatusNotificacao.CONFIRMADO);
        entrada.setRespondidoEm(agora);
        logRepository.save(entrada);

        log.info("[WHATSAPP][RESPOSTA] Agendamento {} confirmado via WhatsApp pelo paciente {}",
                agendamento.getId(), agendamento.getPacienteUuid());
    }

    @Override
    @Transactional
    public void recusarViaToken(UUID token) {
        log.info("[WHATSAPP][RESPOSTA] Recusa recebida para o token {}", resumirToken(token));

        LogNotificacaoWhatsapp entrada = logRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> {
                    log.warn("[WHATSAPP][RESPOSTA] Token {} nao encontrado no log de notificacoes",
                            resumirToken(token));
                    return new TokenConfirmacaoInvalidoException("Token inválido ou não encontrado");
                });

        validarTokenUtilizavel(entrada);

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isAfter(entrada.getTokenExpiraEm())) {
            log.warn("[WHATSAPP][RESPOSTA] Token {} do agendamento {} expirou em {} - recusa nao processada",
                    resumirToken(token), entrada.getAgendamentoId(), entrada.getTokenExpiraEm());
            entrada.setStatus(StatusNotificacao.EXPIRADO);
            logRepository.save(entrada);
            throw new TokenConfirmacaoInvalidoException(
                    Mensagens.get("notificacao.prazo-encerrado"));
        }

        Agendamento agendamento = agendamentoRepository.findById(entrada.getAgendamentoId())
                .orElseThrow(() -> {
                    log.error("[WHATSAPP][RESPOSTA] Token {} referencia o agendamento {}, que nao existe mais",
                            resumirToken(token), entrada.getAgendamentoId());
                    return new TokenConfirmacaoInvalidoException("Agendamento não encontrado");
                });

        liberarAgendamento(agendamento);

        entrada.setStatus(StatusNotificacao.RECUSADO);
        entrada.setRespondidoEm(agora);
        logRepository.save(entrada);

        log.info("[WHATSAPP][RESPOSTA] Agendamento {} recusado via WhatsApp pelo paciente {}",
                agendamento.getId(), agendamento.getPacienteUuid());
    }

    @Override
    @Transactional
    public void processarNaoConfirmados() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.plusHours(2);

        List<Agendamento> candidatos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                agora, limite);

        log.debug("[WHATSAPP][EXPIRACAO] {} agendamento(s) ainda nao confirmado(s) entre {} e {}",
                candidatos.size(), agora, limite);

        int liberados = 0;
        for (Agendamento agendamento : candidatos) {
            if (!logRepository.existsByAgendamentoIdAndTipoAndStatus(
                    agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO)) {
                log.debug("[WHATSAPP][EXPIRACAO] Agendamento {} mantido: nenhuma solicitacao de confirmacao enviada",
                        agendamento.getId());
                continue;
            }

            liberarAgendamento(agendamento);

            logRepository.findByAgendamentoIdAndTipoAndStatus(
                    agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO)
                    .ifPresent(entrada -> {
                        entrada.setStatus(StatusNotificacao.EXPIRADO);
                        logRepository.save(entrada);
                    });

            liberados++;
            log.info("[WHATSAPP][EXPIRACAO] Agendamento {} cancelado por falta de confirmação dentro de 2h",
                    agendamento.getId());
        }

        if (liberados > 0) {
            log.info("[WHATSAPP][EXPIRACAO] {} de {} agendamento(s) liberado(s) por falta de confirmacao",
                    liberados, candidatos.size());
        }
    }

    private void liberarAgendamento(Agendamento agendamento) {
        log.debug("[WHATSAPP] Liberando agendamento {} (status atual {}) do paciente {}",
                agendamento.getId(), agendamento.getStatus(), agendamento.getPacienteUuid());

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);

        FilaEspera entrada = FilaEspera.builder()
                .pacienteUuid(agendamento.getPacienteUuid())
                .profissionalUuid(agendamento.getProfissionalUuid())
                .tipoPreferido(agendamento.getTipo())
                .dataPreferida(agendamento.getInicioEm())
                .prioridade(-100)
                .status(StatusFilaEspera.ATIVO)
                .build();

        filaEsperaRepository.save(entrada);
        log.info("[WHATSAPP] Agendamento {} cancelado; paciente {} devolvido a fila de espera",
                agendamento.getId(), agendamento.getPacienteUuid());
    }

    private void validarTokenUtilizavel(LogNotificacaoWhatsapp entrada) {
        if (entrada.getStatus() == StatusNotificacao.CONFIRMADO
                || entrada.getStatus() == StatusNotificacao.RECUSADO
                || entrada.getStatus() == StatusNotificacao.EXPIRADO) {
            log.warn("[WHATSAPP][RESPOSTA] Token {} do agendamento {} ja utilizado (status {}) - resposta ignorada",
                    resumirToken(entrada.getTokenConfirmacao()), entrada.getAgendamentoId(), entrada.getStatus());
            throw new TokenConfirmacaoInvalidoException(Mensagens.get("notificacao.link-utilizado"));
        }
    }

    private void enviarWhatsApp(String telefone, String mensagem) {
        whatsappCliente.enviarMensagemTexto(telefone, mensagem);
    }

    /** Mantem apenas o prefixo do token nos logs, para nao expor o link de confirmacao. */
    private static String resumirToken(UUID token) {
        if (token == null) {
            return "<sem token>";
        }
        return token.toString().substring(0, 8) + "...";
    }
}

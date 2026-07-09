package com.prontudigital.backend.notificacao.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.FilaEsperaRepository;
import com.prontudigital.backend.compartilhado.clientes.WhatsappCloudApiClient;
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
        if (logRepository.existsByAgendamentoIdAndTipo(agendamento.getId(), TipoNotificacao.LEMBRETE_48H)) {
            return;
        }

        UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
        UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

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
        } catch (Exception e) {
            log.error("Falha ao enviar lembrete 48h para agendamento {}: {}",
                    agendamento.getId(), e.getMessage());
            registro.setStatus(StatusNotificacao.FALHA);
            registro.setTentativas(1);
        }

        logRepository.save(registro);
    }

    @Override
    @Transactional
    public void enviarSolicitacaoConfirmacao24h(Agendamento agendamento) {
        if (logRepository.existsByAgendamentoIdAndTipo(agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H)) {
            return;
        }

        UsuarioDTO paciente = usuarioService.buscarPorUuid(agendamento.getPacienteUuid());
        UsuarioDTO profissional = usuarioService.buscarPorUuid(agendamento.getProfissionalUuid());

        UUID token = UUID.randomUUID();
        LocalDateTime tokenExpiraEm = agendamento.getInicioEm().minusHours(2);

        String linkConfirmar = urlBaseConfirmacao + "/" + token + "/confirmar";
        String linkRecusar = urlBaseConfirmacao + "/" + token + "/recusar";

        String mensagem = String.format(
                "Olá, %s! Sua consulta com %s está marcada para %s. " +
                "Por favor, confirme sua presença:%n" +
                "✅ CONFIRMAR: %s%n" +
                "❌ CANCELAR: %s%n" +
                "Se não responder até 2h antes do horário, a vaga será liberada.",
                paciente.nomeCompleto(),
                profissional.nomeCompleto(),
                agendamento.getInicioEm().format(FORMATTER),
                linkConfirmar,
                linkRecusar);

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
            registro.setEnviadoEm(LocalDateTime.now(clock));
            registro.setTentativas(1);
        } catch (Exception e) {
            log.error("Falha ao enviar solicitação de confirmação 24h para agendamento {}: {}",
                    agendamento.getId(), e.getMessage());
            registro.setStatus(StatusNotificacao.FALHA);
            registro.setTentativas(1);
        }

        logRepository.save(registro);
    }

    @Override
    @Transactional
    public void confirmarViaToken(UUID token) {
        LogNotificacaoWhatsapp entrada = logRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> new TokenConfirmacaoInvalidoException(
                        "Token inválido ou não encontrado"));

        validarTokenUtilizavel(entrada);

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isAfter(entrada.getTokenExpiraEm())) {
            entrada.setStatus(StatusNotificacao.EXPIRADO);
            logRepository.save(entrada);
            throw new TokenConfirmacaoInvalidoException(
                    "O prazo de confirmação encerrou. Entre em contato para reagendar.");
        }

        Agendamento agendamento = agendamentoRepository.findById(entrada.getAgendamentoId())
                .orElseThrow(() -> new TokenConfirmacaoInvalidoException("Agendamento não encontrado"));

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);

        entrada.setStatus(StatusNotificacao.CONFIRMADO);
        entrada.setRespondidoEm(agora);
        logRepository.save(entrada);

        log.info("Agendamento {} confirmado via WhatsApp pelo paciente", agendamento.getId());
    }

    @Override
    @Transactional
    public void recusarViaToken(UUID token) {
        LogNotificacaoWhatsapp entrada = logRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> new TokenConfirmacaoInvalidoException(
                        "Token inválido ou não encontrado"));

        validarTokenUtilizavel(entrada);

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isAfter(entrada.getTokenExpiraEm())) {
            entrada.setStatus(StatusNotificacao.EXPIRADO);
            logRepository.save(entrada);
            throw new TokenConfirmacaoInvalidoException(
                    "O prazo de confirmação encerrou. Entre em contato para reagendar.");
        }

        Agendamento agendamento = agendamentoRepository.findById(entrada.getAgendamentoId())
                .orElseThrow(() -> new TokenConfirmacaoInvalidoException("Agendamento não encontrado"));

        liberarAgendamento(agendamento);

        entrada.setStatus(StatusNotificacao.RECUSADO);
        entrada.setRespondidoEm(agora);
        logRepository.save(entrada);

        log.info("Agendamento {} recusado via WhatsApp pelo paciente", agendamento.getId());
    }

    @Override
    @Transactional
    public void processarNaoConfirmados() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.plusHours(2);

        List<Agendamento> candidatos = agendamentoRepository.findByStatusInAndInicioEmBetween(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.REMARCADO),
                agora, limite);

        for (Agendamento agendamento : candidatos) {
            if (!logRepository.existsByAgendamentoIdAndTipoAndStatus(
                    agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO)) {
                continue;
            }

            liberarAgendamento(agendamento);

            logRepository.findByAgendamentoIdAndTipoAndStatus(
                    agendamento.getId(), TipoNotificacao.CONFIRMACAO_24H, StatusNotificacao.ENVIADO)
                    .ifPresent(entrada -> {
                        entrada.setStatus(StatusNotificacao.EXPIRADO);
                        logRepository.save(entrada);
                    });

            log.info("Agendamento {} cancelado por falta de confirmação dentro de 2h", agendamento.getId());
        }
    }

    private void liberarAgendamento(Agendamento agendamento) {
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
    }

    private void validarTokenUtilizavel(LogNotificacaoWhatsapp entrada) {
        if (entrada.getStatus() == StatusNotificacao.CONFIRMADO
                || entrada.getStatus() == StatusNotificacao.RECUSADO
                || entrada.getStatus() == StatusNotificacao.EXPIRADO) {
            throw new TokenConfirmacaoInvalidoException("Este link já foi utilizado ou expirou.");
        }
    }

    private void enviarWhatsApp(String telefone, String mensagem) {
        whatsappCliente.enviarMensagemTexto(telefone, mensagem);
        log.info("[WHATSAPP] Mensagem enviada para {}", telefone);
    }
}

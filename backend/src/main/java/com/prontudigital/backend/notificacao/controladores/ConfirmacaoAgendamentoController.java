package com.prontudigital.backend.notificacao.controladores;

import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.notificacao.dto.ConfirmacaoResponseDTO;
import com.prontudigital.backend.notificacao.servicos.NotificacaoWhatsappService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/confirmacao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Confirmação de Agendamentos", description = "Endpoints públicos para confirmação ou recusa de consultas via WhatsApp")
public class ConfirmacaoAgendamentoController {

    private final NotificacaoWhatsappService notificacaoService;

    @Operation(summary = "Confirmar presença na consulta",
               description = "Endpoint acessado via link enviado por WhatsApp. Marca o agendamento como CONFIRMADO.")
    @SecurityRequirements
    @GetMapping("/{token}/confirmar")
    public ResponseEntity<ConfirmacaoResponseDTO> confirmar(@PathVariable UUID token) {
        log.info("[WHATSAPP][LINK] GET /api/confirmacao/{}/confirmar", resumirToken(token));
        notificacaoService.confirmarViaToken(token);
        log.info("[WHATSAPP][LINK] Confirmacao concluida para o token {}", resumirToken(token));
        return ResponseEntity.ok(new ConfirmacaoResponseDTO(
                Mensagens.get("confirmacao.presenca-confirmada")));
    }

    @Operation(summary = "Recusar consulta agendada",
               description = "Endpoint acessado via link enviado por WhatsApp. Cancela o agendamento e o paciente é adicionado à fila de espera.")
    @SecurityRequirements
    @GetMapping("/{token}/recusar")
    public ResponseEntity<ConfirmacaoResponseDTO> recusar(@PathVariable UUID token) {
        log.info("[WHATSAPP][LINK] GET /api/confirmacao/{}/recusar", resumirToken(token));
        notificacaoService.recusarViaToken(token);
        log.info("[WHATSAPP][LINK] Recusa concluida para o token {}", resumirToken(token));
        return ResponseEntity.ok(new ConfirmacaoResponseDTO(
                Mensagens.get("confirmacao.consulta-cancelada")));
    }

    /** Mantem apenas o prefixo do token nos logs, para nao expor o link de confirmacao. */
    private static String resumirToken(UUID token) {
        return token == null ? "<sem token>" : token.toString().substring(0, 8) + "...";
    }
}

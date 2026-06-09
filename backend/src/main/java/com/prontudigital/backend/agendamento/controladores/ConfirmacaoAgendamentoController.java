package com.prontudigital.backend.agendamento.controladores;

import com.prontudigital.backend.agendamento.dto.ConfirmacaoResponseDTO;
import com.prontudigital.backend.agendamento.servicos.NotificacaoWhatsappService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/confirmacao")
@RequiredArgsConstructor
@Tag(name = "Confirmação de Agendamentos", description = "Endpoints públicos para confirmação ou recusa de consultas via WhatsApp")
public class ConfirmacaoAgendamentoController {

    private final NotificacaoWhatsappService notificacaoService;

    @Operation(summary = "Confirmar presença na consulta",
               description = "Endpoint acessado via link enviado por WhatsApp. Marca o agendamento como CONFIRMADO.")
    @SecurityRequirements
    @GetMapping("/{token}/confirmar")
    public ResponseEntity<ConfirmacaoResponseDTO> confirmar(@PathVariable UUID token) {
        notificacaoService.confirmarViaToken(token);
        return ResponseEntity.ok(new ConfirmacaoResponseDTO(
                "Presença confirmada com sucesso! Até logo."));
    }

    @Operation(summary = "Recusar consulta agendada",
               description = "Endpoint acessado via link enviado por WhatsApp. Cancela o agendamento e o paciente é adicionado à fila de espera.")
    @SecurityRequirements
    @GetMapping("/{token}/recusar")
    public ResponseEntity<ConfirmacaoResponseDTO> recusar(@PathVariable UUID token) {
        notificacaoService.recusarViaToken(token);
        return ResponseEntity.ok(new ConfirmacaoResponseDTO(
                "Consulta cancelada. Você foi adicionado à fila de espera com prioridade alta."));
    }
}

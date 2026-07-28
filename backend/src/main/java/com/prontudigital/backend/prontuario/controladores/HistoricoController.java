package com.prontudigital.backend.prontuario.controladores;

import com.prontudigital.backend.prontuario.dto.HistoricoItemDTO;
import com.prontudigital.backend.prontuario.servicos.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prontuario/pacientes/{pacienteUuid}/historico")
@RequiredArgsConstructor
@Tag(name = "Prontuario - Historico",
        description = "Linha do tempo consolidada do prontuario do paciente (RF18)")
@SecurityRequirement(name = "bearerAuth")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    @Operation(summary = "Historico clinico consolidado",
            description = "Reune agendamentos, evolucoes, prescricoes e anexos numa unica linha do "
                    + "tempo ordenada da mais recente para a mais antiga. ADMIN acessa qualquer "
                    + "paciente; PROFISSIONAL apenas pacientes com quem possui agendamento; "
                    + "PACIENTE apenas o proprio historico.")
    @ApiResponse(responseCode = "200", description = "Linha do tempo do paciente")
    public ResponseEntity<List<HistoricoItemDTO>> historico(@PathVariable UUID pacienteUuid) {
        return ResponseEntity.ok(historicoService.montarHistorico(pacienteUuid));
    }
}

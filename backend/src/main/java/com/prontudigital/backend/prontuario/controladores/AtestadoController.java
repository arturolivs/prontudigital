package com.prontudigital.backend.prontuario.controladores;

import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.prontuario.dto.AtestadoRequestDTO;
import com.prontudigital.backend.prontuario.dto.AtestadoResponseDTO;
import com.prontudigital.backend.prontuario.servicos.AtestadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prontuario/pacientes/{pacienteUuid}/atestados")
@RequiredArgsConstructor
@Tag(name = "Prontuario - Atestados", description = "Emissao de atestados (RF17)")
@SecurityRequirement(name = "bearerAuth")
public class AtestadoController {

    private final AtestadoService atestadoService;

    @GetMapping
    @Operation(summary = "Listar atestados do paciente",
            description = "ADMIN acessa qualquer paciente; PROFISSIONAL apenas pacientes com "
                    + "quem possui agendamento; PACIENTE apenas os proprios atestados.")
    @ApiResponse(responseCode = "200", description = "Lista de atestados, do mais recente")
    public ResponseEntity<List<AtestadoResponseDTO>> listar(@PathVariable UUID pacienteUuid) {
        return ResponseEntity.ok(atestadoService.listarPorPaciente(pacienteUuid));
    }

    @PostMapping
    @Operation(summary = "Emitir atestado",
            description = "Restrito a ADMIN e ao PROFISSIONAL vinculado ao paciente. "
                    + "AFASTAMENTO exige diasAfastamento; COMPARECIMENTO o recusa.")
    @ApiResponse(responseCode = "201", description = "Atestado emitido")
    @ApiResponse(responseCode = "422", description = "Dias de afastamento incompativeis com o tipo")
    public ResponseEntity<AtestadoResponseDTO> emitir(
            @PathVariable UUID pacienteUuid,
            @Valid @RequestBody AtestadoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atestadoService.emitir(pacienteUuid, request));
    }

    @GetMapping("/{atestadoUuid}/pdf")
    @Operation(summary = "Baixar o atestado em PDF",
            description = "O PDF e regerado a partir do registro a cada chamada.")
    @ApiResponse(responseCode = "200", description = "PDF do atestado")
    @ApiResponse(responseCode = "404", description = "Atestado nao encontrado para o paciente")
    public ResponseEntity<byte[]> baixarPdf(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID atestadoUuid) {

        ArquivoGerado arquivo = atestadoService.gerarPdf(pacienteUuid, atestadoUuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(arquivo.nomeArquivo())
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .body(arquivo.conteudo());
    }

    @DeleteMapping("/{atestadoUuid}")
    @Operation(summary = "Remover atestado")
    @ApiResponse(responseCode = "204", description = "Atestado removido")
    public ResponseEntity<Void> remover(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID atestadoUuid) {
        atestadoService.remover(pacienteUuid, atestadoUuid);
        return ResponseEntity.noContent().build();
    }
}

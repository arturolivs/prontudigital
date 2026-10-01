package com.prontudigital.backend.prontuario.controladores;

import com.prontudigital.backend.prontuario.dto.AnexoDownloadDTO;
import com.prontudigital.backend.prontuario.dto.AnexoResponseDTO;
import com.prontudigital.backend.prontuario.servicos.AnexoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prontuario/pacientes/{pacienteUuid}/anexos")
@RequiredArgsConstructor
@Tag(name = "Prontuario - Anexos",
        description = "Anexacao de exames e documentos ao prontuario (RF15)")
@SecurityRequirement(name = "bearerAuth")
public class AnexoController {

    private final AnexoService anexoService;

    @GetMapping
    @Operation(summary = "Listar anexos do paciente",
            description = "ADMIN acessa qualquer paciente; PROFISSIONAL apenas pacientes com quem "
                    + "possui agendamento; PACIENTE apenas os proprios anexos.")
    @ApiResponse(responseCode = "200", description = "Lista de anexos (do mais recente ao mais antigo)")
    public ResponseEntity<List<AnexoResponseDTO>> listar(@PathVariable UUID pacienteUuid) {
        return ResponseEntity.ok(anexoService.listarPorPaciente(pacienteUuid));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar anexo",
            description = "Upload de exame/documento (imagem ou PDF). Restrito a ADMIN e ao "
                    + "PROFISSIONAL vinculado ao paciente.")
    @ApiResponse(responseCode = "201", description = "Anexo enviado")
    @ApiResponse(responseCode = "422", description = "Arquivo vazio, tipo nao permitido ou acima do limite")
    public ResponseEntity<AnexoResponseDTO> enviar(
            @PathVariable UUID pacienteUuid,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "agendamentoUuid", required = false) UUID agendamentoUuid) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(anexoService.enviar(pacienteUuid, arquivo, agendamentoUuid));
    }

    @GetMapping("/{anexoUuid}/conteudo")
    @Operation(summary = "Baixar/visualizar conteudo do anexo",
            description = "Retorna o binario do anexo (inline) para download ou preview.")
    @ApiResponse(responseCode = "200", description = "Conteudo do anexo")
    @ApiResponse(responseCode = "404", description = "Anexo nao encontrado para o paciente")
    public ResponseEntity<Resource> baixar(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID anexoUuid) {
        AnexoDownloadDTO download = anexoService.baixar(pacienteUuid, anexoUuid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.tipoConteudo()))
                .contentLength(download.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + download.nomeOriginal() + "\"")
                .body(download.recurso());
    }

    @DeleteMapping("/{anexoUuid}")
    @Operation(summary = "Excluir anexo",
            description = "Remove o anexo e seu arquivo. Restrito a ADMIN e ao PROFISSIONAL vinculado.")
    @ApiResponse(responseCode = "204", description = "Anexo excluido")
    @ApiResponse(responseCode = "404", description = "Anexo nao encontrado para o paciente")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID pacienteUuid,
            @PathVariable UUID anexoUuid) {
        anexoService.excluir(pacienteUuid, anexoUuid);
        return ResponseEntity.noContent().build();
    }
}

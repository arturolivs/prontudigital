package com.prontudigital.backend.configuracao.controladores;

import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaRequestDTO;
import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaResponseDTO;
import com.prontudigital.backend.configuracao.dto.LogoDownloadDTO;
import com.prontudigital.backend.configuracao.servicos.ConfiguracaoClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Configuracao da clinica desta instalacao.
 *
 * <p>Leitura PUBLICA (ver GET_PUBLICOS no SecurityConfig): nome e logo compoem
 * a tela de login e o agendamento publico, ambos anteriores a autenticacao —
 * e um {@code <img src>} nao envia o header Authorization. Sao dados
 * institucionais, equivalentes ao que a clinica publica no proprio site.
 *
 * <p>Escrita e exclusiva do ADMIN.
 */
@RestController
@RequestMapping("/api/configuracao")
@RequiredArgsConstructor
@Tag(name = "Configuracao", description = "Dados e marca da clinica")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracaoClinicaController {

    private final ConfiguracaoClinicaService configuracaoService;

    @GetMapping
    @Operation(summary = "Obter configuracao da clinica",
            description = "Disponivel a qualquer usuario autenticado — alimenta o "
                    + "cabecalho da aplicacao.")
    @ApiResponse(responseCode = "200", description = "Configuracao retornada")
    public ResponseEntity<ConfiguracaoClinicaResponseDTO> buscar() {
        return ResponseEntity.ok(configuracaoService.buscar());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar configuracao da clinica")
    @ApiResponse(responseCode = "200", description = "Configuracao atualizada")
    @ApiResponse(responseCode = "403", description = "Apenas ADMIN")
    @ApiResponse(responseCode = "422", description = "Dados invalidos")
    public ResponseEntity<ConfiguracaoClinicaResponseDTO> atualizar(
            @Valid @RequestBody ConfiguracaoClinicaRequestDTO request) {
        return ResponseEntity.ok(configuracaoService.atualizar(request));
    }

    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar logo da clinica",
            description = "Imagem JPG, PNG ou WEBP de ate 2 MB. Substitui a anterior.")
    @ApiResponse(responseCode = "200", description = "Logo atualizada")
    @ApiResponse(responseCode = "403", description = "Apenas ADMIN")
    @ApiResponse(responseCode = "422", description = "Arquivo vazio, tipo nao permitido ou acima do limite")
    public ResponseEntity<ConfiguracaoClinicaResponseDTO> enviarLogo(
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(configuracaoService.enviarLogo(arquivo));
    }

    @GetMapping("/logo")
    @Operation(summary = "Obter a logo da clinica",
            description = "Retorna o binario inline, para uso em <img> e nos documentos.")
    @ApiResponse(responseCode = "200", description = "Binario da logo")
    @ApiResponse(responseCode = "422", description = "Nenhuma logo cadastrada")
    public ResponseEntity<Resource> baixarLogo() {
        LogoDownloadDTO logo = configuracaoService.baixarLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo\"")
                .body(logo.recurso());
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover a logo da clinica")
    @ApiResponse(responseCode = "200", description = "Logo removida")
    @ApiResponse(responseCode = "403", description = "Apenas ADMIN")
    public ResponseEntity<ConfiguracaoClinicaResponseDTO> removerLogo() {
        return ResponseEntity.ok(configuracaoService.removerLogo());
    }
}

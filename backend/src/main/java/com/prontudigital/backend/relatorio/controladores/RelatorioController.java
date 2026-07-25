package com.prontudigital.backend.relatorio.controladores;

import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.compartilhado.documento.FormatoExportacao;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;
import com.prontudigital.backend.relatorio.servicos.RelatorioExportacaoService;
import com.prontudigital.backend.relatorio.servicos.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatorios (RF19/RF20)")
@SecurityRequirement(name = "bearerAuth")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final RelatorioExportacaoService exportacaoService;

    @Operation(summary = "Relatorio de atendimentos por periodo/profissional (RF19)",
            description = "ADMIN pode omitir profissionalUuid para ver a clinica inteira. "
                    + "PROFISSIONAL so consulta a propria agenda.")
    @ApiResponse(responseCode = "200", description = "Relatorio gerado")
    @ApiResponse(responseCode = "403", description = "Sem autorizacao para o recorte pedido")
    @ApiResponse(responseCode = "422", description = "Periodo invalido")
    @GetMapping("/atendimentos")
    public ResponseEntity<RelatorioAtendimentosDTO> atendimentos(
            @Parameter(description = "Primeiro dia do periodo", example = "2026-07-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Ultimo dia do periodo (incluso)", example = "2026-07-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,

            @Parameter(description = "Opcional; ignorado para PROFISSIONAL")
            @RequestParam(required = false) UUID profissionalUuid,

            @RequestParam(required = false) StatusAgendamento status,

            @RequestParam(required = false) TipoAgendamento tipo) {

        return ResponseEntity.ok(
                relatorioService.atendimentos(inicio, fim, profissionalUuid, status, tipo));
    }

    @Operation(summary = "Relatorio de ocupacao da clinica (RF20)",
            description = "Comparecimento, cancelamentos e ocupacao da agenda. A taxa de "
                    + "ocupacao exige um profissional com horarios de trabalho cadastrados "
                    + "(RF05); sem isso vem nula.")
    @ApiResponse(responseCode = "200", description = "Relatorio gerado")
    @ApiResponse(responseCode = "403", description = "Sem autorizacao para o recorte pedido")
    @ApiResponse(responseCode = "422", description = "Periodo invalido")
    @GetMapping("/ocupacao")
    public ResponseEntity<RelatorioOcupacaoDTO> ocupacao(
            @Parameter(description = "Primeiro dia do periodo", example = "2026-07-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Ultimo dia do periodo (incluso)", example = "2026-07-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,

            @Parameter(description = "Opcional; ignorado para PROFISSIONAL")
            @RequestParam(required = false) UUID profissionalUuid) {

        return ResponseEntity.ok(relatorioService.ocupacao(inicio, fim, profissionalUuid));
    }

    @Operation(summary = "Exportar o relatorio de atendimentos em PDF ou Excel (RF21)",
            description = "Mesmos filtros e mesmas regras de acesso de /atendimentos.")
    @ApiResponse(responseCode = "200", description = "Arquivo gerado")
    @GetMapping("/atendimentos/exportar")
    public ResponseEntity<byte[]> exportarAtendimentos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID profissionalUuid,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) TipoAgendamento tipo,

            @Parameter(description = "PDF ou XLSX")
            @RequestParam(defaultValue = "PDF") FormatoExportacao formato) {

        return download(exportacaoService.exportarAtendimentos(
                inicio, fim, profissionalUuid, status, tipo, formato));
    }

    @Operation(summary = "Exportar o relatorio de ocupacao em PDF ou Excel (RF21)")
    @ApiResponse(responseCode = "200", description = "Arquivo gerado")
    @GetMapping("/ocupacao/exportar")
    public ResponseEntity<byte[]> exportarOcupacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID profissionalUuid,

            @Parameter(description = "PDF ou XLSX")
            @RequestParam(defaultValue = "PDF") FormatoExportacao formato) {

        return download(exportacaoService.exportarOcupacao(inicio, fim, profissionalUuid, formato));
    }

    private ResponseEntity<byte[]> download(ArquivoGerado arquivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(arquivo.nomeArquivo())
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .body(arquivo.conteudo());
    }
}

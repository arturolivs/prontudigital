package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.TipoPrescricao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Dados para registro/atualizacao de uma prescricao (RF16)")
public record PrescricaoRequestDTO(

        @NotNull
        @Schema(description = "Tipo do item prescrito", example = "MEDICAMENTO")
        TipoPrescricao tipo,

        @NotBlank
        @Size(max = 1000)
        @Schema(description = "Medicamento prescrito ou cuidado de enfermagem",
                example = "Sulfadiazina de prata 1% - creme")
        String descricao,

        @Size(max = 1000)
        @Schema(description = "Posologia / dose (aplicavel a medicamentos)",
                example = "Aplicar camada fina sobre a ferida")
        String posologia,

        @Size(max = 255)
        @Schema(description = "Frequencia", example = "A cada 12 horas")
        String frequencia,

        @Size(max = 255)
        @Schema(description = "Duracao do tratamento", example = "7 dias")
        String duracao,

        @Size(max = 5000)
        @Schema(description = "Orientacoes complementares ao paciente/cuidador")
        String orientacoes,

        @Schema(description = "Atendimento (agendamento) ao qual a prescricao esta vinculada, se houver")
        UUID agendamentoUuid

) {}

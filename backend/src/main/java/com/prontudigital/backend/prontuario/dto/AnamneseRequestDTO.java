package com.prontudigital.backend.prontuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro/atualizacao da anamnese do paciente (RF13)")
public record AnamneseRequestDTO(

        @Size(max = 5000)
        @Schema(description = "Queixa principal relatada pelo paciente",
                example = "Dor e secrecao em ferida no membro inferior direito")
        String queixaPrincipal,

        @Size(max = 5000)
        @Schema(description = "Historico da doenca atual")
        String historicoDoencaAtual,

        @Size(max = 5000)
        @Schema(description = "Historico medico pregresso (comorbidades, cirurgias)")
        String historicoMedicoPregresso,

        @Size(max = 5000)
        @Schema(description = "Alergias conhecidas", example = "Penicilina, latex")
        String alergias,

        @Size(max = 5000)
        @Schema(description = "Medicamentos em uso continuo", example = "Losartana 50mg, Metformina 850mg")
        String medicamentosEmUso,

        @Size(max = 5000)
        @Schema(description = "Historico familiar relevante")
        String historicoFamiliar,

        @Size(max = 5000)
        @Schema(description = "Habitos de vida (tabagismo, etilismo, atividade fisica)")
        String habitos,

        @Size(max = 5000)
        @Schema(description = "Observacoes complementares")
        String observacoes

) {}

package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.TipoAtestado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Dados para emissao de atestado (RF17)")
public record AtestadoRequestDTO(

        @Schema(description = "COMPARECIMENTO ou AFASTAMENTO")
        @NotNull(message = "Informe o tipo do atestado")
        TipoAtestado tipo,

        @Schema(description = "Atendimento que originou o atestado (opcional)")
        UUID agendamentoUuid,

        @Schema(description = "Dias de afastamento; obrigatorio para AFASTAMENTO "
                + "e recusado para COMPARECIMENTO", example = "3")
        @Min(value = 1, message = "Os dias de afastamento devem ser maiores que zero")
        Integer diasAfastamento,

        @Schema(description = "CID, opcional — depende de consentimento do paciente",
                example = "L97")
        @Size(max = 10, message = "CID deve ter no maximo 10 caracteres")
        String cid,

        @Schema(description = "Observacoes que entram no corpo do atestado")
        String observacoes

) {}

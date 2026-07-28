package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "Endereco do usuario (RF04)")
@Builder
public record EnderecoDTO(

        @Schema(description = "CEP, com ou sem mascara", example = "01310-100")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP invalido")
        String cep,

        @Schema(description = "Logradouro", example = "Avenida Paulista")
        @Size(max = 150)
        String logradouro,

        @Schema(description = "Numero", example = "1578")
        @Size(max = 20)
        String numero,

        @Schema(description = "Complemento", example = "Apto 42")
        @Size(max = 100)
        String complemento,

        @Schema(description = "Bairro", example = "Bela Vista")
        @Size(max = 100)
        String bairro,

        @Schema(description = "Cidade", example = "Sao Paulo")
        @Size(max = 100)
        String cidade,

        @Schema(description = "Unidade federativa", example = "SP")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve ter 2 letras")
        String uf

) {}

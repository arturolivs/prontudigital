package com.prontudigital.backend.agendamento.swagger;

import com.prontudigital.backend.agendamento.dto.AgendamentoResponseDTO;
import com.prontudigital.backend.agendamento.dto.AgendamentoViewDTO;
import com.prontudigital.backend.compartilhado.swagger.CommonsSwagger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class AgendamentoSwagger {

    private AgendamentoSwagger() {}

    // =========================================================
    // POST /api/agendamentos — RF07
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Criar novo agendamento",
            description = """
            Cria um agendamento entre paciente e profissional. \n
            Regras aplicadas:
            - Paciente so pode criar agendamentos para si mesmo
            - Profissional so pode criar agendamentos em sua propria agenda
            - ADMIN pode criar para qualquer paciente/profissional
            - Tratamento exige uma avaliacao previa concluida
            - Duracao deve ser entre 15 minutos e 8 horas
            
            Atende RF07.
            """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Agendamento criado com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AgendamentoResponseDTO.class)
            )
    )
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseConflict
    @CommonsSwagger.ApiResponseUnprocessable
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface AgendarSwagger {}

    // =========================================================
    // PATCH /api/agendamentos/{id}/cancelar — RF10
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Cancelar agendamento",
            description = """
            Cancela um agendamento existente. \n
            Regras:
            - Agendamento ja cancelado ou concluido nao pode ser cancelado novamente
            - Paciente so pode cancelar seu proprio agendamento
            - Profissional so pode cancelar agendamentos de sua agenda
            - Apos cancelamento, a fila de espera do profissional pode ser notificada (RF12)
            
            Atende RF10.
            """
    )
    @ApiResponse(responseCode = "204", description = "Agendamento cancelado com sucesso")
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseNotFound
    @CommonsSwagger.ApiResponseConflict
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface CancelarSwagger {}

    // =========================================================
    // PATCH /api/agendamentos/{id}/reagendar — RF10
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Reagendar agendamento",
            description = """
            Move um agendamento para uma nova data/horario. \n
            Regras:
            - Agendamento cancelado ou concluido nao pode ser reagendado
            - Mesmas validacoes de disponibilidade do agendamento original
            - Mantem historico da alteracao (auditoria)
            - Permissao igual ao cancelamento
            
            Atende RF10.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Reagendamento realizado com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AgendamentoResponseDTO.class)
            )
    )
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseNotFound
    @CommonsSwagger.ApiResponseConflict
    @CommonsSwagger.ApiResponseUnprocessable
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface ReagendarSwagger {}

    // =========================================================
    // PATCH /api/agendamentos/{id}/concluir
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Concluir agendamento",
            description = """
            Marca um agendamento como concluido. \n
            Regras:
            - Apenas ADMIN ou PROFISSIONAL podem concluir
            - Agendamento ja concluido ou cancelado nao pode ser concluido
            - Apos concluir uma avaliacao, tratamentos vinculados podem ser agendados
            """
    )
    @ApiResponse(responseCode = "204", description = "Agendamento concluido com sucesso")
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseNotFound
    @CommonsSwagger.ApiResponseConflict
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface ConcluirSwagger {}

    // =========================================================
    // GET /api/agendamentos/agenda — RF08
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Visualizar agenda por dia, semana ou mes",
            description = """
            Retorna agendamentos em um periodo. \n
            Comportamento por perfil:
            - PROFISSIONAL: ve sempre a propria agenda (parametro profissionalUuid e ignorado)
            - ADMIN: deve informar profissionalUuid obrigatoriamente
            - PACIENTE: nao tem acesso a este endpoint
            
            Tipo de visualizacao:
            - DIA: do inicio ate o fim do dia informado
            - SEMANA: de segunda a domingo da semana da data informada
            - MES: do primeiro ao ultimo dia do mes da data informada
            
            Atende RF08.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de agendamentos do periodo",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AgendamentoViewDTO.class))
            )
    )
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseUnprocessable
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface VisualizarAgendaSwagger {}

    // =========================================================
    // GET /api/agendamentos/avaliacoes/{id}/tratamentos
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Listar tratamentos vinculados a uma avaliacao",
            description = """
            Retorna todos os tratamentos que foram criados a partir de uma avaliacao. \n
            Apenas o paciente da avaliacao, o profissional responsavel ou ADMIN podem visualizar.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de tratamentos da avaliacao",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AgendamentoViewDTO.class))
            )
    )
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseNotFound
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface TratamentosPorAvaliacaoSwagger {}

    // =========================================================
    // GET /api/agendamentos/meus
    // =========================================================
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Listar meus agendamentos (paciente)",
            description = """
            Retorna todos os agendamentos do paciente autenticado, ordenados do mais recente para o mais antigo. \n
            Regras:
            - Exclusivo para usuarios com perfil PACIENTE
            - Filtra automaticamente pelo UUID do paciente extraido do JWT
            - Nao requer parametros
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de agendamentos do paciente autenticado",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AgendamentoViewDTO.class))
            )
    )
    @CommonsSwagger.ApiResponseUnauthorized
    @CommonsSwagger.ApiResponseForbidden
    @CommonsSwagger.ApiResponseInternalServerError
    public @interface MeusAgendamentosSwagger {}
}
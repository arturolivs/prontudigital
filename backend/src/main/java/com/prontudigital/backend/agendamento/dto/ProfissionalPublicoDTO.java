package com.prontudigital.backend.agendamento.dto;

import java.util.UUID;

/**
 * Profissional exposto na tela publica de agendamento.
 *
 * <p>{@code temAvatar} evita que a tela peca a imagem de quem nao tem: sem ele,
 * cada profissional sem foto geraria um 422 e um icone de imagem quebrada.
 * O binario vem de {@code GET /api/public/profissionais/{uuid}/avatar}.
 */
public record ProfissionalPublicoDTO(UUID uuid, String nomeCompleto, boolean temAvatar) {}

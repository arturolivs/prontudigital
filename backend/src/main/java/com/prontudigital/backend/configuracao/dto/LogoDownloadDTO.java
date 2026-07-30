package com.prontudigital.backend.configuracao.dto;

import org.springframework.core.io.Resource;

/** Binario da logo para streaming, com o tipo necessario ao Content-Type. */
public record LogoDownloadDTO(Resource recurso, String tipoConteudo) {}

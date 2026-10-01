package com.prontudigital.backend.autenticacao.dto;

import org.springframework.core.io.Resource;

/** Binario do avatar para streaming, com o tipo necessario ao Content-Type. */
public record AvatarDownloadDTO(Resource recurso, String tipoConteudo) {}

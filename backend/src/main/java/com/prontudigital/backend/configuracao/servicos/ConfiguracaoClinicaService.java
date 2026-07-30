package com.prontudigital.backend.configuracao.servicos;

import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaRequestDTO;
import com.prontudigital.backend.configuracao.dto.ConfiguracaoClinicaResponseDTO;
import com.prontudigital.backend.configuracao.dto.LogoDownloadDTO;
import com.prontudigital.backend.configuracao.entidades.ConfiguracaoClinica;
import org.springframework.web.multipart.MultipartFile;

/**
 * Configuracao da clinica desta instalacao (modelo silo — ver a entidade).
 */
public interface ConfiguracaoClinicaService {

    ConfiguracaoClinicaResponseDTO buscar();

    ConfiguracaoClinicaResponseDTO atualizar(ConfiguracaoClinicaRequestDTO request);

    ConfiguracaoClinicaResponseDTO enviarLogo(MultipartFile arquivo);

    LogoDownloadDTO baixarLogo();

    ConfiguracaoClinicaResponseDTO removerLogo();

    /**
     * Entidade crua, para quem monta documento e precisa de nome, endereco e
     * da chave da logo — evita que o PdfBuilder dependa dos DTOs de API.
     */
    ConfiguracaoClinica obterConfiguracao();
}

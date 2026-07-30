package com.prontudigital.backend.configuracao.repositorios;

import com.prontudigital.backend.configuracao.entidades.ConfiguracaoClinica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoClinicaRepository
        extends JpaRepository<ConfiguracaoClinica, Long> {
}

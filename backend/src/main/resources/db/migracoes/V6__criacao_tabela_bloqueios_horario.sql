-- =============================================================
-- V6 - Tabela de bloqueios de horario
-- Registra periodos em que um profissional esta indisponivel
-- (ferias, urgencia, folga, etc).
-- =============================================================

CREATE TABLE bloqueios_horario (
    id                BIGSERIAL    PRIMARY KEY,
    uuid              UUID         DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    profissional_uuid UUID         NOT NULL,

    inicio_em         TIMESTAMP    NOT NULL,
    fim_em            TIMESTAMP    NOT NULL,

    motivo            VARCHAR(255),

    -- Valores validos: INDISPONIVEL, URGENCIA
    tipo              VARCHAR(20)  NOT NULL,

    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Garante integridade temporal: fim sempre apos o inicio
    CONSTRAINT chk_bloqueio_periodo_valido
        CHECK (fim_em > inicio_em)
);

-- Buscas por profissional
CREATE INDEX idx_bloqueios_profissional_uuid
    ON bloqueios_horario(profissional_uuid);

-- Indice composto: cobre diretamente a query findConflitos
-- (profissional_uuid, inicio_em, fim_em) usada na validacao de disponibilidade
CREATE INDEX idx_bloqueios_prof_periodo
    ON bloqueios_horario(profissional_uuid, inicio_em, fim_em);

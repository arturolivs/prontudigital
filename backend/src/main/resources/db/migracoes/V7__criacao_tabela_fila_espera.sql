-- =============================================================
-- V7 - Tabela de fila de espera
-- Registra pacientes aguardando disponibilidade de horario
-- com um profissional.
-- =============================================================

CREATE TABLE fila_espera (
    id                BIGSERIAL    PRIMARY KEY,
    uuid              UUID         DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    paciente_uuid     UUID         NOT NULL,
    profissional_uuid UUID         NOT NULL,

    -- Valores validos: AVALIACAO, TRATAMENTO
    tipo_preferido    VARCHAR(20),

    data_preferida    TIMESTAMP,

    -- Quanto menor o numero, maior a prioridade (0 = sem prioridade definida)
    prioridade        INTEGER      NOT NULL DEFAULT 0,

    -- Valores validos: ATIVO, NOTIFICADO, CANCELADO
    status            VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',

    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Buscas por paciente
CREATE INDEX idx_fila_espera_paciente_uuid
    ON fila_espera(paciente_uuid);

-- Buscas por profissional (listar fila de um profissional)
CREATE INDEX idx_fila_espera_profissional_uuid
    ON fila_espera(profissional_uuid);

-- Filtro por status (buscar apenas entradas ATIVAS)
CREATE INDEX idx_fila_espera_status
    ON fila_espera(status);

-- Indice composto: listagem da fila ativa de um profissional ordenada por prioridade
CREATE INDEX idx_fila_espera_prof_status_prioridade
    ON fila_espera(profissional_uuid, status, prioridade);

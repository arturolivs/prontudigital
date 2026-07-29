-- =============================================================
-- V17 - Tabela de bloqueios recorrentes por dia da semana
-- Registra regras que repetem toda semana num dia/horario fixo.
-- =============================================================

CREATE TABLE bloqueios_recorrentes (
    id                BIGSERIAL    PRIMARY KEY,
    uuid              UUID         DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    profissional_uuid UUID         NOT NULL,

    -- ISO-8601: 1=Segunda ... 6=Sabado, 7=Domingo
    -- INTEGER (nao SMALLINT): a entidade mapeia Integer e o Hibernate valida
    -- o tipo no boot (ddl-auto: validate nos perfis dev e prod).
    dia_semana        INTEGER      NOT NULL,

    hora_inicio       TIME         NOT NULL,
    hora_fim          TIME         NOT NULL,

    motivo            VARCHAR(255),

    tipo              VARCHAR(20)  NOT NULL,

    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,

    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_recorrente_dia_valido
        CHECK (dia_semana BETWEEN 1 AND 7),

    CONSTRAINT chk_recorrente_hora_valida
        CHECK (hora_fim > hora_inicio)
);

CREATE INDEX idx_bloqueios_recorrentes_prof
    ON bloqueios_recorrentes(profissional_uuid)
    WHERE ativo = TRUE;

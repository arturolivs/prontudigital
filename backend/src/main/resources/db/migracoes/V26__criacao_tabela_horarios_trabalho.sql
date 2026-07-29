-- =============================================================
-- V26 - Horarios de trabalho do profissional (RF05)
--
-- Janelas semanais em que o profissional atende. Alimentam a
-- validacao de disponibilidade da agenda: um agendamento precisa
-- caber inteiro dentro de uma janela do dia da semana.
--
-- Profissional sem nenhuma janela cadastrada continua podendo ser
-- agendado a qualquer hora (regra so passa a valer quando ha
-- horario definido), preservando os cadastros existentes.
-- =============================================================

CREATE TABLE horarios_trabalho (
    id                BIGSERIAL    PRIMARY KEY,
    uuid              UUID         DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    profissional_uuid UUID         NOT NULL,

    -- ISO-8601: 1=Segunda ... 6=Sabado, 7=Domingo
    -- INTEGER (nao SMALLINT): a entidade mapeia Integer e o Hibernate valida
    -- o tipo no boot (ddl-auto: validate nos perfis dev e prod).
    dia_semana        INTEGER      NOT NULL,

    hora_inicio       TIME         NOT NULL,
    hora_fim          TIME         NOT NULL,

    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,

    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_horario_trabalho_dia_valido
        CHECK (dia_semana BETWEEN 1 AND 7),

    CONSTRAINT chk_horario_trabalho_hora_valida
        CHECK (hora_fim > hora_inicio)
);

CREATE INDEX idx_horarios_trabalho_prof
    ON horarios_trabalho(profissional_uuid)
    WHERE ativo = TRUE;

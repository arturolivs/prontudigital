-- =============================================================
-- V5 - Tabela de agendamentos
-- Registra consultas, avaliacoes e tratamentos entre
-- pacientes e profissionais.
-- =============================================================

CREATE TABLE agendamentos (
    id                BIGSERIAL    PRIMARY KEY,
    uuid              UUID         DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    paciente_uuid     UUID         NOT NULL,
    profissional_uuid UUID         NOT NULL,

    inicio_em         TIMESTAMP    NOT NULL,
    fim_em            TIMESTAMP    NOT NULL,

    -- Valores validos: AGENDADO, CONFIRMADO, CANCELADO, REMARCADO, REALIZADO, NAO_COMPARECEU
    status            VARCHAR(20)  NOT NULL,

    -- Valores validos: AVALIACAO, TRATAMENTO
    tipo              VARCHAR(20)  NOT NULL,

    -- Auto-referencia: preenchido apenas quando tipo = TRATAMENTO
    avaliacao_id      BIGINT,

    concluido_em      TIMESTAMP,
    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP,

    -- Impede deletar uma avaliacao que possui tratamentos vinculados
    CONSTRAINT fk_agendamento_avaliacao
        FOREIGN KEY (avaliacao_id)
        REFERENCES agendamentos(id)
        ON DELETE RESTRICT,

    -- Garante integridade temporal: fim sempre apos o inicio
    CONSTRAINT chk_agendamento_periodo_valido
        CHECK (fim_em > inicio_em)
);

-- Buscas por paciente (visualizacao e verificacao de conflitos)
CREATE INDEX idx_agendamentos_paciente_uuid
    ON agendamentos(paciente_uuid);

-- Buscas por profissional (visualizacao de agenda)
CREATE INDEX idx_agendamentos_profissional_uuid
    ON agendamentos(profissional_uuid);

-- Buscas por data de inicio (ordenacao e filtro por periodo)
CREATE INDEX idx_agendamentos_inicio_em
    ON agendamentos(inicio_em);

-- Indice composto: cobre a query findConflitosParaProfissional e
-- findByProfissionalUuidAndInicioEmBetween sem leitura de heap extra
CREATE INDEX idx_agendamentos_prof_periodo
    ON agendamentos(profissional_uuid, inicio_em, fim_em);

-- Indice composto: cobre a query findConflitosParaPaciente
CREATE INDEX idx_agendamentos_pac_periodo
    ON agendamentos(paciente_uuid, inicio_em, fim_em);

-- Buscas de tratamentos por avaliacao (findByAvaliacaoId)
CREATE INDEX idx_agendamentos_avaliacao_id
    ON agendamentos(avaliacao_id);

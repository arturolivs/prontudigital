-- =============================================================
-- V27 - Atestados emitidos ao paciente (RF17)
--
-- O PDF nao e guardado: e regerado a partir destes campos sempre
-- que solicitado, entao o registro e a fonte da verdade.
-- =============================================================

CREATE TABLE atestados (
    id                 BIGSERIAL   PRIMARY KEY,
    uuid               UUID        DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    paciente_uuid      UUID        NOT NULL,

    -- Atendimento que originou o atestado, quando houver.
    agendamento_uuid   UUID,

    -- COMPARECIMENTO | AFASTAMENTO
    tipo               VARCHAR(20) NOT NULL,

    -- Só faz sentido em AFASTAMENTO; a regra fica no service.
    dias_afastamento   INTEGER,

    -- Informar CID e opcional e depende de consentimento do paciente.
    cid                VARCHAR(10),

    observacoes        TEXT,

    -- Profissional que assinou.
    emitido_por        UUID,

    criado_em          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_atestado_dias_positivos
        CHECK (dias_afastamento IS NULL OR dias_afastamento > 0)
);

CREATE INDEX idx_atestados_paciente ON atestados(paciente_uuid);

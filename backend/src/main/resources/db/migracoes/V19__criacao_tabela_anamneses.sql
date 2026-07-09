-- RF13: Registro de anamnese do paciente (histórico, queixas, alergias, medicamentos em uso).
-- Uma anamnese por paciente (paciente_uuid único). Vinculada ao paciente, nao ao atendimento.

CREATE TABLE anamneses (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    paciente_uuid               UUID NOT NULL UNIQUE,

    queixa_principal            TEXT,
    historico_doenca_atual      TEXT,
    historico_medico_pregresso  TEXT,
    alergias                    TEXT,
    medicamentos_em_uso         TEXT,
    historico_familiar          TEXT,
    habitos                     TEXT,
    observacoes                 TEXT,

    registrado_por              UUID,

    criado_em                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMP
);

CREATE INDEX idx_anamneses_paciente_uuid ON anamneses(paciente_uuid);

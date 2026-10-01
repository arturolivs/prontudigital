-- RF16: Prescricao de medicamentos e cuidados de enfermagem.
-- Relacao 1:N com o paciente (varias prescricoes ao longo do tempo).
-- Vinculada ao paciente e, opcionalmente, a um atendimento (agendamento_uuid).

CREATE TABLE prescricoes (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    paciente_uuid     UUID NOT NULL,
    agendamento_uuid  UUID,

    tipo              VARCHAR(20) NOT NULL,
    descricao         TEXT NOT NULL,
    posologia         TEXT,
    frequencia        VARCHAR(255),
    duracao           VARCHAR(255),
    orientacoes       TEXT,

    registrado_por    UUID,

    criado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP
);

CREATE INDEX idx_prescricoes_paciente_uuid ON prescricoes(paciente_uuid);
CREATE INDEX idx_prescricoes_agendamento_uuid ON prescricoes(agendamento_uuid);

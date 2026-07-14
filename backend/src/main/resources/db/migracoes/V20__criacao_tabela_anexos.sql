-- RF15: Anexacao de exames e documentos ao prontuario.
-- Relacao 1:N com o paciente (varios anexos ao longo do tempo).
-- O binario NAO fica no banco: guarda-se apenas a chave de armazenamento
-- (caminho relativo no volume/filesystem, ou chave de objeto em S3/MinIO no futuro).

CREATE TABLE anexos (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    paciente_uuid        UUID NOT NULL,
    agendamento_uuid     UUID,

    nome_original        VARCHAR(255) NOT NULL,
    tipo_conteudo        VARCHAR(100) NOT NULL,
    tamanho_bytes        BIGINT NOT NULL,
    chave_armazenamento  VARCHAR(512) NOT NULL,

    registrado_por       UUID,

    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anexos_paciente_uuid ON anexos(paciente_uuid);
CREATE INDEX idx_anexos_agendamento_uuid ON anexos(agendamento_uuid);

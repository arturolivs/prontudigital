CREATE TABLE codigos_recuperacao_senha (
    id          BIGSERIAL    PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo      VARCHAR(6)   NOT NULL,
    expira_em   TIMESTAMP    NOT NULL,
    utilizado   BOOLEAN      NOT NULL DEFAULT FALSE,
    tentativas  INT          NOT NULL DEFAULT 0,
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_codigos_recuperacao_usuario_id
    ON codigos_recuperacao_senha(usuario_id);

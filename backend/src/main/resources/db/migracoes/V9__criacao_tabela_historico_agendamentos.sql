CREATE TABLE historico_agendamentos (
    id              BIGSERIAL    PRIMARY KEY,
    agendamento_id  BIGINT       NOT NULL,
    inicio_anterior TIMESTAMP    NOT NULL,
    fim_anterior    TIMESTAMP    NOT NULL,
    inicio_novo     TIMESTAMP    NOT NULL,
    fim_novo        TIMESTAMP    NOT NULL,
    motivo          VARCHAR(255),
    alterado_por    UUID         NOT NULL,
    alterado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historico_agendamento
        FOREIGN KEY (agendamento_id)
        REFERENCES agendamentos(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_historico_agendamento_id ON historico_agendamentos(agendamento_id);
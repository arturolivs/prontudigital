
CREATE TABLE log_notificacoes_whatsapp (
    id                  BIGSERIAL    PRIMARY KEY,
    agendamento_id      BIGINT       NOT NULL,
    paciente_uuid       UUID         NOT NULL,
    tipo                VARCHAR(30)  NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDENTE',
    token_confirmacao   UUID         UNIQUE,
    token_expira_em     TIMESTAMP,

    telefone            VARCHAR(20),
    mensagem            TEXT,
    tentativas          INT          NOT NULL DEFAULT 0,
    enviado_em          TIMESTAMP,
    respondido_em       TIMESTAMP,
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_log_notificacao_agendamento
        FOREIGN KEY (agendamento_id)
        REFERENCES agendamentos(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_log_notif_agendamento_id
    ON log_notificacoes_whatsapp(agendamento_id);

CREATE INDEX idx_log_notif_token
    ON log_notificacoes_whatsapp(token_confirmacao)
    WHERE token_confirmacao IS NOT NULL;

CREATE INDEX idx_log_notif_status
    ON log_notificacoes_whatsapp(status);

CREATE INDEX idx_log_notif_agendamento_tipo
    ON log_notificacoes_whatsapp(agendamento_id, tipo);

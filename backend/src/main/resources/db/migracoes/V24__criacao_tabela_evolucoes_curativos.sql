-- Ficha de Evolucao Diaria - Curativos (modelo ajustar_campos/modelos/FICHA DE EVOLUCAO DIARIA - CURATIVOS.pdf).
-- 1:1 com agendamento do tipo TRATAMENTO. Avaliacao diaria segue a legenda TIME
-- (Tecido, Infeccao/inflamacao, Moisture/exsudato, Edges/bordas).
-- No plano/acoes futuras, preencher a observacao equivale a marcar a acao.

CREATE TABLE evolucoes_curativos (
    id                          BIGSERIAL PRIMARY KEY,
    agendamento_id              BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),

    -- 1. Avaliacao diaria
    comprimento                 NUMERIC(6, 2),
    largura                     NUMERIC(6, 2),
    profundidade                NUMERIC(6, 2),
    area_aproximada             NUMERIC(8, 2),
    tecido                      VARCHAR(20),
    infeccao_inflamacao         VARCHAR(20),
    exsudato                    VARCHAR(20),
    bordas                      VARCHAR(20),
    odor_presente               BOOLEAN,
    dor_escala                  SMALLINT CHECK (dor_escala IS NULL OR (dor_escala BETWEEN 0 AND 10)),
    pele_perilesional           TEXT,

    -- 2. Intervencoes
    limpeza_irrigacao           TEXT,
    desbridamento               VARCHAR(20),
    desbridamento_obs           TEXT,
    cobertura_primaria          TEXT,
    orientacoes_paciente        TEXT,

    -- 3. Avaliacao da evolucao
    evolucao                    VARCHAR(10),
    observacoes                 TEXT,

    -- 4. Plano / acoes futuras (observacao preenchida = acao marcada)
    plano_manter_conduta        TEXT,
    plano_alterar_cobertura     TEXT,
    plano_solicitar_exames      TEXT,
    plano_encaminhamento        TEXT,
    retorno_previsto            TEXT,

    criado_em                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMP
);

CREATE INDEX idx_evolucoes_curativos_agendamento_id ON evolucoes_curativos(agendamento_id);

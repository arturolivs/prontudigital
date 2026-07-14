-- Separa os campos de evolução clínica da tabela agendamentos para uma tabela própria.
-- Estrutura baseada no Checklist de Avaliação de Feridas.

CREATE TABLE evolucoes_clinicas (
    id                          BIGSERIAL PRIMARY KEY,
    agendamento_id              BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),

    -- Dados da ferida
    localizacao_anatomica       TEXT,
    etiologia                   TEXT,
    tempo_evolucao              VARCHAR(100),

    -- Mensuração
    medida_comprimento          NUMERIC(6, 2),
    medida_largura              NUMERIC(6, 2),
    medida_profundidade         NUMERIC(6, 2),
    tunelizacao                 BOOLEAN,
    descolamento_bordas         BOOLEAN,

    -- Leito da ferida (checkbox + percentual)
    epitelizacao_percentual     SMALLINT CHECK (epitelizacao_percentual IS NULL OR (epitelizacao_percentual BETWEEN 0 AND 100)),
    granulacao_percentual       SMALLINT CHECK (granulacao_percentual IS NULL OR (granulacao_percentual BETWEEN 0 AND 100)),
    esfacelo_percentual         SMALLINT CHECK (esfacelo_percentual IS NULL OR (esfacelo_percentual BETWEEN 0 AND 100)),
    necrose_percentual          SMALLINT CHECK (necrose_percentual IS NULL OR (necrose_percentual BETWEEN 0 AND 100)),
    tendao_exposto              BOOLEAN,
    musculo_exposto             BOOLEAN,
    osso_exposto                BOOLEAN,

    -- Exsudato
    exsudato_volume             VARCHAR(20),
    exsudato_caracteristica     VARCHAR(30),
    odor_intensidade            VARCHAR(20),

    -- Dor
    classificacao_dor           VARCHAR(20),

    -- Avaliação vascular
    grau_edema                  VARCHAR(20),
    avaliacao_pulsos            VARCHAR(20),

    -- Evolução da ferida
    evolucao_ferida             VARCHAR(20),

    -- Conduta
    limpeza_lesao               BOOLEAN,
    desbridamento               BOOLEAN,
    cobertura_aplicada          BOOLEAN,
    cobertura_descricao         TEXT,
    terapia_adjuvante           BOOLEAN,
    terapia_adjuvante_descricao TEXT,
    orientacoes_fornecidas      BOOLEAN,

    -- Observações
    observacoes                 TEXT
);

-- Bordas (múltipla escolha)
CREATE TABLE evolucao_caracteristicas_bordas (
    evolucao_id    BIGINT NOT NULL REFERENCES evolucoes_clinicas(id),
    caracteristica VARCHAR(30) NOT NULL,
    PRIMARY KEY (evolucao_id, caracteristica)
);

-- Pele perilesional (múltipla escolha)
CREATE TABLE evolucao_caracteristicas_perilesional (
    evolucao_id    BIGINT NOT NULL REFERENCES evolucoes_clinicas(id),
    caracteristica VARCHAR(30) NOT NULL,
    PRIMARY KEY (evolucao_id, caracteristica)
);

-- Sinais de infecção (múltipla escolha)
CREATE TABLE evolucao_sinais_infeccao (
    evolucao_id BIGINT NOT NULL REFERENCES evolucoes_clinicas(id),
    sinal       VARCHAR(30) NOT NULL,
    PRIMARY KEY (evolucao_id, sinal)
);

-- Sinais de evolução (múltipla escolha)
CREATE TABLE evolucao_sinais_evolucao (
    evolucao_id BIGINT NOT NULL REFERENCES evolucoes_clinicas(id),
    sinal       VARCHAR(30) NOT NULL,
    PRIMARY KEY (evolucao_id, sinal)
);

-- Remove colunas clínicas da tabela agendamentos (adicionadas na V15)
ALTER TABLE agendamentos
    DROP COLUMN localizacao_anatomica,
    DROP COLUMN tipo_lesao,
    DROP COLUMN medida_comprimento,
    DROP COLUMN medida_largura,
    DROP COLUMN medida_profundidade,
    DROP COLUMN aspecto_leito_ferida,
    DROP COLUMN exsudato_volume,
    DROP COLUMN exsudato_caracteristica,
    DROP COLUMN condicao_bordas,
    DROP COLUMN aspecto_perilesional,
    DROP COLUMN sinais_flogisticos,
    DROP COLUMN presenca_odor,
    DROP COLUMN limpeza_realizada,
    DROP COLUMN coberturas_aplicadas,
    DROP COLUMN produtos_utilizados,
    DROP COLUMN aceitacao_procedimento,
    DROP COLUMN escala_dor,
    DROP COLUMN intercorrencias,
    DROP COLUMN cuidados_curativo,
    DROP COLUMN sinais_alerta,
    DROP COLUMN orientacao_retorno;

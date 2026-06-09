-- Separa os campos de evolução clínica da tabela agendamentos para uma tabela própria

CREATE TABLE evolucoes_clinicas (
    id                      BIGSERIAL PRIMARY KEY,
    agendamento_id          BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),

    -- Seção 2: Avaliação da lesão
    localizacao_anatomica   TEXT,
    tipo_lesao              VARCHAR(100),
    medida_comprimento      NUMERIC(6, 2),
    medida_largura          NUMERIC(6, 2),
    medida_profundidade     NUMERIC(6, 2),
    aspecto_leito_ferida    TEXT,
    exsudato_volume         VARCHAR(20),
    exsudato_caracteristica VARCHAR(30),
    condicao_bordas         TEXT,
    aspecto_perilesional    TEXT,
    sinais_flogisticos      BOOLEAN,
    presenca_odor           BOOLEAN,

    -- Seção 3: Procedimento realizado
    limpeza_realizada       TEXT,
    coberturas_aplicadas    TEXT,
    produtos_utilizados     TEXT,

    -- Seção 4: Resposta do paciente
    aceitacao_procedimento  TEXT,
    escala_dor              SMALLINT CHECK (escala_dor IS NULL OR (escala_dor BETWEEN 0 AND 10)),
    intercorrencias         TEXT,

    -- Seção 5: Orientações fornecidas
    cuidados_curativo       TEXT,
    sinais_alerta           TEXT,
    orientacao_retorno      TEXT
);

-- Migra dados existentes para a nova tabela
INSERT INTO evolucoes_clinicas (
    agendamento_id, localizacao_anatomica, tipo_lesao, medida_comprimento,
    medida_largura, medida_profundidade, aspecto_leito_ferida, exsudato_volume,
    exsudato_caracteristica, condicao_bordas, aspecto_perilesional, sinais_flogisticos,
    presenca_odor, limpeza_realizada, coberturas_aplicadas, produtos_utilizados,
    aceitacao_procedimento, escala_dor, intercorrencias, cuidados_curativo,
    sinais_alerta, orientacao_retorno
)
SELECT
    id, localizacao_anatomica, tipo_lesao, medida_comprimento,
    medida_largura, medida_profundidade, aspecto_leito_ferida, exsudato_volume,
    exsudato_caracteristica, condicao_bordas, aspecto_perilesional, sinais_flogisticos,
    presenca_odor, limpeza_realizada, coberturas_aplicadas, produtos_utilizados,
    aceitacao_procedimento, escala_dor, intercorrencias, cuidados_curativo,
    sinais_alerta, orientacao_retorno
FROM agendamentos
WHERE localizacao_anatomica IS NOT NULL
   OR tipo_lesao IS NOT NULL
   OR limpeza_realizada IS NOT NULL
   OR escala_dor IS NOT NULL;

-- Remove colunas clínicas da tabela agendamentos
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

-- Evolução clínica de enfermagem pós-curativo (tratamento.md)
-- Campos opcionais: preenchidos somente em agendamentos do tipo TRATAMENTO_FERIDAS

ALTER TABLE agendamentos
    -- Seção 2: Avaliação da lesão
    ADD COLUMN localizacao_anatomica    TEXT,
    ADD COLUMN tipo_lesao               VARCHAR(100),
    ADD COLUMN medida_comprimento       NUMERIC(6, 2),
    ADD COLUMN medida_largura           NUMERIC(6, 2),
    ADD COLUMN medida_profundidade      NUMERIC(6, 2),
    ADD COLUMN aspecto_leito_ferida     TEXT,
    ADD COLUMN exsudato_volume          VARCHAR(20),
    ADD COLUMN exsudato_caracteristica  VARCHAR(30),
    ADD COLUMN condicao_bordas          TEXT,
    ADD COLUMN aspecto_perilesional     TEXT,
    ADD COLUMN sinais_flogisticos       BOOLEAN,
    ADD COLUMN presenca_odor            BOOLEAN,

    -- Seção 3: Procedimento realizado
    ADD COLUMN limpeza_realizada        TEXT,
    ADD COLUMN coberturas_aplicadas     TEXT,
    ADD COLUMN produtos_utilizados      TEXT,

    -- Seção 4: Resposta do paciente
    ADD COLUMN aceitacao_procedimento   TEXT,
    ADD COLUMN escala_dor               SMALLINT CHECK (escala_dor IS NULL OR (escala_dor BETWEEN 0 AND 10)),
    ADD COLUMN intercorrencias          TEXT,

    -- Seção 5: Orientações fornecidas
    ADD COLUMN cuidados_curativo        TEXT,
    ADD COLUMN sinais_alerta            TEXT,
    ADD COLUMN orientacao_retorno       TEXT;

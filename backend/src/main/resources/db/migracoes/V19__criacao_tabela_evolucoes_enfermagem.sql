-- Ficha de Evolucao de Enfermagem (modelo ajustar_campos/modelos/FICHA DE EVOLUCAO DE ENFERMAGEM.pdf).
-- 1:1 com agendamento do tipo AVALIACAO. Avaliacao da ferida segue o acronimo TIME
-- (Tecido, Infeccao/inflamacao, Moisture/exsudato, Edges/bordas).

CREATE TABLE evolucoes_enfermagem (
    id                          BIGSERIAL PRIMARY KEY,
    agendamento_id              BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),

    -- 1. Dados da avaliacao
    data_avaliacao              DATE,
    hora_avaliacao              TIME,
    diagnostico_medico          TEXT,
    comorb_diabetes             BOOLEAN,
    comorb_hipertensao          BOOLEAN,
    comorb_doenca_vascular      BOOLEAN,
    comorb_neuropatia           BOOLEAN,
    comorb_outras               BOOLEAN,
    comorb_outras_detalhe       TEXT,
    medicamentos_relevantes     TEXT,

    -- 2. Avaliacao da ferida (TIME)
    localizacao_anatomica       TEXT,
    tipo_ferida                 VARCHAR(20),
    tipo_ferida_outra           TEXT,
    dimensoes                   TEXT,
    comprimento                 NUMERIC(6, 2),
    largura                     NUMERIC(6, 2),
    profundidade                NUMERIC(6, 2),
    tunelizacao                 BOOLEAN,
    descolamento                BOOLEAN,
    tecido_leito                VARCHAR(20),
    infeccao_inflamacao         VARCHAR(30),
    exsudato                    VARCHAR(20),
    exsudato_tipo               TEXT,
    bordas                      VARCHAR(20),
    pele_perilesional           TEXT,
    -- INTEGER (nao SMALLINT): a entidade mapeia Integer e o Hibernate valida
    -- o tipo no boot (ddl-auto: validate nos perfis dev e prod).
    dor_escala                  INTEGER CHECK (dor_escala IS NULL OR (dor_escala BETWEEN 0 AND 10)),
    sinais_vitais               TEXT,
    pa                          VARCHAR(30),
    fc                          VARCHAR(30),
    fr                          VARCHAR(30),
    temp                        VARCHAR(30),

    -- 3. Diagnosticos de enfermagem
    diag_integridade_pele       BOOLEAN,
    diag_integridade_tissular   BOOLEAN,
    diag_risco_infeccao         BOOLEAN,
    diag_perfusao_ineficaz      BOOLEAN,
    diag_dor_aguda              BOOLEAN,
    diag_outros                 TEXT,

    -- 4. Conduta realizada
    limpeza                     VARCHAR(10),
    limpeza_outro               TEXT,
    desbridamento               VARCHAR(20),
    cobertura_primaria          TEXT,
    cobertura_secundaria        TEXT,
    fixacao                     TEXT,
    orientacoes_paciente        TEXT,

    -- 5. Avaliacao da evolucao
    avaliacao_evolucao          VARCHAR(10),
    reducao_area                BOOLEAN,
    observacoes                 TEXT,

    -- 6. Plano
    plano_manter_conduta        BOOLEAN,
    plano_ajustar_cobertura     BOOLEAN,
    plano_avaliacao_medica      BOOLEAN,
    plano_solicitar_exames      BOOLEAN,
    plano_encaminhamento        BOOLEAN,
    retorno_dias                INTEGER,

    criado_em                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMP
);

CREATE INDEX idx_evolucoes_enfermagem_agendamento_id ON evolucoes_enfermagem(agendamento_id);

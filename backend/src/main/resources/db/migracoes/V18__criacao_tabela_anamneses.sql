-- RF13: Anamnese do paciente conforme modelo (ajustar_campos/modelos/Anamnese.pdf).
-- Uma anamnese por paciente (paciente_uuid único). Vinculada ao paciente, nao ao atendimento.
-- Dados de identificacao (nome, CPF, nascimento, sexo, contato) vem do cadastro do paciente.

CREATE TABLE anamneses (
    id                                  BIGSERIAL PRIMARY KEY,
    uuid                                UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    paciente_uuid                       UUID NOT NULL UNIQUE,

    -- Identificacao complementar
    profissao                           TEXT,
    responsavel_cuidador                TEXT,

    -- Historia da ferida
    motivo_consulta                     TEXT,
    tempo_existencia_ferida             TEXT,
    como_ferida_surgiu                  TEXT,
    data_inicio_aproximada              DATE,
    tratamentos_anteriores              TEXT,
    curativos_previos                   TEXT,

    -- Historico de saude (Sim/Nao + detalhe)
    diabetes_mellitus                   BOOLEAN,
    diabetes_mellitus_detalhe           TEXT,
    hipertensao_arterial                BOOLEAN,
    hipertensao_arterial_detalhe        TEXT,
    doenca_venosa_cronica               BOOLEAN,
    doenca_venosa_cronica_detalhe       TEXT,
    doenca_arterial_periferica          BOOLEAN,
    doenca_arterial_periferica_detalhe  TEXT,
    insuficiencia_renal                 BOOLEAN,
    insuficiencia_renal_detalhe         TEXT,
    cancer                              BOOLEAN,
    cancer_detalhe                      TEXT,
    problemas_neurologicos              BOOLEAN,
    problemas_neurologicos_detalhe      TEXT,
    historico_cirurgias                 BOOLEAN,
    historico_cirurgias_detalhe         TEXT,

    -- Medicamentos em uso
    med_antibioticos                    BOOLEAN,
    med_anticoagulantes                 BOOLEAN,
    med_corticoides                     BOOLEAN,
    med_insulina_hipoglicemiantes       BOOLEAN,
    med_outros_continuos                BOOLEAN,

    -- Alergias
    alergia_medicamentos                BOOLEAN,
    alergia_produtos_topicos            BOOLEAN,
    alergia_curativos_adesivos          BOOLEAN,

    -- Habitos de vida
    tabagismo                           BOOLEAN,
    consumo_alcool                      BOOLEAN,
    alimentacao_estado_nutricional      TEXT,
    ingestao_hidrica                    BOOLEAN,
    ingestao_hidrica_detalhe            TEXT,

    -- Mobilidade
    deambula_sozinho                    BOOLEAN,
    deambula_sozinho_detalhe            TEXT,
    acamado_ou_cadeirante               BOOLEAN,
    acamado_ou_cadeirante_detalhe       TEXT,
    uso_dispositivos                    BOOLEAN,
    uso_dispositivos_detalhe            TEXT,
    mudanca_posicao_leito               BOOLEAN,
    mudanca_posicao_leito_detalhe       TEXT,

    -- Outros
    exames_recentes                     BOOLEAN,
    rede_apoio                          VARCHAR(40),
    acompanhamento_medico               BOOLEAN,
    acompanhamento_medico_detalhe       TEXT,

    registrado_por                      UUID,

    criado_em                           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em                       TIMESTAMP
);

CREATE INDEX idx_anamneses_paciente_uuid ON anamneses(paciente_uuid);

-- RF06: servicos/procedimentos em tabela (substitui o enum TipoProcedimento).
-- O campo codigo guarda o valor legado do enum para os registros migrados;
-- procedimentos novos criados pelo ADMIN nao possuem codigo.

CREATE TABLE procedimentos (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    codigo                  VARCHAR(30) UNIQUE,
    nome                    VARCHAR(100) NOT NULL,
    descricao               TEXT,
    duracao_padrao_minutos  INTEGER,
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP
);

CREATE UNIQUE INDEX uk_procedimentos_nome ON procedimentos (LOWER(nome));

-- Catalogo inicial. Dado de REFERENCIA, nao de exemplo: sem nenhuma linha
-- aqui a tela publica de agendamento nao oferece nada e a instalacao nasce
-- inutilizavel ate o ADMIN cadastrar o primeiro procedimento. O campo codigo
-- guarda o valor do enum legado TipoProcedimento; procedimentos criados pelo
-- ADMIN nascem sem codigo.
INSERT INTO procedimentos (codigo, nome, descricao, duracao_padrao_minutos)
VALUES
    ('PODIATRIA', 'Podiatria', 'Cuidados podologicos de enfermagem', 60),
    ('TRATAMENTO_FERIDAS', 'Tratamento de Feridas', 'Avaliacao e tratamento de feridas e curativos', 60);

-- Agendamentos passam a referenciar o procedimento por id.
-- A coluna tipo_procedimento e mantida (agora opcional) para compatibilidade
-- durante a transicao.
ALTER TABLE agendamentos
    ADD COLUMN procedimento_id BIGINT REFERENCES procedimentos(id);

ALTER TABLE agendamentos
    ALTER COLUMN tipo_procedimento DROP NOT NULL;
ALTER TABLE agendamentos
    ALTER COLUMN tipo_procedimento DROP DEFAULT;

CREATE INDEX idx_agendamentos_procedimento_id ON agendamentos(procedimento_id);

-- =============================================================
-- V25 - Dados cadastrais complementares de usuarios (RF04 / RF05)
--
-- RF04 (paciente): CPF, data de nascimento e endereco.
-- RF05 (profissional): registro no COREN e especialidade.
--
-- Todas as colunas sao opcionais: os cadastros existentes foram
-- criados apenas com nome e telefone (cadastro rapido de paciente)
-- e continuam validos.
-- =============================================================

ALTER TABLE usuarios
    -- RF04 - identificacao do paciente
    ADD COLUMN cpf             VARCHAR(11),
    ADD COLUMN data_nascimento DATE,

    -- RF04 - endereco
    ADD COLUMN cep             VARCHAR(8),
    ADD COLUMN logradouro      VARCHAR(150),
    ADD COLUMN numero          VARCHAR(20),
    ADD COLUMN complemento     VARCHAR(100),
    ADD COLUMN bairro          VARCHAR(100),
    ADD COLUMN cidade          VARCHAR(100),
    ADD COLUMN uf              CHAR(2),

    -- RF05 - credenciais do profissional
    ADD COLUMN coren           VARCHAR(20),
    ADD COLUMN especialidade   VARCHAR(100);

-- CPF e unico quando informado; varios cadastros sem CPF continuam
-- possiveis porque NULL nao conflita em indice unico no Postgres.
CREATE UNIQUE INDEX idx_usuarios_cpf ON usuarios(cpf);

-- Mesma regra para o COREN.
CREATE UNIQUE INDEX idx_usuarios_coren ON usuarios(coren);

-- Guarda-corpo de formato. A data de nascimento nao entra aqui de proposito:
-- CURRENT_DATE nao e imutavel e quebraria dump/restore; @Past no DTO cobre.
ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_cpf_digitos
        CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    ADD CONSTRAINT chk_usuarios_cep_digitos
        CHECK (cep IS NULL OR cep ~ '^[0-9]{8}$');

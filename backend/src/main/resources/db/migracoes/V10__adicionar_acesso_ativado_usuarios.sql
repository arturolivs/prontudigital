-- O email deixa de ser obrigatorio: o cadastro rapido de paciente (RF04) cria
-- o usuario so com nome e telefone, e o acesso e ativado depois.
ALTER TABLE usuarios
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE usuarios
    ADD COLUMN acesso_ativado BOOLEAN NOT NULL DEFAULT FALSE;

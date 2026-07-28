ALTER TABLE usuarios
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE usuarios
    ADD COLUMN acesso_ativado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE usuarios SET acesso_ativado = TRUE WHERE email IS NOT NULL;

-- =============================================================
-- V29 - Avatar do usuario
--
-- Guarda apenas a referencia ao arquivo, nunca o binario: a imagem
-- vai para o ArmazenamentoService (volume em disco hoje, objeto em
-- nuvem no futuro), sob a chave gravada em avatar_chave. Mesma
-- decisao ja tomada para os anexos do prontuario (V20) e para a
-- logo da clinica (V28) — banco pequeno e dump rapido.
--
-- Ambas as colunas sao opcionais e andam juntas: ou o usuario tem
-- avatar (as duas preenchidas) ou nao tem (as duas nulas). O CHECK
-- abaixo impede o meio-termo, que produziria um download sem
-- Content-Type ou uma referencia orfa.
-- =============================================================

ALTER TABLE usuarios
    ADD COLUMN avatar_chave         VARCHAR(255),
    ADD COLUMN avatar_tipo_conteudo VARCHAR(100);

ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_avatar_completo
        CHECK (
            (avatar_chave IS NULL AND avatar_tipo_conteudo IS NULL)
            OR (avatar_chave IS NOT NULL AND avatar_tipo_conteudo IS NOT NULL)
        );

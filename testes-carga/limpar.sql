-- =============================================================
-- Remove a massa criada por seed.sql e o que o teste gravou por cima.
--
-- A ordem importa. Nem toda FK que aponta para `agendamentos` cascateia:
--   historico_agendamentos (V8)      ON DELETE CASCADE  — sai sozinho
--   log_notificacoes_whatsapp (V9)   ON DELETE CASCADE  — sai sozinho
--   evolucoes_enfermagem (V19)       sem ON DELETE      — precisa ir antes
--   evolucoes_curativos  (V20)       sem ON DELETE      — precisa ir antes
--   agendamentos.avaliacao_id (V5)   ON DELETE RESTRICT — tratamento antes
--                                                         da avaliacao
--
-- E `agendamentos` referencia usuario por UUID solto, sem FK: apagar o
-- usuario primeiro nao daria erro nenhum, so deixaria agendamento orfao
-- que nenhuma tela abre e que o proximo DELETE por prefixo nao alcanca.
--
-- Uso (a partir de docker/prod/):
--   docker compose -f docker-compose.prod.yml exec -T postgres \
--     psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../../testes-carga/limpar.sql
-- =============================================================

BEGIN;

CREATE TEMP TABLE uuids_carga ON COMMIT DROP AS
SELECT uuid FROM usuarios WHERE username LIKE 'carga\_%';

CREATE TEMP TABLE ids_carga ON COMMIT DROP AS
SELECT id FROM agendamentos
 WHERE profissional_uuid IN (SELECT uuid FROM uuids_carga)
    OR paciente_uuid     IN (SELECT uuid FROM uuids_carga);

-- 1. Evolucoes: sem ON DELETE, travam o DELETE do agendamento
DELETE FROM evolucoes_enfermagem WHERE agendamento_id IN (SELECT id FROM ids_carga);
DELETE FROM evolucoes_curativos  WHERE agendamento_id IN (SELECT id FROM ids_carga);

-- 2. Tratamentos antes das avaliacoes (auto-FK RESTRICT)
DELETE FROM agendamentos
 WHERE id IN (SELECT id FROM ids_carga) AND avaliacao_id IS NOT NULL;

DELETE FROM agendamentos WHERE id IN (SELECT id FROM ids_carga);

-- 3. Expediente
DELETE FROM horarios_trabalho
 WHERE profissional_uuid IN (SELECT uuid FROM uuids_carga);

-- 4. Usuarios. usuario_perfis (V3) e refresh_tokens (V4) saem por CASCADE.
DELETE FROM usuarios WHERE username LIKE 'carga\_%';

COMMIT;

-- Fora da transacao: VACUUM nao roda dentro de uma.
VACUUM ANALYZE agendamentos;
VACUUM ANALYZE usuarios;

SELECT 'usuarios de carga restantes' AS conjunto, count(*) AS total
  FROM usuarios WHERE username LIKE 'carga\_%'
UNION ALL
SELECT 'agendamentos restantes', count(*) FROM agendamentos;

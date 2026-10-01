-- =============================================================
-- Massa de teste para os cenarios de carga (RNF05 / RNF06).
--
-- POR QUE ISTO EXISTE: desde a correcao da §1.2 do DEPLOY.md o banco
-- nasce sem nenhum usuario e sem nenhum agendamento. Um teste de carga
-- contra tabela vazia aprova qualquer indice, qualquer plano de query e
-- qualquer tamanho de pool — o Postgres devolve tudo do cache e o p95
-- fica lindo. O numero so significa alguma coisa com volume parecido
-- com o de uma clinica em operacao.
--
-- O QUE CRIA:
--     1  ADMIN           carga_admin
--     4  PROFISSIONAIS   carga_prof_1 .. carga_prof_4
--   500  PACIENTES       carga_pac_1 .. carga_pac_500
--   ~19k agendamentos    2 anos para tras, 60 dias para frente
--    20  janelas de horario de trabalho (seg-sex, 08h-18h)
--
-- Senha de todos: CargaTeste!2026  (igual a SENHA_CARGA de k6/config.js)
--
-- >>> NUNCA rode isto num banco com dado real de paciente. <<<
-- Todo usuario criado aqui tem username com prefixo `carga_`, e o
-- limpar.sql apaga exatamente esse conjunto — mas o caminho seguro e uma
-- instalacao separada, nao a de producao.
--
-- Uso (a partir de docker/prod/, com a stack no ar):
--   docker compose -f docker-compose.prod.yml exec -T postgres \
--     psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../../testes-carga/seed.sql
-- =============================================================

BEGIN;

-- Guarda-corpo: recusa rodar se o banco tiver usuario que nao seja de
-- carga nem o ADMIN de bootstrap. Errar de terminal e facil; o custo de
-- errar aqui e escrever 19 mil agendamentos falsos na agenda da clinica.
DO $$
DECLARE
    estranhos INTEGER;
BEGIN
    SELECT count(*) INTO estranhos
      FROM usuarios
     WHERE username NOT LIKE 'carga\_%';

    IF estranhos > 1 THEN
        RAISE EXCEPTION
            'Banco com % usuarios fora do conjunto de carga. Este script so roda '
            'em instalacao de teste — veja o cabecalho do arquivo.', estranhos;
    END IF;
END $$;

-- BCrypt gerado pelo proprio banco, para nao versionar hash de senha —
-- foi exatamente isso que a §1.2 do DEPLOY.md tirou do repositorio.
-- gen_salt('bf', 10) usa a mesma forca do BCryptPasswordEncoder padrao
-- (SecurityConfig.java), entao o custo de CPU por login durante o teste
-- e o custo real de producao, que e o que se quer medir.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------------------------------------------
-- 1. Usuarios
-- -------------------------------------------------------------
INSERT INTO usuarios (username, email, senha_hash, nome_completo, telefone,
                      ativo, acesso_ativado)
VALUES ('carga_admin', 'carga_admin@teste.local',
        crypt('CargaTeste!2026', gen_salt('bf', 10)),
        'Admin de Carga', '11900000000', TRUE, TRUE);

INSERT INTO usuarios (username, email, senha_hash, nome_completo, telefone,
                      coren, especialidade, ativo, acesso_ativado)
SELECT 'carga_prof_' || n,
       'carga_prof_' || n || '@teste.local',
       crypt('CargaTeste!2026', gen_salt('bf', 10)),
       'Profissional de Carga ' || n,
       '1191000' || lpad(n::text, 4, '0'),
       'CARGA-' || lpad(n::text, 4, '0'),
       'Enfermagem',
       TRUE, TRUE
  FROM generate_series(1, 4) n;

-- Pacientes sem e-mail: e o cadastro rapido do RF04 (nome + telefone),
-- que e como a maioria entra de verdade. O e-mail deixou de ser
-- obrigatorio na V10 e o indice unico de CPF ignora NULL.
INSERT INTO usuarios (username, senha_hash, nome_completo, telefone,
                      data_nascimento, ativo, acesso_ativado)
SELECT 'carga_pac_' || n,
       crypt('CargaTeste!2026', gen_salt('bf', 10)),
       'Paciente de Carga ' || n,
       '1192' || lpad(n::text, 7, '0'),
       (DATE '1950-01-01' + ((n * 37) % 25000))::date,
       TRUE, FALSE
  FROM generate_series(1, 500) n;

-- -------------------------------------------------------------
-- 2. Perfis
-- -------------------------------------------------------------
INSERT INTO usuario_perfis (usuario_id, perfil_id)
SELECT u.id, p.id
  FROM usuarios u
  JOIN perfis p
    ON p.nome = CASE
           WHEN u.username = 'carga_admin'       THEN 'ADMIN'
           WHEN u.username LIKE 'carga\_prof\_%' THEN 'PROFISSIONAL'
           ELSE                                       'PACIENTE'
       END
 WHERE u.username LIKE 'carga\_%';

-- -------------------------------------------------------------
-- 3. Horario de trabalho (RF05 / V22)
--
-- Sem janela cadastrada o profissional aceita agendamento a qualquer
-- hora, o que tornaria o cenario de escrita mais barato do que a
-- realidade: validar o expediente e parte do custo da gravacao.
-- -------------------------------------------------------------
INSERT INTO horarios_trabalho (profissional_uuid, dia_semana, hora_inicio, hora_fim)
SELECT u.uuid, d, TIME '08:00', TIME '18:00'
  FROM usuarios u
 CROSS JOIN generate_series(1, 5) d
 WHERE u.username LIKE 'carga\_prof\_%';

-- -------------------------------------------------------------
-- 4. Agendamentos — 2 anos para tras, 60 dias para frente
--
-- Volume e o ponto: o relatorio do RF19 varre periodo e a tela de agenda
-- ordena por inicio_em. Com 200 linhas qualquer plano parece otimo; com
-- 19 mil os indices da V5 comecam a ser exercitados de verdade.
--
-- `chave` e um pseudo-aleatorio deterministico: mesma massa toda vez que
-- o seed roda, entao duas execucoes do teste sao comparaveis entre si.
-- Aleatoriedade real tornaria cada rodada uma base diferente e a
-- comparacao antes/depois de um ajuste perderia o sentido.
--
-- Status separado entre passado e futuro porque as taxas do RF19
-- (comparecimento, falta) tem como denominador realizados + faltas: com
-- tudo AGENDADO, o relatorio nunca percorre o caminho que importa.
-- -------------------------------------------------------------
WITH prof AS (
    SELECT uuid, row_number() OVER (ORDER BY id) AS rn
      FROM usuarios WHERE username LIKE 'carga\_prof\_%'
),
pac AS (
    SELECT uuid, row_number() OVER (ORDER BY id) AS rn
      FROM usuarios WHERE username LIKE 'carga\_pac\_%'
),
proc AS (
    SELECT id, codigo, row_number() OVER (ORDER BY id) AS rn
      FROM procedimentos
),
dias AS (
    SELECT g AS d
      FROM generate_series((CURRENT_DATE - INTERVAL '2 years')::timestamp,
                           (CURRENT_DATE + INTERVAL '60 days')::timestamp,
                           INTERVAL '1 day') g
     WHERE EXTRACT(ISODOW FROM g) <= 5
),
base AS (
    SELECT prof.uuid AS prof_uuid,
           dias.d    AS d,
           s         AS hora,
           (EXTRACT(DOY FROM dias.d)::int * 7 + s * 13 + prof.rn::int) AS chave
      FROM prof
     CROSS JOIN dias
     CROSS JOIN generate_series(8, 16) s
)
INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em,
                          status, tipo, local_atendimento, paciente_acamado,
                          procedimento_id, tipo_procedimento, concluido_em)
SELECT pac.uuid,
       base.prof_uuid,
       base.d + make_interval(hours => base.hora),
       base.d + make_interval(hours => base.hora, mins => 30),
       CASE
           WHEN base.d::date < CURRENT_DATE
               THEN (ARRAY['REALIZADO', 'REALIZADO', 'REALIZADO', 'REALIZADO',
                           'NAO_COMPARECEU', 'CANCELADO'])[1 + (base.chave % 6)]
           ELSE (ARRAY['AGENDADO', 'CONFIRMADO'])[1 + (base.chave % 2)]
       END,
       CASE WHEN base.chave % 3 = 0 THEN 'AVALIACAO' ELSE 'TRATAMENTO' END,
       CASE WHEN base.chave % 11 = 0 THEN 'RESIDENCIAL' ELSE 'CLINICA' END,
       (base.chave % 23 = 0),
       proc.id,
       -- Preenchido junto com procedimento_id porque a coluna legada
       -- continua sendo lida para compatibilidade (RF06).
       proc.codigo,
       CASE
           WHEN base.d::date < CURRENT_DATE AND base.chave % 6 < 4
               THEN base.d + make_interval(hours => base.hora, mins => 35)
           ELSE NULL
       END
  FROM base
  JOIN pac  ON pac.rn  = 1 + (base.chave % (SELECT count(*) FROM pac))
  JOIN proc ON proc.rn = 1 + (base.chave % (SELECT count(*) FROM proc));

COMMIT;

-- -------------------------------------------------------------
-- ANALYZE, e nao e opcional.
--
-- O planejador do Postgres decide por estatistica. Recem-carregada, a
-- tabela ainda parece vazia para ele, e o primeiro teste mediria o plano
-- errado — sequential scan onde producao usaria indice.
-- -------------------------------------------------------------
ANALYZE usuarios;
ANALYZE usuario_perfis;
ANALYZE agendamentos;
ANALYZE horarios_trabalho;

SELECT 'usuarios de carga' AS conjunto, count(*) AS total
  FROM usuarios WHERE username LIKE 'carga\_%'
UNION ALL
SELECT 'agendamentos', count(*) FROM agendamentos
UNION ALL
SELECT 'horarios de trabalho', count(*) FROM horarios_trabalho;

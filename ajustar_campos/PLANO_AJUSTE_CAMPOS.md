# Plano — Ajuste dos campos das consultas e evoluções

Objetivo: alinhar os formulários preenchidos nas consultas aos modelos em `ajustar_campos/modelos`:

| Tipo de agendamento | Formulários (modelos) |
|---|---|
| **AVALIACAO** | `Anamnese.pdf` + `FICHA DE EVOLUÇÃO DE ENFERMAGEM.pdf` |
| **TRATAMENTO** | `FICHA DE EVOLUÇÃO DIÁRIA - CURATIVOS.pdf` |

Premissas:
- Banco será gerado do zero → **alterar/remover migrações de criação**, sem migração de alteração.
- Campos existentes que não constam nos modelos **podem ser removidos**.
- Dados de identificação do cabeçalho dos PDFs (nome, CPF, nascimento, sexo, telefone, e-mail) já vêm do cadastro do paciente — não duplicar nos formulários.

---

## Situação atual (o que muda)

| Hoje | Problema |
|---|---|
| `Anamnese` (1:1 paciente, RF13): 8 campos de texto livre, sem UI no frontend | Modelo tem ~30 campos estruturados (Sim/Não + detalhe, checkboxes) |
| `EvolucaoClinica` (1:1 agendamento): checklist antigo de feridas (percentuais do leito, edema, pulsos, 4 tabelas de múltipla escolha) | Não corresponde a nenhum modelo; é usada tanto em Avaliação quanto Tratamento |
| Página `agenda/procedimento/[id]` exibe o mesmo formulário para os dois tipos | Avaliação e Tratamento passam a ter formulários distintos |

---

## Modelagem alvo

### 1. `anamneses` (remodelada — mantém 1:1 com paciente, preenchida na Avaliação)

Identificação complementar: `profissao`, `responsavel_cuidador`.

História da ferida: `motivo_consulta`, `tempo_existencia_ferida`, `como_ferida_surgiu`,
`data_inicio_aproximada`, `tratamentos_anteriores`, `curativos_previos`.

Histórico de saúde (BOOLEAN + detalhe TEXT cada): `diabetes_mellitus`, `hipertensao_arterial`,
`doenca_venosa_cronica`, `doenca_arterial_periferica`, `insuficiencia_renal`, `cancer`,
`problemas_neurologicos`, `historico_cirurgias`.

Medicamentos em uso (BOOLEAN): `med_antibioticos`, `med_anticoagulantes`, `med_corticoides`,
`med_insulina_hipoglicemiantes`, `med_outros_continuos`.

Alergias (BOOLEAN): `alergia_medicamentos`, `alergia_produtos_topicos`, `alergia_curativos_adesivos`.

Hábitos de vida: `tabagismo` (bool), `consumo_alcool` (bool), `alimentacao_estado_nutricional` (texto),
`ingestao_hidrica` (bool + detalhe).

Mobilidade (BOOLEAN + detalhe): `deambula_sozinho`, `acamado_ou_cadeirante`, `uso_dispositivos`,
`mudanca_posicao_leito`.

Outros: `exames_recentes` (bool), `rede_apoio` (enum: CUIDADOR | FAMILIAR_RESPONSAVEL |
CONDICOES_CURATIVOS_CASA), `acompanhamento_medico` (bool) + `acompanhamento_medico_detalhe`
(nome/especialidade/contato).

**Removidos** (não constam no modelo): `queixa_principal`, `historico_doenca_atual`,
`historico_medico_pregresso`, `historico_familiar`, `habitos`, `observacoes`,
`alergias` (texto), `medicamentos_em_uso` (texto).

### 2. `evolucoes_enfermagem` (nova — 1:1 com agendamento AVALIACAO)

1. Dados da avaliação: `data_avaliacao`, `hora_avaliacao` (pré-preenchidos do agendamento),
   `diagnostico_medico`; comorbidades (bool): `comorb_diabetes`, `comorb_hipertensao`,
   `comorb_doenca_vascular`, `comorb_neuropatia`, `comorb_outras` + `comorb_outras_detalhe`;
   `medicamentos_relevantes`.
2. Avaliação da ferida (TIME): `localizacao_anatomica`; `tipo_ferida` (enum: CIRURGICA | TRAUMATICA |
   ULCERA_VENOSA | LESAO_PRESSAO | PE_DIABETICO | OUTRA) + `tipo_ferida_outra`; `dimensoes` (texto);
   `comprimento`, `largura`, `profundidade` (numeric); `tunelizacao`, `descolamento` (bool);
   `tecido_leito` (enum T: GRANULACAO | ESFACELO | NECROSE | EPITELIZACAO);
   `infeccao_inflamacao` (enum I: AUSENTE | EXSUDATO_PURULENTO | ODOR | DOR);
   `exsudato` (enum M: AUSENTE | PEQUENO | MODERADO | INTENSO) + `exsudato_tipo` (texto);
   `bordas` (enum E: INTEGRAS | MACERADAS | DESCOLADAS | EPITELIZANDO);
   `pele_perilesional` (texto); `dor_escala` (0–10);
   `sinais_vitais`, `pa`, `fc`, `fr`, `temp` (texto).
3. Diagnósticos de enfermagem (bool): `diag_integridade_pele`, `diag_integridade_tissular`,
   `diag_risco_infeccao`, `diag_perfusao_ineficaz`, `diag_dor_aguda` + `diag_outros` (texto).
4. Conduta realizada: `limpeza` (enum: SF_09 | OUTRO) + `limpeza_outro`;
   `desbridamento` (enum: NAO | AUTOLITICO | INSTRUMENTAL | ENZIMATICO);
   `cobertura_primaria`, `cobertura_secundaria`, `fixacao`, `orientacoes_paciente` (texto).
5. Avaliação da evolução: `avaliacao_evolucao` (enum: MELHORA | ESTAVEL | PIORA);
   `reducao_area` (bool); `observacoes` (texto).
6. Plano (bool): `plano_manter_conduta`, `plano_ajustar_cobertura`, `plano_avaliacao_medica`,
   `plano_solicitar_exames`, `plano_encaminhamento`; `retorno_dias` (int).

### 3. `evolucoes_curativos` (nova — 1:1 com agendamento TRATAMENTO)

1. Avaliação diária: `comprimento`, `largura`, `profundidade` (numeric); `area_aproximada`
   (numeric, C×L calculado no front); `tecido` (enum T); `infeccao_inflamacao`
   (enum: AUSENTE | LOCAL | SISTEMICA — legenda 0/1/2); `exsudato` (enum M — 0/1/2/3);
   `bordas` (enum E); `odor_presente` (bool); `dor_escala` (0–10); `pele_perilesional` (texto).
2. Intervenções (observação texto por linha): `limpeza_irrigacao`; `desbridamento` (enum NAO |
   AUTOLITICO | INSTRUMENTAL | ENZIMATICO) + `desbridamento_obs`; `cobertura_primaria`;
   `orientacoes_paciente`.
3. Avaliação da evolução: `evolucao` (enum: MELHORA | ESTAVEL | PIORA); `observacoes`.
4. Plano/Ações futuras (texto de observação por ação — preenchido = ação marcada):
   `plano_manter_conduta`, `plano_alterar_cobertura`, `plano_solicitar_exames`,
   `plano_encaminhamento`, `retorno_previsto`.

Enums compartilhados entre as duas fichas: `TecidoLeito`, `BordasFerida`, `ExsudatoTime`,
`TipoDesbridamento`, `AvaliacaoEvolucao`.

**Removidos**: entidade/tabela `evolucoes_clinicas` + tabelas auxiliares
(`evolucao_caracteristicas_bordas`, `evolucao_caracteristicas_perilesional`,
`evolucao_sinais_infeccao`, `evolucao_sinais_evolucao`) e os enums antigos não reaproveitados
(`AvaliacaoPulsos`, `CaracteristicaBorda`, `CaracteristicaPerilesional`, `ClassificacaoDor`,
`EvolucaoFerida`, `ExsudatoCaracteristica`, `ExsudatoVolume`, `GrauEdema`, `OdorIntensidade`,
`SinalEvolucao`, `SinalInfeccao`).

---

## Passos incrementais

Cada passo deixa o projeto compilando, com testes verdes.

> **Status:** Passos 1–8 concluídos. Backend nos commits `2580034` / `a699ad7` /
> `d13fe1b`; frontend em `e3202f4`.

### Passo 1 — Backend: remodelar Anamnese — ✅ CONCLUÍDO
- Reescrever `V18__criacao_tabela_anamneses.sql` com os novos campos.
- Atualizar entidade `Anamnese`, `AnamneseRequestDTO`, `AnamneseResponseDTO`, mapeamento no
  `AnamneseServiceImpl` e criar enum `RedeApoio`.
- Atualizar testes de `AnamneseServiceImpl`.

### Passo 2 — Backend: criar Ficha de Evolução de Enfermagem (Avaliação) — ✅ CONCLUÍDO
- Nova migração de criação `evolucoes_enfermagem` (numerar após a última existente).
- Novos enums compartilhados + entidade `EvolucaoEnfermagem`, repository, DTOs.
- Endpoint no fluxo do agendamento (`registrarEvolucaoEnfermagem`) restrito a agendamentos
  do tipo `AVALIACAO`; expor a ficha no `AgendamentoDetalhadoDTO`.
- Testes de service.

### Passo 3 — Backend: criar Ficha de Evolução Diária – Curativos (Tratamento) — ✅ CONCLUÍDO
- Nova migração de criação `evolucoes_curativos`.
- Entidade `EvolucaoCurativo`, repository, DTOs; `registrarEvolucaoCurativo` restrito a
  `TRATAMENTO`; expor no `AgendamentoDetalhadoDTO`.
- Testes de service.

### Passo 4 — Backend: remover a EvolucaoClinica antiga — ✅ CONCLUÍDO
- Excluir `V14__adicionar_campos_evolucao_tratamento_agendamentos.sql` e
  `V15__separar_evolucao_clinica.sql` (banco do zero; V14 só criava colunas que a V15 dropava).
- Remover entidade `EvolucaoClinica`, `EvolucaoClinicaDTO`, `EvolucaoClinicaRepository`,
  `EvolucaoTratamentoRequestDTO`, enums antigos e o `registrarEvolucao` legado.
- `HistoricoServiceImpl` passa a montar os itens de histórico a partir das novas fichas
  (enfermagem e curativos); ajustar `messages.properties`.
- Atualizar/remover testes e fixtures que referenciam a estrutura antiga.

### Passo 5 — Frontend: tipos e serviços — ✅ CONCLUÍDO
- Reescrever tipos em `tipos/agendamento.ts` (novas fichas + enums) e criar `tipos/anamnese.ts`.
- Atualizar `lib/agendamento.service.ts` (novos endpoints) e criar `lib/anamnese.service.ts`.
- Adicionar mensagens em `lib/mensagens.ts`.

### Passo 6 — Frontend: formulário da Avaliação — ✅ CONCLUÍDO
- Na página `agenda/procedimento/[id]`: quando `tipo === 'AVALIACAO'`, exibir **Anamnese**
  (seções do modelo, Sim/Não + detalhe, checkboxes, rede de apoio) e **Ficha de Evolução de
  Enfermagem** (6 seções do modelo, data/hora pré-preenchidas do agendamento).
- Finalizar consulta grava anamnese + ficha de enfermagem.

### Passo 7 — Frontend: formulário do Tratamento — ✅ CONCLUÍDO
- Quando `tipo === 'TRATAMENTO'`, exibir a **Ficha de Evolução Diária – Curativos**
  (avaliação diária com legenda TIME, intervenções, evolução, plano/ações futuras);
  `area_aproximada` calculada automaticamente (C×L).
- Remover o formulário antigo (dados da ferida/leito/exsudato/percentuais etc.).

### Passo 8 — Verificação final — ✅ CONCLUÍDO (exceto E2E)
- `mvn test` no backend (JaCoCo), build do frontend, revisão do histórico do prontuário
  (`AbaHistorico`) e do fluxo completo: agendar Avaliação → preencher anamnese + ficha →
  concluir → agendar Tratamento → preencher ficha diária → concluir.

Resultado (24/07/2026):

| Verificação | Resultado |
|---|---|
| `mvnw test` (backend) | 253/253 verdes, 0 falhas/erros |
| `tsc --noEmit` | limpo |
| `next build` | sucesso (16 rotas) |
| `eslint` | limpo nos arquivos da fatia |
| Fluxo E2E em runtime | **pendente** — exige Docker + DB + gateway de pé |

---

## Decisões assumidas (validar)

1. **Anamnese continua 1:1 com o paciente** (preenchida/editada durante a Avaliação), como hoje —
   o modelo é um histórico do paciente, não da consulta.
2. Campos Sim/Não do modelo que têm caixa de texto logo abaixo viram **boolean + campo detalhe**.
3. No plano da ficha de curativos (tabela Ação × Observações), preencher a observação equivale a
   marcar a ação (campos texto, sem boolean separado).
4. Cabeçalho dos PDFs (dados do paciente e do profissional) vem do cadastro — não são campos do
   formulário.

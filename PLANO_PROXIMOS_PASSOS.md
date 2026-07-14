# Plano de Próximos Passos — ProntuDigital

> Plano dos requisitos pendentes do [`Requisitos.md`](./Requisitos.md).
> Status do produto: clínica de enfermagem em feridas/curativos. Perfis: **ADMIN, PROFISSIONAL, PACIENTE**.

## Princípio orientador

Cada fatia nova segue o **padrão já estabelecido pela Anamnese** (RF13):

```
entidade → migration Flyway → DTOs (request/response) → repository
→ service (interface + impl) → controller sob /api/prontuario/...
→ ProntuarioPermissaoPolicy (RN03) → testes → tela Next.js
```

Isso mantém consistência e reaproveita a política de acesso existente
(`ProntuarioPermissaoPolicy.podeVisualizar` / `podeEditar`).

---

## Fase 2 — Concluir o PEP (prioridade máxima)

> **Progresso:** RF16, RF15 e RF18 concluídos (backend + frontend). **Fase 2 / PEP completo.**
> Próximo: **Fase 3 — RF06**.

### RF16 — Prescrição de medicamentos e cuidados de enfermagem — ✅ CONCLUÍDO
Espelha a Anamnese, mas **1:N por paciente** (várias prescrições ao longo do tempo).

- ✅ **Backend:** entidade `Prescricao` (paciente_uuid, agendamento_uuid opcional, tipo
  [`MEDICAMENTO` | `CUIDADO`], descrição, posologia/frequência, duração, orientações,
  `registradoPor`, timestamps) + migration `V20`.
- ✅ DTOs, `PrescricaoRepository` (findByPacienteUuid ordenado por data), `PrescricaoService`/impl.
- ✅ `PrescricaoController` em `/api/prontuario/pacientes/{pacienteUuid}/prescricoes`
  (GET lista, POST, PUT, DELETE), reusando `podeEditar` / `podeVisualizar`.
- ✅ Testes de serviço e de permissão (RN03) — `PrescricaoServiceImplTest`.
- ✅ **Frontend:** aba "Prescrições" na tela de prontuário do paciente.

### RF15 — Anexação de exames e documentos — ✅ CONCLUÍDO
- ✅ **Arquitetura:** storage abstraído atrás de `ArmazenamentoService`
  (`compartilhado.armazenamento`) com implementação em **filesystem/volume Docker**
  (`ArmazenamentoFilesystemService`, com proteção contra path traversal),
  permitindo trocar por S3/MinIO depois sem mexer no domínio.
- ✅ **Backend:** entidade `Anexo` (paciente_uuid, agendamento_uuid opcional, nome
  original, mime type, tamanho, chave de armazenamento, `registradoPor`, timestamp) +
  migration `V21`. O binário não fica no banco.
- ✅ Endpoint `multipart/form-data` para upload, download/preview (stream, inline) e
  exclusão; validação de tipo (imagens JPG/PNG/WEBP + PDF) e tamanho (10 MB); nome de
  arquivo sanitizado; extensão derivada do MIME. Reusa `podeEditar`/`podeVisualizar`.
- ✅ Testes de serviço e permissão (RN03) — `AnexoServiceImplTest` (10 testes).
- ✅ **Frontend:** aba "Anexos" na tela de prontuário, com upload, grade de cards,
  preview de imagens (via blob autenticado) e abertura de PDFs.
- ✅ Config: `spring.servlet.multipart` (10 MB), `app.armazenamento.*` e volume
  `anexos_prontudigital_data` no docker-compose.

### RF18 — Consolidar histórico clínico — ✅ CONCLUÍDO
- ✅ Endpoint agregador (leitura) `GET /api/prontuario/pacientes/{pacienteUuid}/historico`
  que junta agendamentos, evoluções, prescrições e anexos em ordem cronológica. Evoluções
  derivadas de `agendamento.getEvolucaoClinica()`. Reusa `podeVisualizar` (RN03).
  `HistoricoServiceImplTest` (5 testes, 100%).
- ✅ **Frontend:** aba "Histórico" (padrão) — timeline única com marcadores/badges por tipo
  e filtro por tipo de evento.

---

## Fase 3 — Enriquecer cadastros

### RF06 — Serviços/procedimentos em tabela
- Migrar de `enum TipoProcedimento` para entidade `Procedimento` (nome, descrição,
  duração padrão, ativo) com CRUD ADMIN + migration de dados a partir do enum atual.
- Ajustar `Agendamento` para referenciar `procedimento_id` (manter compatibilidade
  durante a migração).

### RF04 / RF05 — Campos faltantes
- **RF04:** adicionar CPF (com validação), data de nascimento e endereço ao cadastro de paciente.
- **RF05:** adicionar COREN, especialidade e horários de trabalho ao profissional — os
  horários alimentam a validação de disponibilidade da agenda.
- Migrations incrementais + ajustes de formulário no frontend.

---

## Fase 4 — Relatórios e documentos

- **RF19:** relatório de atendimentos por período/profissional (query + endpoint filtrado).
- **RF20:** relatório de ocupação (taxa de comparecimento × cancelamentos), reaproveitando
  `HistoricoAgendamento` e status.
- **RF17 / RF21:** emissão de atestados e **exportação PDF/Excel** — adicionar biblioteca de
  geração (ex.: OpenPDF/JasperReports para PDF, Apache POI para Excel). RF17 e RF21
  compartilham a mesma infra de geração.

---

## Fase 5 — Requisitos não funcionais e regras residuais

- **RNF03 (auditoria):** generalizar o padrão de `HistoricoAgendamento` para um log de
  acesso/alteração transversal (ex.: `@EntityListeners` ou aspecto).
- **RNF01 (LGPD):** criptografia em repouso de campos sensíveis (CPF, dados clínicos) —
  atributo converter JPA ou criptografia de coluna.
- **RNF02 (backup):** rotina de `pg_dump` agendada no Docker/infra.
- **RN01:** job agendado para arquivar pacientes inativos há +2 anos (reuso do padrão de
  scheduler do módulo `notificacao`).
- **RNF08:** definir e expor API pública documentada (Swagger já presente) para terceiros —
  escopo a definir com o cliente.

---

## Sequência recomendada

~~RF16~~ ~~RF15~~ (✅ concluídos) **→ RF18** (fecha o PEP, maior valor clínico) **→ RF06 → RF04/RF05
→ RF19/RF20 → RF17/RF21 → NFRs**

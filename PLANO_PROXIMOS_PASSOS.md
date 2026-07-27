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
> Próximo: **Fase 3 — RF04/RF05** (RF06 concluído).

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

> **Progresso:** RF06 e RF04/RF05 concluídos (backend + frontend). **Fase 3 completa.**
> Próximo: **Fase 4 — RF19/RF20** (relatórios).

### RF06 — Serviços/procedimentos em tabela — ✅ CONCLUÍDO
- ✅ **Backend:** entidade `Procedimento` (codigo legado do enum, nome, descrição,
  duração padrão em minutos, ativo) + migration `V22` com seed a partir do enum
  (`PODIATRIA`, `TRATAMENTO_FERIDAS`).
- ✅ CRUD em `/api/procedimentos` (GET público/autenticado; POST/PUT/DELETE
  restritos a ADMIN — `@PreAuthorize` + checagem no service). DELETE físico só
  quando sem vínculos; procedimento em uso é desativado via PUT (`ativo=false`).
- ✅ `Agendamento` referencia `procedimento_id` (FK, backfill na migration); a
  coluna `tipo_procedimento` foi mantida (agora opcional) para compatibilidade.
  `AgendamentoRequestDTO` aceita `procedimentoId` (preferido) ou o enum legado;
  DTOs de resposta expõem `procedimentoId`/`procedimentoNome`.
- ✅ Testes: `ProcedimentoServiceImplTest` (CRUD, permissão ADMIN, nome duplicado,
  em uso) + novos casos em `AgendamentoServiceImplTest` (procedimento inativo/
  inexistente/ausente).
- ✅ **Frontend:** tela ADMIN `/dashboard/procedimentos` (CRUD + ativar/desativar);
  formulários de agendamento (modal e página pública `/agendar`) passaram a listar
  procedimentos da API e enviar `procedimentoId`; exibição usa `procedimentoNome`
  com fallback ao rótulo do enum legado (`nomeProcedimento()`).

### RF04 / RF05 — Campos faltantes — ✅ CONCLUÍDO
- ✅ **RF04 (backend):** `cpf` (único, guardado só com dígitos, validado por
  `@CPF` do hibernate-validator), `data_nascimento` (`@Past`) e endereço
  (`@Embeddable Endereco`: cep/logradouro/número/complemento/bairro/cidade/uf)
  em `usuarios` — migration `V25`, todas as colunas opcionais para não invalidar
  o cadastro rápido de paciente (só nome + telefone). `CpfExistenteException` (409).
- ✅ **RF05 (backend):** `coren` (único) e `especialidade` em `usuarios` (mesma
  `V25`); `CorenExistenteException` (409). Nova tabela `horarios_trabalho`
  (migration `V26`) espelhando `bloqueios_recorrentes`, com entidade, repository,
  DTO, service e `HorarioTrabalhoController` em `/api/horarios-trabalho`
  (POST/GET/DELETE + `GET /public`).
- ✅ **Validação da agenda:** `AgendamentoServiceImpl.validarHorarioTrabalho`
  roda em `agendar` e `reagendar` — o atendimento precisa caber **inteiro** em
  uma janela do dia da semana (`ForaDoHorarioTrabalhoException`, 422).
  Profissional sem janelas cadastradas não é validado, preservando os cadastros
  anteriores ao RF05.
- ✅ **Divisão de escrita:** `PATCH /{id}/perfil` (autoatendimento) grava só os
  dados pessoais do RF04; COREN e especialidade são credenciais profissionais,
  alteradas pelo ADMIN via `PUT /api/usuarios/{id}`.
- ✅ **Frontend:** seções "Dados pessoais" e "Dados profissionais" (esta só quando
  o perfil PROFISSIONAL está marcado) no `FormularioUsuarioModal`; campos de CPF,
  nascimento e endereço na tela `/perfil`; componente `HorariosTrabalho` na página
  `/bloqueios`, que passou a ser a tela de disponibilidade completa (expediente +
  bloqueios). Máscaras `mascaraCPF`/`mascaraCEP` em `lib/mascaras.ts`.
- ✅ Testes: `HorarioTrabalhoServiceImplTest` (12), novos casos de horário de
  trabalho em `AgendamentoServiceImplTest` (5) e de CPF/endereço/COREN em
  `UsuarioServiceImplTest` (6). Suíte em **276 testes, 0 falhas**.

> **Observação para o RF04:** o requisito também citava "histórico médico" como
> pendente — isso já foi entregue na Fase 2 pelo prontuário (anamnese, evoluções,
> prescrições, anexos e histórico consolidado).

---

## Fase 4 — Relatórios e documentos

> **Progresso:** RF19, RF20, RF17 e RF21 concluídos (backend + frontend).
> **Fase 4 completa.** Próximo: **Fase 5 — NFRs e regras residuais.**

### RF19 / RF20 — Relatórios gerenciais — ✅ CONCLUÍDO
- ✅ **Backend:** novo módulo `com.prontudigital.backend.relatorio` (DTOs, service,
  `RelatorioController` em `/api/relatorios`), sem entidade nem migration — os dois
  relatórios são leitura agregada sobre `agendamentos`.
- ✅ `AgendamentoRepository.buscarParaRelatorio` — período obrigatório + profissional,
  status e tipo opcionais, seguindo o padrão `(:param IS NULL OR ...)` já usado no repo.
- ✅ **RF19** `GET /atendimentos`: linhas do período (com duração calculada, nomes
  resolvidos e procedimento) + totais por status e por tipo. Os nomes são resolvidos
  com cache por UUID no service, senão seria uma consulta por linha.
- ✅ **RF20** `GET /ocupacao`: realizados/cancelados/faltas/remarcados/em aberto,
  taxas de comparecimento, cancelamento e absenteísmo, horas ocupadas e taxa de
  ocupação. **Comparecimento e absenteísmo são calculados sobre os atendimentos que
  chegaram a acontecer** (realizados + faltas), não sobre o total — cancelamento
  prévio não é falta. Taxas vêm `null` sem base de comparação, nunca zero.
- ✅ A capacidade da agenda reaproveita os **horários de trabalho do RF05**: a taxa de
  ocupação só existe quando há profissional filtrado com expediente cadastrado.
- ✅ **Acesso:** ADMIN vê a clínica inteira ou qualquer profissional; PROFISSIONAL tem o
  filtro forçado para a própria agenda e recebe 403 ao pedir outra (em vez de trocar o
  recorte em silêncio); PACIENTE não acessa.
- ✅ **Frontend:** página `/relatorios` (ADMIN + PROFISSIONAL) com filtros de período,
  profissional (só ADMIN), status e tipo; cards de indicadores, resumo numérico e
  tabela de atendimentos. O item "Relatórios" da barra lateral apontava para `/reports`
  (rota inexistente) e passou a apontar para cá.
- ✅ `RelatorioServiceImplTest` (15 testes). Suíte em **291 testes, 0 falhas**.

### RF17 / RF21 — Atestados e exportação — ✅ CONCLUÍDO
- ✅ **Infra compartilhada** em `compartilhado.documento`: `PdfBuilder` (envolve o
  **OpenPDF** — fork LGPL/MPL do iText 4, sem a AGPL do iText moderno) e
  `PlanilhaBuilder` (**Apache POI**), mais `FormatoExportacao` e `ArquivoGerado`.
  Os arquivos ficam em memória: são pequenos e gerados sob demanda, sem nada a
  persistir — diferente dos anexos, que seguem pelo `ArmazenamentoService`.
- ✅ **RF21:** `GET /api/relatorios/{atendimentos,ocupacao}/exportar?formato=PDF|XLSX`.
  A exportação delega a apuração ao `RelatorioService`, então regras de acesso e
  fórmulas das taxas continuam num lugar só — aqui é apenas formatação. O XLSX de
  atendimentos sai com duas abas (dados + resumo). Taxa nula vira "—", não 0%.
- ✅ **RF17:** entidade `Atestado` (migration `V27`) no módulo `prontuario`, com
  `AtestadoController` em `/api/prontuario/pacientes/{uuid}/atestados`. **O PDF não é
  persistido** — é regerado a partir do registro a cada download, então o registro é a
  fonte da verdade. `AFASTAMENTO` exige `diasAfastamento` e `COMPARECIMENTO` o recusa
  (422): aceitar dias num atestado de comparecimento produziria um documento que afirma
  algo que o tipo não sustenta. Reusa a `ProntuarioPermissaoPolicy` (RN03) — o paciente
  lê os próprios atestados mas nunca emite.
- ✅ **Frontend:** botões de exportação (4 combinações) na página `/relatorios`, baixando
  via axios para que o token seja enviado; aba "Atestados" no prontuário, com emissão,
  abertura do PDF e remoção.
- ✅ Testes: `AtestadoServiceImplTest` (15) e `RelatorioExportacaoServiceImplTest` (8, que
  abrem o XLSX gerado com o POI e conferem o conteúdo das células). Suíte em
  **314 testes, 0 falhas**.

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

~~RF16~~ ~~RF15~~ ~~RF18~~ ~~RF06~~ ~~RF04/RF05~~ ~~RF19/RF20~~ ~~RF17/RF21~~
(✅ concluídos — **todos os RFs pendentes do `Requisitos.md` foram entregues**)
**→ Fase 5: NFRs (RNF01 LGPD, RNF02 backup, RNF03 auditoria, RNF08 API) + RN01**

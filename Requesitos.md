# Módulo de Agenda para Procedimentos de Enfermagem

## RF01 – Agendamento de consultas/procedimentos (avaliação ou tratamento)

**Objetivo:** Registrar um compromisso com dados completos de paciente, profissional, data/hora, tipo e observações.

### Campos obrigatórios (baseados em `Agendamento`)

- `pacienteUuid` – identificador do paciente (vindo do módulo de pacientes)
- `profissionalUuid` – identificador do profissional de enfermagem
- `inicioEm` e `fimEm` – data e hora de início e fim do procedimento
- `tipo` – `AVALIACAO` ou `TRATAMENTO`
- `status` – inicialmente `AGENDADO`

### Regras de negócio

- Não pode haver sobreposição de horários para o mesmo `profissionalUuid`, a menos que o agendamento esteja com status `CANCELADO` ou `REMARCADO` (bloqueio físico deve ser validado via `BloqueioHorario` também).
- Para agendamentos do tipo `TRATAMENTO`, é obrigatório informar o campo `avaliacao` (relacionamento com um agendamento do tipo `AVALIACAO` do mesmo paciente). O sistema deve validar que a avaliação existe, pertence ao mesmo paciente e está com status `REALIZADO`.
- A duração mínima é de 15 minutos; duração máxima definida por tipo de procedimento (configurável).
- Geração de `uuid` automático (via trigger ou lógica de aplicação).
- Ao criar um agendamento, verificar se o paciente não possui outro agendamento ativo no mesmo horário (evitar duplicidade).

### Fluxo para tratamento composto

1. Paciente agenda uma **Avaliação** (`tipo = AVALIACAO`, campo `avaliacao = null`).
2. Após a avaliação ser realizada (`status = REALIZADO`), o profissional ou secretária pode agendar **Tratamentos** subsequentes, cada um apontando para aquela avaliação via `avaliacao`.
3. O sistema pode sugerir horários com base na periodicidade típica do tratamento (ex: 2x por semana) – funcionalidade extra.

---

## RF02 – Visualização da agenda por dia/semana/mês

**Objetivo:** Exibir os compromissos e bloqueios de forma clara para profissionais e administradores.

### Funcionalidades

- Filtros por: profissional (obrigatório), período (dia, semana, mês), tipo de agendamento, status.
- Visualização deve considerar tanto `Agendamento` (com status `AGENDADO`, `CONFIRMADO`, `REALIZADO` e `REMARCADO`) quanto `BloqueioHorario`.
- Cada evento mostra: horário, paciente (nome), tipo (avaliação/tratamento), status (ícone colorido).
- Destaque para avaliações que já geraram tratamentos ativos.
- Opção de exportar (PDF/CSV).

### Campos utilizados

- `Agendamento.inicioEm`, `fimEm`, `profissionalUuid`, `tipo`, `status`
- `BloqueioHorario.inicioEm`, `fimEm`, `profissionalUuid`, `motivo`, `tipoBloqueio`

### Performance

- Índices em `(profissionalUuid, inicioEm)` e `(profissionalUuid, fimEm)`.

---

## RF03 – Confirmação de consultas via SMS/e-mail

**Objetivo:** Enviar lembretes e solicitar confirmação para reduzir faltas.

### Disparos automáticos

- **Lembrete preliminar:** 48h antes do horário de início.
- **Confirmação obrigatória:** 24h antes. O paciente deve responder ou clicar em link.
    - Se confirmado, `Agendamento.status = CONFIRMADO`.
    - Se recusado ou sem resposta até 2h antes, o horário é liberado e o paciente é movido para a **fila de espera** (RF12) como prioridade alta.

### Regras de negócio

- Envio apenas para agendamentos com status `AGENDADO` ou `REMARCADO`.
- Registro de tentativas de envio (log em tabela auxiliar).
- Configuração por profissional/unidade: habilitar/desabilitar notificações.

### Integração com entidades

- Dados de contato (e-mail, telefone) devem vir do módulo de pacientes/usuários, referenciados pelo `pacienteUuid`.

---

## RF4 – Cancelamento e reagendamento de consultas

**Objetivo:** Permitir alteração ou remoção de agendamentos com rastreabilidade.

### Cancelamento

Apenas permitido com **motivo** (campo `observacoes` pode ser usado, mas idealmente criar uma coluna `motivoCancelamento` ou usar histórico).

**Regras:**

- Cancelamento com mais de 24h de antecedência: sem penalidade.
- Cancelamento com menos de 24h: marca falta (`status = NAO_COMPARECEU`) e notifica profissional.
- Ao cancelar, o sistema verifica se existe paciente na fila de espera para aquele horário/profissional e oferece remarcação imediata.

### Reagendamento

- Gera um novo agendamento com os dados atualizados e mantém o original com status `REMARCADO`.
- **Registro de alteração obrigatório** via `HistoricoAgendamento`:
    - Preencher `agendamentoId` (id do registro original), `inicioAnterior`, `fimAnterior`, `inicioNovo`, `fimNovo`, `motivo`, `alteradoPor` (UUID do usuário que alterou).
- O novo agendamento recebe um novo `uuid` e mantém o vínculo com a mesma avaliação (se for tratamento).
- Se o reagendamento envolver mudança de profissional, validar disponibilidade.

---

## RF05 – Bloqueio de horários indisponíveis

**Objetivo:** Impedir agendamentos em períodos já ocupados por folgas, feriados, treinamentos, etc.

### Entidade `BloqueioHorario`

- `profissionalUuid` – pode ser `null` para bloqueio global (toda a unidade).
- `inicioEm` e `fimEm` – período bloqueado.
- `tipo` – categorização do bloqueio.
- `motivo` – descrição livre (ex: "Manutenção da sala").

### Regras

- Bloqueios recorrentes (ex: toda quarta-feira das 14h às 15h) devem ser tratados por lógica de recorrência ou criação de múltiplos registros.
- Validação de criação/edição de `Agendamento`:
    - Não pode haver sobreposição com `BloqueioHorario` para o mesmo `profissionalUuid`.
    - Se `profissionalUuid` for `null`, o bloqueio vale para todos os profissionais.
- Bloqueios podem ser criados manualmente ou via importação de feriados regionais.
- Não é permitido excluir bloqueio que já está em vigor (apenas desativar logicamente ou com permissão de admin).
- Visualização: Mescla com a agenda (RF08) mostrando os períodos como "indisponível".

---

## RF06 – Lista de espera para remarcação

**Objetivo:** Gerenciar pacientes interessados em horários remanescentes ou remarcações de cancelamentos de última hora.

### Entidade `FilaEspera`

- `pacienteUuid`, `profissionalUuid` (pode ser `null` se o paciente aceitar qualquer profissional)
- `tipoPreferido` – `AVALIACAO` ou `TRATAMENTO`
- `dataPreferida` – data/hora desejada (opcional, pode ser `null` para "assim que possível")
- `prioridade` – inteiro: menor número = maior prioridade (calculado por data de entrada + tempo de espera + se é remarcação de cancelamento)
- `status` – `AGUARDANDO`, `NOTIFICADO` (quando encontrado um horário), `REMARCADO`, `EXPIRADO` (após 7 dias sem resposta), `CANCELADO_PELO_PACIENTE`

### Fluxo

1. Quando um agendamento é **cancelado** (com menos de 24h) ou **não confirmado** (RF09), o sistema consulta a fila de espera pelo mesmo `profissionalUuid` e `tipoPreferido` (ou tipo compatível).
2. Seleciona o paciente com maior prioridade e que tenha disponibilidade confirmada no horário vago.
3. Notifica esse paciente (SMS/e-mail) com oferta do horário. O paciente tem **2h** para aceitar.
    - Se aceitar: cria-se um novo agendamento, o registro da fila vai para `REMARCADO`.
    - Se recusar ou expirar: passa para o próximo da fila.
4. Paciente pode se inscrever manualmente na fila, escolhendo profissional, tipo de atendimento e período preferido.
5. A cada novo agendamento criado (agendamento normal), o sistema também verifica se algum paciente da fila poderia ser atendido naquele mesmo horário (se a vaga foi criada por outro cancelamento).

### Prioridade calculada

`prioridade = (dias_na_fila * 2) + (peso_tipo_agendamento) + (peso_remarcacao_urgente)`

Pacientes que tiveram seu horário cancelado pelo profissional ganham +50 no score.

---

## Considerações Transversais

- **Consistência de dados:** Uso de `uuid` público para APIs e `id` sequencial para relacionamentos internos (como `avaliacao_id` e `agendamentoId` no histórico).
- **Auditoria:** `createdAt` e `updatedAt` em `Agendamento`; `alterado_em` e `alterado_por` em `HistoricoAgendamento`.
- **Segurança:** Apenas usuários com papel `ADMIN`, `SECRETARIA` ou `ENFERMEIRO` podem modificar agendamentos. Pacientes podem cancelar seus próprios agendamentos via endpoint restrito.
- **Performance:** Para visualização mensal, recomenda-se consultas paginadas ou uso de materialized views para sumarização.
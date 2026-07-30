# **Requisitos do Sistema**

## **1. Introdução**
O sistema gerencia as operações de uma **clínica de enfermagem especializada em feridas e curativos**, incluindo **agendamento de consultas/procedimentos** e **prontuário eletrônico do paciente (PEP)**. O objetivo é melhorar a eficiência no atendimento, garantir a segurança dos dados e facilitar o acesso às informações clínicas.

**Perfis de acesso:** Administrador, Profissional (enfermagem) e Paciente.

> **Legenda de status:** ✅ Implementado · 🟡 Parcial · ❌ Pendente

---

## **2. Requisitos Funcionais**

### **2.1 Módulo de Autenticação e Perfis de Acesso**
- **RF01:** ✅ Login com autenticação por e-mail e senha (JWT + refresh token).
- **RF02:** ✅ Diferentes níveis de acesso (Administrador, Profissional, Paciente).
- **RF03:** ✅ Recuperação de senha via código.

### **2.2 Módulo de Cadastro**
- **RF04:** ✅ Cadastro de pacientes (nome, telefone, e-mail, CPF, data de nascimento, endereço). Histórico médico coberto pelo prontuário (RF13/RF15/RF16/RF18).
- **RF05:** ✅ Cadastro de profissionais de enfermagem (nome, perfil, COREN, especialidade e horários de trabalho — estes validam a disponibilidade da agenda).
- **RF06:** ✅ Serviços/procedimentos oferecidos pela clínica — tabela `procedimentos` com CRUD do ADMIN em `/dashboard/procedimentos`. O enum `TipoProcedimento` sobrevive apenas como `codigo` dos registros migrados.

### **2.3 Módulo de Agenda**
- **RF07:** ✅ Agendamento de consultas/procedimentos (data, horário, profissional, paciente, tipo de atendimento).
- **RF08:** ✅ Visualização da agenda por dia/semana/mês.
- **RF09:** ✅ Confirmação de consultas via **WhatsApp** (lembrete + solicitação de confirmação por token).
- **RF10:** ✅ Cancelamento e reagendamento de consultas.
- **RF11:** ✅ Bloqueio de horários indisponíveis (pontuais e recorrentes).
- **RF12:** ✅ Lista de espera para remarcação.

### **2.4 Módulo de Prontuário Eletrônico (PEP)**
- **RF13:** ✅ Registro de anamnese (histórico do paciente, queixas, alergias, medicamentos em uso).
- **RF14:** ✅ Registro de evolução clínica do paciente (avaliação de ferida: mensuração, leito, exsudato, bordas, pele perilesional, sinais de infecção, dor, avaliação vascular e conduta).
- **RF15:** ✅ Anexação de exames e documentos (JPG, PNG, WEBP ou PDF, até 10 MB). Os binários ficam em volume via `ArmazenamentoService`; o banco guarda apenas a chave.
- **RF16:** ✅ Prescrição de medicamentos e cuidados de enfermagem — aba própria no prontuário, com CRUD.
- **RF17:** ✅ Emissão de relatórios e atestados (comparecimento e afastamento, com PDF gerado sob demanda).
- **RF18:** ✅ Histórico de atendimentos consolidado — `/api/prontuario/pacientes/{uuid}/historico` reúne a linha do tempo clínica do paciente, exibida na aba Histórico.

### **2.5 Módulo de Relatórios**
- **RF19:** ✅ Relatório de atendimentos por período/profissional.
- **RF20:** ✅ Relatório de ocupação da clínica (taxa de comparecimento e cancelamentos), com taxa de ocupação da agenda a partir dos horários de trabalho (RF05).
- **RF21:** ✅ Exportação de dados em PDF/Excel (relatórios de atendimentos e de ocupação).

### **2.6 Módulo de Configuração da Clínica**
- **RF22:** ✅ Configuração da identidade da clínica pelo ADMIN (nome, CNPJ, endereço, contato, logo e rodapé dos documentos), aplicada ao cabeçalho dos atestados e relatórios em PDF e à tela de login. Editável em `/dashboard/configuracoes`, sem recompilação.

---

---

## **3. Requisitos Não Funcionais**
### **3.1 Segurança**
- **RNF01:** 🟡 Criptografia de dados sensíveis. *Feito:* senhas com BCrypt; TLS automático com HSTS na borda (Caddy); Postgres sem porta publicada em produção. *Pendente:* cifra em repouso (disco/volume) e cifra do backup antes do envio externo.
- **RNF02:** ✅ Backup automático diário — `docker/prod/scripts/backup.sh` exporta banco **e** anexos (nesta ordem, para o pior caso ser arquivo órfão e não download quebrado), com retenção e verificação. Instalação no cron às 3h documentada em `scripts/README.md`; `restore.sh` valida (`--teste`) ou restaura (`--real`).
- **RNF03:** 🟡 Registro de logs de auditoria. *Feito:* `historico_agendamentos` (mudanças de status) e `log_notificacoes_whatsapp` (envios). *Pendente:* trilha de **acesso a prontuário** — quem leu o quê e quando, que é o que a LGPD cobra para dado sensível.

### **3.2 Usabilidade**
- **RNF04:** ✅ Interface intuitiva e responsiva (desktop e mobile) — Next.js + Tailwind.
- **RNF05:** ⚪ Tempo de carregamento inferior a 3 segundos (não medido).

### **3.3 Desempenho**
- **RNF06:** ⚪ Suporte a pelo menos 100 usuários simultâneos (não testado).
- **RNF07:** 🟡 Banco otimizado para consultas rápidas — índices declarados nas migrações de todas as tabelas de consulta frequente. *Pendente:* medição sob carga real.

### **3.4 Integrações**
- **RNF08:** ❌ API para integração com sistemas de terceiros (ex.: laboratórios, planos de saúde). A API REST é documentada em OpenAPI/Swagger, mas não há contrato, autenticação por aplicação nem versionamento voltados a parceiros.
- **RNF09:** ✅ Envio automatizado de lembretes via **WhatsApp** (scheduler).

---

## **4. Regras de Negócio**
- **RN01:** ❌ Pacientes inativos há mais de 2 anos devem ser arquivados. Sem implementação — exige definir o que é "arquivar" para dado de saúde, dado que o CFM exige guarda mínima do prontuário.
- **RN02:** ✅ Consultas só podem ser canceladas com até 24h de antecedência (Administrador ignora a regra).
- **RN03:** ✅ Profissionais só podem visualizar prontuários de seus próprios pacientes.
- **RN04:** ⚪ Não se aplica — o perfil Recepcionista não existe no produto atual.

---

## **5. Tecnologias**
- **Backend:** Java (Spring Boot).
- **Frontend:** Next.js (React) + Tailwind CSS.
- **Banco de Dados:** PostgreSQL (migrations Flyway).
- **Notificações:** WhatsApp Cloud API (Meta).
- **Hospedagem:** VPS única em São Paulo, executando `docker/prod/docker-compose.prod.yml` — ver o comparativo de alternativas e o custo estimado em [`ANALISE_DEPLOY.md`](ANALISE_DEPLOY.md).

---

---

## **6. Modelo de Distribuição**

O produto é entregue em **modelo silo**: uma instalação por cliente, com banco e
volumes próprios. Não há discriminador de *tenant* — a tabela
`configuracao_clinica` guarda uma única linha, garantida por `CHECK (id = 1)`.

A escolha foi deliberada para o estágio atual (poucos clientes, dado de saúde):
isolamento absoluto entre clínicas, sem risco de vazamento cruzado, ao custo de
operação linear. O comparativo com schema-por-cliente e *pool* com RLS, e o
gatilho para migrar, estão em [`ANALISE_DEPLOY.md`](ANALISE_DEPLOY.md).

---

## **7. Panorama de Status**

| Categoria | ✅ | 🟡 | ❌ | ⚪ |
|---|---|---|---|---|
| Requisitos Funcionais (22) | 22 | 0 | 0 | 0 |
| Requisitos Não Funcionais (9) | 3 | 3 | 1 | 2 |
| Regras de Negócio (4) | 2 | 0 | 1 | 1 |

**Todos os requisitos funcionais estão implementados.** As pendências restantes
são não funcionais e concentram-se em três frentes:

1. **RNF01 / RNF03 — LGPD.** Cifra em repouso e trilha de acesso ao prontuário
   são o que falta para uma postura defensável com dado sensível.
2. **RNF05 / RNF06 — desempenho.** Nunca medidos; nenhum teste de carga foi feito.
3. **RNF08 — integrações.** Sem demanda concreta até aqui.

> ⚪ = não avaliado. Este panorama reflete a verificação feita diretamente no
> código; ao alterar um status, confirme no repositório em vez de estimar.

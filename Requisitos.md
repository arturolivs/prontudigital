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
- **RF06:** 🟡 Serviços/procedimentos oferecidos pela clínica (hoje via enum `TipoProcedimento`). *Pendente: cadastro dinâmico em tabela.*

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
- **RF15:** ❌ Anexação de exames e documentos (imagens, laudos, receitas).
- **RF16:** ❌ Prescrição de medicamentos e cuidados de enfermagem.
- **RF17:** ✅ Emissão de relatórios e atestados (comparecimento e afastamento, com PDF gerado sob demanda).
- **RF18:** 🟡 Histórico de atendimentos (hoje: histórico de mudanças de status do agendamento). *Pendente: histórico clínico completo consolidado.*

### **2.5 Módulo de Relatórios**
- **RF19:** ✅ Relatório de atendimentos por período/profissional.
- **RF20:** ✅ Relatório de ocupação da clínica (taxa de comparecimento e cancelamentos), com taxa de ocupação da agenda a partir dos horários de trabalho (RF05).
- **RF21:** ✅ Exportação de dados em PDF/Excel (relatórios de atendimentos e de ocupação).

---

## **3. Requisitos Não Funcionais**
### **3.1 Segurança**
- **RNF01:** ❌ Criptografia de dados sensíveis (LGPD compliance).
- **RNF02:** ❌ Backup automático diário.
- **RNF03:** 🟡 Registro de logs de acesso (auditoria) — hoje apenas histórico de agendamentos.

### **3.2 Usabilidade**
- **RNF04:** ✅ Interface intuitiva e responsiva (desktop e mobile) — Next.js + Tailwind.
- **RNF05:** ⚪ Tempo de carregamento inferior a 3 segundos (não medido).

### **3.3 Desempenho**
- **RNF06:** ⚪ Suporte a pelo menos 100 usuários simultâneos (não testado).
- **RNF07:** 🟡 Banco de dados otimizado para consultas rápidas.

### **3.4 Integrações**
- **RNF08:** ❌ API para integração com sistemas de terceiros (ex.: laboratórios, planos de saúde).
- **RNF09:** ✅ Envio automatizado de lembretes via **WhatsApp** (scheduler).

---

## **4. Regras de Negócio**
- **RN01:** ❌ Pacientes inativos há mais de 2 anos devem ser arquivados.
- **RN02:** ✅ Consultas só podem ser canceladas com até 24h de antecedência (Administrador ignora a regra).
- **RN03:** ✅ Profissionais só podem visualizar prontuários de seus próprios pacientes.
- **RN04:** ⚪ Não se aplica — o perfil Recepcionista não existe no produto atual.

---

## **5. Tecnologias**
- **Backend:** Java (Spring Boot).
- **Frontend:** Next.js (React) + Tailwind CSS.
- **Banco de Dados:** PostgreSQL (migrations Flyway).
- **Notificações:** WhatsApp Cloud API (Meta).
- **Hospedagem:** ---.

---

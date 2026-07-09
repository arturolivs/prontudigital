# **Requisitos do Sistema**  

## **1. Introdução**  
O sistema será desenvolvido para gerenciar as operações de uma clínica de enfermagem, incluindo **agendamento de consultas** e **prontuário eletrônico do paciente (PEP)**. O objetivo é melhorar a eficiência no atendimento, garantir a segurança dos dados e facilitar o acesso às informações clínicas.  

---

## **2. Requisitos Funcionais**  

### **2.1 Módulo de Autenticação e Perfis de Acesso**  
- **RF01:** Login com autenticação por e-mail e senha.  
- **RF02:** Diferentes níveis de acesso (Administrador, Enfermeiro).  
- **RF03:** Recuperação de senha via e-mail.  

### **2.2 Módulo de Cadastro**  
- **RF04:** Cadastro de pacientes (nome, CPF, data de nascimento, telefone, e-mail, endereço, histórico médico).  
- **RF05:** Cadastro de profissionais de enfermagem (nome, registro no COREN, especialidade, horários de trabalho).  
- **RF06:** Cadastro de serviços/procedimentos oferecidos pela clínica.  

### **2.3 Módulo de Agenda**  
- **RF07:** Agendamento de consultas/procedimentos (data, horário, profissional, paciente, tipo de atendimento).  
- **RF08:** Visualização da agenda por dia/semana/mês.  
- **RF09:** Confirmação de consultas via SMS/e-mail.  
- **RF10:** Cancelamento e reagendamento de consultas.  
- **RF11:** Bloqueio de horários indisponíveis.  
- **RF12:** Lista de espera para remarcação.  

### **2.4 Módulo de Prontuário Eletrônico (PEP)**  
- **RF13:** Registro de anamnese (histórico do paciente, queixas, alergias, medicamentos em uso).  
- **RF14:** Registro de evolução do paciente (notas de atendimento, procedimentos realizados).  
- **RF15:** Anexação de exames e documentos (imagens, laudos, receitas).  
- **RF16:** Prescrição de medicamentos e cuidados de enfermagem.  
- **RF17:** Emissão de relatórios e atestados.  
- **RF18:** Histórico completo de atendimentos.  

### **2.5 Módulo de Relatórios**  
- **RF19:** Relatório de atendimentos por período/profissional.  
- **RF20:** Relatório de ocupação da clínica (taxa de comparecimento e cancelamentos).  
- **RF21:** Exportação de dados em PDF/Excel.  

---

## **3. Requisitos Não Funcionais**  
### **3.1 Segurança**  
- **RNF01:** Criptografia de dados sensíveis (LGPD compliance).  
- **RNF02:** Backup automático diário.  
- **RNF03:** Registro de logs de acesso (auditoria).  

### **3.2 Usabilidade**  
- **RNF04:** Interface intuitiva e responsiva (funcionar em desktop e mobile).  
- **RNF05:** Tempo de carregamento inferior a 3 segundos.  

### **3.3 Desempenho**  
- **RNF06:** Suporte a pelo menos 100 usuários simultâneos.  
- **RNF07:** Banco de dados otimizado para consultas rápidas.  

### **3.4 Integrações**  
- **RNF08:** API para integração com sistemas de terceiros (ex.: laboratórios, planos de saúde).  
- **RNF09:** Envio automatizado de lembretes (SMS/e-mail).  

---

## **4. Regras de Negócio**  
- **RN01:** Pacientes inativos há mais de 2 anos devem ser arquivados.  
- **RN02:** Consultas só podem ser canceladas com até 24h de antecedência.  
- **RN03:** Profissionais só podem visualizar prontuários de seus próprios pacientes.  
- **RN04:** Recepcionistas não podem alterar dados clínicos, apenas agendamentos.  

---

## **5. Tecnologias**  
- **Backend:** Java (Spring Boot).  
- **Frontend:** React.  
- **Banco de Dados:** PostgreSQL.  
- **Hospedagem:** ---.  

---
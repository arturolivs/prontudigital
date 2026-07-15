/**
 * Fonte única das mensagens de feedback do frontend.
 *
 * Regra de ownership:
 * - ERROS de chamadas à API vêm do backend (messages.properties). Use
 *   `mensagemErro(err, fallback)` para exibir a mensagem do backend e recorrer
 *   a um texto local apenas quando o backend estiver inacessível (erro de rede).
 * - SUCESSOS e VALIDAÇÕES client-side ficam centralizados aqui (o backend não
 *   os expõe hoje), evitando literais espalhados pelas páginas.
 */
export const MENSAGENS = {
  erro: {
    generico: 'Ocorreu um erro. Tente novamente.',
    carregarAgendamentos: 'Erro ao carregar agendamentos',
    criarAgendamento: 'Erro ao criar agendamento.',
    confirmarAgendamento: 'Não foi possível confirmar o agendamento.',
    criarCadastro: 'Erro ao criar cadastro. Verifique os dados.',
    carregarProfissionais: 'Não foi possível carregar os profissionais.',
    carregarAgendamento: 'Não foi possível carregar o agendamento.',
    carregarPacientes: 'Erro ao carregar pacientes',
    carregarUsuarios: 'Erro ao carregar lista de usuários',
    carregarPerfil: 'Erro ao carregar dados do perfil',
    salvarPerfil: 'Erro ao salvar perfil',
    alterarSenha: 'Erro ao alterar senha',
    salvarUsuario: 'Erro ao salvar usuário',
    excluirUsuario: 'Erro ao excluir usuário',
    alterarStatusUsuario: 'Erro ao alterar status do usuário',
    login: 'Erro ao fazer login',
    credenciaisInvalidas: 'Usuário ou senha inválidos.',
    finalizarConsulta: 'Erro ao finalizar consulta.',
    registrarHorario: 'Erro ao registrar o horário.',
    criarRegraRecorrente: 'Erro ao criar regra recorrente.',
    removerRegra: 'Erro ao remover a regra.',
    removerBloqueio: 'Erro ao remover o bloqueio.',
    identificarProfissional: 'Não foi possível identificar o profissional.',
    carregarHorariosIndisponiveis: 'Erro ao carregar horários indisponíveis.',
    carregarPrescricoes: 'Erro ao carregar as prescrições.',
    salvarPrescricao: 'Erro ao salvar a prescrição.',
    excluirPrescricao: 'Erro ao excluir a prescrição.',
    carregarAnexos: 'Erro ao carregar os anexos.',
    enviarAnexo: 'Erro ao enviar o anexo.',
    excluirAnexo: 'Erro ao excluir o anexo.',
    abrirAnexo: 'Não foi possível abrir o anexo.',
    anexoTipoInvalido: 'Tipo de arquivo não permitido. Envie imagem (JPG, PNG, WEBP) ou PDF.',
    anexoTamanho: 'Arquivo muito grande. O tamanho máximo é 10 MB.',
    carregarHistorico: 'Erro ao carregar o histórico clínico.',
    carregarProcedimentos: 'Erro ao carregar os procedimentos.',
    salvarProcedimento: 'Erro ao salvar o procedimento.',
    excluirProcedimento: 'Erro ao excluir o procedimento.',
  },
  sucesso: {
    horarioRegistrado: 'Horário registrado com sucesso!',
    regraCriada: 'Regra recorrente criada com sucesso!',
    bloqueioRemovido: 'Bloqueio removido com sucesso!',
    regraRemovida: 'Regra recorrente removida!',
    regrasCriadas: (n: number) => `${n} regras recorrentes criadas!`,
    usuarioExcluido: 'Usuário excluído com sucesso!',
    usuarioAtualizado: 'Usuário atualizado com sucesso!',
    usuarioCriado: 'Usuário criado com sucesso!',
    perfilAtualizado: 'Perfil atualizado com sucesso!',
    senhaAlterada: 'Senha alterada com sucesso!',
    consultaFinalizada: 'Consulta finalizada com sucesso!',
    prescricaoCriada: 'Prescrição registrada com sucesso!',
    prescricaoAtualizada: 'Prescrição atualizada com sucesso!',
    prescricaoExcluida: 'Prescrição excluída com sucesso!',
    anexoEnviado: 'Anexo enviado com sucesso!',
    anexoExcluido: 'Anexo excluído com sucesso!',
    procedimentoCriado: 'Procedimento criado com sucesso!',
    procedimentoAtualizado: 'Procedimento atualizado com sucesso!',
    procedimentoExcluido: 'Procedimento excluído com sucesso!',
  },
  validacao: {
    nomeObrigatorio: 'Nome é obrigatório',
    descricaoPrescricaoObrigatoria:
      'Informe o medicamento ou cuidado prescrito',
    emailInvalido: 'Email inválido',
    senhaAtualObrigatoria: 'Informe a senha atual',
    novaSenhaObrigatoria: 'Informe a nova senha',
    senhaMinima: 'A senha deve ter pelo menos 6 caracteres',
    confirmarSenhaObrigatoria: 'Confirme a nova senha',
    senhasNaoCoincidem: 'As senhas não coincidem',
    horarioTerminoInvalido:
      'O horário de término deve ser posterior ao de início.',
    selecioneDiaSemana: 'Selecione ao menos um dia da semana.',
    diasJaComRegra:
      'Todos os dias selecionados já possuem regras recorrentes. Selecione um novo dia para adicionar.',
  },
} as const

/**
 * Extrai a mensagem de erro priorizando o que o backend retornou
 * (`response.data.message`), depois `err.message`, e por fim um fallback local.
 */
export function mensagemErro(
  err: unknown,
  fallback: string = MENSAGENS.erro.generico,
): string {
  const e = err as {
    response?: { data?: { message?: string } }
    message?: string
  }
  return e?.response?.data?.message || e?.message || fallback
}

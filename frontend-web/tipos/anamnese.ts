// Anamnese do paciente (RF13) — preenchida durante o agendamento de AVALIACAO.
// Modelo: ajustar_campos/modelos/Anamnese.pdf
// Espelha AnamneseRequestDTO / AnamneseResponseDTO do backend.
//
// Os dados de identificação do cabeçalho do modelo (nome, nascimento, sexo,
// telefone) vêm do cadastro do paciente e não são campos do formulário.

/** Rede de apoio informada na anamnese. */
export type RedeApoio =
  | 'CUIDADOR'
  | 'FAMILIAR_RESPONSAVEL'
  | 'CONDICOES_CURATIVOS_CASA'

export interface AnamneseRequisicao {
  // Identificação complementar
  profissao?: string
  responsavelCuidador?: string
  // História da ferida
  motivoConsulta?: string
  tempoExistenciaFerida?: string
  comoFeridaSurgiu?: string
  /** Data no formato YYYY-MM-DD */
  dataInicioAproximada?: string
  tratamentosAnteriores?: string
  curativosPrevios?: string
  // Histórico de saúde (Sim/Não + detalhe)
  diabetesMellitus?: boolean
  diabetesMellitusDetalhe?: string
  hipertensaoArterial?: boolean
  hipertensaoArterialDetalhe?: string
  doencaVenosaCronica?: boolean
  doencaVenosaCronicaDetalhe?: string
  doencaArterialPeriferica?: boolean
  doencaArterialPerifericaDetalhe?: string
  insuficienciaRenal?: boolean
  insuficienciaRenalDetalhe?: string
  cancer?: boolean
  cancerDetalhe?: string
  problemasNeurologicos?: boolean
  problemasNeurologicosDetalhe?: string
  historicoCirurgias?: boolean
  historicoCirurgiasDetalhe?: string
  // Medicamentos em uso
  medAntibioticos?: boolean
  medAnticoagulantes?: boolean
  medCorticoides?: boolean
  medInsulinaHipoglicemiantes?: boolean
  medOutrosContinuos?: boolean
  // Alergias
  alergiaMedicamentos?: boolean
  alergiaProdutosTopicos?: boolean
  alergiaCurativosAdesivos?: boolean
  // Hábitos de vida
  tabagismo?: boolean
  consumoAlcool?: boolean
  alimentacaoEstadoNutricional?: string
  ingestaoHidrica?: boolean
  ingestaoHidricaDetalhe?: string
  // Mobilidade e funcionalidade
  deambulaSozinho?: boolean
  deambulaSozinhoDetalhe?: string
  acamadoOuCadeirante?: boolean
  acamadoOuCadeiranteDetalhe?: string
  usoDispositivos?: boolean
  usoDispositivosDetalhe?: string
  mudancaPosicaoLeito?: boolean
  mudancaPosicaoLeitoDetalhe?: string
  // Outros
  examesRecentes?: boolean
  redeApoio?: RedeApoio
  acompanhamentoMedico?: boolean
  /** Nome / especialidade / contato do acompanhamento médico. */
  acompanhamentoMedicoDetalhe?: string
}

/** Resposta do backend (AnamneseResponseDTO) — requisição + auditoria. */
export interface Anamnese extends AnamneseRequisicao {
  uuid: string
  pacienteUuid: string
  pacienteNome?: string
  registradoPor?: string
  criadoEm?: string
  atualizadoEm?: string
}

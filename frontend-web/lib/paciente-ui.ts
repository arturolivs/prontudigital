const CORES_AVATAR = [
  '#2b6cb0',
  '#7991bc',
  '#10b981',
  '#f59e0b',
  '#8b5cf6',
  '#ef4444',
  '#3b82f6',
  '#ec4899',
]

export const obterIniciais = (nome: string): string => {
  const partes = nome.trim().split(' ').filter(Boolean)
  if (partes.length === 1) return partes[0].substring(0, 2).toUpperCase()
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase()
}

export const obterCorAvatar = (uuid: string): string => {
  let hash = 0
  for (let i = 0; i < uuid.length; i++) {
    hash = uuid.charCodeAt(i) + ((hash << 5) - hash)
  }
  return CORES_AVATAR[Math.abs(hash) % CORES_AVATAR.length]
}

export const formatarDataCurta = (dataString: string): string =>
  new Date(dataString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })

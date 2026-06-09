export function mascaraTelefone(valor: string): string {
  const digits = valor.replace(/\D/g, '').slice(0, 11)
  const n = digits.length

  if (n === 0) return ''
  if (n <= 2) return `(${digits}`
  if (n === 3) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`
  if (n <= 7) return `(${digits.slice(0, 2)}) ${digits.slice(2, 3)} ${digits.slice(3)}`
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 3)} ${digits.slice(3, 7)}-${digits.slice(7)}`
}

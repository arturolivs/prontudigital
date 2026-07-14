'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useParams, useSearchParams } from 'next/navigation'
import { RotaProtegida } from '../../../../components/RotaProtegida'
import { useAuth } from '../../../../contexts/AuthContext'
import Layout from '@/components/Layout/Layout'
import AbaPrescricoes from './AbaPrescricoes'
import AbaAnexos from './AbaAnexos'
import AbaHistorico from './AbaHistorico'
import './prontuario.css'

type Aba = 'historico' | 'prescricoes' | 'anexos'

const ABAS: { id: Aba; rotulo: string }[] = [
  { id: 'historico', rotulo: 'Histórico' },
  { id: 'prescricoes', rotulo: 'Prescrições' },
  { id: 'anexos', rotulo: 'Anexos' },
]

export default function ProntuarioPage() {
  const params = useParams()
  const searchParams = useSearchParams()
  const pacienteUuid = String(params.pacienteUuid)
  const nomeQuery = searchParams.get('nome')

  const { usuario, temPerfil } = useAuth()

  const [aba, setAba] = useState<Aba>('historico')
  const [nomePaciente, setNomePaciente] = useState<string>(
    nomeQuery || 'Paciente',
  )

  const isAdmin = temPerfil('ROLE_ADMIN')
  const isProfissional = temPerfil('ROLE_PROFISSIONAL')
  const hasRequiredRole = isAdmin || isProfissional

  if (!usuario || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    )
  }

  return (
    <Layout perfil={isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="pront-page">
          <div className="pront-breadcrumb">
            <Link href="/pacientes" className="pront-voltar">
              ← Pacientes
            </Link>
          </div>

          <div className="pront-header">
            <div>
              <h1 className="pront-titulo">{nomePaciente}</h1>
              <span className="pront-subtitulo">Prontuário</span>
            </div>
          </div>

          <div
            className="pront-tabs"
            role="tablist"
            aria-label="Seções do prontuário"
          >
            {ABAS.map(({ id, rotulo }) => (
              <button
                key={id}
                role="tab"
                aria-selected={aba === id}
                className={`pront-tab${aba === id ? ' pront-tab-ativa' : ''}`}
                onClick={() => setAba(id)}
              >
                {rotulo}
              </button>
            ))}
          </div>

          {aba === 'historico' && <AbaHistorico pacienteUuid={pacienteUuid} />}
          {aba === 'prescricoes' && (
            <AbaPrescricoes
              pacienteUuid={pacienteUuid}
              onNomePaciente={setNomePaciente}
            />
          )}
          {aba === 'anexos' && (
            <AbaAnexos
              pacienteUuid={pacienteUuid}
              onNomePaciente={setNomePaciente}
            />
          )}
        </div>
      </RotaProtegida>
    </Layout>
  )
}

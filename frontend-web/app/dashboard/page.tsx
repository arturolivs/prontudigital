'use client'

import { useAuth } from '../../contexts/AuthContext'
import './dashboard.css' // ← novo CSS específico para o dashboard

export default function DashboardPage() {
  const { usuario, temPerfil } = useAuth()

  return (
    <div className="dashboard-page">
      {' '}
      {/* ← container com mesmo padding da agenda */}
      <h1 className="dashboard-title">Dashboard Administrativo</h1>
      <div className="info-card">
        <h3 className="info-card-title">Informações do Usuário</h3>
        <div className="info-grid">
          <div>
            <p className="info-label">Username</p>
            <p className="info-value">{usuario?.username}</p>
          </div>
          <div>
            <p className="info-label">Perfis</p>
            <div className="perfis-container">
              {usuario?.perfis.map((perfil: string) => (
                <span key={perfil} className="perfil-badge">
                  {perfil}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
      <div className="stats-grid">
        <div className="stat-card">
          <h3>Estatísticas</h3>
          <p>Visualize métricas do sistema</p>
        </div>
        <div className="stat-card">
          <h3>Gerenciar Usuários</h3>
          <p>Administre contas de usuários</p>
        </div>
        <div className="stat-card">
          <h3>Configurações</h3>
          <p>Configure o sistema</p>
        </div>
      </div>
    </div>
  )
}

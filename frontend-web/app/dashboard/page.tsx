'use client'

import { useAuth } from '../../contexts/AuthContext'

export default function DashboardPage() {
  const { usuario, temPerfil } = useAuth()

  return (
    <div>
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

      <style jsx>{`
        .dashboard-title {
          font-size: 1.875rem;
          font-weight: 700;
          color: var(--color-brand);
          margin-bottom: 1.5rem;
        }
        .info-card {
          background: white;
          border-radius: 1rem;
          padding: 1.5rem;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
          margin-bottom: 2rem;
          border: 1px solid var(--color-bg-alt);
        }
        .info-card-title {
          font-size: 1.125rem;
          font-weight: 600;
          color: var(--color-brand);
          margin-bottom: 1rem;
        }
        .info-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 1rem;
        }
        .info-label {
          font-size: 0.75rem;
          text-transform: uppercase;
          font-weight: 600;
          color: var(--color-brand-light);
          margin-bottom: 0.25rem;
        }
        .info-value {
          font-weight: 500;
          color: var(--color-text-heading);
        }
        .perfis-container {
          display: flex;
          gap: 0.5rem;
          flex-wrap: wrap;
        }
        .perfil-badge {
          background: var(--color-bg-alt);
          color: var(--color-brand);
          padding: 0.25rem 0.75rem;
          border-radius: 2rem;
          font-size: 0.75rem;
          font-weight: 600;
        }
        .stats-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
          gap: 1.5rem;
        }
        .stat-card {
          background: white;
          border-radius: 1rem;
          padding: 1.5rem;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
          border: 1px solid var(--color-bg-alt);
          transition:
            transform 0.2s,
            box-shadow 0.2s;
        }
        .stat-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 20px rgba(43, 108, 176, 0.1);
        }
        .stat-card h3 {
          font-size: 1.125rem;
          font-weight: 600;
          color: var(--color-brand);
          margin-bottom: 0.5rem;
        }
        .stat-card p {
          color: var(--color-text-mid);
          font-size: 0.875rem;
        }
      `}</style>
    </div>
  )
}

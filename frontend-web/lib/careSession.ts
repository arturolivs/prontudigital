// lib/appointments.ts
import axios from 'axios'
import { Appointment } from '../types/appointment'

import careSessions from '../mock/careSession'
import { CareSession } from '@/types/CareSession'
import { SessionStatus } from '@/types/SessionStatus'
const API_URL =
  process.env.NEXT_PUBLIC_APPOINTMENTS_API_URL ||
  'http://localhost:9090/api/schedule/appointments'

const api = axios.create()

api.interceptors.request.use(config => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

export const careSessionAPI = {
  getCareSessions: async (
    viewType: string,
    date: Date | string,
  ): Promise<CareSession[]> => {
    /*  viewType = 'month'
    const formattedDate =
      typeof date === 'string' ? date : date.toISOString().split('T')[0]
    console.log('formattedDate @@@@@', formattedDate)
    const response = await api.get(`${API_URL}/view`, {
      params: {
        viewType,
        date: formattedDate,
      },
    })
    return response.data
*/
    return careSessions
  },

  createAppointment: async (
    appointmentData: Partial<Appointment>,
  ): Promise<Appointment> => {
    const response = await api.post(`${API_URL}`, appointmentData)
    return response.data
  },

  updateAppointment: async (
    id: number,
    appointmentData: Partial<Appointment>,
  ): Promise<Appointment> => {
    const response = await api.put(`${API_URL}/${id}`, appointmentData)
    return response.data
  },

  deleteAppointment: async (id: number): Promise<void> => {
    await api.delete(`${API_URL}/${id}`)
  },

  async getCareSessionById(id: string): Promise<CareSession> {
    /*  const response = await fetch(`/api/care-sessions/${id}`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    })

    if (!response.ok) {
      throw new Error('Erro ao buscar sessão')
    }

    return response.json()

    */

    await new Promise(resolve => setTimeout(resolve, 300))

    // Buscar a sessão pelo ID no array de mocks
    const session = careSessions.find(s => s.sessionUuid === id)

    if (!session) {
      throw new Error(`Sessão com ID ${id} não encontrada`)
    }

    // Retornar uma cópia para evitar mutação acidental
    return { ...session }
  },

  async updateSessionStatus(id: string, status: SessionStatus): Promise<void> {
    const response = await fetch(`/api/care-sessions/${id}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ status }),
    })

    if (!response.ok) {
      throw new Error('Erro ao atualizar status da sessão')
    }
  },
}

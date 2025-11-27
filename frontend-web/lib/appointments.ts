// lib/appointments.ts
import axios from 'axios'
import { Appointment } from '../types/appointment'

const API_URL =
  process.env.NEXT_PUBLIC_APPOINTMENTS_API_URL ||
  'http://localhost:8081/api/appointments'

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

export const appointmentsAPI = {
  getAppointments: async (
    professionalUuid: string,
    viewType: string,
    date: string,
  ): Promise<Appointment[]> => {
    const response = await api.get(`${API_URL}/view`, {
      params: {
        professionalUuid,
        viewType,
        date,
      },
    })
    return response.data
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
}

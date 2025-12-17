// lib/appointments.ts
import axios from 'axios'
import { Appointment } from '../types/appointment'

import careSessions from '../mock/careSession'
import { CareSession } from '@/types/CareSession'

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

export const careSessionAPI = {
  getCareSessions: async (
    professionalUuid: string,
    viewType: string,
    date: string,
  ): Promise<CareSession[]> => {
    /**
    const response = await api.get(`${API_URL}/view`, {
      params: {
        professionalUuid,
        viewType,
        date,
      },
    })
    return response.data 
    // */
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
}

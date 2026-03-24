export interface User {
  username: string
  roles: string[]
  token: string
  professionalUuid?: string
}

export interface UserFull {
  id: number
  fullName: string
  username: string
  email: string
  isActive: boolean
  roles: string[]
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  roles: string[]
  professionalUuid?: string
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  username: string
  password: string
  roles?: string[]
}

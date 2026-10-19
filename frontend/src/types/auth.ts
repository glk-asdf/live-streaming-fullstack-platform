export type UserRole = 'USER' | 'ANCHOR' | 'ADMIN'

export interface User {
  id: number
  username: string
  nickname: string
  avatar?: string | null
  bio?: string | null
  role: UserRole
  status: number
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname?: string
}

export interface UpdateProfilePayload {
  nickname?: string
  avatar?: string
  bio?: string
}

import request from './request'
import type {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
  User,
} from '@/types/auth'

export function login(payload: LoginPayload): Promise<AuthResponse> {
  return request.post('/auth/login', payload)
}

export function register(payload: RegisterPayload): Promise<AuthResponse> {
  return request.post('/auth/register', payload)
}

export function logout(refreshToken: string | null): Promise<void> {
  return request.post('/auth/logout', { refreshToken })
}

export function fetchMe(): Promise<User> {
  return request.get('/users/me')
}

export function updateMe(payload: UpdateProfilePayload): Promise<User> {
  return request.put('/users/me', payload)
}

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, User } from '@/types/auth'

interface UserState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  setAuth: (auth: AuthResponse) => void
  setUser: (user: User) => void
  clearAuth: () => void
}

/**
 * 登录态持久化到 localStorage。
 * 学习项目采用该方案；生产可改为 HttpOnly Cookie + CSRF 防护，降低 XSS 窃取风险。
 */
export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setAuth: (auth) =>
        set({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          user: auth.user,
        }),
      setUser: (user) => set({ user }),
      clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'live-auth' },
  ),
)

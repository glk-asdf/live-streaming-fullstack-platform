import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useUserStore } from '@/store/useUserStore'

export default function AuthGuard({ children }: { children: ReactNode }) {
  const accessToken = useUserStore((state) => state.accessToken)
  const location = useLocation()

  if (!accessToken) {
    const redirect = encodeURIComponent(location.pathname)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return <>{children}</>
}

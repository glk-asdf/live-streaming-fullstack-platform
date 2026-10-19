import { Spin } from 'antd'
import { lazy, Suspense, useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'
import AuthGuard from '@/components/AuthGuard'
import HomePage from '@/pages/HomePage'
import NotFoundPage from '@/pages/NotFoundPage'

const LoginPage = lazy(() => import('@/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/RegisterPage'))
const ProfilePage = lazy(() => import('@/pages/ProfilePage'))
const AnchorPage = lazy(() => import('@/pages/AnchorPage'))
const RoomPage = lazy(() => import('@/pages/RoomPage'))

function ScrollToHash() {
  const { hash, pathname } = useLocation()

  useEffect(() => {
    if (hash) {
      const target = document.getElementById(hash.slice(1))
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' })
        return
      }
    }
    window.scrollTo({ top: 0 })
  }, [hash, pathname])

  return null
}

export default function App() {
  return (
    <>
      <ScrollToHash />
      <Suspense
        fallback={
          <div className="page-loading">
            <Spin size="large" />
          </div>
        }
      >
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/rooms/:id" element={<RoomPage />} />
          <Route
            path="/profile"
            element={
              <AuthGuard>
                <ProfilePage />
              </AuthGuard>
            }
          />
          <Route
            path="/anchor"
            element={
              <AuthGuard>
                <AnchorPage />
              </AuthGuard>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </>
  )
}

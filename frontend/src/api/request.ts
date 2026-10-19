import axios, { AxiosError } from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/store/useUserStore'
import type { ApiResult } from '@/types/api'
import type { AuthResponse } from '@/types/auth'

interface ApiClient {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T>
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
})

// 刷新令牌使用裸实例，避免触发响应拦截器造成递归
const rawRequest = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
})

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const { refreshToken, setAuth, clearAuth } = useUserStore.getState()
  if (!refreshToken) {
    throw new Error('缺少 refreshToken')
  }
  try {
    const response = await rawRequest.post<ApiResult<AuthResponse>>('/auth/refresh', { refreshToken })
    const body = response.data
    if (body.code !== 0) {
      throw new Error(body.message || '登录已过期')
    }
    setAuth(body.data)
    return body.data.accessToken
  } catch (error) {
    clearAuth()
    throw error
  }
}

request.interceptors.request.use((config) => {
  const token = useUserStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response.data
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const original = error.config as RetryableConfig | undefined
    const shouldRefresh =
      error.response?.status === 401 &&
      original != null &&
      !original._retry &&
      !original.url?.includes('/auth/')

    if (shouldRefresh && useUserStore.getState().refreshToken) {
      original._retry = true
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null
        })
        const token = await refreshPromise
        original.headers.Authorization = `Bearer ${token}`
        return request(original)
      } catch {
        // 刷新失败时清理登录态，交由路由守卫跳转登录页
      }
    }

    const message =
      error.response?.data?.message ??
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请检查服务是否启动')
    return Promise.reject(new Error(message))
  },
)

export default request as unknown as ApiClient

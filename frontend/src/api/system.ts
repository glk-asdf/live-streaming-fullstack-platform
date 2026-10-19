import request from './request'

export interface PingData {
  service: string
  version: string
  serverTime: string
}

/** 后端探活 */
export function fetchPing(): Promise<PingData> {
  return request.get('/ping')
}

/** MediaMTX 管理 API 探活（开发环境经 Vite 代理），顺带读取活跃流数量 */
export async function fetchMediaStatus(): Promise<{ activeStreams: number }> {
  const response = await fetch('/media-api/v3/paths/list')
  if (!response.ok) {
    throw new Error(`媒体服务器返回 ${response.status}`)
  }
  const data = (await response.json()) as { items?: unknown[] }
  return { activeStreams: data.items?.length ?? 0 }
}

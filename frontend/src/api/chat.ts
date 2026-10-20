import request from './request'
import type { ChatMessage } from '@/types/chat'

export function fetchChatHistory(roomId: number, limit = 100): Promise<ChatMessage[]> {
  return request.get(`/rooms/${roomId}/messages`, { params: { limit } })
}

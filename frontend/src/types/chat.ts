export interface ChatMessage {
  id: number
  roomId: number
  userId: number
  nickname?: string | null
  avatar?: string | null
  content: string
  type: number
  createdAt: string
}

export interface ViewerCountData {
  roomId: number
  count: number
}

export interface RoomEndData {
  roomId: number
  reason: string
}

export type WsMessageType = 'CHAT' | 'VIEWER_COUNT' | 'ROOM_END' | 'ERROR'

export interface WsMessage<T = unknown> {
  type: WsMessageType
  data: T
  ts: number
}

export type RoomStatus = 0 | 1 | 2 | 3

export interface LiveRoom {
  id: number
  anchorId: number
  categoryId?: number | null
  title: string
  cover?: string | null
  status: RoomStatus
  viewerCount: number
  peakViewer: number
  likeCount: number
  hotScore: number
  liveStartTime?: string | null
  liveEndTime?: string | null
  createdAt: string
  anchorNickname?: string | null
  anchorAvatar?: string | null
}

export interface RoomSavePayload {
  title: string
  cover?: string
  categoryId?: number | null
}

export interface RoomStream {
  roomId: number
  server: string
  streamKey: string
  url: string
  status: RoomStatus
}

export interface Category {
  id: number
  name: string
  icon?: string | null
  sort: number
}

export interface PlayInfo {
  roomId: number
  whepUrl: string
  hlsUrl: string
  token: string
  expireAt: string
}

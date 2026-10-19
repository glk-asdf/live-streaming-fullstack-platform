import request from './request'
import type { PageResult } from '@/types/api'
import type { Category, LiveRoom, PlayInfo, RoomSavePayload, RoomStream } from '@/types/room'

export async function fetchMyRoom(): Promise<LiveRoom | null> {
  const room = await request.get<LiveRoom | null>('/rooms/mine')
  return room ?? null
}

export function fetchRooms(params?: {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
  status?: number
}): Promise<PageResult<LiveRoom>> {
  return request.get('/rooms', { params })
}

export function fetchRoom(id: number): Promise<LiveRoom> {
  return request.get(`/rooms/${id}`)
}

export function fetchPlayInfo(id: number): Promise<PlayInfo> {
  return request.get(`/rooms/${id}/play-info`)
}

export function createRoom(payload: RoomSavePayload): Promise<LiveRoom> {
  return request.post('/rooms', payload)
}

export function updateRoom(id: number, payload: RoomSavePayload): Promise<LiveRoom> {
  return request.put(`/rooms/${id}`, payload)
}

export function startRoom(id: number): Promise<RoomStream> {
  return request.post(`/rooms/${id}/start`)
}

export function resetRoomStreamKey(id: number): Promise<RoomStream> {
  return request.post(`/rooms/${id}/stream-key/reset`)
}

export function fetchRoomStream(id: number): Promise<RoomStream> {
  return request.get(`/rooms/${id}/stream`)
}

export function stopRoom(id: number): Promise<void> {
  return request.post(`/rooms/${id}/stop`)
}

export async function fetchCategories(): Promise<Category[]> {
  return request.get('/categories')
}

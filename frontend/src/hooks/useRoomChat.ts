import { Client, type IMessage } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { fetchMe } from '@/api/auth'
import { fetchChatHistory } from '@/api/chat'
import { useUserStore } from '@/store/useUserStore'
import type { ChatMessage, RoomEndData, ViewerCountData, WsMessage } from '@/types/chat'

interface RoomChat {
  messages: ChatMessage[]
  viewerCount: number
  connected: boolean
  ended: boolean
  error?: string
  send: (content: string) => boolean
  clearError: () => void
}

function mergeMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const map = new Map<number, ChatMessage>()
  for (const message of [...current, ...incoming]) {
    map.set(message.id, message)
  }
  return [...map.values()].sort((a, b) => a.id - b.id)
}

export function useRoomChat(roomId: number, enabled: boolean): RoomChat {
  const accessToken = useUserStore((state) => state.accessToken)
  const user = useUserStore((state) => state.user)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [viewerCount, setViewerCount] = useState(0)
  const [connected, setConnected] = useState(false)
  const [ended, setEnded] = useState(false)
  const [error, setError] = useState<string>()
  const clientRef = useRef<Client | null>(null)
  const endedRef = useRef(false)
  const enabledRef = useRef(false)

  useEffect(() => {
    setMessages([])
    setViewerCount(0)
    setEnded(false)
    setError(undefined)
    endedRef.current = false
  }, [roomId])

  useEffect(() => {
    if (!enabled || !Number.isFinite(roomId)) {
      enabledRef.current = false
      return
    }
    // 同一房间关播后重新开播时，允许重新进入连接态
    if (!enabledRef.current) {
      setEnded(false)
      setError(undefined)
      endedRef.current = false
    }
    enabledRef.current = true

    let disposed = false
    let heartbeatTimer: number | undefined
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      reconnectDelay: 3_000,
    })
    clientRef.current = client

    const handleMessage = (frame: IMessage) => {
      if (disposed) {
        return
      }
      try {
        const envelope = JSON.parse(frame.body) as WsMessage
        if (envelope.type === 'CHAT') {
          setMessages((current) => mergeMessages(current, [envelope.data as ChatMessage]))
        } else if (envelope.type === 'VIEWER_COUNT') {
          setViewerCount((envelope.data as ViewerCountData).count)
        } else if (envelope.type === 'ROOM_END') {
          endedRef.current = true
          setEnded(true)
          if (heartbeatTimer) {
            window.clearInterval(heartbeatTimer)
            heartbeatTimer = undefined
          }
          const data = envelope.data as RoomEndData
          setError(data.reason === 'ended' ? undefined : data.reason)
        }
      } catch {
        // 忽略非 JSON 的下行帧
      }
    }

    client.onConnect = () => {
      if (disposed || endedRef.current) {
        return
      }
      setConnected(true)
      setError(undefined)
      client.subscribe(`/topic/rooms/${roomId}`, handleMessage)
      client.subscribe('/user/queue/errors', (frame) => {
        if (disposed) {
          return
        }
        try {
          const envelope = JSON.parse(frame.body) as WsMessage<string>
          setError(envelope.data)
        } catch {
          setError('发送失败，请稍后重试')
        }
      })
      if (heartbeatTimer) {
        window.clearInterval(heartbeatTimer)
      }
      client.publish({
        destination: `/app/rooms/${roomId}/heartbeat`,
        body: '{}',
      })
      heartbeatTimer = window.setInterval(() => {
        if (client.connected) {
          client.publish({
            destination: `/app/rooms/${roomId}/heartbeat`,
            body: '{}',
          })
        }
      }, 15_000)
    }

    client.onStompError = (frame) => {
      if (!disposed) {
        setConnected(false)
        setError(frame.headers.message ?? 'WebSocket 连接失败')
      }
    }
    client.onWebSocketClose = () => {
      if (!disposed) {
        setConnected(false)
      }
    }

    void fetchChatHistory(roomId)
      .then((history) => {
        if (!disposed) {
          setMessages((current) => mergeMessages(current, history))
        }
      })
      .catch(() => {
        if (!disposed) {
          setError('弹幕历史加载失败')
        }
      })

    // 公开房间页不会触发 401 刷新；先探一次 /users/me，让过期 accessToken 在连接前刷新
    if (accessToken) {
      void fetchMe().catch(() => undefined)
    }
    client.activate()

    return () => {
      disposed = true
      if (heartbeatTimer) {
        window.clearInterval(heartbeatTimer)
      }
      void client.deactivate()
      clientRef.current = null
      setConnected(false)
    }
  }, [roomId, enabled, accessToken])

  const send = (content: string) => {
    const text = content.trim()
    if (!user) {
      setError('请先登录后再发言')
      return false
    }
    if (!text) {
      return false
    }
    if (ended) {
      setError('直播已结束')
      return false
    }
    const client = clientRef.current
    if (!client?.connected) {
      setError('弹幕连接未建立，请稍后重试')
      return false
    }
    try {
      setError(undefined)
      client.publish({
        destination: `/app/rooms/${roomId}/chat`,
        body: JSON.stringify({ content: text }),
      })
      return true
    } catch {
      setError('发送失败，请稍后重试')
      return false
    }
  }

  return {
    messages,
    viewerCount,
    connected,
    ended,
    error,
    send,
    clearError: () => setError(undefined),
  }
}

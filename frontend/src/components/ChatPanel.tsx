import { Alert, Avatar, Button, Card, Empty, Input, Space, Tag, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useUserStore } from '@/store/useUserStore'
import type { ChatMessage } from '@/types/chat'

const { Text } = Typography

interface ChatPanelProps {
  active: boolean
  ended: boolean
  blocked?: boolean
  connected: boolean
  messages: ChatMessage[]
  viewerCount: number
  error?: string
  onSend: (content: string) => boolean
  onClearError: () => void
}

export default function ChatPanel({
  active,
  ended,
  blocked = false,
  connected,
  messages,
  viewerCount,
  error,
  onSend,
  onClearError,
}: ChatPanelProps) {
  const user = useUserStore((state) => state.user)
  const [value, setValue] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const list = listRef.current
    if (list) {
      list.scrollTop = list.scrollHeight
    }
  }, [messages])

  const submit = () => {
    if (onSend(value)) {
      setValue('')
    }
  }

  const disabled = !active || ended || !connected || !user

  return (
    <Card
      variant="borderless"
      className="room-side-card chat-card"
      title="实时弹幕"
      extra={
        <Space size={6}>
          <Tag color="blue" bordered={false}>
            {viewerCount} 人在线
          </Tag>
          <Tag color={connected ? 'success' : 'default'} bordered={false}>
            {blocked ? '已封禁' : ended ? '已结束' : !active ? '未开始' : connected ? '已连接' : '连接中'}
          </Tag>
        </Space>
      }
    >
      {error ? (
        <Alert
          type="warning"
          showIcon
          closable
          message={error}
          onClose={onClearError}
          className="chat-alert"
        />
      ) : null}
      {ended ? (
        <Alert type="info" showIcon message="直播已结束" className="chat-alert" />
      ) : null}
      <div ref={listRef} className="chat-list">
        {messages.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              blocked ? '直播间已被封禁' : active ? '还没有弹幕，来说两句吧' : '直播开始后可以发送弹幕'
            }
          />
        ) : (
          messages.map((message) =>
            message.type === 2 ? (
              <div className="chat-system" key={message.id}>
                {message.content}
              </div>
            ) : (
              <div className="chat-item" key={message.id}>
                <Avatar size={24} src={message.avatar || undefined}>
                  {(message.nickname ?? '游').slice(0, 1)}
                </Avatar>
                <div className="chat-body">
                  <Text className="chat-name">{message.nickname ?? `用户${message.userId}`}</Text>
                  <Text className="chat-content">{message.content}</Text>
                </div>
              </div>
            ),
          )
        )}
      </div>
      <div className="chat-submit">
        <Input
          value={value}
          maxLength={100}
          disabled={disabled}
          placeholder={
            blocked
              ? '直播间已被封禁'
              : !user
                ? '登录后参与弹幕'
                : ended
                  ? '直播已结束'
                  : !connected
                    ? '正在连接弹幕…'
                    : '说点什么…'
          }
          onChange={(event) => setValue(event.target.value)}
          onPressEnter={submit}
        />
        <Button type="primary" disabled={disabled || !value.trim()} onClick={submit}>
          发送
        </Button>
      </div>
      {!user ? (
        <div className="chat-login-hint">
          <Text type="secondary">游客可以观看弹幕，</Text>
          <Link to="/login">登录后发言</Link>
        </div>
      ) : null}
    </Card>
  )
}

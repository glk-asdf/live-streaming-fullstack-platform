import { EyeOutlined } from '@ant-design/icons'
import { Avatar, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import type { LiveRoom } from '@/types/room'

const { Text } = Typography

export default function LiveRoomCard({ room }: { room: LiveRoom }) {
  return (
    <Link to={`/rooms/${room.id}`} className="live-room-card">
      <div
        className="live-room-cover"
        style={room.cover ? { backgroundImage: `url(${room.cover})` } : undefined}
      >
        <Tag className="live-room-badge" color="red" bordered={false}>
          LIVE
        </Tag>
        <span className="live-room-viewers">
          <EyeOutlined /> {room.viewerCount}
        </span>
      </div>
      <div className="live-room-body">
        <Avatar size={34} src={room.anchorAvatar || undefined}>
          {(room.anchorNickname ?? '主').slice(0, 1)}
        </Avatar>
        <div className="live-room-meta">
          <Text strong className="live-room-title">
            {room.title}
          </Text>
          <Text type="secondary" className="live-room-anchor">
            {room.anchorNickname ?? `主播 #${room.anchorId}`}
          </Text>
        </div>
      </div>
    </Link>
  )
}

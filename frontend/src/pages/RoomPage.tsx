import { ArrowLeftOutlined, EyeOutlined, HeartOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Avatar, Button, Card, Empty, Skeleton, Space, Tag, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { fetchPlayInfo, fetchRoom } from '@/api/room'
import LivePlayer from '@/components/LivePlayer'
import SiteHeader from '@/components/SiteHeader'
import type { RoomStatus } from '@/types/room'

const { Title, Paragraph, Text } = Typography

const statusMeta: Record<RoomStatus, { label: string; color: string; hint: string }> = {
  0: { label: '未开播', color: 'default', hint: '主播还没有开始直播' },
  1: { label: '直播中', color: 'processing', hint: '' },
  2: { label: '已结束', color: 'default', hint: '这场直播已经结束' },
  3: { label: '已封禁', color: 'red', hint: '直播间不可观看' },
}

export default function RoomPage() {
  const { id } = useParams()
  const roomId = Number(id)
  const validId = Number.isFinite(roomId) && roomId > 0

  const roomQuery = useQuery({
    queryKey: ['room', roomId],
    queryFn: () => fetchRoom(roomId),
    enabled: validId,
    // 直播中定时刷新状态；下播后自动切到“已结束”，避免播放器一直转圈
    refetchInterval: (query) => (query.state.data?.status === 1 ? 10_000 : false),
  })
  const room = roomQuery.data
  const playInfoQuery = useQuery({
    queryKey: ['play-info', roomId],
    queryFn: () => fetchPlayInfo(roomId),
    enabled: validId && room?.status === 1,
    retry: 1,
    // 令牌 5 分钟有效；窗口聚焦时不要重新签发导致播放器重连
    staleTime: 4 * 60_000,
    refetchOnWindowFocus: false,
  })

  const status = room ? statusMeta[room.status] : null

  if (!validId) {
    return (
      <div className="page">
        <SiteHeader />
        <div className="room-wrap">
          <Empty description="直播间地址不合法" />
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <SiteHeader />
      <div className="room-wrap">
        <Link to="/" className="room-back">
          <ArrowLeftOutlined /> 返回首页
        </Link>

        {roomQuery.isLoading ? (
          <Card variant="borderless" className="room-card">
            <Skeleton active paragraph={{ rows: 8 }} />
          </Card>
        ) : roomQuery.isError || !room ? (
          <Card variant="borderless" className="room-card">
            <Empty description={roomQuery.error instanceof Error ? roomQuery.error.message : '直播间不存在'} />
            <div className="room-empty-action">
              <Button onClick={() => roomQuery.refetch()}>重试</Button>
              <Link to="/">
                <Button type="primary">看看其他直播</Button>
              </Link>
            </div>
          </Card>
        ) : (
          <div className="room-layout">
            <div className="room-main">
              {room.status === 1 && playInfoQuery.data ? (
                <LivePlayer
                  whepUrl={playInfoQuery.data.whepUrl}
                  hlsUrl={playInfoQuery.data.hlsUrl}
                />
              ) : (
                <div className="player-shell room-player-placeholder">
                  {room.status === 1 ? (
                    playInfoQuery.isError ? (
                      <Space direction="vertical" align="center">
                        <Text strong>获取播放地址失败</Text>
                        <Text type="secondary">
                          {playInfoQuery.error instanceof Error
                            ? playInfoQuery.error.message
                            : '请稍后重试'}
                        </Text>
                        <Button onClick={() => playInfoQuery.refetch()}>重试</Button>
                      </Space>
                    ) : (
                      <Skeleton.Node active style={{ width: 640, height: 220 }} />
                    )
                  ) : (
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description={status?.hint ?? '直播未开始'}
                    />
                  )}
                </div>
              )}

              <Card variant="borderless" className="room-info-card">
                <div className="room-info-head">
                  <Space align="center" size={14}>
                    <Avatar size={52} src={room.anchorAvatar || undefined}>
                      {(room.anchorNickname ?? '主').slice(0, 1)}
                    </Avatar>
                    <div>
                      <Space align="center" size={8} wrap>
                        <Title level={3} className="room-title">
                          {room.title}
                        </Title>
                        <Tag color={status?.color} bordered={false}>
                          {status?.label}
                        </Tag>
                      </Space>
                      <Text type="secondary">
                        {room.anchorNickname ?? `主播 #${room.anchorId}`}
                        <span className="room-dot">·</span>
                        {room.viewerCount} 人观看
                      </Text>
                    </div>
                  </Space>
                  <Button icon={<HeartOutlined />} disabled>
                    关注（P7）
                  </Button>
                </div>
              </Card>
            </div>

            <aside className="room-side">
              <Card
                variant="borderless"
                className="room-side-card"
                title="实时弹幕"
                extra={<Text type="secondary">P5 接入</Text>}
              >
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="弹幕与在线人数将在 P5 上线"
                />
                <div className="room-side-stats">
                  <Space>
                    <EyeOutlined />
                    <Text type="secondary">{room.viewerCount} 人正在看</Text>
                  </Space>
                  <Space>
                    <HeartOutlined />
                    <Text type="secondary">{room.likeCount} 次点赞</Text>
                  </Space>
                </div>
              </Card>
              <Card variant="borderless" className="room-side-card" title="直播信息">
                <Paragraph className="room-side-line">
                  <Text type="secondary">开播时间</Text>
                  <Text>{room.liveStartTime ?? '-'}</Text>
                </Paragraph>
                <Paragraph className="room-side-line">
                  <Text type="secondary">当前状态</Text>
                  <Text>{status?.label ?? '-'}</Text>
                </Paragraph>
                {playInfoQuery.data ? (
                  <Paragraph className="room-side-line">
                    <Text type="secondary">播放令牌</Text>
                    <Text>5 分钟有效</Text>
                  </Paragraph>
                ) : null}
              </Card>
            </aside>
          </div>
        )}
      </div>
    </div>
  )
}

import {
  PlayCircleOutlined,
  ReloadOutlined,
  SaveOutlined,
  StopOutlined,
  VideoCameraAddOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Popconfirm,
  Row,
  Select,
  Skeleton,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd'
import { useEffect } from 'react'
import {
  createRoom,
  fetchCategories,
  fetchMyRoom,
  fetchRoomStream,
  resetRoomStreamKey,
  startRoom,
  stopRoom,
  updateRoom,
} from '@/api/room'
import SiteHeader from '@/components/SiteHeader'
import type { RoomSavePayload, RoomStatus } from '@/types/room'

const { Title, Paragraph, Text } = Typography

const statusMeta: Record<RoomStatus, { label: string; color: string }> = {
  0: { label: '未开播', color: 'default' },
  1: { label: '直播中', color: 'processing' },
  2: { label: '已结束', color: 'default' },
  3: { label: '已封禁', color: 'red' },
}

export default function AnchorPage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const [editForm] = Form.useForm<RoomSavePayload>()
  const [createForm] = Form.useForm<RoomSavePayload>()

  const roomQuery = useQuery({
    queryKey: ['my-room'],
    queryFn: fetchMyRoom,
    refetchInterval: 5_000,
  })
  const categoriesQuery = useQuery({
    queryKey: ['categories'],
    queryFn: fetchCategories,
    staleTime: 5 * 60_000,
  })
  const room = roomQuery.data ?? null
  const streamQuery = useQuery({
    queryKey: ['room-stream', room?.id],
    queryFn: () => fetchRoomStream(room!.id),
    enabled: room != null && (room.status === 0 || room.status === 1),
  })

  useEffect(() => {
    if (!room) {
      return
    }
    editForm.setFieldsValue({
      title: room.title,
      cover: room.cover ?? '',
      categoryId: room.categoryId ?? undefined,
    })
    // 只在切换房间时回填，避免轮询覆盖用户正在编辑的内容
  }, [room?.id, editForm])

  const createMutation = useMutation({
    mutationFn: createRoom,
    onSuccess: (data) => {
      queryClient.setQueryData(['my-room'], data)
      createForm.resetFields()
      message.success('直播间已创建，可以开始直播了')
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '创建失败'),
  })

  const updateMutation = useMutation({
    mutationFn: (values: RoomSavePayload) => updateRoom(room!.id, values),
    onSuccess: (data) => {
      queryClient.setQueryData(['my-room'], data)
      message.success('直播间信息已保存')
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '保存失败'),
  })

  const startMutation = useMutation({
    mutationFn: () => startRoom(room!.id),
    onSuccess: (data) => {
      queryClient.setQueryData(['room-stream', data.roomId], data)
      // 防止开播前发出的旧推流信息请求晚到，覆盖刚轮换的新密钥
      queryClient.invalidateQueries({ queryKey: ['room-stream', data.roomId] })
      queryClient.invalidateQueries({ queryKey: ['my-room'] })
      message.success('已生成推流地址，去 OBS 开播吧')
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '开播失败'),
  })

  const resetMutation = useMutation({
    mutationFn: () => resetRoomStreamKey(room!.id),
    onSuccess: (data) => {
      queryClient.setQueryData(['room-stream', data.roomId], data)
      queryClient.invalidateQueries({ queryKey: ['room-stream', data.roomId] })
      message.success('推流密钥已重置，旧密钥立即失效')
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '重置失败'),
  })

  const stopMutation = useMutation({
    mutationFn: () => stopRoom(room!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-room'] })
      queryClient.invalidateQueries({ queryKey: ['room-stream'] })
      message.success('直播已结束')
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '结束失败'),
  })

  const status = room ? statusMeta[room.status] : null
  // start/reset 都会把最新结果写进 room-stream 缓存，统一从查询读取，避免显示上一次的旧密钥
  const stream = streamQuery.data
  const busy = startMutation.isPending || resetMutation.isPending || stopMutation.isPending

  return (
    <div className="page">
      <SiteHeader />
      <div className="anchor-wrap">
        <div className="section-head">
          <Title level={2}>主播中心</Title>
          <Paragraph className="anchor-sub">
            创建直播间、获取 OBS 推流地址，并在这里开始或结束直播。
          </Paragraph>
        </div>

        {roomQuery.isLoading ? (
          <Card variant="borderless" className="anchor-card">
            <Skeleton active />
          </Card>
        ) : roomQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="加载直播间失败"
            description={roomQuery.error instanceof Error ? roomQuery.error.message : '请稍后重试'}
            action={
              <Button size="small" onClick={() => roomQuery.refetch()}>
                重试
              </Button>
            }
          />
        ) : !room ? (
          <Card variant="borderless" className="anchor-card">
            <Title level={4}>创建你的直播间</Title>
            <Paragraph className="anchor-sub">
              一个账号同时只能有一个未结束的直播间；创建后自动获得主播身份。
            </Paragraph>
            <Form<RoomSavePayload> form={createForm} layout="vertical" onFinish={(values) => createMutation.mutate(values)}>
              <Form.Item
                name="title"
                label="直播间标题"
                rules={[
                  { required: true, message: '请输入标题' },
                  { max: 80, message: '标题最多 80 个字符' },
                ]}
              >
                <Input size="large" maxLength={80} showCount placeholder="今晚一起看比赛" />
              </Form.Item>
              <Form.Item name="categoryId" label="分类">
                <Select
                  size="large"
                  allowClear
                  placeholder="选择分类"
                  options={(categoriesQuery.data ?? []).map((category) => ({
                    value: category.id,
                    label: category.name,
                  }))}
                />
              </Form.Item>
              <Form.Item name="cover" label="封面地址（可选）" rules={[{ max: 255, message: '最多 255 个字符' }]}>
                <Input size="large" placeholder="https://..." />
              </Form.Item>
              <Button
                type="primary"
                size="large"
                htmlType="submit"
                icon={<VideoCameraAddOutlined />}
                loading={createMutation.isPending}
              >
                创建直播间
              </Button>
            </Form>
          </Card>
        ) : (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Card variant="borderless" className="anchor-card">
              <div className="anchor-head">
                <div>
                  <Space align="center" size={10} wrap>
                    <Title level={3} className="anchor-title">
                      {room.title}
                    </Title>
                    <Tag color={status?.color} bordered={false}>
                      {status?.label}
                    </Tag>
                  </Space>
                  <Text type="secondary">
                    {categoriesQuery.data?.find((item) => item.id === room.categoryId)?.name ?? '未分类'} ·
                    创建于 {room.createdAt}
                  </Text>
                </div>
                <Space wrap>
                  <Button
                    type="primary"
                    size="large"
                    icon={<PlayCircleOutlined />}
                    disabled={room.status === 1 || room.status === 3 || busy}
                    loading={startMutation.isPending}
                    onClick={() => startMutation.mutate()}
                  >
                    {room.status === 1 ? '直播中' : '开始直播'}
                  </Button>
                  <Popconfirm
                    title="结束这场直播？"
                    description="会断开当前推流，观众将无法继续观看。"
                    okText="结束直播"
                    cancelText="取消"
                    onConfirm={() => stopMutation.mutate()}
                    disabled={room.status !== 1}
                  >
                    <Button danger icon={<StopOutlined />} disabled={room.status !== 1 || busy} loading={stopMutation.isPending}>
                      结束直播
                    </Button>
                  </Popconfirm>
                </Space>
              </div>

              <Row gutter={[16, 16]} className="anchor-stats">
                <Col xs={8}>
                  <Statistic title="实时在线" value={room.viewerCount} />
                </Col>
                <Col xs={8}>
                  <Statistic title="峰值在线" value={room.peakViewer} />
                </Col>
                <Col xs={8}>
                  <Statistic title="点赞" value={room.likeCount} />
                </Col>
              </Row>
            </Card>

            {stream && (room.status === 0 || room.status === 1) ? (
              <Card
                variant="borderless"
                className="anchor-card"
                title="OBS 推流设置"
                extra={
                  <Popconfirm
                    title="重置推流密钥？"
                    description="旧密钥会立即失效，需要重新把新密钥填进 OBS。"
                    okText="重置"
                    cancelText="取消"
                    onConfirm={() => resetMutation.mutate()}
                  >
                    <Button
                      size="small"
                      icon={<ReloadOutlined />}
                      loading={resetMutation.isPending}
                      disabled={busy || room.status === 1}
                    >
                      重置密钥
                    </Button>
                  </Popconfirm>
                }
              >
                <Descriptions column={1} size="small" className="stream-desc">
                  <Descriptions.Item label="服务器">
                    <Text copyable>{stream.server}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="串流密钥">
                    <Text copyable code>
                      {stream.streamKey}
                    </Text>
                  </Descriptions.Item>
                </Descriptions>
                <Alert
                  type="warning"
                  showIcon
                  message="串流密钥等同于开播权限，请勿公开或截图分享。"
                  description="OBS → 设置 → 直播：服务选择「自定义」，服务器填上面的地址，串流密钥填下面的值；建议关键帧间隔设为 2 秒，WebRTC 首帧更快。"
                />
              </Card>
            ) : null}

            <Card variant="borderless" className="anchor-card" title="直播间信息">
              <Form<RoomSavePayload>
                form={editForm}
                layout="vertical"
                onFinish={(values) => updateMutation.mutate(values)}
              >
                <Form.Item
                  name="title"
                  label="标题"
                  rules={[
                    { required: true, message: '请输入标题' },
                    { max: 80, message: '标题最多 80 个字符' },
                  ]}
                >
                  <Input size="large" maxLength={80} showCount />
                </Form.Item>
                <Form.Item name="categoryId" label="分类">
                  <Select
                    size="large"
                    allowClear
                    placeholder="选择分类"
                    options={(categoriesQuery.data ?? []).map((category) => ({
                      value: category.id,
                      label: category.name,
                    }))}
                  />
                </Form.Item>
                <Form.Item name="cover" label="封面地址" rules={[{ max: 255, message: '最多 255 个字符' }]}>
                  <Input size="large" placeholder="https://..." />
                </Form.Item>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={updateMutation.isPending}>
                  保存修改
                </Button>
              </Form>
            </Card>
          </Space>
        )}
      </div>
    </div>
  )
}

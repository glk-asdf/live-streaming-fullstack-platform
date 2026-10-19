import { SaveOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Avatar, Button, Card, Form, Input, Space, Tag, Typography } from 'antd'
import { useEffect } from 'react'
import { fetchMe, updateMe } from '@/api/auth'
import SiteHeader from '@/components/SiteHeader'
import { useUserStore } from '@/store/useUserStore'
import type { UpdateProfilePayload } from '@/types/auth'

const { Title, Paragraph, Text } = Typography
const { TextArea } = Input

const roleLabels: Record<string, string> = {
  USER: '观众',
  ANCHOR: '主播',
  ADMIN: '管理员',
}

export default function ProfilePage() {
  const { message } = App.useApp()
  const [form] = Form.useForm<UpdateProfilePayload>()
  const queryClient = useQueryClient()
  const user = useUserStore((state) => state.user)
  const setUser = useUserStore((state) => state.setUser)

  const me = useQuery({
    // 带上用户 ID，切换账号后不会命中上一个账号的缓存
    queryKey: ['me', user?.id],
    queryFn: fetchMe,
    staleTime: 30_000,
  })

  useEffect(() => {
    if (!me.data) {
      return
    }
    setUser(me.data)
    form.setFieldsValue({
      nickname: me.data.nickname,
      avatar: me.data.avatar ?? '',
      bio: me.data.bio ?? '',
    })
  }, [me.data, form, setUser])

  const mutation = useMutation({
    mutationFn: updateMe,
    onSuccess: (data) => {
      setUser(data)
      queryClient.setQueryData(['me', data.id], data)
      form.setFieldsValue({ nickname: data.nickname, avatar: data.avatar ?? '', bio: data.bio ?? '' })
      message.success('资料已更新')
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '更新失败')
    },
  })

  return (
    <div className="page">
      <SiteHeader />
      <div className="profile-wrap">
        <Card className="profile-card" variant="borderless">
          <Space align="center" size={18}>
            <Avatar size={64} src={user?.avatar || undefined}>
              {user?.nickname?.slice(0, 1) ?? '?'}
            </Avatar>
            <div>
              <Title level={3} className="profile-name">
                {user?.nickname ?? '未登录'}
              </Title>
              <Space size={8} wrap>
                <Text type="secondary">@{user?.username ?? '-'}</Text>
                <Tag color="purple" bordered={false}>
                  {roleLabels[user?.role ?? 'USER'] ?? user?.role}
                </Tag>
                {user?.createdAt ? (
                  <Text type="secondary" className="profile-meta">
                    注册于 {user.createdAt}
                  </Text>
                ) : null}
              </Space>
            </div>
          </Space>

          <Paragraph className="profile-tip">
            头像暂用 URL，图片上传（MinIO）在 P4 接入。
          </Paragraph>

          <Form<UpdateProfilePayload>
            form={form}
            layout="vertical"
            initialValues={{
              nickname: user?.nickname ?? '',
              avatar: user?.avatar ?? '',
              bio: user?.bio ?? '',
            }}
            onFinish={(values) => mutation.mutate(values)}
          >
            <Form.Item
              name="nickname"
              label="昵称"
              rules={[
                { required: true, message: '请输入昵称' },
                { max: 20, message: '昵称最多 20 个字符' },
              ]}
            >
              <Input size="large" maxLength={20} showCount />
            </Form.Item>
            <Form.Item name="avatar" label="头像地址" rules={[{ max: 255, message: '最多 255 个字符' }]}>
              <Input size="large" placeholder="https://..." />
            </Form.Item>
            <Form.Item name="bio" label="个人简介" rules={[{ max: 255, message: '最多 255 个字符' }]}>
              <TextArea rows={3} maxLength={255} showCount placeholder="介绍一下自己" />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              icon={<SaveOutlined />}
              loading={mutation.isPending}
            >
              保存修改
            </Button>
          </Form>
        </Card>
      </div>
    </div>
  )
}

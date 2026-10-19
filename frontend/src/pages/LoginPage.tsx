import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { login } from '@/api/auth'
import SiteHeader from '@/components/SiteHeader'
import { useUserStore } from '@/store/useUserStore'
import type { LoginPayload } from '@/types/auth'

const { Title, Paragraph, Text } = Typography

export default function LoginPage() {
  const { message } = App.useApp()
  const setAuth = useUserStore((state) => state.setAuth)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: LoginPayload) => {
    setLoading(true)
    try {
      const auth = await login(values)
      setAuth(auth)
      message.success(`欢迎回来，${auth.user.nickname}`)
      const redirect = searchParams.get('redirect')
      const safeRedirect = redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
      navigate(safeRedirect, { replace: true })
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <SiteHeader />
      <div className="auth-wrap">
        <Card className="auth-card" variant="borderless">
          <Title level={2}>欢迎回来</Title>
          <Paragraph className="auth-sub">登录后可以发弹幕、送礼、管理自己的直播间。</Paragraph>
          <Form<LoginPayload> layout="vertical" onFinish={onFinish} requiredMark={false}>
            <Form.Item
              name="username"
              label="用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input size="large" prefix={<UserOutlined />} placeholder="tester" autoComplete="username" />
            </Form.Item>
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                size="large"
                prefix={<LockOutlined />}
                placeholder="123456"
                autoComplete="current-password"
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" block loading={loading}>
              登录
            </Button>
          </Form>
          <div className="auth-switch">
            还没有账号？<Link to="/register">立即注册</Link>
          </div>
          <div className="auth-hint">
            <Text type="secondary">测试账号 tester / 123456</Text>
          </div>
        </Card>
      </div>
    </div>
  )
}

import { LockOutlined, SmileOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '@/api/auth'
import SiteHeader from '@/components/SiteHeader'
import { useUserStore } from '@/store/useUserStore'
import type { RegisterPayload } from '@/types/auth'

const { Title, Paragraph } = Typography

interface RegisterForm extends RegisterPayload {
  confirmPassword: string
}

export default function RegisterPage() {
  const { message } = App.useApp()
  const setAuth = useUserStore((state) => state.setAuth)
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: RegisterForm) => {
    setLoading(true)
    try {
      const auth = await register({
        username: values.username,
        password: values.password,
        nickname: values.nickname,
      })
      setAuth(auth)
      message.success('注册成功，已自动登录')
      navigate('/', { replace: true })
    } catch (error) {
      message.error(error instanceof Error ? error.message : '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <SiteHeader />
      <div className="auth-wrap">
        <Card className="auth-card" variant="borderless">
          <Title level={2}>创建账号</Title>
          <Paragraph className="auth-sub">注册即可观看直播；创建直播间后会自动获得主播身份。</Paragraph>
          <Form<RegisterForm> layout="vertical" onFinish={onFinish} requiredMark={false}>
            <Form.Item
              name="username"
              label="用户名"
              rules={[
                { required: true, message: '请输入用户名' },
                {
                  pattern: /^[a-zA-Z0-9_]{4,20}$/,
                  message: '4~20 位字母、数字或下划线',
                },
              ]}
            >
              <Input size="large" prefix={<UserOutlined />} placeholder="4~20 位字母、数字或下划线" autoComplete="username" />
            </Form.Item>
            <Form.Item name="nickname" label="昵称（可选）" rules={[{ max: 20, message: '昵称最多 20 个字符' }]}>
              <Input size="large" prefix={<SmileOutlined />} placeholder="不填则使用用户名" />
            </Form.Item>
            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, max: 32, message: '密码长度需为 6~32 位' },
              ]}
            >
              <Input.Password
                size="large"
                prefix={<LockOutlined />}
                placeholder="6~32 位"
                autoComplete="new-password"
              />
            </Form.Item>
            <Form.Item
              name="confirmPassword"
              label="确认密码"
              dependencies={['password']}
              rules={[
                { required: true, message: '请再次输入密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('password') === value) {
                      return Promise.resolve()
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'))
                  },
                }),
              ]}
            >
              <Input.Password
                size="large"
                prefix={<LockOutlined />}
                placeholder="再次输入密码"
                autoComplete="new-password"
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" block loading={loading}>
              注册并登录
            </Button>
          </Form>
          <div className="auth-switch">
            已有账号？<Link to="/login">去登录</Link>
          </div>
        </Card>
      </div>
    </div>
  )
}

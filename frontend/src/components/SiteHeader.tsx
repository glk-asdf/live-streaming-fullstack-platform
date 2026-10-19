import { LogoutOutlined, UserOutlined, VideoCameraOutlined } from '@ant-design/icons'
import { Avatar, Button, Dropdown, Space } from 'antd'
import type { MenuProps } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '@/api/auth'
import { useUserStore } from '@/store/useUserStore'

export default function SiteHeader() {
  const user = useUserStore((state) => state.user)
  const refreshToken = useUserStore((state) => state.refreshToken)
  const clearAuth = useUserStore((state) => state.clearAuth)
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await logout(refreshToken)
    } catch {
      // 服务端失败不阻塞本地登出
    } finally {
      // 清空登录态会让受保护页面的 AuthGuard 立刻重定向到登录页；
      // 用整页跳转回到首页，既清掉内存状态，也避免与守卫发生竞态。
      clearAuth()
      window.location.replace('/')
    }
  }

  const menuItems: MenuProps['items'] = [
    { key: 'profile', label: '个人中心', icon: <UserOutlined />, onClick: () => navigate('/profile') },
    { type: 'divider' },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
      danger: true,
      onClick: () => {
        void handleLogout()
      },
    },
  ]

  return (
    <header className="site-header">
      <Link to="/" className="brand">
        <span className="brand-mark">
          <VideoCameraOutlined />
        </span>
        <span className="brand-name">LiveForge</span>
        <span className="brand-tag">直播全栈</span>
      </Link>
      <nav className="site-nav">
        <Link to="/">首页</Link>
        <Link to="/#roadmap">分档路线</Link>
        <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
          接口文档
        </a>
      </nav>
      <div className="header-actions">
        {user ? (
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <button type="button" className="user-chip">
              <Avatar size={28} src={user.avatar || undefined}>
                {user.nickname.slice(0, 1)}
              </Avatar>
              <span>{user.nickname}</span>
            </button>
          </Dropdown>
        ) : (
          <Space size={8}>
            <Button type="text" onClick={() => navigate('/login')}>
              登录
            </Button>
            <Button type="primary" onClick={() => navigate('/register')}>
              注册
            </Button>
          </Space>
        )}
      </div>
    </header>
  )
}

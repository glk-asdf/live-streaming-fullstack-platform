import {
  ApiOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  CloudServerOutlined,
  LoadingOutlined,
  PlayCircleOutlined,
  RocketOutlined,
  ThunderboltOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd'
import type { ReactNode } from 'react'
import { fetchMediaStatus, fetchPing } from '@/api/system'
import SiteHeader from '@/components/SiteHeader'

const { Title, Paragraph, Text } = Typography

interface StatusCardProps {
  icon: ReactNode
  title: string
  desc: string
  loading: boolean
  ok: boolean
  detail: string
}

function StatusCard({ icon, title, desc, loading, ok, detail }: StatusCardProps) {
  return (
    <Card className="status-card" variant="borderless">
      <Space align="start" size={14}>
        <span className={`status-icon ${ok ? 'is-ok' : loading ? 'is-loading' : 'is-down'}`}>
          {icon}
        </span>
        <div>
          <div className="status-head">
            <Text strong>{title}</Text>
            {loading ? (
              <Tag color="processing" bordered={false} icon={<LoadingOutlined spin />}>
                检测中
              </Tag>
            ) : ok ? (
              <Tag color="success" bordered={false} icon={<CheckCircleFilled />}>
                在线
              </Tag>
            ) : (
              <Tag color="error" bordered={false} icon={<CloseCircleFilled />}>
                未连接
              </Tag>
            )}
          </div>
          <Paragraph className="status-desc">{desc}</Paragraph>
          <Text type="secondary" className="status-detail">
            {detail}
          </Text>
        </div>
      </Space>
    </Card>
  )
}

interface TierPhase {
  name: string
  text: string
}

interface Tier {
  key: string
  label: string
  title: string
  goal: string
  status: '进行中' | '待开始'
  phases: TierPhase[]
}

const tiers: Tier[] = [
  {
    key: 'L1',
    label: 'L1 能推能看能聊',
    title: '打通直播主链路',
    goal: 'OBS 推流 → 浏览器低延迟播放 → 弹幕互通 → 关播下线',
    status: '进行中',
    phases: [
      { name: 'P0', text: '工程骨架与三端探活' },
      { name: 'P1', text: '数据模型与持久层' },
      { name: 'P2', text: '认证与用户' },
      { name: 'P3', text: '直播间与推流鉴权' },
      { name: 'P4', text: '播放页与观看体验' },
      { name: 'P5', text: '实时聊天与在线人数' },
    ],
  },
  {
    key: 'L2',
    label: 'L2 能互动能运营',
    title: '商业化与运营闭环',
    goal: '礼物打赏、热度榜、回放、主播看板、管理后台',
    status: '待开始',
    phases: [
      { name: 'P6', text: '礼物与虚拟币账本' },
      { name: 'P7', text: '热度榜 / 关注 / 搜索' },
      { name: 'P8', text: '录制与回放' },
      { name: 'P9', text: '管理后台与内容审核' },
      { name: 'P10', text: '主播中心与数据看板' },
    ],
  },
  {
    key: 'L3',
    label: 'L3 能上线能扩展',
    title: '生产化与容量',
    goal: '多码率、双实例、压测、可观测性、一键部署',
    status: '待开始',
    phases: [
      { name: 'P11', text: '多码率与低延迟增强' },
      { name: 'P12', text: '水平扩展与消息总线' },
      { name: 'P13', text: '安全风控与压测' },
      { name: 'P14', text: '可观测性与运维' },
      { name: 'P15', text: '容器化部署与 CI/CD' },
    ],
  },
]

const stack = [
  'React 19',
  'TypeScript 6',
  'Vite 8',
  'Ant Design 5',
  'Spring Boot 3.5',
  'Java 25',
  'MySQL 8.4',
  'Redis',
  'MinIO',
  'MediaMTX',
  'WebRTC / HLS',
  'STOMP',
]

export default function HomePage() {
  const ping = useQuery({
    queryKey: ['system', 'ping'],
    queryFn: fetchPing,
    refetchInterval: 15_000,
  })

  const media = useQuery({
    queryKey: ['system', 'media'],
    queryFn: fetchMediaStatus,
    refetchInterval: 15_000,
    retry: 1,
  })

  return (
    <div className="page">
      <SiteHeader />

      <section className="hero">
        <div className="hero-glow" />
        <Tag className="hero-badge" bordered={false} icon={<ThunderboltOutlined />}>
          第一档进行中 · P0~P2 已完成
        </Tag>
        <Title level={1} className="hero-title">
          从推流到弹幕，
          <br />
          一个人也能跑通的直播全栈
        </Title>
        <Paragraph className="hero-sub">
          RTMP 接入、WebRTC 低延迟播放、实时弹幕、礼物账本与录制回放。
          <br />
          三个档位、十六个阶段，每一阶段都能独立跑起来、独立验收。
        </Paragraph>
        <Space size={12} wrap>
          <Button type="primary" size="large" icon={<PlayCircleOutlined />} href="#roadmap">
            查看分档路线
          </Button>
          <Button size="large" icon={<ApiOutlined />} href="http://localhost:8080/swagger-ui.html" target="_blank">
            打开 Swagger
          </Button>
        </Space>
        <div className="hero-metrics">
          <div>
            <strong>3</strong>
            <span>交付档位</span>
          </div>
          <div>
            <strong>16</strong>
            <span>可验收阶段</span>
          </div>
          <div>
            <strong>&lt; 1s</strong>
            <span>WebRTC 延迟</span>
          </div>
          <div>
            <strong>0</strong>
            <span>Docker 依赖</span>
          </div>
        </div>
      </section>

      <section id="status" className="section">
        <div className="section-head">
          <Title level={3}>服务状态</Title>
          <Text type="secondary">每 15 秒自动探活，真实反映当前机器上的运行状态</Text>
        </div>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <StatusCard
              icon={<CloudServerOutlined />}
              title="后端 API"
              desc="Spring Boot · :8080"
              loading={ping.isLoading}
              ok={ping.isSuccess}
              detail={
                ping.isSuccess
                  ? `${ping.data.service} v${ping.data.version}`
                  : ping.isError
                    ? ping.error.message
                    : '正在连接 /api/v1/ping'
              }
            />
          </Col>
          <Col xs={24} md={8}>
            <StatusCard
              icon={<VideoCameraOutlined />}
              title="媒体服务器"
              desc="MediaMTX · RTMP 1935"
              loading={media.isLoading}
              ok={media.isSuccess}
              detail={
                media.isSuccess
                  ? `管理 API 正常 · 活跃流 ${media.data.activeStreams}`
                  : media.isError
                    ? '未启动，执行 media\\media-start.ps1'
                    : '正在连接 :9997'
              }
            />
          </Col>
          <Col xs={24} md={8}>
            <StatusCard
              icon={<RocketOutlined />}
              title="前端应用"
              desc="Vite · :5173"
              loading={false}
              ok
              detail="React 19 + Ant Design 5 已加载"
            />
          </Col>
        </Row>
      </section>

      <section id="roadmap" className="section">
        <div className="section-head">
          <Title level={3}>三档路线</Title>
          <Text type="secondary">L1 不通不碰 L2；每档结束打 tag，随时可回滚</Text>
        </div>
        <Row gutter={[16, 16]}>
          {tiers.map((tier) => (
            <Col xs={24} lg={8} key={tier.key}>
              <Card className={`tier-card ${tier.status === '进行中' ? 'is-active' : ''}`} variant="borderless">
                <div className="tier-head">
                  <span className="tier-key">{tier.key}</span>
                  <Tag color={tier.status === '进行中' ? 'processing' : 'default'} bordered={false}>
                    {tier.status}
                  </Tag>
                </div>
                <Title level={4}>{tier.label}</Title>
                <Paragraph className="tier-goal">{tier.goal}</Paragraph>
                <div className="phase-list">
                  {tier.phases.map((phase) => (
                    <div className="phase-row" key={phase.name}>
                      <span className="phase-name">{phase.name}</span>
                      <span className="phase-text">{phase.text}</span>
                    </div>
                  ))}
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </section>

      <section className="section">
        <div className="section-head">
          <Title level={3}>技术栈</Title>
          <Text type="secondary">与同级全栈项目保持一致，新增媒体层</Text>
        </div>
        <div className="stack-list">
          {stack.map((item) => (
            <Tag key={item} bordered={false} className="stack-tag">
              {item}
            </Tag>
          ))}
        </div>
      </section>

      <footer className="site-footer">
        <Text type="secondary">
          LiveForge · 分档方案见 doc/development-plan.md · 架构设计见 doc/architecture.md
        </Text>
      </footer>
    </div>
  )
}

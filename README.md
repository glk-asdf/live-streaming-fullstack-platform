# LiveForge · 直播全栈平台

前后端分离的直播平台：主播用 OBS 推流，观众在浏览器低延迟观看，支持实时弹幕、礼物打赏、录制回放、主播看板与管理后台。

项目按**三个档位、十六个阶段**推进，每一阶段都能独立运行与验收：

| 档位 | 定位 | 状态 |
|---|---|---|
| L1 能推能看能聊 | 打通「推流 → 播放 → 弹幕」主链路 | 🚧 进行中（P0~P2 已完成） |
| L2 能互动能运营 | 礼物、榜单、回放、后台、数据看板 | ⬜ 未开始 |
| L3 能上线能扩展 | 多码率、双实例、压测、可观测、一键部署 | ⬜ 未开始 |

- 架构设计：[`doc/architecture.md`](doc/architecture.md)
- 分档分步计划：[`doc/development-plan.md`](doc/development-plan.md)

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | React 19 + TypeScript 6 + Vite 8 + Ant Design 5 |
| 前端状态 | Zustand（客户端）+ TanStack Query（服务端） |
| 路由 | React Router 7 |
| 播放器 | WebRTC（WHEP）优先 + hls.js（LL-HLS）兜底 |
| 实时通信 | STOMP over WebSocket |
| 后端 | Spring Boot 3.5.16 + Java 25 |
| 安全 | Spring Security + JWT（access + refresh 轮换） |
| 持久层 | MyBatis-Plus 3.5.17 + MySQL 8.4 |
| 缓存 | Redis（refreshToken、在线人数、热度、限流） |
| 对象存储 | MinIO（封面、头像、回放归档） |
| 媒体服务器 | MediaMTX 1.21（RTMP 接入 / LL-HLS / WHEP / 录制） |
| 构建 | Maven Wrapper（无需本机安装 Maven）+ npm |

## 目录结构

```text
live-streaming-fullstack-platform/
├── backend/          # Spring Boot 后端
│   ├── mvnw / mvnw.cmd
│   └── src/main/java/com/example/live/
├── frontend/         # React + Vite 前端
├── media/            # MediaMTX 配置、启停脚本、二进制（gitignore）
├── sql/              # 建表与初始化数据
├── deploy/           # 部署编排与冒烟测试（L3 创建）
├── doc/              # 架构设计与分档计划
├── .env.example
└── README.md
```

## 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 25 | 本机已装 25.0.4.1 |
| Node.js | 22+ | 本机已装 22.21.0 |
| MySQL | 8.4.9 | `D:\business\tools\mysql-8.4.9-winx64`（已使用） |
| Redis | 5.0.14.1 | `D:\business\tools\redis-5.0.14.1`（已使用） |
| MinIO | 归档版 | `D:\business\tools\minio`（P4 起使用） |
| MediaMTX | 1.21.2 | 启动脚本自动下载到 `media/bin/` |

> 不需要 Docker。生产部署在 L3 才引入容器。
>
> `.env.example` 只是配置模板：本地开发直接用 `application.yml` 里的默认值，Spring Boot 不读取 `.env`；复制成 `.env` 是给 L3 的 Compose 用的。

## 快速开始（P2）

### 1. 启动 MySQL 并初始化数据库

```powershell
powershell -ExecutionPolicy Bypass -File D:\business\tools\mysql-start.ps1

$env:MYSQL_PWD = "123456"
& "D:\business\tools\mysql-8.4.9-winx64\bin\mysql.exe" -u root -h 127.0.0.1 `
  --default-character-set=utf8mb4 -e "source d:/business/ai-fullstack-platform/live-streaming-fullstack-platform/sql/init.sql"
Remove-Item Env:MYSQL_PWD
```

脚本可重复执行：建 `live` 库、5 张表、5 个分类，以及测试账号 `tester` / `123456`。

### 2. 启动 Redis

```powershell
powershell -ExecutionPolicy Bypass -File D:\business\tools\redis-start.ps1
```

refreshToken 存在 Redis，Redis 未启动时登录会失败。

### 3. 启动媒体服务器

```powershell
powershell -ExecutionPolicy Bypass -File media\media-start.ps1
```

首次运行会自动下载 MediaMTX，监听 1935 / 8888 / 8889 / 9997，以及 WebRTC 的 UDP 8189。

### 4. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- 探活：<http://localhost:8080/api/v1/ping>
- 分类：<http://localhost:8080/api/v1/categories>
- 房间：<http://localhost:8080/api/v1/rooms?page=1&size=12>
- 文档：<http://localhost:8080/swagger-ui.html>

### 5. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5173>，页面上的「后端 API」与「媒体服务器」状态卡应显示在线。

### 6. 构建与测试

```powershell
cd backend;  .\mvnw.cmd test; .\mvnw.cmd -DskipTests package
cd ..\frontend; npm run build
```

> 打包前先停止正在运行的后端：Windows 会锁定 `target` 下的 jar，导致 repackage 失败。

## 当前接口（P2）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/ping` | 服务探活 |
| GET | `/api/v1/categories` | 分类列表（按 sort 升序） |
| GET | `/api/v1/rooms` | 房间分页：`page`、`size`（1~50）、`categoryId`、`keyword` |
| POST | `/api/v1/auth/register` | 注册并自动登录 |
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/refresh` | 刷新令牌（refreshToken 轮换） |
| POST | `/api/v1/auth/logout` | 登出（删除服务端 refreshToken） |
| GET / PUT | `/api/v1/users/me` | 当前用户资料 / 更新资料 |

> 公开列表暂未按状态过滤，P3 定义开播状态流转后收紧。

## 认证说明

- accessToken：JWT，默认 30 分钟，放在 `Authorization: Bearer <token>`。
- refreshToken：256 位随机串，默认 7 天，存 Redis；每次刷新都会轮换，旧令牌立即失效。
- 登出删除 refreshToken；已签发的 accessToken 仍会在有效期内可用（无黑名单）。
- 前端把登录态存在 localStorage（学习项目方案，生产建议改 HttpOnly Cookie），刷新页面保持登录；accessToken 失效时自动用 refreshToken 换新并重放请求。
- 测试账号：`tester` / `123456`（角色 ANCHOR，可直接体验个人中心）。

## 端口表

| 端口 | 服务 | 说明 |
|---|---|---|
| 5173 | Vite | 前端开发服务器 |
| 8080 | Spring Boot | API + WebSocket |
| 1935 | MediaMTX | RTMP 推流 |
| 8888 | MediaMTX | LL-HLS 播放 |
| 8889 | MediaMTX | WebRTC（WHEP） |
| 8189 | MediaMTX | WebRTC ICE（UDP） |
| 9997 | MediaMTX | 管理 API（仅本机） |
| 9996 | MediaMTX | 录制回放（P8 启用，仅内网） |
| 3306 | MySQL | P1 起使用 |
| 6379 | Redis | refreshToken 等缓存 |
| 9000 / 9001 | MinIO | P4 起使用 |

## 媒体链路（P3 起启用鉴权）

```text
OBS 推流   rtmp://localhost:1935/live/{roomId}?key={streamKey}
LL-HLS 播放 http://localhost:8888/live/{roomId}/index.m3u8
WHEP 播放   http://localhost:8889/live/{roomId}/whep
```

推流与播放都会回调后端 `/api/v1/media/auth` 校验权限，密钥不会出现在播放地址中。

> 首页的「媒体服务器」状态卡在开发环境经 Vite 的 `/media-api` 代理读取 MediaMTX 管理 API；该代理只存在于 `vite.config.ts`，生产部署不会暴露 9997。

## 开发进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| P0 | 环境与工程骨架 | ✅ 已完成（2026-10-19） |
| P1 | 数据模型与持久层 | ✅ 已完成（2026-10-19） |
| P2 | 认证与用户 | ✅ 已完成（2026-10-19） |
| P3 | 直播间与推流鉴权 | ⬜ 待开始 |
| P4 | 播放页与观看体验 | ⬜ 待开始 |
| P5 | 实时聊天与在线人数 | ⬜ 待开始 |
| P6~P15 | L2 / L3 | ⬜ 待开始 |

任务拆解、验收标准与风险见 [`doc/development-plan.md`](doc/development-plan.md)。

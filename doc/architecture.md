# 直播全栈平台 · 架构设计

> 版本：v1.0（2026-10-19）
> 目标：在 Windows 本机零 Docker 依赖跑通「推流 → 播放 → 弹幕 → 互动 → 回放」，并可平滑升级到生产部署。
> 配套文档：分档分步计划见 `doc/development-plan.md`。

---

## 1. 定位与边界

**做什么**

- 主播：创建直播间、获取推流地址、OBS 开播/关播、查看实时数据、管理回放。
- 观众：浏览/搜索直播间、低延迟观看、实时弹幕、送礼物、关注主播。
- 管理员：用户与直播间管理、封禁/解封、举报处理、平台概览。

**首版不做（避免摊大饼）**

| 不做 | 原因 | 后续位置 |
|---|---|---|
| 真实支付 | 涉及资质与对账，用虚拟币账本替代 | 永不（学习项目） |
| 连麦 / PK | 需要 SFU 与信令，复杂度陡增 | L3 选做 |
| 推荐算法 | 冷启动无数据，先用热度排序 | L3 之后 |
| 多地域 CDN | 单机演示无意义 | 上线阶段预留 |
| 私信 / 动态 | 与直播主线无关 | 不做 |

---

## 2. 角色与核心用例

| 角色 | 核心用例 |
|---|---|
| 游客 | 看直播列表 / 进入直播间观看 / 注册登录 |
| 观众 | 发弹幕 / 送礼物 / 关注 / 看回放 / 举报 |
| 主播 | 申请开播 / 管理直播间 / 获取推流密钥 / 开播关播 / 看数据 |
| 管理员 | 封禁房间或用户 / 处理举报 / 查看平台统计 |

关键约束：

- 未登录可看直播，但不能发弹幕、送礼、关注。
- 任何登录用户都可以创建直播间并自动获得 `ANCHOR` 角色，不单独做主播审核（管理后台可直接封禁）。
- 一个用户同一时间只允许一个未结束的直播间（服务层事务内校验）。
- 推流密钥泄露等于直播间被劫持，必须可一键重置。

---

## 3. 总体架构

```text
                      ┌────────────────────── 浏览器（React 19 + Vite） ─────────────────────┐
                      │  列表 / 直播间 / 主播中心 / 管理后台                                  │
                      │  播放器：WHEP(WebRTC) 优先，hls.js(LL-HLS) 兜底                       │
                      └───────┬───────────────────────────────┬──────────────────────────────┘
                              │ HTTPS /api                     │ WS /ws (STOMP)
                              ▼                                ▼
                        ┌──────────────────────────────────────────┐
                        │  Spring Boot 3.5 (Java 25)  :8080        │
                        │  auth / room / chat / gift / replay /    │
                        │  admin / stats / media-callback          │
                        └───┬───────────┬───────────┬──────────────┘
                            │           │           │
                     MySQL 8.4      Redis 5      MinIO
                    （业务数据）   （在线/热度/令牌）（封面/头像/回放）

   主播 OBS ──RTMP:1935──▶ ┌──────────────────────────────┐ ──LL-HLS:8888 / WHEP:8889──▶ 观众
                           │  MediaMTX（单文件媒体服务器） │
                           │  RTMP 接入 / 转封装 / 录制    │
                           └──────────┬───────────────────┘
                                      │ HTTP 鉴权回调 + 事件回调 + 管理 API:9997
                                      ▼
                                  Spring Boot
```

| 组件 | 职责 | 端口 |
|---|---|---|
| MediaMTX | RTMP 接入、HLS/WebRTC 转封装、录制、鉴权回调 | 1935 / 8888 / 8889 / 9997 |
| Spring Boot | 业务、鉴权、房间状态机、聊天、礼物、对账 | 8080 |
| MySQL | 用户、房间、礼物、回放等持久数据 | 3306 |
| Redis | 播放令牌、在线观众、热度榜、聊天限流、最近弹幕 | 6379 |
| MinIO | 封面、头像、礼物图标、回放归档 | 9000 / 9001 |
| Nginx（仅部署） | 静态托管、API/WS/媒体反代、统一入口 | 80 |

---

## 4. 媒体链路

### 4.1 推流

```text
rtmp://localhost:1935/live/{roomId}?key={streamKey}
```

- `streamKey`：32 位随机串，仅主播本人可见，可重置；泄露后旧密钥立即失效。它放在 query 而不是 path，避免播放地址里泄露密钥。
- MediaMTX 收到 publish 时回调后端 `POST /api/v1/media/auth`，后端校验密钥、房间状态、封禁状态，通过才允许推流。
- 推流成功即把房间置为 `LIVE`；`runOnOffline`（MediaMTX 1.21 事件）回调后端置为 `ENDED`。定时对账任务兜底（防止进程被杀导致状态悬挂）。

### 4.2 播放

| 方案 | 协议 | 延迟 | 用途 |
|---|---|---|---|
| WebRTC | WHEP（HTTP POST SDP） | 300~800ms | 默认，互动直播体验最好 |
| LL-HLS | hls.js + fMP4 分片 | 1~3s | 兼容兜底、移动端弱网 |

播放地址不直接暴露：观众先调 `GET /api/v1/rooms/{id}/play-info`，后端校验房间状态并签发 5 分钟播放令牌，播放器带 `?token=` 访问媒体服务器，MediaMTX 再回调后端校验 `read` 权限。MediaMTX 会把 token 继续传播到 HLS 子播放列表、初始化和分片请求；WHEP 使用同一个 token。

- WHEP 首帧依赖关键帧，OBS/编码器建议 2 秒关键帧间隔。
- RTMP 的 AAC 音频不会被 MediaMTX 转为 Opus，WebRTC 当前仅含视频；HLS 保留音频。

### 4.3 关播与回放

```text
关播 → MediaMTX 停止录制 → runOnOffline 回调 → 房间 ENDED
     → 登记 t_replay（路径/时长/大小）→ 生成封面 → 观众可看回放
```

L2 直接使用 MediaMTX 的录制列表能力（playback 9996）；L3 把录制文件转封装后归档到 MinIO，媒体服务器只负责实时链路。

---

## 5. 技术选型

| 层 | 选型 | 理由 | 备选 |
|---|---|---|---|
| 前端 | React 19 + TypeScript + Vite 8 + Ant Design 5 | 与同级项目一致，生态成熟 | Vue 3 |
| 前端状态 | Zustand（客户端）+ TanStack Query（服务端） | 职责清晰，避免全局 store 塞请求缓存 | Redux Toolkit |
| 路由 | React Router 7 | 数据路由 + 懒加载 | TanStack Router |
| 播放器 | hls.js + 原生 WHEP | 无重型依赖，可控性强 | video.js / flv.js |
| 实时通信 | STOMP over WebSocket | 订阅模型天然匹配房间广播；Spring 原生支持 | 原生 WS + 自定义协议 |
| 后端 | Spring Boot 3.5.16 + Java 25 | 与同级项目一致，JDK 25 LTS | Node/NestJS |
| 持久层 | MyBatis-Plus 3.5.17 + MySQL 8.4 | 分页、逻辑删除、自动填充开箱即用 | JPA |
| 认证 | Spring Security + JWT（access + refresh 轮换） | 无状态、易扩展 | Session |
| 缓存 | Redis | 在线观众、热度、令牌、限流 | Caffeine（单机） |
| 对象存储 | MinIO | 本地 S3 兼容，后续可换 OSS | 本地磁盘 |
| 媒体服务器 | **MediaMTX 1.21** | Windows 单文件、零依赖；RTMP 接入 + LL-HLS + WHEP + HTTP 鉴权 + 录制 | SRS / node-media-server |

### 5.1 为什么是 MediaMTX

| 维度 | MediaMTX | SRS | node-media-server |
|---|---|---|---|
| Windows 原生运行 | ✅ 单 exe | ❌ 需 Docker/WSL | ✅ Node |
| 推流接入 | RTMP / SRT / RTSP | RTMP / SRT / WebRTC | RTMP |
| 低延迟播放 | WHEP <1s、LL-HLS 1~3s | HTTP-FLV 1~3s、WebRTC <1s | HTTP-FLV 1~3s |
| 鉴权回调 | ✅ HTTP（publish/read/api） | ✅ HTTP hooks | ✅ onPublish/onPlay |
| 录制 | ✅ fMP4 + 回放列表 | ✅ DVR | ✅ flv |
| 转码/多码率 | ❌ 需外挂 ffmpeg | ✅ 内置转码 | ❌ |
| 运维复杂度 | 极低 | 中（Docker） | 低 |

结论：**本机开发与首版上线用 MediaMTX**；需要内置转码、HTTP-FLV 或大规模集群时切 SRS（业务层只依赖「鉴权回调 + 播放地址」两件事，替换成本可控）。

---

## 6. 模块划分

### 6.1 后端（`com.example.live`）

```text
common/       Result / PageResult / ResultCode / BizException / GlobalExceptionHandler
config/       Security / MyBatisPlus / Redis / Minio / WebSocket / OpenAPI / MediaProperties
security/     JwtFilter / LoginUser / EntryPoint / AccessDeniedHandler
controller/   Auth / User / Room / Media / Chat / Gift / Wallet / Rank / Replay / Admin / Stats
service/      接口 + impl（事务边界在这一层）
mapper/       MyBatis-Plus Mapper
entity/       User / Room / Category / Follow / ChatMessage / Gift / GiftRecord / Wallet / Replay / Report
dto/ vo/      入参 / 出参
converter/    实体 → VO
job/          在线人数聚合、热度重算、房间状态对账、回放处理
ws/           STOMP 入站/出站、会话管理、限流
```

### 6.2 前端（`src/`）

```text
api/          request（拦截器/刷新令牌）/ auth / room / chat / gift / replay / admin
types/        分页、房间、消息、礼物的类型定义
store/        useUserStore（登录态）
hooks/        useRoomList / useRoomDetail / useLiveSocket / usePlayer
components/   LiveCard / Player（WHEP+HLS）/ Danmaku / ChatPanel / GiftPanel / StreamKeyCard
pages/        Home / Room / Login / Register / AnchorCenter / Admin / NotFound
layouts/      MainLayout（顶部导航）/ RoomLayout（播放器 + 侧栏）
```

### 6.3 媒体与部署

```text
media/        mediamtx.yml / media-start.ps1 / media-stop.ps1 / notify.ps1 / bin（gitignore）
deploy/       nginx.conf / docker-compose.yml / smoke-test.ps1
sql/          init.sql（按阶段增量追加）
doc/          architecture.md / development-plan.md
```

---

## 7. 数据模型

### 7.1 实体关系

```text
t_user 1 ──< t_live_room >── 1 t_category
  │              │
  │              ├──< t_chat_message
  │              ├──< t_gift_record >── t_gift
  │              ├──< t_replay
  │              └──< t_report
  ├──< t_follow（user → anchor）
  ├── 1 t_wallet ──< t_wallet_txn
  └──< t_ban_record
```

### 7.2 核心表（首版字段，后续阶段增量加列）

**t_user**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 雪花/自增 |
| username | VARCHAR(32) UK | 登录名 |
| password | VARCHAR(100) | BCrypt |
| nickname | VARCHAR(32) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| role | VARCHAR(16) | USER / ANCHOR / ADMIN |
| status | TINYINT | 0 正常 / 1 禁言 / 2 封禁 |
| created_at / updated_at / deleted | | 审计与逻辑删除 |

**t_live_room**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| anchor_id | BIGINT | 主播 |
| title / cover | VARCHAR | 标题、封面 |
| category_id | BIGINT | 分类 |
| stream_key | VARCHAR(64) UK | 推流密钥 |
| status | TINYINT | 0 未开播 / 1 直播中 / 2 已结束 / 3 封禁 |
| live_start_time / live_end_time | DATETIME | |
| viewer_count / peak_viewer | INT | 实时/峰值在线 |
| like_count | BIGINT | 点赞 |
| hot_score | INT | 热度（定时重算） |
| replay_status | TINYINT | 0 无 / 1 处理中 / 2 可用 |
| created_at / updated_at / deleted | | |

**t_chat_message**：`id, room_id, user_id, content, type(0 文本/1 礼物/2 系统), created_at`，索引 `(room_id, id)`。

**t_gift / t_gift_record**：礼物定义（名称/图标/价格）；记录含 `request_id UK` 做幂等，`amount` 存下单时单价快照。

**t_wallet / t_wallet_txn**：余额 + 版本号；流水记录每笔变动，`biz_id` 唯一防止重复扣款。

**t_follow / t_replay / t_report / t_ban_record**：按阶段追加到 `sql/init.sql`（P1 只建前五张核心表）。

### 7.3 Redis 键设计

| Key | 类型 | 用途 | TTL |
|---|---|---|---|
| `live:viewers:{roomId}` | ZSET | member=WebSocket sessionId，score=最后心跳时间 | 2h |
| `live:chat:{roomId}` | List | 最近 100 条弹幕 JSON | 6h |
| `chat:rate:{userId}` | String | 发弹幕限流 | 1s |
| `auth:refresh:{refreshToken}` | String | 值为 userId；刷新时 Lua 原子取出并删除 | 7d |
| `live:hot` | ZSET | 热度榜（P7） | 常驻 |
| `live:stream:{streamKey}` | String | 密钥 → 房间 ID 缓存（P12） | 10min |

---

## 8. 接口与实时协议

### 8.1 统一约定

- 前缀 `/api/v1`，响应 `{ code, message, data }`，`code = 0` 成功。
- 认证：`Authorization: Bearer <accessToken>`；access 30 分钟，refresh 7 天并轮换。
- 分页：`page`（从 1 开始）、`size`（默认 12，上限 50）；返回 `{ list, total, page, size }`。
- 时间：统一 `yyyy-MM-dd HH:mm:ss`（`Asia/Shanghai`），日期时间字段使用同一格式。

### 8.2 REST 接口清单

| 域 | 方法与路径 | 说明 | 阶段 |
|---|---|---|---|
| 认证 | `POST /auth/register` `POST /auth/login` `POST /auth/refresh` `POST /auth/logout` | 注册登录刷新登出 | P2 |
| 用户 | `GET /users/me` `PUT /users/me` | 当前用户资料 | P2 |
| 房间 | `GET /rooms` | 分页 + 分类 + 关键词 + 状态（默认 LIVE） | P4 |
| 房间 | `GET /rooms/{id}` | 详情（主播信息 P4 补，关注态 P7 补） | P3 |
| 房间 | `POST /rooms` `PUT /rooms/{id}` | 创建/编辑（主播） | P3 |
| 房间 | `POST /rooms/{id}/start` `POST /rooms/{id}/stop` | 开播（轮换密钥）/ 关播 | P3 |
| 房间 | `GET /rooms/mine` | 我的直播间（无房间时 data 为空） | P3 |
| 房间 | `GET /rooms/{id}/stream` | 推流地址 + 密钥（仅主播） | P3 |
| 房间 | `POST /rooms/{id}/stream-key/reset` | 重置推流密钥 | P3 |
| 媒体 | `POST /media/auth` | MediaMTX 鉴权回调 | P3 |
| 媒体 | `POST /media/online` `POST /media/offline` | 推流开始/结束事件（对应 MediaMTX `runOnOnline/runOnOffline`） | P3 |
| 媒体 | `GET /rooms/{id}/play-info` | 播放地址 + 播放令牌 | P4 |
| 聊天 | `GET /rooms/{id}/messages` | 历史弹幕 | P5 |
| 礼物 | `GET /gifts` `POST /rooms/{id}/gifts` | 礼物列表 / 送礼 | P6 |
| 钱包 | `GET /wallet` `GET /wallet/txns` | 余额与流水 | P6 |
| 榜单 | `GET /rank/hot` `GET /rank/gift` | 热门/贡献榜 | P7 |
| 关注 | `POST /anchors/{id}/follow` `DELETE ...` | 关注/取关 | P7 |
| 回放 | `GET /rooms/{id}/replays` `GET /replays/{id}` | 回放列表/详情 | P8 |
| 管理 | `GET /admin/overview` `GET /admin/rooms` `POST /admin/rooms/{id}/ban` `GET /admin/reports` | 后台 | P9 |
| 统计 | `GET /anchor/overview` `GET /anchor/trend` | 主播数据 | P10 |

### 8.3 WebSocket（STOMP）

- 端点：`/ws`（原生 WebSocket，不用 SockJS）；Vite 开发态代理 `/ws`，Nginx 部署需 `Upgrade`。
- 认证：浏览器无法给 WebSocket 握手加自定义 HTTP 头，因此 token 放在 **STOMP CONNECT 帧的 `Authorization` native header**，`StompAuthChannelInterceptor` 校验并写入 Principal；`/ws` 在 SecurityConfig 放行。token 只在 CONNECT 时校验，长连接内不重复校验。
- 订阅：`/topic/rooms/{roomId}`；游客可订阅。
- 发送：`/app/rooms/{roomId}/chat` 仅登录用户；`/app/rooms/{roomId}/heartbeat` 游客也允许。
- 错误：`@MessageExceptionHandler` 发到 `/user/queue/errors`。
- 下行消息统一信封：

```json
{ "type": "CHAT | VIEWER_COUNT | ROOM_END | ERROR", "data": {}, "ts": 1790000000000 }
```

- 限流：同一用户 1 秒 1 条弹幕、内容 ≤ 100 字、简单敏感词；服务端文本返回，前端禁止 `dangerouslySetInnerHTML`。

---

## 9. 关键机制

### 9.1 推流鉴权

```text
OBS --publish(live/{roomId}?key={key})--> MediaMTX --POST /media/auth--> Backend
                                                  ├─ 查 MySQL（P3 直查；Redis 缓存留 P12）
                                                  ├─ 校验密钥、房间状态、房主封禁
                                                  └─ 200 放行 / 401 拒绝
```

- 密钥在「开播/重置」时轮换，仅房主可读取；数据库存明文仅用于校验（学习项目可接受；生产建议存哈希）。
- 关闭房间立即置 `ENDED` 并调用 MediaMTX API 踢流；封禁时的缓存清理与强制下播在 P9 接入。
- 后端自身调用 MediaMTX 管理 API（对账、踢流）属于 `api` 动作，需在 `authHTTPExclude` 中排除 `api/metrics/pprof`，或只允许 `127.0.0.1`，避免鉴权回调自锁。
- 回调接口 `/media/auth|online|offline` 不携带 JWT，使用共享 token（查询参数）校验；本地开发允许回环地址兜底，生产必须显式配置 token。

### 9.2 在线人数

- 订阅房间时把 sessionId 写入 `live:viewers:{roomId}`，score = 最后心跳时间戳。
- 客户端每 15s 发心跳续期；定时任务每 10s 清理超过 30s 未心跳的成员并广播人数。
- 房间结束后删除集合并广播 0；峰值由房间状态服务写入 `t_live_room.peak_viewer`。

### 9.3 热度

```text
hot = 在线人数 × 3 + 近 10 分钟弹幕数 × 1 + 近 10 分钟礼物金额 / 10
```

- 每 30s 重算一次写入 `t_live_room.hot_score` 与 Redis ZSET；列表默认按热度倒序，避免每次实时聚合。

### 9.4 礼物与钱包

- 扣款：`UPDATE t_wallet SET balance = balance - ? WHERE user_id = ? AND balance >= ?`，影响行数为 0 即余额不足，避免超卖。
- 同一事务写 `t_gift_record` 与 `t_wallet_txn`；`request_id` 唯一索引保证重试不重复扣款。
- 送礼成功后通过 WebSocket 广播礼物消息，并异步累加房间与主播收益。

### 9.5 聊天风控

- P5 已实现：Redis 1 秒限流、长度 ≤ 100、简单敏感词、status=1 禁言/status=2 封禁拦截；刷屏检测与 DFA 词库留 P13。
- 被禁言用户只能读不能发；P5 对封禁用户的发送直接拒绝，管理端强制断开其 WebSocket 会话在 P9 接入。

### 9.6 状态对账

- 每 10s 调 MediaMTX API 拉取活跃 path，与数据库中 `IDLE/LIVE` 房间比对：
  - 有流但房间 IDLE → 修正为 LIVE；
  - LIVE 无流且开播已超过 15s → 置 ENDED（宽限避免刚开播被误杀）。
- 这是防「进程被杀导致房间永远直播中」的最后一道保险；回放登记在 P8 接入。

---

## 10. 安全设计

| 风险 | 措施 |
|---|---|
| 推流地址泄露 | 密钥随机 + 可重置 + 开播轮换 + 封禁即踢流 |
| 播放地址盗链 | 5 分钟播放令牌 + 媒体服务器 read 鉴权 |
| 越权操作房间 | 所有写操作校验 `anchor_id == 当前用户` |
| 媒体事件伪造 | 共享 token + 回环地址校验；生产替换默认 token |
| 头像上传 | 5MB 上限 + MIME 白名单 + 随机对象名；公开桶只读 |
| XSS（弹幕） | 服务端转义 + 前端文本渲染 + CSP |
| 刷弹幕/刷礼物 | Redis 限流 + 幂等键 + 异常行为封禁 |
| WebSocket 冒充/刷屏 | CONNECT 帧 JWT + 游客只读 + 1 秒限流 + 文本渲染 |
| 密钥入库 | `.env` 不入库；本地有开发默认值，生产必须用环境变量，Compose 缺失即启动失败（P15） |
| 管理接口 | `/admin/**` 强制 `ROLE_ADMIN`，操作留痕 |

---

## 11. 部署拓扑

**本地开发（默认）**

```text
MySQL 3306 / Redis 6379 / MinIO 9000+9001（D:\business\tools 绿色版）
MediaMTX 1935(TCP) + 8888(TCP) + 8889(TCP) + 8189(UDP) + 9997(仅本机)（media/bin/mediamtx.exe）
Backend 8080（mvnw spring-boot:run）
Frontend 5173（vite，/api、/ws 代理到 8080；/media-api 仅开发环境代理到 9997）
```

**单机部署（L3）**

```text
Nginx :80 ─┬─ /            前端静态资源
           ├─ /api          → backend:8080
           ├─ /ws           → backend:8080（Upgrade）
           ├─ /hls/         → mediamtx:8888
           └─ /webrtc/      → mediamtx:8889
媒体：1935 推流对公网开放，8189/UDP 供 WebRTC ICE；8889 可经 Nginx 反代
```

对外至少暴露 `80`（HTTP/WS）、`1935`（RTMP）；WebRTC 还需要 `8189/UDP`（或 TURN）。MySQL/Redis/MinIO/管理 API 端口仅内网。
后端下发播放地址时使用 `live.media.hls-base` / `webrtc-base` 配置，部署时改为同源 `/hls`、`/webrtc`，避免前端写死端口。

**扩展路径**

1. 单机 → 前后端分离部署 + 媒体服务器独立机器；
2. 多实例后端 → WebSocket 广播走 Redis Pub/Sub，房间状态共享 Redis；
3. 观众规模上来 → 媒体层换 SRS 集群 / 云直播，业务层只改播放地址下发。

---

## 12. 目录结构

```text
live-streaming-fullstack-platform/
├── backend/                 # Spring Boot
├── frontend/                # React + Vite
├── media/                   # MediaMTX 配置与启停脚本
├── sql/                     # 建表与初始化数据
├── doc/                     # 架构设计与分档计划
├── deploy/                  # 部署编排与冒烟测试（L3 创建）
├── .env.example
├── .gitignore
└── README.md
```

---

## 13. 风险与取舍

| 风险 | 影响 | 应对 |
|---|---|---|
| MediaMTX 无内置转码 | 无法多码率 | L3 外挂 ffmpeg 转码推多路；或换 SRS |
| WebRTC 公网连通性 | NAT 下播放失败 | 配置公网 IP/TURN；HLS 自动兜底 |
| 单机媒体服务器 | 并发上限低 | 明确为学习/演示规模；扩展路径见上 |
| Windows 端口占用 | 1935/8888 起不来 | 启停脚本先检测端口并给出提示 |
| 录制文件膨胀 | 磁盘占满 | 定时清理 + 保留最近 N 场 + 归档 MinIO |
| 弹幕刷屏 | 体验与安全问题 | 限流 + 敏感词 + 封禁联动 |
| 状态不一致 | 房间悬挂直播中 | 事件回调 + 定时对账双保险 |

---

## 14. 验收基线

- 单场直播从开播到关播全链路可复现，异常中断能在 60s 内自愈状态。
- 观众端首帧 ≤ 3s（HLS）/ ≤ 1s（WebRTC，本机）。
- 弹幕端到端延迟 ≤ 300ms（本机），100 并发连接不丢消息。
- 送礼并发 50 次不超扣、不重复扣。
- 一键启动脚本可用，README 能让陌生人 10 分钟内跑起来。

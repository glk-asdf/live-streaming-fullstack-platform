# 直播全栈平台 · 分档分步开发方案

> 版本：v1.0（2026-10-19）
> 对应架构：`doc/architecture.md`
> 目标：把项目拆成**三个可独立演示的档位**，每档内再拆成**可验收的小阶段**，每阶段结束都能跑起来、能演示、能回滚。
> 进度回填：每个阶段完成后在本文件与 README 中同步状态。

---

## 0. 使用方法

1. **档位递进**：L1 不通不碰 L2。每档结束打 Git tag（`v1.0-live` / `v2.0-interact` / `v3.0-prod`）。
2. **阶段纵向切片**：每个阶段交付一条端到端能力，而不是「先写完所有实体」。例如 P3 只做「开播鉴权」，播放页留到 P4。
3. **每阶段五件套**：目标 / 任务 / 交付物 / 验收（DoD）/ 风险。
4. **验收必须可执行**：DoD 写成命令或可点击路径，不写「功能正常」。
5. **时间估算**：1 人全职、每天 4~6 小时有效编码；只作相对参考。

### 档位总览

| 档位 | 定位 | 演示效果 | 阶段 | 预估 | 状态 |
|---|---|---|---|---|---|
| **L1 能推能看能聊** | MVP，打通直播主链路 | OBS 推流 → 浏览器 3s 内看到 → 弹幕互通 → 关播下线 | P0~P5 | 8~9 天 | 🚧 进行中 |
| **L2 能互动能运营** | 有商业化与运营闭环 | 送礼上榜、录制回放、主播看数据、管理员封禁 | P6~P10 | 9~11 天 | ⬜ 未开始 |
| **L3 能上线能扩展** | 生产化 | 多码率、低延迟、压测报告、Docker 一键部署 | P11~P15 | 8~10 天 | ⬜ 未开始 |

### 阶段总览

| 阶段 | 名称 | 档位 | 依赖 | 预估 | 里程碑 | 状态 |
|---|---|---|---|---|---|---|
| P0 | 环境与工程骨架 | L1 | - | 0.5 天 | 前后端 + 媒体服务器各自可启动，链路探活通过 | ✅ 已完成（2026-10-19） |
| P1 | 数据模型与持久层 | L1 | P0 | 1 天 | 建表完成，分页/逻辑删除/自动填充生效 | ✅ 已完成（2026-10-19） |
| P2 | 认证与用户 | L1 | P1 | 1.5 天 | 注册登录刷新登出闭环 | ✅ 已完成（2026-10-19） |
| P3 | 直播间与推流鉴权 | L1 | P2 | 2 天 | OBS 用密钥推流成功，未授权推流被拒 | ⬜ |
| P4 | 播放页与观看体验 | L1 | P3 | 1.5 天 | 浏览器 WHEP/HLS 可播放，列表可进入 | ⬜ |
| P5 | 实时聊天与在线人数 | L1 | P4 | 1.5 天 | 弹幕实时互通，在线人数准确 | ⬜ |
| P6 | 礼物与虚拟币账本 | L2 | P5 | 2 天 | 送礼扣款幂等，礼物广播与榜单可用 | ⬜ |
| P7 | 热度榜 / 关注 / 搜索 | L2 | P6 | 1.5 天 | 首页按热度排序，关注流可用 | ⬜ |
| P8 | 录制与回放 | L2 | P5 | 2 天 | 关播自动生成回放并可播放 | ⬜ |
| P9 | 管理后台与内容审核 | L2 | P6 | 2 天 | 管理员可封禁房间/用户、处理举报 | ⬜ |
| P10 | 主播中心与数据看板 | L2 | P7 | 1.5 天 | 主播可看场次/峰值/收益趋势 | ⬜ |
| P11 | 多码率与低延迟增强 | L3 | P10 | 2.5 天 | 720p/480p 自适应，WebRTC 稳定 | ⬜ |
| P12 | 水平扩展与消息总线 | L3 | P11 | 2 天 | 双后端实例下聊天与在线人数一致 | ⬜ |
| P13 | 安全风控与压测 | L3 | P12 | 1.5 天 | 限流/防刷生效，压测报告产出 | ⬜ |
| P14 | 可观测性与运维 | L3 | P13 | 1 天 | 指标/日志/告警/备份就位 | ⬜ |
| P15 | 容器化部署与 CI/CD | L3 | P14 | 1.5 天 | 单机一键部署 + 冒烟测试通过 | ⬜ |

> **关键路径**：P0 → P1 → P2 → P3 → P4 → P5。P3 是全项目最大风险点（媒体服务器与业务打通），打通后其余功能基本是同一套 CRUD + WebSocket 模式。
> **可并行**：P8（回放）与 P6/P7 可并行；P9 与 P10 可并行。

---

# 第一档 · L1 能推能看能聊（P0~P5）

> 档位验收：陌生人按 README 启动环境，用 OBS 推流，另一位用户在浏览器进入直播间看到画面并互发弹幕；关播后房间从列表消失。

## P0 · 环境与工程骨架（0.5 天）✅

### 目标

建立可运行的三端空壳：后端 `/api/v1/ping`、前端首页、MediaMTX 媒体服务，并打通前端代理链路。

### 任务清单

**仓库**
- [x] 建立 `backend/ frontend/ media/ sql/ doc/ deploy/` 目录
- [x] `.gitignore`（含 `media/bin`、`media/recordings`、`target`、`node_modules`、`.env`）
- [x] `.env.example`（数据库、Redis、MinIO、JWT、媒体服务器地址）
- [x] `README.md`（技术栈、启动步骤、端口表）

**后端**
- [x] Maven Wrapper（复用同级项目的 3.9.x wrapper，本机无需装 Maven）
- [x] `pom.xml`：Spring Boot 3.5.16 + Java 25 + Web + Validation + SpringDoc（安全/持久层按阶段引入）
- [x] 包结构：`common/config/controller`
- [x] `Result` / `ResultCode` / `BizException` / `GlobalExceptionHandler`
- [x] `GET /api/v1/ping` 返回统一响应
- [x] Swagger UI 可访问

**前端**
- [x] Vite 8 + React 19 + TypeScript 6 + Ant Design 5
- [x] 路径别名 `@ → src`、`/api` 代理到 `:8080`
- [x] 深色主题 + 首页骨架（平台介绍 + 三档路线 + 服务状态探活）
- [x] Axios 拦截器骨架（统一解包、错误提示）

**媒体**
- [x] 下载 MediaMTX Windows 版到 `media/bin/`（gitignore）
- [x] `media/mediamtx.yml`：RTMP 1935 / HLS 8888 / WebRTC 8889 / API 9997
- [x] `media/media-start.ps1` / `media-stop.ps1`（幂等、端口预检、API 探活）
- [x] `media/README.md`：OBS 推流参数与播放地址

### 交付物

- 三端可启动，首页能显示后端与媒体服务器状态。
- `sql/` 与 `deploy/` 目录占位（P1/L3 填充）。

### 验收（DoD）

```powershell
# 1. 后端
cd backend; .\mvnw.cmd spring-boot:run
# 另开窗口：
curl http://localhost:8080/api/v1/ping           # {"code":0,"message":"ok","data":...}

# 2. 媒体服务器
powershell -ExecutionPolicy Bypass -File media\media-start.ps1
curl http://127.0.0.1:9997/v3/config/global/get  # 返回 JSON，200

# 3. 前端
cd frontend; npm run dev
# 打开 http://localhost:5173 ，两个状态卡片均为绿色
```

1. `npm run build` 与 `mvnw -DskipTests package` 均通过。
2. 首页无控制台报错，接口经 Vite 代理返回 `code=0`。
3. 媒体服务器停止后首页状态卡变红（验证探活真实有效）。

### 风险

- 端口 8080/1935/8888 被占用 → 启动脚本先检测，README 给出改端口方法。
- 首次 Maven 依赖下载慢 → 配置国内镜像。
- MediaMTX 配置键随版本变化 → 以启动日志为准，只保留稳定键。

### 完成记录（2026-10-19）✅

**实际落地**

| 项 | 结果 |
|---|---|
| 后端 | Spring Boot 3.5.16 + Java 25 + SpringDoc 2.9.1，`/api/v1/ping` 返回统一响应 |
| 前端 | React 19 + TS 6 + Vite 8.3.4 + AntD 5，深色首页 + 服务状态探活 + 三档路线 |
| 媒体 | MediaMTX v1.21.2，RTMP 1935 / LL-HLS 8888 / WHEP 8889 / ICE 8189(UDP) / API 9997，MoQ 已关闭 |
| 构建 | `mvnw -DskipTests package` 通过；`npm run build` 通过（853 KB 主包，P4 起按路由拆包） |
| 链路 | `localhost:5173` 页面 200；`/api/v1/ping` 经代理返回 `code=0`；`/media-api/v3/paths/list` 返回 `itemCount=0`；Swagger UI 200 |

**关键决策**

- 媒体服务器选 MediaMTX 而非 SRS：本机 Docker 守护进程不可用，MediaMTX 是 Windows 单文件、零依赖，且同时提供 LL-HLS 与 WHEP。
- 推流地址改为 `live/{roomId}?key={streamKey}`：密钥放 query，避免播放地址泄露密钥（初版设计曾把密钥放 path）。
- MediaMTX 1.21 的路径事件是 `runOnOnline/runOnOffline`（旧版为 `runOnReady/runOnNotReady`），P3 按新版配置。
- 前端开发态通过 Vite 代理 `/media-api` 读取 MediaMTX 管理 API，仅用于探活，不进入生产代码路径。

**遗留**

- 主包 853 KB（AntD 全量 + 首页同步加载），P4 引入路由懒加载后拆包。
- 已加 `MediaProperties`（`@ConfigurationProperties(prefix = "live.media")`）并验证启动绑定正常，但 IDE 仍提示 unknown property，属配置元数据索引问题，不影响运行。
- 本机未安装 ffmpeg，P3 前用 OBS 或临时下载 ffmpeg 做推流联调。
- 仓库尚未初始化 Git；P1 前需 `git init` 并提交基线，否则「每阶段打 tag」无法执行。
- Spring Boot 3.5.x 的 OSS 支持已于 2026-06-30 结束（IDE 会提示）；当前与同级项目统一锁 3.5.16，L3 前评估升级到 4.x 或商业支持。

### P0 复查记录（2026-10-19）

复查文档与代码后修复：

| 问题 | 影响 | 处理 |
|---|---|---|
| 未匹配路径 / 错误方法被通用异常处理器兜成 500 | 与「统一响应体 + HTTP 语义」约定冲突，前端无法区分 404/405 | 增加 `NoResourceFoundException`/`NoHandlerFoundException` → 404、`HttpRequestMethodNotSupportedException` → 405，补 400 反序列化错误 |
| MediaMTX 默认开启 MoQ | 额外监听 8892/8893 并生成自签证书，端口表未记录 | 配置 `moq: no`，删除生成的证书，`.gitignore` 补 `*.crt`/`*.key` |
| 时间格式不统一 | `spring.jackson.date-format` 不作用于 JSR-310，`LocalDateTime` 仍输出 ISO 的 `T` | 新增 `JacksonConfig` 统一 `LocalDateTime` 的序列化/反序列化格式 |
| 媒体地址配置无绑定类 | 配置写错只能在运行时发现，且 IDE 提示 unknown property | 新增 `MediaProperties` + `@ConfigurationPropertiesScan`，启动即校验绑定 |
| 状态卡加载态用错误图标 | 视觉误导 | 加载态改用 `LoadingOutlined spin` |
| WebSocket 认证写成 HTTP `Authorization` 头 | 浏览器 WebSocket 无法设置自定义头，方案不可实现 | 文档改为 STOMP CONNECT 帧 native header，`/ws` 放行 + ChannelInterceptor 校验 |
| 部署只暴露 80/1935 | WebRTC 实际还需要 8889/TCP 与 8189/UDP，文档会误导部署 | 端口表与部署拓扑补齐 |
| 未定义主播角色来源与多房间约束 | P3 实现时口径不明 | 明确「登录用户创建房间即 ANCHOR」「同一主播仅一个未结束房间」，DoD 加 409 用例 |

---

## P1 · 数据模型与持久层（1 天）✅

### 目标

用一套可重复执行的 SQL 建库建表，后端接入 MyBatis-Plus，分页、逻辑删除、审计字段自动填充全部生效。

### 任务清单

- [x] `sql/init.sql`：`t_user / t_category / t_live_room / t_follow / t_chat_message`，含索引、注释、初始分类与测试账号
- [x] 引入 MyBatis-Plus 3.5.17 + JSqlParser + MySQL Driver
- [x] `MybatisPlusConfig`：分页插件、逻辑删除、自动填充（`created_at/updated_at`）
- [x] `MyMetaObjectHandler`
- [x] 实体与 Mapper：User / Category / LiveRoom / Follow / ChatMessage
- [x] `PageResult<T>` 与分页参数校验
- [x] `GET /api/v1/categories`、`GET /api/v1/rooms`（支持分页/分类/关键词）
- [x] 单元测试：分页拦截器、逻辑删除元数据、审计字段填充、分页结果映射

### 交付物

- 建表脚本可重复执行；`tester` 账号已写入 BCrypt 密码，P2 登录可直接使用。
- 房间列表接口返回标准分页结构。

### 验收（DoD）

1. 执行 `mysql ... source sql/init.sql` 无报错，`show tables` 有 5 张表。
2. `GET /api/v1/rooms?page=1&size=10` 返回 `{list:[],total:0,page:1,size:10}`。
3. 插入一条房间后分页能查到；`deleted=1` 后查不到。
4. `mvnw test` 通过。

### 风险

- MyBatis-Plus 3.5.9+ 分页必须显式引 `mybatis-plus-jsqlparser`（已踩过坑）。
- MySQL 8.4 认证插件与连接串参数不匹配 → `allowPublicKeyRetrieval=true&useSSL=false`。

### 完成记录（2026-10-19）✅

**实际落地**

| 项 | 结果 |
|---|---|
| 数据库 | `live` 库 + 5 张表（user / category / live_room / follow / chat_message），含唯一键与查询索引 |
| 初始数据 | 5 个分类；`tester` / `123456`（BCrypt，角色 ANCHOR） |
| 持久层 | MyBatis-Plus 3.5.17 + jsqlparser，分页上限 50，逻辑删除 `deleted`，`createdAt/updatedAt` 自动填充 |
| 接口 | `GET /api/v1/categories`、`GET /api/v1/rooms?page&size&categoryId&keyword` |
| 测试 | 5 个单元测试全部通过（分页拦截器、逻辑删除元数据、插入填充、更新填充、分页映射） |

**验收实测**

- `GET /rooms?page=1&size=10` → `{list:[],total:0,page:1,size:10}`。
- 插入 2 条房间后 `size=1` → `total=2`、`list` 1 条；`categoryId=2` → `total=1`；`keyword=Room` → `total=2`。
- `size=100` → 400 + `{"code":40000,"message":"每页最多 50 条"}`。
- 把一条记录 `deleted=1` 后列表只剩 1 条；SQL 日志确认 `WHERE deleted=0 ... LIMIT ?`。
- 验证数据已物理清理，当前房间列表为空。

**说明**

- 公开列表暂不按状态过滤（未开播/已结束也会返回），P3 定义公开口径后再收紧。
- 自动填充的单元测试已覆盖；真实 INSERT 留到 P3「创建房间」端到端验证。
- `t_gift / t_wallet / t_replay` 等表按阶段追加，不在 P1 建。

### P1 复查记录（2026-10-19）

| 问题 | 影响 | 处理 |
|---|---|---|
| `updatedAt` 用 `strictUpdateFill` | 只在字段为空时填充，更新已有实体时时间可能不刷新 | 改为 `setFieldValByName` 显式覆盖，新增回归测试（测试数 4 → 5） |
| 媒体启动脚本只检查 TCP 8189 | WebRTC 的 UDP 端口被占用时，启动失败原因不直观 | 增加 `Get-NetUDPEndpoint` 检查；健康检查失败时停止残留进程 |
| 文档写 `/rooms` 支持状态过滤 | 当前实现未支持 | 标注为 P3 随开播流转补齐 |
| 文档写礼物/回放表「结构见 init.sql」 | 这些表当前尚未创建 | 改为「按阶段追加」 |

复查验证：Swagger 暴露 `/ping`、`/rooms`、`/categories`；`page=-1` 返回 400；未知路径返回 404；5 张表均为 InnoDB + `utf8mb4_unicode_ci`；`mvnw clean test` 5/5 通过。

仍待后续处理：

- 逻辑删除 SQL 目前靠日志人工验证，尚未做数据库集成测试。
- `t_user` 逻辑删除 + `uk_username` 会占用已删用户名，P2 注册需显式处理。
- P2 返回用户资料必须用 VO，禁止直接序列化 `User`（含 password）。
- 公开列表状态过滤、Git 初始化仍未完成。

---

## P2 · 认证与用户（1.5 天）✅

### 目标

完成注册、登录、刷新、登出、当前用户资料，建立角色体系（USER/ANCHOR/ADMIN）。

### 任务清单

- [x] 引入 Spring Security + JJWT 0.12.7
- [x] `JwtUtil`、`JwtAuthenticationFilter`、`SecurityConfig`（无状态、放行认证与公开接口）
- [x] 注册：用户名唯一、BCrypt、默认昵称与角色
- [x] 登录：返回 accessToken（30min）+ refreshToken（7d），refresh 存 Redis 并轮换
- [x] 刷新：旧 refresh 立即失效；登出：删除 Redis 记录
- [x] `GET/PUT /api/v1/users/me`
- [ ] 头像上传（MinIO）→ 延后到 P4
- [x] 统一 401/403 响应体
- [x] 前端登录/注册页 + `useUserStore` + 401 自动刷新并重放请求

### 交付物

- Postman/curl 可完成注册 → 登录 → 带 token 访问 `/users/me` → 刷新 → 登出。
- 前端登录后刷新页面仍保持登录态。

### 验收（DoD）

1. 重复用户名注册返回业务错误码，不抛 500。
2. 错误密码登录返回 401，响应体含中文提示。
3. accessToken 过期后前端自动刷新，用户无感知。
4. 登出后旧 refreshToken 再刷新返回 401。
5. 未登录访问 `/api/v1/users/me` 返回 401，访问 Swagger 不受影响。

### 风险

- Security 默认链拦截 Swagger / ping → 放行白名单逐条列全。
- Refresh 并发刷新导致令牌轮换互踢 → 前端单例刷新队列 + 服务端 Lua 原子消费旧令牌。

### 完成记录（2026-10-19）✅

**后端**

| 项 | 结果 |
|---|---|
| 依赖 | Spring Security + JJWT 0.12.7 + Spring Data Redis |
| accessToken | JWT（HMAC，实际算法随密钥长度），默认 30 分钟，带 userId/username/role/jti |
| refreshToken | 256 位随机串，Redis 存 `auth:refresh:{token}` → userId，7 天；Lua 原子取出并删除，刷新即轮换 |
| 接口 | `POST /auth/register|login|refresh|logout`、`GET/PUT /users/me` |
| 安全链 | 无状态、CSRF 关闭、统一 401/403；公开白名单只放 GET 的 ping/categories/rooms |
| 校验 | 用户名 4~20 位、密码 6~32 位、重复用户名 409、封禁账号 403、禁用/未登录 401 |
| 测试 | 8 个单元测试通过（新增 3 个 JWT 用例：claims、过期、短密钥） |

**前端**

- `request.ts` 统一解包 + 自动带 Bearer + 401 单例刷新队列 + 请求重放。
- Zustand `useUserStore` 持久化登录态；`SiteHeader` 展示用户菜单；`AuthGuard` 保护个人中心。
- 登录、注册、个人中心页面；注册后自动登录；资料可改昵称/头像 URL/简介。
- 登出清空本地状态并整页回到首页，避免 AuthGuard 与路由跳转竞态。

**验收实测**

- curl：注册 200、重复注册 409、短密码 400、错误密码 401、封禁登录 403、无 token 401、坏 token 401。
- 刷新：新令牌可用；旧 refresh 再刷新 401；登出后 refresh 401；Redis 中 `auth:refresh:*` 归零。
- 浏览器：登录跳首页、刷新保持登录；把 localStorage accessToken 改成非法值后访问 `/profile`，自动刷新并成功加载；注册自动登录；资料更新落库；登出回首页且本地状态清空。
- 正常流程浏览器控制台 0 error；Card `bordered` 弃用警告与 favicon 404 已修复。自动刷新用例里首次 401 会留下一条预期的网络错误日志。

**说明**

- 已签发 accessToken 在登出后仍有效至过期（无黑名单）；需要立即失效时在 P13 增加 Redis 黑名单。
- 登录态存 localStorage，生产建议改 HttpOnly Cookie + CSRF。
- 头像目前为 URL，MinIO 上传在 P4。
- 登录/注册/送礼限流在 P13，当前仅做参数校验。
- 多标签页同时触发刷新时，严格轮换可能让其中一个标签被登出；P5/P13 可加短宽限或 BroadcastChannel 协调。

**实现中修复**

- JWT 过滤器不声明为 Bean，避免 Spring Boot 再注册一次 Servlet 过滤器导致重复执行。
- 个人中心查询缓存按 userId 分键，切换账号不会读到上一个账号的数据。
- 空头像字符串在服务端归一化为 null，前端 `src` 用 `|| undefined`，消除 Avatar 空 src 警告。
- `User.password` 与 `LiveRoom.streamKey` 加 `@JsonIgnore`，避免未来误把实体直接返回时泄露敏感字段。

---

## P3 · 直播间与推流鉴权（2 天）

### 目标

主播创建直播间、获取推流密钥、开播/关播；MediaMTX 通过 HTTP 回调校验推流权限；房间状态随推流事件流转。

### 任务清单

**数据与接口**
- [ ] `t_live_room` 完整字段 + `t_category` 初始化数据
- [ ] 任何登录用户创建房间时自动获得 `ANCHOR` 角色；同一主播只允许一个未结束房间（创建/开播时校验，冲突返回 409）
- [ ] `POST/GET/PUT /api/v1/rooms`、`GET /rooms/{id}`
- [ ] `POST /rooms/{id}/start`：轮换 streamKey、返回推流地址（状态保持未开播，publish 成功后才置 LIVE）
- [ ] `POST /rooms/{id}/stream-key/reset`：主播重置密钥，旧密钥立即失效
- [ ] `POST /rooms/{id}/stop`：状态置 ENDED、清理 Redis、通知媒体服务器踢流
- [ ] `GET /rooms/{id}/stream`：仅主播本人可读

**媒体回调**
- [ ] `POST /api/v1/media/auth`：解析 `action/path/query`，校验密钥与房间状态；publish 通过则置 LIVE
- [ ] SecurityConfig 放行媒体回调路径（`/api/v1/media/auth`、`/api/v1/media/online|offline`）并校验回调来源/签名，避免伪造事件
- [ ] `POST /api/v1/media/online` / `media/offline`：对应 MediaMTX `runOnOnline/runOnOffline`，事件落库与状态流转
- [ ] MediaMTX 配置 `authMethod: http` + `runOnOnline/runOnOffline` 事件回调
- [ ] `authHTTPExclude` 排除 `api`/`metrics`（或仅允许 127.0.0.1），避免后端调管理 API 时被自己的鉴权拦截
- [ ] 定时对账任务（30s）：MediaMTX 活跃 path ↔ 数据库 LIVE 房间
- [ ] 主播中心页：创建房间、封面、开播、复制推流地址、重置密钥

### 交付物

- 主播端可拿到 `rtmp://localhost:1935/live/{roomId}?key={streamKey}` 并用 OBS 推流。
- 未授权密钥推流被 MediaMTX 拒绝（401）。

### 验收（DoD）

1. OBS 填入正确密钥推流，房间状态 3s 内变 `LIVE`，列表出现。
2. 乱填密钥推流失败，后端日志有拒绝记录。
3. 关闭 OBS，房间 10s 内变 `ENDED`（事件回调）；强杀 OBS 进程时 60s 内由对账任务修正。
4. 非房主调用 `/stream` 返回 403。
5. 重置密钥后旧密钥推流立即失败。
6. 同一主播创建第二个未结束房间返回 409。

### 风险

- MediaMTX 鉴权回调路径/字段与版本不一致 → 先用固定配置验证一次，再写进 yml。
- Windows 上 `runOnOnline` 的命令引号转义 → 用 `curl.exe` + 简单 URL，不拼复杂参数。
- OBS 推流与 ffmpeg 测试流并存时房间状态抖动 → 对账以最新事件为准。
- `authMethod: http` 同样作用于管理 API → 未配置排除项时，对账/踢流请求会被 401，必须显式排除。

---

## P4 · 播放页与观看体验（1.5 天）

### 目标

观众从首页进入直播间，播放器优先 WebRTC、失败自动切 LL-HLS；播放地址经后端签发令牌，防盗链。

### 任务清单

- [ ] `GET /rooms/{id}/play-info`：返回 `whepUrl/hlsUrl/token/expireAt`，房间非 LIVE 返回错误
- [ ] SecurityConfig 放行 `GET /api/v1/rooms/*/play-info`（游客也能取播放令牌），其余主播接口继续要求登录
- [ ] 播放令牌（JWT 5min，含 roomId + 游客/用户标识），MediaMTX `read` 鉴权校验
- [ ] 前端 `Player` 组件：WHEP → 5s 超时/失败 → hls.js
- [ ] 房间页布局：播放器 + 主播信息 + 关注按钮 + 弹幕侧栏占位
- [ ] 首页列表：封面、标题、主播、在线人数、LIVE 角标；空态与骨架屏
- [ ] 播放器状态：加载中 / 直播已结束 / 网络错误 + 重试
- [ ] 头像上传 MinIO（P2 延后项），个人中心接入上传组件
- [ ] 页面埋点：进入房间、播放成功、卡顿（为 L2 数据看板留字段）

### 交付物

- 两个浏览器窗口：主播开播，观众进入页面 3s 内出画面。
- 播放令牌过期后刷新页面可重新获取，不出现永久黑屏。

### 验收（DoD）

1. Chrome 中 WHEP 播放成功，首帧 ≤ 2s（本机）。
2. 手动把 `whepUrl` 改为错误地址，自动回落 HLS 并出画面。
3. 未开播房间进入详情页显示「主播未开播」，不请求媒体地址。
4. 房间结束后播放器显示结束态，不无限重连。

### 风险

- WebRTC 在 localhost 可用、局域网 IP 需 `webrtcAdditionalHosts` → 文档写清。
- HLS 首次分片未生成导致 404 → 播放器重试 3 次，间隔 1s。
- 移动端自动播放限制 → 默认静音 + 点击取消静音。

---

## P5 · 实时聊天与在线人数（1.5 天）

### 目标

直播间弹幕实时互通、历史消息可回溯、在线人数准确；完成 L1 档位验收。

### 任务清单

- [ ] 引入 `spring-boot-starter-websocket`，STOMP 端点 `/ws`；`/ws` 在 SecurityConfig 放行，由 `ChannelInterceptor` 读取 CONNECT 帧的 `Authorization` native header 解析 JWT
- [ ] 订阅 `/topic/rooms/{id}`，发送 `/app/rooms/{id}/chat`；游客只允许 SUBSCRIBE，SEND 直接拒绝
- [ ] 消息校验：长度 ≤ 100、非空、敏感词、Redis 1s 限流、禁言拦截
- [ ] 最近 100 条存 Redis List，进入房间先拉历史
- [ ] 在线人数：Redis ZSET + 15s 心跳 + 10s 清理与广播
- [ ] 前端 `ChatPanel`：消息列表自动滚动、游客只读提示、发送失败回显
- [ ] 主播端「结束直播」按钮 + 房间结束广播
- [ ] L1 档位演示脚本与 tag `v1.0-live`

### 交付物

- 弹幕端到端 ≤ 300ms；刷新页面能看到最近历史。
- 在线人数随窗口开关在 20s 内收敛。

### 验收（DoD）

1. 两个账号互发弹幕，双方 300ms 内看到。
2. 1 秒内连发 5 条，只落 1 条并提示「发送太快」。
3. 关闭一个窗口，20s 内在线人数减 1。
4. 未登录用户能看到弹幕，输入框禁用并提示登录。
5. 主播关播后所有观众收到「直播已结束」，输入框禁用。

### 风险

- WebSocket 代理未配置 Upgrade → Vite `proxy.ws = true`，Nginx 部署时补 `Upgrade` 头。
- 浏览器不能给 WebSocket 握手加自定义 HTTP 头 → token 走 STOMP CONNECT 帧，不要照搬 REST 的 `Authorization` 头方案。
- 断线重连导致重复消息 → 消息带自增 ID，前端按 ID 去重。
- 在线人数与真实观众口径不一致 → 明确「连接数」口径，写入文档。

---

# 第二档 · L2 能互动能运营（P6~P10）

> 档位验收：观众充值虚拟币送礼、礼物特效与贡献榜生效；主播关播后自动生成回放；管理员可封禁违规房间；主播中心看到场次数据。

## P6 · 礼物与虚拟币账本（2 天）

### 目标

礼物目录、送礼扣款、幂等、礼物广播、房间贡献榜；钱包余额与流水可查。

### 任务清单

- [ ] 表：`t_gift / t_gift_record / t_wallet / t_wallet_txn`
- [ ] 初始化礼物与测试钱包（新用户注册赠送 1000 币）
- [ ] `GET /gifts`、`POST /rooms/{id}/gifts`（`requestId` 幂等）
- [ ] 扣款条件更新 + 事务 + 流水；余额不足返回明确错误码
- [ ] WebSocket 广播 `GIFT` 消息，前端礼物特效与飘屏
- [ ] 房间贡献榜（Redis ZSET，日榜/总榜）
- [ ] 钱包页：余额、流水、模拟充值（仅测试环境开关）

### 验收（DoD）

1. 余额 100，送 200 的礼物失败且余额不变。
2. 同一 `requestId` 连续提交 3 次，只扣一次、只广播一次。
3. 50 并发送礼无负余额、无重复流水。
4. 礼物消息 500ms 内到达房间所有观众。

### 风险

- 事务内发 WebSocket 消息导致回滚后仍广播 → 事务提交后发（`TransactionSynchronization`）。
- 榜单 ZSET 与 DB 不一致 → 每日定时从流水重算。

---

## P7 · 热度榜 / 关注 / 搜索（1.5 天）

### 目标

首页从「最新开播」升级为「热度排序」，支持分类筛选、关键词搜索、关注流。

### 任务清单

- [ ] 热度定时任务（30s）：在线/弹幕/礼物加权，写 `hot_score` 与 Redis ZSET
- [ ] `GET /rooms?sort=hot|new&categoryId=&keyword=`
- [ ] 关注/取关接口与 `t_follow`，首页「我的关注」Tab
- [ ] `GET /rank/hot` 热门榜接口与页面模块
- [ ] 搜索防抖、空态、关键词高亮（可选）

### 验收（DoD）

1. 两个房间开播，送礼/弹幕更多的房间 1 分钟内排名上升。
2. 关注主播后其开播房间出现在关注流首位。
3. 搜索标题关键词能命中，分类筛选结果正确。
4. 热度任务重复执行不产生重复累加。

### 风险

- 热度权重不合理导致头部固化 → 加时间衰减与冷启动加权。
- 关键词搜索 `%` 注入 → 参数化 + 转义通配符。

---

## P8 · 录制与回放（2 天）

### 目标

开播自动录制，关播自动登记回放，观众可看回放；磁盘空间可控。

### 任务清单

- [ ] MediaMTX `record: yes`、`recordPath`、`playback: yes`（playback 默认 9996，仅内网）
- [ ] 关播回调读取录制目录，登记 `t_replay`（路径、时长、大小、封面）
- [ ] `GET /rooms/{id}/replays`、`GET /replays/{id}`，返回可播放地址
- [ ] 回放页复用 Player（HLS）
- [ ] 录制保留策略：保留最近 20 场 / 7 天，定时清理
- [ ] 异常场景：推流 10 秒内断开不生成回放

### 验收（DoD）

1. 推流 1 分钟 → 关播 → 2 分钟内回放列表出现且可播放。
2. 回放时长与实际推流时长误差 ≤ 5s。
3. 超过保留策略的录制被清理，数据库状态同步。
4. 无录制文件时回放接口返回空列表，不报 500。

### 风险

- MediaMTX 录制的是 fMP4 分片，播放需其 playback 端点 → 先用官方播放页验证，再接入前端。
- Windows 文件占用导致删除失败 → 重试 + 日志告警。

---

## P9 · 管理后台与内容审核（2 天）

### 目标

管理员可查看平台概览、搜索房间/用户、封禁/解封、处理举报；封禁立即作用于推流与聊天。

### 任务清单

- [ ] 角色鉴权 `/admin/**`（仅 ADMIN）
- [ ] 后台布局与路由（独立 `AdminLayout`）
- [ ] 平台概览：用户数、直播中房间、今日开播场次、今日礼物流水
- [ ] 房间管理：列表、强制关播、封禁（原因/时长）、解封
- [ ] 用户管理：禁言、封禁、角色调整
- [ ] 举报：观众举报入口 + 后台列表 + 处理结果
- [ ] 操作审计：`t_admin_log` 记录操作人/对象/原因

### 验收（DoD）

1. 普通用户访问 `/api/v1/admin/**` 返回 403。
2. 封禁房间后：列表消失、推流被踢、观众收到结束提示。
3. 禁言用户发弹幕返回明确提示，解除后恢复。
4. 所有管理操作在审计表有记录。

### 风险

- 管理员误封 → 二次确认 + 原因必填 + 可解封。
- 封禁只改 DB 未踢流 → 统一走 `RoomStateService`，保证三处联动。

---

## P10 · 主播中心与数据看板（1.5 天）

### 目标

主播可管理自己的房间与回放，查看场次、峰值在线、弹幕数、礼物收益趋势。

### 任务清单

- [ ] `GET /anchor/overview`：累计场次、总观看、峰值、收益
- [ ] `GET /anchor/trend?days=7`：按天聚合
- [ ] `t_live_session`（场次快照）或从房间历史聚合
- [ ] 主播中心页：房间设置、开播记录、回放管理、数据卡片与折线图
- [ ] 收益明细（礼物流水分页）
- [ ] L2 档位演示脚本与 tag `v2.0-interact`

### 验收（DoD）

1. 开播-关播 3 次后，场次统计为 3。
2. 峰值在线与直播中实际峰值一致。
3. 7 天趋势图数据与数据库聚合结果一致。
4. 主播只能看到自己的数据。

### 风险

- 实时聚合慢 → 场次结束时落快照，看板只查快照。
- 时区问题导致按天聚合错位 → 统一 `Asia/Shanghai`，SQL 显式时区。

---

# 第三档 · L3 能上线能扩展（P11~P15）

> 档位验收：双后端实例 + Redis 广播下聊天不丢；压测报告可复现；`docker compose up -d` 一键部署并通过冒烟测试。

## P11 · 多码率与低延迟增强（2.5 天）

### 目标

支持 720p/480p 自适应；WebRTC 播放稳定；可选连麦能力调研与 PoC。

### 任务清单

- [ ] ffmpeg 旁路转码：原流 → 720p/480p 两路，推回 MediaMTX 不同 path
- [ ] `play-info` 返回多档地址，前端根据网络/尺寸切换
- [ ] WebRTC `webrtcAdditionalHosts` / TURN 配置（局域网与公网）
- [ ] 播放质量埋点：首帧、卡顿率、码率切换
- [ ] （选做）连麦 PoC：评估 SRS WebRTC / LiveKit，产出结论不强行落地

### 验收（DoD）

1. 720p/480p 可手动切换且 3s 内生效。
2. 弱网模拟（Chrome DevTools）自动降档。
3. 局域网另一台设备通过 IP 可播放（WebRTC 或 HLS）。

### 风险

- ffmpeg 转码吃满 CPU → 限制分辨率/帧率，压测后给并发上限。
- TURN 部署复杂 → 首版只保证本机与局域网，公网部署单列。

---

## P12 · 水平扩展与消息总线（2 天）

### 目标

后端可起两个实例，WebSocket 广播与在线人数跨实例一致；房间状态不依赖单机内存。

### 任务清单

- [ ] 聊天广播走 Redis Pub/Sub（或 Stream），各实例转发到本地会话
- [ ] 在线人数 ZSET 跨实例统一，会话 ID 带实例标识
- [ ] 定时任务加分布式锁（Redis `SETNX`），避免重复执行
- [ ] 媒体事件回调幂等（事件 ID 去重）
- [ ] Nginx 对 `/ws` 做 ip_hash 或直接广播方案验证
- [ ] 双实例启动脚本 + 验证用例

### 验收（DoD）

1. 两个后端实例（8080/8081）+ Nginx 轮询，用户随机落到不同实例，弹幕互通。
2. 在线人数 = 两个实例连接数之和，误差 ≤ 1。
3. 定时任务在双实例下只执行一次。
4. 杀掉一个实例，客户端 5s 内重连到另一个实例并补历史。

### 风险

- Redis Pub/Sub 丢消息（无持久化）→ 接受「实时消息可丢」，历史消息从 Redis List 补。
- 会话粘性与重连风暴 → 重连指数退避 + 抖动。

---

## P13 · 安全风控与压测（1.5 天）

### 目标

补齐限流、防刷、鉴权边界；产出可复现的压测报告与容量结论。

### 任务清单

- [ ] 接口限流（登录、注册、送礼、弹幕）——Redis 令牌桶/滑动窗口
- [ ] 登录失败锁定、验证码（可选）
- [ ] 播放令牌与推流密钥安全复核；敏感配置全部环境变量
- [ ] XSS/CSRF/越权用例清单与自动化测试
- [ ] 压测脚本（k6 或 JMeter）：列表、进入房间、弹幕、送礼
- [ ] 压测报告：QPS、P95、错误率、瓶颈定位

### 验收（DoD）

1. 登录接口 1 分钟内 20 次失败后锁定，返回明确提示。
2. 单用户 10 并发送礼只成功 1 次（幂等）。
3. 压测 200 并发观看 + 20 并发弹幕，错误率 < 1%，P95 < 500ms（不含媒体流）。
4. 报告落盘 `doc/load-test-report.md`。

### 风险

- 压测打爆本机 MySQL → 限制并发、监控资源。
- 限流误伤正常用户 → 阈值可配置，灰度验证。

---

## P14 · 可观测性与运维（1 天）

### 目标

出问题能定位：结构化日志、关键指标、健康检查、备份与清理脚本。

### 任务清单

- [ ] Actuator：health / metrics / prometheus
- [ ] 关键业务指标：在线房间数、推流成功/失败、弹幕速率、送礼失败率
- [ ] 慢 SQL 日志与接口耗时日志（traceId）
- [ ] 日志滚动与保留策略
- [ ] 备份脚本：MySQL 每日备份保留 7 天；录制清理
- [ ] 告警规则草案（磁盘、媒体进程、后端健康）

### 验收（DoD）

1. `/actuator/health` 汇总 MySQL/Redis/MinIO/媒体服务器状态。
2. 一次请求可用 traceId 串起前后端日志。
3. 备份脚本执行成功并生成可恢复文件。
4. 媒体服务器异常时健康检查变 DOWN。

### 风险

- 指标基数爆炸 → 只保留房间级聚合，不打用户标签。
- 健康检查依赖外部服务导致误报 → 区分 liveness / readiness。

---

## P15 · 容器化部署与 CI/CD（1.5 天）

### 目标

单机 `docker compose up -d` 拉起全栈；冒烟测试脚本覆盖主链路；GitHub Actions 完成构建与测试。

### 任务清单

- [ ] 后端 Dockerfile（多阶段、非 root、健康检查）
- [ ] 前端 Dockerfile（Node 构建 → Nginx 托管）
- [ ] `deploy/docker-compose.yml`：mysql/redis/minio/mediamtx/backend/nginx
- [ ] Nginx：静态托管、`/api`、`/ws`（Upgrade）、`/hls`、`/webrtc` 反代
- [ ] Compose 暴露 `1935/tcp`（推流）与 `8189/udp`（WebRTC ICE），其余端口仅内网
- [ ] `.env.example` 生产必填项校验（缺失即启动失败），媒体地址改同源 `/hls`、`/webrtc`
- [ ] `deploy/smoke-test.ps1`：健康、注册登录、开播鉴权、播放地址、送礼、回放
- [ ] GitHub Actions：后端 `mvn test`、前端 `npm ci && npm run build`
- [ ] 部署文档与排障速查

### 验收（DoD）

1. 干净机器（仅 Docker）执行 `docker compose up -d --build` 后 5 分钟内可访问。
2. 冒烟测试全部通过（≥ 15 项断言）。
3. CI 在 PR 上跑通，失败能阻止合并。
4. 重启容器后数据不丢（具名卷），直播状态能自动对账恢复。

### 风险

- WebRTC 端口与 NAT → 除 8889/TCP 外必须放通 8189/UDP，公网场景再补 TURN。
- 镜像构建慢 → 分层缓存 + 国内镜像源。
- Compose 变量缺失导致弱密码上线 → 必填校验，无默认值。

---

## 环境与常用命令

```powershell
# 中间件（绿色版）
powershell -ExecutionPolicy Bypass -File D:\business\tools\mysql-start.ps1
powershell -ExecutionPolicy Bypass -File D:\business\tools\redis-start.ps1
powershell -ExecutionPolicy Bypass -File D:\business\tools\minio-start.ps1

# 媒体服务器
powershell -ExecutionPolicy Bypass -File media\media-start.ps1
powershell -ExecutionPolicy Bypass -File media\media-stop.ps1

# 后端
cd backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package

# 前端
cd frontend
npm install
npm run dev
npm run build

# OBS 推流
# 服务器：rtmp://localhost:1935/live
# 串流密钥：主播中心复制（P3 起）
```

### 端口表

| 端口 | 服务 | 说明 |
|---|---|---|
| 5173 | Vite | 前端开发服务器 |
| 8080 | Spring Boot | API + WebSocket |
| 1935 | MediaMTX | RTMP 推流 |
| 8888 | MediaMTX | HLS 播放 |
| 8889 | MediaMTX | WebRTC（WHEP） |
| 9997 | MediaMTX | 管理 API（仅本机） |
| 3306 / 6379 / 9000 / 9001 | MySQL / Redis / MinIO | 本地中间件 |

---

## 全局验收清单（三档合并）

- [ ] 陌生人按 README 10 分钟内跑起来
- [ ] 推流 → 播放 → 弹幕 → 送礼 → 回放全链路可复现
- [ ] 异常中断（OBS 强杀、媒体重启、后端重启）状态可自愈
- [ ] 送礼并发不超扣、不重复扣
- [ ] 权限边界：非房主、非管理员、被封禁用户均被正确拦截
- [ ] 双实例部署聊天互通
- [ ] 压测报告与容量结论落盘
- [ ] 一键部署 + 冒烟测试通过

---

## 进度回填

| 阶段 | 完成日期 | 实际耗时 | 偏差与备注 |
|---|---|---|---|
| P0 | 2026-10-19 | 0.5 天 | 按计划完成 |
| P1 | 2026-10-19 | 0.5 天 | 按计划完成 |
| P2 | 2026-10-19 | 0.5 天 | 按计划完成；头像上传延后到 P4 |
| P3 | | | |
| P4 | | | |
| P5 | | | |
| P6 | | | |
| P7 | | | |
| P8 | | | |
| P9 | | | |
| P10 | | | |
| P11 | | | |
| P12 | | | |
| P13 | | | |
| P14 | | | |
| P15 | | | |

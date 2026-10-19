# 媒体服务器（MediaMTX）

单文件、Windows 原生，负责 RTMP 接入与 HLS / WebRTC 转封装。不依赖 Docker、ffmpeg。

## 启停

```powershell
powershell -ExecutionPolicy Bypass -File media\media-start.ps1
powershell -ExecutionPolicy Bypass -File media\media-stop.ps1
```

首次启动会自动下载 `v1.21.2 windows_amd64` 到 `media/bin/`（约 29 MB，已被 git 忽略）。
启动失败时看 `media/mediamtx.log` 与 `media/mediamtx.err.log`。

## 端口

| 端口 | 协议 | 用途 |
|---|---|---|
| 1935 | RTMP | OBS 推流 |
| 8888 | HTTP | LL-HLS 播放 |
| 8889 | HTTP | WebRTC（WHEP）播放 |
| 9997 | HTTP | 管理 API（仅 127.0.0.1） |
| 8189 | UDP | WebRTC ICE |

## 鉴权与事件（P3）

- `authMethod: http`：推流/播放都回调后端 `/api/v1/media/auth`。
- `authHTTPExclude` 排除 `api/metrics/pprof`，否则后端对账与踢流会被自己的鉴权拦截。
- `runOnOnline` / `runOnOffline` 调用 `notify.ps1`，脚本读取 MediaMTX 注入的 `MTX_PATH` 并通知后端。
- 回调共享 token 为开发默认值 `dev-media-callback-token`；生产必须同时修改 `media/mediamtx.yml`、`notify.ps1` 与后端环境变量 `MEDIA_CALLBACK_TOKEN`。
- 事件失败不影响推流，后端每 10 秒对账一次。

## P0 手工验证（还没有后端鉴权）

用 ffmpeg 推一个测试画面（需本机有 ffmpeg，没有可跳过）：

```powershell
ffmpeg -re -f lavfi -i testsrc=size=1280x720:rate=30 -f lavfi -i sine=frequency=440 `
  -c:v libx264 -preset veryfast -tune zerolatency -c:a aac `
  -f flv "rtmp://localhost:1935/live/p0-test"
```

播放地址：

- HLS：`http://localhost:8888/live/p0-test/index.m3u8`
- WHEP：`http://localhost:8889/live/p0-test/whep`
- API 查看活跃流：`http://127.0.0.1:9997/v3/paths/list`

OBS 设置（P3 起带密钥）：

- 服务：自定义
- 服务器：`rtmp://localhost:1935/live`
- 串流密钥：`{roomId}?key={streamKey}`

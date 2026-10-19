import { LoadingOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, Tag, Typography } from 'antd'
import Hls from 'hls.js'
import { useEffect, useRef, useState } from 'react'

const { Text } = Typography

type PlayerMode = 'connecting' | 'webrtc' | 'hls' | 'error'

interface LivePlayerProps {
  whepUrl: string
  hlsUrl: string
}

export default function LivePlayer({ whepUrl, hlsUrl }: LivePlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const [mode, setMode] = useState<PlayerMode>('connecting')
  const [error, setError] = useState<string>()
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const video = videoRef.current
    if (!video) {
      return
    }

    let disposed = false
    let hlsStarted = false
    let pc: RTCPeerConnection | null = null
    let hls: Hls | null = null
    let whepTimeout: number | undefined

    setMode('connecting')
    setError(undefined)

    const playVideo = () => {
      void video.play().catch(() => {
        // 浏览器可能要求用户手势；画面已就绪，保留控制条让用户手动播放
      })
    }

    const fail = (message: string) => {
      if (!disposed) {
        setError(message)
        setMode('error')
      }
    }

    const clearVideoSource = () => {
      video.pause()
      video.srcObject = null
      video.removeAttribute('src')
      video.load()
    }

    const startHls = () => {
      if (disposed) {
        return
      }
      hlsStarted = true
      // WHEP 失败时 srcObject 可能已经被 ontrack 赋值，必须先清掉，否则会抢占 HLS 的 src
      clearVideoSource()
      setMode('hls')
      if (Hls.isSupported()) {
        hls = new Hls({ enableWorker: true, lowLatencyMode: true })
        hls.on(Hls.Events.MANIFEST_PARSED, playVideo)
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) {
            fail('HLS 播放失败，请重试')
          }
        })
        hls.loadSource(hlsUrl)
        hls.attachMedia(video)
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = hlsUrl
        video.addEventListener('loadedmetadata', playVideo, { once: true })
      } else {
        fail('当前浏览器不支持 HLS 播放')
      }
    }

    const startWhep = async () => {
      const connection = new RTCPeerConnection()
      pc = connection
      connection.addTransceiver('video', { direction: 'recvonly' })
      connection.addTransceiver('audio', { direction: 'recvonly' })
      connection.oniceconnectionstatechange = () => {
        console.debug('WHEP ICE state:', connection.iceConnectionState)
      }
      connection.ontrack = (event) => {
        if (disposed || hlsStarted) {
          return
        }
        console.debug('WHEP track:', event.track.kind)
        const stream = event.streams[0] ?? new MediaStream([event.track])
        video.srcObject = stream
        playVideo()
      }

      const offer = await connection.createOffer()
      await connection.setLocalDescription(offer)
      await new Promise<void>((resolve) => {
        if (connection.iceGatheringState === 'complete') {
          resolve()
          return
        }
        const timer = window.setTimeout(resolve, 1_200)
        connection.addEventListener('icegatheringstatechange', () => {
          if (connection.iceGatheringState === 'complete') {
            window.clearTimeout(timer)
            resolve()
          }
        })
      })

      const response = await fetch(whepUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/sdp',
          Accept: 'application/sdp',
        },
        body: connection.localDescription?.sdp,
      })
      if (!response.ok) {
        throw new Error(`WHEP ${response.status}`)
      }
      const answer = await response.text()
      if (disposed || hlsStarted) {
        return
      }
      await connection.setRemoteDescription({ type: 'answer', sdp: answer })
      playVideo()
      await waitFirstFrame()
      if (!disposed && !hlsStarted) {
        setMode('webrtc')
      }
    }

    const waitFirstFrame = () =>
      new Promise<void>((resolve, reject) => {
        const startedAt = Date.now()
        const tick = () => {
          if (disposed) {
            return
          }
          if (video.readyState >= 2 && video.videoWidth > 0) {
            resolve()
            return
          }
          if (Date.now() - startedAt > 5_000) {
            reject(new Error('WHEP 已连接但未出画面'))
            return
          }
          window.requestAnimationFrame(tick)
        }
        tick()
      })

    const timeout = new Promise<never>((_, reject) => {
      whepTimeout = window.setTimeout(() => reject(new Error('WHEP 连接超时')), 6_000)
    })

    void Promise.race([startWhep(), timeout])
      .catch((reason) => {
        if (disposed) {
          return
        }
        console.warn('WHEP failed, fallback to HLS:', reason)
        pc?.close()
        pc = null
        startHls()
      })
      .finally(() => {
        if (whepTimeout) {
          window.clearTimeout(whepTimeout)
        }
      })

    return () => {
      disposed = true
      if (whepTimeout) {
        window.clearTimeout(whepTimeout)
      }
      pc?.close()
      hls?.destroy()
      clearVideoSource()
    }
  }, [whepUrl, hlsUrl, attempt])

  return (
    <div className="player-shell">
      <video ref={videoRef} className="player-video" autoPlay muted playsInline controls />
      {mode === 'connecting' ? (
        <div className="player-overlay">
          <LoadingOutlined spin />
          <Text>正在连接直播…</Text>
        </div>
      ) : null}
      {mode === 'error' ? (
        <div className="player-overlay is-error">
          <Text strong>播放失败</Text>
          <Text type="secondary">{error ?? '请稍后重试'}</Text>
          <Button icon={<ReloadOutlined />} onClick={() => setAttempt((value) => value + 1)}>
            重新连接
          </Button>
        </div>
      ) : null}
      {mode === 'webrtc' || mode === 'hls' ? (
        <Tag className="player-mode" color={mode === 'webrtc' ? 'green' : 'blue'} bordered={false}>
          {mode === 'webrtc' ? 'WebRTC' : 'HLS'}
        </Tag>
      ) : null}
    </div>
  )
}

import { useEffect, useRef, useState } from 'react'
import { Download, MonitorUp, Square } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

export default function ScreenRecorderSurface() {
  const recorder = useRef<MediaRecorder | null>(null)
  const stream = useRef<MediaStream | null>(null)
  const chunks = useRef<Blob[]>([])
  const [recording, setRecording] = useState(false)
  const [includeMic, setIncludeMic] = useState(false)
  const [videoUrl, setVideoUrl] = useState('')
  const [message, setMessage] = useState('Choose a screen or window. Recording stays in browser memory until you download it.')

  useEffect(() => () => {
    stream.current?.getTracks().forEach((track) => track.stop())
    if (videoUrl) URL.revokeObjectURL(videoUrl)
  }, [videoUrl])

  const stop = () => {
    if (recorder.current?.state === 'recording') recorder.current.stop()
    stream.current?.getTracks().forEach((track) => track.stop())
    setRecording(false)
  }

  const start = async () => {
    try {
      const display = await navigator.mediaDevices.getDisplayMedia({ video: { frameRate: 30 }, audio: true })
      const tracks = [...display.getVideoTracks(), ...display.getAudioTracks()]
      if (includeMic) {
        const mic = await navigator.mediaDevices.getUserMedia({ audio: true })
        tracks.push(...mic.getAudioTracks())
      }
      const combined = new MediaStream(tracks)
      const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9,opus') ? 'video/webm;codecs=vp9,opus' : 'video/webm'
      const next = new MediaRecorder(combined, { mimeType, videoBitsPerSecond: 4_000_000 })
      chunks.current = []
      next.ondataavailable = (event) => { if (event.data.size) chunks.current.push(event.data) }
      next.onstop = () => {
        if (videoUrl) URL.revokeObjectURL(videoUrl)
        setVideoUrl(URL.createObjectURL(new Blob(chunks.current, { type: mimeType })))
        setMessage('Recording ready. Preview or download it locally.')
        markToolSuccess('screen-recorder', { headline: 'Screen recording ready', detail: 'Your recording is in browser memory and ready to preview or download.', shareText: 'I recorded my screen locally with PureHub.' })
      }
      display.getVideoTracks()[0].addEventListener('ended', stop, { once: true })
      recorder.current = next
      stream.current = combined
      next.start(1000)
      setRecording(true)
      setMessage('Recording now. Browser sharing controls remain visible for safety.')
    } catch { setMessage('Screen capture was cancelled or is unavailable in this browser.') }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Creator flagship" title="Screen Recorder" description="Record a tab, window or screen directly in your browser, then preview and download the WebM file." accent="violet" />
    <Panel title="Recorder" subtitle={message}>
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={includeMic} onChange={(event) => setIncludeMic(event.target.checked)} disabled={recording} />Include microphone</label>
        {!recording ? <ActionButton onClick={() => void start()}><MonitorUp className="mr-2 inline size-4" />Start</ActionButton> : <ActionButton tone="danger" onClick={stop}><Square className="mr-2 inline size-4" />Stop</ActionButton>}
      </div>
      {videoUrl ? <div className="mt-4"><video src={videoUrl} controls className="max-h-[520px] w-full rounded-[14px] bg-black" /><a href={videoUrl} download={`purehub-recording-${Date.now()}.webm`} className="mt-3 inline-flex min-h-10 items-center rounded-[11px] bg-emerald-700 px-3.5 text-sm font-bold text-white"><Download className="mr-2 size-4" />Download recording</a></div> : null}
    </Panel>
    <p className="rounded-[14px] bg-amber-500/10 p-3 text-xs text-amber-800 dark:text-amber-200">Screen capture always requires a browser permission prompt. PureHub cannot record silently or in the background.</p>
  </div>
}

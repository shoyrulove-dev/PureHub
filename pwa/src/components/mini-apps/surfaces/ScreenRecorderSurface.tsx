import { useEffect, useRef, useState } from 'react'
import { Download, MonitorUp, Square } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { normalizeLocale, type LocaleCode } from '../../../i18n/locales'

const RECORDER_COPY: Record<LocaleCode, Record<string, string>> = {
  en: { initial: 'Choose a screen or window. The recording stays in browser memory until downloaded.', ready: 'Recording ready. Preview or download it locally.', recording: 'Recording now. Browser sharing controls remain visible for safety.', unavailable: 'Screen capture was cancelled or is unavailable in this browser.', eyebrow: 'Creator tool', title: 'Screen Recorder', description: 'Record a tab, window, or screen, then preview and download the WebM file.', panel: 'Recorder', mic: 'Include microphone', start: 'Start', stop: 'Stop', download: 'Download recording', note: 'Screen capture always requires browser permission. PureHub cannot record silently or in the background.' },
  vi: { initial: 'Chọn màn hình hoặc cửa sổ. Bản ghi chỉ nằm trong bộ nhớ trình duyệt cho đến khi tải xuống.', ready: 'Bản ghi đã sẵn sàng. Hãy xem trước hoặc tải xuống.', recording: 'Đang ghi. Điều khiển chia sẻ của trình duyệt vẫn hiển thị để bảo đảm an toàn.', unavailable: 'Đã hủy quay màn hình hoặc trình duyệt không hỗ trợ.', eyebrow: 'Công cụ sáng tạo', title: 'Quay màn hình', description: 'Ghi thẻ, cửa sổ hoặc màn hình rồi xem trước và tải tệp WebM.', panel: 'Trình quay', mic: 'Kèm âm thanh micro', start: 'Bắt đầu', stop: 'Dừng', download: 'Tải bản ghi', note: 'Quay màn hình luôn cần quyền của trình duyệt. PureHub không thể âm thầm ghi hoặc chạy nền.' },
  zh: { initial: '选择屏幕或窗口。下载前，录制内容仅保留在浏览器内存中。', ready: '录制已就绪，可在本机预览或下载。', recording: '正在录制。为确保安全，浏览器共享控件会保持可见。', unavailable: '屏幕录制已取消或此浏览器不支持。', eyebrow: '创作工具', title: '屏幕录制', description: '录制标签页、窗口或屏幕，然后预览并下载 WebM 文件。', panel: '录制器', mic: '包含麦克风', start: '开始', stop: '停止', download: '下载录制', note: '屏幕录制始终需要浏览器授权。PureHub 无法静默录制或在后台录制。' },
  es: { initial: 'Elige una pantalla o ventana. La grabación permanece en la memoria hasta que la descargues.', ready: 'Grabación lista. Puedes verla o descargarla localmente.', recording: 'Grabando. Los controles del navegador siguen visibles por seguridad.', unavailable: 'La captura se canceló o no está disponible en este navegador.', eyebrow: 'Herramienta de creación', title: 'Grabador de pantalla', description: 'Graba una pestaña, ventana o pantalla y descarga el archivo WebM.', panel: 'Grabador', mic: 'Incluir micrófono', start: 'Iniciar', stop: 'Detener', download: 'Descargar grabación', note: 'La captura siempre necesita permiso del navegador. PureHub no puede grabar en silencio ni en segundo plano.' },
}

export default function ScreenRecorderSurface() {
  const copy = RECORDER_COPY[normalizeLocale(window.location.pathname.split('/')[1])]
  const recorder = useRef<MediaRecorder | null>(null)
  const stream = useRef<MediaStream | null>(null)
  const chunks = useRef<Blob[]>([])
  const [recording, setRecording] = useState(false)
  const [includeMic, setIncludeMic] = useState(false)
  const [videoUrl, setVideoUrl] = useState('')
  const [downloadName, setDownloadName] = useState('purehub-recording.webm')
  const [message, setMessage] = useState(copy.initial)

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
        setDownloadName(`purehub-recording-${Date.now()}.webm`)
        setMessage(copy.ready)
        markToolSuccess('screen-recorder', { headline: 'Screen recording ready', detail: 'Your recording is in browser memory and ready to preview or download.', shareText: 'I recorded my screen locally with PureHub.' })
      }
      display.getVideoTracks()[0].addEventListener('ended', stop, { once: true })
      recorder.current = next
      stream.current = combined
      next.start(1000)
      setRecording(true)
      setMessage(copy.recording)
    } catch { setMessage(copy.unavailable) }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow={copy.eyebrow} title={copy.title} description={copy.description} accent="violet" />
    <Panel title={copy.panel} subtitle={message}>
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={includeMic} onChange={(event) => setIncludeMic(event.target.checked)} disabled={recording} />{copy.mic}</label>
        {!recording ? <ActionButton onClick={() => void start()}><MonitorUp className="mr-2 inline size-4" />{copy.start}</ActionButton> : <ActionButton tone="danger" onClick={stop}><Square className="mr-2 inline size-4" />{copy.stop}</ActionButton>}
      </div>
      {videoUrl ? <div className="mt-4"><video src={videoUrl} controls className="max-h-[520px] w-full rounded-[14px] bg-black" /><a href={videoUrl} download={downloadName} className="mt-3 inline-flex min-h-10 items-center rounded-[11px] bg-emerald-700 px-3.5 text-sm font-bold text-white"><Download className="mr-2 size-4" />{copy.download}</a></div> : null}
    </Panel>
    <p className="rounded-[14px] bg-amber-500/10 p-3 text-xs text-amber-800 dark:text-amber-200">{copy.note}</p>
  </div>
}

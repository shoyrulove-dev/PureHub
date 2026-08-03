import { useEffect, useMemo, useRef, useState } from 'react'
import jsQR from 'jsqr'
import { Camera, Clipboard, Download, ExternalLink, Flashlight, ImageUp, ShieldAlert, Trash2 } from 'lucide-react'
import { ActionButton, FormInput, FormTextArea, Panel } from '../MiniAppPrimitives'

const STORAGE_KEY = 'purehub.qr-studio.history.v1'
const MAX_HISTORY = 12

type ScanEntry = { value: string; scannedAt: string }
type QrTemplate = 'text' | 'url' | 'wifi' | 'contact'

function loadHistory(): ScanEntry[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as ScanEntry[]
  } catch {
    return []
  }
}

function decodePixels(data: ImageData) {
  return jsQR(data.data, data.width, data.height, { inversionAttempts: 'attemptBoth' })?.data ?? ''
}

async function decodeImage(file: File) {
  const bitmap = await createImageBitmap(file)
  const canvas = document.createElement('canvas')
  canvas.width = bitmap.width
  canvas.height = bitmap.height
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return ''
  context.drawImage(bitmap, 0, 0)
  bitmap.close()
  return decodePixels(context.getImageData(0, 0, canvas.width, canvas.height))
}

function linkAssessment(value: string) {
  if (!/^https?:\/\//i.test(value)) return { url: '', warning: '' }
  try {
    const url = new URL(value)
    const warning = url.protocol !== 'https:'
      ? 'This link is not encrypted. Check it carefully before opening.'
      : /xn--|\d{1,3}(?:\.\d{1,3}){3}/i.test(url.hostname)
        ? 'This destination uses an unusual hostname. Verify it before opening.'
        : ''
    return { url: url.href, warning }
  } catch {
    return { url: '', warning: 'This looks like a link, but it is not a valid web address.' }
  }
}

export default function QrStudioSurface() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const scanCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const frameRef = useRef<number | null>(null)
  const lastValueRef = useRef('')
  const [template, setTemplate] = useState<QrTemplate>('url')
  const [qrValue, setQrValue] = useState('https://hub.blissbiovn.com')
  const [scanResult, setScanResult] = useState('')
  const [scanStatus, setScanStatus] = useState('Camera and uploaded images are processed locally on this device.')
  const [cameraActive, setCameraActive] = useState(false)
  const [torchAvailable, setTorchAvailable] = useState(false)
  const [torchOn, setTorchOn] = useState(false)
  const [history, setHistory] = useState<ScanEntry[]>(loadHistory)
  const assessment = useMemo(() => linkAssessment(scanResult), [scanResult])

  useEffect(() => {
    if (!canvasRef.current) return
    void import('qrcode').then(({ default: QRCode }) => QRCode.toCanvas(canvasRef.current, qrValue || ' ', {
      width: 240,
      margin: 2,
      color: { dark: '#0f172a', light: '#ffffff' },
    }))
  }, [qrValue])

  const rememberResult = (value: string, source: string) => {
    if (!value || value === lastValueRef.current) return
    lastValueRef.current = value
    setScanResult(value)
    setScanStatus(`QR code detected ${source}.`)
    setHistory((current) => {
      const next = [{ value, scannedAt: new Date().toISOString() }, ...current.filter((item) => item.value !== value)].slice(0, MAX_HISTORY)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }

  const stopCamera = () => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current)
    frameRef.current = null
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setCameraActive(false)
    setTorchAvailable(false)
    setTorchOn(false)
  }

  useEffect(() => stopCamera, [])

  const scanFrame = () => {
    const video = videoRef.current
    const canvas = scanCanvasRef.current
    if (!video || !canvas || video.readyState < 2) {
      frameRef.current = requestAnimationFrame(scanFrame)
      return
    }
    const width = Math.min(video.videoWidth, 960)
    const scale = width / video.videoWidth
    canvas.width = width
    canvas.height = Math.round(video.videoHeight * scale)
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (context) {
      context.drawImage(video, 0, 0, canvas.width, canvas.height)
      const value = decodePixels(context.getImageData(0, 0, canvas.width, canvas.height))
      if (value) rememberResult(value, 'with the live camera')
    }
    frameRef.current = requestAnimationFrame(scanFrame)
  }

  const startCamera = async () => {
    try {
      stopCamera()
      lastValueRef.current = ''
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
      const track = stream.getVideoTracks()[0]
      const capabilities = track.getCapabilities() as MediaTrackCapabilities & { torch?: boolean }
      setTorchAvailable(Boolean(capabilities.torch))
      setCameraActive(true)
      setScanStatus('Point the camera at a QR code. Scanning stays on this device.')
      frameRef.current = requestAnimationFrame(scanFrame)
    } catch {
      setScanStatus('Camera access was unavailable. Allow camera permission or scan from an image instead.')
    }
  }

  const toggleTorch = async () => {
    const track = streamRef.current?.getVideoTracks()[0]
    if (!track) return
    const next = !torchOn
    await track.applyConstraints({ advanced: [{ torch: next } as MediaTrackConstraintSet] })
    setTorchOn(next)
  }

  const handleScanFile = async (file?: File) => {
    if (!file) return
    setScanStatus('Reading the selected image locally...')
    try {
      const value = await decodeImage(file)
      if (value) rememberResult(value, 'from the uploaded image')
      else setScanStatus('No readable QR code was found. Try a sharper, uncropped image.')
    } catch {
      setScanStatus('That image could not be read. Try PNG, JPG, or a camera photo.')
    }
  }

  const applyTemplate = (next: QrTemplate) => {
    setTemplate(next)
    const examples: Record<QrTemplate, string> = {
      text: 'PureHub — free, private, and ad-free tools',
      url: 'https://hub.blissbiovn.com',
      wifi: 'WIFI:T:WPA;S:Network name;P:Password;;',
      contact: 'MECARD:N:PureHub;URL:https://hub.blissbiovn.com;;',
    }
    setQrValue(examples[next])
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[0.92fr_1.08fr]">
      <Panel title="QR Creator" subtitle="Create URLs, Wi-Fi cards, contacts, or plain text without uploading anything.">
        <div className="flex flex-wrap gap-2">
          {(['url', 'text', 'wifi', 'contact'] as const).map((item) => (
            <ActionButton key={item} tone={template === item ? 'primary' : 'muted'} onClick={() => applyTemplate(item)}>{item.toUpperCase()}</ActionButton>
          ))}
        </div>
        <div className="mt-3"><FormTextArea aria-label="QR content" rows={4} value={qrValue} onChange={(event) => setQrValue(event.target.value)} /></div>
        <div className="mt-4 flex flex-col items-center gap-3 rounded-[20px] border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-950">
          <canvas ref={canvasRef} className="max-w-full rounded-xl" />
          <ActionButton onClick={() => {
            const href = canvasRef.current?.toDataURL('image/png')
            if (!href) return
            const link = document.createElement('a'); link.href = href; link.download = 'purehub-qr.png'; link.click()
          }}><Download className="size-4" /> Download PNG</ActionButton>
        </div>
      </Panel>

      <Panel title="Private QR Scanner" subtitle="Live camera plus universal image fallback. No scan leaves your device.">
        <div className="overflow-hidden rounded-[20px] border border-slate-200 bg-slate-950 dark:border-slate-700">
          <video ref={videoRef} playsInline muted className={`aspect-[4/3] w-full object-cover ${cameraActive ? 'block' : 'hidden'}`} />
          {!cameraActive ? <div className="grid aspect-[4/3] place-items-center text-center text-sm text-slate-300"><Camera className="mx-auto mb-3 size-9 text-emerald-300" />Start the camera when you are ready.</div> : null}
          <canvas ref={scanCanvasRef} className="hidden" />
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <ActionButton onClick={cameraActive ? stopCamera : startCamera}><Camera className="size-4" />{cameraActive ? 'Stop camera' : 'Start camera'}</ActionButton>
          {torchAvailable ? <ActionButton tone="muted" onClick={() => void toggleTorch()}><Flashlight className="size-4" />{torchOn ? 'Torch off' : 'Torch on'}</ActionButton> : null}
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-slate-500/20 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
            <ImageUp className="size-4" /> Scan image
            <FormInput className="sr-only" type="file" accept="image/*" onChange={(event) => void handleScanFile(event.target.files?.[0])} />
          </label>
        </div>
        <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{scanStatus}</p>
        {scanResult ? <div className="mt-3 rounded-[18px] border border-emerald-300/30 bg-emerald-50 p-4 dark:bg-emerald-950/30">
          <p className="break-all text-sm font-medium text-slate-900 dark:text-white">{scanResult}</p>
          {assessment.warning ? <p className="mt-2 flex gap-2 text-xs font-semibold text-amber-700 dark:text-amber-300"><ShieldAlert className="size-4 shrink-0" />{assessment.warning}</p> : null}
          <div className="mt-3 flex flex-wrap gap-2">
            <ActionButton tone="muted" onClick={() => void navigator.clipboard.writeText(scanResult)}><Clipboard className="size-4" /> Copy</ActionButton>
            {assessment.url ? <ActionButton onClick={() => window.open(assessment.url, '_blank', 'noopener,noreferrer')}><ExternalLink className="size-4" /> Open checked link</ActionButton> : null}
          </div>
        </div> : null}
        {history.length ? <details className="mt-4 rounded-[18px] border border-slate-500/15 p-3">
          <summary className="cursor-pointer text-sm font-bold text-slate-800 dark:text-slate-100">Recent scans ({history.length})</summary>
          <div className="mt-3 space-y-2">{history.map((item) => <button key={`${item.scannedAt}-${item.value}`} type="button" className="block w-full truncate rounded-xl bg-slate-500/5 px-3 py-2 text-left text-xs text-slate-600 dark:text-slate-300" title={item.value} onClick={() => setScanResult(item.value)}>{item.value}</button>)}</div>
          <button type="button" className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-rose-600" onClick={() => { localStorage.removeItem(STORAGE_KEY); setHistory([]) }}><Trash2 className="size-3.5" /> Clear history</button>
        </details> : null}
      </Panel>
    </div>
  )
}

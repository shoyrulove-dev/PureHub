import { useEffect, useMemo, useRef, useState } from 'react'
import jsQR from 'jsqr'
import {
  Camera, Check, Clipboard, Download, ExternalLink, Flashlight, History, ImageUp,
  QrCode, RefreshCw, ScanLine, Share2, ShieldCheck, Sparkles, Trash2,
} from 'lucide-react'
import { ActionButton, FormTextArea } from '../MiniAppPrimitives'

const STORAGE_KEY = 'purehub.qr-studio.history.v2'
const MAX_HISTORY = 24

type StudioTab = 'scan' | 'create' | 'library'
type ScanSource = 'Camera' | 'Image' | 'Created'
type ScanEntry = { value: string; savedAt: string; source: ScanSource }
type QrTemplate = 'url' | 'text' | 'wifi' | 'email' | 'phone' | 'contact'

const templates: Record<QrTemplate, { label: string; value: string }> = {
  url: { label: 'Website', value: 'https://hub.blissbiovn.com' },
  text: { label: 'Text', value: 'PureHub — free, private, and ad-free tools' },
  wifi: { label: 'Wi-Fi', value: 'WIFI:T:WPA;S:Network name;P:Password;H:false;;' },
  email: { label: 'Email', value: 'mailto:hello@example.com?subject=Hello' },
  phone: { label: 'Phone', value: 'tel:+10000000000' },
  contact: { label: 'Contact', value: 'MECARD:N:PureHub;URL:https://hub.blissbiovn.com;;' },
}

function loadHistory(): ScanEntry[] {
  try {
    const rows = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as Partial<ScanEntry>[]
    return rows.filter((row) => row.value).map((row) => ({
      value: String(row.value),
      savedAt: String(row.savedAt ?? new Date().toISOString()),
      source: row.source === 'Image' || row.source === 'Created' ? row.source : 'Camera',
    }))
  } catch {
    return []
  }
}

function decodePixels(data: ImageData) {
  return jsQR(data.data, data.width, data.height, { inversionAttempts: 'attemptBoth' })?.data ?? ''
}

async function decodeImage(file: File) {
  const bitmap = await createImageBitmap(file)
  const longest = Math.max(bitmap.width, bitmap.height)
  const scale = Math.min(1, 1600 / longest)
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bitmap.width * scale))
  canvas.height = Math.max(1, Math.round(bitmap.height * scale))
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return ''
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  return decodePixels(context.getImageData(0, 0, canvas.width, canvas.height))
}

function payloadDetails(value: string) {
  const trimmed = value.trim()
  if (/^https?:\/\//i.test(trimmed)) {
    try {
      const url = new URL(trimmed)
      const suspicious = url.protocol !== 'https:' || /xn--|^\d{1,3}(?:\.\d{1,3}){3}$/i.test(url.hostname)
      return {
        kind: 'Website',
        destination: url.href,
        action: 'Open website',
        warning: suspicious ? 'Check this address carefully before opening it.' : '',
      }
    } catch {
      return { kind: 'Invalid link', destination: '', action: '', warning: 'This looks like a web link but is not valid.' }
    }
  }
  if (/^WIFI:/i.test(trimmed)) return { kind: 'Wi-Fi network', destination: '', action: '', warning: 'Passwords remain visible in the raw QR content.' }
  if (/^mailto:/i.test(trimmed)) return { kind: 'Email', destination: trimmed, action: 'Open email', warning: '' }
  if (/^tel:/i.test(trimmed)) return { kind: 'Phone number', destination: trimmed, action: 'Open dialer', warning: '' }
  if (/^sms:/i.test(trimmed)) return { kind: 'Message', destination: trimmed, action: 'Open messages', warning: '' }
  if (/^geo:/i.test(trimmed)) return { kind: 'Location', destination: trimmed, action: 'Open map', warning: '' }
  if (/^(MECARD:|BEGIN:VCARD)/i.test(trimmed)) return { kind: 'Contact card', destination: '', action: '', warning: '' }
  return { kind: 'Plain text', destination: '', action: '', warning: '' }
}

export default function QrStudioSurface() {
  const qrCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const scanCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const frameRef = useRef<number | null>(null)
  const lastFrameRef = useRef(0)
  const scanLockedRef = useRef(false)
  const [tab, setTab] = useState<StudioTab>('scan')
  const [template, setTemplate] = useState<QrTemplate>('url')
  const [qrValue, setQrValue] = useState(templates.url.value)
  const [foreground, setForeground] = useState('#0f172a')
  const [scanResult, setScanResult] = useState('')
  const [scanStatus, setScanStatus] = useState('Ready when you are. Nothing is uploaded.')
  const [cameraActive, setCameraActive] = useState(false)
  const [torchAvailable, setTorchAvailable] = useState(false)
  const [torchOn, setTorchOn] = useState(false)
  const [copied, setCopied] = useState(false)
  const [history, setHistory] = useState<ScanEntry[]>(loadHistory)
  const resultDetails = useMemo(() => payloadDetails(scanResult), [scanResult])

  useEffect(() => {
    if (!qrCanvasRef.current) return
    void import('qrcode').then(({ default: QRCode }) => QRCode.toCanvas(qrCanvasRef.current, qrValue || ' ', {
      width: 320,
      margin: 3,
      errorCorrectionLevel: 'M',
      color: { dark: foreground, light: '#ffffff' },
    }))
  }, [foreground, qrValue])

  const saveEntry = (value: string, source: ScanSource) => {
    if (!value.trim()) return
    setHistory((current) => {
      const next = [{ value: value.trim(), savedAt: new Date().toISOString(), source }, ...current.filter((item) => item.value !== value.trim())].slice(0, MAX_HISTORY)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }

  const rememberResult = (value: string, source: ScanSource) => {
    if (!value || scanLockedRef.current) return
    scanLockedRef.current = true
    setScanResult(value)
    setScanStatus(`${source} scan complete. Review the result before taking action.`)
    saveEntry(value, source)
    navigator.vibrate?.(45)
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

  const scanFrame = (timestamp: number) => {
    const video = videoRef.current
    const canvas = scanCanvasRef.current
    if (!scanLockedRef.current && video && canvas && video.readyState >= 2 && timestamp - lastFrameRef.current > 140) {
      lastFrameRef.current = timestamp
      const width = Math.min(video.videoWidth, 960)
      const scale = width / video.videoWidth
      canvas.width = width
      canvas.height = Math.round(video.videoHeight * scale)
      const context = canvas.getContext('2d', { willReadFrequently: true })
      if (context) {
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        rememberResult(decodePixels(context.getImageData(0, 0, canvas.width, canvas.height)), 'Camera')
      }
    }
    frameRef.current = requestAnimationFrame(scanFrame)
  }

  const startCamera = async () => {
    try {
      stopCamera()
      scanLockedRef.current = false
      setScanResult('')
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      })
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
      const capabilities = stream.getVideoTracks()[0]?.getCapabilities() as MediaTrackCapabilities & { torch?: boolean }
      setTorchAvailable(Boolean(capabilities?.torch))
      setCameraActive(true)
      setScanStatus('Hold a QR code inside the frame. Detection is automatic.')
      frameRef.current = requestAnimationFrame(scanFrame)
    } catch {
      setScanStatus('Camera unavailable. Allow permission or choose an image instead.')
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
    scanLockedRef.current = false
    setScanStatus('Reading the image locally…')
    try {
      const value = await decodeImage(file)
      if (value) rememberResult(value, 'Image')
      else setScanStatus('No readable QR code found. Try a sharper or less cropped image.')
    } catch {
      setScanStatus('That image could not be read. Try PNG, JPG, or a camera photo.')
    }
  }

  const resetScanner = () => {
    scanLockedRef.current = false
    setScanResult('')
    setCopied(false)
    setScanStatus(cameraActive ? 'Hold a QR code inside the frame. Detection is automatic.' : 'Ready for another scan.')
  }

  const qrPng = () => qrCanvasRef.current?.toDataURL('image/png') ?? ''
  const downloadQr = () => {
    const href = qrPng()
    if (!href) return
    const link = document.createElement('a')
    link.href = href
    link.download = `purehub-${template}-qr.png`
    link.click()
  }

  const shareQr = async () => {
    const href = qrPng()
    if (!href) return
    const blob = await (await fetch(href)).blob()
    const file = new File([blob], 'purehub-qr.png', { type: 'image/png' })
    if (navigator.canShare?.({ files: [file] })) await navigator.share({ title: 'PureHub QR', files: [file] })
    else downloadQr()
  }

  const shareResult = async () => {
    if (navigator.share) {
      await navigator.share({ text: scanResult })
      return
    }
    await navigator.clipboard.writeText(scanResult)
    setCopied(true)
  }

  return (
    <section className="overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-[0_24px_70px_-45px_rgba(15,23,42,.5)] dark:border-slate-700 dark:bg-slate-900">
      <header className="bg-gradient-to-br from-emerald-50 via-white to-cyan-50 px-4 py-5 sm:px-6 dark:from-emerald-950/45 dark:via-slate-900 dark:to-cyan-950/30">
        <div className="flex items-start justify-between gap-4">
          <div><p className="text-xs font-black uppercase tracking-[.2em] text-emerald-700 dark:text-emerald-300">Private by design</p><h2 className="mt-1 text-2xl font-black tracking-tight text-slate-950 dark:text-white">QR Studio</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Scan, inspect, create, and keep useful codes—without ads or uploads.</p></div>
          <div className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-950 text-white shadow-lg dark:bg-emerald-400 dark:text-slate-950"><QrCode className="size-6" /></div>
        </div>
        <nav className="mt-5 grid grid-cols-3 rounded-2xl border border-slate-200 bg-white/80 p-1 dark:border-slate-700 dark:bg-slate-950/70" aria-label="QR Studio sections">
          {([
            ['scan', ScanLine, 'Scan'], ['create', Sparkles, 'Create'], ['library', History, 'Library'],
          ] as const).map(([value, Icon, label]) => <button key={value} type="button" onClick={() => setTab(value)} className={`flex min-h-11 items-center justify-center gap-2 rounded-xl text-sm font-bold transition ${tab === value ? 'bg-slate-950 text-white shadow-sm dark:bg-emerald-400 dark:text-slate-950' : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}><Icon className="size-4" />{label}</button>)}
        </nav>
      </header>

      <div className="p-4 sm:p-6">
        {tab === 'scan' ? <div className="mx-auto max-w-2xl">
          <div className="relative overflow-hidden rounded-[24px] bg-slate-950 shadow-inner">
            <video ref={videoRef} playsInline muted className={`aspect-[4/3] w-full object-cover ${cameraActive ? 'block' : 'hidden'}`} />
            {!cameraActive ? <div className="grid aspect-[4/3] place-items-center px-8 text-center text-slate-300"><div><div className="mx-auto grid size-16 place-items-center rounded-full bg-white/10"><Camera className="size-7 text-emerald-300" /></div><p className="mt-4 font-bold text-white">Camera stays off until you start it</p><p className="mt-1 text-sm text-slate-400">You can also scan a screenshot or saved image.</p></div></div> : null}
            <div className="pointer-events-none absolute inset-0 grid place-items-center"><div className={`relative aspect-square w-[58%] max-w-64 rounded-[28px] border-2 ${scanResult ? 'border-emerald-400' : 'border-white/75'} shadow-[0_0_0_999px_rgba(2,6,23,.28)]`}><span className="absolute -left-0.5 -top-0.5 size-8 rounded-tl-[26px] border-l-4 border-t-4 border-emerald-400"/><span className="absolute -right-0.5 -top-0.5 size-8 rounded-tr-[26px] border-r-4 border-t-4 border-emerald-400"/><span className="absolute -bottom-0.5 -left-0.5 size-8 rounded-bl-[26px] border-b-4 border-l-4 border-emerald-400"/><span className="absolute -bottom-0.5 -right-0.5 size-8 rounded-br-[26px] border-b-4 border-r-4 border-emerald-400"/>{cameraActive && !scanResult ? <span className="absolute left-[8%] right-[8%] top-1/2 h-0.5 bg-emerald-300 shadow-[0_0_16px_3px_rgba(110,231,183,.7)]"/> : null}{scanResult ? <span className="absolute inset-0 grid place-items-center"><span className="grid size-12 place-items-center rounded-full bg-emerald-400 text-slate-950"><Check className="size-6" /></span></span> : null}</div></div>
            <canvas ref={scanCanvasRef} className="hidden" />
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2 sm:flex sm:justify-center">
            <ActionButton className="justify-center" onClick={cameraActive ? stopCamera : startCamera}><Camera className="size-4" />{cameraActive ? 'Stop' : 'Start camera'}</ActionButton>
            <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-800 dark:border-slate-600 dark:text-slate-100"><ImageUp className="size-4" />Choose image<input className="sr-only" type="file" accept="image/*" onChange={(event) => void handleScanFile(event.target.files?.[0])} /></label>
            {torchAvailable ? <ActionButton className="justify-center" tone="muted" onClick={() => void toggleTorch()}><Flashlight className="size-4" />{torchOn ? 'Torch off' : 'Torch'}</ActionButton> : null}
          </div>
          <p className="mt-3 text-center text-sm text-slate-500 dark:text-slate-400">{scanStatus}</p>
          {scanResult ? <article className="mt-4 rounded-[22px] border border-emerald-200 bg-emerald-50/70 p-4 dark:border-emerald-800 dark:bg-emerald-950/25">
            <div className="flex items-center justify-between gap-3"><span className="rounded-full bg-emerald-700 px-2.5 py-1 text-[11px] font-black uppercase tracking-wide text-white dark:bg-emerald-400 dark:text-slate-950">{resultDetails.kind}</span><span className="flex items-center gap-1 text-xs font-bold text-emerald-800 dark:text-emerald-200"><ShieldCheck className="size-3.5" />Read locally</span></div>
            <p className="mt-3 max-h-32 overflow-auto break-all rounded-xl bg-white/80 p-3 text-sm font-semibold text-slate-900 dark:bg-slate-950/70 dark:text-white">{scanResult}</p>
            {resultDetails.warning ? <p className="mt-2 text-xs font-bold text-amber-700 dark:text-amber-300">{resultDetails.warning}</p> : null}
            <div className="mt-3 grid grid-cols-2 gap-2 sm:flex">
              <ActionButton tone="muted" className="justify-center" onClick={() => { void navigator.clipboard.writeText(scanResult); setCopied(true) }}>{copied ? <Check className="size-4" /> : <Clipboard className="size-4" />}{copied ? 'Copied' : 'Copy'}</ActionButton>
              <ActionButton tone="muted" className="justify-center" onClick={() => void shareResult()}><Share2 className="size-4" />Share</ActionButton>
              {resultDetails.destination ? <ActionButton className="col-span-2 justify-center" onClick={() => window.open(resultDetails.destination, '_blank', 'noopener,noreferrer')}><ExternalLink className="size-4" />{resultDetails.action}</ActionButton> : null}
              <ActionButton tone="muted" className="col-span-2 justify-center" onClick={resetScanner}><RefreshCw className="size-4" />Scan another</ActionButton>
            </div>
          </article> : null}
        </div> : null}

        {tab === 'create' ? <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
          <div><h3 className="text-lg font-black text-slate-950 dark:text-white">Create a code</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Choose a starting format, then edit the raw content before export.</p>
            <div className="mt-4 flex flex-wrap gap-2">{(Object.keys(templates) as QrTemplate[]).map((item) => <button key={item} type="button" onClick={() => { setTemplate(item); setQrValue(templates[item].value) }} className={`min-h-10 rounded-full border px-3 text-xs font-black ${template === item ? 'border-emerald-700 bg-emerald-700 text-white dark:border-emerald-400 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-300 text-slate-600 dark:border-slate-600 dark:text-slate-300'}`}>{templates[item].label}</button>)}</div>
            <FormTextArea className="mt-4 min-h-36" aria-label="QR content" value={qrValue} onChange={(event) => setQrValue(event.target.value)} />
            <div className="mt-3 flex items-center gap-3"><label className="text-sm font-bold text-slate-600 dark:text-slate-300">Code color</label><input type="color" value={foreground} onChange={(event) => setForeground(event.target.value)} className="size-10 cursor-pointer rounded-lg border-0 bg-transparent" /><button type="button" className="text-xs font-bold text-slate-500" onClick={() => setForeground('#0f172a')}>Reset</button></div>
          </div>
          <div className="flex flex-col items-center rounded-[24px] border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-950"><div className="rounded-[20px] bg-white p-3 shadow-sm"><canvas ref={qrCanvasRef} className="h-auto w-full max-w-[300px]" /></div><p className="mt-3 text-center text-xs font-semibold text-slate-500">High-contrast PNG · works with standard scanners</p><div className="mt-4 grid w-full grid-cols-2 gap-2"><ActionButton className="justify-center" onClick={downloadQr}><Download className="size-4" />Download</ActionButton><ActionButton tone="muted" className="justify-center" onClick={() => void shareQr()}><Share2 className="size-4" />Share</ActionButton><ActionButton tone="muted" className="col-span-2 justify-center" onClick={() => saveEntry(qrValue, 'Created')}><History className="size-4" />Save to library</ActionButton></div></div>
        </div> : null}

        {tab === 'library' ? <div><div className="flex items-center justify-between gap-3"><div><h3 className="text-lg font-black text-slate-950 dark:text-white">Private library</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Stored only in this browser. Nothing is synced.</p></div>{history.length ? <button type="button" className="inline-flex min-h-10 items-center gap-1.5 rounded-xl px-3 text-xs font-black text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30" onClick={() => { localStorage.removeItem(STORAGE_KEY); setHistory([]) }}><Trash2 className="size-4" />Clear</button> : null}</div>
          {history.length ? <div className="mt-4 grid gap-2 sm:grid-cols-2">{history.map((item) => <button key={`${item.savedAt}-${item.value}`} type="button" className="group flex min-w-0 items-center gap-3 rounded-2xl border border-slate-200 p-3 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 dark:border-slate-700 dark:hover:border-emerald-700 dark:hover:bg-emerald-950/20" onClick={() => { setScanResult(item.value); setTab('scan'); scanLockedRef.current = true }}><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-700 group-hover:bg-emerald-100 group-hover:text-emerald-800 dark:bg-slate-800 dark:text-slate-200"><QrCode className="size-5" /></span><span className="min-w-0"><span className="block truncate text-sm font-bold text-slate-900 dark:text-white">{item.value}</span><span className="mt-0.5 block text-xs text-slate-500">{item.source} · {new Date(item.savedAt).toLocaleString()}</span></span></button>)}</div> : <div className="mt-5 grid min-h-48 place-items-center rounded-[22px] border border-dashed border-slate-300 text-center dark:border-slate-700"><div><History className="mx-auto size-8 text-slate-400"/><p className="mt-2 font-bold text-slate-700 dark:text-slate-200">No saved codes yet</p><p className="mt-1 text-sm text-slate-500">Scans and codes you save will appear here.</p></div></div>}
        </div> : null}
      </div>
    </section>
  )
}

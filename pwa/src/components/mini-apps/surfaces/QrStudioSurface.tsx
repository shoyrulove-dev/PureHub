import { useEffect, useMemo, useRef, useState } from 'react'
import jsQR from 'jsqr'
import {
  Camera, Check, Clipboard, Download, ExternalLink, Flashlight, History, ImageUp,
  QrCode, RefreshCw, ScanLine, Share2, ShieldCheck, Sparkles, Trash2,
} from 'lucide-react'
import { ActionButton, FormInput, FormTextArea } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

const STORAGE_KEY = 'purehub.qr-studio.history.v2'
const MAX_HISTORY = 24

type StudioTab = 'scan' | 'create' | 'library'
type ScanSource = 'Camera' | 'Image' | 'Created'
type ScanEntry = { value: string; savedAt: string; source: ScanSource; format?: string }
type QrTemplate = 'url' | 'text' | 'wifi' | 'email' | 'phone' | 'sms' | 'location' | 'calendar' | 'contact'
type CreatorFields = { primary: string; secondary: string; tertiary: string }
type DetectedCode = { value: string; format: string }
type NativeBarcode = { rawValue: string; format: string }
type NativeBarcodeDetector = { detect: (source: CanvasImageSource) => Promise<NativeBarcode[]> }
type NativeBarcodeDetectorConstructor = new (options?: { formats?: string[] }) => NativeBarcodeDetector

const templates: Record<QrTemplate, { label: string; value: string }> = {
  url: { label: 'Website', value: 'https://hub.blissbiovn.com' },
  text: { label: 'Text', value: 'PureHub — free, private, and ad-free tools' },
  wifi: { label: 'Wi-Fi', value: 'WIFI:T:WPA;S:Network name;P:Password;H:false;;' },
  email: { label: 'Email', value: 'mailto:hello@example.com?subject=Hello' },
  phone: { label: 'Phone', value: 'tel:+10000000000' },
  sms: { label: 'SMS', value: 'SMSTO:+10000000000:Hello' },
  location: { label: 'Location', value: 'geo:10.7769,106.7009' },
  calendar: { label: 'Calendar', value: 'BEGIN:VEVENT\nSUMMARY:PureHub event\nDTSTART:20260821T090000\nEND:VEVENT' },
  contact: { label: 'Contact', value: 'MECARD:N:PureHub;URL:https://hub.blissbiovn.com;;' },
}

const defaultFields: Record<QrTemplate, CreatorFields> = {
  url: { primary: 'https://hub.blissbiovn.com', secondary: '', tertiary: '' },
  text: { primary: 'PureHub — free, private, and ad-free tools', secondary: '', tertiary: '' },
  wifi: { primary: '', secondary: '', tertiary: 'WPA' },
  email: { primary: '', secondary: '', tertiary: '' },
  phone: { primary: '', secondary: '', tertiary: '' },
  sms: { primary: '', secondary: '', tertiary: '' },
  location: { primary: '', secondary: '', tertiary: '' },
  calendar: { primary: '', secondary: '', tertiary: '' },
  contact: { primary: '', secondary: '', tertiary: '' },
}

const escapeQrField = (value: string) => value.trim().replaceAll('\\', '\\\\').replaceAll(';', '\\;').replaceAll(',', '\\,').replaceAll(':', '\\:')

function buildQrValue(template: QrTemplate, fields: CreatorFields) {
  if (template === 'url' || template === 'text') return fields.primary.trim()
  if (template === 'wifi') return `WIFI:T:${fields.tertiary === 'None' ? '' : fields.tertiary};S:${escapeQrField(fields.primary)};P:${escapeQrField(fields.secondary)};H:false;;`
  if (template === 'email') return `mailto:${fields.primary.trim()}?subject=${encodeURIComponent(fields.secondary.trim())}`
  if (template === 'phone') return `tel:${fields.primary.replaceAll(/\s/g, '')}`
  if (template === 'sms') return `SMSTO:${fields.primary.replaceAll(/\s/g, '')}:${fields.secondary.trim()}`
  if (template === 'location') return `geo:${fields.primary.trim()}`
  if (template === 'calendar') return `BEGIN:VEVENT\nSUMMARY:${fields.primary.trim()}\nDTSTART:${fields.secondary.replaceAll(/[-:]/g, '').replace('T', 'T')}\n${fields.tertiary.trim() ? `LOCATION:${fields.tertiary.trim()}\n` : ''}END:VEVENT`
  return `MECARD:N:${escapeQrField(fields.primary)}${fields.secondary ? `;TEL:${escapeQrField(fields.secondary)}` : ''}${fields.tertiary ? `;EMAIL:${escapeQrField(fields.tertiary)}` : ''};;`
}

function loadHistory(): ScanEntry[] {
  try {
    const rows = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as Partial<ScanEntry>[]
    return rows.filter((row) => row.value).map((row) => ({
      value: String(row.value),
      savedAt: String(row.savedAt ?? new Date().toISOString()),
      source: row.source === 'Image' || row.source === 'Created' ? row.source : 'Camera',
      format: typeof row.format === 'string' ? row.format : undefined,
    }))
  } catch {
    return []
  }
}

let zxingReaderPromise: Promise<InstanceType<(typeof import('@zxing/browser'))['BrowserMultiFormatReader']>> | undefined

function nativeBarcodeDetector() {
  const Detector = (globalThis as typeof globalThis & { BarcodeDetector?: NativeBarcodeDetectorConstructor }).BarcodeDetector
  return Detector ? new Detector({ formats: ['qr_code', 'aztec', 'data_matrix', 'pdf417', 'code_128', 'code_39', 'code_93', 'ean_13', 'ean_8', 'upc_a', 'upc_e', 'itf'] }) : null
}

async function decodeCanvas(canvas: HTMLCanvasElement, context: CanvasRenderingContext2D): Promise<DetectedCode | null> {
  const native = nativeBarcodeDetector()
  if (native) {
    try {
      const [result] = await native.detect(canvas)
      if (result?.rawValue) return { value: result.rawValue, format: result.format.replaceAll('_', ' ').toUpperCase() }
    } catch { /* Native support can be partial; continue with local fallbacks. */ }
  }
  const qr = jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })
  if (qr?.data) return { value: qr.data, format: 'QR CODE' }
  try {
    zxingReaderPromise ??= import('@zxing/browser').then(({ BrowserMultiFormatReader }) => new BrowserMultiFormatReader())
    const result = (await zxingReaderPromise).decodeFromCanvas(canvas)
    return result.getText() ? { value: result.getText(), format: result.getBarcodeFormat().toString().replaceAll('_', ' ') } : null
  } catch {
    return null
  }
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
  return decodeCanvas(canvas, context)
}

function payloadDetails(value: string) {
  const trimmed = value.trim().replace(/^URL:/i, '').trim()
  const webCandidate = /^https?:\/\//i.test(trimmed) ? trimmed : /^(?:www\.|[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,}(?:[/:?#]|$)/i.test(trimmed) ? `https://${trimmed}` : ''
  if (webCandidate) {
    try {
      const url = new URL(webCandidate)
      const risks = [
        url.protocol !== 'https:' ? 'The link is not encrypted (HTTP).' : '',
        /^\d{1,3}(?:\.\d{1,3}){3}$/i.test(url.hostname) ? 'The destination uses a raw IP address.' : '',
        /xn--/i.test(url.hostname) ? 'The domain contains an internationalized/punycode name.' : '',
        url.username || url.password ? 'The link embeds sign-in information.' : '',
        url.port && !['80', '443'].includes(url.port) ? `The link uses unusual port ${url.port}.` : '',
        webCandidate.length > 500 ? 'The destination is unusually long.' : '',
      ].filter(Boolean)
      return {
        kind: 'Website',
        destination: url.href,
        action: 'Open website',
        warning: risks.length ? risks.join(' ') : '',
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
  if (/^(?:\d{8}|\d{12,14})$/.test(trimmed)) return { kind: 'Product barcode', destination: `https://www.google.com/search?q=${encodeURIComponent(trimmed)}`, action: 'Search product online', warning: 'Product lookup opens a search engine and shares this barcode only after you confirm.' }
  return { kind: 'Plain text', destination: '', action: '', warning: '' }
}

function contrastRatio(foreground: string, background: string) {
  const luminance = (hex: string) => {
    const channels = hex.slice(1).match(/.{2}/g)?.map((value) => parseInt(value, 16) / 255) ?? [0, 0, 0]
    const [r, g, b] = channels.map((value) => value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  }
  const first = luminance(foreground); const second = luminance(background)
  return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05)
}

export default function QrStudioSurface() {
  const qrCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const scanCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const frameRef = useRef<number | null>(null)
  const lastFrameRef = useRef(0)
  const scanLockedRef = useRef(false)
  const scanBusyRef = useRef(false)
  const [tab, setTab] = useState<StudioTab>('scan')
  const [template, setTemplate] = useState<QrTemplate>('url')
  const [creatorFields, setCreatorFields] = useState<CreatorFields>(defaultFields.url)
  const [foreground, setForeground] = useState('#0f172a')
  const [background, setBackground] = useState('#ffffff')
  const [batchStatus, setBatchStatus] = useState('')
  const [scanResult, setScanResult] = useState('')
  const [scanFormat, setScanFormat] = useState('')
  const [scanStatus, setScanStatus] = useState('Ready when you are. Nothing is uploaded.')
  const [cameraActive, setCameraActive] = useState(false)
  const [torchAvailable, setTorchAvailable] = useState(false)
  const [torchOn, setTorchOn] = useState(false)
  const [zoomRange, setZoomRange] = useState<{ min: number; max: number; step: number } | null>(null)
  const [zoom, setZoom] = useState(1)
  const [copied, setCopied] = useState(false)
  const [openConfirmed, setOpenConfirmed] = useState(false)
  const [creatorVerified, setCreatorVerified] = useState(false)
  const [history, setHistory] = useState<ScanEntry[]>(loadHistory)
  const qrValue = useMemo(() => buildQrValue(template, creatorFields), [creatorFields, template])
  const resultDetails = useMemo(() => payloadDetails(scanResult), [scanResult])
  const contrast = useMemo(() => contrastRatio(foreground, background), [background, foreground])

  useEffect(() => {
    if (!qrCanvasRef.current) return
    setCreatorVerified(false)
    void import('qrcode').then(async ({ default: QRCode }) => {
      await QRCode.toCanvas(qrCanvasRef.current, qrValue || ' ', { width: 320, margin: 3, errorCorrectionLevel: 'M', color: { dark: foreground, light: background } })
      const canvas = qrCanvasRef.current
      const context = canvas?.getContext('2d', { willReadFrequently: true })
      if (!canvas || !context || !qrValue.trim()) return
      const result = jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })
      setCreatorVerified(result?.data === qrValue)
    })
  }, [background, foreground, qrValue])

  const saveEntry = (value: string, source: ScanSource, format = '') => {
    if (!value.trim()) return
    setHistory((current) => {
      const next = [{ value: value.trim(), savedAt: new Date().toISOString(), source, format: format || undefined }, ...current.filter((item) => item.value !== value.trim())].slice(0, MAX_HISTORY)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }

  const rememberResult = (code: DetectedCode | null, source: ScanSource) => {
    if (!code?.value || scanLockedRef.current) return
    scanLockedRef.current = true
    setScanResult(code.value)
    setScanFormat(code.format)
    setOpenConfirmed(false)
    setScanStatus(`${source} ${code.format.toLowerCase()} scan complete. Review the result before taking action.`)
    saveEntry(code.value, source, code.format)
    navigator.vibrate?.(45)
    markToolSuccess('qr-studio', { headline: 'QR code scanned locally', detail: `${source} result is saved in your private library. Review it before opening or sharing.`, shareText: 'I scanned a QR code locally with PureHub, without ads or uploads.' })
  }

  const stopCamera = () => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current)
    frameRef.current = null
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setCameraActive(false)
    setTorchAvailable(false)
    setTorchOn(false)
    setZoomRange(null)
    setZoom(1)
  }

  useEffect(() => stopCamera, [])

  const scanFrame = (timestamp: number) => {
    const video = videoRef.current
    const canvas = scanCanvasRef.current
    if (!scanLockedRef.current && !scanBusyRef.current && video && canvas && video.readyState >= 2 && timestamp - lastFrameRef.current > 180) {
      lastFrameRef.current = timestamp
      const width = Math.min(video.videoWidth, 1280)
      const scale = width / video.videoWidth
      canvas.width = width
      canvas.height = Math.round(video.videoHeight * scale)
      const context = canvas.getContext('2d', { willReadFrequently: true })
      if (context) {
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        scanBusyRef.current = true
        void decodeCanvas(canvas, context).then((code) => rememberResult(code, 'Camera')).finally(() => { scanBusyRef.current = false })
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
      const capabilities = stream.getVideoTracks()[0]?.getCapabilities() as MediaTrackCapabilities & { torch?: boolean; zoom?: { min: number; max: number; step?: number } }
      setTorchAvailable(Boolean(capabilities?.torch))
      const zoomCapabilities = capabilities?.zoom
      if (zoomCapabilities && typeof zoomCapabilities.min === 'number' && typeof zoomCapabilities.max === 'number' && zoomCapabilities.max > zoomCapabilities.min) {
        setZoomRange({ min: zoomCapabilities.min, max: zoomCapabilities.max, step: zoomCapabilities.step || .1 })
        setZoom(zoomCapabilities.min)
      }
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

  const setCameraZoom = async (next: number) => {
    const track = streamRef.current?.getVideoTracks()[0]
    if (!track) return
    try {
      await track.applyConstraints({ advanced: [{ zoom: next } as MediaTrackConstraintSet] })
      setZoom(next)
    } catch { setScanStatus('This camera does not allow zoom control in the browser.') }
  }

  const handleScanFile = async (file?: File) => {
    if (!file) return
    scanLockedRef.current = false
    setScanStatus('Reading the image locally…')
    try {
      const code = await decodeImage(file)
      if (code) rememberResult(code, 'Image')
      else setScanStatus('No readable code found. Try a sharper image, more light, or a closer crop.')
    } catch {
      setScanStatus('That image could not be read. Try PNG, JPG, or a camera photo.')
    }
  }

  const handleScanFiles = async (files?: FileList | null) => {
    const selected = Array.from(files ?? []).slice(0, 20)
    if (!selected.length) return
    let found = 0
    setScanStatus(`Reading ${selected.length} image(s) locally…`)
    for (const file of selected) {
      try {
        const code = await decodeImage(file)
        if (code) { saveEntry(code.value, 'Image', code.format); found += 1; if (!scanResult) { setScanResult(code.value); setScanFormat(code.format) } }
      } catch { /* continue with the remaining local files */ }
    }
    scanLockedRef.current = found > 0
    setBatchStatus(`${found} QR code(s) found in ${selected.length} image(s). Saved to your private library.`)
    setScanStatus(found ? 'Batch scan complete. Review the first result or open the library.' : 'No readable QR codes were found in these images.')
    if (found) markToolSuccess('qr-studio', { headline: 'Batch scan complete', detail: `${found} QR code${found === 1 ? '' : 's'} found and saved in your private library.`, shareText: `I scanned ${found} QR codes locally with PureHub.` })
  }

  const resetScanner = () => {
    scanLockedRef.current = false
    setScanResult('')
    setScanFormat('')
    setOpenConfirmed(false)
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
    markToolSuccess('qr-studio', { headline: 'QR image exported', detail: 'Your generated QR code was downloaded locally.', shareText: 'I created a QR code locally with PureHub.' })
  }

  const exportHistory = () => {
    const csv = ['value,format,source,saved_at', ...history.map((item) => `"${item.value.replaceAll('"', '""')}",${item.format ?? ''},${item.source},${item.savedAt}`)].join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'purehub-qr-library.csv'; anchor.click(); URL.revokeObjectURL(url)
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
            <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-800 dark:border-slate-600 dark:text-slate-100"><ImageUp className="size-4" />Batch<input className="sr-only" multiple type="file" accept="image/*" onChange={(event) => void handleScanFiles(event.target.files)} /></label>
            {torchAvailable ? <ActionButton className="justify-center" tone="muted" onClick={() => void toggleTorch()}><Flashlight className="size-4" />{torchOn ? 'Torch off' : 'Torch'}</ActionButton> : null}
          </div>
          {zoomRange ? <label className="mt-3 block rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-black text-slate-700 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200">Camera zoom <span className="float-right text-emerald-700 dark:text-emerald-300">{zoom.toFixed(1)}×</span><input aria-label="Camera zoom" className="mt-2 w-full accent-emerald-600" type="range" min={zoomRange.min} max={zoomRange.max} step={zoomRange.step} value={zoom} onChange={(event) => void setCameraZoom(Number(event.target.value))} /></label> : null}
          <p className="mt-3 text-center text-sm text-slate-500 dark:text-slate-400">{scanStatus}</p>
          {batchStatus ? <p className="mt-2 text-center text-xs font-bold text-emerald-700 dark:text-emerald-300">{batchStatus}</p> : null}
          {scanResult ? <article className="mt-4 rounded-[22px] border border-emerald-200 bg-emerald-50/70 p-4 dark:border-emerald-800 dark:bg-emerald-950/25">
            <div className="flex items-center justify-between gap-3"><span className="rounded-full bg-emerald-700 px-2.5 py-1 text-[11px] font-black uppercase tracking-wide text-white dark:bg-emerald-400 dark:text-slate-950">{resultDetails.kind}</span><span className="flex items-center gap-1 text-xs font-bold text-emerald-800 dark:text-emerald-200"><ShieldCheck className="size-3.5" />{scanFormat || 'CODE'} · local</span></div>
            <p className="mt-3 max-h-32 overflow-auto break-all rounded-xl bg-white/80 p-3 text-sm font-semibold text-slate-900 dark:bg-slate-950/70 dark:text-white">{scanResult}</p>
            {resultDetails.warning ? <p className="mt-2 text-xs font-bold text-amber-700 dark:text-amber-300">{resultDetails.warning}</p> : null}
            <div className="mt-3 grid grid-cols-2 gap-2 sm:flex">
              <ActionButton tone="muted" className="justify-center" onClick={() => { void navigator.clipboard.writeText(scanResult); setCopied(true) }}>{copied ? <Check className="size-4" /> : <Clipboard className="size-4" />}{copied ? 'Copied' : 'Copy'}</ActionButton>
              <ActionButton tone="muted" className="justify-center" onClick={() => void shareResult()}><Share2 className="size-4" />Share</ActionButton>
              {resultDetails.destination ? <ActionButton className="col-span-2 justify-center" onClick={() => { if (openConfirmed) window.open(resultDetails.destination, '_blank', 'noopener,noreferrer'); else setOpenConfirmed(true) }}><ExternalLink className="size-4" />{openConfirmed ? resultDetails.action : resultDetails.kind === 'Product barcode' ? 'Confirm external product search' : `Review domain, then ${resultDetails.action.toLowerCase()}`}</ActionButton> : null}
              <ActionButton tone="muted" className="col-span-2 justify-center" onClick={resetScanner}><RefreshCw className="size-4" />Scan another</ActionButton>
            </div>
          </article> : null}
        </div> : null}

        {tab === 'create' ? <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
          <div><h3 className="text-lg font-black text-slate-950 dark:text-white">Create a code</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Pick a format and fill in only the information people need.</p>
            <div className="mt-4 flex flex-wrap gap-2">{(Object.keys(templates) as QrTemplate[]).map((item) => <button key={item} type="button" onClick={() => { setTemplate(item); setCreatorFields(defaultFields[item]) }} className={`min-h-10 rounded-full border px-3 text-xs font-black ${template === item ? 'border-emerald-700 bg-emerald-700 text-white dark:border-emerald-400 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-300 text-slate-600 dark:border-slate-600 dark:text-slate-300'}`}>{templates[item].label}</button>)}</div>
            <div className="mt-4 grid gap-3">
              {template === 'text'
                ? <FormTextArea className="min-h-32" aria-label="Text" placeholder="Write something useful" value={creatorFields.primary} onChange={(event) => setCreatorFields({ ...creatorFields, primary: event.target.value })} />
                : <FormInput aria-label={template === 'wifi' ? 'Network name' : template === 'contact' ? 'Contact name' : template === 'email' ? 'Email address' : template === 'phone' || template === 'sms' ? 'Phone number' : template === 'location' ? 'Coordinates' : template === 'calendar' ? 'Event title' : 'Website address'} placeholder={template === 'wifi' ? 'Wi-Fi name' : template === 'contact' ? 'Full name' : template === 'email' ? 'hello@example.com' : template === 'phone' || template === 'sms' ? '+1 000 000 0000' : template === 'location' ? '10.7769,106.7009' : template === 'calendar' ? 'Event title' : 'https://example.com'} value={creatorFields.primary} onChange={(event) => setCreatorFields({ ...creatorFields, primary: event.target.value })} />}
              {template === 'wifi' ? <><FormInput aria-label="Wi-Fi password" placeholder="Wi-Fi password" type="password" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><select aria-label="Wi-Fi security" className="min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold dark:border-slate-600 dark:bg-slate-900" value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })}><option>WPA</option><option>WEP</option><option>None</option></select></> : null}
              {template === 'email' ? <FormInput aria-label="Email subject" placeholder="Subject" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /> : null}
              {template === 'sms' ? <FormInput aria-label="Message" placeholder="Message (optional)" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /> : null}
              {template === 'calendar' ? <><FormInput aria-label="Start time" type="datetime-local" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><FormInput aria-label="Location" placeholder="Location (optional)" value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })} /></> : null}
              {template === 'contact' ? <><FormInput aria-label="Contact phone" placeholder="Phone" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><FormInput aria-label="Contact email" placeholder="Email" value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })} /></> : null}
            </div>
            <div className="mt-3 flex flex-wrap items-center gap-3"><label className="text-sm font-bold text-slate-600 dark:text-slate-300">Code<input aria-label="QR foreground color" type="color" value={foreground} onChange={(event) => setForeground(event.target.value)} className="ml-2 size-10 cursor-pointer rounded-lg border-0 bg-transparent align-middle" /></label><label className="text-sm font-bold text-slate-600 dark:text-slate-300">Background<input aria-label="QR background color" type="color" value={background} onChange={(event) => setBackground(event.target.value)} className="ml-2 size-10 cursor-pointer rounded-lg border-0 bg-transparent align-middle" /></label><button type="button" className="text-xs font-bold text-slate-500" onClick={() => { setForeground('#0f172a'); setBackground('#ffffff') }}>Reset</button></div>
            <p className={`mt-2 rounded-xl px-3 py-2 text-xs font-bold ${contrast >= 4.5 ? 'bg-emerald-500/10 text-emerald-700' : 'bg-amber-500/15 text-amber-800'}`}>{contrast >= 4.5 ? `Scan-safe contrast · ${contrast.toFixed(1)}:1` : `Low contrast · ${contrast.toFixed(1)}:1. Darken the code or lighten the background.`}</p>
          </div>
          <div className="flex flex-col items-center rounded-[24px] border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-950"><div className="rounded-[20px] bg-white p-3 shadow-sm"><canvas ref={qrCanvasRef} className="h-auto w-full max-w-[300px]" /></div><p className={`mt-3 text-center text-xs font-semibold ${creatorVerified ? 'text-emerald-700 dark:text-emerald-300' : 'text-amber-700 dark:text-amber-300'}`}>{creatorVerified ? 'Self-tested locally · ready to scan' : 'Checking generated code locally…'}</p><div className="mt-4 grid w-full grid-cols-2 gap-2"><ActionButton className="justify-center" disabled={!creatorVerified} onClick={downloadQr}><Download className="size-4" />Download</ActionButton><ActionButton tone="muted" disabled={!creatorVerified} className="justify-center" onClick={() => void shareQr()}><Share2 className="size-4" />Share</ActionButton><ActionButton tone="muted" disabled={!creatorVerified} className="col-span-2 justify-center" onClick={() => saveEntry(qrValue, 'Created', 'QR CODE')}><History className="size-4" />Save to library</ActionButton></div></div>
        </div> : null}

        {tab === 'library' ? <div><div className="flex items-center justify-between gap-3"><div><h3 className="text-lg font-black text-slate-950 dark:text-white">Private library</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Stored only in this browser. Nothing is synced.</p></div>{history.length ? <div className="flex gap-1"><button type="button" title="Export library" className="grid size-10 place-items-center rounded-xl text-emerald-700 hover:bg-emerald-50" onClick={exportHistory}><Download className="size-4" /></button><button type="button" title="Clear library" className="grid size-10 place-items-center rounded-xl text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30" onClick={() => { localStorage.removeItem(STORAGE_KEY); setHistory([]) }}><Trash2 className="size-4" /></button></div> : null}</div>
          {history.length ? <div className="mt-4 grid gap-2 sm:grid-cols-2">{history.map((item) => <button key={`${item.savedAt}-${item.value}`} type="button" className="group flex min-w-0 items-center gap-3 rounded-2xl border border-slate-200 p-3 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 dark:border-slate-700 dark:hover:border-emerald-700 dark:hover:bg-emerald-950/20" onClick={() => { setScanResult(item.value); setScanFormat(item.format ?? ''); setTab('scan'); scanLockedRef.current = true }}><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-700 group-hover:bg-emerald-100 group-hover:text-emerald-800 dark:bg-slate-800 dark:text-slate-200"><QrCode className="size-5" /></span><span className="min-w-0"><span className="block truncate text-sm font-bold text-slate-900 dark:text-white">{item.value}</span><span className="mt-0.5 block text-xs text-slate-500">{item.format ? `${item.format} · ` : ''}{item.source} · {new Date(item.savedAt).toLocaleString()}</span></span></button>)}</div> : <div className="mt-5 grid min-h-48 place-items-center rounded-[22px] border border-dashed border-slate-300 text-center dark:border-slate-700"><div><History className="mx-auto size-8 text-slate-400"/><p className="mt-2 font-bold text-slate-700 dark:text-slate-200">No saved codes yet</p><p className="mt-1 text-sm text-slate-500">Scans and codes you save will appear here.</p></div></div>}
        </div> : null}
      </div>
    </section>
  )
}

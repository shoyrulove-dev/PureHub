import { useEffect, useMemo, useRef, useState } from 'react'
import { Activity, Camera, Compass, Download, Gauge, Lock, Maximize2, Mic, RotateCcw, ShieldCheck, Volume2, VolumeX } from 'lucide-react'
import { ActionButton } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

type SensorMode = 'compass' | 'level' | 'sound'
type PermissionState = 'idle' | 'active' | 'denied' | 'unsupported'

export default function SensorSuiteSurface({ mode }: { mode: SensorMode }) {
  const suiteRef = useRef<HTMLElement>(null)
  const locale = window.location.pathname.split('/')[1] || 'en'
  const levelLocale = levelCopy[(locale in levelCopy ? locale : 'en') as LevelLocale]
  const title = mode === 'compass' ? 'Compass' : mode === 'level' ? levelLocale.title : 'Sound Meter'

  if (mode === 'level') return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-700">
      <div><h2 className="text-lg font-black text-slate-950 dark:text-white">{title}</h2><p className="text-xs text-slate-500">{levelLocale.offline}</p></div>
      <button onClick={() => void suiteRef.current?.requestFullscreen?.()} className="grid size-11 place-items-center rounded-xl border border-sky-200 text-sky-800 dark:border-sky-800 dark:text-sky-200" aria-label={levelLocale.fullscreen}><Maximize2 className="size-5" /></button>
    </div>
    <div className="p-3 sm:p-4"><LevelPanel locale={locale} /></div>
  </section>

  return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-sky-50 via-white to-violet-50 p-5 dark:from-sky-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-start gap-3">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-sky-600 text-white">{mode === 'compass' ? <Compass /> : <Mic />}</span>
        <div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-sky-700 dark:text-sky-300">SENSOR SUITE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">{title}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Clear live readings, local calibration, and private on-device processing.</p></div>
        <ShieldCheck className="hidden size-5 text-emerald-600 sm:block" />
      </div>
      <nav className="mt-4 grid grid-cols-3 gap-2">{([['compass', 'Compass', Compass], ['level', 'Level', Gauge], ['sound', 'Sound', Mic]] as const).map(([id, label, Icon]) => <a key={id} href={`/${locale}/${id === 'level' ? 'bubble-level' : id === 'sound' ? 'decibel-meter' : 'compass'}`} className={`flex min-h-11 items-center justify-center gap-1 rounded-xl border text-xs font-black ${mode === id ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 bg-white/70 dark:border-slate-700 dark:bg-slate-800'}`}><Icon className="size-4" />{label}</a>)}</nav>
    </header>
    <div className="p-4 sm:p-5">
      <button onClick={() => void suiteRef.current?.requestFullscreen?.()} className="mb-4 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-sky-200 text-xs font-black text-sky-800 dark:border-sky-800 dark:text-sky-200"><Maximize2 className="size-4" />Fullscreen instrument</button>
      {mode === 'compass' ? <CompassPanel /> : <SoundPanel />}
    </div>
  </section>
}

function CompassPanel() {
  const [rawHeading, setRawHeading] = useState<number | null>(null)
  const [offset, setOffset] = useState(() => Number(localStorage.getItem('purehub.compass.offset.v1') || 0))
  const [accuracy, setAccuracy] = useState<'stable' | 'check'>('stable')
  const previousRef = useRef<{ value: number; at: number } | null>(null)
  const [held, setHeld] = useState<number | null>(null)
  const [permission, setPermission] = useState<PermissionState>(() => {
    if (typeof DeviceOrientationEvent === 'undefined') return 'unsupported'
    const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
    return orientation.requestPermission ? 'idle' : 'active'
  })
  const heading = held ?? (rawHeading == null ? null : (rawHeading + offset + 360) % 360)

  useEffect(() => {
    if (permission !== 'active') return
    const listener = (event: DeviceOrientationEvent & { webkitCompassHeading?: number }) => {
      const value = typeof event.webkitCompassHeading === 'number' ? event.webkitCompassHeading : event.alpha == null ? null : 360 - event.alpha
      const normalized = value == null ? null : Math.round((value + 360) % 360)
      if (normalized != null && previousRef.current) {
        const delta = Math.abs(normalized - previousRef.current.value); const shortest = Math.min(delta, 360 - delta)
        setAccuracy(shortest > 55 && Date.now() - previousRef.current.at < 500 ? 'check' : 'stable')
      }
      if (normalized != null) previousRef.current = { value: normalized, at: Date.now() }
      setRawHeading(normalized)
    }
    window.addEventListener('deviceorientation', listener as EventListener, { passive: true })
    return () => window.removeEventListener('deviceorientation', listener as EventListener)
  }, [permission])

  const start = async () => {
    if (!('DeviceOrientationEvent' in window)) { setPermission('unsupported'); return }
    try {
      const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
      if (orientation.requestPermission && await orientation.requestPermission() !== 'granted') { setPermission('denied'); return }
      setPermission('active')
    } catch { setPermission('denied') }
  }
  const label = heading == null ? '--' : ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round(heading / 45) % 8]

  return <div className="text-center">
    <div className="relative mx-auto grid size-64 place-items-center rounded-full border-[10px] border-slate-100 bg-slate-950 shadow-inner dark:border-slate-800"><span className="absolute top-3 font-black text-rose-400">N</span><div className="text-white transition-transform duration-300" style={{ transform: `rotate(${-(heading ?? 0)}deg)` }}><Compass className="size-32 stroke-[1] text-sky-300" /></div><div className="absolute"><strong className="block text-4xl text-white">{heading == null ? '—' : `${Math.round(heading)}°`}</strong><span className="text-sm font-black text-sky-300">{label}</span></div></div>
    <PermissionNotice state={permission} />
    {permission === 'active' ? <p className={`mt-3 rounded-xl px-3 py-2 text-xs font-bold ${accuracy === 'stable' ? 'bg-emerald-500/10 text-emerald-700' : 'bg-amber-500/15 text-amber-800'}`}>{accuracy === 'stable' ? 'Reading stable · estimated accuracy' : 'Magnetic jump detected · move away from metal and recalibrate'}</p> : null}
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? 'Compass active' : 'Enable compass'}</ActionButton><ActionButton tone="muted" disabled={heading == null} onClick={() => setHeld((value) => { if (value == null && heading != null) markToolSuccess('compass', { headline: 'Compass reading held', detail: `${Math.round(heading)}° ${label} saved locally until you resume live mode.`, shareText: `I used a private on-device compass reading: ${Math.round(heading)}° ${label}.` }); return value == null ? heading : null })}>{held == null ? 'Hold reading' : 'Resume live'}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={rawHeading == null} onClick={() => { const next = rawHeading == null ? 0 : -rawHeading; setOffset(next); localStorage.setItem('purehub.compass.offset.v1', String(next)); markToolSuccess('compass', { headline: 'Compass calibrated', detail: 'The current direction is now your local north reference.', shareText: 'I calibrated a private on-device compass without an account or tracker.' }) }}>Set current as North</ActionButton><ActionButton tone="muted" onClick={() => { setOffset(0); localStorage.removeItem('purehub.compass.offset.v1') }}><RotateCcw className="mr-1 inline size-4" />Reset calibration</ActionButton></div>
    <p className="mt-3 text-xs text-slate-500">Move the phone in a figure-eight before use. Magnetic readings are estimates and may be affected by cases or nearby metal.</p>
  </div>
}

type LevelLocale = 'en' | 'vi' | 'zh' | 'es'
type LevelMode = 'surface' | 'edge' | 'camera'
type LevelUnit = 'degrees' | 'percent'
type LevelHistory = { id: number; mode: LevelMode; x: number; y: number; slope: number }

const levelCopy: Record<LevelLocale, Record<string, string>> = {
  en: { title: 'Bubble Level', description: 'Live angle, target slope, camera guide, and private local calibration.', offline: 'Ready to measure · offline and private', fullscreen: 'Fullscreen instrument', surface: 'Surface', edge: 'Edge', camera: 'Camera', enable: 'Enable level', active: 'Level active', hold: 'Hold', resume: 'Resume', calibrate: 'Calibrate', reset: 'Reset', calibrationTitle: 'Calibrate level', calibration1: 'Remove a thick phone case if it rocks.', calibration2: 'Place the phone on a known-flat reference.', calibration3: 'Keep it still, then save this position as zero.', localCalibration: 'Calibration stays only on this device.', saveZero: 'Save zero', resetZero: 'Reset zero', cancel: 'Cancel', settings: 'Accuracy & settings', tolerance: 'Tolerance', target: 'Target slope', sound: 'Sound when aligned', history: 'Recent measurements', clear: 'Clear history', save: 'Save', level: 'Level confirmed', moving: 'Device moving · wait for a stable reading', inside: 'Inside tolerance · hold still to confirm', guide: 'Move the bubble into the target', left: 'Left / right', front: 'Front / back', slope: 'Slope', noHistory: 'No saved measurements yet.', cameraOff: 'Camera guide is off', accuracy: 'Calibrate on a known-flat reference. Phone sensors are useful estimates, not certified instruments.', unsupported: 'This browser does not expose the required sensor. Try PureHub on a supported phone.', denied: 'Sensor permission was not granted. Enable it in browser settings and try again.' },
  vi: { title: 'Thước thủy', description: 'Góc trực tiếp, độ dốc mục tiêu, camera và hiệu chỉnh riêng tư trên máy.', offline: 'Sẵn sàng đo · ngoại tuyến và riêng tư', fullscreen: 'Đo toàn màn hình', surface: 'Mặt phẳng', edge: 'Cạnh', camera: 'Camera', enable: 'Bật thước', active: 'Đang đo', hold: 'Giữ', resume: 'Đo tiếp', calibrate: 'Hiệu chỉnh', reset: 'Đặt lại', calibrationTitle: 'Hiệu chỉnh thước', calibration1: 'Tháo ốp dày nếu điện thoại bị cập kênh.', calibration2: 'Đặt điện thoại trên một mặt phẳng chuẩn.', calibration3: 'Giữ yên rồi lưu vị trí này làm mốc 0.', localCalibration: 'Dữ liệu hiệu chỉnh chỉ lưu trên thiết bị này.', saveZero: 'Lưu mốc 0', resetZero: 'Xóa mốc 0', cancel: 'Hủy', settings: 'Độ chính xác & cài đặt', tolerance: 'Sai số', target: 'Độ dốc mục tiêu', sound: 'Âm báo khi cân bằng', history: 'Số đo gần đây', clear: 'Xóa lịch sử', save: 'Lưu', level: 'Đã cân bằng', moving: 'Thiết bị đang di chuyển · chờ ổn định', inside: 'Trong sai số · giữ yên để xác nhận', guide: 'Di chuyển bóng vào vùng mục tiêu', left: 'Trái / phải', front: 'Trước / sau', slope: 'Độ dốc', noHistory: 'Chưa có số đo đã lưu.', cameraOff: 'Hướng dẫn camera đang tắt', accuracy: 'Hiệu chỉnh trên mặt phẳng chuẩn. Cảm biến điện thoại chỉ là ước lượng, không phải thiết bị đo được chứng nhận.', unsupported: 'Trình duyệt không cung cấp cảm biến cần thiết. Hãy thử PureHub trên điện thoại được hỗ trợ.', denied: 'Chưa cấp quyền cảm biến. Hãy bật quyền trong cài đặt trình duyệt rồi thử lại.' },
  zh: { title: '水平仪', description: '实时角度、目标坡度、相机辅助与本地私密校准。', offline: '可立即测量 · 离线且私密', fullscreen: '全屏测量', surface: '平面', edge: '边缘', camera: '相机', enable: '启用水平仪', active: '测量中', hold: '保持', resume: '继续', calibrate: '校准', reset: '重置', calibrationTitle: '校准水平仪', calibration1: '如果厚手机壳导致晃动，请将其取下。', calibration2: '将手机放在已知水平面上。', calibration3: '保持静止，然后将当前位置保存为零点。', localCalibration: '校准数据仅保存在此设备上。', saveZero: '保存零点', resetZero: '清除零点', cancel: '取消', settings: '精度与设置', tolerance: '容差', target: '目标坡度', sound: '对齐时提示音', history: '最近测量', clear: '清除历史', save: '保存', level: '已确认水平', moving: '设备正在移动 · 请等待稳定', inside: '已在容差内 · 保持稳定', guide: '将气泡移入目标区域', left: '左右', front: '前后', slope: '坡度', noHistory: '暂无已保存测量。', cameraOff: '相机辅助线已关闭', accuracy: '请在已知水平面上校准。手机传感器仅供估算，并非认证测量仪器。', unsupported: '浏览器未提供所需传感器。请在受支持的手机上使用 PureHub。', denied: '未授予传感器权限。请在浏览器设置中启用后重试。' },
  es: { title: 'Nivel de burbuja', description: 'Ángulo en vivo, pendiente objetivo, guía de cámara y calibración privada local.', offline: 'Listo para medir · sin conexión y privado', fullscreen: 'Instrumento a pantalla completa', surface: 'Superficie', edge: 'Borde', camera: 'Cámara', enable: 'Activar nivel', active: 'Nivel activo', hold: 'Fijar', resume: 'Reanudar', calibrate: 'Calibrar', reset: 'Restablecer', calibrationTitle: 'Calibrar nivel', calibration1: 'Quita una funda gruesa si el teléfono se balancea.', calibration2: 'Coloca el teléfono sobre una superficie plana conocida.', calibration3: 'Mantenlo quieto y guarda esta posición como cero.', localCalibration: 'La calibración solo se guarda en este dispositivo.', saveZero: 'Guardar cero', resetZero: 'Borrar cero', cancel: 'Cancelar', settings: 'Precisión y ajustes', tolerance: 'Tolerancia', target: 'Pendiente objetivo', sound: 'Sonido al nivelar', history: 'Mediciones recientes', clear: 'Borrar historial', save: 'Guardar', level: 'Nivel confirmado', moving: 'El dispositivo se mueve · espera una lectura estable', inside: 'Dentro de la tolerancia · mantenlo quieto', guide: 'Mueve la burbuja al objetivo', left: 'Izquierda / derecha', front: 'Frente / atrás', slope: 'Pendiente', noHistory: 'Aún no hay mediciones guardadas.', cameraOff: 'La guía de cámara está apagada', accuracy: 'Calibra sobre una referencia plana conocida. Los sensores del teléfono son estimaciones, no instrumentos certificados.', unsupported: 'El navegador no expone el sensor requerido. Prueba PureHub en un teléfono compatible.', denied: 'No se concedió el permiso del sensor. Actívalo en el navegador e inténtalo otra vez.' },
}

function LevelPanel({ locale }: { locale: string }) {
  const t = levelCopy[(locale in levelCopy ? locale : 'en') as LevelLocale]
  const [raw, setRaw] = useState({ x: 0, y: 0 })
  const [zero, setZero] = useState(() => { try { return JSON.parse(localStorage.getItem('purehub.level.zero.v1') ?? '{"x":0,"y":0}') as { x: number; y: number } } catch { return { x: 0, y: 0 } } })
  const [held, setHeld] = useState<{ x: number; y: number } | null>(null)
  const [permission, setPermission] = useState<PermissionState>('idle')
  const [levelMode, setLevelMode] = useState<LevelMode>('surface')
  const [tolerance, setTolerance] = useState(() => Number(localStorage.getItem('purehub.level.tolerance.v1') || 0.5))
  const [targetSlope, setTargetSlope] = useState(() => Number(localStorage.getItem('purehub.level.target.v1') || 0))
  const [unit, setUnit] = useState<LevelUnit>(() => localStorage.getItem('purehub.level.unit.v1') === 'percent' ? 'percent' : 'degrees')
  const [settled, setSettled] = useState(false)
  const [soundCue, setSoundCue] = useState(() => localStorage.getItem('purehub.level.sound.v1') === 'true')
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null)
  const videoRef = useRef<HTMLVideoElement>(null)
  const [history, setHistory] = useState<LevelHistory[]>(() => { try { return JSON.parse(localStorage.getItem('purehub.level.history.v1') || '[]') as LevelHistory[] } catch { return [] } })
  const [showCalibration, setShowCalibration] = useState(false)
  const previous = useRef({ x: 0, y: 0, at: 0 })
  const [moving, setMoving] = useState(false)
  const angles = held ?? { x: Math.round((raw.x - zero.x) * 10) / 10, y: Math.round((raw.y - zero.y) * 10) / 10 }
  const slopeDegrees = Math.hypot(angles.x, angles.y)
  const slopePercent = Math.tan(slopeDegrees * Math.PI / 180) * 100
  const targetDegrees = Math.atan(targetSlope / 100) * 180 / Math.PI
  const isLevel = levelMode === 'surface' ? Math.abs(slopeDegrees - targetDegrees) <= tolerance : Math.abs(Math.abs(angles.x) - targetDegrees) <= tolerance

  useEffect(() => {
    if (permission !== 'active') return
    let movementTimer = 0
    const listener = (event: DeviceOrientationEvent) => {
      const next = { x: event.gamma ?? 0, y: event.beta ?? 0 }
      const delta = Math.abs(next.x - previous.current.x) + Math.abs(next.y - previous.current.y)
      if (previous.current.at && delta > 3) { setMoving(true); window.clearTimeout(movementTimer); movementTimer = window.setTimeout(() => setMoving(false), 650) }
      previous.current = { ...next, at: Date.now() }; setRaw(next)
    }
    window.addEventListener('deviceorientation', listener, { passive: true })
    return () => { window.clearTimeout(movementTimer); window.removeEventListener('deviceorientation', listener) }
  }, [permission])

  useEffect(() => { if (videoRef.current) videoRef.current.srcObject = cameraStream }, [cameraStream])
  useEffect(() => () => cameraStream?.getTracks().forEach((track) => track.stop()), [cameraStream])

  const start = async () => {
    if (!('DeviceOrientationEvent' in window)) { setPermission('unsupported'); return }
    try {
      const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
      if (orientation.requestPermission && await orientation.requestPermission() !== 'granted') { setPermission('denied'); return }
      setPermission('active')
    } catch { setPermission('denied') }
  }
  const selectMode = async (next: LevelMode) => {
    setLevelMode(next)
    if (next !== 'camera') { cameraStream?.getTracks().forEach((track) => track.stop()); setCameraStream(null); return }
    try { setCameraStream(await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false })) } catch { setCameraStream(null) }
  }

  useEffect(() => {
    if (permission !== 'active' || moving || !isLevel || held) { const resetTimer = window.setTimeout(() => setSettled(false), 0); return () => window.clearTimeout(resetTimer) }
    const timer = window.setTimeout(() => {
      setSettled(true); if ('vibrate' in navigator) navigator.vibrate(45)
      if (soundCue) { const audio = new AudioContext(); const oscillator = audio.createOscillator(); const gain = audio.createGain(); oscillator.frequency.value = 660; gain.gain.value = 0.035; oscillator.connect(gain).connect(audio.destination); oscillator.start(); oscillator.stop(audio.currentTime + 0.09); oscillator.onended = () => void audio.close() }
      markToolSuccess('bubble-level', { headline: 'Level confirmed', detail: `The ${levelMode} reading stayed within ±${tolerance}° long enough to confirm.`, shareText: `I checked a ${levelMode} surface with a private on-device bubble level.` })
    }, 1800)
    return () => window.clearTimeout(timer)
  }, [held, isLevel, levelMode, moving, permission, soundCue, tolerance])

  const formatValue = (degrees: number) => unit === 'percent' ? `${(Math.tan(degrees * Math.PI / 180) * 100).toFixed(1)}%` : `${degrees.toFixed(1)}°`
  const saveMeasurement = () => { const next = [{ id: Date.now(), mode: levelMode, x: angles.x, y: angles.y, slope: slopePercent }, ...history].slice(0, 12); setHistory(next); localStorage.setItem('purehub.level.history.v1', JSON.stringify(next)) }
  const resetZero = () => { setZero({ x: 0, y: 0 }); localStorage.removeItem('purehub.level.zero.v1') }

  return <div>
    <div className="mb-3 grid grid-cols-3 gap-2">{([['surface', t.surface], ['edge', t.edge], ['camera', t.camera]] as const).map(([value, label]) => <button key={value} type="button" onClick={() => void selectMode(value)} className={`min-h-11 rounded-xl border text-xs font-black ${levelMode === value ? 'border-sky-500 bg-sky-600 text-white shadow-sm' : 'border-slate-200 dark:border-slate-700'}`}>{value === 'camera' && <Camera className="mr-1 inline size-4" />}{label}</button>)}</div>
    <div className={`relative mx-auto aspect-square w-full max-w-[420px] overflow-hidden rounded-[32px] border-8 transition-colors ${settled ? 'border-emerald-500 bg-emerald-50 shadow-[0_0_40px_rgba(16,185,129,.24)]' : isLevel ? 'border-emerald-300 bg-emerald-50' : 'border-slate-100 bg-sky-50'} dark:bg-slate-950`}>
      {levelMode === 'camera' && (cameraStream ? <video ref={videoRef} autoPlay muted playsInline className="absolute inset-0 size-full object-cover opacity-75" /> : <div className="absolute inset-0 grid place-items-center bg-slate-950 text-xs font-bold text-white"><span><Camera className="mx-auto mb-2" />{t.cameraOff}</span></div>)}
      <div className="absolute left-0 top-1/2 h-px w-full bg-slate-400/80" /><div className="absolute left-1/2 top-0 h-full w-px bg-slate-400/80" /><div className="absolute left-1/2 top-1/2 size-24 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-dashed border-emerald-500" /><span className={`absolute size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white shadow-xl transition-all duration-150 ${settled ? 'bg-emerald-500' : 'bg-sky-500'}`} style={{ left: `${50 + Math.max(-35, Math.min(35, angles.x))}%`, top: `${50 + Math.max(-35, Math.min(35, levelMode === 'edge' ? 0 : angles.y))}%` }} /><span className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-slate-950/75 px-4 py-2 text-sm font-black text-white backdrop-blur">{formatValue(slopeDegrees)}</span>
    </div>
    <PermissionNotice state={permission} unsupported={t.unsupported} denied={t.denied} />
    <p role="status" className={`mt-3 rounded-xl px-3 py-2 text-center text-xs font-bold ${settled ? 'bg-emerald-500/15 text-emerald-800 dark:text-emerald-200' : moving ? 'bg-amber-500/15 text-amber-800 dark:text-amber-200' : 'bg-slate-500/8 text-slate-600 dark:text-slate-300'}`}>{settled ? t.level : moving ? t.moving : isLevel ? t.inside : t.guide}</p>
    <div className="mt-3 grid grid-cols-3 gap-2 text-center"><Metric label={t.left} value={formatValue(angles.x)} /><Metric label={t.front} value={formatValue(angles.y)} /><Metric label={t.slope} value={`${slopePercent.toFixed(1)}%`} /></div>
    <div className="mt-4 grid grid-cols-3 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? t.active : t.enable}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setHeld((value) => value == null ? angles : null)}>{held == null ? t.hold : t.resume}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={saveMeasurement}>{t.save}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setShowCalibration(true)}>{t.calibrate}</ActionButton><ActionButton tone="muted" onClick={resetZero}><RotateCcw className="mr-1 inline size-4" />{t.reset}</ActionButton></div>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">{t.settings}</summary><p className="mt-3 text-xs font-bold text-slate-500">{t.tolerance}</p><div className="mt-2 grid grid-cols-3 gap-2">{[0.2, 0.5, 1].map((value) => <button key={value} type="button" onClick={() => { setTolerance(value); localStorage.setItem('purehub.level.tolerance.v1', String(value)) }} className={`min-h-10 rounded-xl border text-xs font-black ${tolerance === value ? 'border-violet-500 bg-violet-500/10 text-violet-700 dark:text-violet-300' : 'border-slate-200 dark:border-slate-700'}`}>±{value}°</button>)}</div><p className="mt-3 text-xs font-bold text-slate-500">{t.target}</p><div className="mt-2 grid grid-cols-4 gap-2">{[0, 1, 2, 5].map((value) => <button key={value} type="button" onClick={() => { setTargetSlope(value); localStorage.setItem('purehub.level.target.v1', String(value)) }} className={`min-h-10 rounded-xl border text-xs font-black ${targetSlope === value ? 'border-sky-500 bg-sky-500/10 text-sky-700 dark:text-sky-300' : 'border-slate-200 dark:border-slate-700'}`}>{value}%</button>)}</div><div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={() => { setUnit('degrees'); localStorage.setItem('purehub.level.unit.v1', 'degrees') }} className={`min-h-10 rounded-xl border text-xs font-bold ${unit === 'degrees' ? 'border-sky-500 bg-sky-500/10' : 'border-slate-200 dark:border-slate-700'}`}>°</button><button type="button" onClick={() => { setUnit('percent'); localStorage.setItem('purehub.level.unit.v1', 'percent') }} className={`min-h-10 rounded-xl border text-xs font-bold ${unit === 'percent' ? 'border-sky-500 bg-sky-500/10' : 'border-slate-200 dark:border-slate-700'}`}>%</button></div><label className="mt-3 flex min-h-11 items-center justify-between gap-3 text-xs font-bold"><span>{soundCue ? <Volume2 className="mr-2 inline size-4" /> : <VolumeX className="mr-2 inline size-4" />}{t.sound}</span><input type="checkbox" checked={soundCue} onChange={(event) => { setSoundCue(event.target.checked); localStorage.setItem('purehub.level.sound.v1', String(event.target.checked)) }} className="size-5 accent-violet-600" /></label><p className="mt-2 text-xs leading-5 text-slate-500">{t.accuracy}</p></details>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">{t.history} ({history.length})</summary>{history.length === 0 ? <p className="mt-3 text-xs text-slate-500">{t.noHistory}</p> : <div className="mt-3 space-y-2">{history.map((item) => <div key={item.id} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2 text-xs dark:bg-slate-800"><strong>{t[item.mode]}</strong><span>{item.x.toFixed(1)}° · {item.y.toFixed(1)}° · {item.slope.toFixed(1)}%</span></div>)}<button type="button" onClick={() => { setHistory([]); localStorage.removeItem('purehub.level.history.v1') }} className="min-h-10 w-full rounded-xl border border-rose-200 text-xs font-bold text-rose-700">{t.clear}</button></div>}</details>
    {showCalibration && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/65 p-4" role="dialog" aria-modal="true" aria-label={t.calibrationTitle}><div className="w-full max-w-sm rounded-[28px] bg-white p-5 text-left shadow-2xl dark:bg-slate-900"><h3 className="text-xl font-black">{t.calibrationTitle}</h3><ol className="mt-4 space-y-3 text-sm leading-6 text-slate-600 dark:text-slate-300"><li>1. {t.calibration1}</li><li>2. {t.calibration2}</li><li>3. {t.calibration3}</li></ol><p className="mt-4 rounded-xl bg-emerald-500/10 p-3 text-xs font-bold text-emerald-800 dark:text-emerald-200">{t.localCalibration}</p><div className="mt-5 grid grid-cols-3 gap-2"><button type="button" onClick={() => { resetZero(); setShowCalibration(false) }} className="min-h-11 rounded-xl border text-xs font-bold">{t.resetZero}</button><button type="button" onClick={() => setShowCalibration(false)} className="min-h-11 rounded-xl border text-xs font-bold">{t.cancel}</button><button type="button" onClick={() => { setZero(raw); localStorage.setItem('purehub.level.zero.v1', JSON.stringify(raw)); setShowCalibration(false) }} className="min-h-11 rounded-xl bg-sky-600 text-xs font-black text-white">{t.saveZero}</button></div></div></div>}
  </div>
}

function SoundPanel() {
  const [active, setActive] = useState(false)
  const [level, setLevel] = useState(0)
  const [samples, setSamples] = useState<number[]>([])
  const [permission, setPermission] = useState<PermissionState>('idle')
  const [calibration, setCalibration] = useState(() => Number(localStorage.getItem('purehub.sound.calibration.v1') || 0))
  const stream = useRef<MediaStream | null>(null)
  const frame = useRef(0)
  const context = useRef<AudioContext | null>(null)
  const peak = samples.length ? Math.max(...samples) : 0
  const average = samples.length ? Math.round(samples.reduce((sum, value) => sum + value, 0) / samples.length) : 0
  const minimum = samples.length ? Math.min(...samples) : 0

  const stop = () => { cancelAnimationFrame(frame.current); stream.current?.getTracks().forEach((track) => track.stop()); void context.current?.close(); stream.current = null; context.current = null; setActive(false) }
  const finish = () => {
    const sampleCount = samples.length
    stop()
    if (!sampleCount) return
    markToolSuccess('decibel-meter', { headline: 'Sound check complete', detail: `${sampleCount} local samples measured · ${average} dB average · ${peak} dB peak.`, shareText: `I completed a private sound check with an estimated ${average} dB average in PureHub.` })
  }
  useEffect(() => stop, [])
  const start = async () => {
    if (!navigator.mediaDevices?.getUserMedia) { setPermission('unsupported'); return }
    try {
      const media = await navigator.mediaDevices.getUserMedia({ audio: true })
      const audio = new AudioContext(); const analyser = audio.createAnalyser(); analyser.fftSize = 1024
      audio.createMediaStreamSource(media).connect(analyser); stream.current = media; context.current = audio; setActive(true); setPermission('active'); setSamples([])
      const data = new Uint8Array(analyser.fftSize)
      const tick = () => { analyser.getByteTimeDomainData(data); const rms = Math.sqrt(data.reduce((sum, value) => sum + ((value - 128) / 128) ** 2, 0) / data.length); const estimate = Math.max(0, Math.min(120, Math.round(20 * Math.log10(Math.max(rms, .0001)) + 94 + calibration))); setLevel(estimate); setSamples((values) => [...values.slice(-59), estimate]); frame.current = requestAnimationFrame(tick) }
      tick()
    } catch { setPermission('denied'); stop() }
  }
  const path = useMemo(() => samples.map((value, index) => `${index === 0 ? 'M' : 'L'} ${(index / Math.max(1, samples.length - 1)) * 100} ${40 - value * .36}`).join(' '), [samples])

  return <div>
    <div className="rounded-[28px] bg-slate-950 p-6 text-center text-white"><Activity className="mx-auto size-7 text-violet-300" /><strong className="mt-3 block text-6xl tabular-nums">{level}</strong><span className="text-sm font-black text-slate-400">estimated dB</span><div className="mt-6 h-4 overflow-hidden rounded-full bg-white/10"><div className={`h-full rounded-full transition-all ${level > 85 ? 'bg-rose-500' : level > 65 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${level}%` }} /></div>{samples.length > 1 ? <svg viewBox="0 0 100 40" className="mt-4 h-20 w-full overflow-visible" preserveAspectRatio="none" aria-label="Recent sound level history"><path d={path} fill="none" stroke="rgb(196 181 253)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" /></svg> : null}</div>
    <PermissionNotice state={permission} />
    <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4"><Metric label="Current" value={`${level} dB`} /><Metric label="Average" value={`${average} dB`} /><Metric label="Minimum" value={`${minimum} dB`} /><Metric label="Peak" value={`${peak} dB`} /></div>
    <ActionButton className="mt-4 w-full" onClick={() => active ? finish() : void start()}>{active ? 'Finish measurement' : 'Start sound meter'}</ActionButton>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">Calibration & export</summary><label className="mt-3 block text-xs font-semibold">Reference offset {calibration > 0 ? '+' : ''}{calibration} dB<input className="mt-2 w-full accent-violet-600" type="range" min="-20" max="20" value={calibration} onChange={(event) => { const next = Number(event.target.value); setCalibration(next); localStorage.setItem('purehub.sound.calibration.v1', String(next)) }} /></label><ActionButton tone="muted" className="mt-3 w-full" disabled={!samples.length} onClick={() => { const blob = new Blob([`sample,estimated_db\n${samples.map((value, index) => `${index + 1},${value}`).join('\n')}`], { type: 'text/csv' }); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'purehub-sound-readings.csv'; anchor.click(); URL.revokeObjectURL(url); markToolSuccess('decibel-meter', { headline: 'Sound readings exported', detail: `${samples.length} estimated sound samples were saved as a local CSV.`, shareText: 'I exported local sound readings with PureHub.' }) }}><Download className="size-4" />Export CSV</ActionButton></details>
    <p className="mt-3 text-xs text-slate-500">Microphone samples stay in this browser and are discarded when you stop. Values are estimates, not certified safety measurements.</p>
  </div>
}

function PermissionNotice({ state, unsupported, denied }: { state: PermissionState; unsupported?: string; denied?: string }) {
  if (state === 'idle' || state === 'active') return null
  return <div role="status" className="mt-4 flex items-start gap-2 rounded-2xl border border-amber-200 bg-amber-50 p-3 text-left text-xs leading-5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/35 dark:text-amber-100"><Lock className="mt-0.5 size-4 shrink-0" />{state === 'unsupported' ? unsupported ?? 'This browser does not expose the required sensor. Try PureHub on a supported phone.' : denied ?? 'Permission was not granted. Enable sensor or microphone access in browser settings, then try again.'}</div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl border border-slate-200 p-3 text-center dark:border-slate-700"><span className="block text-xs text-slate-500">{label}</span><strong className="text-xl tabular-nums">{value}</strong></div> }

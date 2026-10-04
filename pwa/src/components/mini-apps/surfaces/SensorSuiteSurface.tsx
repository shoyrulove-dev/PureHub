import { useEffect, useMemo, useRef, useState } from 'react'
import { Activity, Camera, Compass, Download, Gauge, Lock, Maximize2, Menu, Mic, RotateCcw, ShieldCheck, Volume2, VolumeX, X } from 'lucide-react'
import { ActionButton } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { normalizeLocale, type LocaleCode } from '../../../i18n/locales'

type SensorMode = 'compass' | 'level' | 'sound'
type PermissionState = 'idle' | 'active' | 'denied' | 'unsupported'

const sensorCopy: Record<LocaleCode, Record<string, string>> = {
  en: { compass: 'Compass', soundMeter: 'Sound Meter', suite: 'Sensor suite', description: 'Live readings, local calibration, and on-device processing.', level: 'Level', sound: 'Sound', fullscreen: 'Fullscreen instrument', stable: 'Reading stable · estimated accuracy', magnetic: 'Magnetic jump detected · move away from metal and recalibrate', compassActive: 'Compass active', enableCompass: 'Enable compass', hold: 'Hold reading', resume: 'Resume live', setNorth: 'Set current as North', resetCalibration: 'Reset calibration', compassHint: 'Move the phone in a figure-eight before use. Magnetic readings are estimates and may be affected by cases or nearby metal.', estimatedDb: 'estimated dB', recentSound: 'Recent sound level history', current: 'Current', average: 'Average', minimum: 'Minimum', peak: 'Peak', finish: 'Finish measurement', startSound: 'Start sound meter', calibrationExport: 'Calibration & export', referenceOffset: 'Reference offset', exportCsv: 'Export CSV', soundHint: 'Microphone samples stay in this browser and are discarded when you stop. Values are estimates, not certified safety measurements.', unsupported: 'This browser does not expose the required sensor. Try PureHub on a supported phone.', denied: 'Permission was not granted. Enable sensor or microphone access in browser settings, then try again.', levelConfirmed: 'Level confirmed', soundComplete: 'Sound check complete', soundExported: 'Sound readings exported' },
  vi: { compass: 'La bàn', soundMeter: 'Đo âm thanh', suite: 'Bộ cảm biến', description: 'Số đo trực tiếp, hiệu chỉnh cục bộ và xử lý trên thiết bị.', level: 'Thước thủy', sound: 'Âm thanh', fullscreen: 'Mở toàn màn hình', stable: 'Số đo ổn định · độ chính xác ước tính', magnetic: 'Phát hiện nhiễu từ · tránh xa kim loại và hiệu chỉnh lại', compassActive: 'La bàn đang hoạt động', enableCompass: 'Bật la bàn', hold: 'Giữ số đo', resume: 'Đo trực tiếp', setNorth: 'Đặt hướng hiện tại làm Bắc', resetCalibration: 'Xóa hiệu chỉnh', compassHint: 'Di chuyển điện thoại theo hình số 8 trước khi dùng. Số đo từ tính là ước tính và có thể bị ảnh hưởng bởi ốp hoặc kim loại gần đó.', estimatedDb: 'dB ước tính', recentSound: 'Lịch sử âm thanh gần đây', current: 'Hiện tại', average: 'Trung bình', minimum: 'Thấp nhất', peak: 'Cao nhất', finish: 'Kết thúc đo', startSound: 'Bắt đầu đo âm thanh', calibrationExport: 'Hiệu chỉnh và xuất', referenceOffset: 'Độ lệch tham chiếu', exportCsv: 'Xuất CSV', soundHint: 'Mẫu micro chỉ nằm trong trình duyệt và bị xóa khi dừng. Giá trị là ước tính, không phải phép đo an toàn được chứng nhận.', unsupported: 'Trình duyệt không cung cấp cảm biến cần thiết. Hãy thử PureHub trên điện thoại được hỗ trợ.', denied: 'Chưa cấp quyền. Hãy bật quyền cảm biến hoặc micro trong cài đặt trình duyệt rồi thử lại.', levelConfirmed: 'Đã xác nhận cân bằng', soundComplete: 'Đã đo âm thanh', soundExported: 'Đã xuất số đo âm thanh' },
  zh: { compass: '罗盘', soundMeter: '声音测量', suite: '传感器工具', description: '实时读数、本地校准和设备端处理。', level: '水平仪', sound: '声音', fullscreen: '全屏仪表', stable: '读数稳定 · 精度为估算值', magnetic: '检测到磁场跳变 · 请远离金属并重新校准', compassActive: '罗盘已启用', enableCompass: '启用罗盘', hold: '保持读数', resume: '恢复实时', setNorth: '将当前方向设为北方', resetCalibration: '重置校准', compassHint: '使用前请将手机按“8”字移动。磁场读数为估算值，可能受手机壳或附近金属影响。', estimatedDb: '估算 dB', recentSound: '最近声音记录', current: '当前', average: '平均', minimum: '最低', peak: '峰值', finish: '结束测量', startSound: '开始声音测量', calibrationExport: '校准与导出', referenceOffset: '参考偏移', exportCsv: '导出 CSV', soundHint: '麦克风样本仅保留在此浏览器中，停止后即丢弃。数值为估算值，并非认证的安全测量。', unsupported: '此浏览器未提供所需传感器，请在受支持的手机上使用 PureHub。', denied: '未授予权限。请在浏览器设置中启用传感器或麦克风权限后重试。', levelConfirmed: '已确认水平', soundComplete: '声音测量完成', soundExported: '声音读数已导出' },
  es: { compass: 'Brújula', soundMeter: 'Medidor de sonido', suite: 'Sensores', description: 'Lecturas en directo, calibración local y procesamiento en el dispositivo.', level: 'Nivel', sound: 'Sonido', fullscreen: 'Instrumento a pantalla completa', stable: 'Lectura estable · precisión estimada', magnetic: 'Cambio magnético detectado · aléjate del metal y calibra de nuevo', compassActive: 'Brújula activa', enableCompass: 'Activar brújula', hold: 'Fijar lectura', resume: 'Reanudar lectura', setNorth: 'Usar la dirección actual como Norte', resetCalibration: 'Restablecer calibración', compassHint: 'Mueve el móvil en forma de ocho antes de usarlo. Las lecturas magnéticas son estimadas y pueden verse afectadas por fundas o metales cercanos.', estimatedDb: 'dB estimados', recentSound: 'Historial reciente de sonido', current: 'Actual', average: 'Media', minimum: 'Mínimo', peak: 'Pico', finish: 'Finalizar medición', startSound: 'Iniciar medidor', calibrationExport: 'Calibración y exportación', referenceOffset: 'Desfase de referencia', exportCsv: 'Exportar CSV', soundHint: 'Las muestras del micrófono permanecen en este navegador y se descartan al detener. Los valores son estimados, no mediciones de seguridad certificadas.', unsupported: 'Este navegador no ofrece el sensor necesario. Prueba PureHub en un teléfono compatible.', denied: 'No se concedió el permiso. Activa el sensor o el micrófono en los ajustes del navegador e inténtalo de nuevo.', levelConfirmed: 'Nivel confirmado', soundComplete: 'Medición de sonido completada', soundExported: 'Lecturas de sonido exportadas' },
}

const compassDirections: Record<LocaleCode, { north: string; labels: string[] }> = {
  en: { north: 'N', labels: ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'] },
  vi: { north: 'B', labels: ['B', 'ĐB', 'Đ', 'ĐN', 'N', 'TN', 'T', 'TB'] },
  zh: { north: '北', labels: ['北', '东北', '东', '东南', '南', '西南', '西', '西北'] },
  es: { north: 'N', labels: ['N', 'NE', 'E', 'SE', 'S', 'SO', 'O', 'NO'] },
}

export default function SensorSuiteSurface({ mode }: { mode: SensorMode }) {
  const suiteRef = useRef<HTMLElement>(null)
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const copy = sensorCopy[locale]
  const levelLocale = levelCopy[(locale in levelCopy ? locale : 'en') as LevelLocale]
  const title = mode === 'compass' ? copy.compass : mode === 'level' ? levelLocale.title : copy.soundMeter

  if (mode === 'level') return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-700">
      <h2 className="text-lg font-black text-slate-950 dark:text-white">{title}</h2>
    </div>
    <div className="p-3 sm:p-4"><LevelPanel locale={locale} /></div>
  </section>

  return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-sky-50 via-white to-violet-50 p-5 dark:from-sky-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-start gap-3">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-sky-600 text-white">{mode === 'compass' ? <Compass /> : <Mic />}</span>
        <div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-sky-700 dark:text-sky-300">{copy.suite}</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">{title}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{copy.description}</p></div>
        <ShieldCheck className="hidden size-5 text-emerald-600 sm:block" />
      </div>
      <nav className="mt-4 grid grid-cols-3 gap-2">{([['compass', copy.compass, Compass], ['level', copy.level, Gauge], ['sound', copy.sound, Mic]] as const).map(([id, label, Icon]) => <a key={id} href={`/${locale}/${id === 'level' ? 'bubble-level' : id === 'sound' ? 'decibel-meter' : 'compass'}`} className={`flex min-h-11 items-center justify-center gap-1 rounded-xl border text-xs font-black ${mode === id ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 bg-white/70 dark:border-slate-700 dark:bg-slate-800'}`}><Icon className="size-4" />{label}</a>)}</nav>
    </header>
    <div className="p-4 sm:p-5">
      <button onClick={() => void suiteRef.current?.requestFullscreen?.()} className="mb-4 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-sky-200 text-xs font-black text-sky-800 dark:border-sky-800 dark:text-sky-200"><Maximize2 className="size-4" />{copy.fullscreen}</button>
      {mode === 'compass' ? <CompassPanel locale={locale} /> : <SoundPanel locale={locale} />}
    </div>
  </section>
}

function CompassPanel({ locale }: { locale: LocaleCode }) {
  const copy = sensorCopy[locale]
  const directions = compassDirections[locale]
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
  const label = heading == null ? '--' : directions.labels[Math.round(heading / 45) % 8]

  return <div className="text-center">
    <div className="relative mx-auto grid size-64 place-items-center rounded-full border-[10px] border-slate-100 bg-slate-950 shadow-inner dark:border-slate-800"><span className="absolute top-3 font-black text-rose-400">{directions.north}</span><div className="text-white transition-transform duration-300" style={{ transform: `rotate(${-(heading ?? 0)}deg)` }}><Compass className="size-32 stroke-[1] text-sky-300" /></div><div className="absolute"><strong className="block text-4xl text-white">{heading == null ? '—' : `${Math.round(heading)}°`}</strong><span className="text-sm font-black text-sky-300">{label}</span></div></div>
    <PermissionNotice state={permission} unsupported={copy.unsupported} denied={copy.denied} />
    {permission === 'active' ? <p className={`mt-3 rounded-xl px-3 py-2 text-xs font-bold ${accuracy === 'stable' ? 'bg-emerald-500/10 text-emerald-700' : 'bg-amber-500/15 text-amber-800'}`}>{accuracy === 'stable' ? copy.stable : copy.magnetic}</p> : null}
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? copy.compassActive : copy.enableCompass}</ActionButton><ActionButton tone="muted" disabled={heading == null} onClick={() => setHeld((value) => { if (value == null && heading != null) markToolSuccess('compass', { headline: copy.hold, detail: `${Math.round(heading)}° ${label}`, shareText: `${copy.compass}: ${Math.round(heading)}° ${label}` }); return value == null ? heading : null })}>{held == null ? copy.hold : copy.resume}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={rawHeading == null} onClick={() => { const next = rawHeading == null ? 0 : -rawHeading; setOffset(next); localStorage.setItem('purehub.compass.offset.v1', String(next)); markToolSuccess('compass', { headline: copy.setNorth, detail: copy.compassHint, shareText: copy.setNorth }) }}>{copy.setNorth}</ActionButton><ActionButton tone="muted" onClick={() => { setOffset(0); localStorage.removeItem('purehub.compass.offset.v1') }}><RotateCcw className="mr-1 inline size-4" />{copy.resetCalibration}</ActionButton></div>
    <p className="mt-3 text-xs text-slate-500">{copy.compassHint}</p>
  </div>
}

type LevelLocale = 'en' | 'vi' | 'zh' | 'es'
type LevelMode = 'surface' | 'edge' | 'camera'
type LevelUnit = 'degrees' | 'percent'
type LevelHistory = { id: number; mode: LevelMode; x: number; y: number; slope: number }

const levelCopy: Record<LevelLocale, Record<string, string>> = {
  en: { title: 'Bubble Level', surface: 'Surface', edge: 'Edge', camera: 'Camera', hold: 'Hold', resume: 'Resume', calibrate: 'Calibrate', reset: 'Reset', calibrationTitle: 'Calibrate', calibration1: 'Remove a thick case if the phone rocks.', calibration2: 'Place the phone on a flat surface.', calibration3: 'Keep still and save zero.', saveZero: 'Save zero', resetZero: 'Reset zero', cancel: 'Cancel', settings: 'Options', tolerance: 'Accuracy', target: 'Target', history: 'History', clear: 'Clear', save: 'Save', level: 'Level', moving: 'Moving', inside: 'Hold still', guide: 'Center bubble', left: 'X', front: 'Y', slope: 'Slope', noHistory: 'No measurements', cameraOff: 'Camera off', unsupported: 'Sensor unavailable.', denied: 'Permission denied.', confirmed: 'Level confirmed', confirmedDetail: 'The reading stayed inside the selected tolerance.', shareResult: 'I checked a surface with PureHub Bubble Level.' },
  vi: { title: 'Thước thủy', surface: 'Mặt phẳng', edge: 'Cạnh', camera: 'Camera', hold: 'Giữ', resume: 'Đo tiếp', calibrate: 'Hiệu chỉnh', reset: 'Đặt lại', calibrationTitle: 'Hiệu chỉnh', calibration1: 'Tháo ốp dày nếu máy bị kênh.', calibration2: 'Đặt máy trên mặt phẳng.', calibration3: 'Giữ yên và lưu mốc 0.', saveZero: 'Lưu mốc 0', resetZero: 'Xóa mốc 0', cancel: 'Hủy', settings: 'Tùy chọn', tolerance: 'Sai số', target: 'Mục tiêu', history: 'Lịch sử', clear: 'Xóa', save: 'Lưu', level: 'Cân bằng', moving: 'Đang di chuyển', inside: 'Giữ yên', guide: 'Căn giữa', left: 'X', front: 'Y', slope: 'Độ dốc', noHistory: 'Chưa có số đo', cameraOff: 'Camera tắt', unsupported: 'Không có cảm biến.', denied: 'Chưa cấp quyền.', confirmed: 'Đã xác nhận cân bằng', confirmedDetail: 'Số đo nằm trong sai số đã chọn.', shareResult: 'Tôi đã kiểm tra mặt phẳng bằng Thước thủy PureHub.' },
  zh: { title: '水平仪', surface: '平面', edge: '边缘', camera: '相机', hold: '保持', resume: '继续', calibrate: '校准', reset: '重置', calibrationTitle: '校准', calibration1: '如手机晃动，请取下厚手机壳。', calibration2: '将手机放在水平面上。', calibration3: '保持静止并保存零点。', saveZero: '保存零点', resetZero: '清除零点', cancel: '取消', settings: '选项', tolerance: '精度', target: '目标', history: '历史', clear: '清除', save: '保存', level: '已水平', moving: '移动中', inside: '保持静止', guide: '移至中心', left: 'X', front: 'Y', slope: '坡度', noHistory: '暂无测量', cameraOff: '相机已关闭', unsupported: '无可用传感器。', denied: '未授权限。', confirmed: '已确认水平', confirmedDetail: '读数保持在所选误差范围内。', shareResult: '我使用 PureHub 水平仪检查了表面。' },
  es: { title: 'Nivel de burbuja', surface: 'Superficie', edge: 'Borde', camera: 'Cámara', hold: 'Fijar', resume: 'Reanudar', calibrate: 'Calibrar', reset: 'Restablecer', calibrationTitle: 'Calibrar', calibration1: 'Quita la funda si el móvil se mueve.', calibration2: 'Pon el móvil en una superficie plana.', calibration3: 'Mantenlo quieto y guarda cero.', saveZero: 'Guardar cero', resetZero: 'Borrar cero', cancel: 'Cancelar', settings: 'Opciones', tolerance: 'Precisión', target: 'Objetivo', history: 'Historial', clear: 'Borrar', save: 'Guardar', level: 'Nivelado', moving: 'En movimiento', inside: 'Mantén fijo', guide: 'Centra la burbuja', left: 'X', front: 'Y', slope: 'Pendiente', noHistory: 'Sin mediciones', cameraOff: 'Cámara apagada', unsupported: 'Sensor no disponible.', denied: 'Permiso denegado.', confirmed: 'Nivel confirmado', confirmedDetail: 'La lectura se mantuvo dentro de la tolerancia elegida.', shareResult: 'He comprobado una superficie con el Nivel de PureHub.' },
}

const levelMenuCopy: Record<LevelLocale, { options: string; mode: string; sound: string; start: string; pause: string }> = {
  en: { options: 'Options', mode: 'Mode', sound: 'Sound', start: 'Start', pause: 'Pause' },
  vi: { options: 'Tùy chọn', mode: 'Chế độ', sound: 'Âm thanh', start: 'Bắt đầu', pause: 'Tạm dừng' },
  zh: { options: '选项', mode: '模式', sound: '声音', start: '开始', pause: '暂停' },
  es: { options: 'Opciones', mode: 'Modo', sound: 'Sonido', start: 'Iniciar', pause: 'Pausar' },
}

function LevelPanel({ locale }: { locale: string }) {
  const t = levelCopy[(locale in levelCopy ? locale : 'en') as LevelLocale]
  const menuText = levelMenuCopy[(locale in levelMenuCopy ? locale : 'en') as LevelLocale]
  const [raw, setRaw] = useState({ x: 0, y: 0 })
  const [zero, setZero] = useState(() => { try { return JSON.parse(localStorage.getItem('purehub.level.zero.v1') ?? '{"x":0,"y":0}') as { x: number; y: number } } catch { return { x: 0, y: 0 } } })
  const [held, setHeld] = useState<{ x: number; y: number } | null>(null)
  const [permission, setPermission] = useState<PermissionState>(() => {
    if (!('DeviceOrientationEvent' in window)) return 'unsupported'
    const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
    return orientation.requestPermission ? 'idle' : 'active'
  })
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
  const [menuOpen, setMenuOpen] = useState(false)
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
      markToolSuccess('bubble-level', { headline: t.confirmed, detail: `${t.confirmedDetail} ±${tolerance}°`, shareText: t.shareResult })
    }, 1800)
    return () => window.clearTimeout(timer)
  }, [held, isLevel, levelMode, moving, permission, soundCue, t, tolerance])

  const formatValue = (degrees: number) => unit === 'percent' ? `${(Math.tan(degrees * Math.PI / 180) * 100).toFixed(1)}%` : `${degrees.toFixed(1)}°`
  const saveMeasurement = () => { const next = [{ id: Date.now(), mode: levelMode, x: angles.x, y: angles.y, slope: slopePercent }, ...history].slice(0, 12); setHistory(next); localStorage.setItem('purehub.level.history.v1', JSON.stringify(next)) }
  const resetZero = () => { setZero({ x: 0, y: 0 }); localStorage.removeItem('purehub.level.zero.v1') }

  return <div className="relative">
    <div className="mb-3 flex items-center gap-2"><button type="button" onClick={() => setMenuOpen(true)} className="grid size-11 place-items-center rounded-xl bg-slate-950 text-white" aria-label={t.settings}><Menu className="size-5" /></button><strong className="text-sm">{t[levelMode]}</strong></div>
    <div className={`relative mx-auto aspect-square w-full max-w-[420px] overflow-hidden rounded-[32px] border-8 transition-colors ${settled ? 'border-emerald-500 bg-emerald-50 shadow-[0_0_40px_rgba(16,185,129,.24)]' : isLevel ? 'border-emerald-300 bg-emerald-50' : 'border-slate-100 bg-sky-50'} dark:bg-slate-950`}>
      {levelMode === 'camera' && (cameraStream ? <video ref={videoRef} autoPlay muted playsInline className="absolute inset-0 size-full object-cover opacity-75" /> : <div className="absolute inset-0 grid place-items-center bg-slate-950 text-xs font-bold text-white"><span><Camera className="mx-auto mb-2" />{t.cameraOff}</span></div>)}
      <div className="absolute left-0 top-1/2 h-px w-full bg-slate-400/80" /><div className="absolute left-1/2 top-0 h-full w-px bg-slate-400/80" /><div className="absolute left-1/2 top-1/2 size-24 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-dashed border-emerald-500" /><span className={`absolute size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white shadow-xl transition-all duration-150 ${settled ? 'bg-emerald-500' : 'bg-sky-500'}`} style={{ left: `${50 + Math.max(-35, Math.min(35, angles.x))}%`, top: `${50 + Math.max(-35, Math.min(35, levelMode === 'edge' ? 0 : angles.y))}%` }} /><span className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-slate-950/75 px-4 py-2 text-sm font-black text-white backdrop-blur">{formatValue(slopeDegrees)}</span>
    </div>
    <PermissionNotice state={permission} unsupported={t.unsupported} denied={t.denied} />
    <p role="status" className={`mt-3 rounded-xl px-3 py-2 text-center text-xs font-bold ${settled ? 'bg-emerald-500/15 text-emerald-800 dark:text-emerald-200' : moving ? 'bg-amber-500/15 text-amber-800 dark:text-amber-200' : 'bg-slate-500/8 text-slate-600 dark:text-slate-300'}`}>{settled ? t.level : moving ? t.moving : isLevel ? t.inside : t.guide}</p>
    <div className="mt-3 grid grid-cols-3 gap-2 text-center"><Metric label={t.left} value={formatValue(angles.x)} /><Metric label={t.front} value={formatValue(angles.y)} /><Metric label={t.slope} value={`${slopePercent.toFixed(1)}%`} /></div>
    <div className="mt-4 grid grid-cols-3 gap-2"><ActionButton onClick={() => permission === 'active' ? setPermission('idle') : void start()}>{permission === 'active' ? menuText.pause : menuText.start}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setHeld((value) => value == null ? angles : null)}>{held == null ? t.hold : t.resume}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={saveMeasurement}>{t.save}</ActionButton></div>
    {menuOpen && <div className="fixed inset-0 z-40 bg-slate-950/60" role="presentation" onClick={() => setMenuOpen(false)}>
      <aside className="h-full w-[52vw] min-w-56 max-w-sm overflow-y-auto bg-white p-4 shadow-2xl dark:bg-slate-900" aria-label={t.settings} onClick={(event) => event.stopPropagation()}>
        <div className="flex items-center justify-between"><strong className="text-base">{menuText.options}</strong><button type="button" onClick={() => setMenuOpen(false)} className="grid size-10 place-items-center rounded-xl border" aria-label={t.cancel}><X className="size-4" /></button></div>
        <p className="mt-3 text-xs font-black text-slate-500">{menuText.mode}</p>
        <div className="mt-2 grid gap-2">{([['surface', t.surface], ['edge', t.edge], ['camera', t.camera]] as const).map(([value, label]) => <button key={value} type="button" onClick={() => { void selectMode(value); setMenuOpen(false) }} className={`min-h-11 rounded-xl border px-3 text-left text-xs font-black ${levelMode === value ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 dark:border-slate-700'}`}>{value === 'camera' && <Camera className="mr-2 inline size-4" />}{label}</button>)}</div>
        <div className="mt-4 grid gap-2"><button type="button" disabled={permission !== 'active'} onClick={() => setShowCalibration(true)} className="min-h-11 rounded-xl bg-sky-600 px-3 text-xs font-black text-white disabled:opacity-50">{t.calibrate}</button><button type="button" onClick={resetZero} className="min-h-11 rounded-xl border px-3 text-xs font-bold"><RotateCcw className="mr-1 inline size-4" />{t.reset}</button></div>
        <p className="mt-4 text-xs font-black text-slate-500">{t.tolerance}</p><div className="mt-2 grid grid-cols-3 gap-2">{[0.2, 0.5, 1].map((value) => <button key={value} type="button" onClick={() => { setTolerance(value); localStorage.setItem('purehub.level.tolerance.v1', String(value)) }} className={`min-h-10 rounded-xl border text-xs font-black ${tolerance === value ? 'border-violet-500 bg-violet-500/10' : 'border-slate-200 dark:border-slate-700'}`}>±{value}°</button>)}</div>
        <p className="mt-4 text-xs font-black text-slate-500">{t.target}</p><div className="mt-2 grid grid-cols-2 gap-2">{[0, 1, 2, 5].map((value) => <button key={value} type="button" onClick={() => { setTargetSlope(value); localStorage.setItem('purehub.level.target.v1', String(value)) }} className={`min-h-10 rounded-xl border text-xs font-black ${targetSlope === value ? 'border-sky-500 bg-sky-500/10' : 'border-slate-200 dark:border-slate-700'}`}>{value}%</button>)}</div>
        <div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={() => { setUnit('degrees'); localStorage.setItem('purehub.level.unit.v1', 'degrees') }} className={`min-h-10 rounded-xl border text-xs font-bold ${unit === 'degrees' ? 'border-sky-500 bg-sky-500/10' : 'border-slate-200 dark:border-slate-700'}`}>°</button><button type="button" onClick={() => { setUnit('percent'); localStorage.setItem('purehub.level.unit.v1', 'percent') }} className={`min-h-10 rounded-xl border text-xs font-bold ${unit === 'percent' ? 'border-sky-500 bg-sky-500/10' : 'border-slate-200 dark:border-slate-700'}`}>%</button></div>
        <label className="mt-3 flex min-h-11 items-center justify-between gap-2 text-xs font-bold"><span>{soundCue ? <Volume2 className="mr-1 inline size-4" /> : <VolumeX className="mr-1 inline size-4" />}{menuText.sound}</span><input type="checkbox" checked={soundCue} onChange={(event) => { setSoundCue(event.target.checked); localStorage.setItem('purehub.level.sound.v1', String(event.target.checked)) }} className="size-5 accent-violet-600" /></label>
        <p className="mt-4 text-xs font-black text-slate-500">{t.history} ({history.length})</p>{history.length === 0 ? <p className="mt-2 text-xs text-slate-500">{t.noHistory}</p> : <div className="mt-2 space-y-2">{history.slice(0, 4).map((item) => <div key={item.id} className="rounded-xl bg-slate-50 px-3 py-2 text-xs dark:bg-slate-800"><strong>{t[item.mode]}</strong><span className="mt-1 block">{item.x.toFixed(1)}° · {item.y.toFixed(1)}°</span></div>)}<button type="button" onClick={() => { setHistory([]); localStorage.removeItem('purehub.level.history.v1') }} className="min-h-10 w-full rounded-xl border text-xs font-bold">{t.clear}</button></div>}
      </aside>
    </div>}
    {showCalibration && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/65 p-4" role="dialog" aria-modal="true" aria-label={t.calibrationTitle}><div className="w-full max-w-sm rounded-[28px] bg-white p-5 text-left shadow-2xl dark:bg-slate-900"><h3 className="text-xl font-black">{t.calibrationTitle}</h3><ol className="mt-4 space-y-3 text-sm leading-6 text-slate-600 dark:text-slate-300"><li>1. {t.calibration1}</li><li>2. {t.calibration2}</li><li>3. {t.calibration3}</li></ol><div className="mt-5 grid grid-cols-3 gap-2"><button type="button" onClick={() => { resetZero(); setShowCalibration(false) }} className="min-h-11 rounded-xl border text-xs font-bold">{t.resetZero}</button><button type="button" onClick={() => setShowCalibration(false)} className="min-h-11 rounded-xl border text-xs font-bold">{t.cancel}</button><button type="button" onClick={() => { setZero(raw); localStorage.setItem('purehub.level.zero.v1', JSON.stringify(raw)); setShowCalibration(false); setMenuOpen(false) }} className="min-h-11 rounded-xl bg-sky-600 text-xs font-black text-white">{t.saveZero}</button></div></div></div>}
  </div>
}

function SoundPanel({ locale }: { locale: LocaleCode }) {
  const copy = sensorCopy[locale]
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
    markToolSuccess('decibel-meter', { headline: copy.soundComplete, detail: `${sampleCount} · ${copy.average}: ${average} dB · ${copy.peak}: ${peak} dB`, shareText: `${copy.soundMeter}: ${copy.average} ${average} dB` })
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
    <div className="rounded-[28px] bg-slate-950 p-6 text-center text-white"><Activity className="mx-auto size-7 text-violet-300" /><strong className="mt-3 block text-6xl tabular-nums">{level}</strong><span className="text-sm font-black text-slate-400">{copy.estimatedDb}</span><div className="mt-6 h-4 overflow-hidden rounded-full bg-white/10"><div className={`h-full rounded-full transition-all ${level > 85 ? 'bg-rose-500' : level > 65 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${level}%` }} /></div>{samples.length > 1 ? <svg viewBox="0 0 100 40" className="mt-4 h-20 w-full overflow-visible" preserveAspectRatio="none" aria-label={copy.recentSound}><path d={path} fill="none" stroke="rgb(196 181 253)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" /></svg> : null}</div>
    <PermissionNotice state={permission} unsupported={copy.unsupported} denied={copy.denied} />
    <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4"><Metric label={copy.current} value={`${level} dB`} /><Metric label={copy.average} value={`${average} dB`} /><Metric label={copy.minimum} value={`${minimum} dB`} /><Metric label={copy.peak} value={`${peak} dB`} /></div>
    <ActionButton className="mt-4 w-full" onClick={() => active ? finish() : void start()}>{active ? copy.finish : copy.startSound}</ActionButton>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">{copy.calibrationExport}</summary><label className="mt-3 block text-xs font-semibold">{copy.referenceOffset} {calibration > 0 ? '+' : ''}{calibration} dB<input className="mt-2 w-full accent-violet-600" type="range" min="-20" max="20" value={calibration} onChange={(event) => { const next = Number(event.target.value); setCalibration(next); localStorage.setItem('purehub.sound.calibration.v1', String(next)) }} /></label><ActionButton tone="muted" className="mt-3 w-full" disabled={!samples.length} onClick={() => { const blob = new Blob([`sample,estimated_db\n${samples.map((value, index) => `${index + 1},${value}`).join('\n')}`], { type: 'text/csv' }); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'purehub-sound-readings.csv'; anchor.click(); URL.revokeObjectURL(url); markToolSuccess('decibel-meter', { headline: copy.soundExported, detail: `${samples.length} · CSV`, shareText: copy.soundExported }) }}><Download className="size-4" />{copy.exportCsv}</ActionButton></details>
    <p className="mt-3 text-xs text-slate-500">{copy.soundHint}</p>
  </div>
}

function PermissionNotice({ state, unsupported, denied }: { state: PermissionState; unsupported?: string; denied?: string }) {
  if (state === 'idle' || state === 'active') return null
  return <div role="status" className="mt-4 flex items-start gap-2 rounded-2xl border border-amber-200 bg-amber-50 p-3 text-left text-xs leading-5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/35 dark:text-amber-100"><Lock className="mt-0.5 size-4 shrink-0" />{state === 'unsupported' ? unsupported ?? 'This browser does not expose the required sensor. Try PureHub on a supported phone.' : denied ?? 'Permission was not granted. Enable sensor or microphone access in browser settings, then try again.'}</div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl border border-slate-200 p-3 text-center dark:border-slate-700"><span className="block text-xs text-slate-500">{label}</span><strong className="text-xl tabular-nums">{value}</strong></div> }

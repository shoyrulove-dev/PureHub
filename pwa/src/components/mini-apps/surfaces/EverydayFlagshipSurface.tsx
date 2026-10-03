import { useEffect, useMemo, useRef, useState } from 'react'
import { Archive, CheckCircle2, Clipboard, Code2, Download, Droplets, Gauge, Image, Lightbulb, Network, Palette, RefreshCw, Save, Share2, ShieldCheck, Sparkles, Trash2, Users, WandSparkles } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, FormTextArea, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { normalizeLocale, type LocaleCode } from '../../../i18n/locales'

type Mode = 'smart-flashlight' | 'unit-converter' | 'color-grabber' | 'deep-cleaner' | 'wifi-analyzer' | 'wallpaper-changer' | 'decision-wheel' | 'community'

type WifiCopy = {
  eyebrow: string
  title: string
  description: string
  panelTitle: string
  panelSubtitle: string
  healthScore: string
  status: string
  online: string
  offline: string
  networkClass: string
  notExposed: string
  browserDownlink: string
  browserRtt: string
  averageLatency: string
  notTested: string
  jitter: string
  requestLoss: string
  dataSaver: string
  enabled: string
  disabled: string
  runTest: string
  checking: string
  history: string
  clearHistory: string
  recommendation: string
  recommendations: [string, string, string, string]
  testNote: string
  androidTitle: string
  androidDescription: string
  androidFeatures: [string, string, string, string]
  installAndroid: string
  successHeadline: string
  successDetail: (latency: number, jitter: number) => string
  successShare: (latency: number) => string
}

const WIFI_COPY: Record<LocaleCode, WifiCopy> = {
  en: {
    eyebrow: 'Connection Care flagship', title: 'Wi-Fi Analyzer', description: 'Understand the connection information your browser exposes, with honest platform limits.',
    panelTitle: 'Connection health', panelSubtitle: 'A browser cannot scan nearby access points. PureHub only reports standards-based connection signals.', healthScore: 'HEALTH SCORE',
    status: 'Status', online: 'Online', offline: 'Offline', networkClass: 'Network class', notExposed: 'Not exposed', browserDownlink: 'Browser downlink', browserRtt: 'Browser RTT', averageLatency: 'Average latency', notTested: 'Not tested', jitter: 'Jitter', requestLoss: 'Request loss', dataSaver: 'Data saver', enabled: 'Enabled', disabled: 'Disabled',
    runTest: 'Run private connection check', checking: 'Checking ten samples…', history: 'Private test history', clearHistory: 'Clear history', recommendation: 'Network health recommendation', recommendations: ['Connection looks healthy for normal browsing.', 'Latency is elevated. Move closer to the router or pause heavy traffic.', 'Connection is unstable. Check router placement and competing traffic.', 'This device is offline. Reconnect before running a health check.'],
    testNote: 'Ten small same-origin requests estimate browser latency, jitter and request loss. This is not ICMP packet loss, a full bandwidth test or a nearby Wi-Fi scan.',
    androidTitle: 'Need native Wi-Fi tools?', androidDescription: 'Browsers cannot expose nearby SSIDs, channels, LAN devices, roaming events or floor-plan coverage. PureHub Android requests those permissions only when you use the matching feature.', androidFeatures: ['Nearby networks and channel analysis', 'LAN devices and vendor identification', 'Speed, outage and roaming diagnostics', 'Walk tests, surveys and heatmap reports'], installAndroid: 'Install PureHub Android',
    successHeadline: 'Connection check complete', successDetail: (latency, jitter) => `${latency} ms average with ${jitter} ms jitter across ten local requests.`, successShare: (latency) => `I ran a private PureHub connection check: ${latency} ms average latency.`,
  },
  vi: {
    eyebrow: 'Công cụ chăm sóc kết nối', title: 'Phân tích Wi-Fi', description: 'Hiểu dữ liệu kết nối mà trình duyệt cho phép hiển thị, với giới hạn nền tảng được giải thích rõ ràng.',
    panelTitle: 'Sức khỏe kết nối', panelSubtitle: 'Trình duyệt không thể quét các điểm truy cập gần đó. PureHub chỉ hiển thị tín hiệu kết nối theo chuẩn web.', healthScore: 'ĐIỂM SỨC KHỎE',
    status: 'Trạng thái', online: 'Đang kết nối', offline: 'Ngoại tuyến', networkClass: 'Loại mạng', notExposed: 'Không được cung cấp', browserDownlink: 'Tốc độ trình duyệt báo', browserRtt: 'RTT trình duyệt', averageLatency: 'Độ trễ trung bình', notTested: 'Chưa kiểm tra', jitter: 'Độ dao động', requestLoss: 'Lỗi yêu cầu', dataSaver: 'Tiết kiệm dữ liệu', enabled: 'Đang bật', disabled: 'Đang tắt',
    runTest: 'Kiểm tra kết nối riêng tư', checking: 'Đang kiểm tra 10 mẫu…', history: 'Lịch sử kiểm tra riêng tư', clearHistory: 'Xóa lịch sử', recommendation: 'Khuyến nghị sức khỏe mạng', recommendations: ['Kết nối phù hợp cho nhu cầu duyệt web thông thường.', 'Độ trễ đang cao. Hãy đến gần router hoặc tạm dừng lưu lượng nặng.', 'Kết nối chưa ổn định. Hãy kiểm tra vị trí router và lưu lượng cạnh tranh.', 'Thiết bị đang ngoại tuyến. Hãy kết nối lại trước khi kiểm tra.'],
    testNote: 'Mười yêu cầu nhỏ cùng tên miền giúp ước tính độ trễ, độ dao động và tỷ lệ yêu cầu lỗi. Đây không phải mất gói ICMP, phép đo toàn bộ băng thông hay quét Wi-Fi lân cận.',
    androidTitle: 'Cần công cụ Wi-Fi native?', androidDescription: 'Trình duyệt không thể xem SSID, kênh, thiết bị LAN, sự kiện roaming hoặc vùng phủ trên sơ đồ. PureHub Android chỉ xin quyền khi bạn dùng đúng tính năng.', androidFeatures: ['Mạng lân cận và phân tích kênh', 'Thiết bị LAN và nhận diện nhà sản xuất', 'Tốc độ, gián đoạn và roaming', 'Walk test, khảo sát và báo cáo heatmap'], installAndroid: 'Cài PureHub Android',
    successHeadline: 'Đã kiểm tra kết nối', successDetail: (latency, jitter) => `Trung bình ${latency} ms, dao động ${jitter} ms qua 10 yêu cầu cục bộ.`, successShare: (latency) => `Tôi đã kiểm tra kết nối riêng tư bằng PureHub: độ trễ trung bình ${latency} ms.`,
  },
  zh: {
    eyebrow: '连接维护旗舰工具', title: 'Wi-Fi 分析', description: '了解浏览器可提供的连接信息，并清楚说明平台限制。',
    panelTitle: '连接健康', panelSubtitle: '浏览器无法扫描附近接入点。PureHub 仅显示 Web 标准允许的连接信号。', healthScore: '健康评分',
    status: '状态', online: '在线', offline: '离线', networkClass: '网络类型', notExposed: '未提供', browserDownlink: '浏览器下行估计', browserRtt: '浏览器 RTT', averageLatency: '平均延迟', notTested: '尚未测试', jitter: '抖动', requestLoss: '请求失败率', dataSaver: '流量节省', enabled: '已开启', disabled: '已关闭',
    runTest: '运行私密连接检查', checking: '正在检查 10 个样本…', history: '私密测试历史', clearHistory: '清除历史', recommendation: '网络健康建议', recommendations: ['连接状况适合日常浏览。', '延迟偏高，请靠近路由器或暂停大流量任务。', '连接不稳定，请检查路由器位置和竞争流量。', '设备当前离线，请重新连接后再检查。'],
    testNote: '通过 10 个同源小请求估算延迟、抖动和请求失败率。这不是 ICMP 丢包测试、完整带宽测试或附近 Wi-Fi 扫描。',
    androidTitle: '需要原生 Wi-Fi 工具？', androidDescription: '浏览器无法读取附近 SSID、信道、局域网设备、漫游事件或楼层覆盖。PureHub Android 只在使用相应功能时请求权限。', androidFeatures: ['附近网络与信道分析', '局域网设备与厂商识别', '速度、中断与漫游诊断', '步行测试、覆盖调查与热力图报告'], installAndroid: '安装 PureHub Android',
    successHeadline: '连接检查完成', successDetail: (latency, jitter) => `10 个本地请求的平均延迟为 ${latency} ms，抖动为 ${jitter} ms。`, successShare: (latency) => `我使用 PureHub 完成了私密连接检查：平均延迟 ${latency} ms。`,
  },
  es: {
    eyebrow: 'Herramienta principal de conexión', title: 'Analizador Wi-Fi', description: 'Comprende la información de conexión que muestra el navegador, con límites de plataforma explicados claramente.',
    panelTitle: 'Salud de la conexión', panelSubtitle: 'El navegador no puede escanear puntos de acceso cercanos. PureHub solo muestra señales permitidas por los estándares web.', healthScore: 'SALUD DE RED',
    status: 'Estado', online: 'En línea', offline: 'Sin conexión', networkClass: 'Tipo de red', notExposed: 'No disponible', browserDownlink: 'Descarga estimada', browserRtt: 'RTT del navegador', averageLatency: 'Latencia media', notTested: 'Sin probar', jitter: 'Variación', requestLoss: 'Fallos de solicitud', dataSaver: 'Ahorro de datos', enabled: 'Activado', disabled: 'Desactivado',
    runTest: 'Comprobar conexión privada', checking: 'Comprobando 10 muestras…', history: 'Historial privado', clearHistory: 'Borrar historial', recommendation: 'Recomendación de salud de red', recommendations: ['La conexión parece adecuada para la navegación normal.', 'La latencia es alta. Acércate al router o pausa el tráfico intenso.', 'La conexión es inestable. Revisa la ubicación del router y el tráfico simultáneo.', 'El dispositivo está sin conexión. Vuelve a conectarlo antes de comprobar.'],
    testNote: 'Diez solicitudes pequeñas al mismo origen estiman latencia, variación y fallos. No es pérdida ICMP, una prueba completa de ancho de banda ni un escaneo Wi-Fi cercano.',
    androidTitle: '¿Necesitas herramientas Wi-Fi nativas?', androidDescription: 'El navegador no puede ver SSID cercanos, canales, dispositivos LAN, eventos de roaming ni cobertura en planos. PureHub Android solicita permisos solo al usar cada función.', androidFeatures: ['Redes cercanas y análisis de canales', 'Dispositivos LAN e identificación del fabricante', 'Velocidad, cortes y diagnóstico de roaming', 'Recorridos, estudios e informes de mapa de calor'], installAndroid: 'Instalar PureHub Android',
    successHeadline: 'Comprobación completada', successDetail: (latency, jitter) => `${latency} ms de media y ${jitter} ms de variación en 10 solicitudes locales.`, successShare: (latency) => `He realizado una comprobación privada con PureHub: ${latency} ms de latencia media.`,
  },
}

const META: Record<Mode, { eyebrow: string; title: string; description: string; accent: 'emerald' | 'violet' | 'amber' | 'sky' }> = {
  'smart-flashlight': { eyebrow: 'Light Suite flagship', title: 'Smart Flashlight', description: 'A bright screen light with dimming, pulse and SOS controls that remain under your control.', accent: 'amber' },
  'unit-converter': { eyebrow: 'Everyday Tools flagship', title: 'Unit Converter', description: 'Search, convert and revisit common measurements instantly without a network request.', accent: 'sky' },
  'color-grabber': { eyebrow: 'Creative Suite flagship', title: 'Color Grabber', description: 'Pick exact colors, extract a local palette and check readable contrast from your own image.', accent: 'violet' },
  'deep-cleaner': { eyebrow: 'Storage Care flagship', title: 'Deep Cleaner', description: 'Review selected files, spot likely duplicates and understand storage without silent deletion.', accent: 'emerald' },
  'wifi-analyzer': { eyebrow: 'Connection Care flagship', title: 'Wi-Fi Analyzer', description: 'Explain the connection information your browser exposes, with honest platform limits.', accent: 'sky' },
  'wallpaper-changer': { eyebrow: 'Creative Suite flagship', title: 'Wallpaper Studio', description: 'Preview, frame and export your own wallpapers locally without feeds or uploads.', accent: 'violet' },
  'decision-wheel': { eyebrow: 'Decision Suite flagship', title: 'Decision Wheel', description: 'Save reusable lists, spin with secure randomness and keep a private result history.', accent: 'amber' },
  community: { eyebrow: 'Community flagship', title: 'PureHub Community', description: 'A clear path to support, report bugs, vote on the roadmap and contribute to free tools.', accent: 'emerald' },
}

export default function EverydayFlagshipSurface({ mode }: { mode: Mode }) {
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const wifiCopy = WIFI_COPY[locale]
  const meta = mode === 'wifi-analyzer'
    ? { eyebrow: wifiCopy.eyebrow, title: wifiCopy.title, description: wifiCopy.description, accent: 'sky' as const }
    : META[mode]
  return <div className="space-y-4"><FlagshipHero {...meta} />{mode === 'smart-flashlight' ? <Flashlight /> : mode === 'unit-converter' ? <Converter /> : mode === 'color-grabber' ? <ColorStudio /> : mode === 'deep-cleaner' ? <Cleaner /> : mode === 'wifi-analyzer' ? <WifiAnalyzer /> : mode === 'wallpaper-changer' ? <WallpaperStudio /> : mode === 'decision-wheel' ? <DecisionWheel /> : <Community />}</div>
}

function Flashlight() {
  const [active, setActive] = useState(false)
  const [brightness, setBrightness] = useState(100)
  const [mode, setMode] = useState<'steady' | 'pulse' | 'sos'>('steady')
  const [visible, setVisible] = useState(false)
  const lightRef = useRef<HTMLDivElement>(null)
  const wakeLock = useRef<{ release: () => Promise<void> } | null>(null)

  useEffect(() => {
    if (!active || mode === 'steady') return
    const sos = [200, 200, 200, 500, 500, 500, 200, 200, 200, 900]
    let index = 0
    const toggle = () => { setVisible((value) => !value); index = (index + 1) % sos.length; timer = window.setTimeout(toggle, mode === 'pulse' ? 450 : sos[index]) }
    let timer = window.setTimeout(toggle, mode === 'pulse' ? 450 : sos[0])
    return () => window.clearTimeout(timer)
  }, [active, mode])
  useEffect(() => () => { void wakeLock.current?.release() }, [])

  const toggle = async () => {
    const next = !active; setActive(next); setVisible(next)
    if (next) {
      try { wakeLock.current = await (navigator as Navigator & { wakeLock?: { request: (type: 'screen') => Promise<{ release: () => Promise<void> }> } }).wakeLock?.request('screen') ?? null } catch { wakeLock.current = null }
      void lightRef.current?.requestFullscreen?.()
      markToolSuccess('smart-flashlight', { headline: 'Light session started', detail: `${mode} light is active at ${brightness}% brightness.`, shareText: 'I used a private screen light with no ads or tracking.' })
    } else { await wakeLock.current?.release(); wakeLock.current = null; if (document.fullscreenElement) await document.exitFullscreen() }
  }

  return <Panel title="Light controls" subtitle="Uses the display as a dependable fallback. Camera torch support varies by browser and device.">
    <div ref={lightRef} className={`relative grid min-h-72 place-items-center overflow-hidden rounded-[28px] transition ${active && visible ? 'bg-white' : 'bg-slate-950'}`} style={{ filter: active ? `brightness(${Math.max(.15, brightness / 100)})` : undefined }}><Lightbulb className={`size-24 ${active && visible ? 'text-amber-400' : 'text-slate-700'}`} /><span className={`absolute bottom-5 rounded-full px-4 py-2 text-xs font-black ${active ? 'bg-slate-950 text-white' : 'bg-white/10 text-slate-300'}`}>{active ? `${mode.toUpperCase()} · ${brightness}%` : 'LIGHT OFF'}</span></div>
    <label className="mt-4 block text-sm font-black">Brightness <span className="float-right text-amber-700 dark:text-amber-300">{brightness}%</span><input className="mt-3 w-full accent-amber-500" type="range" min="15" max="100" value={brightness} onChange={(event) => setBrightness(Number(event.target.value))} /></label>
    <div className="mt-4 grid grid-cols-3 gap-2">{(['steady', 'pulse', 'sos'] as const).map((item) => <button key={item} onClick={() => { setMode(item); if (item === 'steady') setVisible(active) }} className={`min-h-11 rounded-xl border text-xs font-black uppercase ${mode === item ? 'border-amber-500 bg-amber-50 text-amber-900 dark:bg-amber-950 dark:text-amber-100' : 'border-slate-200 dark:border-slate-700'}`}>{item}</button>)}</div>
    <ActionButton className="mt-4 w-full" onClick={() => void toggle()}>{active ? 'Turn light off' : 'Turn light on'}</ActionButton>
    <p className="mt-3 text-xs leading-5 text-slate-500">SOS is a visual attention pattern, not a certified emergency beacon. Avoid flashing modes around people sensitive to light.</p>
  </Panel>
}

type UnitGroup = { label: string; units: Record<string, number>; offset?: Record<string, number> }
const UNIT_GROUPS: Record<string, UnitGroup> = {
  Length: { label: 'Length', units: { metre: 1, kilometre: 1000, centimetre: .01, millimetre: .001, inch: .0254, foot: .3048, yard: .9144, mile: 1609.344 } },
  Weight: { label: 'Weight', units: { kilogram: 1, gram: .001, milligram: .000001, ounce: .0283495, pound: .453592 } },
  Volume: { label: 'Volume', units: { litre: 1, millilitre: .001, 'US gallon': 3.78541, 'US cup': .236588, 'fluid ounce': .0295735 } },
  Speed: { label: 'Speed', units: { 'm/s': 1, 'km/h': .277778, mph: .44704, knot: .514444 } },
  Temperature: { label: 'Temperature', units: { Celsius: 1, Fahrenheit: 1, Kelvin: 1 } },
  Data: { label: 'Digital data', units: { byte: 1, kilobyte: 1000, megabyte: 1e6, gigabyte: 1e9, kibibyte: 1024, mebibyte: 1048576 } },
}

function convertValue(group: string, value: number, from: string, to: string) {
  if (group === 'Temperature') {
    const celsius = from === 'Celsius' ? value : from === 'Fahrenheit' ? (value - 32) * 5 / 9 : value - 273.15
    return to === 'Celsius' ? celsius : to === 'Fahrenheit' ? celsius * 9 / 5 + 32 : celsius + 273.15
  }
  const units = UNIT_GROUPS[group].units
  return value * units[from] / units[to]
}

function Converter() {
  const [group, setGroup] = useState('Length'); const [value, setValue] = useState('1'); const [from, setFrom] = useState('metre'); const [to, setTo] = useState('kilometre')
  const [recent, setRecent] = useState<string[]>(() => JSON.parse(localStorage.getItem('purehub.converter.recent') ?? '[]') as string[])
  const units = Object.keys(UNIT_GROUPS[group].units)
  const result = convertValue(group, Number(value) || 0, from, to)
  const setCategory = (next: string) => { const nextUnits = Object.keys(UNIT_GROUPS[next].units); setGroup(next); setFrom(nextUnits[0]); setTo(nextUnits[1] ?? nextUnits[0]) }
  const saveRecent = () => { const line = `${Number(value) || 0} ${from} = ${Number(result.toPrecision(10))} ${to}`; const next = [line, ...recent.filter((item) => item !== line)].slice(0, 8); setRecent(next); localStorage.setItem('purehub.converter.recent', JSON.stringify(next)); markToolSuccess('unit-converter', { headline: 'Conversion saved', detail: line, shareText: `I converted ${line} locally with PureHub.` }) }
  return <div className="grid gap-4 lg:grid-cols-[.8fr_1.2fr]"><Panel title="Conversion type" subtitle="Six useful categories with decimal and binary data units."><div className="grid grid-cols-2 gap-2">{Object.keys(UNIT_GROUPS).map((item) => <button key={item} onClick={() => setCategory(item)} className={`min-h-11 rounded-xl border px-2 text-xs font-black ${group === item ? 'border-sky-500 bg-sky-50 text-sky-900 dark:bg-sky-950 dark:text-sky-100' : 'border-slate-200 dark:border-slate-700'}`}>{UNIT_GROUPS[item].label}</button>)}</div>{recent.length ? <details className="mt-4 rounded-xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-black">Recent conversions ({recent.length})</summary><div className="mt-2 space-y-2">{recent.map((item) => <p key={item} className="rounded-lg bg-slate-50 p-2 text-xs dark:bg-slate-950">{item}</p>)}</div></details> : null}</Panel><Panel title="Instant result" subtitle="Calculated locally as you type."><FormInput inputMode="decimal" value={value} onChange={(event) => setValue(event.target.value)} aria-label="Value to convert" /><div className="mt-3 grid grid-cols-[1fr_auto_1fr] items-center gap-2"><select value={from} onChange={(event) => setFrom(event.target.value)} className="min-h-11 rounded-xl border border-slate-300 bg-white px-2 text-sm dark:border-slate-700 dark:bg-slate-900">{units.map((item) => <option key={item}>{item}</option>)}</select><button title="Swap units" onClick={() => { setFrom(to); setTo(from) }} className="grid size-11 place-items-center rounded-xl bg-slate-100 dark:bg-slate-800"><RefreshCw className="size-4" /></button><select value={to} onChange={(event) => setTo(event.target.value)} className="min-h-11 rounded-xl border border-slate-300 bg-white px-2 text-sm dark:border-slate-700 dark:bg-slate-900">{units.map((item) => <option key={item}>{item}</option>)}</select></div><div className="mt-4 rounded-2xl bg-slate-950 p-5 text-white"><span className="text-xs text-slate-400">Result</span><strong className="mt-2 block break-all text-3xl tabular-nums">{Number(result.toPrecision(10))}</strong><span className="text-sm text-sky-300">{to}</span></div><div className="mt-3 grid grid-cols-2 gap-2"><ActionButton onClick={saveRecent}><Save className="mr-1 inline size-4" />Remember</ActionButton><ActionButton tone="muted" onClick={() => void navigator.clipboard.writeText(`${result} ${to}`)}><Clipboard className="mr-1 inline size-4" />Copy</ActionButton></div></Panel></div>
}

function ColorStudio() {
  const canvasRef = useRef<HTMLCanvasElement>(null); const [source, setSource] = useState(''); const [picked, setPicked] = useState('#0f766e'); const [palette, setPalette] = useState<string[]>([])
  useEffect(() => { if (!source || !canvasRef.current) return; const image = new window.Image(); image.onload = () => { const canvas = canvasRef.current!; const max = 1200; const scale = Math.min(1, max / Math.max(image.width, image.height)); canvas.width = Math.round(image.width * scale); canvas.height = Math.round(image.height * scale); const context = canvas.getContext('2d')!; context.drawImage(image, 0, 0, canvas.width, canvas.height); const colors: string[] = []; for (let y = 1; y < 5; y++) for (let x = 1; x < 5; x++) { const [r, g, b] = context.getImageData(canvas.width * x / 5, canvas.height * y / 5, 1, 1).data; const color = rgbHex(r, g, b); if (!colors.some((item) => colorDistance(item, color) < 70)) colors.push(color) } setPalette(colors.slice(0, 6)) }; image.src = source }, [source])
  const contrastWhite = contrast(picked, '#ffffff'); const contrastBlack = contrast(picked, '#000000'); const textColor = contrastWhite >= contrastBlack ? '#ffffff' : '#000000'
  return <Panel title="Local image palette" subtitle="The image stays inside this browser; only a scaled preview is held in memory."><FormInput type="file" accept="image/*" onChange={(event) => { const file = event.target.files?.[0]; if (!file) return; const reader = new FileReader(); reader.onload = () => setSource(String(reader.result)); reader.readAsDataURL(file) }} />{source ? <div className="mt-4 grid gap-4 lg:grid-cols-[1.3fr_.7fr]"><canvas ref={canvasRef} className="max-h-[520px] w-full cursor-crosshair rounded-2xl bg-slate-100 object-contain dark:bg-slate-950" onClick={(event) => { const canvas = canvasRef.current!; const rect = canvas.getBoundingClientRect(); const [r, g, b] = canvas.getContext('2d')!.getImageData((event.clientX - rect.left) / rect.width * canvas.width, (event.clientY - rect.top) / rect.height * canvas.height, 1, 1).data; setPicked(rgbHex(r, g, b)) }} /><div className="space-y-3"><div className="grid min-h-36 place-items-center rounded-2xl border border-slate-200 text-center dark:border-slate-700" style={{ background: picked, color: textColor }}><div><strong className="text-2xl">{picked.toUpperCase()}</strong><p className="text-xs">Best text: {textColor === '#ffffff' ? 'white' : 'black'}</p></div></div><div className="grid grid-cols-2 gap-2"><Metric label="White contrast" value={`${contrastWhite.toFixed(2)}:1`} /><Metric label="Black contrast" value={`${contrastBlack.toFixed(2)}:1`} /></div><ActionButton className="w-full" onClick={() => { void navigator.clipboard.writeText(picked); markToolSuccess('color-grabber', { headline: 'Color copied', detail: `${picked.toUpperCase()} with contrast guidance is ready to use.`, shareText: `I picked ${picked.toUpperCase()} from an image locally with PureHub.` }) }}>Copy color</ActionButton></div></div> : <Empty icon={<Palette />} text="Choose an image, then tap anywhere to inspect its color." />}{palette.length ? <div className="mt-4"><strong className="text-sm">Extracted palette</strong><div className="mt-2 grid grid-cols-3 gap-2 sm:grid-cols-6">{palette.map((color) => <button key={color} onClick={() => setPicked(color)} className="min-h-20 rounded-xl border border-slate-200 p-2 text-[10px] font-black dark:border-slate-700" style={{ background: color, color: contrast(color, '#fff') > 3 ? '#fff' : '#000' }}>{color}</button>)}</div></div> : null}</Panel>
}

function Cleaner() {
  const [files, setFiles] = useState<File[]>([]); const [selected, setSelected] = useState<Set<number>>(new Set())
  const groups = useMemo(() => files.reduce<Record<string, { count: number; bytes: number }>>((result, file) => { const type = file.type.startsWith('image/') ? 'Images' : file.type.startsWith('video/') ? 'Videos' : file.type.startsWith('audio/') ? 'Audio' : file.type.includes('pdf') || file.type.includes('document') || file.type.includes('text') ? 'Documents' : 'Other'; const row = result[type] ?? { count: 0, bytes: 0 }; result[type] = { count: row.count + 1, bytes: row.bytes + file.size }; return result }, {}), [files])
  const duplicates = useMemo(() => files.reduce<Record<string, number[]>>((result, file, index) => { const key = `${file.name.toLowerCase()}:${file.size}`; (result[key] ??= []).push(index); return result }, {}), [files]); const duplicateIndexes = new Set(Object.values(duplicates).filter((rows) => rows.length > 1).flat())
  const selectedBytes = [...selected].reduce((sum, index) => sum + (files[index]?.size ?? 0), 0)
  return <Panel title="Safe storage review" subtitle="PureHub only reads files you explicitly select and never deletes them from the web."><FormInput type="file" multiple onChange={(event) => { setFiles(Array.from(event.target.files ?? [])); setSelected(new Set()) }} /><div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4"><Metric label="Reviewed" value={String(files.length)} /><Metric label="Total" value={bytes(files.reduce((sum, file) => sum + file.size, 0))} /><Metric label="Likely duplicates" value={String(duplicateIndexes.size)} /><Metric label="Selected" value={bytes(selectedBytes)} /></div>{Object.keys(groups).length ? <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">{Object.entries(groups).map(([name, row]) => <div key={name} className="rounded-xl bg-emerald-50 p-3 dark:bg-emerald-950/35"><strong className="text-sm">{name}</strong><p className="text-xs text-slate-500">{row.count} · {bytes(row.bytes)}</p></div>)}</div> : null}<div className="mt-4 max-h-80 space-y-2 overflow-auto">{files.map((file, index) => <label key={`${file.name}-${file.lastModified}-${index}`} className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700"><input type="checkbox" checked={selected.has(index)} onChange={() => setSelected((current) => { const next = new Set(current); if (next.has(index)) next.delete(index); else next.add(index); return next })} /><Archive className="size-4 text-emerald-600" /><div className="min-w-0 flex-1"><strong className="block truncate text-xs">{file.name}</strong><span className="text-[11px] text-slate-500">{bytes(file.size)}</span></div>{duplicateIndexes.has(index) ? <span className="rounded-full bg-amber-100 px-2 py-1 text-[10px] font-black text-amber-800">Review duplicate</span> : null}</label>)}</div>{files.length ? <div className="mt-4 flex items-start gap-2 rounded-xl bg-sky-50 p-3 text-xs leading-5 text-sky-900 dark:bg-sky-950/35 dark:text-sky-100"><ShieldCheck className="mt-0.5 size-4 shrink-0" />Selection is an estimate for review. Use your operating system or PureHub Android to confirm and perform recoverable cleanup.</div> : <Empty icon={<Archive />} text="Select a folder or group of files to receive a private storage summary." />}</Panel>
}

function WifiAnalyzer() {
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const copy = WIFI_COPY[locale]
  const dateLocale = { en: 'en-US', vi: 'vi-VN', zh: 'zh-CN', es: 'es-ES' }[locale]
  const getConnection = () => (navigator as Navigator & { connection?: { effectiveType?: string; downlink?: number; rtt?: number; saveData?: boolean; addEventListener?: (name: string, fn: () => void) => void; removeEventListener?: (name: string, fn: () => void) => void } }).connection
  type WebTest = { at: number; latency: number | null; jitter: number | null; loss: number }
  const [version, setVersion] = useState(0); const [latency, setLatency] = useState<number | null>(null); const [jitter, setJitter] = useState<number | null>(null); const [loss, setLoss] = useState<number | null>(null); const [testing, setTesting] = useState(false)
  const [history, setHistory] = useState<WebTest[]>(() => { try { return JSON.parse(localStorage.getItem('purehub.wifi.web-tests.v1') ?? '[]') as WebTest[] } catch { return [] } })
  useEffect(() => { const refresh = () => setVersion((value) => value + 1); window.addEventListener('online', refresh); window.addEventListener('offline', refresh); getConnection()?.addEventListener?.('change', refresh); return () => { window.removeEventListener('online', refresh); window.removeEventListener('offline', refresh); getConnection()?.removeEventListener?.('change', refresh) } }, [])
  void version; const connection = getConnection(); const online = navigator.onLine; const score = !online ? 0 : latency != null ? Math.max(10, Math.min(100, Math.round(110 - latency / 3))) : connection?.effectiveType === '4g' ? 85 : connection?.effectiveType === '3g' ? 60 : 70
  const test = async () => {
    setTesting(true)
    const samples: number[] = []; let failed = 0
    for (let index = 0; index < 10; index++) {
      const start = performance.now()
      try { const response = await fetch(`/favicon.ico?health=${Date.now()}-${index}`, { cache: 'no-store' }); if (!response.ok) throw new Error('request failed'); samples.push(Math.round(performance.now() - start)) } catch { failed++ }
    }
    const nextLatency = samples.length ? Math.round(samples.reduce((sum, value) => sum + value, 0) / samples.length) : null
    const differences = samples.slice(1).map((value, index) => Math.abs(value - samples[index]))
    const nextJitter = differences.length ? Math.round(differences.reduce((sum, value) => sum + value, 0) / differences.length) : null
    const nextLoss = failed * 10; setLatency(nextLatency); setJitter(nextJitter); setLoss(nextLoss)
    const row = { at: Date.now(), latency: nextLatency, jitter: nextJitter, loss: nextLoss }; const nextHistory = [row, ...history].slice(0, 10); setHistory(nextHistory); localStorage.setItem('purehub.wifi.web-tests.v1', JSON.stringify(nextHistory))
    if (nextLatency != null) markToolSuccess('wifi-analyzer', { headline: copy.successHeadline, detail: copy.successDetail(nextLatency, nextJitter ?? 0), shareText: copy.successShare(nextLatency) })
    setTesting(false)
  }
  const recommendation = !online ? copy.recommendations[3] : latency != null && (latency > 180 || (loss ?? 0) >= 20) ? copy.recommendations[2] : latency != null && latency > 90 ? copy.recommendations[1] : copy.recommendations[0]
  const clearHistory = () => { setHistory([]); localStorage.removeItem('purehub.wifi.web-tests.v1') }
  return <div className="space-y-4">
    <Panel title={copy.panelTitle} subtitle={copy.panelSubtitle}>
      <div className="grid gap-4 lg:grid-cols-[.7fr_1.3fr]">
        <div className="grid place-items-center rounded-[24px] bg-slate-950 p-6 text-white"><Gauge className="size-8 text-sky-300" /><strong className="mt-3 text-6xl tabular-nums">{score}</strong><span className="text-xs font-black text-slate-400">{copy.healthScore}</span><div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-white/10"><div className="h-full rounded-full bg-sky-400" style={{ width: `${score}%` }} /></div></div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3"><Metric label={copy.status} value={online ? copy.online : copy.offline} /><Metric label={copy.networkClass} value={connection?.effectiveType?.toUpperCase() ?? copy.notExposed} /><Metric label={copy.browserDownlink} value={connection?.downlink ? `${connection.downlink} Mbps` : copy.notExposed} /><Metric label={copy.browserRtt} value={connection?.rtt ? `${connection.rtt} ms` : copy.notExposed} /><Metric label={copy.averageLatency} value={latency == null ? copy.notTested : `${latency} ms`} /><Metric label={copy.jitter} value={jitter == null ? copy.notTested : `${jitter} ms`} /><Metric label={copy.requestLoss} value={loss == null ? copy.notTested : `${loss}%`} /><Metric label={copy.dataSaver} value={connection?.saveData ? copy.enabled : copy.disabled} /></div>
      </div>
      <div className="mt-4 rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sky-950 dark:border-sky-900 dark:bg-sky-950/35 dark:text-sky-100"><strong className="text-sm">{copy.recommendation}</strong><p className="mt-1 text-xs leading-5">{recommendation}</p></div>
      <ActionButton className="mt-4 w-full" disabled={testing || !online} onClick={() => void test()}><Network className="mr-2 inline size-4" />{testing ? copy.checking : copy.runTest}</ActionButton>
      {history.length ? <details className="mt-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-black">{copy.history} ({history.length}/10)</summary><div className="mt-2 space-y-2">{history.map((item) => <div key={item.at} className="flex justify-between gap-3 text-xs text-slate-500"><span>{new Date(item.at).toLocaleString(dateLocale)}</span><strong>{item.latency ?? '--'} ms · {copy.jitter.toLowerCase()} {item.jitter ?? '--'} · {copy.requestLoss.toLowerCase()} {item.loss}%</strong></div>)}</div><button type="button" className="mt-3 text-xs font-black text-rose-600 dark:text-rose-300" onClick={clearHistory}>{copy.clearHistory}</button></details> : null}
      <p className="mt-3 text-xs leading-5 text-slate-500">{copy.testNote}</p>
    </Panel>
    <section className="rounded-[24px] border border-emerald-200 bg-gradient-to-br from-emerald-50 to-sky-50 p-5 dark:border-emerald-900 dark:from-emerald-950/45 dark:to-sky-950/35">
      <div className="flex items-start gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-emerald-600 text-white"><Download className="size-5" /></span><div><h3 className="font-black">{copy.androidTitle}</h3><p className="mt-1 text-xs leading-5 text-slate-600 dark:text-slate-300">{copy.androidDescription}</p></div></div>
      <div className="mt-4 grid gap-2 sm:grid-cols-2">{copy.androidFeatures.map((feature) => <div key={feature} className="flex items-center gap-2 rounded-xl bg-white/80 p-3 text-xs font-bold dark:bg-slate-950/55"><CheckCircle2 className="size-4 shrink-0 text-emerald-600" />{feature}</div>)}</div>
      <a href={`/${locale}/download?utm_source=wifi-pwa&utm_campaign=native-capability`} className="mt-4 flex min-h-12 w-full items-center justify-center rounded-xl bg-emerald-700 px-4 text-sm font-black text-white transition hover:bg-emerald-800"><Download className="mr-2 size-4" />{copy.installAndroid}</a>
    </section>
  </div>
}

function WallpaperStudio() {
  const [images, setImages] = useState<string[]>([]); const [selected, setSelected] = useState(0); const [fit, setFit] = useState<'cover' | 'contain'>('cover'); const [tone, setTone] = useState<'normal' | 'warm' | 'mono'>('normal')
  const load = async (files: File[]) => { const urls = await Promise.all(files.map((file) => new Promise<string>((resolve) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.readAsDataURL(file) }))); setImages(urls); setSelected(0) }
  const download = () => { if (!images[selected]) return; const link = document.createElement('a'); link.href = images[selected]; link.download = `purehub-wallpaper-${selected + 1}.jpg`; link.click(); markToolSuccess('wallpaper-changer', { headline: 'Wallpaper export ready', detail: 'Your selected original was prepared locally with no upload or feed involved.', shareText: 'I prepared a wallpaper locally with PureHub.' }) }
  const filter = tone === 'warm' ? 'sepia(.28) saturate(1.1)' : tone === 'mono' ? 'grayscale(1)' : 'none'
  return <Panel title="Private wallpaper collection" subtitle="Preview phone framing and export your chosen original without uploading it."><FormInput type="file" accept="image/*" multiple onChange={(event) => void load(Array.from(event.target.files ?? []))} />{images.length ? <><div className="mx-auto mt-4 aspect-[9/16] max-h-[560px] overflow-hidden rounded-[32px] border-[10px] border-slate-950 bg-slate-900 shadow-xl"><img src={images[selected]} alt="Selected wallpaper preview" className={`h-full w-full ${fit === 'cover' ? 'object-cover' : 'object-contain'}`} style={{ filter }} /></div><div className="mt-4 grid grid-cols-2 gap-2"><select value={fit} onChange={(event) => setFit(event.target.value as 'cover' | 'contain')} className="min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold dark:border-slate-700 dark:bg-slate-900"><option value="cover">Fill screen</option><option value="contain">Fit whole image</option></select><select value={tone} onChange={(event) => setTone(event.target.value as typeof tone)} className="min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold dark:border-slate-700 dark:bg-slate-900"><option value="normal">Original tone</option><option value="warm">Warm preview</option><option value="mono">Mono preview</option></select></div><div className="mt-3 flex gap-2 overflow-x-auto">{images.map((item, index) => <button key={`${item.slice(0, 30)}-${index}`} onClick={() => setSelected(index)} className={`h-24 w-16 shrink-0 overflow-hidden rounded-xl ${selected === index ? 'ring-3 ring-violet-500' : ''}`}><img src={item} alt={`Wallpaper ${index + 1}`} className="h-full w-full object-cover" /></button>)}</div><ActionButton className="mt-4 w-full" onClick={download}><Download className="mr-2 inline size-4" />Download selected original</ActionButton><p className="mt-2 text-xs text-slate-500">Tone controls are previews. Android can apply Home, Lock or both screens directly.</p></> : <Empty icon={<Image />} text="Choose one or more local images to start a private collection." />}</Panel>
}

function DecisionWheel() {
  const [text, setText] = useState(() => localStorage.getItem('purehub.wheel.options') ?? 'Coffee\nTea\nJuice\nWater'); const [rotation, setRotation] = useState(0); const [result, setResult] = useState(''); const [history, setHistory] = useState<string[]>(() => JSON.parse(localStorage.getItem('purehub.wheel.history') ?? '[]') as string[])
  const options = text.split('\n').map((item) => item.trim()).filter(Boolean).slice(0, 24); const colors = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#f43f5e', '#14b8a6']; const gradient = options.length ? `conic-gradient(${options.map((_, index) => `${colors[index % colors.length]} ${index / options.length * 360}deg ${(index + 1) / options.length * 360}deg`).join(',')})` : '#94a3b8'
  const spin = () => { if (!options.length) return; const random = new Uint32Array(1); crypto.getRandomValues(random); const winner = options[random[0] % options.length]; setRotation((value) => value + 1800 + random[0] % 360); setResult(winner); const next = [winner, ...history].slice(0, 10); setHistory(next); localStorage.setItem('purehub.wheel.history', JSON.stringify(next)); markToolSuccess('decision-wheel', { headline: 'Decision selected', detail: `The private random wheel chose “${winner}”.`, shareText: `PureHub's private decision wheel chose: ${winner}.` }) }
  return <div className="grid gap-4 lg:grid-cols-[.8fr_1.2fr]"><Panel title="Reusable list" subtitle="One option per line, up to 24."><FormTextArea rows={9} value={text} onChange={(event) => setText(event.target.value)} /><div className="mt-3 grid grid-cols-2 gap-2"><ActionButton onClick={() => { localStorage.setItem('purehub.wheel.options', text) }}><Save className="mr-1 inline size-4" />Save list</ActionButton><ActionButton tone="muted" onClick={() => { setText(''); setResult('') }}><Trash2 className="mr-1 inline size-4" />Clear</ActionButton></div>{history.length ? <details className="mt-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-black">Private result history</summary><div className="mt-2 flex flex-wrap gap-2">{history.map((item, index) => <span key={`${item}-${index}`} className="rounded-full bg-slate-100 px-3 py-1 text-xs dark:bg-slate-800">{item}</span>)}</div></details> : null}</Panel><Panel title="Secure random spin" subtitle="Uses the browser cryptographic random generator."><div className="flex flex-col items-center"><div className="relative"><span className="absolute left-1/2 top-[-8px] z-10 h-0 w-0 -translate-x-1/2 border-x-[12px] border-b-[20px] border-x-transparent border-b-rose-400" /><div className="grid size-72 place-items-center rounded-full border-8 border-white shadow-xl transition-transform duration-[2200ms] ease-out dark:border-slate-800" style={{ background: gradient, transform: `rotate(${rotation}deg)` }}><div className="grid size-24 place-items-center rounded-full bg-white/95 p-2 text-center text-sm font-black text-slate-950">{result || 'Ready'}</div></div></div><ActionButton className="mt-5 w-full" disabled={!options.length} onClick={spin}><WandSparkles className="mr-2 inline size-4" />Spin the wheel</ActionButton>{result ? <ActionButton tone="muted" className="mt-2 w-full" onClick={() => navigator.share ? void navigator.share({ title: 'PureHub Decision Wheel', text: `The wheel chose: ${result}` }) : void navigator.clipboard.writeText(result)}><Share2 className="mr-2 inline size-4" />Share result</ActionButton> : null}</div></Panel></div>
}

function Community() {
  const channels = [
    { title: 'Telegram community', copy: 'Fast support, releases and community discussion.', href: 'https://t.me/purehubaaa', icon: <Users /> },
    { title: 'GitHub project', copy: 'Source code, issues, roadmap and pull requests.', href: 'https://github.com/shoyrulove-dev/PureHub', icon: <Code2 /> },
    { title: 'Report a bug', copy: 'Share reproducible steps without exposing private data.', href: 'https://github.com/shoyrulove-dev/PureHub/issues/new', icon: <Droplets /> },
    { title: 'Try all tools', copy: 'Open the complete free, no-ads utility catalog.', href: '/en/tools', icon: <Sparkles /> },
  ]
  return <Panel title="Choose how to contribute" subtitle="No Pro tier, referral gate or paywall—community participation improves the same free product for everyone."><div className="grid gap-3 sm:grid-cols-2">{channels.map((item) => <a key={item.title} href={item.href} target={item.href.startsWith('http') ? '_blank' : undefined} rel="noreferrer" className="group rounded-2xl border border-slate-200 p-4 transition hover:-translate-y-0.5 hover:border-emerald-400 hover:shadow-md dark:border-slate-700"><span className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-200">{item.icon}</span><strong className="mt-3 block">{item.title}</strong><p className="mt-1 text-xs leading-5 text-slate-500">{item.copy}</p></a>)}</div><div className="mt-4 grid gap-2 sm:grid-cols-3"><Contribution label="1. Try" copy="Use one tool for a real task." /><Contribution label="2. Report" copy="Tell us what was confusing." /><Contribution label="3. Improve" copy="Vote, translate, code or share." /></div><p className="mt-4 flex items-start gap-2 rounded-xl bg-emerald-50 p-3 text-xs leading-5 text-emerald-900 dark:bg-emerald-950/35 dark:text-emerald-100"><CheckCircle2 className="mt-0.5 size-4 shrink-0" />PureHub does not unlock hidden features for joining. Every mini-app remains free and ad-free for everyone.</p></Panel>
}

function Contribution({ label, copy }: { label: string; copy: string }) { return <div className="rounded-xl bg-slate-50 p-3 dark:bg-slate-950"><strong className="text-xs">{label}</strong><p className="mt-1 text-[11px] text-slate-500">{copy}</p></div> }
function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-xl border border-slate-200 p-3 dark:border-slate-700"><span className="block text-[11px] text-slate-500">{label}</span><strong className="mt-1 block text-lg tabular-nums">{value}</strong></div> }
function Empty({ icon, text }: { icon: React.ReactNode; text: string }) { return <div className="mt-4 grid min-h-40 place-items-center rounded-2xl border border-dashed border-slate-300 p-5 text-center text-sm text-slate-500 dark:border-slate-700"><div><span className="mx-auto grid size-11 place-items-center rounded-xl bg-slate-100 dark:bg-slate-800">{icon}</span><p className="mt-3">{text}</p></div></div> }
function bytes(value: number) { if (!value) return '0 B'; const units = ['B', 'KB', 'MB', 'GB']; const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1); return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}` }
function rgbHex(r: number, g: number, b: number) { return `#${[r, g, b].map((value) => Math.round(value).toString(16).padStart(2, '0')).join('')}` }
function hexRgb(hex: string) { const value = Number.parseInt(hex.slice(1), 16); return [(value >> 16) & 255, (value >> 8) & 255, value & 255] }
function colorDistance(a: string, b: string) { const first = hexRgb(a); const second = hexRgb(b); return Math.sqrt(first.reduce((sum, value, index) => sum + (value - second[index]) ** 2, 0)) }
function contrast(a: string, b: string) { const luminance = (hex: string) => { const channels = hexRgb(hex).map((value) => { const normalized = value / 255; return normalized <= .03928 ? normalized / 12.92 : ((normalized + .055) / 1.055) ** 2.4 }); return .2126 * channels[0] + .7152 * channels[1] + .0722 * channels[2] }; const [high, low] = [luminance(a), luminance(b)].sort((x, y) => y - x); return (high + .05) / (low + .05) }

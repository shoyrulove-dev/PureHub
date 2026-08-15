import { useEffect, useMemo, useRef, useState } from 'react'
import { Activity, Compass, Download, Gauge, Lock, Maximize2, Mic, RotateCcw, ShieldCheck } from 'lucide-react'
import { ActionButton } from '../MiniAppPrimitives'
import { trackProductEvent } from '../../../lib/community-api'

type SensorMode = 'compass' | 'level' | 'sound'
type PermissionState = 'idle' | 'active' | 'denied' | 'unsupported'

export default function SensorSuiteSurface({ mode }: { mode: SensorMode }) {
  const suiteRef = useRef<HTMLElement>(null)
  const title = mode === 'compass' ? 'Compass' : mode === 'level' ? 'Bubble Level' : 'Sound Meter'
  const locale = window.location.pathname.split('/')[1] || 'en'

  return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-sky-50 via-white to-violet-50 p-5 dark:from-sky-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-start gap-3">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-sky-600 text-white">{mode === 'compass' ? <Compass /> : mode === 'level' ? <Gauge /> : <Mic />}</span>
        <div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-sky-700 dark:text-sky-300">SENSOR SUITE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">{title}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Clear live readings, local calibration, and private on-device processing.</p></div>
        <ShieldCheck className="hidden size-5 text-emerald-600 sm:block" />
      </div>
      <nav className="mt-4 grid grid-cols-3 gap-2">{([['compass', 'Compass', Compass], ['level', 'Level', Gauge], ['sound', 'Sound', Mic]] as const).map(([id, label, Icon]) => <a key={id} href={`/${locale}/${id === 'level' ? 'bubble-level' : id === 'sound' ? 'decibel-meter' : 'compass'}`} className={`flex min-h-11 items-center justify-center gap-1 rounded-xl border text-xs font-black ${mode === id ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 bg-white/70 dark:border-slate-700 dark:bg-slate-800'}`}><Icon className="size-4" />{label}</a>)}</nav>
    </header>
    <div className="p-4 sm:p-5">
      <button onClick={() => void suiteRef.current?.requestFullscreen?.()} className="mb-4 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-sky-200 text-xs font-black text-sky-800 dark:border-sky-800 dark:text-sky-200"><Maximize2 className="size-4" />Fullscreen instrument</button>
      {mode === 'compass' ? <CompassPanel /> : mode === 'level' ? <LevelPanel /> : <SoundPanel />}
    </div>
  </section>
}

function CompassPanel() {
  const [rawHeading, setRawHeading] = useState<number | null>(null)
  const [offset, setOffset] = useState(() => Number(localStorage.getItem('purehub.compass.offset.v1') || 0))
  const [accuracy, setAccuracy] = useState<'stable' | 'check'>('stable')
  const previousRef = useRef<{ value: number; at: number } | null>(null)
  const [held, setHeld] = useState<number | null>(null)
  const [permission, setPermission] = useState<PermissionState>('idle')
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
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? 'Compass active' : 'Enable compass'}</ActionButton><ActionButton tone="muted" disabled={heading == null} onClick={() => setHeld((value) => value == null ? heading : null)}>{held == null ? 'Hold reading' : 'Resume live'}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={rawHeading == null} onClick={() => { const next = rawHeading == null ? 0 : -rawHeading; setOffset(next); localStorage.setItem('purehub.compass.offset.v1', String(next)) }}>Set current as North</ActionButton><ActionButton tone="muted" onClick={() => { setOffset(0); localStorage.removeItem('purehub.compass.offset.v1') }}><RotateCcw className="mr-1 inline size-4" />Reset calibration</ActionButton></div>
    <p className="mt-3 text-xs text-slate-500">Move the phone in a figure-eight before use. Magnetic readings are estimates and may be affected by cases or nearby metal.</p>
  </div>
}

function LevelPanel() {
  const [raw, setRaw] = useState({ x: 0, y: 0 })
  const [zero, setZero] = useState(() => { try { return JSON.parse(localStorage.getItem('purehub.level.zero.v1') ?? '{"x":0,"y":0}') as { x: number; y: number } } catch { return { x: 0, y: 0 } } })
  const [held, setHeld] = useState<{ x: number; y: number } | null>(null)
  const [permission, setPermission] = useState<PermissionState>('idle')
  const [levelMode, setLevelMode] = useState<'flat' | 'horizontal' | 'vertical'>('flat')
  const [tolerance, setTolerance] = useState(() => Number(localStorage.getItem('purehub.level.tolerance.v1') || 0.5))
  const [settled, setSettled] = useState(false)
  const [soundCue, setSoundCue] = useState(false)
  const previous = useRef({ x: 0, y: 0, at: 0 })
  const [moving, setMoving] = useState(false)
  const angles = held ?? { x: Math.round((raw.x - zero.x) * 10) / 10, y: Math.round((raw.y - zero.y) * 10) / 10 }

  useEffect(() => {
    if (permission !== 'active') return
    let movementTimer = 0
    const listener = (event: DeviceOrientationEvent) => {
      const next = { x: event.gamma ?? 0, y: event.beta ?? 0 }
      const delta = Math.abs(next.x - previous.current.x) + Math.abs(next.y - previous.current.y)
      if (previous.current.at && delta > 3) {
        setMoving(true)
        window.clearTimeout(movementTimer)
        movementTimer = window.setTimeout(() => setMoving(false), 650)
      }
      previous.current = { ...next, at: Date.now() }
      setRaw(next)
    }
    window.addEventListener('deviceorientation', listener, { passive: true })
    return () => { window.clearTimeout(movementTimer); window.removeEventListener('deviceorientation', listener) }
  }, [permission])

  const start = async () => {
    if (!('DeviceOrientationEvent' in window)) { setPermission('unsupported'); return }
    try {
      const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
      if (orientation.requestPermission && await orientation.requestPermission() !== 'granted') { setPermission('denied'); return }
      setPermission('active')
    } catch { setPermission('denied') }
  }
  const isLevel = levelMode === 'flat'
    ? Math.abs(angles.x) <= tolerance && Math.abs(angles.y) <= tolerance
    : levelMode === 'horizontal'
      ? Math.abs(angles.x) <= tolerance
      : Math.abs(angles.y) <= tolerance

  useEffect(() => {
    if (permission !== 'active' || moving || !isLevel || held) { setSettled(false); return }
    const timer = window.setTimeout(() => {
      setSettled(true)
      if ('vibrate' in navigator) navigator.vibrate(45)
      if (soundCue) {
        const audio = new AudioContext()
        const oscillator = audio.createOscillator(); const gain = audio.createGain()
        oscillator.frequency.value = 660; gain.gain.value = 0.035
        oscillator.connect(gain).connect(audio.destination); oscillator.start(); oscillator.stop(audio.currentTime + 0.09)
        oscillator.onended = () => void audio.close()
      }
      const day = new Date().toISOString().slice(0, 10)
      const key = `purehub-complete-bubble-level-${day}`
      if (!localStorage.getItem(key)) { localStorage.setItem(key, 'true'); void trackProductEvent('bubble-level', 'complete') }
    }, 1800)
    return () => window.clearTimeout(timer)
  }, [held, isLevel, moving, permission, soundCue])

  return <div>
    <div className="mb-4 grid grid-cols-3 gap-2">{([['flat', 'Surface'], ['horizontal', 'Edge X'], ['vertical', 'Edge Y']] as const).map(([value, label]) => <button key={value} type="button" onClick={() => setLevelMode(value)} className={`min-h-11 rounded-xl border text-xs font-black ${levelMode === value ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 dark:border-slate-700'}`}>{label}</button>)}</div>
    <div className={`relative mx-auto aspect-square max-w-72 overflow-hidden rounded-[32px] border-8 transition-colors ${settled ? 'border-emerald-500 bg-emerald-50 shadow-[0_0_40px_rgba(16,185,129,.24)]' : isLevel ? 'border-emerald-300 bg-emerald-50' : 'border-slate-100 bg-sky-50'} dark:bg-slate-950`}><div className="absolute left-0 top-1/2 h-px w-full bg-slate-300" /><div className="absolute left-1/2 top-0 h-full w-px bg-slate-300" /><div className="absolute left-1/2 top-1/2 size-20 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-dashed border-emerald-500/60" /><span className={`absolute size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white shadow-xl transition-all duration-150 ${settled ? 'bg-emerald-500' : 'bg-sky-500'}`} style={{ left: `${50 + Math.max(-35, Math.min(35, levelMode === 'vertical' ? 0 : angles.x))}%`, top: `${50 + Math.max(-35, Math.min(35, levelMode === 'horizontal' ? 0 : angles.y))}%` }} /></div>
    <PermissionNotice state={permission} />
    <p role="status" className={`mt-3 rounded-xl px-3 py-2 text-center text-xs font-bold ${settled ? 'bg-emerald-500/15 text-emerald-800 dark:text-emerald-200' : moving ? 'bg-amber-500/15 text-amber-800 dark:text-amber-200' : 'bg-slate-500/8 text-slate-600 dark:text-slate-300'}`}>{settled ? 'Level confirmed · reading held steady' : moving ? 'Device moving · wait for a stable reading' : isLevel ? 'Inside tolerance · hold still to confirm' : 'Move the bubble into the center target'}</p>
    <div className="mt-3 grid grid-cols-2 gap-2 text-center"><Metric label="Left / right" value={`${angles.x}°`} /><Metric label="Front / back" value={`${angles.y}°`} /></div>
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? 'Level active' : 'Enable level'}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setHeld((value) => value == null ? angles : null)}>{held == null ? 'Hold reading' : 'Resume live'}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => { setZero(raw); localStorage.setItem('purehub.level.zero.v1', JSON.stringify(raw)) }}>Save current zero</ActionButton><ActionButton tone="muted" onClick={() => { setZero({ x: 0, y: 0 }); localStorage.removeItem('purehub.level.zero.v1') }}><RotateCcw className="mr-1 inline size-4" />Reset zero</ActionButton></div>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">Accuracy & cues</summary><p className="mt-3 text-xs text-slate-500">Tolerance</p><div className="mt-2 grid grid-cols-3 gap-2">{[0.2, 0.5, 1].map((value) => <button key={value} type="button" onClick={() => { setTolerance(value); localStorage.setItem('purehub.level.tolerance.v1', String(value)) }} className={`min-h-10 rounded-xl border text-xs font-black ${tolerance === value ? 'border-violet-500 bg-violet-500/10 text-violet-700 dark:text-violet-300' : 'border-slate-200 dark:border-slate-700'}`}>±{value}°</button>)}</div><label className="mt-3 flex min-h-11 items-center justify-between gap-3 text-xs font-bold"><span>Sound when level</span><input type="checkbox" checked={soundCue} onChange={(event) => setSoundCue(event.target.checked)} className="size-5 accent-violet-600" /></label><p className="mt-2 text-xs leading-5 text-slate-500">Calibrate on a known-flat reference. Phone sensors are useful estimates, not certified measurement tools.</p></details>
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
    <ActionButton className="mt-4 w-full" onClick={() => active ? stop() : void start()}>{active ? 'Stop listening' : 'Start sound meter'}</ActionButton>
    <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="cursor-pointer text-sm font-bold">Calibration & export</summary><label className="mt-3 block text-xs font-semibold">Reference offset {calibration > 0 ? '+' : ''}{calibration} dB<input className="mt-2 w-full accent-violet-600" type="range" min="-20" max="20" value={calibration} onChange={(event) => { const next = Number(event.target.value); setCalibration(next); localStorage.setItem('purehub.sound.calibration.v1', String(next)) }} /></label><ActionButton tone="muted" className="mt-3 w-full" disabled={!samples.length} onClick={() => { const blob = new Blob([`sample,estimated_db\n${samples.map((value, index) => `${index + 1},${value}`).join('\n')}`], { type: 'text/csv' }); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'purehub-sound-readings.csv'; anchor.click(); URL.revokeObjectURL(url) }}><Download className="size-4" />Export CSV</ActionButton></details>
    <p className="mt-3 text-xs text-slate-500">Microphone samples stay in this browser and are discarded when you stop. Values are estimates, not certified safety measurements.</p>
  </div>
}

function PermissionNotice({ state }: { state: PermissionState }) {
  if (state === 'idle' || state === 'active') return null
  return <div role="status" className="mt-4 flex items-start gap-2 rounded-2xl border border-amber-200 bg-amber-50 p-3 text-left text-xs leading-5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/35 dark:text-amber-100"><Lock className="mt-0.5 size-4 shrink-0" />{state === 'unsupported' ? 'This browser does not expose the required sensor. Try PureHub on a supported phone.' : 'Permission was not granted. Enable sensor or microphone access in browser settings, then try again.'}</div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl border border-slate-200 p-3 text-center dark:border-slate-700"><span className="block text-xs text-slate-500">{label}</span><strong className="text-xl tabular-nums">{value}</strong></div> }

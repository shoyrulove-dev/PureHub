import { useEffect, useMemo, useRef, useState } from 'react'
import { Activity, Compass, Gauge, Lock, Maximize2, Mic, RotateCcw, ShieldCheck } from 'lucide-react'
import { ActionButton } from '../MiniAppPrimitives'

type SensorMode = 'compass' | 'level' | 'sound'
type PermissionState = 'idle' | 'active' | 'denied' | 'unsupported'

export default function SensorSuiteSurface({ mode }: { mode: SensorMode }) {
  const suiteRef = useRef<HTMLElement>(null)
  const title = mode === 'compass' ? 'Compass' : mode === 'level' ? 'Bubble Level' : 'Sound Meter'

  return <section ref={suiteRef} className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm fullscreen:overflow-auto dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-sky-50 via-white to-violet-50 p-5 dark:from-sky-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-start gap-3">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-sky-600 text-white">{mode === 'compass' ? <Compass /> : mode === 'level' ? <Gauge /> : <Mic />}</span>
        <div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-sky-700 dark:text-sky-300">SENSOR SUITE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">{title}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Clear live readings, local calibration, and private on-device processing.</p></div>
        <ShieldCheck className="hidden size-5 text-emerald-600 sm:block" />
      </div>
      <nav className="mt-4 grid grid-cols-3 gap-2">{([['compass', 'Compass', Compass], ['level', 'Level', Gauge], ['sound', 'Sound', Mic]] as const).map(([id, label, Icon]) => <a key={id} href={`/en/${id === 'level' ? 'bubble-level' : id === 'sound' ? 'decibel-meter' : 'compass'}`} className={`flex min-h-11 items-center justify-center gap-1 rounded-xl border text-xs font-black ${mode === id ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 bg-white/70 dark:border-slate-700 dark:bg-slate-800'}`}><Icon className="size-4" />{label}</a>)}</nav>
    </header>
    <div className="p-4 sm:p-5">
      <button onClick={() => void suiteRef.current?.requestFullscreen?.()} className="mb-4 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-sky-200 text-xs font-black text-sky-800 dark:border-sky-800 dark:text-sky-200"><Maximize2 className="size-4" />Fullscreen instrument</button>
      {mode === 'compass' ? <CompassPanel /> : mode === 'level' ? <LevelPanel /> : <SoundPanel />}
    </div>
  </section>
}

function CompassPanel() {
  const [rawHeading, setRawHeading] = useState<number | null>(null)
  const [offset, setOffset] = useState(0)
  const [held, setHeld] = useState<number | null>(null)
  const [permission, setPermission] = useState<PermissionState>('idle')
  const heading = held ?? (rawHeading == null ? null : (rawHeading + offset + 360) % 360)

  useEffect(() => {
    if (permission !== 'active') return
    const listener = (event: DeviceOrientationEvent & { webkitCompassHeading?: number }) => {
      const value = typeof event.webkitCompassHeading === 'number' ? event.webkitCompassHeading : event.alpha == null ? null : 360 - event.alpha
      setRawHeading(value == null ? null : Math.round((value + 360) % 360))
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
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? 'Compass active' : 'Enable compass'}</ActionButton><ActionButton tone="muted" disabled={heading == null} onClick={() => setHeld((value) => value == null ? heading : null)}>{held == null ? 'Hold reading' : 'Resume live'}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={rawHeading == null} onClick={() => setOffset(rawHeading == null ? 0 : -rawHeading)}>Set current as North</ActionButton><ActionButton tone="muted" onClick={() => setOffset(0)}><RotateCcw className="mr-1 inline size-4" />Reset calibration</ActionButton></div>
    <p className="mt-3 text-xs text-slate-500">Move the phone in a figure-eight before use. Magnetic readings are estimates and may be affected by cases or nearby metal.</p>
  </div>
}

function LevelPanel() {
  const [raw, setRaw] = useState({ x: 0, y: 0 })
  const [zero, setZero] = useState({ x: 0, y: 0 })
  const [held, setHeld] = useState<{ x: number; y: number } | null>(null)
  const [permission, setPermission] = useState<PermissionState>('idle')
  const angles = held ?? { x: Math.round((raw.x - zero.x) * 10) / 10, y: Math.round((raw.y - zero.y) * 10) / 10 }

  useEffect(() => {
    if (permission !== 'active') return
    const listener = (event: DeviceOrientationEvent) => setRaw({ x: event.gamma ?? 0, y: event.beta ?? 0 })
    window.addEventListener('deviceorientation', listener, { passive: true })
    return () => window.removeEventListener('deviceorientation', listener)
  }, [permission])

  const start = async () => {
    if (!('DeviceOrientationEvent' in window)) { setPermission('unsupported'); return }
    try {
      const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
      if (orientation.requestPermission && await orientation.requestPermission() !== 'granted') { setPermission('denied'); return }
      setPermission('active')
    } catch { setPermission('denied') }
  }
  const flat = Math.abs(angles.x) <= 1 && Math.abs(angles.y) <= 1

  return <div>
    <div className={`relative mx-auto aspect-square max-w-72 overflow-hidden rounded-[32px] border-8 ${flat ? 'border-emerald-400 bg-emerald-50' : 'border-slate-100 bg-sky-50'} dark:bg-slate-950`}><div className="absolute left-0 top-1/2 h-px w-full bg-slate-300" /><div className="absolute left-1/2 top-0 h-full w-px bg-slate-300" /><span className="absolute size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white bg-sky-500 shadow-xl transition-all duration-150" style={{ left: `${50 + Math.max(-35, Math.min(35, angles.x))}%`, top: `${50 + Math.max(-35, Math.min(35, angles.y))}%` }} /></div>
    <PermissionNotice state={permission} />
    <div className="mt-4 grid grid-cols-2 gap-2 text-center"><Metric label="Left / right" value={`${angles.x}°`} /><Metric label="Front / back" value={`${angles.y}°`} /></div>
    <div className="mt-4 grid grid-cols-2 gap-2"><ActionButton onClick={() => void start()}>{permission === 'active' ? 'Level active' : 'Enable level'}</ActionButton><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setHeld((value) => value == null ? angles : null)}>{held == null ? 'Hold reading' : 'Resume live'}</ActionButton></div>
    <div className="mt-2 grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={permission !== 'active'} onClick={() => setZero(raw)}>Set current as zero</ActionButton><ActionButton tone="muted" onClick={() => setZero({ x: 0, y: 0 })}><RotateCcw className="mr-1 inline size-4" />Reset zero</ActionButton></div>
  </div>
}

function SoundPanel() {
  const [active, setActive] = useState(false)
  const [level, setLevel] = useState(0)
  const [samples, setSamples] = useState<number[]>([])
  const [permission, setPermission] = useState<PermissionState>('idle')
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
      const tick = () => { analyser.getByteTimeDomainData(data); const rms = Math.sqrt(data.reduce((sum, value) => sum + ((value - 128) / 128) ** 2, 0) / data.length); const estimate = Math.max(0, Math.min(100, Math.round(20 * Math.log10(Math.max(rms, .0001)) + 94))); setLevel(estimate); setSamples((values) => [...values.slice(-59), estimate]); frame.current = requestAnimationFrame(tick) }
      tick()
    } catch { setPermission('denied'); stop() }
  }
  const path = useMemo(() => samples.map((value, index) => `${index === 0 ? 'M' : 'L'} ${(index / Math.max(1, samples.length - 1)) * 100} ${40 - value * .36}`).join(' '), [samples])

  return <div>
    <div className="rounded-[28px] bg-slate-950 p-6 text-center text-white"><Activity className="mx-auto size-7 text-violet-300" /><strong className="mt-3 block text-6xl tabular-nums">{level}</strong><span className="text-sm font-black text-slate-400">estimated dB</span><div className="mt-6 h-4 overflow-hidden rounded-full bg-white/10"><div className={`h-full rounded-full transition-all ${level > 85 ? 'bg-rose-500' : level > 65 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${level}%` }} /></div>{samples.length > 1 ? <svg viewBox="0 0 100 40" className="mt-4 h-20 w-full overflow-visible" preserveAspectRatio="none" aria-label="Recent sound level history"><path d={path} fill="none" stroke="rgb(196 181 253)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" /></svg> : null}</div>
    <PermissionNotice state={permission} />
    <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4"><Metric label="Current" value={`${level} dB`} /><Metric label="Average" value={`${average} dB`} /><Metric label="Minimum" value={`${minimum} dB`} /><Metric label="Peak" value={`${peak} dB`} /></div>
    <ActionButton className="mt-4 w-full" onClick={() => active ? stop() : void start()}>{active ? 'Stop listening' : 'Start sound meter'}</ActionButton>
    <p className="mt-3 text-xs text-slate-500">Microphone samples stay in this browser and are discarded when you stop. Values are estimates, not certified safety measurements.</p>
  </div>
}

function PermissionNotice({ state }: { state: PermissionState }) {
  if (state === 'idle' || state === 'active') return null
  return <div role="status" className="mt-4 flex items-start gap-2 rounded-2xl border border-amber-200 bg-amber-50 p-3 text-left text-xs leading-5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/35 dark:text-amber-100"><Lock className="mt-0.5 size-4 shrink-0" />{state === 'unsupported' ? 'This browser does not expose the required sensor. Try PureHub on a supported phone.' : 'Permission was not granted. Enable sensor or microphone access in browser settings, then try again.'}</div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl border border-slate-200 p-3 text-center dark:border-slate-700"><span className="block text-xs text-slate-500">{label}</span><strong className="text-xl tabular-nums">{value}</strong></div> }

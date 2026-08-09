import { useEffect, useRef, useState } from 'react'
import { Activity, Compass, Gauge, Mic, ShieldCheck } from 'lucide-react'
import { ActionButton } from '../MiniAppPrimitives'

type SensorMode = 'compass' | 'level' | 'sound'

export default function SensorSuiteSurface({ mode }: { mode: SensorMode }) {
  return <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-sky-50 via-white to-violet-50 p-5 dark:from-sky-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-start gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-sky-600 text-white">{mode === 'compass' ? <Compass /> : mode === 'level' ? <Gauge /> : <Mic />}</span><div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-sky-700 dark:text-sky-300">SENSOR SUITE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">{mode === 'compass' ? 'Compass' : mode === 'level' ? 'Bubble Level' : 'Sound Meter'}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Live device readings with a focused, privacy-first interface.</p></div><ShieldCheck className="hidden size-5 text-emerald-600 sm:block" /></div>
      <nav className="mt-4 grid grid-cols-3 gap-2">{([['compass', 'Compass', Compass], ['level', 'Level', Gauge], ['sound', 'Sound', Mic]] as const).map(([id, label, Icon]) => <a key={id} href={`/en/${id === 'level' ? 'bubble-level' : id === 'sound' ? 'decibel-meter' : 'compass'}`} className={`flex min-h-11 items-center justify-center gap-1 rounded-xl border text-xs font-black ${mode === id ? 'border-sky-500 bg-sky-600 text-white' : 'border-slate-200 bg-white/70 dark:border-slate-700 dark:bg-slate-800'}`}><Icon className="size-4" />{label}</a>)}</nav>
    </header>
    <div className="p-4 sm:p-5">{mode === 'compass' ? <CompassPanel /> : mode === 'level' ? <LevelPanel /> : <SoundPanel />}</div>
  </section>
}

function CompassPanel() {
  const [heading, setHeading] = useState<number | null>(null)
  const start = async () => {
    const orientation = DeviceOrientationEvent as typeof DeviceOrientationEvent & { requestPermission?: () => Promise<string> }
    if (orientation.requestPermission && await orientation.requestPermission() !== 'granted') return
    window.addEventListener('deviceorientation', (event) => setHeading(event.alpha == null ? null : Math.round((360 - event.alpha) % 360)), { passive: true })
  }
  const label = heading == null ? '--' : ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round(heading / 45) % 8]
  return <div className="text-center"><div className="relative mx-auto grid size-64 place-items-center rounded-full border-[10px] border-slate-100 bg-slate-950 shadow-inner dark:border-slate-800"><span className="absolute top-3 font-black text-rose-400">N</span><div className="text-white transition-transform duration-300" style={{ transform: `rotate(${-(heading ?? 0)}deg)` }}><Compass className="size-32 stroke-[1] text-sky-300" /></div><div className="absolute"><strong className="block text-4xl text-white">{heading == null ? '—' : `${heading}°`}</strong><span className="text-sm font-black text-sky-300">{label}</span></div></div><ActionButton className="mt-5 w-full" onClick={() => void start()}>Enable compass</ActionButton><p className="mt-3 text-xs text-slate-500">Move the phone in a figure-eight to calibrate. Magnetic readings are estimates.</p></div>
}

function LevelPanel() {
  const [angles, setAngles] = useState({ x: 0, y: 0 }); const [enabled, setEnabled] = useState(false)
  useEffect(() => { if (!enabled) return; const listener = (event: DeviceOrientationEvent) => setAngles({ x: Math.round(event.gamma ?? 0), y: Math.round(event.beta ?? 0) }); window.addEventListener('deviceorientation', listener); return () => window.removeEventListener('deviceorientation', listener) }, [enabled])
  const flat = Math.abs(angles.x) <= 1 && Math.abs(angles.y) <= 1
  return <div><div className={`relative mx-auto aspect-square max-w-72 overflow-hidden rounded-[32px] border-8 ${flat ? 'border-emerald-400 bg-emerald-50' : 'border-slate-100 bg-sky-50'} dark:bg-slate-950`}><div className="absolute left-0 top-1/2 h-px w-full bg-slate-300" /><div className="absolute left-1/2 top-0 h-full w-px bg-slate-300" /><span className="absolute size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white bg-sky-500 shadow-xl transition-all duration-150" style={{ left: `${50 + Math.max(-35, Math.min(35, angles.x))}%`, top: `${50 + Math.max(-35, Math.min(35, angles.y))}%` }} /></div><div className="mt-4 grid grid-cols-2 gap-2 text-center"><Metric label="Left / right" value={`${angles.x}°`} /><Metric label="Front / back" value={`${angles.y}°`} /></div><ActionButton className="mt-4 w-full" onClick={() => setEnabled(true)}>Enable level</ActionButton></div>
}

function SoundPanel() {
  const [active, setActive] = useState(false); const [level, setLevel] = useState(0); const [peak, setPeak] = useState(0)
  const stream = useRef<MediaStream | null>(null); const frame = useRef(0); const context = useRef<AudioContext | null>(null)
  const stop = () => { cancelAnimationFrame(frame.current); stream.current?.getTracks().forEach((track) => track.stop()); void context.current?.close(); stream.current = null; context.current = null; setActive(false) }
  useEffect(() => stop, [])
  const start = async () => { const media = await navigator.mediaDevices.getUserMedia({ audio: true }); const audio = new AudioContext(); const analyser = audio.createAnalyser(); analyser.fftSize = 1024; audio.createMediaStreamSource(media).connect(analyser); stream.current = media; context.current = audio; setActive(true); const data = new Uint8Array(analyser.fftSize); const tick = () => { analyser.getByteTimeDomainData(data); const rms = Math.sqrt(data.reduce((sum, value) => sum + ((value - 128) / 128) ** 2, 0) / data.length); const estimate = Math.max(0, Math.min(100, Math.round(20 * Math.log10(Math.max(rms, .0001)) + 94))); setLevel(estimate); setPeak((value) => Math.max(value, estimate)); frame.current = requestAnimationFrame(tick) }; tick() }
  return <div><div className="rounded-[28px] bg-slate-950 p-6 text-center text-white"><Activity className="mx-auto size-7 text-violet-300" /><strong className="mt-3 block text-6xl tabular-nums">{level}</strong><span className="text-sm font-black text-slate-400">estimated dB</span><div className="mt-6 h-4 overflow-hidden rounded-full bg-white/10"><div className={`h-full rounded-full transition-all ${level > 85 ? 'bg-rose-500' : level > 65 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${level}%` }} /></div></div><div className="mt-3 grid grid-cols-2 gap-2"><Metric label="Current" value={`${level} dB`} /><Metric label="Peak" value={`${peak} dB`} /></div><ActionButton className="mt-4 w-full" onClick={() => active ? stop() : void start()}>{active ? 'Stop listening' : 'Start sound meter'}</ActionButton><p className="mt-3 text-xs text-slate-500">Microphone data stays in this browser. Values are estimates, not certified safety measurements.</p></div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><span className="block text-xs text-slate-500">{label}</span><strong className="text-xl">{value}</strong></div> }

import { useEffect, useRef, useState } from 'react'
import { AudioLines, CheckCircle2, CircleStop, Droplets, History, Play, RotateCcw, ShieldCheck, Sparkles, Volume2 } from 'lucide-react'

const PRESETS = [
  { label: 'Gentle', frequency: 150, pulse: 1.2 },
  { label: 'Balanced', frequency: 165, pulse: .8 },
  { label: 'Deep pulse', frequency: 185, pulse: .55 },
] as const

export default function SpeakerCleanerFlagship() {
  const [frequency, setFrequency] = useState(165)
  const [duration, setDuration] = useState(30)
  const [remaining, setRemaining] = useState(30)
  const [playing, setPlaying] = useState(false)
  const [pulse, setPulse] = useState(.8)
  const [completed, setCompleted] = useState(false)
  const [recentRuns, setRecentRuns] = useState<number[]>(() => { try { return JSON.parse(localStorage.getItem('purehub.speaker-cleaner.runs') ?? '[]') as number[] } catch { return [] } })
  const contextRef = useRef<AudioContext | null>(null)
  const oscillatorRef = useRef<OscillatorNode | null>(null)
  const gainRef = useRef<GainNode | null>(null)
  const pulseRef = useRef<number | null>(null)

  const stop = () => {
    if (pulseRef.current) window.clearInterval(pulseRef.current)
    pulseRef.current = null
    try { oscillatorRef.current?.stop() } catch { /* already stopped */ }
    oscillatorRef.current?.disconnect(); gainRef.current?.disconnect()
    oscillatorRef.current = null; gainRef.current = null
    setPlaying(false)
  }

  const start = async () => {
    stop()
    setCompleted(false)
    const context = contextRef.current ?? new AudioContext()
    contextRef.current = context
    await context.resume()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.type = 'sine'; oscillator.frequency.value = frequency
    gain.gain.value = .22
    oscillator.connect(gain).connect(context.destination)
    oscillator.start()
    oscillatorRef.current = oscillator; gainRef.current = gain
    let high = true
    pulseRef.current = window.setInterval(() => {
      high = !high
      gain.gain.setTargetAtTime(high ? .22 : .07, context.currentTime, .04)
    }, pulse * 1000)
    setRemaining(duration); setPlaying(true)
  }

  useEffect(() => {
    if (!playing) return
    const timer = window.setInterval(() => setRemaining((value) => {
      if (value <= 1) { setRecentRuns((current) => { const next = [duration, ...current].slice(0, 5); localStorage.setItem('purehub.speaker-cleaner.runs', JSON.stringify(next)); return next }); setCompleted(true); stop(); return 0 }
      return value - 1
    }), 1000)
    return () => window.clearInterval(timer)
  }, [duration, playing])
  useEffect(() => () => { stop(); void contextRef.current?.close() }, [])
  useEffect(() => { if (oscillatorRef.current) oscillatorRef.current.frequency.setTargetAtTime(frequency, contextRef.current?.currentTime ?? 0, .04) }, [frequency])

  const progress = playing ? ((duration - remaining) / duration) * 100 : 0
  return <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-cyan-50 via-white to-emerald-50 p-5 dark:from-cyan-950/40 dark:via-slate-900 dark:to-emerald-950/35">
      <div className="flex items-start gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-cyan-700 text-white dark:bg-cyan-300 dark:text-slate-950"><Droplets className="size-6" /></span><div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-cyan-700 dark:text-cyan-300">AUDIO CARE · ON DEVICE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">Speaker Cleaner</h2><p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">Controlled low-frequency pulses with a clear, timed routine.</p></div><ShieldCheck className="hidden size-5 text-emerald-600 sm:block" /></div>
      <div className="mt-5 grid grid-cols-3 gap-2">{PRESETS.map((item) => <button key={item.label} onClick={() => { setFrequency(item.frequency); setPulse(item.pulse) }} className={`rounded-xl border px-2 py-3 text-xs font-black transition ${frequency === item.frequency ? 'border-cyan-400 bg-cyan-700 text-white dark:bg-cyan-300 dark:text-slate-950' : 'border-slate-200 bg-white/75 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200'}`}>{item.label}<span className="mt-1 block text-[10px] opacity-75">{item.frequency} Hz</span></button>)}</div>
    </header>
    <div className="space-y-5 p-4 sm:p-5">
      <div className="relative overflow-hidden rounded-[22px] bg-slate-950 p-6 text-white">
        <div className={`absolute inset-0 bg-[radial-gradient(circle_at_center,_rgba(34,211,238,.25),_transparent_62%)] ${playing ? 'animate-pulse' : ''}`} />
        <div className="relative text-center"><div className="mx-auto grid size-28 place-items-center rounded-full border border-cyan-300/30 bg-cyan-400/10"><Volume2 className={`size-12 text-cyan-300 ${playing ? 'animate-pulse' : ''}`} /></div><p className="mt-4 text-4xl font-black tabular-nums">{playing ? `${remaining}s` : `${frequency} Hz`}</p><p className="mt-1 text-sm text-slate-300">{playing ? 'Pulse cycle running locally' : 'Ready for a controlled cycle'}</p><div className="mt-4 h-1.5 overflow-hidden rounded-full bg-white/15"><div className="h-full rounded-full bg-cyan-300 transition-all" style={{ width: `${progress}%` }} /></div></div>
      </div>
      <label className="block text-sm font-black text-slate-800 dark:text-slate-100">Frequency <span className="float-right text-cyan-700 dark:text-cyan-300">{frequency} Hz</span><input className="mt-3 w-full accent-cyan-600" type="range" min="120" max="220" value={frequency} onChange={(event) => setFrequency(Number(event.target.value))} /></label>
      <div><p className="mb-2 text-sm font-black">Cycle length</p><div className="grid grid-cols-3 gap-2">{[15, 30, 60].map((value) => <button key={value} disabled={playing} onClick={() => { setDuration(value); setRemaining(value) }} className={`min-h-11 rounded-xl border text-sm font-bold ${duration === value ? 'border-cyan-400 bg-cyan-50 text-cyan-800 dark:bg-cyan-950 dark:text-cyan-200' : 'border-slate-200 dark:border-slate-700'}`}>{value}s</button>)}</div></div>
      <button onClick={() => playing ? stop() : void start()} className={`flex min-h-14 w-full items-center justify-center gap-2 rounded-2xl text-base font-black shadow-sm ${playing ? 'bg-rose-600 text-white' : 'bg-cyan-700 text-white dark:bg-cyan-300 dark:text-slate-950'}`}>{playing ? <CircleStop className="size-5" /> : <Play className="size-5" />}{playing ? 'Stop safely' : 'Start cleaning cycle'}</button>
      {completed ? <div role="status" className="flex items-center gap-3 rounded-2xl bg-emerald-50 p-4 text-emerald-900 dark:bg-emerald-950/45 dark:text-emerald-100"><CheckCircle2 className="size-6 shrink-0" /><div><strong className="block">Cleaning cycle complete</strong><span className="text-xs opacity-80">Test normal audio before running another cycle.</span></div></div> : null}
      <div className="grid grid-cols-2 gap-2"><button onClick={() => { stop(); setRemaining(duration) }} className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-200 text-sm font-bold dark:border-slate-700"><RotateCcw className="size-4" />Reset</button><div className="flex items-center justify-center gap-2 rounded-xl bg-emerald-50 text-xs font-bold text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200"><AudioLines className="size-4" />No recording</div></div>
      <aside className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950 dark:border-amber-900 dark:bg-amber-950/35 dark:text-amber-100"><strong className="flex items-center gap-2"><Sparkles className="size-4" />Safe routine</strong><ol className="mt-2 list-inside list-decimal space-y-1"><li>Remove the case and face the speaker downward.</li><li>Start at a comfortable volume.</li><li>Stop immediately if sound distorts or the phone heats.</li></ol><p className="mt-2 text-xs opacity-80">This utility may move light moisture. It cannot repair damaged hardware or replace professional service.</p></aside>
      {recentRuns.length ? <details className="rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 py-3 text-sm font-black"><History className="size-4" />Recent completed runs ({recentRuns.length})</summary><div className="mt-1 flex flex-wrap gap-2">{recentRuns.map((seconds, index) => <span key={`${seconds}-${index}`} className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-bold dark:bg-slate-800">{seconds}s cycle</span>)}</div></details> : null}
    </div>
  </section>
}

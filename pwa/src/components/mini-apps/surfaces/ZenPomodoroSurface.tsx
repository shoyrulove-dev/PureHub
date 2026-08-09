import { useEffect, useMemo, useRef, useState } from 'react'
import { BarChart3, CalendarDays, CheckCircle2, Coffee, RotateCcw, Settings2, Target, Timer, Zap } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, Panel } from '../MiniAppPrimitives'

const STORAGE_KEY = 'purehub.zen-pomodoro.stats.v1'
type DayStats = Record<string, { sessions: number; minutes: number }>
type Soundscape = 'white' | 'brown' | 'rain'
type ActiveSound = { context: AudioContext; source: AudioBufferSourceNode; gain: GainNode }

const soundscapes: Array<{ id: Soundscape; label: string }> = [
  { id: 'white', label: 'White noise' },
  { id: 'brown', label: 'Brown noise' },
  { id: 'rain', label: 'Soft rain' },
]

function readStats(): DayStats {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as DayStats } catch { return {} }
}

function format(seconds: number) {
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

export default function ZenPomodoroSurface() {
  const [mode, setMode] = useState<'focus' | 'break'>('focus')
  const [minutes, setMinutes] = useState(25)
  const [remaining, setRemaining] = useState(25 * 60)
  const [running, setRunning] = useState(false)
  const [targetAt, setTargetAt] = useState<number | null>(null)
  const [stats, setStats] = useState<DayStats>(readStats)
  const [soundscape, setSoundscape] = useState<Soundscape>('white')
  const [volume, setVolume] = useState(0.3)
  const [customMinutes, setCustomMinutes] = useState(35)
  const completedRef = useRef(false)
  const soundRef = useRef<ActiveSound | null>(null)
  const totalSeconds = minutes * 60
  const elapsed = Math.max(0, totalSeconds - remaining)
  const progress = totalSeconds ? elapsed / totalSeconds : 0
  const days = useMemo(() => Array.from({ length: 7 }, (_, offset) => {
    const date = new Date(); date.setDate(date.getDate() - (6 - offset))
    const key = date.toISOString().slice(0, 10)
    return { key, label: date.toLocaleDateString(undefined, { weekday: 'short' }), ...(stats[key] ?? { sessions: 0, minutes: 0 }) }
  }), [stats])
  const week = useMemo(() => days.reduce((sum, day) => ({ sessions: sum.sessions + day.sessions, minutes: sum.minutes + day.minutes }), { sessions: 0, minutes: 0 }), [days])

  const stopSound = () => {
    const active = soundRef.current
    soundRef.current = null
    if (!active) return
    try { active.source.stop() } catch { /* source may already be stopped */ }
    active.source.disconnect()
    active.gain.disconnect()
    void active.context.close()
  }

  const startSound = async (kind: Soundscape = soundscape) => {
    stopSound()
    const AudioContextClass = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AudioContextClass) return
    const context = new AudioContextClass()
    await context.resume()
    const length = context.sampleRate * 2
    const buffer = context.createBuffer(1, length, context.sampleRate)
    const data = buffer.getChannelData(0)
    let smoothed = 0
    for (let index = 0; index < length; index += 1) {
      const white = Math.random() * 2 - 1
      if (kind === 'brown') {
        smoothed = (smoothed + 0.02 * white) / 1.02
        data[index] = smoothed * 3.2
      } else if (kind === 'rain') {
        smoothed = smoothed * 0.985 + white * 0.015
        data[index] = white * 0.18 + smoothed * 1.6
      } else {
        data[index] = white * 0.72
      }
    }
    const source = context.createBufferSource()
    const gain = context.createGain()
    source.buffer = buffer
    source.loop = true
    gain.gain.value = volume
    source.connect(gain).connect(context.destination)
    source.start()
    soundRef.current = { context, source, gain }
  }

  useEffect(() => () => stopSound(), [])

  useEffect(() => {
    const active = soundRef.current
    if (active) active.gain.gain.setTargetAtTime(volume, active.context.currentTime, 0.04)
  }, [volume])

  const selectSession = (nextMode: 'focus' | 'break', nextMinutes: number) => {
    stopSound()
    setMode(nextMode); setMinutes(nextMinutes); setRemaining(nextMinutes * 60); setRunning(false); setTargetAt(null); completedRef.current = false
  }

  useEffect(() => {
    if (!running || !targetAt) return
    const update = () => {
      const next = Math.max(0, Math.ceil((targetAt - Date.now()) / 1000))
      setRemaining(next)
      if (next === 0) {
        stopSound()
        setRunning(false); setTargetAt(null)
        if (mode === 'focus' && !completedRef.current) {
          completedRef.current = true
          const key = new Date().toISOString().slice(0, 10)
          setStats((current) => {
            const previous = current[key] ?? { sessions: 0, minutes: 0 }
            const nextStats = { ...current, [key]: { sessions: previous.sessions + 1, minutes: previous.minutes + minutes } }
            localStorage.setItem(STORAGE_KEY, JSON.stringify(nextStats)); return nextStats
          })
          if ('vibrate' in navigator) navigator.vibrate([160, 80, 160])
        }
      }
    }
    update()
    const id = window.setInterval(update, 250)
    document.addEventListener('visibilitychange', update)
    return () => { window.clearInterval(id); document.removeEventListener('visibilitychange', update) }
  }, [mode, minutes, running, targetAt])

  const toggle = async () => {
    if (remaining <= 0) { setRemaining(totalSeconds); completedRef.current = false }
    if (running) {
      stopSound()
      setRunning(false); setTargetAt(null)
    } else {
      await startSound()
      setTargetAt(Date.now() + (remaining <= 0 ? totalSeconds : remaining) * 1000); setRunning(true)
    }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Zen Suite flagship" title="Zen Pomodoro" description="A calm focus workspace with accurate timing, private progress, quick presets, and locally generated soundscapes." accent="emerald">
      <div className="grid grid-cols-3 gap-2 sm:max-w-lg"><div className="rounded-2xl bg-white/75 p-3 dark:bg-slate-950/45"><Target className="size-4" /><strong className="mt-2 block text-xl text-slate-950 dark:text-white">{week.sessions}</strong><span className="text-xs text-slate-500">sessions</span></div><div className="rounded-2xl bg-white/75 p-3 dark:bg-slate-950/45"><Timer className="size-4" /><strong className="mt-2 block text-xl text-slate-950 dark:text-white">{week.minutes}</strong><span className="text-xs text-slate-500">minutes</span></div><div className="rounded-2xl bg-white/75 p-3 dark:bg-slate-950/45"><CheckCircle2 className="size-4" /><strong className="mt-2 block text-xl text-slate-950 dark:text-white">{running ? 'Live' : 'Ready'}</strong><span className="text-xs text-slate-500">on device</span></div></div>
    </FlagshipHero>
    <div className="grid gap-4 xl:grid-cols-[1fr_0.52fr]">
    <Panel title="Focus timer" subtitle="Stays accurate after tab switches and device sleep.">
      <div className="flex flex-wrap gap-2">
        {[[15, 'Quick 15'], [25, 'Focus 25'], [50, 'Deep 50']] .map(([value, label]) => <ActionButton key={value} tone={mode === 'focus' && minutes === value ? 'primary' : 'muted'} onClick={() => selectSession('focus', Number(value))}><Zap className="size-4" />{label}</ActionButton>)}
        <ActionButton tone={mode === 'break' ? 'primary' : 'muted'} onClick={() => selectSession('break', 5)}><Coffee className="size-4" />Break 5</ActionButton>
      </div>
      <details className="mt-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 text-sm font-bold"><Settings2 className="size-4" />Custom focus length</summary><div className="mt-2 flex gap-2"><FormInput aria-label="Custom focus minutes" type="number" min="1" max="180" value={customMinutes} onChange={(event) => setCustomMinutes(Math.max(1, Math.min(180, Number(event.target.value) || 1)))} /><ActionButton onClick={() => selectSession('focus', customMinutes)}>Use {customMinutes} min</ActionButton></div></details>
      <div className="mt-6 flex flex-col items-center text-center">
        <div className="grid size-64 place-items-center rounded-full p-3 shadow-[0_24px_70px_rgba(16,185,129,.16)]" style={{ background: `conic-gradient(${mode === 'focus' ? '#10b981' : '#38bdf8'} ${progress * 360}deg, rgba(148,163,184,.16) 0deg)` }}>
          <div className="grid size-full place-items-center rounded-full bg-white dark:bg-[#111827]">
            <div><p className="text-xs font-bold uppercase tracking-[.2em] text-emerald-700 dark:text-emerald-300">{running ? mode : remaining === 0 ? 'complete' : 'ready'}</p><p className="mt-2 text-5xl font-black tabular-nums tracking-tight text-slate-950 dark:text-white">{format(remaining)}</p></div>
          </div>
        </div>
        <div className="mt-5 flex gap-2"><ActionButton onClick={toggle}><Timer className="size-4" />{running ? 'Pause' : 'Start'}</ActionButton><ActionButton tone="muted" onClick={() => selectSession(mode, minutes)}><RotateCcw className="size-4" />Reset</ActionButton></div>
        <details className="mt-5 w-full max-w-md rounded-[18px] border border-slate-500/15 bg-slate-500/5 p-4 text-left"><summary className="cursor-pointer list-none text-sm font-bold text-slate-950 dark:text-white">Focus sound <span className="ml-2 text-xs font-normal text-slate-500">{soundscapes.find((item) => item.id === soundscape)?.label}</span></summary>
          <div className="flex items-center justify-between gap-3">
            <div><p className="text-sm font-bold text-slate-950 dark:text-white">Focus sound</p><p className="text-xs text-slate-500">Plays locally while the timer is running.</p></div>
            <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${running ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300' : 'bg-slate-500/10 text-slate-500'}`}>{running ? 'Playing' : 'Ready'}</span>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {soundscapes.map((option) => <ActionButton key={option.id} tone={soundscape === option.id ? 'primary' : 'muted'} className="min-h-9 px-3 py-1.5 text-xs" onClick={() => { setSoundscape(option.id); if (running) void startSound(option.id) }}>{option.label}</ActionButton>)}
          </div>
          <label className="mt-3 block text-xs font-semibold text-slate-600 dark:text-slate-300">Volume {Math.round(volume * 100)}%
            <input className="mt-2 w-full accent-emerald-600" type="range" min="0" max="0.7" step="0.05" value={volume} onChange={(event) => setVolume(Number(event.target.value))} />
          </label>
        </details>
        <p className="mt-4 max-w-md text-xs leading-5 text-slate-500">Your timer and weekly totals remain on this device. No account, tracking, or ads.</p>
      </div>
    </Panel>
    <Panel title="This week" subtitle="Private progress stored locally.">
      <div className="grid grid-cols-2 gap-3"><div className="rounded-[18px] bg-emerald-500/10 p-4"><BarChart3 className="size-5 text-emerald-600" /><p className="mt-5 text-3xl font-black text-slate-950 dark:text-white">{week.sessions}</p><p className="text-xs text-slate-500">focus sessions</p></div><div className="rounded-[18px] bg-sky-500/10 p-4"><Timer className="size-5 text-sky-600" /><p className="mt-5 text-3xl font-black text-slate-950 dark:text-white">{week.minutes}</p><p className="text-xs text-slate-500">focused minutes</p></div></div>
      <div className="mt-4 rounded-2xl border border-slate-200 p-4 dark:border-slate-700"><div className="mb-4 flex items-center gap-2 text-sm font-bold"><CalendarDays className="size-4" />Daily rhythm</div><div className="flex h-28 items-end gap-2">{days.map((day) => <div key={day.key} className="flex min-w-0 flex-1 flex-col items-center gap-1"><span className="text-[10px] font-bold text-slate-500">{day.minutes || ''}</span><div className="w-full rounded-t-lg bg-emerald-500 transition-[height] duration-200" style={{ height: `${Math.max(5, Math.min(88, day.minutes / Math.max(1, ...days.map((item) => item.minutes)) * 88))}px`, opacity: day.minutes ? 1 : .16 }} /><span className="text-[10px] text-slate-500">{day.label}</span></div>)}</div></div>
      <div className="mt-4 space-y-2">{days.slice().reverse().filter((day) => day.sessions).map((day) => <div key={day.key} className="flex items-center justify-between rounded-xl border border-slate-500/10 px-3 py-2 text-xs"><span>{day.key}</span><strong>{day.sessions} · {day.minutes} min</strong></div>)}</div>
    </Panel>
    </div>
  </div>
}

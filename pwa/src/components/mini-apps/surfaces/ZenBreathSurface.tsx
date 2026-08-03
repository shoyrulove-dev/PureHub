import { useEffect, useMemo, useState } from 'react'
import { Activity, Pause, Play, RotateCcw, Waves } from 'lucide-react'
import { ActionButton, Panel } from '../MiniAppPrimitives'

const STORAGE_KEY = 'purehub.zen-breath.sessions.v1'
const PATTERNS = {
  calm: { label: 'Calm 4–6', phases: [{ label: 'Inhale', seconds: 4, scale: 1.06 }, { label: 'Exhale', seconds: 6, scale: .7 }] },
  box: { label: 'Box 4–4–4–4', phases: [{ label: 'Inhale', seconds: 4, scale: 1.06 }, { label: 'Hold', seconds: 4, scale: 1.06 }, { label: 'Exhale', seconds: 4, scale: .7 }, { label: 'Hold', seconds: 4, scale: .7 }] },
  relax: { label: 'Relax 4–7–8', phases: [{ label: 'Inhale', seconds: 4, scale: 1.06 }, { label: 'Hold', seconds: 7, scale: 1.06 }, { label: 'Exhale', seconds: 8, scale: .7 }] },
} as const

export default function ZenBreathSurface() {
  const [pattern, setPattern] = useState<keyof typeof PATTERNS>('calm')
  const [phaseIndex, setPhaseIndex] = useState(0)
  const [remaining, setRemaining] = useState<number>(PATTERNS.calm.phases[0].seconds)
  const [running, setRunning] = useState(false)
  const [cycles, setCycles] = useState(0)
  const [sessionSeconds, setSessionSeconds] = useState(0)
  const [totalSessions, setTotalSessions] = useState(() => Number(localStorage.getItem(STORAGE_KEY) ?? 0))
  const phases = PATTERNS[pattern].phases
  const phase = phases[phaseIndex]
  const reducedMotion = useMemo(() => window.matchMedia('(prefers-reduced-motion: reduce)').matches, [])

  const reset = (nextPattern = pattern) => {
    setPattern(nextPattern); setPhaseIndex(0); setRemaining(PATTERNS[nextPattern].phases[0].seconds); setRunning(false); setCycles(0); setSessionSeconds(0)
  }

  useEffect(() => {
    if (!running) return
    const id = window.setInterval(() => {
      setSessionSeconds((value) => value + 1)
      setRemaining((value) => {
        if (value > 1) return value - 1
        const nextIndex = (phaseIndex + 1) % phases.length
        if (nextIndex === 0) setCycles((count) => count + 1)
        setPhaseIndex(nextIndex)
        if ('vibrate' in navigator) navigator.vibrate(35)
        return phases[nextIndex].seconds
      })
    }, 1000)
    return () => window.clearInterval(id)
  }, [phaseIndex, phases, running])

  const toggle = () => {
    if (running) {
      setRunning(false)
      if (sessionSeconds >= 30) {
        const next = totalSessions + 1; setTotalSessions(next); localStorage.setItem(STORAGE_KEY, String(next))
      }
    } else setRunning(true)
  }

  return <div className="grid gap-4 xl:grid-cols-[1fr_.5fr]">
    <Panel title="Zen Breath" subtitle="Clear pacing, accessible motion, and gentle on-device cues.">
      <div className="flex flex-wrap gap-2">{(Object.keys(PATTERNS) as Array<keyof typeof PATTERNS>).map((id) => <ActionButton key={id} tone={pattern === id ? 'primary' : 'muted'} onClick={() => reset(id)}>{PATTERNS[id].label}</ActionButton>)}</div>
      <div className="flex flex-col items-center py-6 text-center">
        <div className="relative grid size-64 place-items-center">
          <div className="absolute size-48 rounded-full bg-gradient-to-br from-emerald-300/35 to-sky-300/20 shadow-[0_0_80px_rgba(52,211,153,.3)]" style={{ transform: `scale(${phase.scale})`, transition: reducedMotion ? 'none' : `transform ${phase.seconds}s ease-in-out` }} />
          <div className="relative grid size-36 place-items-center rounded-full border border-white/60 bg-white/80 shadow-xl backdrop-blur dark:border-slate-700 dark:bg-slate-950/80"><div><p className="text-sm font-black uppercase tracking-[.25em] text-emerald-700 dark:text-emerald-300">{running ? phase.label : 'Ready'}</p><p className="mt-2 text-4xl font-black tabular-nums text-slate-950 dark:text-white">{remaining}</p></div></div>
        </div>
        <div className="flex gap-2"><ActionButton onClick={toggle}>{running ? <Pause className="size-4" /> : <Play className="size-4" />}{running ? 'Pause' : 'Start session'}</ActionButton><ActionButton tone="muted" onClick={() => reset()}><RotateCcw className="size-4" />Reset</ActionButton></div>
        <p className="mt-4 text-xs leading-5 text-slate-500">Breathe comfortably. Stop if you feel dizzy or unwell. Reduced-motion settings are respected automatically.</p>
      </div>
    </Panel>
    <Panel title="Session" subtitle="A simple private rhythm summary.">
      <div className="space-y-3"><div className="rounded-[18px] bg-emerald-500/10 p-4"><Waves className="size-5 text-emerald-600" /><p className="mt-4 text-3xl font-black text-slate-950 dark:text-white">{cycles}</p><p className="text-xs text-slate-500">complete cycles</p></div><div className="rounded-[18px] bg-violet-500/10 p-4"><Activity className="size-5 text-violet-600" /><p className="mt-4 text-3xl font-black text-slate-950 dark:text-white">{Math.floor(sessionSeconds / 60)}:{String(sessionSeconds % 60).padStart(2, '0')}</p><p className="text-xs text-slate-500">current session</p></div><p className="rounded-xl border border-slate-500/10 px-3 py-2 text-xs text-slate-500">Completed sessions on this device: <strong className="text-slate-800 dark:text-white">{totalSessions}</strong></p></div>
    </Panel>
  </div>
}

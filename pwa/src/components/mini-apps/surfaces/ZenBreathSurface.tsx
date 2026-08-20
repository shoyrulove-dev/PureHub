import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Activity, CheckCircle2, Pause, Play, RotateCcw, Sparkles, Vibrate, Volume2, Waves } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

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
  const [targetMinutes, setTargetMinutes] = useState(3)
  const [haptics, setHaptics] = useState(true)
  const [sound, setSound] = useState(false)
  const [lessMotion, setLessMotion] = useState(false)
  const completedRef = useRef(false)
  const phases = PATTERNS[pattern].phases
  const phase = phases[phaseIndex]
  const reducedMotion = useMemo(() => lessMotion || window.matchMedia('(prefers-reduced-motion: reduce)').matches, [lessMotion])
  const targetSeconds = targetMinutes * 60
  const progress = Math.min(1, sessionSeconds / targetSeconds)

  const cue = useCallback(() => {
    if (haptics && 'vibrate' in navigator) navigator.vibrate(35)
    if (!sound) return
    const AudioContextClass = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AudioContextClass) return
    const context = new AudioContextClass()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.frequency.value = 440
    gain.gain.setValueAtTime(.035, context.currentTime)
    gain.gain.exponentialRampToValueAtTime(.001, context.currentTime + .18)
    oscillator.connect(gain).connect(context.destination)
    oscillator.start(); oscillator.stop(context.currentTime + .2)
    oscillator.onended = () => void context.close()
  }, [haptics, sound])

  const saveSession = useCallback(() => {
    if (completedRef.current) return
    completedRef.current = true
    setTotalSessions((current) => {
      const next = current + 1
      localStorage.setItem(STORAGE_KEY, String(next))
      return next
    })
    markToolSuccess('zen-breath', { headline: 'Breathing session complete', detail: `${targetMinutes}-minute ${PATTERNS[pattern].label.toLowerCase()} session saved only on this device.`, shareText: `I completed a private ${targetMinutes}-minute breathing session with PureHub.` })
  }, [pattern, targetMinutes])

  const reset = (nextPattern = pattern) => {
    setPattern(nextPattern); setPhaseIndex(0); setRemaining(PATTERNS[nextPattern].phases[0].seconds)
    setRunning(false); setCycles(0); setSessionSeconds(0); completedRef.current = false
  }

  useEffect(() => {
    if (!running) return
    const id = window.setInterval(() => {
      setSessionSeconds((value) => value + 1)
      setRemaining((value) => {
        if (value > 1) return value - 1
        const nextIndex = (phaseIndex + 1) % phases.length
        if (nextIndex === 0) setCycles((count) => count + 1)
        setPhaseIndex(nextIndex); cue()
        return phases[nextIndex].seconds
      })
    }, 1000)
    return () => window.clearInterval(id)
  }, [cue, phaseIndex, phases, running])

  useEffect(() => {
    if (sessionSeconds < targetSeconds || completedRef.current) return
    saveSession(); setRunning(false)
    if (haptics && 'vibrate' in navigator) navigator.vibrate([100, 70, 160])
  }, [haptics, saveSession, sessionSeconds, targetSeconds])

  const toggle = () => {
    if (running) { if (sessionSeconds >= 30) saveSession(); setRunning(false) }
    else { if (progress >= 1) reset(); else setRunning(true) }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Zen Suite flagship" title="Zen Breath" description="A gentle on-device breathing coach with clear pacing, accessible motion, optional cues, and private session goals." accent="violet">
      <div className="flex flex-wrap gap-2">{[1, 3, 5].map((value) => <ActionButton key={value} tone={targetMinutes === value ? 'primary' : 'muted'} onClick={() => { setTargetMinutes(value); reset() }}>{value} min</ActionButton>)}</div>
    </FlagshipHero>
    <div className="grid gap-4 xl:grid-cols-[1fr_.5fr]">
      <Panel title="Breathing guide" subtitle="Choose a rhythm, settle comfortably, and follow the gentle visual cue.">
        <div className="flex flex-wrap gap-2">{(Object.keys(PATTERNS) as Array<keyof typeof PATTERNS>).map((id) => <ActionButton key={id} tone={pattern === id ? 'primary' : 'muted'} onClick={() => reset(id)}>{PATTERNS[id].label}</ActionButton>)}</div>
        <div className="flex flex-col items-center py-6 text-center">
          <div className="relative grid size-64 place-items-center">
            <div className="absolute size-48 rounded-full bg-gradient-to-br from-violet-300/40 to-sky-300/20 shadow-[0_0_80px_rgba(139,92,246,.25)]" style={{ transform: `scale(${phase.scale})`, transition: reducedMotion ? 'none' : `transform ${phase.seconds}s ease-in-out` }} />
            <div className="relative grid size-36 place-items-center rounded-full border border-white/60 bg-white/85 shadow-xl backdrop-blur dark:border-slate-700 dark:bg-slate-950/80"><div><p className="text-sm font-black uppercase tracking-[.25em] text-violet-700 dark:text-violet-300">{running ? phase.label : progress >= 1 ? 'Complete' : 'Ready'}</p><p className="mt-2 text-4xl font-black tabular-nums text-slate-950 dark:text-white">{remaining}</p></div></div>
          </div>
          <div className="flex gap-2"><ActionButton onClick={toggle}>{running ? <Pause className="size-4" /> : <Play className="size-4" />}{running ? 'Pause' : progress >= 1 ? 'Start again' : 'Start session'}</ActionButton><ActionButton tone="muted" onClick={() => reset()}><RotateCcw className="size-4" />Reset</ActionButton></div>
          <div className="mt-5 h-2 w-full max-w-md overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700"><div className="h-full rounded-full bg-violet-600 transition-[width] duration-200" style={{ width: `${progress * 100}%` }} /></div>
          <p className="mt-2 text-xs font-bold text-violet-700 dark:text-violet-300">{Math.round(progress * 100)}% of {targetMinutes}-minute goal</p>
          <p className="mt-4 text-xs leading-5 text-slate-500">Breathe comfortably. Stop if you feel dizzy or unwell.</p>
        </div>
      </Panel>
      <Panel title="Session" subtitle="Private progress and comfort controls.">
        <div className="grid grid-cols-2 gap-3"><div className="rounded-[18px] bg-emerald-500/10 p-4"><Waves className="size-5 text-emerald-600" /><p className="mt-4 text-3xl font-black text-slate-950 dark:text-white">{cycles}</p><p className="text-xs text-slate-500">cycles</p></div><div className="rounded-[18px] bg-violet-500/10 p-4"><Activity className="size-5 text-violet-600" /><p className="mt-4 text-3xl font-black text-slate-950 dark:text-white">{Math.floor(sessionSeconds / 60)}:{String(sessionSeconds % 60).padStart(2, '0')}</p><p className="text-xs text-slate-500">elapsed</p></div></div>
        <p className="mt-3 rounded-xl border border-slate-500/10 px-3 py-2 text-xs text-slate-500">Completed on this device: <strong className="text-slate-800 dark:text-white">{totalSessions}</strong></p>
        <div className="mt-4 space-y-2"><ToggleRow icon={<Vibrate className="size-4" />} label="Gentle haptics" enabled={haptics} onClick={() => setHaptics((value) => !value)} /><ToggleRow icon={<Volume2 className="size-4" />} label="Phase chime" enabled={sound} onClick={() => setSound((value) => !value)} /><ToggleRow icon={<Sparkles className="size-4" />} label="Reduce motion" enabled={reducedMotion} onClick={() => setLessMotion((value) => !value)} />{progress >= 1 ? <div className="flex items-center gap-2 rounded-xl bg-emerald-500/10 p-3 text-sm font-bold text-emerald-700 dark:text-emerald-300"><CheckCircle2 className="size-5" />Session complete. Take this calm with you.</div> : null}</div>
      </Panel>
    </div>
  </div>
}

function ToggleRow({ icon, label, enabled, onClick }: { icon: ReactNode; label: string; enabled: boolean; onClick: () => void }) {
  return <button onClick={onClick} className="flex min-h-11 w-full items-center justify-between rounded-xl border border-slate-200 px-3 text-sm font-bold dark:border-slate-700"><span className="flex items-center gap-2">{icon}{label}</span><span>{enabled ? 'On' : 'Off'}</span></button>
}

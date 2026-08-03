import { useEffect, useMemo, useRef, useState } from 'react'
import { BarChart3, Coffee, RotateCcw, Timer, Zap } from 'lucide-react'
import { ActionButton, Panel } from '../MiniAppPrimitives'

const STORAGE_KEY = 'purehub.zen-pomodoro.stats.v1'
type DayStats = Record<string, { sessions: number; minutes: number }>

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
  const completedRef = useRef(false)
  const totalSeconds = minutes * 60
  const elapsed = Math.max(0, totalSeconds - remaining)
  const progress = totalSeconds ? elapsed / totalSeconds : 0
  const week = useMemo(() => Object.values(stats).slice(-7).reduce((sum, day) => ({ sessions: sum.sessions + day.sessions, minutes: sum.minutes + day.minutes }), { sessions: 0, minutes: 0 }), [stats])

  const selectSession = (nextMode: 'focus' | 'break', nextMinutes: number) => {
    setMode(nextMode); setMinutes(nextMinutes); setRemaining(nextMinutes * 60); setRunning(false); setTargetAt(null); completedRef.current = false
  }

  useEffect(() => {
    if (!running || !targetAt) return
    const update = () => {
      const next = Math.max(0, Math.ceil((targetAt - Date.now()) / 1000))
      setRemaining(next)
      if (next === 0) {
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

  const toggle = () => {
    if (remaining <= 0) { setRemaining(totalSeconds); completedRef.current = false }
    if (running) {
      setRunning(false); setTargetAt(null)
    } else {
      setTargetAt(Date.now() + (remaining <= 0 ? totalSeconds : remaining) * 1000); setRunning(true)
    }
  }

  return <div className="grid gap-4 xl:grid-cols-[1fr_0.52fr]">
    <Panel title="Zen Pomodoro" subtitle="A resilient focus timer that stays accurate after tab switches and sleep.">
      <div className="flex flex-wrap gap-2">
        {[[15, 'Quick 15'], [25, 'Focus 25'], [50, 'Deep 50']] .map(([value, label]) => <ActionButton key={value} tone={mode === 'focus' && minutes === value ? 'primary' : 'muted'} onClick={() => selectSession('focus', Number(value))}><Zap className="size-4" />{label}</ActionButton>)}
        <ActionButton tone={mode === 'break' ? 'primary' : 'muted'} onClick={() => selectSession('break', 5)}><Coffee className="size-4" />Break 5</ActionButton>
      </div>
      <div className="mt-6 flex flex-col items-center text-center">
        <div className="grid size-64 place-items-center rounded-full p-3 shadow-[0_24px_70px_rgba(16,185,129,.16)]" style={{ background: `conic-gradient(${mode === 'focus' ? '#10b981' : '#38bdf8'} ${progress * 360}deg, rgba(148,163,184,.16) 0deg)` }}>
          <div className="grid size-full place-items-center rounded-full bg-white dark:bg-[#111827]">
            <div><p className="text-xs font-bold uppercase tracking-[.2em] text-emerald-700 dark:text-emerald-300">{running ? mode : remaining === 0 ? 'complete' : 'ready'}</p><p className="mt-2 text-5xl font-black tabular-nums tracking-tight text-slate-950 dark:text-white">{format(remaining)}</p></div>
          </div>
        </div>
        <div className="mt-5 flex gap-2"><ActionButton onClick={toggle}><Timer className="size-4" />{running ? 'Pause' : 'Start'}</ActionButton><ActionButton tone="muted" onClick={() => selectSession(mode, minutes)}><RotateCcw className="size-4" />Reset</ActionButton></div>
        <p className="mt-4 max-w-md text-xs leading-5 text-slate-500">Your timer and weekly totals remain on this device. No account, tracking, or ads.</p>
      </div>
    </Panel>
    <Panel title="This week" subtitle="Private progress stored locally.">
      <div className="grid grid-cols-2 gap-3"><div className="rounded-[18px] bg-emerald-500/10 p-4"><BarChart3 className="size-5 text-emerald-600" /><p className="mt-5 text-3xl font-black text-slate-950 dark:text-white">{week.sessions}</p><p className="text-xs text-slate-500">focus sessions</p></div><div className="rounded-[18px] bg-sky-500/10 p-4"><Timer className="size-5 text-sky-600" /><p className="mt-5 text-3xl font-black text-slate-950 dark:text-white">{week.minutes}</p><p className="text-xs text-slate-500">focused minutes</p></div></div>
      <div className="mt-4 space-y-2">{Object.entries(stats).slice(-7).reverse().map(([day, value]) => <div key={day} className="flex items-center justify-between rounded-xl border border-slate-500/10 px-3 py-2 text-xs"><span>{day}</span><strong>{value.sessions} · {value.minutes} min</strong></div>)}</div>
    </Panel>
  </div>
}

import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Archive, CalendarDays, Check, Download, Flame, Leaf, Plus, RotateCcw, Sparkles, Target, Trash2 } from 'lucide-react'
import {
  habitCheckInRepository,
  habitRepository,
  type HabitCheckInRecord,
  type HabitRecord,
} from '../../../lib/db/purehub-db'

type View = 'today' | 'insights' | 'manage'

const COLORS = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#f43f5e']
const CATEGORIES = ['Wellness', 'Focus', 'Movement', 'Learning', 'Personal']

function localDay(offset = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function createId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function currentStreak(days: string[]) {
  const completed = new Set(days)
  let cursor = completed.has(localDay()) ? 0 : -1
  let streak = 0
  while (completed.has(localDay(cursor))) {
    streak += 1
    cursor -= 1
  }
  return streak
}

function bestStreak(days: string[]) {
  const sorted = [...new Set(days)].sort()
  let best = 0
  let run = 0
  let previous = ''
  for (const day of sorted) {
    const expected = previous ? new Date(`${previous}T12:00:00`) : null
    expected?.setDate(expected.getDate() + 1)
    run = expected && localDateKey(expected) === day ? run + 1 : 1
    best = Math.max(best, run)
    previous = day
  }
  return best
}

function localDateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function weekDays() {
  return Array.from({ length: 7 }, (_, index) => {
    const offset = index - 6
    const date = new Date()
    date.setDate(date.getDate() + offset)
    return {
      key: localDateKey(date),
      label: new Intl.DateTimeFormat(undefined, { weekday: 'narrow' }).format(date),
      number: date.getDate(),
      today: offset === 0,
    }
  })
}

export default function ZenHabitSurface() {
  const [habits, setHabits] = useState<HabitRecord[]>([])
  const [checkIns, setCheckIns] = useState<HabitCheckInRecord[]>([])
  const [view, setView] = useState<View>('today')
  const [showComposer, setShowComposer] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState(CATEGORIES[0])
  const [colorHex, setColorHex] = useState(COLORS[0])
  const [targetDaysPerWeek, setTargetDaysPerWeek] = useState(7)

  const load = async () => {
    const nextHabits = await habitRepository.list()
    const grouped = await Promise.all(nextHabits.map((item) => habitCheckInRepository.listByHabit(item.id)))
    setHabits(nextHabits)
    setCheckIns(grouped.flat())
  }

  useEffect(() => { void load() }, [])

  const active = habits.filter((item) => !item.archivedAt)
  const archived = habits.filter((item) => item.archivedAt)
  const today = localDay()
  const days = useMemo(weekDays, [])
  const completedToday = active.filter((habit) => checkIns.some((item) => item.habitId === habit.id && item.completedOn === today)).length
  const weeklyDone = checkIns.filter((item) => active.some((habit) => habit.id === item.habitId) && days.some((day) => day.key === item.completedOn)).length
  const weeklyGoal = active.reduce((sum, habit) => sum + Math.max(1, habit.targetDaysPerWeek ?? 7), 0)
  const completionRate = weeklyGoal ? Math.min(100, Math.round((weeklyDone / weeklyGoal) * 100)) : 0
  const strongestStreak = Math.max(0, ...active.map((habit) => currentStreak(checkIns.filter((item) => item.habitId === habit.id).map((item) => item.completedOn))))

  const toggleDay = async (habitId: string, day: string) => {
    const existing = checkIns.find((item) => item.habitId === habitId && item.completedOn === day)
    if (existing) await habitCheckInRepository.remove(existing.id)
    else await habitCheckInRepository.upsert({ id: createId(), habitId, completedOn: day, createdAt: new Date().toISOString() })
    await load()
  }

  const saveHabit = async () => {
    if (!name.trim()) return
    await habitRepository.put({
      id: createId(),
      name: name.trim(),
      description: description.trim(),
      category,
      colorHex,
      targetDaysPerWeek,
      createdAt: new Date().toISOString(),
    })
    setName('')
    setDescription('')
    setShowComposer(false)
    await load()
  }

  const exportBackup = () => {
    const blob = new Blob([JSON.stringify({ exportedAt: new Date().toISOString(), habits, checkIns }, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `purehub-zen-habit-${today}.json`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return (
    <section className="overflow-hidden rounded-[24px] border border-emerald-200/80 bg-white shadow-[0_22px_70px_-40px_rgba(5,150,105,0.55)] dark:border-emerald-900/60 dark:bg-slate-950">
      <div className="relative overflow-hidden bg-gradient-to-br from-emerald-950 via-teal-900 to-sky-900 px-5 py-6 text-white sm:px-7">
        <div className="absolute -right-12 -top-20 size-52 rounded-full bg-emerald-300/15 blur-3xl" />
        <div className="relative flex items-start justify-between gap-4">
          <div>
            <p className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-emerald-200"><Sparkles className="size-4" /> Private daily rhythm</p>
            <h2 className="mt-2 text-3xl font-black tracking-tight">Zen Habit</h2>
            <p className="mt-2 max-w-xl text-sm leading-6 text-emerald-50/80">Build consistency without accounts, feeds, ads or guilt. Every check-in stays on this device.</p>
          </div>
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl border border-white/15 bg-white/10"><Leaf className="size-6 text-emerald-200" /></span>
        </div>
        <div className="relative mt-5 grid grid-cols-3 gap-2">
          <Stat label="Today" value={`${completedToday}/${active.length}`} icon={<Check className="size-4" />} />
          <Stat label="Best active" value={`${strongestStreak}d`} icon={<Flame className="size-4" />} />
          <Stat label="This week" value={`${completionRate}%`} icon={<Target className="size-4" />} />
        </div>
      </div>

      <nav className="grid grid-cols-3 border-b border-slate-200 bg-slate-50/80 p-2 dark:border-slate-800 dark:bg-slate-900/70" aria-label="Zen Habit sections">
        {(['today', 'insights', 'manage'] as View[]).map((item) => (
          <button key={item} type="button" onClick={() => setView(item)} className={`rounded-xl px-3 py-2.5 text-sm font-bold capitalize transition ${view === item ? 'bg-white text-emerald-800 shadow-sm ring-1 ring-slate-200 dark:bg-slate-800 dark:text-emerald-300 dark:ring-slate-700' : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'}`}>{item}</button>
        ))}
      </nav>

      <div className="p-4 sm:p-6">
        {view === 'today' ? (
          <div>
            <div className="flex items-center justify-between gap-3">
              <div><p className="text-xs font-black uppercase tracking-[0.16em] text-emerald-700 dark:text-emerald-300">Today</p><h3 className="mt-1 text-xl font-black text-slate-950 dark:text-white">Small actions count</h3></div>
              <button type="button" onClick={() => setShowComposer((value) => !value)} className="grid size-11 place-items-center rounded-2xl bg-emerald-700 text-white shadow-sm transition hover:bg-emerald-800" aria-label="Add habit"><Plus className="size-5" /></button>
            </div>

            {showComposer ? <HabitComposer name={name} setName={setName} description={description} setDescription={setDescription} category={category} setCategory={setCategory} colorHex={colorHex} setColorHex={setColorHex} target={targetDaysPerWeek} setTarget={setTargetDaysPerWeek} onSave={() => void saveHabit()} /> : null}

            <div className="mt-5 space-y-3">
              {active.map((habit) => {
                const habitDays = checkIns.filter((item) => item.habitId === habit.id).map((item) => item.completedOn)
                const done = habitDays.includes(today)
                const streak = currentStreak(habitDays)
                return (
                  <article key={habit.id} className="rounded-[20px] border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                    <div className="flex items-center gap-3">
                      <button type="button" onClick={() => void toggleDay(habit.id, today)} aria-label={done ? `Undo ${habit.name}` : `Complete ${habit.name}`} className="grid size-12 shrink-0 place-items-center rounded-2xl border-2 transition active:scale-95" style={{ borderColor: habit.colorHex, backgroundColor: done ? habit.colorHex : 'transparent', color: done ? '#fff' : habit.colorHex }}><Check className={`size-6 transition ${done ? 'opacity-100' : 'opacity-30'}`} /></button>
                      <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h4 className="truncate font-black text-slate-950 dark:text-white">{habit.name}</h4><span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-300">{habit.category ?? 'Personal'}</span></div><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{habit.description || `${habit.targetDaysPerWeek ?? 7} days each week`}</p></div>
                      <div className="text-right"><p className="flex items-center justify-end gap-1 font-black text-amber-600"><Flame className="size-4" /> {streak}</p><p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">day streak</p></div>
                    </div>
                    <div className="mt-4 grid grid-cols-7 gap-1.5">
                      {days.map((day) => { const checked = habitDays.includes(day.key); return <button key={day.key} type="button" onClick={() => void toggleDay(habit.id, day.key)} className={`rounded-xl py-2 text-center transition ${checked ? 'text-white shadow-sm' : day.today ? 'bg-emerald-50 ring-1 ring-emerald-300 dark:bg-emerald-950/30' : 'bg-slate-50 dark:bg-slate-800/70'}`} style={checked ? { backgroundColor: habit.colorHex } : undefined}><span className="block text-[10px] font-bold uppercase opacity-70">{day.label}</span><span className="mt-0.5 block text-xs font-black">{checked ? <Check className="mx-auto size-3.5" /> : day.number}</span></button> })}
                    </div>
                  </article>
                )
              })}
              {!active.length ? <EmptyState onAdd={() => setShowComposer(true)} /> : null}
            </div>
          </div>
        ) : null}

        {view === 'insights' ? (
          <div className="space-y-4">
            <div><p className="text-xs font-black uppercase tracking-[0.16em] text-violet-700 dark:text-violet-300">Weekly insights</p><h3 className="mt-1 text-xl font-black text-slate-950 dark:text-white">Progress, not perfection</h3></div>
            <div className="grid gap-3 sm:grid-cols-3"><Insight label="Check-ins" value={weeklyDone} detail="last 7 days" /><Insight label="Weekly goal" value={weeklyGoal} detail="planned actions" /><Insight label="Completion" value={`${completionRate}%`} detail="across active habits" /></div>
            <div className="space-y-3">
              {active.map((habit) => {
                const entries = checkIns.filter((item) => item.habitId === habit.id)
                const week = entries.filter((item) => days.some((day) => day.key === item.completedOn)).length
                const target = Math.max(1, habit.targetDaysPerWeek ?? 7)
                const percent = Math.min(100, Math.round((week / target) * 100))
                const best = bestStreak(entries.map((item) => item.completedOn))
                return <div key={habit.id} className="rounded-2xl border border-slate-200 p-4 dark:border-slate-800"><div className="flex items-center justify-between gap-3"><div><p className="font-bold text-slate-950 dark:text-white">{habit.name}</p><p className="text-xs text-slate-500">Best streak {best} {best === 1 ? 'day' : 'days'}</p></div><strong className="text-sm" style={{ color: habit.colorHex }}>{week}/{target}</strong></div><div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800"><div className="h-full rounded-full transition-all" style={{ width: `${percent}%`, backgroundColor: habit.colorHex }} /></div></div>
              })}
            </div>
          </div>
        ) : null}

        {view === 'manage' ? (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[0.16em] text-sky-700 dark:text-sky-300">Private data</p><h3 className="mt-1 text-xl font-black text-slate-950 dark:text-white">Manage your habits</h3></div><button type="button" onClick={exportBackup} className="flex min-h-10 items-center gap-2 rounded-xl border border-slate-300 px-3 text-sm font-bold text-slate-700 dark:border-slate-700 dark:text-slate-200"><Download className="size-4" /> Export</button></div>
            {[...active, ...archived].map((habit) => <div key={habit.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-800"><span className="size-3 rounded-full" style={{ backgroundColor: habit.colorHex }} /><div className="min-w-0 flex-1"><p className="truncate font-bold text-slate-950 dark:text-white">{habit.name}</p><p className="text-xs text-slate-500">{habit.archivedAt ? 'Archived' : `${habit.targetDaysPerWeek ?? 7} days per week`}</p></div><button type="button" onClick={async () => { if (habit.archivedAt) await habitRepository.restore(habit.id); else await habitRepository.archive(habit.id, new Date().toISOString()); await load() }} className="grid size-10 place-items-center rounded-xl border border-slate-200 text-slate-500 dark:border-slate-700" aria-label={habit.archivedAt ? 'Restore habit' : 'Archive habit'}>{habit.archivedAt ? <RotateCcw className="size-4" /> : <Archive className="size-4" />}</button><button type="button" onClick={async () => { if (!window.confirm(`Delete ${habit.name} and its check-ins?`)) return; await habitRepository.remove(habit.id); await load() }} className="grid size-10 place-items-center rounded-xl border border-rose-200 text-rose-600 dark:border-rose-900" aria-label="Delete habit"><Trash2 className="size-4" /></button></div>)}
          </div>
        ) : null}
      </div>
    </section>
  )
}

function Stat({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return <div className="rounded-2xl border border-white/10 bg-white/10 p-3 backdrop-blur"><p className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-emerald-100/70">{icon}{label}</p><p className="mt-1 text-xl font-black">{value}</p></div>
}

function Insight({ label, value, detail }: { label: string; value: string | number; detail: string }) {
  return <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900"><p className="text-xs font-bold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-black text-slate-950 dark:text-white">{value}</p><p className="text-xs text-slate-500">{detail}</p></div>
}

function EmptyState({ onAdd }: { onAdd: () => void }) {
  return <div className="rounded-[22px] border border-dashed border-emerald-300 bg-emerald-50/60 p-8 text-center dark:border-emerald-900 dark:bg-emerald-950/20"><CalendarDays className="mx-auto size-8 text-emerald-600" /><h4 className="mt-3 font-black text-slate-950 dark:text-white">Start with one gentle habit</h4><p className="mx-auto mt-1 max-w-sm text-sm text-slate-500">Pick something small enough to repeat. You can change direction without losing your history.</p><button type="button" onClick={onAdd} className="mt-4 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-bold text-white">Create a habit</button></div>
}

type ComposerProps = {
  name: string; setName: (value: string) => void
  description: string; setDescription: (value: string) => void
  category: string; setCategory: (value: string) => void
  colorHex: string; setColorHex: (value: string) => void
  target: number; setTarget: (value: number) => void
  onSave: () => void
}

function HabitComposer(props: ComposerProps) {
  return <div className="mt-5 rounded-[20px] border border-emerald-200 bg-emerald-50/60 p-4 dark:border-emerald-900/60 dark:bg-emerald-950/20"><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-bold text-slate-600 dark:text-slate-300">Habit name<input autoFocus value={props.name} onChange={(event) => props.setName(event.target.value)} maxLength={60} placeholder="Read for 10 minutes" className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-emerald-600 dark:border-slate-700 dark:bg-slate-900 dark:text-white" /></label><label className="text-xs font-bold text-slate-600 dark:text-slate-300">Category<select value={props.category} onChange={(event) => props.setCategory(event.target.value)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-slate-700 dark:bg-slate-900 dark:text-white">{CATEGORIES.map((item) => <option key={item}>{item}</option>)}</select></label></div><label className="mt-3 block text-xs font-bold text-slate-600 dark:text-slate-300">Why it matters<input value={props.description} onChange={(event) => props.setDescription(event.target.value)} maxLength={120} placeholder="Optional gentle reminder" className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-emerald-600 dark:border-slate-700 dark:bg-slate-900 dark:text-white" /></label><div className="mt-3 flex flex-wrap items-end justify-between gap-4"><div><p className="text-xs font-bold text-slate-600 dark:text-slate-300">Color</p><div className="mt-2 flex gap-2">{COLORS.map((color) => <button key={color} type="button" onClick={() => props.setColorHex(color)} className={`size-8 rounded-xl transition ${props.colorHex === color ? 'ring-2 ring-offset-2 ring-slate-700 dark:ring-slate-200 dark:ring-offset-slate-950' : ''}`} style={{ backgroundColor: color }} aria-label={`Use ${color}`} />)}</div></div><label className="text-xs font-bold text-slate-600 dark:text-slate-300">Weekly target<select value={props.target} onChange={(event) => props.setTarget(Number(event.target.value))} className="mt-1.5 min-h-10 rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-slate-700 dark:bg-slate-900 dark:text-white">{[1,2,3,4,5,6,7].map((item) => <option key={item} value={item}>{item} days</option>)}</select></label><button type="button" onClick={props.onSave} disabled={!props.name.trim()} className="flex min-h-11 items-center gap-2 rounded-xl bg-emerald-700 px-4 text-sm font-bold text-white disabled:opacity-40"><Plus className="size-4" /> Add habit</button></div></div>
}

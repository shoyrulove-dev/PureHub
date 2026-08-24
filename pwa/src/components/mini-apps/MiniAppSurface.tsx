import { lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent, ReactNode } from 'react'
import type { MiniAppId } from '../../features/catalog/tabs'
import { ActionButton, FlagshipHero, FormInput, FormTextArea, Panel } from './MiniAppPrimitives'
import {
  expenseRepository,
  type ExpenseRecord,
} from '../../lib/db/purehub-db'
import { markToolSuccess } from '../../lib/tool-success'

type MiniAppSurfaceProps = {
  miniAppId: MiniAppId
}

const OcrTextSurface = lazy(() => import('./surfaces/OcrTextSurface'))
const PasswordVaultSurface = lazy(() => import('./surfaces/PasswordVaultSurface'))
const QrStudioSurface = lazy(() => import('./surfaces/QrStudioSurface'))
const ZenPomodoroSurface = lazy(() => import('./surfaces/ZenPomodoroSurface'))
const ZenBreathSurface = lazy(() => import('./surfaces/ZenBreathSurface'))
const ZenHabitSurface = lazy(() => import('./surfaces/ZenHabitSurface'))
const SpeakerCleanerFlagship = lazy(() => import('./surfaces/SpeakerCleanerFlagship'))
const DocumentSuiteSurface = lazy(() => import('./surfaces/DocumentSuiteSurface'))
const FinanceSuiteSurface = lazy(() => import('./surfaces/FinanceSuiteSurface'))
const SensorSuiteSurface = lazy(() => import('./surfaces/SensorSuiteSurface'))
const EverydayFlagshipSurface = lazy(() => import('./surfaces/EverydayFlagshipSurface'))
const AuthenticatorVaultSurface = lazy(() => import('./surfaces/AuthenticatorVaultSurface'))
const FileStudioSurface = lazy(() => import('./surfaces/FileStudioSurface'))
const ScreenRecorderSurface = lazy(() => import('./surfaces/ScreenRecorderSurface'))
const DeepCleanerFlagship = lazy(() => import('./surfaces/DeepCleanerFlagship'))
const PhotoPrivacySurface = lazy(() => import('./surfaces/PhotoPrivacySurface'))

export function MiniAppSurface({ miniAppId }: MiniAppSurfaceProps) {
  switch (miniAppId) {
    case 'lunar-calendar':
      return <LunarCalendarSurface />
    case 'zen-habit':
      return <LazyTool><ZenHabitSurface /></LazyTool>
    case 'zen-pomodoro':
      return <LazyTool><ZenPomodoroSurface /></LazyTool>
    case 'zen-breath':
      return <LazyTool><ZenBreathSurface /></LazyTool>
    case 'compass':
      return <LazyTool><SensorSuiteSurface mode="compass" /></LazyTool>
    case 'bubble-level':
      return <LazyTool><SensorSuiteSurface mode="level" /></LazyTool>
    case 'decibel-meter':
      return <LazyTool><SensorSuiteSurface mode="sound" /></LazyTool>
    case 'smart-flashlight':
      return <LazyTool><EverydayFlagshipSurface mode="smart-flashlight" /></LazyTool>
    case 'unit-converter':
      return <LazyTool><EverydayFlagshipSurface mode="unit-converter" /></LazyTool>
    case 'qr-studio':
      return <LazyTool><QrStudioSurface /></LazyTool>
    case 'doc-to-pdf':
      return <LazyTool><DocumentSuiteSurface /></LazyTool>
    case 'ocr-text':
      return <LazyTool><OcrTextSurface /></LazyTool>
    case 'color-grabber':
      return <LazyTool><EverydayFlagshipSurface mode="color-grabber" /></LazyTool>
    case 'speaker-cleaner':
      return <LazyTool><SpeakerCleanerFlagship /></LazyTool>
    case 'deep-cleaner':
      return <LazyTool><DeepCleanerFlagship /></LazyTool>
    case 'photo-privacy':
      return <LazyTool><PhotoPrivacySurface /></LazyTool>
    case 'wifi-analyzer':
      return <LazyTool><EverydayFlagshipSurface mode="wifi-analyzer" /></LazyTool>
    case 'password-vault':
      return <LazyTool><PasswordVaultSurface /></LazyTool>
    case 'authenticator-vault':
      return <LazyTool><AuthenticatorVaultSurface /></LazyTool>
    case 'file-studio':
      return <LazyTool><FileStudioSurface /></LazyTool>
    case 'wallpaper-changer':
      return <LazyTool><EverydayFlagshipSurface mode="wallpaper-changer" /></LazyTool>
    case 'bill-splitter':
      return <LazyTool><FinanceSuiteSurface mode="split" /></LazyTool>
    case 'expense-tracker':
      return <LazyTool><FinanceSuiteSurface mode="expenses" /></LazyTool>
    case 'decision-wheel':
      return <LazyTool><EverydayFlagshipSurface mode="decision-wheel" /></LazyTool>
    case 'community-pro-unlock':
      return <LazyTool><EverydayFlagshipSurface mode="community" /></LazyTool>
    case 'screen-recorder':
      return <LazyTool><ScreenRecorderSurface /></LazyTool>
    default:
      return null
  }
}

function LazyTool({ children }: { children: ReactNode }) {
  return (
    <Suspense fallback={<div className="app-surface min-h-56 animate-pulse rounded-[18px]" aria-label="Loading secure tool" />}>
      {children}
    </Suspense>
  )
}

function buildMoonDay(date: Date) {
  return solarToVietnameseLunar(date.getDate(), date.getMonth() + 1, date.getFullYear())
}

function LunarCalendarSurface() {
  const [cursor, setCursor] = useState(() => new Date())
  const [selected, setSelected] = useState(() => new Date())
  const today = new Date()
  const selectedLunar = buildMoonDay(selected)
  const conversionText = `${selected.toLocaleDateString()} → ${selectedLunar.day}/${selectedLunar.month}/${selectedLunar.year}${selectedLunar.leap ? ' (leap month)' : ''}`
  const copyConversion = async () => {
    await navigator.clipboard.writeText(conversionText)
    markToolSuccess('lunar-calendar', { headline: 'Lunar date copied', detail: `${conversionText} was calculated and copied locally.`, shareText: `PureHub converted ${conversionText} offline.` })
  }
  const monthDays = useMemo(() => {
    const year = cursor.getFullYear()
    const month = cursor.getMonth()
    const first = new Date(year, month, 1)
    const startOffset = (first.getDay() + 6) % 7
    const daysInMonth = new Date(year, month + 1, 0).getDate()
    const cells: Array<{ date: Date; inMonth: boolean }> = []

    for (let index = 0; index < 42; index += 1) {
      const day = index - startOffset + 1
      const date = new Date(year, month, day)
      cells.push({ date, inMonth: date.getMonth() === month })
    }

    return { cells, daysInMonth }
  }, [cursor])

  return (
    <div className="space-y-4">
      <FlagshipHero eyebrow="Calendar Suite flagship" title="Lunar Calendar" description="A polished Vietnamese solar–lunar calendar with fast month navigation and private on-device conversion." accent="violet" />
      <div className="grid gap-3 sm:grid-cols-3">
        <Metric label="Selected solar" value={selected.toLocaleDateString()} />
        <Metric label="Selected lunar" value={`${selectedLunar.day}/${selectedLunar.month}/${selectedLunar.year}${selectedLunar.leap ? ' leap' : ''}`} />
        <button className="min-h-16 rounded-[15px] border border-violet-200 bg-violet-50 px-4 text-sm font-black text-violet-900 dark:border-violet-900 dark:bg-violet-950/35 dark:text-violet-100" onClick={() => { const now = new Date(); setCursor(now); setSelected(now) }}>Jump to today</button>
      </div>
      <ActionButton className="w-full justify-center" onClick={() => void copyConversion()}>Copy selected conversion</ActionButton>
      <Panel
        title={cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        subtitle="Vietnamese solar-to-lunar conversion calculated locally for UTC+7."
      >
        <div className="flex items-center justify-between gap-3">
          <ActionButton tone="muted" onClick={() => setCursor((value) => new Date(value.getFullYear(), value.getMonth() - 1, 1))}>
            Previous
          </ActionButton>
          <div className="rounded-2xl border border-slate-500/15 bg-slate-500/5 px-4 py-2 text-sm text-slate-700 dark:text-slate-300">
            Today: {today.toLocaleDateString()}
          </div>
          <ActionButton tone="muted" onClick={() => setCursor((value) => new Date(value.getFullYear(), value.getMonth() + 1, 1))}>
            Next
          </ActionButton>
        </div>

        <div className="mt-4 grid grid-cols-7 gap-1 text-center text-[10px] uppercase tracking-[0.08em] text-slate-500 sm:gap-2 sm:text-xs sm:tracking-[0.2em]">
          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((label) => (
            <div key={label}>{label}</div>
          ))}
        </div>
        <div className="mt-3 grid grid-cols-7 gap-1 sm:gap-2">
          {monthDays.cells.map(({ date, inMonth }) => {
            const lunarDate = buildMoonDay(date)
            const isToday =
              date.getDate() === today.getDate() &&
              date.getMonth() === today.getMonth() &&
              date.getFullYear() === today.getFullYear()

            return (
              <button
                key={date.toISOString()}
                type="button"
                onClick={() => setSelected(date)}
                className={[
                  'min-w-0 rounded-xl border p-1.5 text-left transition hover:-translate-y-0.5 hover:shadow-sm sm:rounded-2xl sm:p-3',
                  date.toDateString() === selected.toDateString() ? 'ring-2 ring-violet-500' : '',
                  isToday
                    ? 'border-emerald-300/45 bg-emerald-400/12 shadow-[0_10px_30px_-16px_rgba(16,185,129,0.45)]'
                    : 'border-slate-500/15 bg-slate-500/5',
                  inMonth ? 'text-slate-950 dark:text-white' : 'text-slate-500',
                ].join(' ')}
              >
                <p className="text-xs font-semibold sm:text-sm">{date.getDate()}</p>
                <p className="mt-1 truncate text-[10px] text-slate-500 dark:text-slate-400 sm:mt-2 sm:text-xs">
                  {lunarDate.day}/{lunarDate.month}{lunarDate.leap ? '*' : ''}
                </p>
              </button>
            )
          })}
        </div>
      </Panel>
    </div>
  )
}

export function CompassSurface() {
  const [heading, setHeading] = useState(0)
  const [permissionState, setPermissionState] = useState<'idle' | 'granted' | 'denied'>('idle')

  useEffect(() => {
    if (permissionState !== 'granted') return

    const onOrientation = (event: DeviceOrientationEvent) => {
      const webkitHeading = (event as DeviceOrientationEvent & { webkitCompassHeading?: number }).webkitCompassHeading
      const nextHeading =
        typeof webkitHeading === 'number'
          ? webkitHeading
          : typeof event.alpha === 'number'
            ? 360 - event.alpha
            : 0
      setHeading(nextHeading)
    }

    window.addEventListener('deviceorientation', onOrientation)
    return () => window.removeEventListener('deviceorientation', onOrientation)
  }, [permissionState])

  const requestPermission = async () => {
    const permissionAPI = (
      DeviceOrientationEvent as typeof DeviceOrientationEvent & {
        requestPermission?: () => Promise<'granted' | 'denied'>
      }
    )

    if (typeof permissionAPI.requestPermission === 'function') {
      const result = await permissionAPI.requestPermission()
      setPermissionState(result === 'granted' ? 'granted' : 'denied')
      return
    }

    setPermissionState('granted')
  }

  const cardinal = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round(heading / 45) % 8]

  return (
    <Panel title="Compass" subtitle="Uses device orientation when your browser exposes motion sensors.">
      <div className="flex flex-col items-center gap-5">
        <div className="relative flex size-72 items-center justify-center rounded-full border border-slate-500/20 bg-[radial-gradient(circle,rgba(15,23,42,0.95),rgba(2,6,23,1))]">
          <div className="absolute inset-6 rounded-full border border-dashed border-slate-500/20" />
          <div className="absolute inset-12 rounded-full border border-slate-500/15" />
          <div
            className="absolute inset-3 transition-transform duration-300"
            style={{ transform: `rotate(${-heading}deg)` }}
            aria-hidden="true"
          >
            <span className="absolute left-1/2 top-1 -translate-x-1/2 text-sm font-black text-rose-300">N</span>
            <span className="absolute bottom-1 left-1/2 -translate-x-1/2 text-sm font-black text-slate-200">S</span>
            <span className="absolute right-1 top-1/2 -translate-y-1/2 text-sm font-black text-slate-200">E</span>
            <span className="absolute left-1 top-1/2 -translate-y-1/2 text-sm font-black text-slate-200">W</span>
          </div>
          <div
            className="absolute h-28 w-1 rounded-full bg-gradient-to-b from-rose-400 to-emerald-300 transition-transform duration-300"
            style={{ transform: `rotate(${heading}deg) translateY(-72px)` }}
          />
          <div className="text-center">
            <p className="text-4xl font-semibold text-slate-950 dark:text-white">{Math.round(heading)}°</p>
            <p className="mt-2 text-sm text-emerald-300">{cardinal}</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <ActionButton onClick={requestPermission}>
            {permissionState === 'granted' ? 'Sensor active' : 'Enable compass'}
          </ActionButton>
          <div className="rounded-2xl border border-slate-500/15 bg-slate-500/5 px-4 py-3 text-sm text-slate-700 dark:text-slate-300">
            {permissionState === 'denied'
              ? 'Sensor permission was denied.'
              : 'Works best in mobile browsers with motion sensor access.'}
          </div>
        </div>
      </div>
    </Panel>
  )
}

export function ExpenseTrackerSurface() {
  const [records, setRecords] = useState<ExpenseRecord[]>([])
  const [title, setTitle] = useState('')
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState('General')
  const [note, setNote] = useState('')

  const loadRecords = async () => {
    const nextRecords = await expenseRepository.list()
    setRecords(nextRecords.sort((a, b) => b.createdAt.localeCompare(a.createdAt)))
  }

  useEffect(() => {
    void loadRecords()
  }, [])

  const total = records.reduce((sum, record) => sum + record.amount, 0)

  return (
    <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
      <Panel title="Add expense" subtitle="Everything stays on this device in IndexedDB.">
        <div className="space-y-3">
          <FormInput value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Coffee, hosting, lunch..." />
          <div className="grid gap-3 sm:grid-cols-2">
            <FormInput value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="Amount" inputMode="decimal" />
            <FormInput value={category} onChange={(event) => setCategory(event.target.value)} placeholder="Category" />
          </div>
          <FormTextArea rows={3} value={note} onChange={(event) => setNote(event.target.value)} placeholder="Optional note" />
          <ActionButton
            onClick={async () => {
              const parsedAmount = Number.parseFloat(amount)
              if (!title.trim() || Number.isNaN(parsedAmount)) return
              await expenseRepository.put({
                id: createId(),
                title: title.trim(),
                amount: parsedAmount,
                category: category.trim() || 'General',
                note: note.trim() || undefined,
                createdAt: new Date().toISOString(),
              })
              setTitle('')
              setAmount('')
              setCategory('General')
              setNote('')
              await loadRecords()
            }}
          >
            Save expense
          </ActionButton>
        </div>
      </Panel>

      <Panel title="Ledger" subtitle={`Total tracked locally: ${formatCurrency(total)}`}>
        <div className="space-y-3">
          {records.length === 0 ? (
            <div className="rounded-[24px] border border-dashed border-slate-500/20 bg-slate-500/4 p-6 text-sm text-slate-500 dark:text-slate-400">
              No expenses yet. Add your first line item to start the offline ledger.
            </div>
          ) : (
            records.map((record) => (
              <div key={record.id} className="rounded-[24px] border border-slate-500/15 bg-slate-500/5 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950 dark:text-white">{record.title}</p>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                      {record.category} · {new Date(record.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold text-emerald-300">{formatCurrency(record.amount)}</p>
                    <button
                      type="button"
                      onClick={async () => {
                        await expenseRepository.remove(record.id)
                        await loadRecords()
                      }}
                      className="mt-2 text-xs text-rose-300 transition hover:text-rose-200"
                    >
                      Delete
                    </button>
                  </div>
                </div>
                {record.note ? <p className="mt-3 text-sm text-slate-700 dark:text-slate-300">{record.note}</p> : null}
              </div>
            ))
          )}
        </div>
      </Panel>
    </div>
  )
}

export function BillSplitterSurface() {
  const [total, setTotal] = useState('0')
  const [tip, setTip] = useState('0')
  const [peopleText, setPeopleText] = useState('Alex, Bao, Chen')

  const people = useMemo(
    () =>
      peopleText
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
    [peopleText],
  )

  const grandTotal = Number.parseFloat(total || '0') + Number.parseFloat(tip || '0')
  const perPerson = people.length > 0 ? grandTotal / people.length : 0

  return (
    <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
      <Panel title="Split setup" subtitle="Fast local calculation for groups, trips, and shared meals.">
        <div className="space-y-3">
          <FormInput value={total} onChange={(event) => setTotal(event.target.value)} placeholder="Bill total" inputMode="decimal" />
          <FormInput value={tip} onChange={(event) => setTip(event.target.value)} placeholder="Tip / tax" inputMode="decimal" />
          <FormTextArea rows={4} value={peopleText} onChange={(event) => setPeopleText(event.target.value)} placeholder="One or more names, separated by commas" />
        </div>
      </Panel>

      <Panel title="Split result" subtitle={`Grand total: ${formatCurrency(grandTotal)}`}>
        <div className="space-y-3">
          {people.map((person) => (
            <div key={person} className="flex items-center justify-between rounded-[22px] border border-slate-500/15 bg-slate-500/5 px-4 py-3">
              <span className="text-sm text-slate-950 dark:text-white">{person}</span>
              <span className="font-semibold text-emerald-300">{formatCurrency(perPerson)}</span>
            </div>
          ))}
          {people.length === 0 ? (
            <div className="rounded-[24px] border border-dashed border-slate-500/20 bg-slate-500/4 p-6 text-sm text-slate-500 dark:text-slate-400">
              Add at least one person to see the split.
            </div>
          ) : null}
        </div>
      </Panel>
    </div>
  )
}

export function BubbleLevelSurface() {
  const [tilt, setTilt] = useState({ beta: 0, gamma: 0 })

  useEffect(() => {
    const onOrientation = (event: DeviceOrientationEvent) => {
      setTilt({
        beta: event.beta ?? 0,
        gamma: event.gamma ?? 0,
      })
    }
    window.addEventListener('deviceorientation', onOrientation)
    return () => window.removeEventListener('deviceorientation', onOrientation)
  }, [])

  const left = Math.max(12, Math.min(88, 50 + tilt.gamma))
  const top = Math.max(12, Math.min(88, 50 + tilt.beta))
  const isLevel = Math.abs(tilt.beta) < 2 && Math.abs(tilt.gamma) < 2

  return (
    <Panel title="Bubble Level" subtitle="Quick 2D balance feedback using device orientation.">
      <div className="mx-auto flex max-w-sm flex-col items-center gap-4">
        <div className="relative aspect-square w-full rounded-[32px] border border-slate-500/20 bg-slate-500/5">
          <div className="absolute inset-[10%] rounded-[26px] border border-dashed border-slate-500/20" />
          <div className="absolute inset-0" aria-hidden="true">
            <span className="absolute left-[10%] top-[10%] h-px w-[56.5%] origin-left rotate-45 bg-slate-400/35" />
            <span className="absolute right-[10%] top-[10%] h-px w-[56.5%] origin-right -rotate-45 bg-slate-400/35" />
            <span className="absolute bottom-[10%] left-[10%] h-px w-[56.5%] origin-left -rotate-45 bg-slate-400/35" />
            <span className="absolute bottom-[10%] right-[10%] h-px w-[56.5%] origin-right rotate-45 bg-slate-400/35" />
            <span className={`absolute left-1/2 top-1/2 size-16 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 ${isLevel ? 'border-emerald-500 bg-emerald-500/10' : 'border-slate-400/45'}`} />
            <span className="absolute left-1/2 top-[38%] h-[24%] w-px -translate-x-1/2 bg-slate-400/45" />
            <span className="absolute left-[38%] top-1/2 h-px w-[24%] -translate-y-1/2 bg-slate-400/45" />
          </div>
          <div
            className={`absolute size-12 -translate-x-1/2 -translate-y-1/2 rounded-full border shadow-[0_0_35px_rgba(103,232,249,0.45)] transition-all duration-150 ${isLevel ? 'border-emerald-100 bg-emerald-400' : 'border-cyan-200/20 bg-cyan-300/85'}`}
            style={{ left: `${left}%`, top: `${top}%` }}
          />
        </div>
        <p className={`rounded-full px-3 py-1 text-xs font-bold ${isLevel ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300' : 'bg-slate-500/10 text-slate-600 dark:text-slate-300'}`}>
          {isLevel ? 'Centered · surface is level' : 'Move the bubble into the center target'}
        </p>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Beta {tilt.beta.toFixed(1)}° · Gamma {tilt.gamma.toFixed(1)}°
        </p>
      </div>
    </Panel>
  )
}

export function DecibelMeterSurface() {
  const [running, setRunning] = useState(false)
  const [level, setLevel] = useState(0)
  const [peak, setPeak] = useState(0)
  const [samples, setSamples] = useState<Array<{ value: number; at: number }>>([])
  const [averageWindow, setAverageWindow] = useState(5)
  const analyserRef = useRef<AnalyserNode | null>(null)
  const audioContextRef = useRef<AudioContext | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  useEffect(() => {
    if (!running) return

    let animationFrame = 0
    let lastSampleAt = 0

    const start = async () => {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      const audioContext = new AudioContext()
      audioContextRef.current = audioContext
      const source = audioContext.createMediaStreamSource(stream)
      const analyser = audioContext.createAnalyser()
      analyser.fftSize = 512
      source.connect(analyser)
      analyserRef.current = analyser
      const data = new Uint8Array(analyser.frequencyBinCount)

      const tick = () => {
        analyser.getByteTimeDomainData(data)
        let sum = 0
        for (const value of data) {
          const normalized = (value - 128) / 128
          sum += normalized * normalized
        }
        const rms = Math.sqrt(sum / data.length)
        const db = Math.max(0, Math.min(100, Math.round(20 * Math.log10(rms || 0.0001) + 100)))
        setLevel(db)
        setPeak((value) => Math.max(value, db))
        const now = Date.now()
        if (now - lastSampleAt >= 250) {
          lastSampleAt = now
          setSamples((values) => [...values.filter((sample) => sample.at >= now - 60_000), { value: db, at: now }])
        }
        animationFrame = window.requestAnimationFrame(tick)
      }

      tick()
    }

    void start()

    return () => {
      window.cancelAnimationFrame(animationFrame)
      analyserRef.current = null
      streamRef.current?.getTracks().forEach((track) => track.stop())
      streamRef.current = null
      void audioContextRef.current?.close()
      audioContextRef.current = null
      setLevel(0)
    }
  }, [running])

  const averageSamples = samples.filter((sample) => sample.at >= Date.now() - averageWindow * 1000)
  const average = averageSamples.length
    ? Math.round(averageSamples.reduce((sum, sample) => sum + sample.value, 0) / averageSamples.length)
    : 0
  const chartSamples = samples.slice(-40)

  return (
    <Panel title="Decibel Meter" subtitle="Private microphone analysis with no uploads and no ads.">
      <div className="space-y-4">
        <div className="flex h-28 items-end gap-1 rounded-[16px] bg-slate-500/5 p-3" aria-label="Recent loudness history">
          {chartSamples.length ? chartSamples.map((sample) => (
            <span key={sample.at} className="min-w-1 flex-1 rounded-t bg-emerald-500/70 transition-all" style={{ height: `${Math.max(4, sample.value)}%` }} />
          )) : <span className="m-auto text-sm text-slate-500">Start the microphone to see a private local chart.</span>}
        </div>
        <div className="h-4 overflow-hidden rounded-full border border-slate-500/20 bg-slate-500/5">
          <div className="h-full rounded-full bg-gradient-to-r from-emerald-300 via-amber-300 to-rose-300 transition-all" style={{ width: `${level}%` }} />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-3xl font-semibold text-slate-950 dark:text-white">~{level} dB</p>
            <p className="mt-1 text-sm text-slate-500">Peak ~{peak} dB · {noiseLabel(level)}</p>
          </div>
          <ActionButton onClick={() => setRunning((value) => !value)}>{running ? 'Stop mic' : 'Start mic'}</ActionButton>
        </div>
        <div className="rounded-[16px] border border-slate-500/15 bg-slate-500/5 p-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-slate-500">Rolling average</p>
              <p className="mt-1 text-2xl font-bold text-slate-950 dark:text-white">~{average} dB <span className="text-sm font-medium text-slate-500">last {averageWindow}s</span></p>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {[5, 10, 30, 60].map((seconds) => (
                <ActionButton key={seconds} tone={averageWindow === seconds ? 'primary' : 'muted'} onClick={() => setAverageWindow(seconds)} className="min-h-9 px-3 py-1.5 text-xs">
                  {seconds}s
                </ActionButton>
              ))}
            </div>
          </div>
        </div>
        <p className="rounded-[14px] bg-amber-500/10 p-3 text-xs leading-5 text-amber-800 dark:text-amber-200">
          Estimated reading only. Browser microphones are not calibrated sound meters and this result must not be used for legal or workplace safety decisions.
        </p>
      </div>
    </Panel>
  )
}

export function SmartFlashlightSurface() {
  const [active, setActive] = useState(false)
  const [color, setColor] = useState('#ffffff')

  useEffect(() => {
    if (!active) return
    const previous = document.body.style.background
    document.body.style.background = color
    return () => {
      document.body.style.background = previous
    }
  }, [active, color])

  return (
    <Panel title="Smart Flashlight" subtitle="A browser-safe screen light. The native Android app can also control the hardware torch.">
      <div className="grid gap-4 sm:grid-cols-[1fr_0.8fr]">
        <button
          type="button"
          className="grid min-h-64 place-items-center rounded-[22px] border border-slate-500/15 text-lg font-bold shadow-inner transition"
          style={{ backgroundColor: active ? color : undefined, color: active && color === '#ffffff' ? '#0f172a' : undefined }}
          onClick={() => setActive((value) => !value)}
        >
          {active ? 'Tap to turn off' : 'Tap to turn on'}
        </button>
        <div>
          <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">Screen color</p>
          <div className="mt-3 grid grid-cols-3 gap-2">
            {['#ffffff', '#fee2e2', '#dcfce7', '#dbeafe', '#fef3c7', '#e9d5ff'].map((item) => (
              <button key={item} type="button" className={`aspect-square rounded-[14px] border ${color === item ? 'ring-2 ring-emerald-500' : 'border-slate-500/15'}`} style={{ backgroundColor: item }} onClick={() => setColor(item)} aria-label={`Use ${item}`} />
            ))}
          </div>
          <p className="mt-4 text-xs leading-5 text-slate-500">Avoid flashing patterns around people who may be sensitive to light.</p>
        </div>
      </div>
    </Panel>
  )
}

export function UnitConverterSurface() {
  const [category, setCategory] = useState<'length' | 'weight' | 'temperature'>('length')
  const [value, setValue] = useState('1')

  const parsed = Number.parseFloat(value || '0')
  const conversions =
    category === 'length'
      ? [
          ['Meters', parsed],
          ['Kilometers', parsed / 1000],
          ['Feet', parsed * 3.28084],
          ['Miles', parsed * 0.000621371],
        ]
      : category === 'weight'
        ? [
            ['Kilograms', parsed],
            ['Grams', parsed * 1000],
            ['Pounds', parsed * 2.20462],
            ['Ounces', parsed * 35.274],
          ]
        : [
            ['Celsius', parsed],
            ['Fahrenheit', parsed * (9 / 5) + 32],
            ['Kelvin', parsed + 273.15],
          ]

  return (
    <Panel title="Unit Converter" subtitle="Instant client-side math with no latency and no network dependency.">
      <div className="grid gap-3 sm:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-3">
          <select
            value={category}
            onChange={(event) => setCategory(event.target.value as typeof category)}
            className="w-full rounded-2xl border border-slate-500/20 bg-slate-500/5 px-4 py-3 text-sm text-slate-950 dark:text-white outline-none"
          >
            <option value="length">Length</option>
            <option value="weight">Weight</option>
            <option value="temperature">Temperature</option>
          </select>
          <FormInput value={value} onChange={(event) => setValue(event.target.value)} inputMode="decimal" />
        </div>
        <div className="space-y-3">
          {conversions.map(([label, converted]) => (
            <div key={label} className="flex items-center justify-between rounded-[22px] border border-slate-500/15 bg-slate-500/5 px-4 py-3">
              <span className="text-sm text-slate-700 dark:text-slate-300">{label}</span>
              <span className="font-semibold text-slate-950 dark:text-white">{Number(converted).toFixed(3)}</span>
            </div>
          ))}
        </div>
      </div>
    </Panel>
  )
}

export function DocToPdfSurface() {
  const [images, setImages] = useState<string[]>([])

  const handleFiles = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? [])
    const nextImages = await Promise.all(files.map(readFileAsDataUrl))
    setImages(nextImages)
  }

  return (
    <Panel title="Doc to PDF" subtitle="Pick local images, then export them into one PDF without uploading anything.">
      <FormInput type="file" accept="image/*" multiple onChange={handleFiles} />
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        {images.map((image, index) => (
          <div key={`${image.slice(0, 32)}-${index}`} className="rounded-[18px] border border-slate-500/15 bg-slate-500/5 p-2">
            <img src={image} alt={`Document page ${index + 1}`} className="aspect-[3/4] w-full rounded-[14px] object-cover" />
            <div className="mt-2 flex items-center justify-between gap-2">
              <span className="text-xs font-semibold text-slate-500">Page {index + 1}</span>
              <div className="flex gap-1">
                <button type="button" className="filter-chip" disabled={index === 0} onClick={() => setImages((items) => moveItem(items, index, index - 1))}>←</button>
                <button type="button" className="filter-chip" disabled={index === images.length - 1} onClick={() => setImages((items) => moveItem(items, index, index + 1))}>→</button>
                <button type="button" className="filter-chip text-rose-500" onClick={() => setImages((items) => items.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="mt-4">
        <ActionButton
          disabled={images.length === 0}
          onClick={async () => {
            const { jsPDF } = await import('jspdf')
            const pdf = new jsPDF({ unit: 'px', format: 'a4' })
            for (const [index, image] of images.entries()) {
              if (index > 0) pdf.addPage()
              pdf.addImage(image, 'JPEG', 24, 24, 547, 770, undefined, 'FAST')
            }
            pdf.save('purehub-doc.pdf')
          }}
        >
          Export PDF
        </ActionButton>
      </div>
    </Panel>
  )
}

export function ColorGrabberSurface() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const [image, setImage] = useState<string | null>(null)
  const [pickedColor, setPickedColor] = useState('#000000')

  useEffect(() => {
    if (!image || !canvasRef.current) return
    const context = canvasRef.current.getContext('2d')
    if (!context) return
    const element = new Image()
    element.onload = () => {
      canvasRef.current!.width = element.width
      canvasRef.current!.height = element.height
      context.drawImage(element, 0, 0)
    }
    element.src = image
  }, [image])

  return (
    <Panel title="Color Grabber" subtitle="Upload an image, tap the canvas, and sample precise local colors.">
      <FormInput
        type="file"
        accept="image/*"
        onChange={async (event) => {
          const file = event.target.files?.[0]
          if (!file) return
          setImage(await readFileAsDataUrl(file))
        }}
      />
      <div className="mt-4 grid gap-4 xl:grid-cols-[1fr_0.4fr]">
        <div className="overflow-hidden rounded-[24px] border border-slate-500/15 bg-slate-500/5 p-3">
          <canvas
            ref={canvasRef}
            className="max-h-[420px] w-full cursor-crosshair rounded-2xl object-contain"
            onClick={(event) => {
              const canvas = canvasRef.current
              const context = canvas?.getContext('2d')
              if (!canvas || !context) return
              const rect = canvas.getBoundingClientRect()
              const x = Math.floor(((event.clientX - rect.left) / rect.width) * canvas.width)
              const y = Math.floor(((event.clientY - rect.top) / rect.height) * canvas.height)
              const [r, g, b] = context.getImageData(x, y, 1, 1).data
              setPickedColor(`#${[r, g, b].map((value) => value.toString(16).padStart(2, '0')).join('')}`)
            }}
          />
        </div>
        <div className="rounded-[24px] border border-slate-500/15 bg-slate-500/5 p-4">
          <div className="h-24 rounded-2xl border border-slate-500/20" style={{ backgroundColor: pickedColor }} />
          <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">HEX</p>
          <p className="mt-1 text-lg font-semibold text-slate-950 dark:text-white">{pickedColor}</p>
        </div>
      </div>
    </Panel>
  )
}

export function SpeakerCleanerSurface() {
  const [playing, setPlaying] = useState(false)
  const [frequency, setFrequency] = useState(165)
  const audioContextRef = useRef<AudioContext | null>(null)
  const oscillatorRef = useRef<OscillatorNode | null>(null)

  useEffect(() => {
    if (!playing) {
      oscillatorRef.current?.stop()
      oscillatorRef.current = null
      void audioContextRef.current?.close()
      audioContextRef.current = null
      return
    }

    const context = new AudioContext()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.frequency.value = frequency
    oscillator.type = 'sine'
    gain.gain.value = 0.08
    oscillator.connect(gain)
    gain.connect(context.destination)
    oscillator.start()
    audioContextRef.current = context
    oscillatorRef.current = oscillator

    return () => {
      oscillator.stop()
      void context.close()
    }
  }, [frequency, playing])

  return (
    <Panel title="Speaker Cleaner" subtitle="Local tone generator designed to help move residual water from a phone speaker.">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-slate-700 dark:text-slate-300">Frequency</p>
          <p className="mt-1 text-3xl font-semibold text-slate-950 dark:text-white">{frequency} Hz</p>
        </div>
        <ActionButton onClick={() => setPlaying((value) => !value)}>{playing ? 'Stop tone' : 'Start tone'}</ActionButton>
      </div>
      <input className="mt-5 w-full accent-emerald-500" type="range" min="120" max="240" step="5" value={frequency} onChange={(event) => setFrequency(Number(event.target.value))} aria-label="Tone frequency" />
      <p className="mt-4 rounded-[14px] bg-amber-500/10 p-3 text-xs leading-5 text-amber-800 dark:text-amber-200">Start at a comfortable volume, keep the speaker facing down, and stop if the sound distorts. This cannot repair damaged hardware.</p>
    </Panel>
  )
}

export function DeepCleanerSurface() {
  const [files, setFiles] = useState<File[]>([])
  const totalBytes = files.reduce((sum, file) => sum + file.size, 0)
  const duplicateNames = new Set(
    files
      .map((file) => file.name)
      .filter((name, index, items) => items.indexOf(name) !== index),
  )

  return (
    <Panel title="Device Cleaner" subtitle="Review files safely. PureHub never deletes browser-selected files automatically.">
      <FormInput
        type="file"
        multiple
        onChange={(event) => setFiles(Array.from(event.target.files ?? []))}
      />
      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <Metric label="Files reviewed" value={String(files.length)} />
        <Metric label="Total size" value={formatBytes(totalBytes)} />
        <Metric label="Possible duplicates" value={String(duplicateNames.size)} />
      </div>
      <div className="mt-4 space-y-2">
        {files.slice(0, 20).map((file, index) => (
          <div key={`${file.name}-${file.lastModified}-${index}`} className="flex items-center justify-between gap-3 rounded-[14px] bg-slate-500/5 px-4 py-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-950 dark:text-white">{file.name}</p>
              <p className="text-xs text-slate-500">{formatBytes(file.size)}</p>
            </div>
            {duplicateNames.has(file.name) ? <span className="rounded-full bg-amber-500/10 px-2 py-1 text-xs text-amber-700 dark:text-amber-200">Review duplicate</span> : null}
          </div>
        ))}
      </div>
      <p className="mt-4 rounded-[14px] bg-sky-500/8 p-3 text-xs leading-5 text-sky-800 dark:text-sky-200">
        Browsers cannot clean your device silently—and that is safer. Use the native Android cleaner for preview, selection and recoverable deletion.
      </p>
    </Panel>
  )
}

export function WifiAnalyzerSurface() {
  const connection = (navigator as Navigator & {
    connection?: { effectiveType?: string; downlink?: number; rtt?: number; saveData?: boolean }
  }).connection
  const online = navigator.onLine

  return (
    <Panel title="Wi-Fi Analyzer" subtitle="A privacy-safe browser overview. Nearby access-point scanning is available in the Android app.">
      <div className="grid gap-3 sm:grid-cols-2">
        <Metric label="Status" value={online ? 'Online' : 'Offline'} />
        <Metric label="Connection" value={connection?.effectiveType?.toUpperCase() ?? 'Not exposed'} />
        <Metric label="Estimated downlink" value={connection?.downlink ? `${connection.downlink} Mbps` : 'Not exposed'} />
        <Metric label="Estimated latency" value={connection?.rtt ? `${connection.rtt} ms` : 'Not exposed'} />
      </div>
      <p className="mt-4 rounded-[14px] bg-emerald-500/8 p-3 text-xs leading-5 text-emerald-800 dark:text-emerald-200">
        PureHub does not fingerprint nearby Wi-Fi networks from the web. The Android version asks for Nearby/Location permission only when you start a scan.
      </p>
    </Panel>
  )
}

export function WallpaperChangerSurface() {
  const [images, setImages] = useState<string[]>([])
  const [selected, setSelected] = useState(0)

  return (
    <Panel title="Wallpaper Studio" subtitle="Preview your own images locally without uploads, feeds, or tracking.">
      <FormInput
        type="file"
        accept="image/*"
        multiple
        onChange={async (event) => {
          const selectedFiles = Array.from(event.target.files ?? [])
          setImages(await Promise.all(selectedFiles.map(readFileAsDataUrl)))
          setSelected(0)
        }}
      />
      {images.length ? (
        <>
          <div className="mx-auto mt-4 aspect-[9/16] max-h-[520px] overflow-hidden rounded-[28px] border-[8px] border-slate-900 bg-slate-900 shadow-xl">
            <img src={images[selected]} alt="Wallpaper preview" className="h-full w-full object-cover" />
          </div>
          <div className="mt-4 flex gap-2 overflow-x-auto">
            {images.map((image, index) => (
              <button key={`${image.slice(0, 32)}-${index}`} type="button" className={`h-20 w-14 shrink-0 overflow-hidden rounded-[10px] ${selected === index ? 'ring-2 ring-emerald-500' : ''}`} onClick={() => setSelected(index)}>
                <img src={image} alt={`Wallpaper ${index + 1}`} className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
          <p className="mt-4 text-xs leading-5 text-slate-500">Download or use the Android app to apply the selected image to Home, Lock, or both screens.</p>
        </>
      ) : <div className="empty-state mt-4"><p>Choose local images to build a private wallpaper collection.</p></div>}
    </Panel>
  )
}

export function DecisionWheelSurface() {
  const [optionsText, setOptionsText] = useState('Coffee\nTea\nJuice\nWater')
  const [rotation, setRotation] = useState(0)
  const [result, setResult] = useState('')

  const options = useMemo(
    () =>
      optionsText
        .split('\n')
        .map((item) => item.trim())
        .filter(Boolean),
    [optionsText],
  )
  const wheelColors = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#f43f5e', '#14b8a6']
  const wheelGradient = options.length
    ? `conic-gradient(${options.map((_, index) => {
        const start = (index / options.length) * 360
        const end = ((index + 1) / options.length) * 360
        return `${wheelColors[index % wheelColors.length]} ${start}deg ${end}deg`
      }).join(',')})`
    : 'conic-gradient(#94a3b8 0deg 360deg)'

  return (
    <div className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
      <Panel title="Decision Wheel" subtitle="Give fate a spin with local options and a game-like result reveal.">
        <FormTextArea rows={8} value={optionsText} onChange={(event) => setOptionsText(event.target.value)} />
        <div className="mt-4 flex items-center gap-3">
          <ActionButton
            disabled={options.length === 0}
            onClick={() => {
              const random = new Uint32Array(1)
              crypto.getRandomValues(random)
              const winnerIndex = random[0] % options.length
              const segment = 360 / options.length
              setRotation((value) => {
                const current = ((value % 360) + 360) % 360
                const winnerCenter = winnerIndex * segment + segment / 2
                const target = (360 - winnerCenter - current + 360) % 360
                return value + 1800 + target
              })
              setResult(options[winnerIndex])
            }}
          >
            Spin
          </ActionButton>
          {result ? <p className="text-sm text-emerald-300">Selected: {result}</p> : null}
        </div>
      </Panel>

      <Panel title="Wheel" subtitle="A lightweight roulette built with pure CSS rotation.">
        <div className="flex flex-col items-center gap-6">
          <div className="relative">
            <div className="absolute left-1/2 top-[-10px] z-10 h-0 w-0 -translate-x-1/2 border-x-[12px] border-b-[18px] border-x-transparent border-b-rose-300" />
            <div
              className="flex size-72 items-center justify-center rounded-full border border-slate-500/20 bg-[conic-gradient(from_90deg,#34d399,#22d3ee,#a855f7,#f59e0b,#34d399)] transition-transform duration-[2200ms] ease-out"
              style={{ transform: `rotate(${rotation}deg)`, background: wheelGradient }}
            >
              <div className="flex size-24 items-center justify-center rounded-full border border-slate-500/20 bg-white/90 dark:bg-slate-950/90 text-center text-sm text-slate-950 dark:text-white">
                {result || 'Spin'}
              </div>
            </div>
          </div>
          <div className="grid w-full gap-2 sm:grid-cols-2">
            {options.map((option) => (
              <div key={option} className="rounded-[20px] border border-slate-500/15 bg-slate-500/5 px-4 py-3 text-sm text-slate-700 dark:text-slate-300">
                {option}
              </div>
            ))}
          </div>
        </div>
      </Panel>
    </div>
  )
}

export function CommunityUnlockSurface() {
  return (
    <Panel title="PureHub Community" subtitle="Every tool stays free. Join the people building and improving PureHub together.">
      <div className="grid gap-3 sm:grid-cols-2">
        <ActionButton
          onClick={() => window.open('https://t.me/aaa_letan_vip_bot', '_blank', 'noopener,noreferrer')}
        >
          Join Telegram
        </ActionButton>
        <ActionButton
          tone="muted"
          onClick={() => window.open('https://github.com/shoyrulove-dev/PureHub', '_blank', 'noopener,noreferrer')}
        >
          Contribute on GitHub
        </ActionButton>
      </div>
      <div className="mt-4 rounded-[16px] bg-emerald-500/8 p-4 text-sm leading-6 text-emerald-900 dark:text-emerald-100">
        No Pro code, no paywall and no ads. Report bugs, suggest useful mini apps, improve translations, or share PureHub with someone who needs it.
      </div>
    </Panel>
  )
}

function jdFromDate(day: number, month: number, year: number) {
  const a = Math.floor((14 - month) / 12)
  const y = year + 4800 - a
  const m = month + 12 * a - 3
  let jd = day + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045
  if (jd < 2299161) {
    jd = day + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - 32083
  }
  return jd
}

function newMoon(k: number) {
  const t = k / 1236.85
  const t2 = t * t
  const t3 = t2 * t
  const dr = Math.PI / 180
  let jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
  jd1 += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
  const m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
  const moon = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
  const f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
  let c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * dr * m)
  c1 -= 0.4068 * Math.sin(moon * dr) + 0.0161 * Math.sin(2 * dr * moon)
  c1 -= 0.0004 * Math.sin(3 * dr * moon)
  c1 += 0.0104 * Math.sin(2 * dr * f) - 0.0051 * Math.sin((m + moon) * dr)
  c1 -= 0.0074 * Math.sin((m - moon) * dr) + 0.0004 * Math.sin((2 * f + m) * dr)
  c1 -= 0.0004 * Math.sin((2 * f - m) * dr) - 0.0006 * Math.sin((2 * f + moon) * dr)
  c1 += 0.001 * Math.sin((2 * f - moon) * dr) + 0.0005 * Math.sin((2 * moon + m) * dr)
  const deltaT = t < -11
    ? 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
    : -0.000278 + 0.000265 * t + 0.000262 * t2
  return jd1 + c1 - deltaT
}

function sunLongitude(jdn: number) {
  const t = (jdn - 2451545) / 36525
  const t2 = t * t
  const dr = Math.PI / 180
  const m = 357.5291 + 35999.0503 * t - 0.0001559 * t2 - 0.00000048 * t * t2
  const l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
  let dl = (1.9146 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m)
  dl += (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.00029 * Math.sin(dr * 3 * m)
  let longitude = (l0 + dl) * dr
  longitude -= Math.PI * 2 * Math.floor(longitude / (Math.PI * 2))
  return longitude
}

function getNewMoonDay(k: number, timeZone = 7) {
  return Math.floor(newMoon(k) + 0.5 + timeZone / 24)
}

function getSunLongitude(dayNumber: number, timeZone = 7) {
  return Math.floor((sunLongitude(dayNumber - 0.5 - timeZone / 24) / Math.PI) * 6)
}

function getLunarMonth11(year: number, timeZone = 7) {
  const offset = jdFromDate(31, 12, year) - 2415021
  const k = Math.floor(offset / 29.530588853)
  let newMoonDay = getNewMoonDay(k, timeZone)
  if (getSunLongitude(newMoonDay, timeZone) >= 9) newMoonDay = getNewMoonDay(k - 1, timeZone)
  return newMoonDay
}

function getLeapMonthOffset(a11: number, timeZone = 7) {
  const k = Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853)
  let last = 0
  let index = 1
  let arc = getSunLongitude(getNewMoonDay(k + index, timeZone), timeZone)
  do {
    last = arc
    index += 1
    arc = getSunLongitude(getNewMoonDay(k + index, timeZone), timeZone)
  } while (arc !== last && index < 15)
  return index - 1
}

function solarToVietnameseLunar(day: number, month: number, year: number) {
  const dayNumber = jdFromDate(day, month, year)
  const k = Math.floor((dayNumber - 2415021.076998695) / 29.530588853)
  let monthStart = getNewMoonDay(k + 1)
  if (monthStart > dayNumber) monthStart = getNewMoonDay(k)
  let a11 = getLunarMonth11(year)
  let b11 = a11
  let lunarYear: number
  if (a11 >= monthStart) {
    lunarYear = year
    a11 = getLunarMonth11(year - 1)
  } else {
    lunarYear = year + 1
    b11 = getLunarMonth11(year + 1)
  }
  const lunarDay = dayNumber - monthStart + 1
  const diff = Math.floor((monthStart - a11) / 29)
  let lunarMonth = diff + 11
  let leap = false
  if (b11 - a11 > 365) {
    const leapDiff = getLeapMonthOffset(a11)
    if (diff >= leapDiff) {
      lunarMonth = diff + 10
      leap = diff === leapDiff
    }
  }
  if (lunarMonth > 12) lunarMonth -= 12
  if (lunarMonth >= 11 && diff < 4) lunarYear -= 1
  return { day: lunarDay, month: lunarMonth, year: lunarYear, leap }
}

function createId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function formatCurrency(value: number) {
  const locale = navigator.language || 'en-US'
  const currency = locale.toLowerCase().startsWith('vi') ? 'VND' : 'USD'
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
    maximumFractionDigits: currency === 'VND' ? 0 : 2,
  }).format(value || 0)
}

function formatBytes(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function moveItem<T>(items: T[], from: number, to: number) {
  const next = [...items]
  const [item] = next.splice(from, 1)
  next.splice(to, 0, item)
  return next
}

function noiseLabel(value: number) {
  if (value < 35) return 'Quiet'
  if (value < 60) return 'Moderate'
  if (value < 80) return 'Loud'
  return 'Very loud'
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[15px] bg-slate-500/5 p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-lg font-bold text-slate-950 dark:text-white">{value}</p>
    </div>
  )
}

async function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

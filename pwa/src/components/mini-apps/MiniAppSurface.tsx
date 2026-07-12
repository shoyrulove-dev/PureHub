import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import type { MiniAppId } from '../../features/catalog/tabs'
import {
  expenseRepository,
  habitCheckInRepository,
  habitRepository,
  type ExpenseRecord,
  type HabitCheckInRecord,
  type HabitRecord,
} from '../../lib/db/purehub-db'

type MiniAppSurfaceProps = {
  miniAppId: MiniAppId
}

type PanelProps = {
  title: string
  subtitle?: string
  children: React.ReactNode
}

type FormInputProps = React.InputHTMLAttributes<HTMLInputElement>

export function MiniAppSurface({ miniAppId }: MiniAppSurfaceProps) {
  switch (miniAppId) {
    case 'lunar-calendar':
      return <LunarCalendarSurface />
    case 'zen-habit':
      return <ZenHabitSurface />
    case 'zen-pomodoro':
      return <ZenPomodoroSurface />
    case 'zen-breath':
      return <ZenBreathSurface />
    case 'compass':
      return <CompassSurface />
    case 'bubble-level':
      return <BubbleLevelSurface />
    case 'decibel-meter':
      return <DecibelMeterSurface />
    case 'unit-converter':
      return <UnitConverterSurface />
    case 'qr-studio':
      return <QrStudioSurface />
    case 'doc-to-pdf':
      return <DocToPdfSurface />
    case 'ocr-text':
      return <OcrTextSurface />
    case 'color-grabber':
      return <ColorGrabberSurface />
    case 'speaker-cleaner':
      return <SpeakerCleanerSurface />
    case 'password-vault':
      return <PasswordVaultSurface />
    case 'bill-splitter':
      return <BillSplitterSurface />
    case 'expense-tracker':
      return <ExpenseTrackerSurface />
    case 'decision-wheel':
      return <DecisionWheelSurface />
    case 'community-pro-unlock':
      return <CommunityUnlockSurface />
    default:
      return null
  }
}

function Panel({ title, subtitle, children }: PanelProps) {
  return (
    <section className="rounded-[28px] border border-white/8 bg-slate-950/90 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.45)]">
      <div className="mb-4">
        <h2 className="text-lg font-semibold text-white">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm leading-6 text-slate-400">{subtitle}</p> : null}
      </div>
      {children}
    </section>
  )
}

function FormInput(props: FormInputProps) {
  return (
    <input
      {...props}
      className={[
        'w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-emerald-300/40 focus:bg-white/7',
        props.className,
      ]
        .filter(Boolean)
        .join(' ')}
    />
  )
}

function FormTextArea(
  props: React.TextareaHTMLAttributes<HTMLTextAreaElement>,
) {
  return (
    <textarea
      {...props}
      className={[
        'w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-emerald-300/40 focus:bg-white/7',
        props.className,
      ]
        .filter(Boolean)
        .join(' ')}
    />
  )
}

function ActionButton(
  props: React.ButtonHTMLAttributes<HTMLButtonElement> & { tone?: 'primary' | 'muted' },
) {
  const tone = props.tone ?? 'primary'
  return (
    <button
      {...props}
      className={[
        'rounded-2xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50',
        tone === 'primary'
          ? 'bg-emerald-400/14 text-emerald-200 ring-1 ring-inset ring-emerald-400/18 hover:bg-emerald-400/20'
          : 'bg-white/5 text-slate-300 hover:bg-white/8',
        props.className,
      ]
        .filter(Boolean)
        .join(' ')}
    />
  )
}

function buildMoonDay(date: Date) {
  const knownNewMoon = Date.UTC(2000, 0, 6, 18, 14)
  const lunarCycle = 29.530588853
  const diffDays = (Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) - knownNewMoon) / 86400000
  const normalized = ((diffDays % lunarCycle) + lunarCycle) % lunarCycle
  return Math.floor(normalized) + 1
}

function LunarCalendarSurface() {
  const [cursor, setCursor] = useState(() => new Date())
  const today = new Date()
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
      <Panel
        title={cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        subtitle="Offline month board with a lightweight moon-cycle estimate for quick browsing."
      >
        <div className="flex items-center justify-between gap-3">
          <ActionButton tone="muted" onClick={() => setCursor((value) => new Date(value.getFullYear(), value.getMonth() - 1, 1))}>
            Previous
          </ActionButton>
          <div className="rounded-2xl border border-white/8 bg-white/5 px-4 py-2 text-sm text-slate-300">
            Today: {today.toLocaleDateString()}
          </div>
          <ActionButton tone="muted" onClick={() => setCursor((value) => new Date(value.getFullYear(), value.getMonth() + 1, 1))}>
            Next
          </ActionButton>
        </div>

        <div className="mt-4 grid grid-cols-7 gap-2 text-center text-xs uppercase tracking-[0.2em] text-slate-500">
          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((label) => (
            <div key={label}>{label}</div>
          ))}
        </div>
        <div className="mt-3 grid grid-cols-7 gap-2">
          {monthDays.cells.map(({ date, inMonth }) => {
            const moonDay = buildMoonDay(date)
            const isToday =
              date.getDate() === today.getDate() &&
              date.getMonth() === today.getMonth() &&
              date.getFullYear() === today.getFullYear()

            return (
              <div
                key={date.toISOString()}
                className={[
                  'rounded-2xl border p-3 text-left transition',
                  isToday
                    ? 'border-emerald-300/45 bg-emerald-400/12 shadow-[0_10px_30px_-16px_rgba(16,185,129,0.45)]'
                    : 'border-white/8 bg-white/5',
                  inMonth ? 'text-white' : 'text-slate-500',
                ].join(' ')}
              >
                <p className="text-sm font-semibold">{date.getDate()}</p>
                <p className="mt-2 text-xs text-slate-400">Moon {moonDay}</p>
              </div>
            )
          })}
        </div>
      </Panel>
    </div>
  )
}

function CompassSurface() {
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
        <div className="relative flex size-72 items-center justify-center rounded-full border border-white/10 bg-[radial-gradient(circle,rgba(15,23,42,0.95),rgba(2,6,23,1))]">
          <div className="absolute inset-6 rounded-full border border-dashed border-white/10" />
          <div className="absolute inset-12 rounded-full border border-white/8" />
          <div
            className="absolute h-28 w-1 rounded-full bg-gradient-to-b from-rose-400 to-emerald-300 transition-transform duration-300"
            style={{ transform: `rotate(${heading}deg) translateY(-72px)` }}
          />
          <div className="text-center">
            <p className="text-4xl font-semibold text-white">{Math.round(heading)}°</p>
            <p className="mt-2 text-sm text-emerald-300">{cardinal}</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <ActionButton onClick={requestPermission}>
            {permissionState === 'granted' ? 'Sensor active' : 'Enable compass'}
          </ActionButton>
          <div className="rounded-2xl border border-white/8 bg-white/5 px-4 py-3 text-sm text-slate-300">
            {permissionState === 'denied'
              ? 'Sensor permission was denied.'
              : 'Works best in mobile browsers with motion sensor access.'}
          </div>
        </div>
      </div>
    </Panel>
  )
}

function QrStudioSurface() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const [qrValue, setQrValue] = useState('https://hub.blissbiovn.com')
  const [scanResult, setScanResult] = useState('')
  const [scanStatus, setScanStatus] = useState('Upload an image to scan if BarcodeDetector is supported.')

  useEffect(() => {
    if (!canvasRef.current) return
    void (async () => {
      const { default: QRCode } = await import('qrcode')
      await QRCode.toCanvas(canvasRef.current, qrValue || ' ', {
        width: 220,
        margin: 2,
        color: {
          dark: '#f8fafc',
          light: '#020617',
        },
      })
    })()
  }, [qrValue])

  const handleScanFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const Detector = (window as Window & { BarcodeDetector?: new (options?: { formats?: string[] }) => { detect: (input: ImageBitmap) => Promise<Array<{ rawValue?: string }>> } }).BarcodeDetector
    if (!Detector) {
      setScanStatus('BarcodeDetector is not available in this browser yet.')
      return
    }

    const bitmap = await createImageBitmap(file)
    const detector = new Detector({ formats: ['qr_code'] })
    const detected = await detector.detect(bitmap)
    const rawValue = detected[0]?.rawValue ?? ''
    setScanResult(rawValue)
    setScanStatus(rawValue ? 'QR detected from the uploaded image.' : 'No QR code found in that image.')
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_0.9fr]">
      <Panel title="QR Forge" subtitle="Generate a shareable QR code entirely offline.">
        <FormTextArea rows={4} value={qrValue} onChange={(event) => setQrValue(event.target.value)} />
        <div className="mt-4 flex flex-col items-center gap-4 rounded-[24px] border border-white/8 bg-slate-950/80 p-4">
          <canvas ref={canvasRef} className="rounded-2xl" />
          <ActionButton
            onClick={() => {
              const dataUrl = canvasRef.current?.toDataURL('image/png')
              if (!dataUrl) return
              const link = document.createElement('a')
              link.href = dataUrl
              link.download = 'purehub-qr.png'
              link.click()
            }}
          >
            Download PNG
          </ActionButton>
        </div>
      </Panel>

      <Panel title="QR Scan" subtitle="Uses the browser BarcodeDetector when available.">
        <label className="block">
          <span className="mb-2 block text-sm text-slate-300">Scan from image</span>
          <FormInput type="file" accept="image/*" onChange={handleScanFile} />
        </label>
        <div className="mt-4 rounded-[24px] border border-white/8 bg-white/5 p-4">
          <p className="text-sm text-slate-400">{scanStatus}</p>
          {scanResult ? (
            <div className="mt-3 rounded-2xl border border-emerald-300/20 bg-emerald-400/10 p-4 text-sm text-emerald-100">
              {scanResult}
            </div>
          ) : null}
        </div>
      </Panel>
    </div>
  )
}

function ExpenseTrackerSurface() {
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
            <div className="rounded-[24px] border border-dashed border-white/10 bg-white/4 p-6 text-sm text-slate-400">
              No expenses yet. Add your first line item to start the offline ledger.
            </div>
          ) : (
            records.map((record) => (
              <div key={record.id} className="rounded-[24px] border border-white/8 bg-white/5 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-white">{record.title}</p>
                    <p className="mt-1 text-sm text-slate-400">
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
                {record.note ? <p className="mt-3 text-sm text-slate-300">{record.note}</p> : null}
              </div>
            ))
          )}
        </div>
      </Panel>
    </div>
  )
}

function BillSplitterSurface() {
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
            <div key={person} className="flex items-center justify-between rounded-[22px] border border-white/8 bg-white/5 px-4 py-3">
              <span className="text-sm text-white">{person}</span>
              <span className="font-semibold text-emerald-300">{formatCurrency(perPerson)}</span>
            </div>
          ))}
          {people.length === 0 ? (
            <div className="rounded-[24px] border border-dashed border-white/10 bg-white/4 p-6 text-sm text-slate-400">
              Add at least one person to see the split.
            </div>
          ) : null}
        </div>
      </Panel>
    </div>
  )
}

function ZenHabitSurface() {
  const [habits, setHabits] = useState<HabitRecord[]>([])
  const [checkIns, setCheckIns] = useState<HabitCheckInRecord[]>([])
  const [name, setName] = useState('')

  const load = async () => {
    const [nextHabits, nextCheckIns] = await Promise.all([
      habitRepository.list(),
      habitRepository.list().then(async (items) => {
        const grouped = await Promise.all(items.map((item) => habitCheckInRepository.listByHabit(item.id)))
        return grouped.flat()
      }),
    ])
    setHabits(nextHabits.filter((item) => !item.archivedAt))
    setCheckIns(nextCheckIns)
  }

  useEffect(() => {
    void load()
  }, [])

  const today = new Date().toISOString().slice(0, 10)

  return (
    <Panel title="Zen Habit" subtitle="Track simple streaks offline and keep your rhythm local.">
      <div className="flex flex-col gap-3 sm:flex-row">
        <FormInput value={name} onChange={(event) => setName(event.target.value)} placeholder="New habit name" />
        <ActionButton
          onClick={async () => {
            if (!name.trim()) return
            await habitRepository.put({
              id: createId(),
              name: name.trim(),
              colorHex: '#34d399',
              createdAt: new Date().toISOString(),
            })
            setName('')
            await load()
          }}
        >
          Add habit
        </ActionButton>
      </div>
      <div className="mt-4 space-y-3">
        {habits.map((habit) => {
          const habitCheckIns = checkIns.filter((item) => item.habitId === habit.id)
          const isDoneToday = habitCheckIns.some((item) => item.completedOn === today)
          const streak = calculateStreak(habitCheckIns.map((item) => item.completedOn))
          return (
            <div key={habit.id} className="rounded-[24px] border border-white/8 bg-white/5 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-white">{habit.name}</p>
                  <p className="mt-1 text-sm text-slate-400">Current streak: {streak} days</p>
                </div>
                <ActionButton
                  tone={isDoneToday ? 'muted' : 'primary'}
                  onClick={async () => {
                    const existing = habitCheckIns.find((item) => item.completedOn === today)
                    if (existing) {
                      await habitCheckInRepository.remove(existing.id)
                    } else {
                      await habitCheckInRepository.upsert({
                        id: createId(),
                        habitId: habit.id,
                        completedOn: today,
                        createdAt: new Date().toISOString(),
                      })
                    }
                    await load()
                  }}
                >
                  {isDoneToday ? 'Undo today' : 'Complete today'}
                </ActionButton>
              </div>
            </div>
          )
        })}
      </div>
    </Panel>
  )
}

function ZenPomodoroSurface() {
  const [minutes, setMinutes] = useState(25)
  const [remaining, setRemaining] = useState(minutes * 60)
  const [running, setRunning] = useState(false)

  useEffect(() => {
    setRemaining(minutes * 60)
  }, [minutes])

  useEffect(() => {
    if (!running) return
    const timer = window.setInterval(() => {
      setRemaining((value) => {
        if (value <= 1) {
          window.clearInterval(timer)
          setRunning(false)
          return 0
        }
        return value - 1
      })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [running])

  return (
    <Panel title="Zen Pomodoro" subtitle="A clean focus loop that runs fully offline.">
      <div className="flex flex-wrap gap-2">
        {[15, 25, 50].map((preset) => (
          <ActionButton key={preset} tone={minutes === preset ? 'primary' : 'muted'} onClick={() => setMinutes(preset)}>
            {preset} min
          </ActionButton>
        ))}
      </div>
      <div className="mt-6 text-center">
        <p className="text-6xl font-semibold text-white">{formatDuration(remaining)}</p>
        <div className="mt-5 flex justify-center gap-2">
          <ActionButton onClick={() => setRunning((value) => !value)}>{running ? 'Pause' : 'Start'}</ActionButton>
          <ActionButton tone="muted" onClick={() => { setRunning(false); setRemaining(minutes * 60) }}>Reset</ActionButton>
        </div>
      </div>
    </Panel>
  )
}

function ZenBreathSurface() {
  const [phaseIndex, setPhaseIndex] = useState(0)
  const phases = [
    { label: 'Inhale', duration: 4000, scale: 1 },
    { label: 'Hold', duration: 2500, scale: 1.15 },
    { label: 'Exhale', duration: 4500, scale: 0.78 },
    { label: 'Rest', duration: 2000, scale: 0.72 },
  ]

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPhaseIndex((value) => (value + 1) % phases.length)
    }, phases[phaseIndex].duration)
    return () => window.clearTimeout(timer)
  }, [phaseIndex, phases])

  const phase = phases[phaseIndex]

  return (
    <Panel title="Zen Breath" subtitle="A gentle breathing ritual with a large animated focus orb.">
      <div className="flex flex-col items-center gap-6 py-4">
        <div className="relative flex size-64 items-center justify-center">
          <div
            className="absolute rounded-full bg-emerald-400/15 shadow-[0_0_80px_rgba(52,211,153,0.35)] transition-transform duration-[4000ms]"
            style={{ width: '12rem', height: '12rem', transform: `scale(${phase.scale})` }}
          />
          <div className="relative rounded-full border border-white/10 bg-slate-950/80 px-8 py-6 text-center">
            <p className="text-sm uppercase tracking-[0.3em] text-emerald-300">{phase.label}</p>
            <p className="mt-2 text-sm text-slate-400">{Math.round(phase.duration / 1000)} sec</p>
          </div>
        </div>
      </div>
    </Panel>
  )
}

function BubbleLevelSurface() {
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

  return (
    <Panel title="Bubble Level" subtitle="Quick 2D balance feedback using device orientation.">
      <div className="mx-auto flex max-w-sm flex-col items-center gap-4">
        <div className="relative aspect-square w-full rounded-[32px] border border-white/10 bg-white/5">
          <div className="absolute inset-[10%] rounded-[26px] border border-dashed border-white/10" />
          <div
            className="absolute size-12 -translate-x-1/2 -translate-y-1/2 rounded-full border border-cyan-200/20 bg-cyan-300/85 shadow-[0_0_35px_rgba(103,232,249,0.45)] transition-all duration-150"
            style={{ left: `${left}%`, top: `${top}%` }}
          />
        </div>
        <p className="text-sm text-slate-400">
          Beta {tilt.beta.toFixed(1)}° · Gamma {tilt.gamma.toFixed(1)}°
        </p>
      </div>
    </Panel>
  )
}

function DecibelMeterSurface() {
  const [running, setRunning] = useState(false)
  const [level, setLevel] = useState(0)
  const analyserRef = useRef<AnalyserNode | null>(null)
  const audioContextRef = useRef<AudioContext | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  useEffect(() => {
    if (!running) return

    let animationFrame = 0

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

  return (
    <Panel title="Decibel Meter" subtitle="Local microphone analysis with no uploads and no ads.">
      <div className="space-y-4">
        <div className="h-6 overflow-hidden rounded-full border border-white/10 bg-white/5">
          <div className="h-full rounded-full bg-gradient-to-r from-emerald-300 via-amber-300 to-rose-300 transition-all" style={{ width: `${level}%` }} />
        </div>
        <div className="flex items-center justify-between">
          <p className="text-3xl font-semibold text-white">{level} dB</p>
          <ActionButton onClick={() => setRunning((value) => !value)}>{running ? 'Stop mic' : 'Start mic'}</ActionButton>
        </div>
      </div>
    </Panel>
  )
}

function UnitConverterSurface() {
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
            className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none"
          >
            <option value="length">Length</option>
            <option value="weight">Weight</option>
            <option value="temperature">Temperature</option>
          </select>
          <FormInput value={value} onChange={(event) => setValue(event.target.value)} inputMode="decimal" />
        </div>
        <div className="space-y-3">
          {conversions.map(([label, converted]) => (
            <div key={label} className="flex items-center justify-between rounded-[22px] border border-white/8 bg-white/5 px-4 py-3">
              <span className="text-sm text-slate-300">{label}</span>
              <span className="font-semibold text-white">{Number(converted).toFixed(3)}</span>
            </div>
          ))}
        </div>
      </div>
    </Panel>
  )
}

function DocToPdfSurface() {
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
        {images.map((image) => (
          <img key={image} src={image} alt="Document page" className="rounded-[24px] border border-white/8 bg-white/5 object-cover" />
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

function OcrTextSurface() {
  const [ocrText, setOcrText] = useState('')
  const [running, setRunning] = useState(false)

  const handleFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    setRunning(true)
    const { createWorker } = await import('tesseract.js')
    const worker = await createWorker('eng')
    const result = await worker.recognize(file)
    setOcrText(result.data.text)
    await worker.terminate()
    setRunning(false)
  }

  return (
    <Panel title="OCR Text" subtitle="Runs OCR in the browser so images never leave the device.">
      <FormInput type="file" accept="image/*" onChange={handleFile} />
      <div className="mt-4 rounded-[24px] border border-white/8 bg-white/5 p-4">
        <p className="text-sm text-slate-400">{running ? 'Recognizing text...' : 'Extracted text appears here.'}</p>
        {ocrText ? <pre className="mt-3 whitespace-pre-wrap text-sm text-white">{ocrText}</pre> : null}
      </div>
    </Panel>
  )
}

function ColorGrabberSurface() {
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
        <div className="overflow-hidden rounded-[24px] border border-white/8 bg-white/5 p-3">
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
        <div className="rounded-[24px] border border-white/8 bg-white/5 p-4">
          <div className="h-24 rounded-2xl border border-white/10" style={{ backgroundColor: pickedColor }} />
          <p className="mt-4 text-sm text-slate-400">HEX</p>
          <p className="mt-1 text-lg font-semibold text-white">{pickedColor}</p>
        </div>
      </div>
    </Panel>
  )
}

function SpeakerCleanerSurface() {
  const [playing, setPlaying] = useState(false)
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
    oscillator.frequency.value = 165
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
  }, [playing])

  return (
    <Panel title="Speaker Cleaner" subtitle="Local 165Hz tone generator to help clear water and dust from speakers.">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm text-slate-300">Frequency</p>
          <p className="mt-1 text-3xl font-semibold text-white">165 Hz</p>
        </div>
        <ActionButton onClick={() => setPlaying((value) => !value)}>{playing ? 'Stop tone' : 'Start tone'}</ActionButton>
      </div>
    </Panel>
  )
}

function PasswordVaultSurface() {
  const storageKey = 'purehub-vault-items'
  const [passphrase, setPassphrase] = useState('')
  const [label, setLabel] = useState('')
  const [secret, setSecret] = useState('')
  const [items, setItems] = useState<Array<{ id: string; label: string; payload: string; iv: string }>>([])
  const [preview, setPreview] = useState<string>('')

  useEffect(() => {
    const raw = window.localStorage.getItem(storageKey)
    if (raw) {
      setItems(JSON.parse(raw) as Array<{ id: string; label: string; payload: string; iv: string }>)
    }
  }, [])

  const persist = (nextItems: Array<{ id: string; label: string; payload: string; iv: string }>) => {
    setItems(nextItems)
    window.localStorage.setItem(storageKey, JSON.stringify(nextItems))
  }

  return (
    <Panel title="Password Vault" subtitle="Secrets are encrypted with your passphrase before being stored locally.">
      <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-3">
          <FormInput type="password" value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="Master passphrase" />
          <FormInput value={label} onChange={(event) => setLabel(event.target.value)} placeholder="Entry label" />
          <FormInput type="password" value={secret} onChange={(event) => setSecret(event.target.value)} placeholder="Secret value" />
          <ActionButton
            disabled={!passphrase || !label || !secret}
            onClick={async () => {
              const encrypted = await encryptSecret(secret, passphrase)
              persist([
                {
                  id: createId(),
                  label: label.trim(),
                  payload: encrypted.payload,
                  iv: encrypted.iv,
                },
                ...items,
              ])
              setLabel('')
              setSecret('')
            }}
          >
            Save encrypted entry
          </ActionButton>
        </div>

        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="rounded-[22px] border border-white/8 bg-white/5 p-4">
              <div className="flex items-center justify-between gap-3">
                <p className="font-semibold text-white">{item.label}</p>
                <button
                  type="button"
                  className="text-xs text-slate-400 transition hover:text-white"
                  onClick={async () => {
                    if (!passphrase) return
                    try {
                      const decrypted = await decryptSecret(item.payload, item.iv, passphrase)
                      setPreview(`${item.label}: ${decrypted}`)
                    } catch {
                      setPreview('Failed to decrypt. Check your passphrase.')
                    }
                  }}
                >
                  Decrypt
                </button>
              </div>
            </div>
          ))}
          {preview ? <div className="rounded-[22px] border border-emerald-300/20 bg-emerald-400/10 p-4 text-sm text-emerald-100">{preview}</div> : null}
        </div>
      </div>
    </Panel>
  )
}

function DecisionWheelSurface() {
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

  return (
    <div className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
      <Panel title="Decision Wheel" subtitle="Give fate a spin with local options and a game-like result reveal.">
        <FormTextArea rows={8} value={optionsText} onChange={(event) => setOptionsText(event.target.value)} />
        <div className="mt-4 flex items-center gap-3">
          <ActionButton
            disabled={options.length === 0}
            onClick={() => {
              const winnerIndex = Math.floor(Math.random() * options.length)
              const spin = 1800 + Math.random() * 900
              setRotation((value) => value + spin)
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
              className="flex size-72 items-center justify-center rounded-full border border-white/10 bg-[conic-gradient(from_90deg,#34d399,#22d3ee,#a855f7,#f59e0b,#34d399)] transition-transform duration-[2200ms] ease-out"
              style={{ transform: `rotate(${rotation}deg)` }}
            >
              <div className="flex size-24 items-center justify-center rounded-full border border-white/10 bg-slate-950/90 text-center text-sm text-white">
                {result || 'Spin'}
              </div>
            </div>
          </div>
          <div className="grid w-full gap-2 sm:grid-cols-2">
            {options.map((option) => (
              <div key={option} className="rounded-[20px] border border-white/8 bg-white/5 px-4 py-3 text-sm text-slate-300">
                {option}
              </div>
            ))}
          </div>
        </div>
      </Panel>
    </div>
  )
}

function CommunityUnlockSurface() {
  const storageKey = 'purehub-pro-code'
  const [code, setCode] = useState(() => window.localStorage.getItem(storageKey) ?? '')
  const active = code.trim().length > 0

  return (
    <Panel title="Community Pro Unlock" subtitle="Keep the growth engine lightweight and local.">
      <div className="space-y-4">
        <ActionButton
          onClick={() => window.open('https://t.me/aaa_letan_vip_bot?start=getcode', '_blank', 'noopener,noreferrer')}
        >
          Join community to get Pro code
        </ActionButton>
        <FormInput
          value={code}
          onChange={(event) => {
            setCode(event.target.value)
            window.localStorage.setItem(storageKey, event.target.value)
          }}
          placeholder="Enter your Pro code"
        />
        <div className={`rounded-[22px] border px-4 py-3 text-sm ${active ? 'border-emerald-300/25 bg-emerald-400/10 text-emerald-100' : 'border-white/8 bg-white/5 text-slate-400'}`}>
          {active ? 'Pro code saved on this device.' : 'No Pro code saved yet.'}
        </div>
      </div>
    </Panel>
  )
}

function createId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value || 0)
}

function formatDuration(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function calculateStreak(days: string[]) {
  const sorted = [...new Set(days)].sort().reverse()
  let streak = 0
  let cursor = new Date()

  for (const entry of sorted) {
    const currentDay = cursor.toISOString().slice(0, 10)
    if (entry === currentDay) {
      streak += 1
      cursor.setDate(cursor.getDate() - 1)
      continue
    }
    if (streak === 0) {
      cursor.setDate(cursor.getDate() - 1)
      if (entry === cursor.toISOString().slice(0, 10)) {
        streak += 1
        cursor.setDate(cursor.getDate() - 1)
        continue
      }
    }
    break
  }

  return streak
}

async function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

async function deriveCryptoKey(passphrase: string) {
  const encoder = new TextEncoder()
  const baseKey = await crypto.subtle.importKey('raw', encoder.encode(passphrase), 'PBKDF2', false, ['deriveKey'])
  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: encoder.encode('purehub-vault-salt'),
      iterations: 120000,
      hash: 'SHA-256',
    },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  )
}

async function encryptSecret(secret: string, passphrase: string) {
  const key = await deriveCryptoKey(passphrase)
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    new TextEncoder().encode(secret),
  )
  return {
    iv: arrayBufferToBase64(iv.buffer),
    payload: arrayBufferToBase64(encrypted),
  }
}

async function decryptSecret(payload: string, iv: string, passphrase: string) {
  const key = await deriveCryptoKey(passphrase)
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: new Uint8Array(base64ToArrayBuffer(iv)) },
    key,
    base64ToArrayBuffer(payload),
  )
  return new TextDecoder().decode(decrypted)
}

function arrayBufferToBase64(buffer: ArrayBuffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
}

function base64ToArrayBuffer(base64: string) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes.buffer
}

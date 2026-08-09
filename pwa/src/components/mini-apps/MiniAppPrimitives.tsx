import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { Ban, LockKeyhole, Sparkles, WifiOff } from 'lucide-react'

type PanelProps = {
  title: string
  subtitle?: string
  children: ReactNode
}

type FlagshipHeroProps = {
  eyebrow: string
  title: string
  description: string
  children?: ReactNode
  accent?: 'emerald' | 'violet' | 'amber' | 'sky'
}

const heroAccent = {
  emerald: 'from-emerald-100 via-white to-sky-100 text-emerald-800 dark:from-emerald-950/55 dark:via-slate-900 dark:to-sky-950/40 dark:text-emerald-200',
  violet: 'from-violet-100 via-white to-fuchsia-100 text-violet-800 dark:from-violet-950/55 dark:via-slate-900 dark:to-fuchsia-950/40 dark:text-violet-200',
  amber: 'from-amber-100 via-white to-orange-100 text-amber-900 dark:from-amber-950/55 dark:via-slate-900 dark:to-orange-950/40 dark:text-amber-200',
  sky: 'from-sky-100 via-white to-cyan-100 text-sky-900 dark:from-sky-950/55 dark:via-slate-900 dark:to-cyan-950/40 dark:text-sky-200',
}

export function FlagshipHero({ eyebrow, title, description, children, accent = 'emerald' }: FlagshipHeroProps) {
  return <section className={`overflow-hidden rounded-[20px] border border-slate-200/80 bg-gradient-to-br p-4 shadow-sm dark:border-slate-700/70 ${heroAccent[accent]}`}>
    <p className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[.2em]"><Sparkles className="size-4" />{eyebrow}</p>
    <div className="mt-2 flex flex-col justify-between gap-3 lg:flex-row lg:items-end">
      <div><h1 className="text-xl font-black tracking-tight text-slate-950 sm:text-2xl dark:text-white">{title}</h1><p className="mt-1 max-w-2xl text-sm leading-5 text-slate-600 dark:text-slate-300">{description}</p></div>
      <div className="flex gap-1.5 text-[11px] font-bold text-slate-600 dark:text-slate-300" aria-label="Offline, private, and ad-free">
        <span title="Offline ready" className="grid size-8 place-items-center rounded-full bg-white/75 dark:bg-slate-950/45"><WifiOff className="size-3.5" /><span className="sr-only">Offline ready</span></span>
        <span title="Private" className="grid size-8 place-items-center rounded-full bg-white/75 dark:bg-slate-950/45"><LockKeyhole className="size-3.5" /><span className="sr-only">Private</span></span>
        <span title="No ads" className="grid size-8 place-items-center rounded-full bg-white/75 dark:bg-slate-950/45"><Ban className="size-3.5" /><span className="sr-only">No ads</span></span>
      </div>
    </div>
    {children ? <div className="mt-3">{children}</div> : null}
  </section>
}

export function Panel({ title, subtitle, children }: PanelProps) {
  return (
    <section className="app-surface overflow-hidden rounded-[16px] border border-slate-200/80 bg-white shadow-sm dark:border-slate-700/70 dark:bg-slate-900">
      <div className="border-b border-slate-200/70 bg-gradient-to-r from-emerald-50 via-white to-sky-50 px-3.5 py-3 dark:border-slate-700/70 dark:from-emerald-950/35 dark:via-slate-900 dark:to-sky-950/25">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-base font-bold tracking-tight text-slate-950 dark:text-white">{title}</h2>
            {subtitle ? <p className="mt-0.5 max-w-2xl text-xs leading-5 text-slate-600 dark:text-slate-300">{subtitle}</p> : null}
          </div>
          <span title="Private by design" className="grid size-8 shrink-0 place-items-center rounded-full border border-emerald-200 bg-white/80 text-emerald-800 dark:border-emerald-800 dark:bg-slate-900/70 dark:text-emerald-200"><LockKeyhole className="size-3.5" aria-hidden="true" /><span className="sr-only">Private by design</span></span>
        </div>
      </div>
      <div className="p-3.5">{children}</div>
    </section>
  )
}

export function FormInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={[
        'min-h-11 w-full rounded-[12px] border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-medium text-slate-950 outline-none transition placeholder:font-normal placeholder:text-slate-400 hover:border-slate-400 focus:border-emerald-600 focus:ring-3 focus:ring-emerald-500/15 dark:border-slate-600 dark:bg-slate-950 dark:text-white dark:placeholder:text-slate-500',
        props.className,
      ].filter(Boolean).join(' ')}
    />
  )
}

export function FormTextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      {...props}
      className={[
        'w-full rounded-[12px] border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-medium text-slate-950 outline-none transition placeholder:font-normal placeholder:text-slate-400 hover:border-slate-400 focus:border-emerald-600 focus:ring-3 focus:ring-emerald-500/15 dark:border-slate-600 dark:bg-slate-950 dark:text-white dark:placeholder:text-slate-500',
        props.className,
      ].filter(Boolean).join(' ')}
    />
  )
}

export function ActionButton(props: ButtonHTMLAttributes<HTMLButtonElement> & { tone?: 'primary' | 'muted' | 'danger' }) {
  const tone = props.tone ?? 'primary'
  return (
    <button
      {...props}
      className={[
        'min-h-10 rounded-[11px] px-3.5 py-2 text-sm font-bold shadow-sm transition duration-150 focus-visible:outline-none focus-visible:ring-3 active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50',
        tone === 'primary' && 'bg-emerald-700 text-white hover:bg-emerald-800 focus-visible:ring-emerald-500/30 dark:bg-emerald-500 dark:text-slate-950 dark:hover:bg-emerald-400',
        tone === 'muted' && 'border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 focus-visible:ring-slate-400/25 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700',
        tone === 'danger' && 'border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 focus-visible:ring-rose-400/25 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200',
        props.className,
      ].filter(Boolean).join(' ')}
    />
  )
}

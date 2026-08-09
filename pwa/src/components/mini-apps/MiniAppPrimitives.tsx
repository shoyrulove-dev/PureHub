import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { ShieldCheck, Sparkles, Smartphone } from 'lucide-react'

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
  return <section className={`overflow-hidden rounded-[24px] border border-slate-200/80 bg-gradient-to-br p-5 shadow-sm sm:p-6 dark:border-slate-700/70 ${heroAccent[accent]}`}>
    <p className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[.2em]"><Sparkles className="size-4" />{eyebrow}</p>
    <div className="mt-3 flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
      <div><h1 className="text-2xl font-black tracking-tight text-slate-950 sm:text-3xl dark:text-white">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">{description}</p></div>
      <div className="flex flex-wrap gap-2 text-[11px] font-bold text-slate-600 dark:text-slate-300"><span className="rounded-full bg-white/75 px-3 py-1.5 dark:bg-slate-950/45">Offline ready</span><span className="rounded-full bg-white/75 px-3 py-1.5 dark:bg-slate-950/45">Private</span><span className="rounded-full bg-white/75 px-3 py-1.5 dark:bg-slate-950/45">No ads</span></div>
    </div>
    {children ? <div className="mt-5">{children}</div> : null}
  </section>
}

export function Panel({ title, subtitle, children }: PanelProps) {
  return (
    <section className="app-surface overflow-hidden rounded-[18px] border border-slate-200/80 bg-white shadow-sm dark:border-slate-700/70 dark:bg-slate-900">
      <div className="border-b border-slate-200/70 bg-gradient-to-r from-emerald-50 via-white to-sky-50 px-4 py-4 sm:px-5 dark:border-slate-700/70 dark:from-emerald-950/35 dark:via-slate-900 dark:to-sky-950/25">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-lg font-bold tracking-tight text-slate-950 dark:text-white">{title}</h2>
            {subtitle ? <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">{subtitle}</p> : null}
          </div>
          <div className="hidden shrink-0 items-center gap-1.5 rounded-full border border-emerald-200 bg-white/80 px-2.5 py-1 text-[11px] font-bold text-emerald-800 sm:flex dark:border-emerald-800 dark:bg-slate-900/70 dark:text-emerald-200">
            <ShieldCheck className="size-3.5" aria-hidden="true" /> No ads
          </div>
        </div>
        <div className="mt-3 flex items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
          <Smartphone className="size-3.5" aria-hidden="true" /> Designed for quick, one-handed use
        </div>
      </div>
      <div className="p-4 sm:p-5">{children}</div>
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
        'min-h-11 rounded-[12px] px-4 py-2.5 text-sm font-bold shadow-sm transition duration-150 focus-visible:outline-none focus-visible:ring-3 active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50',
        tone === 'primary' && 'bg-emerald-700 text-white hover:bg-emerald-800 focus-visible:ring-emerald-500/30 dark:bg-emerald-500 dark:text-slate-950 dark:hover:bg-emerald-400',
        tone === 'muted' && 'border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 focus-visible:ring-slate-400/25 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700',
        tone === 'danger' && 'border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 focus-visible:ring-rose-400/25 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200',
        props.className,
      ].filter(Boolean).join(' ')}
    />
  )
}

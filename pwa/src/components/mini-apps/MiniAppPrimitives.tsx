import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { ShieldCheck, Smartphone } from 'lucide-react'

type PanelProps = {
  title: string
  subtitle?: string
  children: ReactNode
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

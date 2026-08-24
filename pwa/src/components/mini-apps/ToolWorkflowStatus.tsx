import { CheckCircle2, CircleDot, LockKeyhole, Sparkles } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { MiniAppId } from '../../features/catalog/tabs'

export function ToolWorkflowStatus({ miniAppId }: { miniAppId: MiniAppId }) {
  const [completed, setCompleted] = useState(
    () => window.localStorage.getItem(`purehub-completed-${miniAppId}`) === 'true',
  )

  useEffect(() => {
    setCompleted(window.localStorage.getItem(`purehub-completed-${miniAppId}`) === 'true')
    const onComplete = (event: Event) => {
      const detail = (event as CustomEvent<{ miniAppId: MiniAppId }>).detail
      if (detail?.miniAppId === miniAppId) setCompleted(true)
    }
    window.addEventListener('purehub:product-complete', onComplete)
    return () => window.removeEventListener('purehub:product-complete', onComplete)
  }, [miniAppId])

  const steps = [
    { label: 'Ready', detail: 'Choose input', icon: CircleDot, active: !completed },
    { label: 'Private work', detail: 'On this device', icon: LockKeyhole, active: false },
    { label: 'Result', detail: completed ? 'Ready to use' : 'After success', icon: completed ? CheckCircle2 : Sparkles, active: completed },
  ]

  return (
    <div className="grid grid-cols-3 gap-2" aria-label={completed ? 'Workflow complete' : 'Workflow ready'}>
      {steps.map(({ label, detail, icon: Icon, active }) => (
        <div key={label} className={`rounded-[13px] border px-2.5 py-2 ${active ? 'border-emerald-300 bg-emerald-50 text-emerald-950 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100' : 'border-slate-200 bg-white text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'}`}>
          <span className="flex items-center gap-1.5 text-xs font-black"><Icon className="size-3.5" />{label}</span>
          <span className="mt-0.5 block truncate text-[11px] opacity-75">{detail}</span>
        </div>
      ))}
    </div>
  )
}

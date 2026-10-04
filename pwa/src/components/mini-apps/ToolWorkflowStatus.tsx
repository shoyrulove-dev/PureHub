import { CheckCircle2, CircleDot, LockKeyhole, Sparkles } from 'lucide-react'
import { useCallback, useSyncExternalStore } from 'react'
import type { MiniAppId } from '../../features/catalog/tabs'
import { normalizeLocale } from '../../i18n/locales'
import { getProductCopy } from '../../i18n/product-copy'

export function ToolWorkflowStatus({ miniAppId }: { miniAppId: MiniAppId }) {
  const copy = getProductCopy(normalizeLocale(window.location.pathname.split('/')[1])).workflow
  const subscribe = useCallback((onStoreChange: () => void) => {
    const onComplete = (event: Event) => {
      const detail = (event as CustomEvent<{ miniAppId: MiniAppId }>).detail
      if (detail?.miniAppId === miniAppId) onStoreChange()
    }
    const onStorage = (event: StorageEvent) => {
      if (event.key === `purehub-completed-${miniAppId}`) onStoreChange()
    }
    window.addEventListener('purehub:product-complete', onComplete)
    window.addEventListener('storage', onStorage)
    return () => {
      window.removeEventListener('purehub:product-complete', onComplete)
      window.removeEventListener('storage', onStorage)
    }
  }, [miniAppId])
  const getSnapshot = useCallback(
    () => window.localStorage.getItem(`purehub-completed-${miniAppId}`) === 'true',
    [miniAppId],
  )
  const completed = useSyncExternalStore(subscribe, getSnapshot, () => false)

  const steps = [
    { label: copy.ready, detail: copy.chooseInput, icon: CircleDot, active: !completed },
    { label: copy.privateWork, detail: copy.onDevice, icon: LockKeyhole, active: false },
    { label: copy.result, detail: completed ? copy.readyToUse : copy.afterSuccess, icon: completed ? CheckCircle2 : Sparkles, active: completed },
  ]

  return (
    <div className="grid grid-cols-3 gap-2" aria-label={completed ? copy.complete : copy.waiting}>
      {steps.map(({ label, detail, icon: Icon, active }) => (
        <div key={label} className={`rounded-[13px] border px-2.5 py-2 ${active ? 'border-emerald-300 bg-emerald-50 text-emerald-950 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100' : 'border-slate-200 bg-white text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'}`}>
          <span className="flex items-center gap-1.5 text-xs font-black"><Icon className="size-3.5" />{label}</span>
          <span className="mt-0.5 block truncate text-[11px] opacity-75">{detail}</span>
        </div>
      ))}
    </div>
  )
}

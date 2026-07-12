import { useTranslation } from 'react-i18next'
import type { TabDefinition } from '../features/catalog/tabs'
import { MINI_APP_ITEMS } from '../features/catalog/tabs'

type TabLandingPageProps = {
  tab: TabDefinition
}

export function TabLandingPage({ tab }: TabLandingPageProps) {
  const { t } = useTranslation()
  const miniApps = MINI_APP_ITEMS.filter((item) => item.tabId === tab.id)

  return (
    <section className="space-y-4">
      <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.45)]">
        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-600">
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-slate-950">
          {t(tab.labelKey)}
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">{t(tab.descriptionKey)}</p>
      </div>

      <div className="rounded-[28px] border border-slate-200 bg-white p-5">
        <p className="text-sm font-medium text-slate-900">{t('app.miniAppMap')}</p>
        <ul className="mt-4 grid gap-3">
          {miniApps.map((miniApp, index) => (
            <li
              key={miniApp.id}
              className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3"
            >
              <span className="text-sm font-medium text-slate-800">{t(miniApp.titleKey)}</span>
              <span className="text-xs text-slate-400">#{index + 1}</span>
            </li>
          ))}
        </ul>
      </div>

      <div className="rounded-[28px] border border-dashed border-slate-300 bg-slate-100/80 p-5 text-sm leading-6 text-slate-500">
        {t('app.phaseDeferred')}
      </div>
    </section>
  )
}

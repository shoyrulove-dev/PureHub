import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import type { TabDefinition } from '../features/catalog/tabs'
import { MINI_APP_ITEMS } from '../features/catalog/tabs'
import { buildMiniAppPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'
import { Dashboard } from '../components/dashboard/Dashboard'

type TabLandingPageProps = {
  tab: TabDefinition
}

export function TabLandingPage({ tab }: TabLandingPageProps) {
  const { t } = useTranslation()
  const { locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)
  const miniApps = MINI_APP_ITEMS.filter((item) => item.tabId === tab.id)

  if (tab.id === 'zen-time') {
    return <Dashboard />
  }

  return (
    <section className="space-y-4">
      <div
        className={`rounded-[30px] border border-white/8 bg-gradient-to-br ${tab.accentSurfaceClass} p-5 shadow-[0_24px_70px_-48px_rgba(15,23,42,0.45)] sm:p-6`}
      >
        <p className={`text-xs font-semibold uppercase tracking-[0.28em] ${tab.accentClass}`}>
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-white sm:text-3xl">
          {t(tab.labelKey)}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300 sm:text-[15px]">
          {t(tab.descriptionKey)}
        </p>
      </div>

      <div className="rounded-[30px] border border-white/8 bg-slate-950/86 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.35)] sm:p-6">
        <p className="text-sm font-medium text-slate-200">{t('app.miniAppMap')}</p>
        <ul className="mt-4 grid gap-3 sm:grid-cols-2">
          {miniApps.map((miniApp, index) => (
            <li key={miniApp.id}>
              <Link
                to={buildMiniAppPath(normalizedLocale, miniApp.id)}
                className="group flex h-full items-start gap-4 rounded-[24px] border border-white/8 bg-white/5 px-4 py-4 transition hover:-translate-y-0.5 hover:border-white/16 hover:bg-white/7 hover:shadow-[0_20px_50px_-34px_rgba(15,23,42,0.45)] focus:outline-none focus:ring-2 focus:ring-emerald-300/50"
              >
                <div className={`flex size-12 shrink-0 items-center justify-center rounded-2xl border border-white/8 bg-slate-950/70 ${tab.accentClass} shadow-sm`}>
                  <miniApp.icon className="size-5" strokeWidth={2.2} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-sm font-semibold text-white sm:text-[15px]">
                      {t(miniApp.titleKey)}
                    </span>
                    <span className="text-xs text-slate-500">#{index + 1}</span>
                  </div>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{t(miniApp.summaryKey)}</p>
                  <span className={`mt-3 inline-flex text-xs font-semibold ${tab.accentClass}`}>
                    {t('app.openMiniApp')}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </div>

      <div className="rounded-[28px] border border-dashed border-white/10 bg-white/4 p-5 text-sm leading-6 text-slate-400">
        {t('app.phaseDeferred')}
      </div>
    </section>
  )
}

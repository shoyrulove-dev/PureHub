import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import type { TabDefinition } from '../features/catalog/tabs'
import { MINI_APP_ITEMS } from '../features/catalog/tabs'
import { buildMiniAppPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'

type TabLandingPageProps = {
  tab: TabDefinition
}

export function TabLandingPage({ tab }: TabLandingPageProps) {
  const { t } = useTranslation()
  const { lang } = useParams()
  const normalizedLocale = normalizeLocale(lang)
  const miniApps = MINI_APP_ITEMS.filter((item) => item.tabId === tab.id)

  return (
    <section className="space-y-7">
      <div>
        <p className={`eyebrow ${tab.accentClass}`}>
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-white">
          {t(tab.labelKey)}
        </h1>
        <p className="mt-2 max-w-2xl leading-7 text-slate-600 dark:text-slate-400">
          {t(tab.descriptionKey)}
        </p>
      </div>

      <div>
        <p className="text-sm font-semibold text-slate-500">{t('app.miniAppMap')}</p>
        <ul className="mt-3 grid gap-3 sm:grid-cols-2">
          {miniApps.map((miniApp) => (
            <li key={miniApp.id} className="h-full">
              <Link
                to={buildMiniAppPath(normalizedLocale, miniApp.id)}
                className="tool-card tool-card__link group h-full"
              >
                <div className={`tool-card__icon ${tab.accentClass}`}>
                  <miniApp.icon className="size-5" strokeWidth={2.2} />
                </div>
                <div className="min-w-0 flex-1">
                  <span className="font-semibold text-slate-900 dark:text-white">{t(miniApp.titleKey)}</span>
                  <p className="mt-1 text-sm leading-5 text-slate-500 dark:text-slate-400">{t(miniApp.summaryKey)}</p>
                  <span className={`mt-3 inline-flex text-xs font-semibold ${tab.accentClass}`}>
                    {t('app.openMiniApp')}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </div>

      <div className="rounded-[18px] border border-dashed border-slate-500/20 bg-slate-500/5 p-4 text-sm leading-6 text-slate-500 dark:text-slate-400">
        {t('app.phaseDeferred')}
      </div>
    </section>
  )
}

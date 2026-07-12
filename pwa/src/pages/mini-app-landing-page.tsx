import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import type { MiniAppDefinition, TabDefinition } from '../features/catalog/tabs'
import { buildTabPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'

type MiniAppLandingPageProps = {
  miniApp: MiniAppDefinition
  tab: TabDefinition
}

export function MiniAppLandingPage({ miniApp, tab }: MiniAppLandingPageProps) {
  const { t } = useTranslation()
  const { locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)

  return (
    <section className="space-y-4">
      <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.45)]">
        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-600">
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-slate-950">
          {t(miniApp.titleKey)}
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">{t(miniApp.summaryKey)}</p>
      </div>

      <div className="rounded-[28px] border border-slate-200 bg-white p-5">
        <p className="text-sm font-medium text-slate-900">{t('app.routeLabel')}</p>
        <p className="mt-2 break-all rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
          /{normalizedLocale}/{miniApp.slugs[normalizedLocale]}
        </p>
        <p className="mt-4 text-sm leading-6 text-slate-500">{t('app.openPlaceholder')}</p>
        <Link
          to={buildTabPath(normalizedLocale, tab.id)}
          className="mt-4 inline-flex rounded-2xl bg-slate-950 px-4 py-2 text-sm font-medium text-white"
        >
          {t(tab.labelKey)}
        </Link>
      </div>
    </section>
  )
}

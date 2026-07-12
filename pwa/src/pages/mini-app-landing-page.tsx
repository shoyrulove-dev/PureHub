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
      <div
        className={`rounded-[30px] border border-white/8 bg-gradient-to-br ${tab.accentSurfaceClass} p-5 shadow-[0_24px_70px_-48px_rgba(15,23,42,0.45)] sm:p-6`}
      >
        <div className={`flex size-14 items-center justify-center rounded-3xl border border-white/8 bg-white/5 ${tab.accentClass} shadow-sm`}>
          <miniApp.icon className="size-6" strokeWidth={2.1} />
        </div>
        <p className={`mt-4 text-xs font-semibold uppercase tracking-[0.28em] ${tab.accentClass}`}>
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-white sm:text-3xl">
          {t(miniApp.titleKey)}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300 sm:text-[15px]">
          {t(miniApp.summaryKey)}
        </p>
      </div>

      <div className="rounded-[30px] border border-white/8 bg-slate-950/86 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.35)] sm:p-6">
        <p className="text-sm font-medium text-slate-200">{t('app.routeLabel')}</p>
        <p className="mt-2 break-all rounded-2xl border border-white/8 bg-white/5 px-4 py-3 text-sm text-slate-300">
          /{normalizedLocale}/{miniApp.slugs[normalizedLocale]}
        </p>
        <p className="mt-4 text-sm leading-6 text-slate-400">{t('app.openPlaceholder')}</p>
        <Link
          to={buildTabPath(normalizedLocale, tab.id)}
          className="mt-4 inline-flex rounded-2xl bg-emerald-400/14 px-4 py-2.5 text-sm font-medium text-emerald-200 shadow-[0_20px_50px_-32px_rgba(16,185,129,0.25)] ring-1 ring-inset ring-emerald-400/15"
        >
          {t('app.browseTab')}
        </Link>
      </div>
    </section>
  )
}

import { useTranslation } from 'react-i18next'
import { Heart } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { lazy, Suspense, useEffect } from 'react'
import type { MiniAppDefinition, TabDefinition } from '../features/catalog/tabs'
import { normalizeLocale } from '../i18n/locales'
import { buildTabPath } from '../i18n/routing'
import { rememberRecentTool, useToolPreferences } from '../lib/preferences'

const MiniAppSurface = lazy(() =>
  import('../components/mini-apps/MiniAppSurface').then((module) => ({
    default: module.MiniAppSurface,
  })),
)

type MiniAppLandingPageProps = {
  miniApp: MiniAppDefinition
  tab: TabDefinition
}

export function MiniAppLandingPage({ miniApp, tab }: MiniAppLandingPageProps) {
  const { t } = useTranslation()
  const { lang } = useParams()
  const normalizedLocale = normalizeLocale(lang)
  const { favorites, toggleFavorite } = useToolPreferences()
  const favorite = favorites.includes(miniApp.id)

  useEffect(() => {
    rememberRecentTool(miniApp.id)
  }, [miniApp.id])

  return (
    <section className="space-y-5">
      <div className="flex items-start gap-4">
        <div className={`tool-card__icon size-13 ${tab.accentClass}`}>
          <miniApp.icon className="size-6" strokeWidth={2.1} />
        </div>
        <div className="min-w-0 flex-1">
          <p className={`eyebrow ${tab.accentClass}`}>{t(tab.labelKey)}</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl dark:text-white">
            {t(miniApp.titleKey)}
          </h1>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500 dark:text-slate-400">{t(miniApp.summaryKey)}</p>
        </div>
        <button type="button" className="grid size-11 shrink-0 place-items-center rounded-[14px] border border-slate-500/15" onClick={() => toggleFavorite(miniApp.id)} aria-label="Favorite tool">
          <Heart className={`size-5 ${favorite ? 'fill-rose-500 text-rose-500' : 'text-slate-400'}`} />
        </button>
      </div>

      <Suspense fallback={<div className="app-surface min-h-56 animate-pulse rounded-[18px]" aria-label="Loading tool" />}>
        <MiniAppSurface miniAppId={miniApp.id} />
      </Suspense>

      <div className="app-surface rounded-[18px] p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{t('app.routeLabel')}</p>
            <p className="mt-2 break-all rounded-[14px] bg-slate-500/8 px-4 py-3 text-sm text-slate-500 dark:text-slate-300">
              /{normalizedLocale}/{miniApp.slugs[normalizedLocale]}
            </p>
          </div>
          <Link
            to={buildTabPath(normalizedLocale, tab.id)}
            className="text-link"
          >
            {t('app.browseTab')}
          </Link>
        </div>
      </div>
    </section>
  )
}

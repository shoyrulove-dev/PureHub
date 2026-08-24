import { useTranslation } from 'react-i18next'
import { ArrowLeft, Heart, HardDrive, ShieldCheck, Sparkles, Wifi, WifiOff } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { lazy, Suspense, useEffect } from 'react'
import type { MiniAppDefinition, TabDefinition } from '../features/catalog/tabs'
import { normalizeLocale } from '../i18n/locales'
import { buildTabPath } from '../i18n/routing'
import { rememberRecentTool, useToolPreferences } from '../lib/preferences'
import { MiniAppEngagement } from '../components/mini-apps/MiniAppEngagement'
import { MiniAppErrorBoundary } from '../components/mini-apps/MiniAppErrorBoundary'
import { ToolWorkflowStatus } from '../components/mini-apps/ToolWorkflowStatus'
import { getMiniAppRuntime } from '../features/miniapps/runtime'
import { WorkspaceNavigator } from '../components/mini-apps/WorkspaceNavigator'

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
  const runtime = getMiniAppRuntime(miniApp.id)
  const capabilityLabel = miniApp.id === 'community-pro-unlock'
    ? 'Community online'
    : miniApp.id === 'ocr-text'
      ? 'Pack on demand'
      : 'Offline ready'
  const CapabilityIcon = miniApp.id === 'community-pro-unlock' || miniApp.id === 'ocr-text' ? Wifi : WifiOff

  useEffect(() => {
    rememberRecentTool(miniApp.id)
  }, [miniApp.id])

  return (
    <section className="space-y-4">
      <div className="flex items-start gap-3">
        <div className={`tool-card__icon size-11 ${tab.accentClass}`}>
          <miniApp.icon className="size-5" strokeWidth={2.2} />
        </div>
        <div className="min-w-0 flex-1">
          <p className={`eyebrow ${tab.accentClass}`}>{t(tab.labelKey)}</p>
          <h1 className="mt-0.5 text-xl font-bold tracking-tight text-slate-950 sm:text-2xl dark:text-white">
            {t(miniApp.titleKey)}
          </h1>
          <p className="mt-0.5 line-clamp-2 max-w-2xl text-sm leading-5 text-slate-500 dark:text-slate-400">{t(miniApp.summaryKey)}</p>
        </div>
        <button type="button" className="grid size-11 shrink-0 place-items-center rounded-[14px] border border-slate-500/15" onClick={() => toggleFavorite(miniApp.id)} aria-label="Favorite tool">
          <Heart className={`size-5 ${favorite ? 'fill-rose-500 text-rose-500' : 'text-slate-400'}`} />
        </button>
      </div>

      <div className="grid grid-cols-3 gap-2" aria-label="PureHub promises">
        {[
          { label: 'No ads', icon: Sparkles },
          { label: 'Private first', icon: ShieldCheck },
          { label: capabilityLabel, icon: CapabilityIcon },
        ].map(({ label, icon: Icon }) => (
          <div key={label} title={label} className="flex min-h-10 items-center justify-center gap-1.5 rounded-[11px] border border-slate-200 bg-white px-2 text-center text-xs font-bold text-slate-700 shadow-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200">
            <Icon className="size-3.5 shrink-0 text-emerald-700 dark:text-emerald-300" aria-hidden="true" />
            <span className="sr-only sm:not-sr-only">{label}</span>
          </div>
        ))}
      </div>

      <ToolWorkflowStatus miniAppId={miniApp.id} />

      <WorkspaceNavigator miniAppId={miniApp.id} />

      <MiniAppErrorBoundary key={miniApp.id} appId={miniApp.id}>
        <Suspense fallback={<div className="app-surface min-h-56 animate-pulse rounded-[18px]" aria-label="Loading tool" />}>
          <MiniAppSurface miniAppId={miniApp.id} />
        </Suspense>
      </MiniAppErrorBoundary>

      <MiniAppEngagement miniAppId={miniApp.id} title={t(miniApp.titleKey)} />

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-[14px] border border-slate-500/10 bg-slate-500/5 p-2.5 text-xs text-slate-500">
        <span className="flex items-center gap-1.5" title={`Isolated storage: ${runtime.storageNamespace}`}><HardDrive className="size-3.5" /> Local storage</span>
        <Link to={buildTabPath(normalizedLocale, tab.id)} className="text-link min-h-9"><ArrowLeft className="size-3.5" />{t('app.browseTab')}</Link>
      </div>
    </section>
  )
}

import { Link, Outlet, useParams } from 'react-router-dom'
import { MonitorSmartphone, Sparkles } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { BottomNavigation } from '../components/navigation/bottom-navigation'
import { SUPPORTED_LOCALES, normalizeLocale } from '../i18n/locales'
import { buildMiniAppPath, buildTabPath, resolveEntryBySlug } from '../i18n/routing'
import { TAB_BY_ID } from '../features/catalog/tabs'

export function AppShell() {
  const { t } = useTranslation()
  const { locale, slug } = useParams()
  const normalizedLocale = normalizeLocale(locale)
  const currentEntry = slug ? resolveEntryBySlug(slug) : null
  const activeTabId =
    currentEntry?.kind === 'tab'
      ? currentEntry.item.id
      : currentEntry?.kind === 'miniApp'
        ? currentEntry.item.tabId
        : 'zen-time'
  const activeTab = TAB_BY_ID.get(activeTabId)

  const localizedTarget = (nextLocale: (typeof SUPPORTED_LOCALES)[number]) => {
    if (!currentEntry) {
      return buildTabPath(nextLocale, 'zen-time')
    }

    return currentEntry.kind === 'tab'
      ? buildTabPath(nextLocale, currentEntry.item.id)
      : buildMiniAppPath(nextLocale, currentEntry.item.id)
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,rgba(45,212,191,0.18),transparent_28%),linear-gradient(180deg,#f8fafc_0%,#eef2ff_100%)] px-3 py-3 text-slate-950 sm:px-5 sm:py-5">
      <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-6xl gap-5 xl:items-start">
        <aside className="hidden w-[280px] shrink-0 xl:sticky xl:top-5 xl:block">
          <div className="rounded-[32px] border border-white/70 bg-slate-950 p-6 text-white shadow-[0_40px_100px_-48px_rgba(15,23,42,0.95)]">
            <div className="flex size-14 items-center justify-center rounded-3xl bg-white/10 text-teal-300">
              <MonitorSmartphone className="size-6" />
            </div>
            <p className="mt-6 text-2xl font-semibold tracking-tight">PureHub</p>
            <p className="mt-2 text-sm leading-6 text-slate-300">{t('app.subtitle')}</p>
            <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-400">
                {t('app.localeLabel')}
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {SUPPORTED_LOCALES.map((item) => (
                  <Link
                    key={item}
                    to={localizedTarget(item)}
                    className={[
                      'rounded-2xl px-3 py-2 text-xs font-semibold uppercase transition',
                      item === normalizedLocale
                        ? 'bg-white text-slate-950'
                        : 'bg-white/8 text-slate-300 hover:bg-white/14',
                    ].join(' ')}
                  >
                    {item}
                  </Link>
                ))}
              </div>
            </div>
            <div className="mt-8 rounded-3xl bg-white/6 p-4">
              <p className={`text-sm font-semibold ${activeTab?.accentClass ?? 'text-teal-300'}`}>
                {activeTab ? t(activeTab.labelKey) : 'PureHub'}
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                {activeTab ? t(activeTab.descriptionKey) : t('app.subtitle')}
              </p>
            </div>
          </div>
        </aside>

        <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-3xl flex-1 flex-col overflow-hidden rounded-[34px] border border-white/70 bg-white/80 shadow-[0_35px_110px_-60px_rgba(15,23,42,0.55)] backdrop-blur">
          <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/88 px-4 py-4 backdrop-blur sm:px-5">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-950 text-teal-300 shadow-[0_18px_50px_-22px_rgba(15,23,42,0.8)]">
                  <Sparkles className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-lg font-semibold tracking-tight">PureHub</p>
                  <p className="text-sm text-slate-500">{t('app.subtitle')}</p>
                </div>
              </div>
              <div className="flex items-center gap-1 rounded-2xl bg-slate-100 p-1 xl:hidden">
                {SUPPORTED_LOCALES.map((item) => (
                  <Link
                    key={item}
                    to={localizedTarget(item)}
                    className={[
                      'rounded-xl px-2.5 py-1 text-xs font-medium uppercase transition',
                      item === normalizedLocale
                        ? 'bg-white text-slate-950 shadow-sm'
                        : 'text-slate-500 hover:bg-slate-50 hover:text-slate-950',
                    ].join(' ')}
                  >
                    {item}
                  </Link>
                ))}
              </div>
            </div>
          </header>

          <main className="flex-1 overflow-y-auto px-4 pb-28 pt-4 sm:px-5">
            <Outlet />
          </main>

          <BottomNavigation />
        </div>
      </div>
    </div>
  )
}

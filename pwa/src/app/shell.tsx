import { Link, Outlet, useParams } from 'react-router-dom'
import { MonitorSmartphone, Sparkles } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { BottomNav } from '../components/navigation/BottomNav'
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
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,rgba(52,211,153,0.18),transparent_18%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.12),transparent_24%),linear-gradient(180deg,#020617_0%,#0f172a_48%,#111827_100%)] px-3 py-3 text-slate-100 sm:px-5 sm:py-5">
      <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-6xl gap-5 xl:items-start">
        <aside className="hidden w-[280px] shrink-0 xl:sticky xl:top-5 xl:block">
          <div className="rounded-[32px] border border-emerald-400/12 bg-slate-950/96 p-6 text-white shadow-[0_40px_100px_-48px_rgba(16,185,129,0.28)]">
            <div className="flex size-14 items-center justify-center rounded-3xl border border-white/8 bg-white/5 text-emerald-300">
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
            <div className="mt-8 rounded-3xl border border-white/10 bg-white/4 p-4">
              <p className={`text-sm font-semibold ${activeTab?.accentClass ?? 'text-emerald-300'}`}>
                {activeTab ? t(activeTab.labelKey) : 'PureHub'}
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                {activeTab ? t(activeTab.descriptionKey) : t('app.subtitle')}
              </p>
            </div>
          </div>
        </aside>

        <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-4xl flex-1 flex-col overflow-hidden rounded-[34px] border border-white/8 bg-slate-950/78 shadow-[0_35px_110px_-60px_rgba(16,185,129,0.24)] backdrop-blur-xl">
          <header className="sticky top-0 z-20 border-b border-white/8 bg-slate-950/72 px-4 py-4 backdrop-blur-xl sm:px-5">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex size-11 items-center justify-center rounded-2xl border border-emerald-400/14 bg-white/5 text-emerald-300 shadow-[0_18px_50px_-22px_rgba(16,185,129,0.28)]">
                  <Sparkles className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-lg font-semibold tracking-tight">PureHub</p>
                  <p className="text-sm text-slate-400">{t('app.subtitle')}</p>
                </div>
              </div>
              <div className="flex items-center gap-1 rounded-2xl border border-white/8 bg-white/5 p-1 xl:hidden">
                {SUPPORTED_LOCALES.map((item) => (
                  <Link
                    key={item}
                    to={localizedTarget(item)}
                    className={[
                      'rounded-xl px-2.5 py-1 text-xs font-medium uppercase transition',
                      item === normalizedLocale
                        ? 'bg-white text-slate-950 shadow-sm'
                        : 'text-slate-400 hover:bg-white/8 hover:text-white',
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

          <BottomNav />
        </div>
      </div>
    </div>
  )
}

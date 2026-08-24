import { Archive, Code2, Download, Grid2X2, Home, Moon, Settings, Sparkles, Sun, Users } from 'lucide-react'
import { useEffect } from 'react'
import { Link, Outlet, useLocation, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { BottomNav } from '../components/navigation/BottomNav'
import { PwaInstallPrompt } from '../components/pwa/PwaInstallPrompt'
import { SUPPORTED_LOCALES, normalizeLocale } from '../i18n/locales'
import {
  buildMiniAppPath,
  buildTabPath,
  persistSelectedLocale,
  resolveEntryBySlug,
} from '../i18n/routing'
import { anonymousMetricsEnabled, useThemePreference } from '../lib/preferences'
import { trackJourneyEvent } from '../lib/community-api'

const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'

export function AppShell() {
  const { t } = useTranslation()
  const location = useLocation()
  const { lang, appSlug } = useParams()
  const locale = normalizeLocale(lang)
  const currentEntry = appSlug ? resolveEntryBySlug(appSlug) : null
  const { theme, setTheme } = useThemePreference()

  useEffect(() => {
    if (!anonymousMetricsEnabled()) return
    const today = new Date().toISOString().slice(0, 10)
    if (window.localStorage.getItem('purehub-visit-day') !== today) {
      window.localStorage.setItem('purehub-visit-day', today)
      void trackJourneyEvent('visit')
    }
    const installed = window.matchMedia('(display-mode: standalone)').matches || Boolean((navigator as Navigator & { standalone?: boolean }).standalone)
    if (installed && !window.localStorage.getItem('purehub-installed-open-v1')) {
      window.localStorage.setItem('purehub-installed-open-v1', today)
      void trackJourneyEvent('installed_open')
    }
  }, [])

  const localizedTarget = (nextLocale: (typeof SUPPORTED_LOCALES)[number]) => {
    if (location.pathname.endsWith('/tools')) return `/${nextLocale}/tools`
    if (location.pathname.endsWith('/community')) return `/${nextLocale}/community`
    if (location.pathname.endsWith('/settings')) return `/${nextLocale}/settings`
    if (location.pathname.endsWith('/results')) return `/${nextLocale}/results`
    if (location.pathname.endsWith('/privacy-center')) return `/${nextLocale}/privacy-center`
    if (location.pathname.endsWith('/download')) return `/${nextLocale}/download`
    if (location.pathname.endsWith('/changelog')) return `/${nextLocale}/changelog`
    if (location.pathname.endsWith('/privacy')) return `/${nextLocale}/privacy`
    if (location.pathname.endsWith('/terms')) return `/${nextLocale}/terms`
    if (!currentEntry) return `/${nextLocale}`
    return currentEntry.kind === 'tab'
      ? buildTabPath(nextLocale, currentEntry.item.id)
      : buildMiniAppPath(nextLocale, currentEntry.item.id)
  }

  const navItems = [
    { label: t('nav.home'), icon: Home, path: `/${locale}` },
    { label: t('nav.tools'), icon: Grid2X2, path: `/${locale}/tools` },
    { label: t('nav.results'), icon: Archive, path: `/${locale}/results` },
    { label: t('nav.community'), icon: Users, path: `/${locale}/community` },
    { label: t('nav.settings'), icon: Settings, path: `/${locale}/settings` },
  ]

  return (
    <div className="min-h-screen px-2 py-2 text-slate-800 sm:px-4 sm:py-4 dark:text-slate-100">
      <a href="#main-content" className="fixed left-3 top-3 z-[100] -translate-y-24 rounded-xl bg-slate-950 px-4 py-3 text-sm font-bold text-white shadow-xl transition focus:translate-y-0 dark:bg-white dark:text-slate-950">
        Skip to content
      </a>
      <div className="mx-auto flex min-h-[calc(100vh-1rem)] w-full max-w-7xl gap-4 xl:items-start">
        <aside className="hidden w-[256px] shrink-0 xl:sticky xl:top-5 xl:block">
          <div className="app-surface rounded-[20px] p-3">
            <Link to={`/${locale}`} className="flex items-center gap-3 rounded-[16px] p-2">
              <span className="grid size-11 place-items-center rounded-[14px] bg-emerald-500 text-white shadow-sm">
                <Sparkles className="size-5" />
              </span>
              <span><strong className="block text-lg text-slate-950 dark:text-white">PureHub</strong><small className="text-slate-500">Community utilities</small></span>
            </Link>

            <nav className="mt-3 space-y-1">
              {navItems.map(({ label, icon: Icon, path }) => {
                const active = location.pathname === path || (path !== `/${locale}` && location.pathname.startsWith(path))
                return (
                  <Link key={label} to={path} className={`flex min-h-11 items-center gap-3 rounded-[13px] px-3 text-sm font-semibold transition ${active ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300' : 'text-slate-500 hover:bg-slate-500/8 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}>
                    <Icon className="size-5" />{label}
                  </Link>
                )
              })}
            </nav>

            <div className="mt-4 rounded-[14px] bg-emerald-500/8 p-3">
              <p className="text-sm font-bold text-slate-900 dark:text-white">Free forever</p>
              <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">No ads. No mandatory account. Open source with the community.</p>
              <a href={GITHUB_URL} target="_blank" rel="noreferrer" className="text-link mt-3"><Code2 className="size-4" /> GitHub</a>
              <Link to={`/${locale}/download`} className="text-link mt-2"><Download className="size-4" /> Android</Link>
            </div>
          </div>
        </aside>

        <div className="app-surface relative mx-auto flex min-h-[calc(100vh-1rem)] w-full max-w-4xl flex-1 flex-col overflow-hidden rounded-[20px]">
          <header className="sticky top-0 z-30 border-b border-slate-500/10 bg-white/82 px-3 py-2 backdrop-blur-xl sm:px-5 dark:bg-[#131b26]/86">
            <div className="flex items-center justify-between gap-3">
              <Link to={`/${locale}`} className="flex min-w-0 items-center gap-3 xl:hidden">
                <span className="grid size-10 shrink-0 place-items-center rounded-[13px] bg-emerald-500 text-white">
                  <Sparkles className="size-4.5" />
                </span>
                <span className="min-w-0"><strong className="block truncate text-slate-950 dark:text-white">PureHub</strong><small className="block truncate text-slate-500">{t('app.subtitle')}</small></span>
              </Link>
              <p className="hidden text-sm font-semibold text-slate-500 xl:block">{t('app.subtitle')}</p>

              <div className="flex items-center gap-1.5">
                <Link to={`/${locale}/results`} className="grid size-10 place-items-center rounded-[13px] text-slate-500 transition hover:bg-slate-500/10 hover:text-slate-900 dark:hover:text-white" aria-label={t('nav.results')}><Archive className="size-4.5" /></Link>
                <PwaInstallPrompt />
                <button
                  type="button"
                  className="grid size-10 place-items-center rounded-[13px] text-slate-500 transition hover:bg-slate-500/10 hover:text-slate-900 dark:hover:text-white"
                  aria-label="Toggle theme"
                  onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
                >
                  {theme === 'dark' ? <Sun className="size-4.5" /> : <Moon className="size-4.5" />}
                </button>
                <div className="hidden items-center gap-1 rounded-[13px] bg-slate-500/8 p-1 sm:flex">
                  {SUPPORTED_LOCALES.map((item) => (
                    <Link
                      key={item}
                      to={localizedTarget(item)}
                      onClick={() => persistSelectedLocale(item)}
                      className={`rounded-[10px] px-2.5 py-1.5 text-[11px] font-bold uppercase ${item === locale ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-white' : 'text-slate-500'}`}
                    >
                      {item}
                    </Link>
                  ))}
                </div>
              </div>
            </div>
          </header>

          <main id="main-content" tabIndex={-1} className="flex-1 px-3 pb-24 pt-4 outline-none sm:px-5 sm:pt-5">
            <div key={location.pathname} className="page-enter"><Outlet /></div>
            <footer className="mt-6 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 border-t border-slate-500/10 pt-4 text-xs text-slate-500">
              <Link to={`/${locale}/privacy`} className="hover:text-slate-900 dark:hover:text-white">Privacy</Link>
              <Link to={`/${locale}/terms`} className="hover:text-slate-900 dark:hover:text-white">Terms</Link>
              <a href={GITHUB_URL} target="_blank" rel="noreferrer" className="hover:text-slate-900 dark:hover:text-white">Open source</a>
            </footer>
          </main>

          <BottomNav />
        </div>
      </div>
    </div>
  )
}

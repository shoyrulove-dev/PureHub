import { Code2, Grid2X2, Home, Moon, Settings, Sparkles, Sun, Users } from 'lucide-react'
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
import { useThemePreference } from '../lib/preferences'

const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'

export function AppShell() {
  const { t } = useTranslation()
  const location = useLocation()
  const { lang, appSlug } = useParams()
  const locale = normalizeLocale(lang)
  const currentEntry = appSlug ? resolveEntryBySlug(appSlug) : null
  const { theme, setTheme } = useThemePreference()

  const localizedTarget = (nextLocale: (typeof SUPPORTED_LOCALES)[number]) => {
    if (location.pathname.endsWith('/tools')) return `/${nextLocale}/tools`
    if (location.pathname.endsWith('/community')) return `/${nextLocale}/community`
    if (location.pathname.endsWith('/settings')) return `/${nextLocale}/settings`
    if (!currentEntry) return `/${nextLocale}`
    return currentEntry.kind === 'tab'
      ? buildTabPath(nextLocale, currentEntry.item.id)
      : buildMiniAppPath(nextLocale, currentEntry.item.id)
  }

  const navItems = [
    { label: t('nav.home'), icon: Home, path: `/${locale}` },
    { label: t('nav.tools'), icon: Grid2X2, path: `/${locale}/tools` },
    { label: t('nav.community'), icon: Users, path: `/${locale}/community` },
    { label: t('nav.settings'), icon: Settings, path: `/${locale}/settings` },
  ]

  return (
    <div className="min-h-screen px-3 py-3 text-slate-800 sm:px-5 sm:py-5 dark:text-slate-100">
      <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-7xl gap-5 xl:items-start">
        <aside className="hidden w-[256px] shrink-0 xl:sticky xl:top-5 xl:block">
          <div className="app-surface rounded-[24px] p-4">
            <Link to={`/${locale}`} className="flex items-center gap-3 rounded-[16px] p-2">
              <span className="grid size-11 place-items-center rounded-[14px] bg-emerald-500 text-white shadow-sm">
                <Sparkles className="size-5" />
              </span>
              <span><strong className="block text-lg text-slate-950 dark:text-white">PureHub</strong><small className="text-slate-500">Community utilities</small></span>
            </Link>

            <nav className="mt-5 space-y-1">
              {navItems.map(({ label, icon: Icon, path }) => {
                const active = location.pathname === path || (path !== `/${locale}` && location.pathname.startsWith(path))
                return (
                  <Link key={label} to={path} className={`flex min-h-12 items-center gap-3 rounded-[14px] px-3 text-sm font-semibold transition ${active ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300' : 'text-slate-500 hover:bg-slate-500/8 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}>
                    <Icon className="size-5" />{label}
                  </Link>
                )
              })}
            </nav>

            <div className="mt-6 rounded-[16px] bg-emerald-500/8 p-4">
              <p className="text-sm font-bold text-slate-900 dark:text-white">Free forever</p>
              <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">No ads. No mandatory account. Open source with the community.</p>
              <a href={GITHUB_URL} target="_blank" rel="noreferrer" className="text-link mt-3"><Code2 className="size-4" /> GitHub</a>
            </div>
          </div>
        </aside>

        <div className="app-surface relative mx-auto flex min-h-[calc(100vh-1.5rem)] w-full max-w-4xl flex-1 flex-col overflow-hidden rounded-[24px]">
          <header className="sticky top-0 z-30 border-b border-slate-500/10 bg-white/82 px-4 py-3 backdrop-blur-xl sm:px-6 dark:bg-[#131b26]/86">
            <div className="flex items-center justify-between gap-3">
              <Link to={`/${locale}`} className="flex min-w-0 items-center gap-3 xl:hidden">
                <span className="grid size-10 shrink-0 place-items-center rounded-[13px] bg-emerald-500 text-white">
                  <Sparkles className="size-4.5" />
                </span>
                <span className="min-w-0"><strong className="block truncate text-slate-950 dark:text-white">PureHub</strong><small className="block truncate text-slate-500">{t('app.subtitle')}</small></span>
              </Link>
              <p className="hidden text-sm font-semibold text-slate-500 xl:block">{t('app.subtitle')}</p>

              <div className="flex items-center gap-1.5">
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

          <main className="flex-1 px-4 pb-28 pt-5 sm:px-6 sm:pt-6">
            <div key={location.pathname} className="page-enter"><Outlet /></div>
          </main>

          <BottomNav />
        </div>
      </div>
    </div>
  )
}

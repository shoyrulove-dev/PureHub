import { Link, Outlet, useParams } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { BottomNavigation } from '../components/navigation/bottom-navigation'
import { SUPPORTED_LOCALES, normalizeLocale } from '../i18n/locales'
import { buildMiniAppPath, buildTabPath, resolveEntryBySlug } from '../i18n/routing'

export function AppShell() {
  const { t } = useTranslation()
  const { locale, slug } = useParams()
  const normalizedLocale = normalizeLocale(locale)
  const currentEntry = slug ? resolveEntryBySlug(slug) : null

  const localizedTarget = (nextLocale: (typeof SUPPORTED_LOCALES)[number]) => {
    if (!currentEntry) {
      return buildTabPath(nextLocale, 'zen-time')
    }

    return currentEntry.kind === 'tab'
      ? buildTabPath(nextLocale, currentEntry.item.id)
      : buildMiniAppPath(nextLocale, currentEntry.item.id)
  }

  return (
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col bg-slate-50 text-slate-950">
      <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/90 px-5 py-4 backdrop-blur">
        <div className="flex items-start justify-between gap-3">
          <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-950 text-teal-300 shadow-[0_18px_50px_-22px_rgba(15,23,42,0.8)]">
            <Sparkles className="size-5" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold tracking-tight">PureHub</p>
            <p className="text-sm text-slate-500">{t('app.subtitle')}</p>
          </div>
          <div className="flex items-center gap-1 rounded-2xl bg-slate-100 p-1">
            {SUPPORTED_LOCALES.map((item) => (
              <Link
                key={item}
                to={localizedTarget(item)}
                className={[
                  'rounded-xl px-2.5 py-1 text-xs font-medium uppercase transition',
                  item === normalizedLocale ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500',
                ].join(' ')}
              >
                {item}
              </Link>
            ))}
          </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto px-4 pb-28 pt-4">
        <Outlet />
      </main>

      <BottomNavigation />
    </div>
  )
}

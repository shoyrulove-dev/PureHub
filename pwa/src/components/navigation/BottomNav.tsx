import { Grid2X2, Home, Settings, Users } from 'lucide-react'
import { NavLink, useLocation, useParams } from 'react-router-dom'
import { normalizeLocale } from '../../i18n/locales'
import { useTranslation } from 'react-i18next'

export function BottomNav() {
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const location = useLocation()
  const { t } = useTranslation()
  const items = [
    { label: t('nav.home'), icon: Home, path: `/${locale}`, active: location.pathname === `/${locale}` },
    { label: t('nav.tools'), icon: Grid2X2, path: `/${locale}/tools`, active: location.pathname.includes(`/${locale}/tools`) },
    { label: t('nav.community'), icon: Users, path: `/${locale}/community`, active: location.pathname.includes(`/${locale}/community`) },
    { label: t('nav.settings'), icon: Settings, path: `/${locale}/settings`, active: location.pathname.includes(`/${locale}/settings`) },
  ]

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 mx-auto w-full max-w-4xl px-3 pb-[calc(env(safe-area-inset-bottom)+0.75rem)] xl:absolute">
      <ul className="app-surface grid grid-cols-4 gap-1 rounded-[18px] p-1 backdrop-blur-xl">
        {items.map(({ label, icon: Icon, path, active }) => (
          <li key={label}>
            <NavLink
              to={path}
              className={[
                'flex min-h-[50px] flex-col items-center justify-center gap-0.5 rounded-[14px] px-2 text-[10px] font-semibold transition',
                active
                  ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                  : 'text-slate-500 hover:bg-slate-500/8 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white',
              ].join(' ')}
            >
              <Icon className="size-5" strokeWidth={active ? 2.4 : 2} />
              <span className={active ? '' : 'sr-only'}>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

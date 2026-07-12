import type { IconType } from 'react-icons'
import {
  GiCheckedShield,
  GiCompass,
  GiHourglass,
  GiPortal,
  GiThirdEye,
} from 'react-icons/gi'
import { NavLink, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { TAB_ITEMS, type TabId } from '../../features/catalog/tabs'
import { normalizeLocale } from '../../i18n/locales'
import { buildTabPath, resolveEntryBySlug } from '../../i18n/routing'

const tabIcons: Record<TabId, IconType> = {
  'zen-time': GiHourglass,
  'measure-tools': GiCompass,
  vision: GiThirdEye,
  'security-audio': GiCheckedShield,
  'finance-community': GiPortal,
}

export function BottomNav() {
  const { t } = useTranslation()
  const { lang, appSlug } = useParams()
  const normalizedLocale = normalizeLocale(lang)
  const currentEntry = appSlug ? resolveEntryBySlug(appSlug) : null
  const activeTabId =
    currentEntry?.kind === 'tab'
      ? currentEntry.item.id
      : currentEntry?.kind === 'miniApp'
        ? currentEntry.item.tabId
        : 'zen-time'

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 mx-auto w-full max-w-4xl px-3 pb-[calc(env(safe-area-inset-bottom)+0.85rem)]">
      <div className="rounded-[30px] border border-emerald-400/18 bg-slate-950/92 p-2 shadow-[0_28px_100px_-42px_rgba(16,185,129,0.55)] backdrop-blur-xl">
        <ul className="grid grid-cols-5 gap-2">
          {TAB_ITEMS.map((tab) => {
            const Icon = tabIcons[tab.id]
            const isActive = activeTabId === tab.id

            return (
              <li key={tab.id}>
                <NavLink
                  to={buildTabPath(normalizedLocale, tab.id)}
                  className={[
                    'group flex min-h-[60px] flex-col items-center justify-center gap-1 rounded-[22px] px-2 py-2 text-[11px] font-semibold transition duration-200',
                    isActive
                      ? 'bg-emerald-400/12 text-emerald-300 shadow-[inset_0_0_0_1px_rgba(52,211,153,0.22)]'
                      : 'text-slate-400 hover:bg-white/6 hover:text-slate-100',
                  ].join(' ')}
                >
                  <Icon
                    className={[
                      'text-[1.2rem] transition duration-200',
                      isActive
                        ? 'text-emerald-300 drop-shadow-[0_0_12px_rgba(52,211,153,0.55)]'
                        : 'text-purple-300/85 group-hover:text-emerald-200',
                    ].join(' ')}
                  />
                  <span className="truncate">{t(tab.shortLabelKey)}</span>
                </NavLink>
              </li>
            )
          })}
        </ul>
      </div>
    </nav>
  )
}

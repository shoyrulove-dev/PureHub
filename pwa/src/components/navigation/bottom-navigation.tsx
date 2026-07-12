import type { LucideIcon } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { TAB_ITEMS } from '../../features/catalog/tabs'

export function BottomNavigation() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-md border-t border-slate-200/80 bg-white/95 px-3 pb-[calc(env(safe-area-inset-bottom)+0.75rem)] pt-3 backdrop-blur">
      <ul className="grid grid-cols-5 gap-2">
        {TAB_ITEMS.map((tab) => (
          <li key={tab.id}>
            <NavLink
              to={tab.path}
              className={({ isActive }) =>
                [
                  'flex flex-col items-center justify-center gap-1 rounded-2xl px-2 py-2 text-[11px] font-medium transition',
                  isActive
                    ? 'bg-slate-950 text-white shadow-[0_18px_35px_-20px_rgba(15,23,42,0.9)]'
                    : 'text-slate-500 hover:bg-slate-100 hover:text-slate-950',
                ].join(' ')
              }
            >
              <TabIcon icon={tab.icon} />
              <span>{tab.shortLabel}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

function TabIcon({ icon: Icon }: { icon: LucideIcon }) {
  return <Icon className="size-4" strokeWidth={2.2} />
}

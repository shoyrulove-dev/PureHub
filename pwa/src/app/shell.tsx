import { Outlet } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import { BottomNavigation } from '../components/navigation/bottom-navigation'

export function AppShell() {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col bg-slate-50 text-slate-950">
      <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/90 px-5 py-4 backdrop-blur">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-950 text-teal-300 shadow-[0_18px_50px_-22px_rgba(15,23,42,0.8)]">
            <Sparkles className="size-5" />
          </div>
          <div>
            <p className="text-lg font-semibold tracking-tight">PureHub</p>
            <p className="text-sm text-slate-500">Offline utility super app</p>
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

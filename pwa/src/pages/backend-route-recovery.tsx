import { useEffect, useState } from 'react'

const RECOVERY_KEY = 'purehub-backend-route-recovery'

async function clearPwaNavigationControl() {
  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(registrations.map((registration) => registration.unregister()))
  }

  if ('caches' in window) {
    const cacheNames = await caches.keys()
    await Promise.all(cacheNames.map((cacheName) => caches.delete(cacheName)))
  }
}

function reloadBackendRoute() {
  const url = new URL(window.location.href)
  url.searchParams.set('_backend', Date.now().toString())
  window.location.replace(url.toString())
}

export function BackendRouteRecovery() {
  const [manualRecovery, setManualRecovery] = useState(false)

  useEffect(() => {
    if (window.sessionStorage.getItem(RECOVERY_KEY)) {
      setManualRecovery(true)
      return
    }

    window.sessionStorage.setItem(RECOVERY_KEY, 'attempted')
    void clearPwaNavigationControl()
      .catch(() => undefined)
      .finally(reloadBackendRoute)
  }, [])

  const retry = () => {
    window.sessionStorage.removeItem(RECOVERY_KEY)
    void clearPwaNavigationControl()
      .catch(() => undefined)
      .finally(reloadBackendRoute)
  }

  return (
    <main className="grid min-h-screen place-items-center bg-slate-950 px-5 text-slate-100">
      <section className="w-full max-w-md rounded-[28px] border border-cyan-300/15 bg-slate-900 p-7 shadow-2xl shadow-cyan-950/30">
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-cyan-300">PureHub recovery</p>
        <h1 className="mt-3 text-2xl font-bold">Opening Command Center</h1>
        <p className="mt-3 text-sm leading-7 text-slate-300">
          {manualRecovery
            ? 'A cached PWA route is still active. Retry once to clear only PureHub service-worker caches.'
            : 'Removing an outdated PWA navigation cache, then loading the secure backend directly.'}
        </p>
        {manualRecovery ? (
          <button
            type="button"
            onClick={retry}
            className="mt-5 w-full rounded-2xl border border-cyan-300/20 bg-cyan-400/10 px-4 py-3 font-semibold text-cyan-100"
          >
            Retry Command Center
          </button>
        ) : (
          <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-white/5">
            <div className="h-full w-2/3 animate-pulse rounded-full bg-cyan-300" />
          </div>
        )}
      </section>
    </main>
  )
}

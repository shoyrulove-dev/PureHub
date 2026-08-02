import { useEffect, useState } from 'react'
import type { MiniAppId } from '../features/catalog/tabs'

export type ThemePreference = 'system' | 'light' | 'dark'

const FAVORITES_KEY = 'purehub-favorite-tools'
const RECENTS_KEY = 'purehub-recent-tools'
const THEME_KEY = 'purehub-theme'
const ANONYMOUS_METRICS_KEY = 'purehub-anonymous-metrics'
const CHANGE_EVENT = 'purehub-preferences-change'

function readList(key: string): MiniAppId[] {
  if (typeof window === 'undefined') return []
  try {
    return JSON.parse(window.localStorage.getItem(key) ?? '[]') as MiniAppId[]
  } catch {
    return []
  }
}

function emitChange() {
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function useToolPreferences() {
  const [favorites, setFavorites] = useState<MiniAppId[]>(() => readList(FAVORITES_KEY))
  const [recents, setRecents] = useState<MiniAppId[]>(() => readList(RECENTS_KEY))

  useEffect(() => {
    const sync = () => {
      setFavorites(readList(FAVORITES_KEY))
      setRecents(readList(RECENTS_KEY))
    }
    window.addEventListener(CHANGE_EVENT, sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener(CHANGE_EVENT, sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  const toggleFavorite = (id: MiniAppId) => {
    const next = favorites.includes(id)
      ? favorites.filter((item) => item !== id)
      : [id, ...favorites]
    window.localStorage.setItem(FAVORITES_KEY, JSON.stringify(next))
    emitChange()
  }

  return { favorites, recents, toggleFavorite }
}

export function rememberRecentTool(id: MiniAppId) {
  if (typeof window === 'undefined') return
  const next = [id, ...readList(RECENTS_KEY).filter((item) => item !== id)].slice(0, 6)
  window.localStorage.setItem(RECENTS_KEY, JSON.stringify(next))
  emitChange()
}

export function useThemePreference() {
  const [theme, setThemeState] = useState<ThemePreference>(() => {
    if (typeof window === 'undefined') return 'system'
    return (window.localStorage.getItem(THEME_KEY) as ThemePreference | null) ?? 'system'
  })

  useEffect(() => {
    const sync = () => {
      setThemeState((window.localStorage.getItem(THEME_KEY) as ThemePreference | null) ?? 'system')
    }
    window.addEventListener(CHANGE_EVENT, sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener(CHANGE_EVENT, sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () => {
      const dark = theme === 'dark' || (theme === 'system' && media.matches)
      document.documentElement.classList.toggle('dark', dark)
      document.documentElement.dataset.theme = dark ? 'dark' : 'light'
    }
    apply()
    media.addEventListener('change', apply)
    return () => media.removeEventListener('change', apply)
  }, [theme])

  const setTheme = (next: ThemePreference) => {
    window.localStorage.setItem(THEME_KEY, next)
    setThemeState(next)
    emitChange()
  }

  return { theme, setTheme }
}

export function anonymousMetricsEnabled() {
  if (typeof window === 'undefined') return false
  if (window.navigator.doNotTrack === '1') return false
  return window.localStorage.getItem(ANONYMOUS_METRICS_KEY) !== 'false'
}

export function useAnonymousMetricsPreference() {
  const [enabled, setEnabledState] = useState(() => anonymousMetricsEnabled())

  useEffect(() => {
    const sync = () => setEnabledState(anonymousMetricsEnabled())
    window.addEventListener(CHANGE_EVENT, sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener(CHANGE_EVENT, sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  const setEnabled = (next: boolean) => {
    window.localStorage.setItem(ANONYMOUS_METRICS_KEY, String(next))
    setEnabledState(next)
    emitChange()
  }

  return { enabled, setEnabled, lockedByDnt: typeof navigator !== 'undefined' && navigator.doNotTrack === '1' }
}

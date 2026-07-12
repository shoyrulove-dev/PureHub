import i18n from 'i18next'
import {
  MINI_APP_BY_ID,
  MINI_APP_ITEMS,
  TAB_BY_ID,
  TAB_ITEMS,
  type MiniAppDefinition,
  type TabDefinition,
} from '../features/catalog/tabs'
import { DEFAULT_LOCALE, normalizeLocale, type LocaleCode } from './locales'

const PREFERRED_LOCALE_KEY = 'purehub-preferred-locale'

type ResolvedEntry =
  | { kind: 'tab'; item: TabDefinition }
  | { kind: 'miniApp'; item: MiniAppDefinition }

export function detectPreferredLocale(): LocaleCode {
  if (typeof window !== 'undefined') {
    const savedLocale = window.localStorage.getItem(PREFERRED_LOCALE_KEY)
    if (savedLocale) {
      return normalizeLocale(savedLocale)
    }
  }

  const detected = i18n.services.languageDetector?.detect()
  const detectedValue = Array.isArray(detected) ? detected[0] : detected
  const resolvedLocale = normalizeLocale(
    detectedValue ?? i18n.resolvedLanguage ?? i18n.language ?? DEFAULT_LOCALE,
  )

  if (typeof window !== 'undefined') {
    window.localStorage.setItem(PREFERRED_LOCALE_KEY, resolvedLocale)
  }

  return resolvedLocale
}

export function buildTabPath(locale: LocaleCode, tabId: TabDefinition['id']) {
  const tab = TAB_BY_ID.get(tabId)
  return `/${locale}/${tab?.segments[locale] ?? TAB_ITEMS[0].segments[locale]}`
}

export function buildMiniAppPath(locale: LocaleCode, miniAppId: MiniAppDefinition['id']) {
  const miniApp = MINI_APP_BY_ID.get(miniAppId)
  return `/${locale}/${miniApp?.slugs[locale] ?? MINI_APP_ITEMS[0].slugs[locale]}`
}

export function resolveEntryBySlug(slug: string): ResolvedEntry | null {
  const normalizedSlug = decodeURIComponent(slug)
  const tab = TAB_ITEMS.find((item) =>
    Object.values(item.segments).some((segment) => segment === normalizedSlug),
  )
  if (tab) {
    return { kind: 'tab', item: tab }
  }

  const miniApp = MINI_APP_ITEMS.find((item) =>
    Object.values(item.slugs).some((segment) => segment === normalizedSlug),
  )
  if (miniApp) {
    return { kind: 'miniApp', item: miniApp }
  }

  return null
}

export function canonicalPathForEntry(locale: LocaleCode, entry: ResolvedEntry) {
  if (entry.kind === 'tab') {
    return buildTabPath(locale, entry.item.id)
  }

  return buildMiniAppPath(locale, entry.item.id)
}

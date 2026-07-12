import { Navigate, useParams } from 'react-router-dom'
import { TAB_BY_ID } from '../features/catalog/tabs'
import { normalizeLocale } from '../i18n/locales'
import { buildTabPath, canonicalPathForEntry, resolveEntryBySlug } from '../i18n/routing'
import { MiniAppLandingPage } from './mini-app-landing-page'
import { TabLandingPage } from './tab-landing-page'

export function LocalizedEntryPage() {
  const { slug, locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)

  if (!slug) {
    return <Navigate to={buildTabPath(normalizedLocale, 'zen-time')} replace />
  }

  const entry = resolveEntryBySlug(slug)
  if (!entry) {
    return <Navigate to={buildTabPath(normalizedLocale, 'zen-time')} replace />
  }

  const canonicalPath = canonicalPathForEntry(normalizedLocale, entry)
  if (`/${normalizedLocale}/${slug}` !== canonicalPath) {
    return <Navigate to={canonicalPath} replace />
  }

  if (entry.kind === 'tab') {
    return <TabLandingPage tab={entry.item} />
  }

  return <MiniAppLandingPage miniApp={entry.item} tab={TAB_BY_ID.get(entry.item.tabId)!} />
}

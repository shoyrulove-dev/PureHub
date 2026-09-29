import { Navigate, useParams } from 'react-router-dom'
import { buildCanonicalUrl, getSeoMetaBySlug } from '../config/seoMeta'
import { TAB_BY_ID } from '../features/catalog/tabs'
import { normalizeLocale } from '../i18n/locales'
import { buildTabPath, canonicalPathForEntry, resolveEntryBySlug } from '../i18n/routing'
import { SeoHelmet } from '../components/seo/SeoHelmet'
import { MiniAppLandingPage } from './mini-app-landing-page'
import { TabLandingPage } from './tab-landing-page'

export function LocalizedEntryPage() {
  const { appSlug, lang } = useParams()
  const normalizedLocale = normalizeLocale(lang)

  if (!appSlug) {
    return <Navigate to={buildTabPath(normalizedLocale, 'zen-time')} replace />
  }

  const entry = resolveEntryBySlug(appSlug)
  if (!entry) {
    return <Navigate to={buildTabPath(normalizedLocale, 'zen-time')} replace />
  }

  const canonicalPath = canonicalPathForEntry(normalizedLocale, entry)
  if (`/${normalizedLocale}/${appSlug}` !== canonicalPath) {
    return <Navigate to={canonicalPath} replace />
  }

  if (entry.kind === 'tab') {
    return <TabLandingPage tab={entry.item} />
  }

  const seoEntry = getSeoMetaBySlug(normalizedLocale, appSlug)
  const tab = TAB_BY_ID.get(entry.item.tabId)!

  return (
    <>
      {seoEntry ? (
        <SeoHelmet
          appId={seoEntry.appId}
          lang={seoEntry.lang}
          title={seoEntry.title}
          description={seoEntry.description}
          keywords={seoEntry.keywords}
          canonicalUrl={buildCanonicalUrl(seoEntry.lang, seoEntry.slug)}
        />
      ) : null}
      <MiniAppLandingPage miniApp={entry.item} tab={tab} />
    </>
  )
}

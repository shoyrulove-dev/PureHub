import { useEffect } from 'react'
import { Helmet } from 'react-helmet-async'
import { Navigate, useLocation, useParams } from 'react-router-dom'
import i18n from 'i18next'
import { AppShell } from './shell'
import { normalizeLocale } from '../i18n/locales'
import { SiteSeoHelmet } from '../components/seo/SiteSeoHelmet'
import type { SeoSitePageId } from '../config/seoMeta'

const SEO_PAGE_BY_SEGMENT: Record<string, SeoSitePageId> = {
  '': 'home',
  tools: 'tools',
  community: 'community',
  download: 'download',
  changelog: 'changelog',
}

export function LocaleLayout() {
  const location = useLocation()
  const { lang } = useParams()
  const normalizedLocale = normalizeLocale(lang)

  useEffect(() => {
    void i18n.changeLanguage(normalizedLocale)
    document.documentElement.lang = normalizedLocale
  }, [normalizedLocale])

  if (lang !== normalizedLocale) {
    const suffix = location.pathname.replace(/^\/[^/]+/, '') || ''
    return <Navigate to={`/${normalizedLocale}${suffix}`} replace />
  }

  const pageSegment = location.pathname.replace(/^\/[^/]+\/?/, '').split('/')[0] ?? ''
  const seoPageId = SEO_PAGE_BY_SEGMENT[pageSegment]

  return (
    <>
      {seoPageId ? <SiteSeoHelmet lang={normalizedLocale} pageId={seoPageId} /> : null}
      {pageSegment === 'settings' ? (
        <Helmet>
          <title>PureHub Settings</title>
          <meta name="robots" content="noindex, follow" />
        </Helmet>
      ) : null}
      <AppShell />
    </>
  )
}

import { Helmet } from 'react-helmet-async'
import {
  SEO_LANGUAGES,
  SITE_ORIGIN,
  buildSitePageUrl,
  seoSiteMeta,
  type SeoLanguage,
  type SeoSitePageId,
} from '../../config/seoMeta'

type SiteSeoHelmetProps = {
  lang: SeoLanguage
  pageId: SeoSitePageId
}

export function SiteSeoHelmet({ lang, pageId }: SiteSeoHelmetProps) {
  const meta = seoSiteMeta[pageId][lang]
  const canonicalUrl = buildSitePageUrl(lang, pageId)

  return (
    <Helmet htmlAttributes={{ lang }}>
      <title>{meta.title}</title>
      <meta name="description" content={meta.description} />
      <meta name="keywords" content={meta.keywords.join(', ')} />
      <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1" />
      <meta property="og:type" content="website" />
      <meta property="og:site_name" content="PureHub" />
      <meta property="og:title" content={meta.title} />
      <meta property="og:description" content={meta.description} />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:image" content={`${SITE_ORIGIN}/og-image.png`} />
      <meta property="og:image:width" content="1200" />
      <meta property="og:image:height" content="630" />
      <meta property="og:image:alt" content="PureHub – 22 free, ad-free mini apps" />
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={meta.title} />
      <meta name="twitter:description" content={meta.description} />
      <meta name="twitter:image" content={`${SITE_ORIGIN}/og-image.png`} />
      <link rel="canonical" href={canonicalUrl} />
      {SEO_LANGUAGES.map((alternateLang) => (
        <link
          key={alternateLang}
          rel="alternate"
          hrefLang={alternateLang}
          href={buildSitePageUrl(alternateLang, pageId)}
        />
      ))}
      <link rel="alternate" hrefLang="x-default" href={buildSitePageUrl('en', pageId)} />
      <script type="application/ld+json">
        {JSON.stringify({
          '@context': 'https://schema.org',
          '@type': pageId === 'tools' ? 'CollectionPage' : 'WebPage',
          name: meta.title,
          description: meta.description,
          url: canonicalUrl,
          inLanguage: lang,
          isPartOf: {
            '@type': 'WebSite',
            '@id': `${SITE_ORIGIN}/#website`,
            name: 'PureHub',
            url: SITE_ORIGIN,
          },
        })}
      </script>
    </Helmet>
  )
}

import { Helmet } from 'react-helmet-async'
import { SEO_LANGUAGES, SITE_ORIGIN, seoMeta, type SeoLanguage, type SeoMiniAppId } from '../../config/seoMeta'

type SeoHelmetProps = {
  title: string
  description: string
  keywords: string[]
  canonicalUrl: string
  lang: SeoLanguage
  appId: SeoMiniAppId
}

export function SeoHelmet({
  title,
  description,
  keywords,
  canonicalUrl,
  lang,
  appId,
}: SeoHelmetProps) {
  return (
    <Helmet htmlAttributes={{ lang }}>
      <title>{title}</title>
      <meta name="description" content={description} />
      <meta name="keywords" content={keywords.join(', ')} />
      <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1" />
      <meta property="og:type" content="website" />
      <meta property="og:site_name" content="PureHub" />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:image" content={`${SITE_ORIGIN}/og-image.png`} />
      <meta property="og:image:width" content="1200" />
      <meta property="og:image:height" content="630" />
      <meta property="og:image:alt" content={`${title} – PureHub`} />
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={description} />
      <meta name="twitter:image" content={`${SITE_ORIGIN}/og-image.png`} />
      <link rel="canonical" href={canonicalUrl} />
      {SEO_LANGUAGES.map((alternateLang) => (
        <link
          key={alternateLang}
          rel="alternate"
          hrefLang={alternateLang}
          href={`${SITE_ORIGIN}/${alternateLang}/${seoMeta[appId][alternateLang].slug}`}
        />
      ))}
      <link rel="alternate" hrefLang="x-default" href={`${SITE_ORIGIN}/en/${seoMeta[appId].en.slug}`} />
      <script type="application/ld+json">
        {JSON.stringify({
          '@context': 'https://schema.org',
          '@type': 'WebApplication',
          name: title,
          description,
          url: canonicalUrl,
          applicationCategory: 'UtilitiesApplication',
          operatingSystem: 'Any',
          browserRequirements: 'Requires a modern web browser with JavaScript enabled',
          inLanguage: lang,
          isAccessibleForFree: true,
          offers: {
            '@type': 'Offer',
            price: '0',
            priceCurrency: 'USD',
          },
          publisher: {
            '@type': 'Organization',
            name: 'PureHub Community',
            url: SITE_ORIGIN,
          },
        })}
      </script>
    </Helmet>
  )
}

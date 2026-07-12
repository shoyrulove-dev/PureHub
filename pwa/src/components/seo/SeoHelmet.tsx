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
      <meta property="og:type" content="website" />
      <meta property="og:site_name" content="PureHub" />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />
      <meta property="og:url" content={canonicalUrl} />
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={description} />
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
    </Helmet>
  )
}

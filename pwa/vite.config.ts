import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { defineConfig } from 'vite'
import Sitemap from 'vite-plugin-sitemap'
import { VitePWA } from 'vite-plugin-pwa'
import {
  buildSeoSitemapPaths,
  buildSitePageUrl,
  SEO_LANGUAGES,
  SEO_SITE_PAGE_IDS,
  SITE_ORIGIN,
  seoMeta,
  seoRouteEntries,
  seoSiteMeta,
} from './src/config/seoMeta.js'

function escapeAttribute(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}

function injectSeoHtml(
  source: string,
  page: { lang: string; title: string; description: string; canonicalUrl: string; alternates: Record<string, string>; schema: object },
) {
  const title = escapeAttribute(page.title)
  const description = escapeAttribute(page.description)
  const canonicalUrl = escapeAttribute(page.canonicalUrl)
  const alternateLinks = Object.entries(page.alternates)
    .map(([lang, href]) => `    <link rel="alternate" hreflang="${lang}" href="${escapeAttribute(href)}" />`)
    .join('\n')
  const schema = JSON.stringify(page.schema).replaceAll('<', '\\u003c')

  return source
    .replace(/<html lang="[^"]+">/, `<html lang="${page.lang}">`)
    .replace(/<title>[^<]*<\/title>/, `<title>${title}</title>`)
    .replace(/<meta name="description" content="[^"]*" \/>/, `<meta name="description" content="${description}" />`)
    .replace(/<meta property="og:title" content="[^"]*" \/>/, `<meta property="og:title" content="${title}" />`)
    .replace(/<meta property="og:description" content="[^"]*" \/>/, `<meta property="og:description" content="${description}" />`)
    .replace(/<meta property="og:url" content="[^"]*" \/>/, `<meta property="og:url" content="${canonicalUrl}" />`)
    .replace(/<meta name="twitter:title" content="[^"]*" \/>/, `<meta name="twitter:title" content="${title}" />`)
    .replace(/<meta name="twitter:description" content="[^"]*" \/>/, `<meta name="twitter:description" content="${description}" />`)
    .replace(/\s*<link rel="alternate"[^>]*\/>/g, '')
    .replace(
      /<link rel="canonical" href="[^"]*" \/>/,
      `<link rel="canonical" href="${canonicalUrl}" />\n${alternateLinks}`,
    )
    .replace('</head>', `    <script type="application/ld+json" data-seo-page>${schema}</script>\n  </head>`)
}

function staticSeoPages() {
  return {
    name: 'purehub-static-seo-pages',
    closeBundle() {
      const distRoot = resolve(import.meta.dirname, 'dist')
      const shell = readFileSync(resolve(distRoot, 'index.html'), 'utf8')
      const pages: Array<{
        path: string
        lang: string
        title: string
        description: string
        canonicalUrl: string
        alternates: Record<string, string>
        schema: object
      }> = []

      for (const entry of seoRouteEntries) {
        const meta = seoMeta[entry.appId][entry.lang]
        const canonicalUrl = `${SITE_ORIGIN}${entry.path}`
        pages.push({
          path: entry.path,
          lang: entry.lang,
          title: meta.title,
          description: meta.description,
          canonicalUrl,
          alternates: Object.fromEntries([
            ...SEO_LANGUAGES.map((lang) => [lang, `${SITE_ORIGIN}/${lang}/${seoMeta[entry.appId][lang].slug}`]),
            ['x-default', `${SITE_ORIGIN}/en/${seoMeta[entry.appId].en.slug}`],
          ]),
          schema: {
            '@context': 'https://schema.org',
            '@type': 'WebApplication',
            name: meta.title,
            description: meta.description,
            url: canonicalUrl,
            applicationCategory: 'UtilitiesApplication',
            operatingSystem: 'Any',
            inLanguage: entry.lang,
            isAccessibleForFree: true,
            offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },
          },
        })
      }

      for (const pageId of SEO_SITE_PAGE_IDS) {
        for (const lang of SEO_LANGUAGES) {
          const meta = seoSiteMeta[pageId][lang]
          const canonicalUrl = buildSitePageUrl(lang, pageId)
          pages.push({
            path: new URL(canonicalUrl).pathname,
            lang,
            title: meta.title,
            description: meta.description,
            canonicalUrl,
            alternates: Object.fromEntries([
              ...SEO_LANGUAGES.map((alternateLang) => [alternateLang, buildSitePageUrl(alternateLang, pageId)]),
              ['x-default', buildSitePageUrl('en', pageId)],
            ]),
            schema: {
              '@context': 'https://schema.org',
              '@type': pageId === 'tools' ? 'CollectionPage' : 'WebPage',
              name: meta.title,
              description: meta.description,
              url: canonicalUrl,
              inLanguage: lang,
              isPartOf: { '@type': 'WebSite', '@id': `${SITE_ORIGIN}/#website`, name: 'PureHub', url: SITE_ORIGIN },
            },
          })
        }
      }

      for (const page of pages) {
        const outputPath = resolve(distRoot, page.path.slice(1), 'index.html')
        mkdirSync(dirname(outputPath), { recursive: true })
        writeFileSync(outputPath, injectSeoHtml(shell, page), 'utf8')
      }
    },
  }
}

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    Sitemap({
      hostname: SITE_ORIGIN,
      dynamicRoutes: buildSeoSitemapPaths(),
      exclude: ['/'],
      readable: true,
      generateRobotsTxt: true,
    }),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      strategies: 'generateSW',
      includeAssets: ['favicon.ico', 'favicon.svg', 'favicon-*.png', 'apple-touch-icon.png', 'og-image.png', 'icons/*.svg'],
      manifest: false,
      devOptions: {
        enabled: true,
      },
      workbox: {
        cleanupOutdatedCaches: true,
        clientsClaim: true,
        skipWaiting: true,
        navigateFallback: 'index.html',
        navigateFallbackDenylist: [
          /^\/admin(?:\/|$)/,
          /^\/public-api(?:\/|$)/,
          /^\/api(?:\/|$)/,
        ],
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webmanifest,woff2,json,txt}'],
        globIgnores: ['**/*/index.html'],
      },
    }),
    staticSeoPages(),
  ],
  server: {
    host: true,
  },
  preview: {
    host: true,
  },
})

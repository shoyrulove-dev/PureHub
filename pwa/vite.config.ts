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
import { getSeoContent, getSeoCopy } from './src/config/seoContent.js'
import { growthLandingPages, growthLandingRoutes } from './src/config/growthLandingPages.js'
import {
  buildProgrammaticConverterPath,
  convertProgrammaticValue,
  formatProgrammaticValue,
  programmaticConverters,
  programmaticConverterDescription,
  programmaticConverterQuickAnswer,
  programmaticConverterTitle,
} from './src/config/programmaticSeo.js'

function escapeAttribute(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}

function escapeHtml(value: string) {
  return escapeAttribute(value).replaceAll("'", '&#39;')
}

function renderStaticSeoContent(appId: Parameters<typeof getSeoContent>[0], lang: Parameters<typeof getSeoContent>[1], title: string) {
  const content = getSeoContent(appId, lang)
  const labels = getSeoCopy(lang)
  const faq = content.faqs.map((item) => `<details><summary>${escapeHtml(item.question)}</summary><p>${escapeHtml(item.answer)}</p></details>`).join('')
  return `<main class="seo-static-content" aria-label="${escapeAttribute(title)}"><h1>${escapeHtml(title)}</h1><p>${escapeHtml(content.intro)}</p><section><h2>${escapeHtml(labels.features)}</h2><ul>${content.benefits.map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul></section><section><h2>${escapeHtml(labels.steps)}</h2><ol>${content.steps.map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ol></section><section><h2>${escapeHtml(labels.note)}</h2><p>${escapeHtml(content.note)}</p></section><section><h2>${escapeHtml(labels.faq)}</h2>${faq}</section></main>`
}

function renderStaticSiteContent(title: string, description: string, lang: string) {
  const heading = lang === 'vi' ? 'Giới thiệu PureHub' : lang === 'zh' ? '关于 PureHub' : 'About PureHub'
  const next = lang === 'vi' ? 'Mở danh mục công cụ' : lang === 'zh' ? '浏览工具目录' : 'Browse the tool collection'
  return `<main class="seo-static-content" aria-label="${escapeAttribute(title)}"><h1>${escapeHtml(title)}</h1><p>${escapeHtml(description)}</p><section><h2>${heading}</h2><p>PureHub provides free, private-first tools for focused everyday workflows. Use the browser app now, install the PWA for quick access, or choose Android when native capabilities are useful.</p></section><a href="/${lang}/tools">${next}</a></main>`
}

function renderStaticProgrammaticContent(converter: (typeof programmaticConverters)[number], lang: Parameters<typeof programmaticConverterTitle>[1]) {
  const title = programmaticConverterTitle(converter, lang)
  const quickAnswer = programmaticConverterQuickAnswer(converter, lang)
  const labels = lang === 'vi'
    ? { table: 'Bảng quy đổi phổ biến', formula: 'Công thức', faq: 'Câu hỏi thường gặp', input: converter.from.vi, output: converter.to.vi }
    : lang === 'zh'
      ? { table: '常用换算表', formula: '计算公式', faq: '常见问题', input: converter.from.zh, output: converter.to.zh }
      : { table: 'Common conversion table', formula: 'Formula', faq: 'Frequently asked questions', input: converter.from.en, output: converter.to.en }
  const faqQuestion = lang === 'vi' ? `100 ${converter.from.vi} bằng bao nhiêu ${converter.to.vi}?` : lang === 'zh' ? `100${converter.from.zh}等于多少${converter.to.zh}？` : `How many ${converter.to.en} are 100 ${converter.from.en}?`
  const faqAnswer = lang === 'vi' ? `100 ${converter.from.vi} bằng ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.vi}.` : lang === 'zh' ? `100${converter.from.zh}等于${formatProgrammaticValue(convertProgrammaticValue(converter, 100))}${converter.to.zh}。` : `100 ${converter.from.en} equals ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.en}.`
  const rows = converter.values.map((value) => `<tr><td>${escapeHtml(formatProgrammaticValue(value))}</td><td>${escapeHtml(formatProgrammaticValue(convertProgrammaticValue(converter, value)))}</td></tr>`).join('')
  return `<main class="seo-static-content" aria-label="${escapeAttribute(title)}"><h1>${escapeHtml(title)}</h1><p>${escapeHtml(programmaticConverterDescription(converter, lang))}</p><p><strong>${escapeHtml(quickAnswer)}</strong></p><section><h2>${escapeHtml(labels.table)}</h2><table><thead><tr><th>${escapeHtml(labels.input)}</th><th>${escapeHtml(labels.output)}</th></tr></thead><tbody>${rows}</tbody></table></section><section><h2>${escapeHtml(labels.formula)}</h2><p><code>${escapeHtml(converter.formula[lang])}</code></p></section><section><h2>${escapeHtml(labels.faq)}</h2><details><summary>${escapeHtml(faqQuestion)}</summary><p>${escapeHtml(faqAnswer)}</p></details></section></main>`
}

function injectSeoHtml(
  source: string,
  page: { lang: string; title: string; description: string; canonicalUrl: string; alternates: Record<string, string>; schema: object; bodyHtml: string },
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
    .replace('<div id="root"></div>', page.bodyHtml)
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
        bodyHtml: string
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
            '@graph': [
              { '@type': 'WebApplication', name: meta.title, description: meta.description, url: canonicalUrl, applicationCategory: 'UtilitiesApplication', operatingSystem: 'Any', inLanguage: entry.lang, isAccessibleForFree: true, offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' } },
              { '@type': 'BreadcrumbList', itemListElement: [{ '@type': 'ListItem', position: 1, name: 'PureHub', item: SITE_ORIGIN }, { '@type': 'ListItem', position: 2, name: meta.title, item: canonicalUrl }] },
              { '@type': 'FAQPage', mainEntity: getSeoContent(entry.appId, entry.lang).faqs.map((faq) => ({ '@type': 'Question', name: faq.question, acceptedAnswer: { '@type': 'Answer', text: faq.answer } })) },
            ],
          },
          bodyHtml: renderStaticSeoContent(entry.appId, entry.lang, meta.title),
        })
      }

      for (const converter of programmaticConverters) {
        for (const lang of SEO_LANGUAGES) {
          const path = buildProgrammaticConverterPath(lang, converter.slug)
          const canonicalUrl = `${SITE_ORIGIN}${path}`
          const title = programmaticConverterTitle(converter, lang)
          const description = programmaticConverterDescription(converter, lang)
          const faqQuestion = lang === 'vi' ? `100 ${converter.from.vi} bằng bao nhiêu ${converter.to.vi}?` : lang === 'zh' ? `100${converter.from.zh}等于多少${converter.to.zh}？` : `How many ${converter.to.en} are 100 ${converter.from.en}?`
          const faqAnswer = lang === 'vi' ? `100 ${converter.from.vi} bằng ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.vi}.` : lang === 'zh' ? `100${converter.from.zh}等于${formatProgrammaticValue(convertProgrammaticValue(converter, 100))}${converter.to.zh}。` : `100 ${converter.from.en} equals ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.en}.`
          pages.push({
            path,
            lang,
            title,
            description,
            canonicalUrl,
            alternates: Object.fromEntries([
              ...SEO_LANGUAGES.map((alternateLang) => [alternateLang, `${SITE_ORIGIN}${buildProgrammaticConverterPath(alternateLang, converter.slug)}`]),
              ['x-default', `${SITE_ORIGIN}${buildProgrammaticConverterPath('en', converter.slug)}`],
            ]),
            schema: {
              '@context': 'https://schema.org',
              '@graph': [
                { '@type': 'WebPage', name: title, description, url: canonicalUrl, inLanguage: lang },
                { '@type': 'FAQPage', mainEntity: [{ '@type': 'Question', name: faqQuestion, acceptedAnswer: { '@type': 'Answer', text: faqAnswer } }] },
              ],
            },
            bodyHtml: renderStaticProgrammaticContent(converter, lang),
          })
        }
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
            bodyHtml: renderStaticSiteContent(meta.title, meta.description, lang),
          })
        }
      }

      for (const landing of Object.values(growthLandingPages)) {
        const canonicalUrl = `${SITE_ORIGIN}/en/${landing.slug}`
        pages.push({
          path: `/en/${landing.slug}`,
          lang: 'en',
          title: landing.title,
          description: landing.description,
          canonicalUrl,
          alternates: {},
          schema: {
            '@context': 'https://schema.org',
            '@graph': [
              { '@type': 'WebPage', name: landing.title, description: landing.description, url: canonicalUrl, inLanguage: 'en' },
              { '@type': 'FAQPage', mainEntity: landing.faqs.map((faq) => ({ '@type': 'Question', name: faq.question, acceptedAnswer: { '@type': 'Answer', text: faq.answer } })) },
            ],
          },
          bodyHtml: `<main class="seo-static-content" aria-label="${escapeAttribute(landing.title)}"><h1>${escapeHtml(landing.headline)}</h1><p>${escapeHtml(landing.lead)}</p><section><h2>How it works</h2><ol>${landing.steps.map((step) => `<li>${escapeHtml(step)}</li>`).join('')}</ol></section><section><h2>Important note</h2><p>${escapeHtml(landing.browserNote)}</p></section><section><h2>Questions people ask</h2>${landing.faqs.map((faq) => `<details><summary>${escapeHtml(faq.question)}</summary><p>${escapeHtml(faq.answer)}</p></details>`).join('')}</section></main>`,
        })
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
      dynamicRoutes: [...buildSeoSitemapPaths(), ...growthLandingRoutes, ...programmaticConverters.flatMap((converter) => SEO_LANGUAGES.map((lang) => buildProgrammaticConverterPath(lang, converter.slug)))],
      // Verification files are required for Search Console, but are not content
      // pages and must never be submitted for indexing.  Vercel's SPA fallback
      // serves the shell at the extensionless variant, which otherwise creates
      // an alternate-canonical entry in the generated sitemap.
      exclude: ['/', '/google11321854edbe5f72', '/google11321854edbe5f72.html'],
      readable: true,
      generateRobotsTxt: true,
    }),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      strategies: 'generateSW',
      includeAssets: ['favicon.ico', 'favicon-*.png', 'apple-touch-icon.png', 'og-image.png', 'icons/*.png'],
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

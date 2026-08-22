import { ArrowRight, CheckCircle2, Download, ShieldCheck } from 'lucide-react'
import { Helmet } from 'react-helmet-async'
import { Link, Navigate, useParams } from 'react-router-dom'
import { getGrowthLandingPage } from '../config/growthLandingPages'
import type { GrowthLandingId } from '../config/growthLandingPages'
import { SITE_ORIGIN } from '../config/seoMeta'
import { normalizeLocale } from '../i18n/locales'

export function GrowthLandingPage({ landingId }: { landingId: GrowthLandingId }) {
  const { lang } = useParams()
  const page = getGrowthLandingPage(landingId)
  const locale = normalizeLocale(lang)

  if (!page) return <Navigate to={`/${locale}/tools`} replace />
  if (locale !== 'en') return <Navigate to={`/en/${page.slug}`} replace />

  const canonicalUrl = `${SITE_ORIGIN}/en/${page.slug}`
  const toolUrl = `/en/${page.toolSlug}?utm_source=organic_landing&utm_campaign=${page.id}`
  const downloadUrl = `/en/download?utm_source=organic_landing&utm_campaign=${page.id}`
  const Icon = page.icon

  return (
    <article className="space-y-5">
      <Helmet htmlAttributes={{ lang: 'en' }}>
        <title>{page.title}</title>
        <meta name="description" content={page.description} />
        <meta name="keywords" content={page.keywords.join(', ')} />
        <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1" />
        <link rel="canonical" href={canonicalUrl} />
        <meta property="og:type" content="website" /><meta property="og:site_name" content="PureHub" />
        <meta property="og:title" content={page.title} /><meta property="og:description" content={page.description} /><meta property="og:url" content={canonicalUrl} />
        <meta name="twitter:card" content="summary_large_image" /><meta name="twitter:title" content={page.title} /><meta name="twitter:description" content={page.description} />
        <script type="application/ld+json">{JSON.stringify({ '@context': 'https://schema.org', '@type': 'FAQPage', mainEntity: page.faqs.map((faq) => ({ '@type': 'Question', name: faq.question, acceptedAnswer: { '@type': 'Answer', text: faq.answer } })) })}</script>
      </Helmet>

      <section className="hero-panel">
        <p className="eyebrow"><ShieldCheck className="size-4" />{page.eyebrow}</p>
        <div className="mt-4 grid gap-5 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="max-w-3xl text-3xl font-black tracking-tight text-slate-950 sm:text-4xl dark:text-white">{page.headline}</h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600 dark:text-slate-300">{page.lead}</p>
          </div>
          <div className="grid size-16 place-items-center rounded-[20px] bg-emerald-600 text-white shadow-lg shadow-emerald-700/20"><Icon className="size-8" /></div>
        </div>
        <div className="mt-5 flex flex-wrap gap-3">
          <Link className="primary-button" to={toolUrl}>{page.primaryCta}<ArrowRight className="size-4" /></Link>
          <Link className="secondary-button" to={downloadUrl}><Download className="size-4" />Get Android app</Link>
        </div>
        <p className="mt-3 text-xs leading-5 text-slate-500 dark:text-slate-400">Free to use. No ad wall. No mandatory account.</p>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <h2 className="text-xl font-bold text-slate-950 dark:text-white">A simple local workflow</h2>
        <ol className="mt-4 grid gap-3 sm:grid-cols-3">
          {page.steps.map((step, index) => <li key={step} className="rounded-[14px] bg-slate-500/7 p-3 text-sm leading-6 text-slate-600 dark:text-slate-300"><strong className="mb-2 flex size-6 items-center justify-center rounded-full bg-emerald-600 text-xs text-white">{index + 1}</strong>{step}</li>)}
        </ol>
      </section>

      <section className="rounded-[18px] border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950 dark:border-amber-800/70 dark:bg-amber-950/30 dark:text-amber-100">
        <strong className="flex items-center gap-2"><CheckCircle2 className="size-4" />Honest device note</strong>
        <p className="mt-1">{page.browserNote}</p>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <h2 className="text-xl font-bold text-slate-950 dark:text-white">Questions people ask</h2>
        <div className="mt-3 divide-y divide-slate-200 dark:divide-slate-700">
          {page.faqs.map((faq) => <div key={faq.question} className="py-4 first:pt-1"><h3 className="font-bold text-slate-950 dark:text-white">{faq.question}</h3><p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">{faq.answer}</p></div>)}
        </div>
      </section>

      <section className="flex flex-wrap items-center justify-between gap-3 rounded-[18px] bg-slate-950 p-5 text-white dark:bg-emerald-950">
        <div><h2 className="font-bold">Try the workflow before you install anything.</h2><p className="mt-1 text-sm text-slate-300">Open the PWA now, or use Android when native capabilities matter.</p></div>
        <Link className="primary-button bg-white text-emerald-800 hover:bg-emerald-50" to={toolUrl}>{page.primaryCta}<ArrowRight className="size-4" /></Link>
      </section>
    </article>
  )
}

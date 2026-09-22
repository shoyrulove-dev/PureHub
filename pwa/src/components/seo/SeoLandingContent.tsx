import { ArrowRight, CheckCircle2, Info, ListChecks, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getSeoContent, getSeoCopy } from '../../config/seoContent'
import { getSeoMetaByAppId, type SeoLanguage, type SeoMiniAppId } from '../../config/seoMeta'
import { buildCanonicalUrl } from '../../config/seoMeta'

const relatedTools: Partial<Record<SeoMiniAppId, SeoMiniAppId[]>> = {
  'ocr-text': ['doc-to-pdf', 'qr-studio', 'photo-privacy'],
  'doc-to-pdf': ['ocr-text', 'photo-privacy', 'file-studio'],
  'qr-studio': ['ocr-text', 'unit-converter', 'photo-privacy'],
  'unit-converter': ['expense-tracker', 'bill-splitter', 'decibel-meter'],
  'wifi-analyzer': ['compass', 'bubble-level', 'file-studio'],
}

export function SeoLandingContent({ appId, lang }: { appId: SeoMiniAppId; lang: SeoLanguage }) {
  const meta = getSeoMetaByAppId(appId, lang)
  const content = getSeoContent(appId, lang)
  const labels = getSeoCopy(lang)
  if (!meta) return null

  const related = (relatedTools[appId] ?? ['ocr-text', 'qr-studio', 'unit-converter'])
    .filter((id) => id !== appId)
    .slice(0, 3)

  return (
    <section className="seo-landing-content space-y-4" aria-label={meta.title}>
      <div className="app-surface rounded-[18px] p-5">
        <p className="text-sm leading-7 text-slate-600 dark:text-slate-300">{content.intro}</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          {content.benefits.map((benefit) => (
            <div key={benefit} className="rounded-[14px] bg-slate-500/7 p-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
              <CheckCircle2 className="mb-2 size-4 text-emerald-600" aria-hidden="true" />
              {benefit}
            </div>
          ))}
        </div>
      </div>

      <div className="app-surface rounded-[18px] p-5">
        <h2 className="flex items-center gap-2 text-xl font-bold text-slate-950 dark:text-white">
          <ListChecks className="size-5 text-emerald-600" aria-hidden="true" />{labels.steps}
        </h2>
        <ol className="mt-4 grid gap-3 sm:grid-cols-3">
          {content.steps.map((step, index) => (
            <li key={step} className="rounded-[14px] border border-slate-200 p-3 text-sm leading-6 text-slate-600 dark:border-slate-700 dark:text-slate-300">
              <strong className="mb-2 flex size-6 items-center justify-center rounded-full bg-emerald-600 text-xs text-white">{index + 1}</strong>
              {step}
            </li>
          ))}
        </ol>
      </div>

      <div className="rounded-[18px] border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950 dark:border-amber-800/70 dark:bg-amber-950/30 dark:text-amber-100">
        <p className="flex items-center gap-2 font-bold"><Info className="size-4" aria-hidden="true" />{labels.note}</p>
        <p className="mt-1">{content.note}</p>
      </div>

      <div className="app-surface rounded-[18px] p-5">
        <h2 className="flex items-center gap-2 text-xl font-bold text-slate-950 dark:text-white"><ShieldCheck className="size-5 text-emerald-600" aria-hidden="true" />{labels.faq}</h2>
        <div className="mt-3 divide-y divide-slate-200 dark:divide-slate-700">
          {content.faqs.map((faq) => (
            <details key={faq.question} className="py-3 first:pt-1">
              <summary className="cursor-pointer font-bold text-slate-950 dark:text-white">{faq.question}</summary>
              <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{faq.answer}</p>
            </details>
          ))}
        </div>
      </div>

      <nav aria-label={labels.related} className="rounded-[18px] bg-slate-950 p-5 text-white dark:bg-emerald-950">
        <h2 className="font-bold">{labels.related}</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {related.map((relatedId) => {
            const relatedMeta = getSeoMetaByAppId(relatedId, lang)
            if (!relatedMeta) return null
            return <Link key={relatedId} className="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-2 text-sm hover:bg-white/20" to={buildCanonicalUrl(lang, relatedMeta.slug)}>{relatedMeta.title}<ArrowRight className="size-3.5" /></Link>
          })}
        </div>
      </nav>
    </section>
  )
}

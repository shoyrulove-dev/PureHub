import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { Link, useParams } from 'react-router-dom'
import { ArrowRight, Calculator, CheckCircle2, Code2, Table2 } from 'lucide-react'
import {
  buildProgrammaticConverterPath,
  convertProgrammaticValue,
  formatProgrammaticValue,
  getProgrammaticConverter,
  programmaticConverterDescription,
  programmaticConverterQuickAnswer,
  programmaticConverterTitle,
} from '../config/programmaticSeo'
import { normalizeLocale } from '../i18n/locales'
import { buildCanonicalUrl, getSeoMetaByAppId, SITE_ORIGIN } from '../config/seoMeta'
import { AdsterraNativeBanner } from '../components/ads/AdsterraNativeBanner'

export function ProgrammaticConverterPage({ converterSlug }: { converterSlug?: string }) {
  const { lang, pairSlug } = useParams()
  const locale = normalizeLocale(lang)
  const seoLocale = locale === 'es' ? 'en' : locale
  const converter = getProgrammaticConverter(converterSlug ?? pairSlug ?? '')
  const [value, setValue] = useState('1')

  if (!converter) return null

  const title = programmaticConverterTitle(converter, seoLocale)
  const description = programmaticConverterDescription(converter, seoLocale)
  const result = formatProgrammaticValue(convertProgrammaticValue(converter, Number(value) || 0))
  const unitConverterMeta = getSeoMetaByAppId('unit-converter', seoLocale)
  const canonicalUrl = `${SITE_ORIGIN}${buildProgrammaticConverterPath(seoLocale, converter.slug)}`
  const quickAnswer = programmaticConverterQuickAnswer(converter, seoLocale)
  const labels = locale === 'vi'
    ? { table: 'Bảng quy đổi phổ biến', formula: 'Công thức', use: 'Dùng công cụ chuyển đổi', input: 'Giá trị', result: 'Kết quả', faq: 'Câu hỏi thường gặp', faqQ: `100 ${converter.from.vi} bằng bao nhiêu ${converter.to.vi}?`, faqA: `100 ${converter.from.vi} bằng ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.vi}.` }
    : locale === 'zh'
      ? { table: '常用换算表', formula: '计算公式', use: '打开完整转换工具', input: '数值', result: '结果', faq: '常见问题', faqQ: `100${converter.from.zh}等于多少${converter.to.zh}？`, faqA: `100${converter.from.zh}等于${formatProgrammaticValue(convertProgrammaticValue(converter, 100))}${converter.to.zh}。` }
      : { table: 'Common conversion table', formula: 'Formula', use: 'Open the full converter', input: 'Value', result: 'Result', faq: 'Frequently asked questions', faqQ: `How many ${converter.to.en} are 100 ${converter.from.en}?`, faqA: `100 ${converter.from.en} equals ${formatProgrammaticValue(convertProgrammaticValue(converter, 100))} ${converter.to.en}.` }

  const faq = [{ question: labels.faqQ, answer: labels.faqA }]

  return (
    <>
      <Helmet htmlAttributes={{ lang: locale }}>
        <title>{title} | PureHub</title>
        <meta name="description" content={description} />
        <meta name="robots" content="index, follow, max-image-preview:large" />
        <link rel="canonical" href={canonicalUrl} />
        <script type="application/ld+json">{JSON.stringify({
          '@context': 'https://schema.org',
          '@graph': [
            { '@type': 'WebPage', name: title, description, url: canonicalUrl, inLanguage: locale },
            { '@type': 'FAQPage', mainEntity: faq.map((item) => ({ '@type': 'Question', name: item.question, acceptedAnswer: { '@type': 'Answer', text: item.answer } })) },
          ],
        })}</script>
      </Helmet>
      <main className="space-y-4">
        <header className="app-surface rounded-[18px] p-5">
          <p className="eyebrow text-sky-700 dark:text-sky-300">PureHub Unit Converter</p>
          <h1 className="mt-1 text-2xl font-black text-slate-950 dark:text-white">{title}</h1>
          <p className="mt-2 text-sm leading-7 text-slate-600 dark:text-slate-300">{description}</p>
          <p className="mt-4 rounded-xl bg-sky-50 p-3 text-sm font-semibold leading-6 text-sky-950 dark:bg-sky-950/30 dark:text-sky-100">{quickAnswer}</p>
        </header>

        <section className="app-surface rounded-[18px] p-5" aria-label="Local converter">
          <h2 className="flex items-center gap-2 text-xl font-bold text-slate-950 dark:text-white"><Calculator className="size-5 text-sky-600" />{labels.use}</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-end">
            <label className="text-sm font-bold text-slate-700 dark:text-slate-200">{labels.input}<input value={value} onChange={(event) => setValue(event.target.value)} inputMode="decimal" className="mt-1 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-lg dark:border-slate-700 dark:bg-slate-900" /></label>
            <span className="pb-3 text-center text-sm font-bold text-slate-500">{converter.from[seoLocale]} → {converter.to[seoLocale]}</span>
            <div className="rounded-xl bg-slate-950 p-3 text-white"><span className="text-xs text-slate-400">{labels.result}</span><strong className="mt-1 block text-2xl tabular-nums">{result}</strong><span className="text-xs text-sky-300">{converter.to[seoLocale]}</span></div>
          </div>
        </section>

        <section className="app-surface rounded-[18px] p-5">
          <h2 className="flex items-center gap-2 text-xl font-bold text-slate-950 dark:text-white"><Table2 className="size-5 text-emerald-600" />{labels.table}</h2>
          <div className="mt-3 overflow-x-auto"><table className="w-full min-w-[420px] text-left text-sm"><thead><tr className="border-b border-slate-200 dark:border-slate-700"><th className="px-3 py-2">{converter.from[seoLocale]}</th><th className="px-3 py-2">{converter.to[seoLocale]}</th></tr></thead><tbody>{converter.values.map((item) => <tr key={item} className="border-b border-slate-100 dark:border-slate-800"><td className="px-3 py-2">{formatProgrammaticValue(item)}</td><td className="px-3 py-2 font-semibold">{formatProgrammaticValue(convertProgrammaticValue(converter, item))}</td></tr>)}</tbody></table></div>
        </section>

        <section className="app-surface rounded-[18px] p-5"><h2 className="flex items-center gap-2 text-xl font-bold text-slate-950 dark:text-white"><Code2 className="size-5 text-violet-600" />{labels.formula}</h2><code className="mt-3 block rounded-xl bg-slate-950 p-4 text-sm text-emerald-200">{converter.formula[seoLocale]}</code></section>

        <section className="app-surface rounded-[18px] p-5"><h2 className="text-xl font-bold text-slate-950 dark:text-white">{labels.faq}</h2>{faq.map((item) => <details key={item.question} className="mt-3 border-t border-slate-200 pt-3 dark:border-slate-700"><summary className="cursor-pointer font-bold">{item.question}</summary><p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{item.answer}</p></details>)}</section>

        <AdsterraNativeBanner />

        {unitConverterMeta ? <Link to={buildCanonicalUrl(seoLocale, unitConverterMeta.slug)} className="flex items-center justify-between rounded-[18px] bg-slate-950 p-4 font-bold text-white dark:bg-emerald-950"><span className="flex items-center gap-2"><CheckCircle2 className="size-5 text-emerald-300" />{labels.use}</span><ArrowRight className="size-5" /></Link> : null}
      </main>
    </>
  )
}

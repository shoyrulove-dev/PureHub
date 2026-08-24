import { ArrowRight, CheckCircle2, Clock3, Search, Trash2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MINI_APP_BY_ID, TAB_BY_ID } from '../features/catalog/tabs'
import { buildMiniAppPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'
import { clearToolResults, removeToolResult, useToolResults } from '../lib/result-center'

export function ResultsPage() {
  const { t } = useTranslation()
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const results = useToolResults()
  const [query, setQuery] = useState('')
  const visible = useMemo(() => results.filter((result) => {
    const tool = MINI_APP_BY_ID.get(result.miniAppId)
    return `${result.headline} ${tool ? t(tool.titleKey) : result.miniAppId}`.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())
  }), [query, results, t])

  return <section className="space-y-5">
    <header className="rounded-[24px] bg-gradient-to-br from-emerald-600 to-cyan-700 p-5 text-white shadow-lg">
      <p className="text-[11px] font-black uppercase tracking-[.2em] text-emerald-100">Flagship 3.0</p>
      <h1 className="mt-2 text-3xl font-black">Result Center</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-emerald-50">A private activity index for completed workflows. PureHub stores only the tool, outcome label and time here—not QR contents, passwords, amounts or files.</p>
      <div className="mt-4 grid grid-cols-2 gap-2"><Metric label="Recent results" value={String(results.length)} /><Metric label="Stored content" value="Metadata only" /></div>
    </header>

    <div className="flex gap-2">
      <label className="search-box flex-1"><Search className="size-5 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search results or tools" /></label>
      <button type="button" disabled={!results.length} onClick={() => { if (window.confirm('Clear the Result Center history? Tool data and exported files will not be deleted.')) clearToolResults() }} className="grid size-12 shrink-0 place-items-center rounded-[14px] border border-rose-200 text-rose-600 disabled:opacity-40 dark:border-rose-900" aria-label="Clear Result Center"><Trash2 className="size-5" /></button>
    </div>

    {visible.length ? <div className="space-y-2">{visible.map((result) => {
      const tool = MINI_APP_BY_ID.get(result.miniAppId)
      if (!tool) return null
      const tab = TAB_BY_ID.get(tool.tabId)
      const Icon = tool.icon
      return <article key={result.id} className="flex items-center gap-3 rounded-[18px] border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900">
        <span className={`tool-card__icon ${tab?.accentClass ?? 'text-emerald-500'}`}><Icon className="size-5" /></span>
        <div className="min-w-0 flex-1"><strong className="block truncate text-sm text-slate-950 dark:text-white">{result.headline}</strong><span className="mt-1 flex items-center gap-1 text-xs text-slate-500"><Clock3 className="size-3" />{t(tool.titleKey)} · {new Date(result.createdAt).toLocaleString()}</span></div>
        <Link to={buildMiniAppPath(locale, tool.id)} className="grid size-10 place-items-center rounded-xl text-emerald-700" aria-label={`Open ${t(tool.titleKey)}`}><ArrowRight className="size-4" /></Link>
        <button type="button" onClick={() => removeToolResult(result.id)} className="grid size-10 place-items-center rounded-xl text-rose-600" aria-label="Remove result"><Trash2 className="size-4" /></button>
      </article>
    })}</div> : <div className="grid min-h-52 place-items-center rounded-[22px] border border-dashed border-slate-300 p-6 text-center dark:border-slate-700"><div><CheckCircle2 className="mx-auto size-8 text-emerald-500" /><strong className="mt-3 block">{results.length ? 'No matching result' : 'Complete a real task'}</strong><p className="mt-1 text-sm text-slate-500">Successful tool workflows will appear here automatically.</p></div></div>}
  </section>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl bg-white/12 p-3"><span className="block text-xs text-emerald-100">{label}</span><strong className="mt-1 block text-lg">{value}</strong></div> }

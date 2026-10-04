import { ArrowRight, CheckCircle2, Clock3, Search, Trash2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MINI_APP_BY_ID, TAB_BY_ID } from '../features/catalog/tabs'
import { buildMiniAppPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'
import { clearToolResults, removeToolResult, useToolResults } from '../lib/result-center'

const RESULT_COPY = {
  en: { eyebrow: 'Flagship 3.0', title: 'Result Center', intro: 'A private activity index for completed workflows. Only the tool, outcome label and time are stored—not QR contents, passwords, amounts or files.', recent: 'Recent results', stored: 'Stored content', metadata: 'Metadata only', search: 'Search results or tools', clearConfirm: 'Clear Result Center history? Tool data and exported files will not be deleted.', clear: 'Clear Result Center', open: 'Open', remove: 'Remove result', noMatch: 'No matching result', empty: 'Complete a real task', emptyHint: 'Successful tool workflows will appear here automatically.' },
  vi: { eyebrow: 'Tính năng chính 3.0', title: 'Trung tâm kết quả', intro: 'Danh mục hoạt động riêng tư cho các quy trình đã hoàn tất. Chỉ tên công cụ, nhãn kết quả và thời gian được lưu—không lưu nội dung QR, mật khẩu, số tiền hay tệp.', recent: 'Kết quả gần đây', stored: 'Nội dung đã lưu', metadata: 'Chỉ siêu dữ liệu', search: 'Tìm kết quả hoặc công cụ', clearConfirm: 'Xóa lịch sử Trung tâm kết quả? Dữ liệu công cụ và tệp đã xuất sẽ không bị xóa.', clear: 'Xóa Trung tâm kết quả', open: 'Mở', remove: 'Xóa kết quả', noMatch: 'Không có kết quả phù hợp', empty: 'Hoàn tất một tác vụ thật', emptyHint: 'Quy trình công cụ thành công sẽ tự động xuất hiện tại đây.' },
  zh: { eyebrow: '旗舰功能 3.0', title: '结果中心', intro: '已完成操作的私密索引。这里只保存工具、结果标签和时间，不保存二维码内容、密码、金额或文件。', recent: '最近结果', stored: '保存内容', metadata: '仅元数据', search: '搜索结果或工具', clearConfirm: '清除结果中心历史记录？工具数据和导出文件不会被删除。', clear: '清除结果中心', open: '打开', remove: '删除结果', noMatch: '没有匹配结果', empty: '完成一个实际任务', emptyHint: '成功完成的工具操作会自动显示在这里。' },
  es: { eyebrow: 'Función principal 3.0', title: 'Centro de resultados', intro: 'Índice privado de flujos completados. Solo guarda la herramienta, la etiqueta del resultado y la hora; nunca contenidos QR, contraseñas, importes ni archivos.', recent: 'Resultados recientes', stored: 'Contenido guardado', metadata: 'Solo metadatos', search: 'Buscar resultados o herramientas', clearConfirm: '¿Borrar el historial del Centro de resultados? No se eliminarán los datos de herramientas ni los archivos exportados.', clear: 'Borrar Centro de resultados', open: 'Abrir', remove: 'Eliminar resultado', noMatch: 'No hay resultados coincidentes', empty: 'Completa una tarea real', emptyHint: 'Los flujos completados aparecerán aquí automáticamente.' },
} as const

export function ResultsPage() {
  const { t } = useTranslation()
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const copy = RESULT_COPY[locale]
  const results = useToolResults()
  const [query, setQuery] = useState('')
  const visible = useMemo(() => results.filter((result) => {
    const tool = MINI_APP_BY_ID.get(result.miniAppId)
    return `${result.headline} ${tool ? t(tool.titleKey) : result.miniAppId}`.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())
  }), [query, results, t])

  return <section className="space-y-5">
    <header className="rounded-[24px] bg-gradient-to-br from-emerald-600 to-cyan-700 p-5 text-white shadow-lg">
      <p className="text-[11px] font-black uppercase tracking-[.2em] text-emerald-100">{copy.eyebrow}</p>
      <h1 className="mt-2 text-3xl font-black">{copy.title}</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-emerald-50">{copy.intro}</p>
      <div className="mt-4 grid grid-cols-2 gap-2"><Metric label={copy.recent} value={String(results.length)} /><Metric label={copy.stored} value={copy.metadata} /></div>
    </header>

    <div className="flex gap-2">
      <label className="search-box flex-1"><Search className="size-5 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={copy.search} /></label>
      <button type="button" disabled={!results.length} onClick={() => { if (window.confirm(copy.clearConfirm)) clearToolResults() }} className="grid size-12 shrink-0 place-items-center rounded-[14px] border border-rose-200 text-rose-600 disabled:opacity-40 dark:border-rose-900" aria-label={copy.clear}><Trash2 className="size-5" /></button>
    </div>

    {visible.length ? <div className="space-y-2">{visible.map((result) => {
      const tool = MINI_APP_BY_ID.get(result.miniAppId)
      if (!tool) return null
      const tab = TAB_BY_ID.get(tool.tabId)
      const Icon = tool.icon
      return <article key={result.id} className="flex items-center gap-3 rounded-[18px] border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900">
        <span className={`tool-card__icon ${tab?.accentClass ?? 'text-emerald-500'}`}><Icon className="size-5" /></span>
        <div className="min-w-0 flex-1"><strong className="block truncate text-sm text-slate-950 dark:text-white">{result.headline}</strong><span className="mt-1 flex items-center gap-1 text-xs text-slate-500"><Clock3 className="size-3" />{t(tool.titleKey)} · {new Date(result.createdAt).toLocaleString()}</span></div>
        <Link to={buildMiniAppPath(locale, tool.id)} className="grid size-10 place-items-center rounded-xl text-emerald-700" aria-label={`${copy.open} ${t(tool.titleKey)}`}><ArrowRight className="size-4" /></Link>
        <button type="button" onClick={() => removeToolResult(result.id)} className="grid size-10 place-items-center rounded-xl text-rose-600" aria-label={copy.remove}><Trash2 className="size-4" /></button>
      </article>
    })}</div> : <div className="grid min-h-52 place-items-center rounded-[22px] border border-dashed border-slate-300 p-6 text-center dark:border-slate-700"><div><CheckCircle2 className="mx-auto size-8 text-emerald-500" /><strong className="mt-3 block">{results.length ? copy.noMatch : copy.empty}</strong><p className="mt-1 text-sm text-slate-500">{copy.emptyHint}</p></div></div>}
  </section>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-2xl bg-white/12 p-3"><span className="block text-xs text-emerald-100">{label}</span><strong className="mt-1 block text-lg">{value}</strong></div> }

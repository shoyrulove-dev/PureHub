import { ArrowRight, Layers3 } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { MINI_APP_BY_ID, type MiniAppId } from '../../features/catalog/tabs'
import { WORKSPACE_BY_TOOL, workspaceText } from '../../features/catalog/workspaces'
import { normalizeLocale } from '../../i18n/locales'
import { buildMiniAppPath } from '../../i18n/routing'
import { useTranslation } from 'react-i18next'

export function WorkspaceNavigator({ miniAppId }: { miniAppId: MiniAppId }) {
  const { lang } = useParams()
  const { t } = useTranslation()
  const locale = normalizeLocale(lang)
  const workspace = WORKSPACE_BY_TOOL.get(miniAppId)
  if (!workspace || workspace.toolIds.length < 2) return null

  const related = workspace.toolIds
    .filter((id) => id !== miniAppId)
    .map((id) => MINI_APP_BY_ID.get(id))
    .filter((tool) => Boolean(tool))
    .slice(0, 4)

  return (
    <aside className={`rounded-[16px] border border-slate-500/10 bg-gradient-to-r ${workspace.surfaceClass} p-3`} aria-label="Related workflow tools">
      <div className="flex items-center gap-2">
        <Layers3 className={`size-4 ${workspace.accentClass}`} />
        <div className="min-w-0 flex-1">
          <span className="block text-[10px] font-black uppercase tracking-[.16em] text-slate-500">Workspace</span>
          <strong className="block text-sm text-slate-950 dark:text-white">{workspaceText(workspace.title, locale)}</strong>
        </div>
      </div>
      <div className="mt-2 flex gap-2 overflow-x-auto pb-0.5">
        {related.map((tool) => {
          if (!tool) return null
          const Icon = tool.icon
          return <Link key={tool.id} to={buildMiniAppPath(locale, tool.id)} className="flex min-h-10 shrink-0 items-center gap-1.5 rounded-xl border border-white/60 bg-white/80 px-3 text-xs font-bold text-slate-700 shadow-sm transition hover:-translate-y-0.5 dark:border-slate-700 dark:bg-slate-900/80 dark:text-slate-200"><Icon className="size-3.5" />{t(tool.titleKey)}<ArrowRight className="size-3" /></Link>
        })}
      </div>
    </aside>
  )
}

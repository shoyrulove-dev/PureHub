import { ArrowRight, Heart, Search, ShieldCheck, Sparkles, WifiOff } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  MINI_APP_BY_ID,
  MINI_APP_ITEMS,
  TAB_BY_ID,
  TAB_ITEMS,
  type MiniAppDefinition,
  type MiniAppId,
  type TabId,
} from '../../features/catalog/tabs'
import { buildMiniAppPath, buildTabPath } from '../../i18n/routing'
import { normalizeLocale } from '../../i18n/locales'
import { useToolPreferences } from '../../lib/preferences'
import { matchesToolSearch } from '../../features/catalog/search'
import { WORKSPACE_ITEMS, workspaceText } from '../../features/catalog/workspaces'

const quickIds: MiniAppId[] = ['qr-studio', 'ocr-text', 'bubble-level', 'wifi-analyzer']

function ToolCard({
  tool,
  favorite,
  onFavorite,
}: {
  tool: MiniAppDefinition
  favorite: boolean
  onFavorite: () => void
}) {
  const { t } = useTranslation()
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const tab = TAB_BY_ID.get(tool.tabId)
  const Icon = tool.icon

  return (
    <article className="tool-card group">
      <Link to={buildMiniAppPath(locale, tool.id)} className="tool-card__link">
        <span className={`tool-card__icon ${tab?.accentClass ?? 'text-emerald-600'}`}>
          <Icon className="size-5" strokeWidth={2} />
        </span>
        <span className="min-w-0 flex-1">
          <span className="flex flex-wrap items-center gap-2 font-semibold text-slate-900 dark:text-white">
            {t(tool.titleKey)}
            {tool.flagship ? <span title="Flagship mini-app" className="grid size-5 place-items-center rounded-full bg-violet-500/10 text-violet-700 dark:text-violet-300"><Sparkles className="size-3" /><span className="sr-only">Flagship</span></span> : null}
          </span>
          <span className="mt-0.5 line-clamp-1 block text-xs leading-5 text-slate-500 dark:text-slate-400">
            {t(tool.summaryKey)}
          </span>
        </span>
        <ArrowRight className="mt-1 size-4 shrink-0 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-emerald-600 dark:text-slate-600" />
      </Link>
      <button
        type="button"
        className="tool-card__favorite"
        aria-label={favorite ? 'Remove from favorites' : 'Add to favorites'}
        aria-pressed={favorite}
        onClick={onFavorite}
      >
        <Heart className={`size-4 ${favorite ? 'fill-rose-500 text-rose-500' : 'text-slate-400'}`} />
      </button>
    </article>
  )
}

export function Dashboard() {
  const { t } = useTranslation()
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const { favorites, recents, toggleFavorite } = useToolPreferences()
  const [query, setQuery] = useState('')
  const [activeTab, setActiveTab] = useState<TabId | 'all'>('all')

  const filteredTools = useMemo(() => {
    return MINI_APP_ITEMS.filter((tool) => {
      const matchesTab = activeTab === 'all' || tool.tabId === activeTab
      return matchesTab && matchesToolSearch(tool, t(tool.titleKey), t(tool.summaryKey), query)
    })
  }, [activeTab, query, t])

  const favoriteTools = favorites
    .map((id) => MINI_APP_BY_ID.get(id))
    .filter((tool): tool is MiniAppDefinition => Boolean(tool))
  const recentTools = recents
    .map((id) => MINI_APP_BY_ID.get(id))
    .filter((tool): tool is MiniAppDefinition => Boolean(tool))
  const quickTools = quickIds
    .map((id) => MINI_APP_BY_ID.get(id))
    .filter((tool): tool is MiniAppDefinition => Boolean(tool))
  const flagshipTools = quickIds
    .map((id) => MINI_APP_BY_ID.get(id))
    .filter((tool): tool is MiniAppDefinition => Boolean(tool))

  return (
    <section className="space-y-6">
      <div className="hero-panel">
        <div className="max-w-2xl">
          <span className="eyebrow">
            <Sparkles className="size-3.5" />
            {t('app.phaseShell')}
          </span>
          <h1 className="mt-3 text-3xl font-bold tracking-[-0.035em] text-slate-950 sm:text-4xl dark:text-white">
            PureHub
          </h1>
          <p className="mt-1.5 max-w-xl text-sm leading-6 text-slate-600 dark:text-slate-300">
            {t('app.freePromise')}
          </p>
          <label className="search-box mt-4">
            <Search className="size-5 text-slate-400" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t('app.searchPlaceholder')}
              autoComplete="off"
            />
          </label>
        </div>

        <div className="mt-4 grid grid-cols-3 gap-2">
          <div className="promise-card">
            <WifiOff className="size-5 text-emerald-600 dark:text-emerald-400" />
            <div><strong>Offline</strong><span className="hidden sm:block">{t('dashboard.offline')}</span></div>
          </div>
          <div className="promise-card">
            <ShieldCheck className="size-5 text-sky-600 dark:text-sky-400" />
            <div><strong>Private</strong><span className="hidden sm:block">{t('dashboard.onDevice')}</span></div>
          </div>
          <div className="promise-card">
            <Heart className="size-5 text-rose-500" />
            <div><strong>Open</strong><span className="hidden sm:block">Community built</span></div>
          </div>
        </div>
      </div>

      {!query && favoriteTools.length > 0 ? (
        <ToolSection
          title={t('app.favorites')}
          tools={favoriteTools}
          favorites={favorites}
          onFavorite={toggleFavorite}
        />
      ) : null}

      {!query ? <WorkspaceSection /> : null}

      {!query ? (
        <div className="rounded-[18px] border border-violet-200 bg-violet-50/70 p-3.5 dark:border-violet-500/20 dark:bg-violet-500/5">
          <p className="eyebrow text-violet-700 dark:text-violet-300">Improve with us</p>
          <h2 className="mt-1 text-lg font-bold text-slate-950 dark:text-white">26 flagship tools, shaped by real use</h2>
          <p className="mt-0.5 text-xs leading-5 text-slate-600 dark:text-slate-400">Start with a favorite, then tell us what should feel simpler.</p>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            {flagshipTools.map((tool) => <ToolCard key={tool.id} tool={tool} favorite={favorites.includes(tool.id)} onFavorite={() => toggleFavorite(tool.id)} />)}
          </div>
          <Link to={`/${locale}/community#early-testers`} className="text-link mt-3">Join testers <ArrowRight className="size-3.5" /></Link>
        </div>
      ) : null}

      {!query && recentTools.length > 0 ? (
        <ToolSection
          title={t('app.recent')}
          tools={recentTools}
          favorites={favorites}
          onFavorite={toggleFavorite}
        />
      ) : null}

      {!query && recentTools.length === 0 ? (
        <ToolSection
          title={t('dashboard.quickAccess')}
          tools={quickTools}
          favorites={favorites}
          onFavorite={toggleFavorite}
        />
      ) : null}

      <section>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="eyebrow text-emerald-700 dark:text-emerald-300">{t('app.miniAppMap')}</p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-950 dark:text-white">
              {query ? `${filteredTools.length} results` : t('app.allTools')}
            </h2>
          </div>
          <div className="flex max-w-full gap-2 overflow-x-auto pb-1">
            <button
              type="button"
              className={`filter-chip ${activeTab === 'all' ? 'filter-chip--active' : ''}`}
              onClick={() => setActiveTab('all')}
            >
              All
            </button>
            {TAB_ITEMS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                className={`filter-chip ${activeTab === tab.id ? 'filter-chip--active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                {t(tab.shortLabelKey)}
              </button>
            ))}
          </div>
        </div>

        {filteredTools.length ? (
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            {filteredTools.map((tool) => (
              <ToolCard
                key={tool.id}
                tool={tool}
                favorite={favorites.includes(tool.id)}
                onFavorite={() => toggleFavorite(tool.id)}
              />
            ))}
          </div>
        ) : (
          <div className="empty-state mt-5">
            <Search className="size-6" />
            <p>No tool matched your search.</p>
          </div>
        )}

        <div className="mt-6 flex flex-wrap gap-2">
          {TAB_ITEMS.map((tab) => (
            <Link key={tab.id} to={buildTabPath(locale, tab.id)} className="text-link">
              {t(tab.labelKey)} <ArrowRight className="size-3.5" />
            </Link>
          ))}
        </div>
      </section>
    </section>
  )
}

function WorkspaceSection() {
  const { lang } = useParams()
  const { t } = useTranslation()
  const locale = normalizeLocale(lang)

  return (
    <section>
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="eyebrow text-violet-700 dark:text-violet-300">Flagship 4.0</p>
          <h2 className="mt-1 text-xl font-bold text-slate-950 dark:text-white">Choose a workspace</h2>
          <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">Start with a goal. PureHub connects the next useful action without uploading your work.</p>
        </div>
      </div>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        {WORKSPACE_ITEMS.map((workspace) => {
          const Icon = workspace.icon
          const featured = MINI_APP_BY_ID.get(workspace.featuredToolId)
          if (!featured) return null
          return (
            <article key={workspace.id} className={`group rounded-[20px] border border-slate-500/10 bg-gradient-to-br ${workspace.surfaceClass} p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md`}>
              <div className="flex items-start gap-3">
                <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-white/85 shadow-sm dark:bg-slate-900/85"><Icon className={`size-5 ${workspace.accentClass}`} /></span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2"><h3 className="font-black text-slate-950 dark:text-white">{workspaceText(workspace.title, locale)}</h3><span className="rounded-full bg-white/75 px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-slate-500 dark:bg-slate-900/75">{workspace.toolIds.length} tools</span></div>
                  <p className="mt-1 text-xs leading-5 text-slate-600 dark:text-slate-300">{workspaceText(workspace.description, locale)}</p>
                </div>
              </div>
              <div className="mt-3 flex items-center justify-between gap-2 border-t border-slate-500/10 pt-3">
                <span className="truncate text-xs font-semibold text-slate-500">Start with {t(featured.titleKey)}</span>
                <Link to={buildMiniAppPath(locale, featured.id)} className={`flex min-h-10 shrink-0 items-center gap-1 rounded-xl bg-white/85 px-3 text-xs font-black shadow-sm dark:bg-slate-900/85 ${workspace.accentClass}`}>Open <ArrowRight className="size-3.5 transition group-hover:translate-x-0.5" /></Link>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}

function ToolSection({
  title,
  tools,
  favorites,
  onFavorite,
}: {
  title: string
  tools: MiniAppDefinition[]
  favorites: MiniAppId[]
  onFavorite: (id: MiniAppId) => void
}) {
  return (
    <section>
      <h2 className="text-lg font-bold text-slate-950 dark:text-white">{title}</h2>
      <div className="mt-2 grid gap-2 sm:grid-cols-2">
        {tools.map((tool) => (
          <ToolCard
            key={tool.id}
            tool={tool}
            favorite={favorites.includes(tool.id)}
            onFavorite={() => onFavorite(tool.id)}
          />
        ))}
      </div>
    </section>
  )
}

import { Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { MINI_APP_ITEMS, TAB_ITEMS } from '../features/catalog/tabs'
import { normalizeLocale } from '../i18n/locales'
import { buildMiniAppPath } from '../i18n/routing'

export function ToolsPage() {
  const { t } = useTranslation()
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const [query, setQuery] = useState('')
  const normalizedQuery = query.trim().toLocaleLowerCase()
  const visible = useMemo(
    () =>
      MINI_APP_ITEMS.filter((tool) =>
        `${t(tool.titleKey)} ${t(tool.summaryKey)}`.toLocaleLowerCase().includes(normalizedQuery),
      ),
    [normalizedQuery, t],
  )

  return (
    <section className="space-y-7">
      <div>
        <p className="eyebrow">{t('app.phaseShell')}</p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-white">
          {t('app.allTools')}
        </h1>
        <p className="mt-2 max-w-2xl text-slate-600 dark:text-slate-400">{t('app.phaseDeferred')}</p>
        <label className="search-box mt-5">
          <Search className="size-5 text-slate-400" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t('app.searchPlaceholder')}
          />
        </label>
      </div>

      {TAB_ITEMS.map((tab) => {
        const tools = visible.filter((tool) => tool.tabId === tab.id)
        if (!tools.length) return null
        return (
          <section key={tab.id}>
            <div className="mb-3">
              <h2 className="text-lg font-bold text-slate-950 dark:text-white">{t(tab.labelKey)}</h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t(tab.descriptionKey)}</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {tools.map((tool) => {
                const Icon = tool.icon
                return (
                  <Link key={tool.id} to={buildMiniAppPath(locale, tool.id)} className="tool-card__link tool-card">
                    <span className={`tool-card__icon ${tab.accentClass}`}>
                      <Icon className="size-5" />
                    </span>
                    <span className="min-w-0">
                      <strong className="block text-slate-900 dark:text-white">{t(tool.titleKey)}</strong>
                      <span className="mt-1 block text-sm leading-5 text-slate-500 dark:text-slate-400">
                        {t(tool.summaryKey)}
                      </span>
                    </span>
                  </Link>
                )
              })}
            </div>
          </section>
        )
      })}
    </section>
  )
}

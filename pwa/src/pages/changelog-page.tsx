import { useEffect, useState } from 'react'
import { ExternalLink, History, Rss } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { normalizeLocale } from '../i18n/locales'

const CHANGELOG_COPY = {
  en: { eyebrow: 'Release history', title: 'Changelog', intro: 'Every public Android release, its changes, and its original GitHub record.', rss: 'RSS feed', preview: 'Preview', notes: 'Full release notes', empty: 'Public release notes will appear here with the first signed Android preview.' },
  vi: { eyebrow: 'Lịch sử phát hành', title: 'Nhật ký thay đổi', intro: 'Mọi bản Android công khai, nội dung thay đổi và bản ghi GitHub gốc.', rss: 'Nguồn RSS', preview: 'Bản thử', notes: 'Xem ghi chú đầy đủ', empty: 'Ghi chú phát hành công khai sẽ xuất hiện sau khi có bản Android đã ký.' },
  zh: { eyebrow: '发布历史', title: '更新日志', intro: '查看每个公开 Android 版本、变更内容及原始 GitHub 记录。', rss: 'RSS 订阅', preview: '预览版', notes: '查看完整说明', empty: '首个签名 Android 预览版发布后，公开说明会显示在这里。' },
  es: { eyebrow: 'Historial de versiones', title: 'Registro de cambios', intro: 'Todas las versiones públicas de Android, sus cambios y el registro original de GitHub.', rss: 'Canal RSS', preview: 'Preliminar', notes: 'Notas completas', empty: 'Las notas públicas aparecerán aquí con la primera versión Android firmada.' },
} as const

type Release = {
  release_id: string
  version: string
  title: string
  summary: string
  changelog: string
  github_url: string
  prerelease: boolean
  published_at?: string
}

export function ChangelogPage() {
  const locale = normalizeLocale(useParams().lang)
  const copy = CHANGELOG_COPY[locale]
  const [items, setItems] = useState<Release[]>([])

  useEffect(() => {
    const controller = new AbortController()
    fetch('/public-api/releases', { signal: controller.signal })
      .then((response) => response.ok ? response.json() : Promise.reject(new Error('Release service unavailable')))
      .then((payload: { items?: Release[] }) => setItems(payload.items ?? []))
      .catch(() => undefined)
    return () => controller.abort()
  }, [])

  return (
    <section className="space-y-6">
      <div className="hero-panel">
        <span className="eyebrow"><History className="size-4" /> {copy.eyebrow}</span>
        <div className="mt-4 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">{copy.title}</h1>
            <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
              {copy.intro}
            </p>
          </div>
          <a className="secondary-button" href="/public-api/releases.xml" target="_blank" rel="noreferrer">
            <Rss className="size-4" /> {copy.rss}
          </a>
        </div>
      </div>

      <div className="space-y-3">
        {items.map((release) => (
          <article key={release.release_id} className="app-surface rounded-[18px] p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <span className="text-xs font-bold uppercase tracking-[0.16em] text-emerald-600 dark:text-emerald-300">
                  v{release.version} {release.prerelease ? `· ${copy.preview}` : ''}
                </span>
                <h2 className="mt-2 text-xl font-bold text-slate-950 dark:text-white">{release.title}</h2>
              </div>
              {release.published_at && (
                <time className="text-xs text-slate-500" dateTime={release.published_at}>
                  {new Intl.DateTimeFormat(locale === 'zh' ? 'zh-CN' : locale, { dateStyle: 'medium' }).format(new Date(release.published_at))}
                </time>
              )}
            </div>
            <p className="mt-3 leading-7 text-slate-500 dark:text-slate-400">{release.summary}</p>
            {release.changelog && (
              <div className="mt-4 whitespace-pre-wrap rounded-[16px] bg-slate-500/7 p-4 text-sm leading-6 text-slate-600 dark:text-slate-300">
                {release.changelog}
              </div>
            )}
            {release.github_url && (
              <a className="text-link mt-4" href={release.github_url} target="_blank" rel="noreferrer">
                {copy.notes} <ExternalLink className="size-4" />
              </a>
            )}
          </article>
        ))}
        {!items.length && (
          <div className="app-surface rounded-[18px] p-6 text-slate-500 dark:text-slate-400">
            {copy.empty}
          </div>
        )}
      </div>
    </section>
  )
}

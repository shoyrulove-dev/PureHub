import { useEffect, useState } from 'react'
import { ExternalLink, History, Rss } from 'lucide-react'

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
        <span className="eyebrow"><History className="size-4" /> Release history</span>
        <div className="mt-4 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">Changelog</h1>
            <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
              Every public Android release, its changes, and its original GitHub record.
            </p>
          </div>
          <a className="secondary-button" href="/public-api/releases.xml" target="_blank" rel="noreferrer">
            <Rss className="size-4" /> RSS feed
          </a>
        </div>
      </div>

      <div className="space-y-3">
        {items.map((release) => (
          <article key={release.release_id} className="app-surface rounded-[18px] p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <span className="text-xs font-bold uppercase tracking-[0.16em] text-emerald-600 dark:text-emerald-300">
                  v{release.version} {release.prerelease ? '· Preview' : ''}
                </span>
                <h2 className="mt-2 text-xl font-bold text-slate-950 dark:text-white">{release.title}</h2>
              </div>
              {release.published_at && (
                <time className="text-xs text-slate-500" dateTime={release.published_at}>
                  {new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(release.published_at))}
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
                Full release notes <ExternalLink className="size-4" />
              </a>
            )}
          </article>
        ))}
        {!items.length && (
          <div className="app-surface rounded-[18px] p-6 text-slate-500 dark:text-slate-400">
            Public release notes will appear here with the first signed Android preview.
          </div>
        )}
      </div>
    </section>
  )
}

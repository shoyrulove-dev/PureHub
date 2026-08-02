import { useEffect, useState } from 'react'
import { CheckCircle2, Code2, Download, FileArchive, RefreshCw, ShieldCheck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { normalizeLocale } from '../i18n/locales'
import { trackJourneyEvent } from '../lib/community-api'

type Release = {
  release_id: string
  version: string
  title: string
  summary: string
  github_url: string
  apk_url: string
  aab_url: string
  sha256: string
  prerelease: boolean
  published_at?: string
}

async function loadReleases(signal: AbortSignal): Promise<Release[]> {
  const response = await fetch('/public-api/releases', { signal })
  if (!response.ok) throw new Error('Release service is temporarily unavailable.')
  const payload = (await response.json()) as { items?: Release[] }
  return payload.items ?? []
}

export function DownloadPage() {
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const [releases, setReleases] = useState<Release[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    loadReleases(controller.signal)
      .then(setReleases)
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : 'Unable to load releases.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])

  const latest = releases[0]

  return (
    <section className="space-y-6">
      <div className="hero-panel">
        <span className="eyebrow"><Download className="size-4" /> Android preview</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          PureHub for Android
        </h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
          Free, ad-free, and open source. Downloads are published through GitHub with a SHA-256 checksum so every build can be verified.
        </p>
      </div>

      {loading && (
        <div className="app-surface flex items-center gap-3 rounded-[18px] p-5 text-slate-500">
          <RefreshCw className="size-5 animate-spin" /> Checking the latest signed build…
        </div>
      )}

      {!loading && error && (
        <div className="app-surface rounded-[18px] p-5">
          <p className="font-semibold text-rose-600 dark:text-rose-300">{error}</p>
          <a className="text-link mt-3" href="https://github.com/shoyrulove-dev/PureHub/releases" target="_blank" rel="noreferrer">
            <Code2 className="size-4" /> Check GitHub releases
          </a>
        </div>
      )}

      {!loading && !error && !latest && (
        <div className="app-surface rounded-[18px] p-6">
          <h2 className="text-xl font-bold text-slate-950 dark:text-white">The first signed preview is being prepared</h2>
          <p className="mt-2 leading-7 text-slate-500 dark:text-slate-400">
            The web app remains available now. Android downloads will appear here only after signing and checksum verification.
          </p>
          <a className="text-link mt-4" href="https://github.com/shoyrulove-dev/PureHub/releases" target="_blank" rel="noreferrer">
            Follow releases on GitHub
          </a>
        </div>
      )}

      {latest && (
        <article className="app-surface rounded-[20px] p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="eyebrow">{latest.prerelease ? 'Preview release' : 'Stable release'}</span>
              <h2 className="mt-3 text-2xl font-bold text-slate-950 dark:text-white">{latest.title}</h2>
              <p className="mt-2 max-w-2xl leading-7 text-slate-500 dark:text-slate-400">{latest.summary}</p>
            </div>
            <span className="rounded-full bg-emerald-500/10 px-3 py-1.5 text-sm font-bold text-emerald-700 dark:text-emerald-300">
              v{latest.version}
            </span>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {latest.apk_url ? (
              <a className="primary-button justify-center" href={latest.apk_url} onClick={() => void trackJourneyEvent('download')}>
                <Download className="size-4" /> Download signed APK
              </a>
            ) : (
              <span className="flex min-h-11 items-center justify-center rounded-[14px] bg-slate-500/8 px-4 text-sm font-semibold text-slate-500">
                APK upload pending
              </span>
            )}
            {latest.github_url && (
              <a className="secondary-button justify-center" href={latest.github_url} target="_blank" rel="noreferrer">
                <Code2 className="size-4" /> View on GitHub
              </a>
            )}
          </div>

          {latest.sha256 && (
            <div className="mt-5 rounded-[16px] bg-slate-500/7 p-4">
              <p className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                <ShieldCheck className="size-4 text-emerald-500" /> APK SHA-256
              </p>
              <code className="mt-2 block break-all text-xs leading-5 text-slate-500 dark:text-slate-400">{latest.sha256}</code>
            </div>
          )}
        </article>
      )}

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="promise-card"><ShieldCheck className="size-5 text-emerald-500" /><span>Signed release builds</span></div>
        <div className="promise-card"><CheckCircle2 className="size-5 text-sky-500" /><span>No ads or trackers</span></div>
        <div className="promise-card"><FileArchive className="size-5 text-violet-500" /><span>Source available on GitHub</span></div>
      </div>

      <p className="text-center text-sm text-slate-500">
        Want the release history? <Link className="font-semibold text-emerald-600 dark:text-emerald-300" to={`/${locale}/changelog`}>Open changelog</Link>
      </p>
    </section>
  )
}

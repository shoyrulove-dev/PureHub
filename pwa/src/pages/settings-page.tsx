import { Activity, ArrowRight, Bug, Check, ExternalLink, Globe2, Languages, Moon, ShieldCheck, Smartphone, Sun, SunMoon, TestTube2 } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useState } from 'react'
import { SUPPORTED_LOCALES, normalizeLocale } from '../i18n/locales'
import { persistSelectedLocale } from '../i18n/routing'
import { type ThemePreference, useAnonymousMetricsPreference, useThemePreference } from '../lib/preferences'
import { PLAY_TESTER_GROUP_URL } from '../lib/play-testers'
import { getUiCopy } from '../i18n/ui-copy'

export function SettingsPage() {
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const copy = getUiCopy(locale).settings
  const { theme, setTheme } = useThemePreference()
  const { enabled: anonymousMetrics, setEnabled: setAnonymousMetrics, lockedByDnt } = useAnonymousMetricsPreference()
  const [diagnosticsCopied, setDiagnosticsCopied] = useState(false)
  const reportCopy = {
    en: { title: 'App information', hint: 'The report includes only this PWA version and your selected language.', action: 'Report a problem', copy: 'Copy diagnostics', copied: 'Copied' },
    vi: { title: 'Thông tin ứng dụng', hint: 'Báo cáo chỉ gồm phiên bản PWA và ngôn ngữ bạn đã chọn.', action: 'Báo lỗi', copy: 'Sao chép chẩn đoán', copied: 'Đã sao chép' },
    zh: { title: '应用信息', hint: '报告仅包含此 PWA 版本和您选择的语言。', action: '报告问题', copy: '复制诊断信息', copied: '已复制' },
    es: { title: 'Información de la app', hint: 'El informe solo incluye la versión de esta PWA y el idioma elegido.', action: 'Informar un problema', copy: 'Copiar diagnóstico', copied: 'Copiado' },
  }[locale]
  const diagnostics = `## Environment\n- PureHub PWA: ${__APP_VERSION__}\n- Language: ${locale}\n- Offline cache: ${typeof navigator !== 'undefined' && 'serviceWorker' in navigator ? 'supported' : 'unavailable'}\n- Local storage: ${typeof localStorage === 'undefined' ? 'unavailable' : 'available'}`
  const issueParams = new URLSearchParams({
    title: `PureHub PWA ${__APP_VERSION__} issue`,
    body: `${diagnostics}\n\n## What happened\n\n## Steps to reproduce\n\nDo not attach private files, passwords, QR contents, scans, or API keys.`,
  })
  const reportUrl = `https://github.com/shoyrulove-dev/PureHub/issues/new?${issueParams.toString()}`
  const themes: Array<{ id: ThemePreference; label: string; icon: typeof Sun }> = [
    { id: 'system', label: copy.system, icon: SunMoon },
    { id: 'light', label: copy.light, icon: Sun },
    { id: 'dark', label: copy.dark, icon: Moon },
  ]

  return (
    <section className="space-y-6">
      <div>
        <p className="eyebrow">{copy.eyebrow}</p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-white">{copy.title}</h1>
      </div>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-center gap-3">
          <span className="tool-card__icon text-amber-500"><SunMoon className="size-5" /></span>
          <div><h2 className="font-bold text-slate-950 dark:text-white">{copy.appearance}</h2><p className="text-sm text-slate-500">{copy.appearanceHint}</p></div>
        </div>
        <div className="mt-4 grid gap-2 sm:grid-cols-3">
          {themes.map(({ id, label, icon: Icon }) => (
            <button key={id} type="button" className={`promise-card cursor-pointer text-left ${theme === id ? 'border-violet-400 bg-violet-100/80 ring-2 ring-violet-400/25 dark:bg-violet-500/15' : ''}`} onClick={() => setTheme(id)}>
              <Icon className="size-5 text-violet-600 dark:text-violet-300" /><strong className="flex-1">{label}</strong>{theme === id ? <Check className="size-4 text-violet-600 dark:text-violet-300" /> : null}
            </button>
          ))}
        </div>
      </section>

      <Link to={`/${locale}/privacy-center`} className="flex min-h-16 items-center gap-3 rounded-[18px] border border-violet-300/70 bg-gradient-to-r from-violet-100 to-fuchsia-50 p-4 text-violet-950 dark:border-violet-500/30 dark:from-violet-500/15 dark:to-fuchsia-500/10 dark:text-violet-100"><ShieldCheck className="size-6 text-violet-600 dark:text-violet-300" /><span className="min-w-0 flex-1"><strong className="block">{copy.privacyCenter}</strong><small className="text-violet-700 dark:text-violet-300">{copy.privacyCenterHint}</small></span><ArrowRight className="size-5" /></Link>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start gap-3">
          <Smartphone className="mt-0.5 size-5 text-emerald-500" />
          <div>
            <h2 className="font-bold text-slate-950 dark:text-white">{copy.platform}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{copy.platformHint}</p>
            <Link to={`/${locale}/download`} className="text-link mt-3"><Globe2 className="size-4" /> {copy.compare}</Link>
          </div>
        </div>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-center gap-3">
          <span className="tool-card__icon text-sky-500"><Languages className="size-5" /></span>
          <div><h2 className="font-bold text-slate-950 dark:text-white">{copy.language}</h2><p className="text-sm text-slate-500">{copy.languageHint}</p></div>
        </div>
        <div className="mt-4 flex gap-2">
          {SUPPORTED_LOCALES.map((item) => (
            <Link
              key={item}
              to={`/${item}/settings`}
              onClick={() => persistSelectedLocale(item)}
              className={`filter-chip uppercase ${locale === item ? ({ en: 'border-sky-400 bg-sky-500 text-white', vi: 'border-rose-400 bg-rose-500 text-white', zh: 'border-amber-400 bg-amber-400 text-slate-950', es: 'border-violet-400 bg-violet-500 text-white' }[item]) : ''}`}
            >
              {item}
            </Link>
          ))}
        </div>
      </section>

      <a href={PLAY_TESTER_GROUP_URL} target="_blank" rel="noreferrer" className="flex min-h-16 items-center gap-3 rounded-[18px] border border-violet-200 bg-violet-50 p-4 text-violet-950 dark:border-violet-900 dark:bg-violet-950/25 dark:text-violet-100"><TestTube2 className="size-6" /><span className="min-w-0 flex-1"><strong className="block">{copy.tester}</strong><small className="text-violet-800 dark:text-violet-300">{copy.testerHint}</small></span><ExternalLink className="size-5" /></a>

      <section className="rounded-[18px] border border-violet-400/40 bg-gradient-to-br from-violet-950 to-fuchsia-950 p-5 text-white">
        <div className="flex items-start gap-3">
          <Bug className="mt-0.5 size-6 shrink-0 text-fuchsia-300" aria-hidden="true" />
          <div className="min-w-0 flex-1">
            <h2 className="font-bold">{reportCopy.title}</h2>
            <p className="mt-1 font-semibold text-fuchsia-200">PureHub PWA {__APP_VERSION__}</p>
            <p className="mt-2 text-sm leading-6 text-violet-100/80">{reportCopy.hint}</p>
            <div className="mt-4 flex flex-wrap gap-2">
            <button type="button" className="inline-flex min-h-12 items-center rounded-xl border border-violet-300/60 px-4 py-3 font-bold text-white hover:bg-white/10" onClick={async () => { await navigator.clipboard.writeText(diagnostics); setDiagnosticsCopied(true) }}>
              {diagnosticsCopied ? reportCopy.copied : reportCopy.copy}
            </button>
            <a href={reportUrl} target="_blank" rel="noreferrer" className="inline-flex min-h-12 items-center gap-2 rounded-xl bg-fuchsia-500 px-4 py-3 font-bold text-white hover:bg-fuchsia-400 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
              {reportCopy.action}<ExternalLink className="size-4" aria-hidden="true" />
            </a>
            </div>
          </div>
        </div>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <Activity className="mt-0.5 size-5 text-sky-500" />
            <div>
              <h2 className="font-bold text-slate-950 dark:text-white">{copy.counters}</h2>
              <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{copy.countersHint}</p>
              {lockedByDnt ? <p className="mt-1 text-xs font-semibold text-emerald-600">{copy.dnt}</p> : null}
            </div>
          </div>
          <button type="button" role="switch" aria-checked={anonymousMetrics} disabled={lockedByDnt} className={`relative h-7 w-12 shrink-0 rounded-full transition ${anonymousMetrics ? 'bg-violet-500' : 'bg-slate-300 dark:bg-slate-700'}`} onClick={() => setAnonymousMetrics(!anonymousMetrics)}>
            <span className={`absolute top-1 size-5 rounded-full bg-white shadow transition ${anonymousMetrics ? 'left-6' : 'left-1'}`} />
          </button>
        </div>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 text-emerald-500" />
          <div>
            <h2 className="font-bold text-slate-950 dark:text-white">{copy.privacyPromise}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
              {copy.privacyPromiseHint}
            </p>
          </div>
        </div>
      </section>
    </section>
  )
}

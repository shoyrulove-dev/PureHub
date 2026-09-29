import { Activity, ArrowRight, Check, ExternalLink, Globe2, Languages, Moon, ShieldCheck, Smartphone, Sun, SunMoon, TestTube2 } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
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
            <button key={id} type="button" className={`promise-card cursor-pointer text-left ${theme === id ? 'ring-2 ring-emerald-400/35' : ''}`} onClick={() => setTheme(id)}>
              <Icon className="size-5" /><strong className="flex-1">{label}</strong>{theme === id ? <Check className="size-4 text-emerald-500" /> : null}
            </button>
          ))}
        </div>
      </section>

      <Link to={`/${locale}/privacy-center`} className="flex min-h-16 items-center gap-3 rounded-[18px] border border-emerald-200 bg-emerald-50 p-4 text-emerald-950 dark:border-emerald-900 dark:bg-emerald-950/25 dark:text-emerald-100"><ShieldCheck className="size-6" /><span className="min-w-0 flex-1"><strong className="block">{copy.privacyCenter}</strong><small className="text-emerald-800 dark:text-emerald-300">{copy.privacyCenterHint}</small></span><ArrowRight className="size-5" /></Link>

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
              className={`filter-chip uppercase ${locale === item ? 'filter-chip--active' : ''}`}
            >
              {item}
            </Link>
          ))}
        </div>
      </section>

      <a href={PLAY_TESTER_GROUP_URL} target="_blank" rel="noreferrer" className="flex min-h-16 items-center gap-3 rounded-[18px] border border-violet-200 bg-violet-50 p-4 text-violet-950 dark:border-violet-900 dark:bg-violet-950/25 dark:text-violet-100"><TestTube2 className="size-6" /><span className="min-w-0 flex-1"><strong className="block">{copy.tester}</strong><small className="text-violet-800 dark:text-violet-300">{copy.testerHint}</small></span><ExternalLink className="size-5" /></a>

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
          <button type="button" role="switch" aria-checked={anonymousMetrics} disabled={lockedByDnt} className={`relative h-7 w-12 shrink-0 rounded-full transition ${anonymousMetrics ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}`} onClick={() => setAnonymousMetrics(!anonymousMetrics)}>
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

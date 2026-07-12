import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import type { MiniAppDefinition, TabDefinition } from '../features/catalog/tabs'
import { buildTabPath } from '../i18n/routing'
import { normalizeLocale } from '../i18n/locales'

type MiniAppLandingPageProps = {
  miniApp: MiniAppDefinition
  tab: TabDefinition
}

export function MiniAppLandingPage({ miniApp, tab }: MiniAppLandingPageProps) {
  const { t } = useTranslation()
  const { lang } = useParams()
  const normalizedLocale = normalizeLocale(lang)
  const runes = buildRunes(miniApp.id, t)
  const phaseNotes = buildPhaseNotes(miniApp.id)

  return (
    <section className="space-y-4">
      <div
        className={`rounded-[30px] border border-white/8 bg-gradient-to-br ${tab.accentSurfaceClass} p-5 shadow-[0_24px_70px_-48px_rgba(15,23,42,0.45)] sm:p-6`}
      >
        <div className={`flex size-14 items-center justify-center rounded-3xl border border-white/8 bg-white/5 ${tab.accentClass} shadow-sm`}>
          <miniApp.icon className="size-6" strokeWidth={2.1} />
        </div>
        <p className={`mt-4 text-xs font-semibold uppercase tracking-[0.28em] ${tab.accentClass}`}>
          {t('app.phaseShell')}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-white sm:text-3xl">
          {t(miniApp.titleKey)}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300 sm:text-[15px]">
          {t(miniApp.summaryKey)}
        </p>
      </div>

      <div className="rounded-[30px] border border-white/8 bg-slate-950/86 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.35)] sm:p-6">
        <p className="text-sm font-medium text-slate-200">{t('app.routeLabel')}</p>
        <p className="mt-2 break-all rounded-2xl border border-white/8 bg-white/5 px-4 py-3 text-sm text-slate-300">
          /{normalizedLocale}/{miniApp.slugs[normalizedLocale]}
        </p>
        <p className="mt-4 text-sm leading-6 text-slate-400">{t('app.openPlaceholder')}</p>
        <Link
          to={buildTabPath(normalizedLocale, tab.id)}
          className="mt-4 inline-flex rounded-2xl bg-emerald-400/14 px-4 py-2.5 text-sm font-medium text-emerald-200 shadow-[0_20px_50px_-32px_rgba(16,185,129,0.25)] ring-1 ring-inset ring-emerald-400/15"
        >
          {t('app.browseTab')}
        </Link>
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-[30px] border border-white/8 bg-slate-950/88 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.35)] sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className={`text-xs font-semibold uppercase tracking-[0.26em] ${tab.accentClass}`}>
                {t('miniAppDetails.ritualBoard')}
              </p>
              <h2 className="mt-2 text-xl font-semibold text-white">{t(miniApp.titleKey)}</h2>
            </div>
            <div className={`rounded-full border border-white/8 bg-white/5 px-3 py-1 text-xs font-semibold ${tab.accentClass}`}>
              {t('miniAppDetails.modeLabel')}
            </div>
          </div>

          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            {runes.map((rune) => (
              <div key={rune.label} className="rounded-[24px] border border-white/8 bg-white/5 p-4">
                <p className="text-xs uppercase tracking-[0.22em] text-slate-500">{rune.label}</p>
                <p className={`mt-2 text-lg font-semibold ${tab.accentClass}`}>{rune.value}</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{rune.note}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-[30px] border border-white/8 bg-slate-950/88 p-5 shadow-[0_20px_60px_-52px_rgba(15,23,42,0.35)] sm:p-6">
          <p className={`text-xs font-semibold uppercase tracking-[0.26em] ${tab.accentClass}`}>
            {t('miniAppDetails.nextPhase')}
          </p>
          <ul className="mt-4 space-y-3">
            {phaseNotes.map((note, index) => (
              <li key={note} className="rounded-[22px] border border-white/8 bg-white/5 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.2em] text-slate-500">
                  {t('miniAppDetails.focusLabel')} #{index + 1}
                </p>
                <p className="mt-2 text-sm leading-6 text-slate-300">{note}</p>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}

function buildRunes(
  miniAppId: MiniAppDefinition['id'],
  t: (key: string) => string,
) {
  const shared = {
    primary: t('miniAppDetails.modeLabel'),
    focus: t('miniAppDetails.focusLabel'),
  }

  const map: Record<
    MiniAppDefinition['id'],
    Array<{ label: string; value: string; note: string }>
  > = {
    'lunar-calendar': [
      { label: shared.primary, value: 'Solar → Lunar', note: 'Offline date conversion flow is prepared for a richer month board.' },
      { label: shared.focus, value: 'Month reading', note: 'Designed for fast browsing with future festival and note layers.' },
    ],
    'zen-habit': [
      { label: shared.primary, value: 'IndexedDB streaks', note: 'Habit data remains private and fully local.' },
      { label: shared.focus, value: 'Daily rhythm', note: 'Ready for streak heatmaps and completion rituals.' },
    ],
    'zen-pomodoro': [
      { label: shared.primary, value: 'Focus cycle', note: 'Prepared for timer loops, breaks, and ambient sound controls.' },
      { label: shared.focus, value: 'White noise', note: 'Future controls can expand into richer soundscapes.' },
    ],
    'zen-breath': [
      { label: shared.primary, value: 'Motion guide', note: 'The breathing shell is ready for smooth inhale and exhale states.' },
      { label: shared.focus, value: 'Calm flow', note: 'Designed to feel ritual-like rather than clinical.' },
    ],
    compass: [
      { label: shared.primary, value: 'Heading', note: 'Prepared for orientation-driven motion and bearing lock.' },
      { label: shared.focus, value: 'Sensor path', note: 'Built around browser device orientation events.' },
    ],
    'bubble-level': [
      { label: shared.primary, value: 'Alignment', note: 'Ready for sensor-based bubble calibration.' },
      { label: shared.focus, value: 'Balance plane', note: 'Future UI can show both flat and edge modes.' },
    ],
    'decibel-meter': [
      { label: shared.primary, value: 'Wave capture', note: 'Microphone shell is ready for waveform and dB layers.' },
      { label: shared.focus, value: 'Noise readout', note: 'Future pass can add history, thresholds, and alerts.' },
    ],
    'unit-converter': [
      { label: shared.primary, value: 'Instant math', note: 'Conversion shell is structured for zero-latency interactions.' },
      { label: shared.focus, value: 'Preset groups', note: 'Prepared for measurement, finance, and utility categories.' },
    ],
    'qr-studio': [
      { label: shared.primary, value: 'Scan + Forge', note: 'The shell supports both scanner and generator directions.' },
      { label: shared.focus, value: 'Camera lane', note: 'Ready for camera permission and code handling flows.' },
    ],
    'doc-to-pdf': [
      { label: shared.primary, value: 'Capture stack', note: 'Designed for staged snapshots and export rituals.' },
      { label: shared.focus, value: 'PDF forge', note: 'Future pass can add crop, page ordering, and export polish.' },
    ],
    'ocr-text': [
      { label: shared.primary, value: 'Text extraction', note: 'Prepared for image-to-text processing in-browser.' },
      { label: shared.focus, value: 'Reading pass', note: 'Future UI can add copy, cleanup, and language filters.' },
    ],
    'color-grabber': [
      { label: shared.primary, value: 'Pixel sampling', note: 'Ready for video frame sampling and palette capture.' },
      { label: shared.focus, value: 'Color ritual', note: 'Future flow can expand to saved swatches and export.' },
    ],
    'speaker-cleaner': [
      { label: shared.primary, value: '165Hz pulse', note: 'Prepared for oscillator-based cleanup routines.' },
      { label: shared.focus, value: 'Safe playback', note: 'Future pass can add session presets and safety hints.' },
    ],
    'password-vault': [
      { label: shared.primary, value: 'Local vault', note: 'Structured for private, device-only credential storage.' },
      { label: shared.focus, value: 'Secure notes', note: 'Future pass can add lock states and search tools.' },
    ],
    'bill-splitter': [
      { label: shared.primary, value: 'Group math', note: 'Prepared for shared totals, custom assignments, and tips.' },
      { label: shared.focus, value: 'Trip rhythm', note: 'Future pass can add save/share and expense sessions.' },
    ],
    'expense-tracker': [
      { label: shared.primary, value: 'Private ledger', note: 'Ready for local entries, tags, and monthly summaries.' },
      { label: shared.focus, value: 'Budget pulse', note: 'Future pass can add charts and recurring flows.' },
    ],
    'decision-wheel': [
      { label: shared.primary, value: 'Fate spin', note: 'Prepared for canvas-based spin motion and result focus.' },
      { label: shared.focus, value: 'Choice pool', note: 'Future pass can add weighted options and saved sets.' },
    ],
    'community-pro-unlock': [
      { label: shared.primary, value: 'Portal unlock', note: 'Built for Telegram deep-linking and local code memory.' },
      { label: shared.focus, value: 'Growth engine', note: 'Future pass can add clear unlock state and rewards.' },
    ],
  }

  return map[miniAppId]
}

function buildPhaseNotes(miniAppId: MiniAppDefinition['id']) {
  const shared = [
    'Refine live interactions so the module feels closer to a native ritual than a static page.',
    'Expand the offline data and processing layer without introducing network dependency.',
    'Add stronger feedback states, motion polish, and export-ready flows.',
  ]

  if (miniAppId === 'community-pro-unlock') {
    return [
      'Clarify the Pro code journey with stronger visual states and reward messaging.',
      'Surface the Telegram unlock path more prominently while keeping it lightweight.',
      'Add transparent local-state indicators so users always know what is unlocked.',
    ]
  }

  if (miniAppId === 'expense-tracker' || miniAppId === 'bill-splitter') {
    return [
      'Expand number entry interactions for faster one-hand mobile use.',
      'Layer in save, restore, and share flows that still stay fully local.',
      'Add visual summaries that make group or monthly totals easy to scan.',
    ]
  }

  return shared
}

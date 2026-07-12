import type { IconType } from 'react-icons'
import {
  GiAbacus,
  GiCircleSparks,
  GiMagnifyingGlass,
  GiMoon,
  GiOpenTreasureChest,
  GiPadlock,
  GiScrollQuill,
  GiSoundWaves,
  GiWaterDrop,
  GiWaterSplash,
  GiWhirlwind,
} from 'react-icons/gi'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { buildMiniAppPath } from '../../i18n/routing'
import { normalizeLocale } from '../../i18n/locales'
import type { MiniAppId } from '../../features/catalog/tabs'

type DashboardFeature = {
  id: MiniAppId
  icon: IconType
  titleKey: string
  summaryKey: string
  glowClass: string
  panelClass: string
}

const dashboardFeatures: DashboardFeature[] = [
  {
    id: 'lunar-calendar',
    icon: GiMoon,
    titleKey: 'miniApps.lunarCalendar.title',
    summaryKey: 'miniApps.lunarCalendar.summary',
    glowClass: 'text-emerald-300',
    panelClass: 'from-emerald-400/14 to-cyan-400/6',
  },
  {
    id: 'zen-habit',
    icon: GiScrollQuill,
    titleKey: 'miniApps.zenHabit.title',
    summaryKey: 'miniApps.zenHabit.summary',
    glowClass: 'text-amber-300',
    panelClass: 'from-amber-400/14 to-orange-400/6',
  },
  {
    id: 'zen-breath',
    icon: GiWhirlwind,
    titleKey: 'miniApps.zenBreath.title',
    summaryKey: 'miniApps.zenBreath.summary',
    glowClass: 'text-violet-300',
    panelClass: 'from-violet-400/14 to-fuchsia-400/6',
  },
  {
    id: 'bubble-level',
    icon: GiWaterDrop,
    titleKey: 'miniApps.bubbleLevel.title',
    summaryKey: 'miniApps.bubbleLevel.summary',
    glowClass: 'text-sky-300',
    panelClass: 'from-sky-400/14 to-cyan-400/6',
  },
  {
    id: 'decibel-meter',
    icon: GiSoundWaves,
    titleKey: 'miniApps.decibelMeter.title',
    summaryKey: 'miniApps.decibelMeter.summary',
    glowClass: 'text-rose-300',
    panelClass: 'from-rose-400/14 to-pink-400/6',
  },
  {
    id: 'ocr-text',
    icon: GiMagnifyingGlass,
    titleKey: 'miniApps.ocrText.title',
    summaryKey: 'miniApps.ocrText.summary',
    glowClass: 'text-indigo-300',
    panelClass: 'from-indigo-400/14 to-purple-400/6',
  },
  {
    id: 'speaker-cleaner',
    icon: GiWaterSplash,
    titleKey: 'miniApps.speakerCleaner.title',
    summaryKey: 'miniApps.speakerCleaner.summary',
    glowClass: 'text-cyan-300',
    panelClass: 'from-cyan-400/14 to-teal-400/6',
  },
  {
    id: 'password-vault',
    icon: GiPadlock,
    titleKey: 'miniApps.passwordVault.title',
    summaryKey: 'miniApps.passwordVault.summary',
    glowClass: 'text-purple-300',
    panelClass: 'from-purple-400/14 to-violet-400/6',
  },
  {
    id: 'unit-converter',
    icon: GiAbacus,
    titleKey: 'miniApps.unitConverter.title',
    summaryKey: 'miniApps.unitConverter.summary',
    glowClass: 'text-emerald-200',
    panelClass: 'from-lime-400/14 to-emerald-400/6',
  },
  {
    id: 'expense-tracker',
    icon: GiOpenTreasureChest,
    titleKey: 'miniApps.expenseTracker.title',
    summaryKey: 'miniApps.expenseTracker.summary',
    glowClass: 'text-yellow-300',
    panelClass: 'from-yellow-400/14 to-amber-400/6',
  },
  {
    id: 'decision-wheel',
    icon: GiCircleSparks,
    titleKey: 'miniApps.decisionWheel.title',
    summaryKey: 'miniApps.decisionWheel.summary',
    glowClass: 'text-fuchsia-300',
    panelClass: 'from-fuchsia-400/14 to-pink-400/6',
  },
]

export function Dashboard() {
  const { t } = useTranslation()
  const { locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)

  return (
    <section className="space-y-5">
      <div className="overflow-hidden rounded-[32px] border border-emerald-400/16 bg-[radial-gradient(circle_at_top,rgba(16,185,129,0.22),transparent_38%),linear-gradient(180deg,rgba(15,23,42,0.96),rgba(2,6,23,0.98))] p-5 shadow-[0_32px_120px_-56px_rgba(52,211,153,0.42)] sm:p-7">
        <div className="inline-flex rounded-full border border-emerald-400/18 bg-emerald-400/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.3em] text-emerald-300">
          {t('app.phaseShell')}
        </div>
        <h1 className="mt-4 max-w-xl text-3xl font-semibold tracking-tight text-white sm:text-4xl">
          {t('tabs.zenTime.label')}
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300 sm:text-[15px]">
          {t('tabs.zenTime.description')}
        </p>
        <div className="mt-6 grid gap-3 sm:grid-cols-3">
          <div className="rounded-[24px] border border-white/8 bg-white/6 p-4">
            <p className="text-xs uppercase tracking-[0.24em] text-slate-500">{t('dashboard.offline')}</p>
            <p className="mt-2 text-lg font-semibold text-emerald-300">100%</p>
          </div>
          <div className="rounded-[24px] border border-white/8 bg-white/6 p-4">
            <p className="text-xs uppercase tracking-[0.24em] text-slate-500">{t('dashboard.quickAccess')}</p>
            <p className="mt-2 text-lg font-semibold text-purple-300">{t('dashboard.spells')}</p>
          </div>
          <div className="rounded-[24px] border border-white/8 bg-white/6 p-4">
            <p className="text-xs uppercase tracking-[0.24em] text-slate-500">{t('dashboard.privacy')}</p>
            <p className="mt-2 text-lg font-semibold text-sky-300">{t('dashboard.onDevice')}</p>
          </div>
        </div>
      </div>

      <div className="rounded-[30px] border border-white/8 bg-slate-950/88 p-5 shadow-[0_30px_110px_-64px_rgba(168,85,247,0.45)] sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-purple-300/80">
              {t('app.miniAppMap')}
            </p>
            <h2 className="mt-2 text-xl font-semibold text-white sm:text-2xl">
              {t('dashboard.collection')}
            </h2>
          </div>
        </div>

        <ul className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {dashboardFeatures.map((feature) => {
            const Icon = feature.icon

            return (
              <li key={feature.id}>
                <Link
                  to={buildMiniAppPath(normalizedLocale, feature.id)}
                  className={`group block h-full rounded-[26px] border border-white/8 bg-gradient-to-br ${feature.panelClass} from-0% via-slate-900 to-slate-950 p-[1px] transition duration-200 hover:-translate-y-0.5 hover:border-white/16 hover:shadow-[0_24px_70px_-40px_rgba(168,85,247,0.45)] focus:outline-none focus:ring-2 focus:ring-emerald-300/50`}
                >
                  <div className="flex h-full flex-col rounded-[25px] bg-slate-950/92 p-4">
                    <div className="flex items-start justify-between gap-4">
                      <div
                        className={`flex size-14 items-center justify-center rounded-[20px] border border-white/8 bg-white/5 ${feature.glowClass} shadow-[inset_0_0_0_1px_rgba(255,255,255,0.03)]`}
                      >
                        <Icon className="text-[1.7rem] drop-shadow-[0_0_14px_rgba(167,139,250,0.32)]" />
                      </div>
                      <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-[11px] font-semibold text-slate-400">
                        #{dashboardFeatures.indexOf(feature) + 1}
                      </span>
                    </div>
                    <h3 className="mt-4 text-base font-semibold text-white">
                      {t(feature.titleKey)}
                    </h3>
                    <p className="mt-2 text-sm leading-6 text-slate-400">
                      {t(feature.summaryKey)}
                    </p>
                    <span className={`mt-4 inline-flex text-xs font-semibold ${feature.glowClass}`}>
                      {t('app.openMiniApp')}
                    </span>
                  </div>
                </Link>
              </li>
            )
          })}
        </ul>
      </div>
    </section>
  )
}

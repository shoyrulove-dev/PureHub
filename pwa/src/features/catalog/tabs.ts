import type { LucideIcon } from 'lucide-react'
import {
  Activity,
  AudioLines,
  Camera,
  CreditCard,
  ShieldCheck,
  Sparkles,
  Timer,
  Wallet,
  Waves,
  Wrench,
} from 'lucide-react'
import { seoMeta, type SeoLanguage } from '../../config/seoMeta'
import type { LocaleCode } from '../../i18n/locales'

type LocalizedSlugMap = Record<LocaleCode, string>

export type TabId =
  | 'zen-time'
  | 'measure-tools'
  | 'vision'
  | 'security-audio'
  | 'finance-community'

export type MiniAppId =
  | 'lunar-calendar'
  | 'zen-habit'
  | 'zen-pomodoro'
  | 'zen-breath'
  | 'compass'
  | 'bubble-level'
  | 'decibel-meter'
  | 'unit-converter'
  | 'qr-studio'
  | 'doc-to-pdf'
  | 'ocr-text'
  | 'color-grabber'
  | 'speaker-cleaner'
  | 'password-vault'
  | 'bill-splitter'
  | 'expense-tracker'
  | 'decision-wheel'
  | 'community-pro-unlock'

export type TabDefinition = {
  id: TabId
  icon: LucideIcon
  labelKey: string
  shortLabelKey: string
  descriptionKey: string
  segments: LocalizedSlugMap
  accentClass: string
  accentSurfaceClass: string
}

export type MiniAppDefinition = {
  id: MiniAppId
  tabId: TabId
  icon: LucideIcon
  titleKey: string
  summaryKey: string
  slugs: LocalizedSlugMap
}

function seoSlugs(id: Exclude<MiniAppId, 'community-pro-unlock'>): Record<SeoLanguage, string> {
  return {
    en: seoMeta[id].en.slug,
    vi: seoMeta[id].vi.slug,
    zh: seoMeta[id].zh.slug,
  }
}

export const TAB_ITEMS: TabDefinition[] = [
  {
    id: 'zen-time',
    icon: Sparkles,
    labelKey: 'tabs.zenTime.label',
    shortLabelKey: 'tabs.zenTime.short',
    descriptionKey: 'tabs.zenTime.description',
    segments: {
      en: 'zen-time',
      vi: 'thien-thoi-gian',
      zh: 'chan-shi-jian',
    },
    accentClass: 'text-emerald-300',
    accentSurfaceClass: 'from-emerald-400/14 via-slate-950 to-cyan-400/8',
  },
  {
    id: 'measure-tools',
    icon: Wrench,
    labelKey: 'tabs.measureTools.label',
    shortLabelKey: 'tabs.measureTools.short',
    descriptionKey: 'tabs.measureTools.description',
    segments: {
      en: 'measure-tools',
      vi: 'do-luong-cong-cu',
      zh: 'ce-liang-gong-ju',
    },
    accentClass: 'text-sky-300',
    accentSurfaceClass: 'from-sky-400/14 via-slate-950 to-indigo-400/8',
  },
  {
    id: 'vision',
    icon: Camera,
    labelKey: 'tabs.vision.label',
    shortLabelKey: 'tabs.vision.short',
    descriptionKey: 'tabs.vision.description',
    segments: {
      en: 'vision',
      vi: 'thi-giac-camera',
      zh: 'shi-jue-xiang-ji',
    },
    accentClass: 'text-violet-300',
    accentSurfaceClass: 'from-violet-400/14 via-slate-950 to-fuchsia-400/8',
  },
  {
    id: 'security-audio',
    icon: ShieldCheck,
    labelKey: 'tabs.securityAudio.label',
    shortLabelKey: 'tabs.securityAudio.short',
    descriptionKey: 'tabs.securityAudio.description',
    segments: {
      en: 'security-audio',
      vi: 'bao-mat-am-thanh',
      zh: 'an-quan-yin-pin',
    },
    accentClass: 'text-teal-300',
    accentSurfaceClass: 'from-teal-400/14 via-slate-950 to-emerald-400/8',
  },
  {
    id: 'finance-community',
    icon: CreditCard,
    labelKey: 'tabs.financeCommunity.label',
    shortLabelKey: 'tabs.financeCommunity.short',
    descriptionKey: 'tabs.financeCommunity.description',
    segments: {
      en: 'finance-community',
      vi: 'tai-chinh-cong-dong',
      zh: 'cai-wu-she-qu',
    },
    accentClass: 'text-amber-300',
    accentSurfaceClass: 'from-amber-400/14 via-slate-950 to-orange-400/8',
  },
]

export const MINI_APP_ITEMS: MiniAppDefinition[] = [
  {
    id: 'lunar-calendar',
    tabId: 'zen-time',
    icon: Sparkles,
    titleKey: 'miniApps.lunarCalendar.title',
    summaryKey: 'miniApps.lunarCalendar.summary',
    slugs: seoSlugs('lunar-calendar'),
  },
  {
    id: 'zen-habit',
    tabId: 'zen-time',
    icon: Activity,
    titleKey: 'miniApps.zenHabit.title',
    summaryKey: 'miniApps.zenHabit.summary',
    slugs: seoSlugs('zen-habit'),
  },
  {
    id: 'zen-pomodoro',
    tabId: 'zen-time',
    icon: Timer,
    titleKey: 'miniApps.zenPomodoro.title',
    summaryKey: 'miniApps.zenPomodoro.summary',
    slugs: seoSlugs('zen-pomodoro'),
  },
  {
    id: 'zen-breath',
    tabId: 'zen-time',
    icon: Waves,
    titleKey: 'miniApps.zenBreath.title',
    summaryKey: 'miniApps.zenBreath.summary',
    slugs: seoSlugs('zen-breath'),
  },
  {
    id: 'compass',
    tabId: 'measure-tools',
    icon: Sparkles,
    titleKey: 'miniApps.compass.title',
    summaryKey: 'miniApps.compass.summary',
    slugs: seoSlugs('compass'),
  },
  {
    id: 'bubble-level',
    tabId: 'measure-tools',
    icon: Activity,
    titleKey: 'miniApps.bubbleLevel.title',
    summaryKey: 'miniApps.bubbleLevel.summary',
    slugs: seoSlugs('bubble-level'),
  },
  {
    id: 'decibel-meter',
    tabId: 'measure-tools',
    icon: AudioLines,
    titleKey: 'miniApps.decibelMeter.title',
    summaryKey: 'miniApps.decibelMeter.summary',
    slugs: seoSlugs('decibel-meter'),
  },
  {
    id: 'unit-converter',
    tabId: 'measure-tools',
    icon: Wrench,
    titleKey: 'miniApps.unitConverter.title',
    summaryKey: 'miniApps.unitConverter.summary',
    slugs: seoSlugs('unit-converter'),
  },
  {
    id: 'qr-studio',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.qrStudio.title',
    summaryKey: 'miniApps.qrStudio.summary',
    slugs: seoSlugs('qr-studio'),
  },
  {
    id: 'doc-to-pdf',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.docToPdf.title',
    summaryKey: 'miniApps.docToPdf.summary',
    slugs: seoSlugs('doc-to-pdf'),
  },
  {
    id: 'ocr-text',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.ocrText.title',
    summaryKey: 'miniApps.ocrText.summary',
    slugs: seoSlugs('ocr-text'),
  },
  {
    id: 'color-grabber',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.colorGrabber.title',
    summaryKey: 'miniApps.colorGrabber.summary',
    slugs: seoSlugs('color-grabber'),
  },
  {
    id: 'speaker-cleaner',
    tabId: 'security-audio',
    icon: AudioLines,
    titleKey: 'miniApps.speakerCleaner.title',
    summaryKey: 'miniApps.speakerCleaner.summary',
    slugs: seoSlugs('speaker-cleaner'),
  },
  {
    id: 'password-vault',
    tabId: 'security-audio',
    icon: ShieldCheck,
    titleKey: 'miniApps.passwordVault.title',
    summaryKey: 'miniApps.passwordVault.summary',
    slugs: seoSlugs('password-vault'),
  },
  {
    id: 'bill-splitter',
    tabId: 'finance-community',
    icon: CreditCard,
    titleKey: 'miniApps.billSplitter.title',
    summaryKey: 'miniApps.billSplitter.summary',
    slugs: seoSlugs('bill-splitter'),
  },
  {
    id: 'expense-tracker',
    tabId: 'finance-community',
    icon: Wallet,
    titleKey: 'miniApps.expenseTracker.title',
    summaryKey: 'miniApps.expenseTracker.summary',
    slugs: seoSlugs('expense-tracker'),
  },
  {
    id: 'decision-wheel',
    tabId: 'finance-community',
    icon: Sparkles,
    titleKey: 'miniApps.decisionWheel.title',
    summaryKey: 'miniApps.decisionWheel.summary',
    slugs: seoSlugs('decision-wheel'),
  },
  {
    id: 'community-pro-unlock',
    tabId: 'finance-community',
    icon: CreditCard,
    titleKey: 'miniApps.communityUnlock.title',
    summaryKey: 'miniApps.communityUnlock.summary',
    slugs: {
      en: 'community-pro-unlock',
      vi: 'mo-khoa-cong-dong',
      zh: 'she-qu-jie-suo',
    },
  },
]

export const TAB_BY_ID = new Map(TAB_ITEMS.map((tab) => [tab.id, tab]))
export const MINI_APP_BY_ID = new Map(MINI_APP_ITEMS.map((miniApp) => [miniApp.id, miniApp]))

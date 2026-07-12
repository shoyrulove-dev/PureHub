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
      zh: '禅与时间',
    },
    accentClass: 'text-teal-600',
    accentSurfaceClass: 'from-teal-500/16 via-white to-cyan-500/10',
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
      zh: '测量工具',
    },
    accentClass: 'text-sky-600',
    accentSurfaceClass: 'from-sky-500/16 via-white to-indigo-500/10',
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
      zh: '视觉相机',
    },
    accentClass: 'text-violet-600',
    accentSurfaceClass: 'from-violet-500/16 via-white to-fuchsia-500/10',
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
      zh: '安全音频',
    },
    accentClass: 'text-emerald-700',
    accentSurfaceClass: 'from-emerald-500/16 via-white to-teal-500/10',
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
      zh: '财务社区',
    },
    accentClass: 'text-amber-600',
    accentSurfaceClass: 'from-amber-500/16 via-white to-orange-500/10',
  },
]

export const MINI_APP_ITEMS: MiniAppDefinition[] = [
  {
    id: 'lunar-calendar',
    tabId: 'zen-time',
    icon: Sparkles,
    titleKey: 'miniApps.lunarCalendar.title',
    summaryKey: 'miniApps.lunarCalendar.summary',
    slugs: { en: 'lunar-calendar', vi: 'lich-am', zh: '农历' },
  },
  {
    id: 'zen-habit',
    tabId: 'zen-time',
    icon: Activity,
    titleKey: 'miniApps.zenHabit.title',
    summaryKey: 'miniApps.zenHabit.summary',
    slugs: { en: 'zen-habit', vi: 'thoi-quen-zen', zh: '禅习惯' },
  },
  {
    id: 'zen-pomodoro',
    tabId: 'zen-time',
    icon: Timer,
    titleKey: 'miniApps.zenPomodoro.title',
    summaryKey: 'miniApps.zenPomodoro.summary',
    slugs: { en: 'zen-pomodoro', vi: 'pomodoro-zen', zh: '禅番茄' },
  },
  {
    id: 'zen-breath',
    tabId: 'zen-time',
    icon: Waves,
    titleKey: 'miniApps.zenBreath.title',
    summaryKey: 'miniApps.zenBreath.summary',
    slugs: { en: 'zen-breath', vi: 'tho-zen', zh: '禅呼吸' },
  },
  {
    id: 'compass',
    tabId: 'measure-tools',
    icon: Sparkles,
    titleKey: 'miniApps.compass.title',
    summaryKey: 'miniApps.compass.summary',
    slugs: { en: 'compass', vi: 'la-ban', zh: '罗盘' },
  },
  {
    id: 'bubble-level',
    tabId: 'measure-tools',
    icon: Activity,
    titleKey: 'miniApps.bubbleLevel.title',
    summaryKey: 'miniApps.bubbleLevel.summary',
    slugs: { en: 'bubble-level', vi: 'thuoc-thuy', zh: '水平仪' },
  },
  {
    id: 'decibel-meter',
    tabId: 'measure-tools',
    icon: AudioLines,
    titleKey: 'miniApps.decibelMeter.title',
    summaryKey: 'miniApps.decibelMeter.summary',
    slugs: { en: 'decibel-meter', vi: 'do-on', zh: '分贝仪' },
  },
  {
    id: 'unit-converter',
    tabId: 'measure-tools',
    icon: Wrench,
    titleKey: 'miniApps.unitConverter.title',
    summaryKey: 'miniApps.unitConverter.summary',
    slugs: { en: 'unit-converter', vi: 'doi-don-vi', zh: '单位换算' },
  },
  {
    id: 'qr-studio',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.qrStudio.title',
    summaryKey: 'miniApps.qrStudio.summary',
    slugs: { en: 'qr-studio', vi: 'qr-studio', zh: '二维码工坊' },
  },
  {
    id: 'doc-to-pdf',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.docToPdf.title',
    summaryKey: 'miniApps.docToPdf.summary',
    slugs: { en: 'doc-to-pdf', vi: 'tai-lieu-pdf', zh: '文档转pdf' },
  },
  {
    id: 'ocr-text',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.ocrText.title',
    summaryKey: 'miniApps.ocrText.summary',
    slugs: { en: 'ocr-text', vi: 'trich-xuat-van-ban', zh: 'ocr文字' },
  },
  {
    id: 'color-grabber',
    tabId: 'vision',
    icon: Camera,
    titleKey: 'miniApps.colorGrabber.title',
    summaryKey: 'miniApps.colorGrabber.summary',
    slugs: { en: 'color-grabber', vi: 'lay-mau', zh: '取色器' },
  },
  {
    id: 'speaker-cleaner',
    tabId: 'security-audio',
    icon: AudioLines,
    titleKey: 'miniApps.speakerCleaner.title',
    summaryKey: 'miniApps.speakerCleaner.summary',
    slugs: { en: 'speaker-cleaner', vi: 'lam-sach-loa', zh: '清理扬声器' },
  },
  {
    id: 'password-vault',
    tabId: 'security-audio',
    icon: ShieldCheck,
    titleKey: 'miniApps.passwordVault.title',
    summaryKey: 'miniApps.passwordVault.summary',
    slugs: { en: 'password-vault', vi: 'kho-mat-khau', zh: '密码保险库' },
  },
  {
    id: 'bill-splitter',
    tabId: 'finance-community',
    icon: CreditCard,
    titleKey: 'miniApps.billSplitter.title',
    summaryKey: 'miniApps.billSplitter.summary',
    slugs: { en: 'bill-splitter', vi: 'chia-hoa-don', zh: '分账器' },
  },
  {
    id: 'expense-tracker',
    tabId: 'finance-community',
    icon: Wallet,
    titleKey: 'miniApps.expenseTracker.title',
    summaryKey: 'miniApps.expenseTracker.summary',
    slugs: { en: 'expense-tracker', vi: 'so-chi-tieu', zh: '记账本' },
  },
  {
    id: 'decision-wheel',
    tabId: 'finance-community',
    icon: Sparkles,
    titleKey: 'miniApps.decisionWheel.title',
    summaryKey: 'miniApps.decisionWheel.summary',
    slugs: { en: 'decision-wheel', vi: 'vong-quay-quyet-dinh', zh: '决策转盘' },
  },
  {
    id: 'community-pro-unlock',
    tabId: 'finance-community',
    icon: CreditCard,
    titleKey: 'miniApps.communityUnlock.title',
    summaryKey: 'miniApps.communityUnlock.summary',
    slugs: { en: 'community-pro-unlock', vi: 'mo-khoa-cong-dong', zh: '社区解锁' },
  },
]

export const TAB_BY_ID = new Map(TAB_ITEMS.map((tab) => [tab.id, tab]))
export const MINI_APP_BY_ID = new Map(MINI_APP_ITEMS.map((miniApp) => [miniApp.id, miniApp]))

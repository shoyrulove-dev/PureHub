import { useEffect, useState } from 'react'
import { ArrowRight, Check, Download, Heart, MessageSquare, Send, Share2, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MINI_APP_BY_ID, type MiniAppId } from '../../features/catalog/tabs'
import { buildMiniAppPath } from '../../i18n/routing'
import { normalizeLocale } from '../../i18n/locales'
import { submitProductFeedback, trackJourneyEvent, trackProductEvent, type FeedbackCategory } from '../../lib/community-api'
import { shareCard } from '../../lib/share-card'
import type { ToolSuccess } from '../../lib/tool-success'
import { getProductCopy } from '../../i18n/product-copy'

type MiniAppEngagementProps = {
  miniAppId: MiniAppId
  title: string
}

type CompletionCta = {
  label: string
  detail: string
}

const wifiCompletionCtas: Record<ReturnType<typeof normalizeLocale>, CompletionCta> = {
  en: { label: 'Run a real Wi-Fi scan on Android', detail: 'Install Android for nearby networks, channels, LAN discovery, roaming and coverage surveys.' },
  vi: { label: 'Quét Wi-Fi thực trên Android', detail: 'Cài Android để xem mạng lân cận, kênh, thiết bị LAN, roaming và khảo sát vùng phủ.' },
  zh: { label: '在 Android 上运行真实 Wi-Fi 扫描', detail: '安装 Android 版以使用附近网络、信道、局域网发现、漫游和覆盖调查。' },
  es: { label: 'Escanear Wi-Fi de verdad en Android', detail: 'Instala Android para ver redes cercanas, canales, dispositivos LAN, roaming y estudios de cobertura.' },
}

const testerSprintApps: MiniAppId[] = ['qr-studio', 'ocr-text', 'deep-cleaner']

const NEXT_TOOL: Record<MiniAppId, MiniAppId> = {
  'lunar-calendar': 'zen-habit',
  'zen-habit': 'zen-pomodoro',
  'zen-pomodoro': 'zen-breath',
  'zen-breath': 'zen-habit',
  compass: 'bubble-level',
  'bubble-level': 'unit-converter',
  'decibel-meter': 'speaker-cleaner',
  'smart-flashlight': 'compass',
  'unit-converter': 'bill-splitter',
  'qr-studio': 'ocr-text',
  'doc-to-pdf': 'ocr-text',
  'ocr-text': 'doc-to-pdf',
  'color-grabber': 'wallpaper-changer',
  'speaker-cleaner': 'decibel-meter',
  'deep-cleaner': 'file-studio',
  'photo-privacy': 'file-studio',
  'wifi-analyzer': 'qr-studio',
  'password-vault': 'authenticator-vault',
  'wallpaper-changer': 'color-grabber',
  'bill-splitter': 'expense-tracker',
  'expense-tracker': 'bill-splitter',
  'decision-wheel': 'community-pro-unlock',
  'community-pro-unlock': 'qr-studio',
  'authenticator-vault': 'password-vault',
  'file-studio': 'photo-privacy',
  'screen-recorder': 'file-studio',
}

export function MiniAppEngagement({ miniAppId, title }: MiniAppEngagementProps) {
  const { t } = useTranslation()
  const helpfulKey = `purehub-helpful-${miniAppId}`
  const [helpful, setHelpful] = useState(() => window.localStorage.getItem(helpfulKey) === 'true')
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const [category, setCategory] = useState<FeedbackCategory>('feedback')
  const [message, setMessage] = useState('')
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')
  const [shareStatus, setShareStatus] = useState('')
  const [completed, setCompleted] = useState(() => window.localStorage.getItem(`purehub-completed-${miniAppId}`) === 'true')
  const [success, setSuccess] = useState<ToolSuccess | null>(null)
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const copy = getProductCopy(locale).engagement
  const nextTool = MINI_APP_BY_ID.get(NEXT_TOOL[miniAppId])
  const completionCta = miniAppId === 'wifi-analyzer' ? wifiCompletionCtas[locale] : {
    label: copy.keepOffline,
    detail: copy.keepOfflineDetail,
  }
  const downloadUrl = `/${locale}/download?utm_source=tool_success&utm_campaign=completion-${miniAppId}`
  const testerSprintUrl = `/${locale}/community?utm_source=tool_success&utm_campaign=tester-sprint-${miniAppId}`
  const isTesterSprintApp = testerSprintApps.includes(miniAppId)

  useEffect(() => {
    const day = new Date().toISOString().slice(0, 10)
    const openKey = `purehub-open-${miniAppId}-${day}`
    if (window.localStorage.getItem(openKey)) return
    window.localStorage.setItem(openKey, 'true')
    void trackProductEvent(miniAppId, 'open')
  }, [miniAppId])

  useEffect(() => {
    const onComplete = (event: Event) => {
      const detail = (event as CustomEvent<{ miniAppId: MiniAppId }>).detail
      if (detail?.miniAppId === miniAppId) setCompleted(true)
    }
    window.addEventListener('purehub:product-complete', onComplete)
    return () => window.removeEventListener('purehub:product-complete', onComplete)
  }, [miniAppId])

  useEffect(() => {
    const onSuccess = (event: Event) => {
      const detail = (event as CustomEvent<{ miniAppId: MiniAppId; success: ToolSuccess }>).detail
      if (detail?.miniAppId === miniAppId) setSuccess(detail.success)
    }
    window.addEventListener('purehub:tool-success', onSuccess)
    return () => window.removeEventListener('purehub:tool-success', onSuccess)
  }, [miniAppId])

  const markHelpful = () => {
    if (helpful) return
    window.localStorage.setItem(helpfulKey, 'true')
    setHelpful(true)
    void trackProductEvent(miniAppId, 'helpful')
  }

  const shareTool = async () => {
    try {
      const result = await shareCard({
        title,
        headline: success?.headline ?? `${copy.usefulResult} ${title}`,
        detail: success?.shareText ?? copy.shareDetail,
      })
      setShareStatus(result)
      void trackProductEvent(miniAppId, 'share')
      window.setTimeout(() => setShareStatus(''), 1800)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return
      setShareStatus(copy.shareFailed)
    }
  }

  const sendFeedback = async () => {
    setStatus('sending')
    try {
      await submitProductFeedback(miniAppId, category, message)
      setStatus('sent')
      setMessage('')
    } catch {
      setStatus('error')
    }
  }

  return (
    <section className="app-surface rounded-[18px] p-4" aria-label={copy.feedback}>
      {success ? <div className="mb-4 rounded-[14px] border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-950 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100"><strong className="block">{success.headline}</strong><span className="mt-1 block text-xs leading-5">{success.detail}</span></div> : null}
      {completed ? <a href={downloadUrl} className="mb-4 flex min-h-12 items-center justify-between gap-3 rounded-[14px] bg-gradient-to-r from-emerald-600 to-cyan-600 px-4 text-sm font-black text-white shadow-sm"><span><strong className="block">{completionCta.label}</strong><span className="mt-0.5 block text-xs font-medium text-white/85">{completionCta.detail}</span></span><Download className="size-5 shrink-0" /></a> : null}
      {completed ? <>
        {nextTool ? <Link to={buildMiniAppPath(locale, nextTool.id)} className="mb-4 flex min-h-12 items-center justify-between gap-3 rounded-[14px] border border-slate-200 bg-white px-3 text-sm font-bold text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-white"><span><span className="block text-[11px] font-black uppercase tracking-wider text-emerald-700 dark:text-emerald-300">{copy.continue}</span>{t(nextTool.titleKey)}</span><ArrowRight className="size-4 shrink-0" /></Link> : null}
        {isTesterSprintApp ? <a href={testerSprintUrl} onClick={() => void trackJourneyEvent('tester_join')} className="mb-4 block rounded-[14px] border border-cyan-200 bg-cyan-50 px-3 py-2 text-xs font-semibold leading-5 text-cyan-950 dark:border-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-100"><strong>{copy.testerTitle}</strong> {copy.testerHint}</a> : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold text-slate-950 dark:text-white">{copy.helpedTitle}</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{copy.helpedHint}</p>
        </div>
        <div className="flex items-center gap-2">
          <button type="button" className={`filter-chip ${helpful ? 'filter-chip--active' : ''}`} onClick={markHelpful} disabled={helpful}>
            {helpful ? <Check className="size-4" /> : <Heart className="size-4" />} {helpful ? copy.helpful : copy.itHelped}
          </button>
          <button type="button" className="filter-chip" onClick={() => { setFeedbackOpen((value) => !value); setStatus('idle') }}>
            <MessageSquare className="size-4" /> {copy.feedback}
          </button>
          <button type="button" className="filter-chip" onClick={() => void shareTool()}>
            <Share2 className="size-4" /> {shareStatus || copy.share}
          </button>
        </div>
      </div>
      </> : <p className="text-xs font-medium leading-5 text-slate-500 dark:text-slate-400">{copy.finish}</p>}

      {feedbackOpen ? (
        <div className="mt-4 border-t border-slate-200 pt-4 dark:border-slate-700">
          <div className="flex items-center justify-between gap-3">
            <strong className="text-sm text-slate-900 dark:text-white">{copy.privateFeedback}</strong>
            <button type="button" className="grid size-8 place-items-center rounded-lg text-slate-500" onClick={() => setFeedbackOpen(false)} aria-label={copy.closeFeedback}><X className="size-4" /></button>
          </div>
          {status === 'sent' ? (
            <p className="mt-3 rounded-xl bg-emerald-500/10 px-3 py-2 text-sm font-semibold text-emerald-700 dark:text-emerald-300">{copy.thankYou}</p>
          ) : (
            <>
              <div className="mt-3 flex flex-wrap gap-2">
                {([['feedback', copy.general], ['bug', copy.bug], ['feature_request', copy.idea], ['device_report', copy.device]] as Array<[FeedbackCategory, string]>).map(([value, label]) => (
                  <button key={value} type="button" className={`filter-chip ${category === value ? 'filter-chip--active' : ''}`} onClick={() => setCategory(value)}>{label}</button>
                ))}
              </div>
              <textarea className="mt-3 min-h-24 w-full rounded-[14px] border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-400 dark:border-slate-700 dark:bg-slate-900 dark:text-white" value={message} onChange={(event) => setMessage(event.target.value)} maxLength={1000} placeholder={copy.placeholder} />
              <div className="mt-2 flex items-center justify-between gap-3">
                <span className="text-xs text-slate-500">{copy.privacy}</span>
                <button type="button" className="filter-chip filter-chip--active" disabled={message.trim().length < 10 || status === 'sending'} onClick={() => void sendFeedback()}><Send className="size-4" />{status === 'sending' ? copy.sending : copy.send}</button>
              </div>
              {status === 'error' ? <p className="mt-2 text-xs font-semibold text-rose-600">{copy.sendError}</p> : null}
            </>
          )}
        </div>
      ) : null}
    </section>
  )
}

import { useEffect, useState } from 'react'
import { Check, Download, Heart, MessageSquare, Send, Share2, X } from 'lucide-react'
import type { MiniAppId } from '../../features/catalog/tabs'
import { submitProductFeedback, trackProductEvent, type FeedbackCategory } from '../../lib/community-api'
import { shareCard } from '../../lib/share-card'
import type { ToolSuccess } from '../../lib/tool-success'

type MiniAppEngagementProps = {
  miniAppId: MiniAppId
  title: string
}

export function MiniAppEngagement({ miniAppId, title }: MiniAppEngagementProps) {
  const helpfulKey = `purehub-helpful-${miniAppId}`
  const [helpful, setHelpful] = useState(() => window.localStorage.getItem(helpfulKey) === 'true')
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const [category, setCategory] = useState<FeedbackCategory>('feedback')
  const [message, setMessage] = useState('')
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')
  const [shareStatus, setShareStatus] = useState('')
  const [completed, setCompleted] = useState(() => window.localStorage.getItem(`purehub-completed-${miniAppId}`) === 'true')
  const [success, setSuccess] = useState<ToolSuccess | null>(null)

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
        headline: success?.headline ?? `A useful result from ${title}`,
        detail: success?.shareText ?? 'Works without ads, tracking walls, or surprise paywalls.',
      })
      setShareStatus(result)
      void trackProductEvent(miniAppId, 'share')
      window.setTimeout(() => setShareStatus(''), 1800)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return
      setShareStatus('Could not share')
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
    <section className="app-surface rounded-[18px] p-4" aria-label="Help improve this mini app">
      {success ? <div className="mb-4 rounded-[14px] border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-950 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100"><strong className="block">{success.headline}</strong><span className="mt-1 block text-xs leading-5">{success.detail}</span></div> : null}
      {completed ? <a href={`/${window.location.pathname.split('/')[1] || 'en'}/download?utm_source=completed_tool&utm_campaign=useful_result`} className="mb-4 flex min-h-12 items-center justify-between gap-3 rounded-[14px] bg-gradient-to-r from-emerald-600 to-cyan-600 px-4 text-sm font-black text-white shadow-sm"><span>Keep this workflow offline on Android</span><Download className="size-5 shrink-0" /></a> : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold text-slate-950 dark:text-white">Did this tool help?</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">One tap helps the community choose what to improve next.</p>
        </div>
        <div className="flex items-center gap-2">
          <button type="button" className={`filter-chip ${helpful ? 'filter-chip--active' : ''}`} onClick={markHelpful} disabled={helpful}>
            {helpful ? <Check className="size-4" /> : <Heart className="size-4" />} {helpful ? 'Helpful' : 'It helped'}
          </button>
          <button type="button" className="filter-chip" onClick={() => { setFeedbackOpen((value) => !value); setStatus('idle') }}>
            <MessageSquare className="size-4" /> Feedback
          </button>
          <button type="button" className="filter-chip" onClick={() => void shareTool()}>
            <Share2 className="size-4" /> {shareStatus || 'Share'}
          </button>
        </div>
      </div>

      {feedbackOpen ? (
        <div className="mt-4 border-t border-slate-200 pt-4 dark:border-slate-700">
          <div className="flex items-center justify-between gap-3">
            <strong className="text-sm text-slate-900 dark:text-white">Private product feedback</strong>
            <button type="button" className="grid size-8 place-items-center rounded-lg text-slate-500" onClick={() => setFeedbackOpen(false)} aria-label="Close feedback"><X className="size-4" /></button>
          </div>
          {status === 'sent' ? (
            <p className="mt-3 rounded-xl bg-emerald-500/10 px-3 py-2 text-sm font-semibold text-emerald-700 dark:text-emerald-300">Thank you. Your feedback is now in the PureHub support inbox.</p>
          ) : (
            <>
              <div className="mt-3 flex flex-wrap gap-2">
                {([['feedback', 'General'], ['bug', 'Bug'], ['feature_request', 'Idea']] as Array<[FeedbackCategory, string]>).map(([value, label]) => (
                  <button key={value} type="button" className={`filter-chip ${category === value ? 'filter-chip--active' : ''}`} onClick={() => setCategory(value)}>{label}</button>
                ))}
              </div>
              <textarea className="mt-3 min-h-24 w-full rounded-[14px] border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-400 dark:border-slate-700 dark:bg-slate-900 dark:text-white" value={message} onChange={(event) => setMessage(event.target.value)} maxLength={1000} placeholder="What worked, what felt confusing, or what should change?" />
              <div className="mt-2 flex items-center justify-between gap-3">
                <span className="text-xs text-slate-500">No account, device ID, or IP is stored.</span>
                <button type="button" className="filter-chip filter-chip--active" disabled={message.trim().length < 10 || status === 'sending'} onClick={() => void sendFeedback()}><Send className="size-4" />{status === 'sending' ? 'Sending…' : 'Send'}</button>
              </div>
              {status === 'error' ? <p className="mt-2 text-xs font-semibold text-rose-600">Could not send right now. Please try again.</p> : null}
            </>
          )}
        </div>
      ) : null}
    </section>
  )
}

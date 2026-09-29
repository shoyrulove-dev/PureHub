import { Bot, Bug, Check, Code2, Download, HeartHandshake, Languages, Lightbulb, MessageCircle, Send, Smartphone, TestTube2, Vote } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { normalizeLocale } from '../i18n/locales'
import { getUiCopy } from '../i18n/ui-copy'
import { loadRoadmap, submitProductFeedback, submitRoadmapVote, trackJourneyEvent, type RoadmapOption } from '../lib/community-api'
import type { MiniAppId } from '../features/catalog/tabs'

const TELEGRAM_URL = 'https://t.me/aaa_letan_vip_bot'
const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'

export function CommunityPage() {
  const locale = normalizeLocale(useParams().lang)
  const copy = getUiCopy(locale).community
  const actions = [
    { icon: MessageCircle, title: copy.telegram, description: copy.telegramHint, href: TELEGRAM_URL, label: copy.openTelegram },
    { icon: Code2, title: copy.discussions, description: copy.discussionsHint, href: `${GITHUB_URL}/discussions`, label: copy.joinDiscussions },
    { icon: Bug, title: copy.bug, description: copy.bugHint, href: `${GITHUB_URL}/discussions/categories/ideas`, label: copy.createIssue },
    { icon: Lightbulb, title: copy.suggest, description: copy.suggestHint, href: `${GITHUB_URL}/issues/new`, label: copy.shareIdea },
  ]
  const [roadmap, setRoadmap] = useState<RoadmapOption[]>([])
  const [totalVotes, setTotalVotes] = useState(0)
  const [votedFor, setVotedFor] = useState(() => window.localStorage.getItem('purehub-roadmap-vote') ?? '')
  const [roadmapStatus, setRoadmapStatus] = useState<'loading' | 'ready' | 'voting' | 'error'>('loading')
  const [testerTool, setTesterTool] = useState<MiniAppId>('zen-pomodoro')
  const [deviceReport, setDeviceReport] = useState('')
  const [testerStatus, setTesterStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')

  useEffect(() => {
    loadRoadmap().then((payload) => {
      setRoadmap(payload.items)
      setTotalVotes(payload.total_votes)
      setRoadmapStatus('ready')
    }).catch(() => setRoadmapStatus('error'))
  }, [])

  const voteFor = async (optionId: string) => {
    if (votedFor || roadmapStatus === 'voting') return
    setRoadmapStatus('voting')
    try {
      await submitRoadmapVote(optionId)
      window.localStorage.setItem('purehub-roadmap-vote', optionId)
      setVotedFor(optionId)
      setRoadmap((items) => items.map((item) => item.option_id === optionId ? { ...item, votes: item.votes + 1 } : item))
      setTotalVotes((value) => value + 1)
      setRoadmapStatus('ready')
    } catch {
      setRoadmapStatus('error')
    }
  }
  const sendDeviceReport = async () => {
    const report = deviceReport.trim()
    if (report.length < 10 || testerStatus === 'sending') return
    setTesterStatus('sending')
    try {
      await submitProductFeedback(testerTool, 'device_report', report)
      void trackJourneyEvent('tester_join')
      setDeviceReport('')
      setTesterStatus('sent')
    } catch {
      setTesterStatus('error')
    }
  }
  return (
    <section className="space-y-7">
      <div className="hero-panel">
        <span className="eyebrow"><HeartHandshake className="size-4" /> {copy.eyebrow}</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          {copy.title}
        </h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
          {copy.intro}
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {actions.map(({ icon: Icon, title, description, href, label }) => (
          <article key={title} className="app-surface rounded-[18px] p-5">
            <span className="tool-card__icon text-emerald-600 dark:text-emerald-300"><Icon className="size-5" /></span>
            <h2 className="mt-4 font-bold text-slate-950 dark:text-white">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{description}</p>
            <a className="text-link mt-4" href={href} target="_blank" rel="noreferrer">{label}</a>
          </article>
        ))}
      </div>

      <section id="early-testers" className="rounded-[20px] border border-emerald-200 bg-emerald-50/70 p-5 dark:border-emerald-500/20 dark:bg-emerald-500/5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="tool-card__icon text-emerald-600 dark:text-emerald-300"><TestTube2 className="size-5" /></span>
            <div>
              <p className="eyebrow text-emerald-700 dark:text-emerald-300">{copy.earlyEyebrow}</p>
              <h2 className="mt-2 text-xl font-bold text-slate-950 dark:text-white">{copy.earlyTitle}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-400">{copy.earlyHint}</p>
            </div>
          </div>
          <span className="filter-chip"><Smartphone className="size-3.5" /> {copy.goal}</span>
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-[minmax(0,0.8fr)_minmax(0,1.8fr)_auto] sm:items-end">
          <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">{copy.toolTested}
            <select value={testerTool} onChange={(event) => setTesterTool(event.target.value as MiniAppId)} className="mt-2 min-h-11 w-full rounded-[14px] border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900">
              <option value="zen-habit">Zen Habit</option><option value="ocr-text">OCR Studio</option><option value="zen-pomodoro">Zen Pomodoro</option><option value="zen-breath">Zen Breath</option><option value="qr-studio">QR Studio</option>
            </select>
          </label>
          <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">{copy.deviceReport}
            <input value={deviceReport} onChange={(event) => { setDeviceReport(event.target.value); setTesterStatus('idle') }} maxLength={1000} placeholder={copy.devicePlaceholder} className="mt-2 min-h-11 w-full rounded-[14px] border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900" />
          </label>
          <button type="button" onClick={() => void sendDeviceReport()} disabled={deviceReport.trim().length < 10 || testerStatus === 'sending'} className="primary-button min-h-11 justify-center disabled:cursor-not-allowed disabled:opacity-50"><Send className="size-4" /> {testerStatus === 'sending' ? copy.sending : copy.send}</button>
        </div>
        {testerStatus === 'sent' ? <p className="mt-3 flex items-center gap-2 text-sm font-semibold text-emerald-700 dark:text-emerald-300"><Check className="size-4" /> {copy.sent}</p> : null}
        {testerStatus === 'error' ? <p className="mt-3 text-sm font-semibold text-rose-600">{copy.sendError}</p> : null}
        <p className="mt-3 text-xs text-slate-500">{copy.privacyNote}</p>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="tool-card__icon text-violet-600 dark:text-violet-300"><Vote className="size-5" /></span>
            <div><h2 className="font-bold text-slate-950 dark:text-white">{copy.roadmap}</h2><p className="mt-1 text-sm text-slate-500">{copy.roadmapHint}</p></div>
          </div>
          <span className="filter-chip">{totalVotes} {copy.votes}</span>
        </div>
        <div className="mt-4 grid gap-2 sm:grid-cols-2">
          {roadmap.map((item) => {
            const selected = votedFor === item.option_id
            const percent = totalVotes ? Math.round((item.votes / totalVotes) * 100) : 0
            return (
              <button key={item.option_id} type="button" disabled={Boolean(votedFor) || roadmapStatus === 'voting'} onClick={() => void voteFor(item.option_id)} className={`relative overflow-hidden rounded-[16px] border p-4 text-left transition ${selected ? 'border-emerald-400 bg-emerald-500/8' : 'border-slate-200 bg-white hover:border-violet-300 dark:border-slate-700 dark:bg-slate-900'}`}>
                <span className="absolute inset-y-0 left-0 bg-violet-500/6" style={{ width: `${percent}%` }} />
                <span className="relative flex items-start justify-between gap-3"><strong className="text-sm text-slate-950 dark:text-white">{item.title}</strong><span className="text-xs font-bold text-violet-600 dark:text-violet-300">{item.votes} · {percent}%</span></span>
                <span className="relative mt-2 block text-xs leading-5 text-slate-500">{item.description}</span>
                {selected ? <span className="relative mt-3 flex items-center gap-1 text-xs font-bold text-emerald-600"><Check className="size-4" /> {copy.yourVote}</span> : null}
              </button>
            )
          })}
        </div>
        {roadmapStatus === 'loading' ? <p className="mt-4 text-sm text-slate-500">{copy.loading}</p> : null}
        {roadmapStatus === 'error' ? <p className="mt-4 text-sm font-semibold text-rose-600">{copy.unavailable}</p> : null}
        <p className="mt-3 text-xs text-slate-500">{copy.voteNote}</p>
      </section>

      <a href={`/${locale}/download`} className="app-surface flex items-center justify-between gap-4 rounded-[18px] p-5">
        <span><strong className="block text-slate-950 dark:text-white">{copy.getAndroid}</strong><small className="mt-1 block text-slate-500">{copy.releaseHint}</small></span>
        <Download className="size-5 text-emerald-500" />
      </a>

      <div className="app-surface rounded-[18px] p-5">
        <h2 className="font-bold text-slate-950 dark:text-white">{copy.contribute}</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="promise-card"><Bot className="size-5 text-sky-500" /><span>{copy.testFlows}</span></div>
          <div className="promise-card"><Languages className="size-5 text-violet-500" /><span>{copy.improveTranslations}</span></div>
          <div className="promise-card"><HeartHandshake className="size-5 text-rose-500" /><span>{copy.share}</span></div>
        </div>
      </div>
    </section>
  )
}

import { Bot, Bug, Check, Code2, Download, HeartHandshake, Languages, Lightbulb, MessageCircle, Vote } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { normalizeLocale } from '../i18n/locales'
import { loadRoadmap, submitRoadmapVote, type RoadmapOption } from '../lib/community-api'

const TELEGRAM_URL = 'https://t.me/aaa_letan_vip_bot'
const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'

const actions = [
  {
    icon: MessageCircle,
    title: 'Telegram community',
    description: 'Discuss ideas, receive updates, and get help from the PureHub community.',
    href: TELEGRAM_URL,
    label: 'Open Telegram',
  },
  {
    icon: Code2,
    title: 'GitHub Discussions',
    description: 'Ask questions, share feedback, follow announcements, and help shape the roadmap.',
    href: `${GITHUB_URL}/discussions`,
    label: 'Join discussions',
  },
  {
    icon: Bug,
    title: 'Report a bug',
    description: 'Share the device, tool, and steps needed for the community to reproduce an issue.',
    href: `${GITHUB_URL}/discussions/categories/ideas`,
    label: 'Create issue',
  },
  {
    icon: Lightbulb,
    title: 'Suggest a mini app',
    description: 'Suggest a small, useful, local-first tool that can remain free and ad-free.',
    href: `${GITHUB_URL}/issues/new`,
    label: 'Share idea',
  },
]

export function CommunityPage() {
  const locale = normalizeLocale(useParams().lang)
  const [roadmap, setRoadmap] = useState<RoadmapOption[]>([])
  const [totalVotes, setTotalVotes] = useState(0)
  const [votedFor, setVotedFor] = useState(() => window.localStorage.getItem('purehub-roadmap-vote') ?? '')
  const [roadmapStatus, setRoadmapStatus] = useState<'loading' | 'ready' | 'voting' | 'error'>('loading')

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
  return (
    <section className="space-y-7">
      <div className="hero-panel">
        <span className="eyebrow"><HeartHandshake className="size-4" /> Community built</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          PureHub belongs to everyone
        </h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
          No Pro codes, no paywalls, and no ads. Telegram keeps people connected while GitHub keeps the product transparent and open.
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

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="tool-card__icon text-violet-600 dark:text-violet-300"><Vote className="size-5" /></span>
            <div><h2 className="font-bold text-slate-950 dark:text-white">Community roadmap vote</h2><p className="mt-1 text-sm text-slate-500">Choose the miniapp upgrade that should receive the next deep product pass.</p></div>
          </div>
          <span className="filter-chip">{totalVotes} votes</span>
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
                {selected ? <span className="relative mt-3 flex items-center gap-1 text-xs font-bold text-emerald-600"><Check className="size-4" /> Your vote</span> : null}
              </button>
            )
          })}
        </div>
        {roadmapStatus === 'loading' ? <p className="mt-4 text-sm text-slate-500">Loading roadmap…</p> : null}
        {roadmapStatus === 'error' ? <p className="mt-4 text-sm font-semibold text-rose-600">Roadmap is temporarily unavailable.</p> : null}
        <p className="mt-3 text-xs text-slate-500">One vote is stored only in this browser. No account or personal identifier is collected.</p>
      </section>

      <a href={`/${locale}/download`} className="app-surface flex items-center justify-between gap-4 rounded-[18px] p-5">
        <span><strong className="block text-slate-950 dark:text-white">Get PureHub for Android</strong><small className="mt-1 block text-slate-500">Signed APK releases, checksums, and changelog</small></span>
        <Download className="size-5 text-emerald-500" />
      </a>

      <div className="app-surface rounded-[18px] p-5">
        <h2 className="font-bold text-slate-950 dark:text-white">Ways to contribute</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="promise-card"><Bot className="size-5 text-sky-500" /><span>Test the bot and community flows</span></div>
          <div className="promise-card"><Languages className="size-5 text-violet-500" /><span>Improve Vietnamese or Chinese translations</span></div>
          <div className="promise-card"><HeartHandshake className="size-5 text-rose-500" /><span>Share PureHub with someone who needs it</span></div>
        </div>
      </div>
    </section>
  )
}

import { Bot, Bug, Code2, Download, HeartHandshake, Languages, Lightbulb, MessageCircle } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { normalizeLocale } from '../i18n/locales'

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

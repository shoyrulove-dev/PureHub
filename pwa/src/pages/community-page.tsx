import { Bot, Bug, Code2, HeartHandshake, Languages, Lightbulb, MessageCircle } from 'lucide-react'

const TELEGRAM_URL = 'https://t.me/aaa_letan_vip_bot'
const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'

const actions = [
  {
    icon: MessageCircle,
    title: 'Telegram community',
    description: 'Trao đổi, nhận tin cập nhật và hỗ trợ người dùng PureHub.',
    href: TELEGRAM_URL,
    label: 'Mở Telegram',
  },
  {
    icon: Code2,
    title: 'Open-source on GitHub',
    description: 'Xem mã nguồn, báo lỗi, thảo luận và gửi pull request.',
    href: GITHUB_URL,
    label: 'Mở GitHub',
  },
  {
    icon: Bug,
    title: 'Báo lỗi',
    description: 'Mô tả thiết bị, công cụ và các bước để cộng đồng tái hiện lỗi.',
    href: `${GITHUB_URL}/issues/new`,
    label: 'Tạo issue',
  },
  {
    icon: Lightbulb,
    title: 'Đề xuất mini app',
    description: 'Đề xuất công cụ nhỏ, hữu ích, có thể chạy local-first và không quảng cáo.',
    href: `${GITHUB_URL}/issues/new`,
    label: 'Gửi ý tưởng',
  },
]

export function CommunityPage() {
  return (
    <section className="space-y-7">
      <div className="hero-panel">
        <span className="eyebrow"><HeartHandshake className="size-4" /> Community built</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          PureHub thuộc về mọi người
        </h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
          Không mã Pro, không paywall và không quảng cáo. Telegram giúp mọi người kết nối; GitHub giữ cho sản phẩm minh bạch và mở.
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

      <div className="app-surface rounded-[18px] p-5">
        <h2 className="font-bold text-slate-950 dark:text-white">Cách đóng góp</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="promise-card"><Bot className="size-5 text-sky-500" /><span>Test bot và luồng community</span></div>
          <div className="promise-card"><Languages className="size-5 text-violet-500" /><span>Đóng góp bản dịch tự nhiên</span></div>
          <div className="promise-card"><HeartHandshake className="size-5 text-rose-500" /><span>Chia sẻ PureHub với người cần</span></div>
        </div>
      </div>
    </section>
  )
}

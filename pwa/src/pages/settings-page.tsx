import { Check, Languages, Moon, ShieldCheck, Sun, SunMoon } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { SUPPORTED_LOCALES, normalizeLocale } from '../i18n/locales'
import { persistSelectedLocale } from '../i18n/routing'
import { type ThemePreference, useThemePreference } from '../lib/preferences'

const themes: Array<{ id: ThemePreference; label: string; icon: typeof Sun }> = [
  { id: 'system', label: 'Theo thiết bị', icon: SunMoon },
  { id: 'light', label: 'Sáng', icon: Sun },
  { id: 'dark', label: 'Tối', icon: Moon },
]

export function SettingsPage() {
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const { theme, setTheme } = useThemePreference()

  return (
    <section className="space-y-6">
      <div>
        <p className="eyebrow">PureHub preferences</p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-white">Cài đặt</h1>
      </div>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-center gap-3">
          <span className="tool-card__icon text-amber-500"><SunMoon className="size-5" /></span>
          <div><h2 className="font-bold text-slate-950 dark:text-white">Giao diện</h2><p className="text-sm text-slate-500">Chọn theme phù hợp với bạn.</p></div>
        </div>
        <div className="mt-4 grid gap-2 sm:grid-cols-3">
          {themes.map(({ id, label, icon: Icon }) => (
            <button key={id} type="button" className={`promise-card cursor-pointer text-left ${theme === id ? 'ring-2 ring-emerald-400/35' : ''}`} onClick={() => setTheme(id)}>
              <Icon className="size-5" /><strong className="flex-1">{label}</strong>{theme === id ? <Check className="size-4 text-emerald-500" /> : null}
            </button>
          ))}
        </div>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-center gap-3">
          <span className="tool-card__icon text-sky-500"><Languages className="size-5" /></span>
          <div><h2 className="font-bold text-slate-950 dark:text-white">Ngôn ngữ</h2><p className="text-sm text-slate-500">Đổi ngôn ngữ nhưng giữ nguyên khu vực đang xem.</p></div>
        </div>
        <div className="mt-4 flex gap-2">
          {SUPPORTED_LOCALES.map((item) => (
            <Link
              key={item}
              to={`/${item}/settings`}
              onClick={() => persistSelectedLocale(item)}
              className={`filter-chip uppercase ${locale === item ? 'filter-chip--active' : ''}`}
            >
              {item}
            </Link>
          ))}
        </div>
      </section>

      <section className="app-surface rounded-[18px] p-5">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 text-emerald-500" />
          <div>
            <h2 className="font-bold text-slate-950 dark:text-white">Cam kết riêng tư</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
              PureHub không hiển thị quảng cáo và ưu tiên xử lý dữ liệu ngay trên thiết bị. Các quyền camera, microphone và cảm biến chỉ được yêu cầu khi một công cụ thực sự cần.
            </p>
          </div>
        </div>
      </section>
    </section>
  )
}

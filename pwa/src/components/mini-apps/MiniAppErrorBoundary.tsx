import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RotateCcw, ShieldCheck } from 'lucide-react'
import { normalizeLocale } from '../../i18n/locales'

const ERROR_COPY = {
  en: ['This tool paused safely', 'Your other PureHub tools are unaffected. Retry this mini-app without reloading the whole app.', 'Retry tool'],
  vi: ['Công cụ đã tạm dừng an toàn', 'Các công cụ PureHub khác không bị ảnh hưởng. Hãy thử lại mini-app này mà không cần tải lại toàn bộ ứng dụng.', 'Thử lại'],
  zh: ['工具已安全暂停', '其他 PureHub 工具不受影响。无需重新加载整个应用即可重试此工具。', '重试'],
  es: ['La herramienta se pausó de forma segura', 'Las demás herramientas de PureHub no se ven afectadas. Vuelve a intentarlo sin recargar toda la aplicación.', 'Reintentar'],
} as const

type Props = { appId: string; children: ReactNode }
type State = { failed: boolean }

export class MiniAppErrorBoundary extends Component<Props, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (import.meta.env.DEV) console.error(`Mini-app ${this.props.appId} failed`, error, info)
  }

  render() {
    if (!this.state.failed) return this.props.children
    const locale = normalizeLocale(typeof window === 'undefined' ? 'en' : window.location.pathname.split('/')[1])
    const copy = ERROR_COPY[locale]
    return <div className="app-surface rounded-[18px] border border-amber-300/40 p-6 text-center">
      <ShieldCheck className="mx-auto size-9 text-amber-600" />
      <h2 className="mt-3 text-lg font-black text-slate-950 dark:text-white">{copy[0]}</h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">{copy[1]}</p>
      <button type="button" className="mt-4 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-4 py-2 text-sm font-bold text-white dark:bg-white dark:text-slate-950" onClick={() => this.setState({ failed: false })}><RotateCcw className="size-4" />{copy[2]}</button>
    </div>
  }
}

import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RotateCcw, ShieldCheck } from 'lucide-react'

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
    return <div className="app-surface rounded-[18px] border border-amber-300/40 p-6 text-center">
      <ShieldCheck className="mx-auto size-9 text-amber-600" />
      <h2 className="mt-3 text-lg font-black text-slate-950 dark:text-white">This tool paused safely</h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">Your other PureHub tools are unaffected. Retry this mini-app without reloading the whole app.</p>
      <button type="button" className="mt-4 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-4 py-2 text-sm font-bold text-white dark:bg-white dark:text-slate-950" onClick={() => this.setState({ failed: false })}><RotateCcw className="size-4" />Retry tool</button>
    </div>
  }
}

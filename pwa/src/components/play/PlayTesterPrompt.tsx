import { ExternalLink, TestTube2 } from 'lucide-react'
import { useState } from 'react'
import type { LocaleCode } from '../../i18n/locales'
import { PLAY_TESTER_INVITE_DISMISSED_KEY, openPlayTesterGroup } from '../../lib/play-testers'

const copy: Record<LocaleCode, { title: string; body: string; join: string; later: string }> = {
  en: { title: 'Help test PureHub on Google Play', body: 'Join the tester group to receive the closed-test opt-in link when the Play track is ready.', join: 'Join tester group', later: 'Later' },
  vi: { title: 'Tham gia thử nghiệm PureHub trên CH Play', body: 'Vào nhóm tester để nhận link opt-in closed test khi Play Console sẵn sàng.', join: 'Tham gia nhóm tester', later: 'Để sau' },
  zh: { title: '加入 PureHub Google Play 测试', body: '加入测试群，Google Play 封闭测试准备好后即可收到加入链接。', join: '加入测试群', later: '以后再说' },
  es: { title: 'Ayuda a probar PureHub en Google Play', body: 'Únete al grupo para recibir el enlace de acceso de la prueba cerrada cuando esté listo.', join: 'Unirme al grupo de prueba', later: 'Más tarde' },
}

export function PlayTesterPrompt({ locale }: { locale: LocaleCode }) {
  const [visible, setVisible] = useState(() => !window.localStorage.getItem(PLAY_TESTER_INVITE_DISMISSED_KEY))
  if (!visible) return null
  const text = copy[locale]
  const dismiss = () => {
    window.localStorage.setItem(PLAY_TESTER_INVITE_DISMISSED_KEY, 'true')
    setVisible(false)
  }
  const join = () => {
    dismiss()
    openPlayTesterGroup()
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 p-3 sm:p-5" role="dialog" aria-modal="true" aria-label={text.title}>
      <div className="mx-auto max-w-lg rounded-[24px] border border-emerald-300/40 bg-white p-5 shadow-2xl dark:border-emerald-500/30 dark:bg-slate-900">
        <div className="flex items-start gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-emerald-500/15 text-emerald-600 dark:text-emerald-300"><TestTube2 className="size-5" /></span><div><h2 className="font-bold text-slate-950 dark:text-white">{text.title}</h2><p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">{text.body}</p></div></div>
        <div className="mt-4 flex gap-2"><button type="button" onClick={join} className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 text-sm font-bold text-slate-950"><ExternalLink className="size-4" />{text.join}</button><button type="button" onClick={dismiss} className="min-h-11 rounded-xl border border-slate-300 px-4 text-sm font-semibold text-slate-700 dark:border-slate-700 dark:text-slate-200">{text.later}</button></div>
      </div>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { GiCrystalBall, GiStarsStack } from 'react-icons/gi'

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

export function PwaInstallPrompt() {
  const { t } = useTranslation()
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault()
      setDeferredPrompt(event as BeforeInstallPromptEvent)
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    return () => window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
  }, [])

  if (!deferredPrompt || dismissed) {
    return null
  }

  return (
    <div className="mb-4 rounded-[28px] border border-emerald-400/14 bg-[linear-gradient(135deg,rgba(16,185,129,0.12),rgba(15,23,42,0.98),rgba(168,85,247,0.18))] p-[1px] shadow-[0_28px_90px_-52px_rgba(16,185,129,0.45)]">
      <div className="rounded-[27px] bg-slate-950/96 p-4 sm:p-5">
        <div className="flex items-start gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-white/8 bg-white/5 text-emerald-300">
            <GiCrystalBall className="text-[1.4rem]" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 text-emerald-300">
              <GiStarsStack className="text-sm" />
              <p className="text-xs font-semibold uppercase tracking-[0.28em]">
                {t('pwa.installBadge')}
              </p>
            </div>
            <p className="mt-2 text-base font-semibold text-white">{t('pwa.installTitle')}</p>
            <p className="mt-2 text-sm leading-6 text-slate-300">{t('pwa.installDescription')}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={async () => {
                  await deferredPrompt.prompt()
                  await deferredPrompt.userChoice
                  setDeferredPrompt(null)
                }}
                className="rounded-2xl bg-emerald-400/14 px-4 py-2.5 text-sm font-semibold text-emerald-200 ring-1 ring-inset ring-emerald-400/18 transition hover:bg-emerald-400/18"
              >
                {t('pwa.installNow')}
              </button>
              <button
                type="button"
                onClick={() => setDismissed(true)}
                className="rounded-2xl bg-white/5 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/8"
              >
                {t('pwa.later')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

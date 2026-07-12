import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { GiCrystalBall } from 'react-icons/gi'

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

export function PwaInstallPrompt() {
  const { t } = useTranslation()
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault()
      setDeferredPrompt(event as BeforeInstallPromptEvent)
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    return () => window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
  }, [])

  if (!deferredPrompt) {
    return null
  }

  return (
    <div className="relative">
      <button
        type="button"
        aria-label={t('pwa.installIconLabel')}
        onClick={() => setOpen((value) => !value)}
        className="flex size-10 items-center justify-center rounded-2xl border border-emerald-400/14 bg-white/5 text-emerald-300 shadow-[0_18px_50px_-22px_rgba(16,185,129,0.28)] transition hover:bg-white/8"
      >
        <GiCrystalBall className="text-[1.1rem]" />
      </button>

      {open ? (
        <div className="absolute right-0 top-12 z-30 w-72 rounded-[24px] border border-emerald-400/14 bg-slate-950/96 p-4 shadow-[0_28px_90px_-52px_rgba(16,185,129,0.45)] backdrop-blur-xl">
          <p className="text-sm font-semibold text-white">{t('pwa.installTitle')}</p>
          <p className="mt-2 text-sm leading-6 text-slate-300">{t('pwa.installDescription')}</p>
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={async () => {
                await deferredPrompt.prompt()
                await deferredPrompt.userChoice
                setDeferredPrompt(null)
                setOpen(false)
              }}
              className="rounded-2xl bg-emerald-400/14 px-4 py-2.5 text-sm font-semibold text-emerald-200 ring-1 ring-inset ring-emerald-400/18 transition hover:bg-emerald-400/18"
            >
              {t('pwa.installNow')}
            </button>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-2xl bg-white/5 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/8"
            >
              {t('pwa.later')}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}

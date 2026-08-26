import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, CheckCircle2, Copy, ExternalLink, MonitorDown, Share2, Smartphone, X } from 'lucide-react'
import { trackJourneyEvent } from '../../lib/community-api'

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

export function PwaInstallPrompt() {
  const { t } = useTranslation()
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [open, setOpen] = useState(false)
  const [installed, setInstalled] = useState(false)
  const [copied, setCopied] = useState(false)
  const trackInstallAccepted = () => {
    const key = `purehub-pwa_install_accepted-${new Date().toISOString().slice(0, 10)}`
    if (window.localStorage.getItem(key)) return
    window.localStorage.setItem(key, 'true')
    void trackJourneyEvent('pwa_install_accepted')
  }

  useEffect(() => {
    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault()
      setDeferredPrompt(event as BeforeInstallPromptEvent)
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    const standalone = window.matchMedia('(display-mode: standalone)')
    const updateInstalled = () => {
      const nextInstalled = standalone.matches || Boolean((navigator as Navigator & { standalone?: boolean }).standalone)
      setInstalled(nextInstalled)
      if (nextInstalled) {
        const key = `purehub-installed-open-${new Date().toISOString().slice(0, 10)}`
        if (!window.localStorage.getItem(key)) {
          window.localStorage.setItem(key, 'true')
          void trackJourneyEvent('installed_open')
        }
      }
    }
    const onInstalled = () => {
      updateInstalled()
      trackInstallAccepted()
    }
    updateInstalled()
    standalone.addEventListener('change', updateInstalled)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
      standalone.removeEventListener('change', updateInstalled)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [])

  const trackInstallStep = (stage: 'pwa_install_prompt_opened' | 'pwa_install_guide_viewed' | 'pwa_install_dismissed') => {
    const key = `purehub-${stage}-${new Date().toISOString().slice(0, 10)}`
    if (window.localStorage.getItem(key)) return
    window.localStorage.setItem(key, 'true')
    void trackJourneyEvent(stage)
  }

  if (installed) return null

  const userAgent = navigator.userAgent.toLowerCase()
  const isIos = /iphone|ipad|ipod/.test(userAgent)
  const isAndroid = /android/.test(userAgent)
  const isMetaBrowser = /fban|fbav|instagram/.test(userAgent)
  const chromeIntentUrl = isAndroid
    ? `intent://${window.location.host}${window.location.pathname}${window.location.search}#Intent;scheme=https;package=com.android.chrome;end`
    : ''
  const installHint = deferredPrompt
    ? t('pwa.installReadyHint')
    : isMetaBrowser
      ? t('pwa.installMetaHint')
      : isIos
        ? t('pwa.installIosHint')
        : t('pwa.installHint')
  const guideSteps = isMetaBrowser
    ? isIos
      ? [t('pwa.installMetaMenuStep'), t('pwa.installMetaSafariStep'), t('pwa.installMetaIosStep')]
      : [t('pwa.installMetaMenuStep'), t('pwa.installMetaChromeStep'), t('pwa.installMetaAndroidStep')]
    : isIos
      ? ['Tap the Share button in Safari.', 'Choose “Add to Home Screen”, then tap Add.']
      : ['Open your browser menu.', 'Choose “Install app” or “Add to Home screen”.']

  const copyCurrentLink = async () => {
    const value = window.location.href
    let copiedLink = false
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(value)
        copiedLink = true
      }
    } catch {
      copiedLink = false
    }
    if (!copiedLink) {
      const input = document.createElement('textarea')
      input.value = value
      input.style.position = 'fixed'
      input.style.opacity = '0'
      document.body.appendChild(input)
      input.select()
      copiedLink = document.execCommand('copy')
      input.remove()
    }
    if (copiedLink) {
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="relative">
      <button
        type="button"
        aria-label={t('pwa.installIconLabel')}
        onClick={() => setOpen((value) => {
          const next = !value
          if (next) {
            trackInstallStep('pwa_install_prompt_opened')
            if (!deferredPrompt) trackInstallStep('pwa_install_guide_viewed')
          }
          return next
        })}
        className="flex min-h-10 items-center gap-1.5 rounded-[13px] border border-emerald-500/30 bg-emerald-500 px-3 text-sm font-black text-white shadow-[0_18px_50px_-22px_rgba(16,185,129,0.5)] transition hover:bg-emerald-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-emerald-400/35"
      >
        <img src="/icons/app-icon-192.png" alt="" className="size-5 rounded-md" />
        <span>{t('pwa.installButton')}</span>
      </button>

      {open ? (
        <div className="absolute right-0 top-12 z-30 w-[min(22rem,calc(100vw-1.5rem))] rounded-[20px] border border-emerald-400/20 bg-slate-950/98 p-4 shadow-[0_28px_90px_-52px_rgba(16,185,129,0.55)] backdrop-blur-xl">
          <div className="flex items-start justify-between gap-3">
            <div><p className="flex items-center gap-2 text-sm font-black text-white"><Smartphone className="size-4 text-emerald-300" />{t('pwa.installTitle')}</p><p className="mt-1 text-xs font-semibold text-emerald-200">{t('pwa.installBenefit')}</p></div>
            <button type="button" onClick={() => { trackInstallStep('pwa_install_dismissed'); setOpen(false) }} className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-white/10 hover:text-white" aria-label={t('pwa.later')}><X className="size-4" /></button>
          </div>
          <p className="mt-3 text-sm leading-6 text-slate-300">{t('pwa.installDescription')}</p>
          <p className="mt-2 text-xs font-semibold text-slate-400">Takes about 10 seconds · works offline · no account needed.</p>
          <div className="mt-3 flex items-start gap-2 rounded-[14px] bg-white/6 p-3 text-xs leading-5 text-slate-200">
            {isMetaBrowser ? <ExternalLink className="mt-0.5 size-4 shrink-0 text-emerald-300" /> : isIos ? <Share2 className="mt-0.5 size-4 shrink-0 text-emerald-300" /> : deferredPrompt ? <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-300" /> : <MonitorDown className="mt-0.5 size-4 shrink-0 text-emerald-300" />}
            <span>{installHint}</span>
          </div>
          {!deferredPrompt ? <ol className="mt-3 space-y-2 rounded-[14px] border border-white/10 bg-white/[.035] p-3 text-xs font-semibold leading-5 text-slate-200">
            {guideSteps.map((step, index) => <li key={step} className="flex gap-2"><span className="grid size-5 shrink-0 place-items-center rounded-full bg-emerald-400 font-black text-slate-950">{index + 1}</span><span>{step}</span></li>)}
          </ol> : null}
          <div className="mt-4 flex flex-wrap gap-2">
            {deferredPrompt ? (
              <button
                type="button"
                onClick={async () => {
                  await deferredPrompt.prompt()
                  const choice = await deferredPrompt.userChoice
                  if (choice.outcome === 'accepted') trackInstallAccepted()
                  setDeferredPrompt(null)
                  setOpen(false)
                }}
                className="rounded-[12px] bg-emerald-400 px-4 py-2.5 text-sm font-black text-slate-950 transition hover:bg-emerald-300"
              >
                {t('pwa.installNow')}
              </button>
            ) : isMetaBrowser ? (
              <>
                {chromeIntentUrl ? (
                  <a
                    href={chromeIntentUrl}
                    className="inline-flex items-center gap-2 rounded-[12px] bg-emerald-400 px-4 py-2.5 text-sm font-black text-slate-950 transition hover:bg-emerald-300"
                  >
                    <ExternalLink className="size-4" /> {t('pwa.openInChrome')}
                  </a>
                ) : null}
                <button
                  type="button"
                  onClick={() => void copyCurrentLink()}
                  className="inline-flex items-center gap-2 rounded-[12px] bg-white/5 px-4 py-2.5 text-sm font-semibold text-slate-200 transition hover:bg-white/10"
                >
                  {copied ? <Check className="size-4 text-emerald-300" /> : <Copy className="size-4" />}
                  {copied ? t('pwa.linkCopied') : t('pwa.copyLink')}
                </button>
              </>
            ) : (
              <div className="rounded-[12px] bg-white/5 px-4 py-2.5 text-sm text-slate-300">
                {t('pwa.installGuideLabel')}
              </div>
            )}
            <button
              type="button"
              onClick={() => { trackInstallStep('pwa_install_dismissed'); setOpen(false) }}
              className="rounded-[12px] bg-white/5 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/8"
            >
              {t('pwa.later')}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}

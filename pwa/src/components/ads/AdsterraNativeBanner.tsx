import { useEffect, useRef } from 'react'

const SCRIPT_SRC = 'https://pl31407675.profitableratecpmnetwork.com/9a2e2697d053d95dc97b895e6e6a2f61/invoke.js'
const CONTAINER_ID = 'container-9a2e2697d053d95dc97b895e6e6a2f61'

/**
 * Web-only ad slot for SEO pages. It is intentionally not mounted by any
 * mini-app surface, so camera/OCR workflows remain ad-free.
 */
export function AdsterraNativeBanner() {
  const slotRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const slot = slotRef.current
    if (!slot || slot.querySelector(`#${CONTAINER_ID}`)) return

    const script = document.createElement('script')
    script.async = true
    script.setAttribute('data-cfasync', 'false')
    script.src = SCRIPT_SRC

    const container = document.createElement('div')
    container.id = CONTAINER_ID
    slot.append(script, container)

    return () => {
      script.remove()
      container.remove()
    }
  }, [])

  return (
    <aside ref={slotRef} className="mx-auto min-h-20 w-full max-w-3xl overflow-hidden" aria-label="Advertisement" />
  )
}

import type { MiniAppId } from '../features/catalog/tabs'
import { trackProductEvent } from './community-api'

export type ToolSuccess = {
  headline: string
  detail: string
  shareText: string
}

export function markToolSuccess(miniAppId: MiniAppId, success: ToolSuccess) {
  const day = new Date().toISOString().slice(0, 10)
  const key = `purehub-success-${miniAppId}-${day}`
  const firstSuccessToday = typeof window === 'undefined' || !window.localStorage.getItem(key)
  if (typeof window !== 'undefined' && firstSuccessToday) window.localStorage.setItem(key, 'true')
  if (firstSuccessToday) void trackProductEvent(miniAppId, 'complete')
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('purehub:tool-success', { detail: { miniAppId, success } }))
  }
}

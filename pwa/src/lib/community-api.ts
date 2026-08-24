import type { MiniAppId } from '../features/catalog/tabs'
import { anonymousMetricsEnabled } from './preferences'

export type ProductEvent = 'open' | 'complete' | 'helpful' | 'share' | 'feedback'
export type FeedbackCategory = 'feedback' | 'bug' | 'feature_request' | 'device_report'
export type JourneyStage =
  | 'visit'
  | 'download'
  | 'apk_download_click'
  | 'pwa_install_prompt_opened'
  | 'pwa_install_guide_viewed'
  | 'pwa_install_accepted'
  | 'pwa_install_dismissed'
  | 'installed_open'
  | 'tester_join'

export type RoadmapOption = {
  option_id: string
  title: string
  description: string
  votes: number
}

async function postJson(path: string, payload: Record<string, unknown>) {
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    keepalive: true,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as { detail?: string }
    throw new Error(error.detail || 'PureHub could not save this request.')
  }
  return response.json() as Promise<Record<string, unknown>>
}

export function trackProductEvent(miniAppId: MiniAppId, event: ProductEvent) {
  if (event === 'complete' && typeof window !== 'undefined') {
    window.localStorage.setItem(`purehub-completed-${miniAppId}`, 'true')
    window.dispatchEvent(new CustomEvent('purehub:product-complete', { detail: { miniAppId } }))
  }
  if (!anonymousMetricsEnabled()) return Promise.resolve()
  return postJson('/public-api/product-event', { miniapp_id: miniAppId, event }).then(() => undefined).catch(() => undefined)
}

export function trackJourneyEvent(stage: JourneyStage) {
  if (!anonymousMetricsEnabled()) return Promise.resolve()
  const query = new URLSearchParams(window.location.search)
  const source = query.get('utm_source') || document.referrer.split('/')[2] || 'direct'
  const campaign = query.get('utm_campaign') || 'none'
  return postJson('/public-api/journey-event', { stage, source, campaign }).then(() => undefined).catch(() => undefined)
}

export function submitProductFeedback(miniAppId: MiniAppId, category: FeedbackCategory, message: string) {
  return postJson('/public-api/feedback', { miniapp_id: miniAppId, category, message, website: '' })
}

export async function loadRoadmap() {
  const response = await fetch('/public-api/roadmap', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('Roadmap is temporarily unavailable.')
  return response.json() as Promise<{ items: RoadmapOption[]; total_votes: number }>
}

export function submitRoadmapVote(optionId: string) {
  return postJson('/public-api/roadmap/vote', { option_id: optionId })
}

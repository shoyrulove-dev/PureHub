import type { MiniAppId } from '../features/catalog/tabs'
import { anonymousMetricsEnabled } from './preferences'

export type ProductEvent = 'open' | 'helpful' | 'share' | 'feedback'
export type FeedbackCategory = 'feedback' | 'bug' | 'feature_request'

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
  if (!anonymousMetricsEnabled()) return Promise.resolve()
  return postJson('/public-api/product-event', { miniapp_id: miniAppId, event }).then(() => undefined).catch(() => undefined)
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

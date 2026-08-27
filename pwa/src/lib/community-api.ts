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

type JourneyAttribution = {
  source: string
  campaign: string
}

const JOURNEY_ATTRIBUTION_KEY = 'purehub-journey-attribution-v1'

export function normalizeJourneySource(value: string | null | undefined) {
  const raw = (value || '').trim().toLowerCase()
  if (!raw) return 'direct'

  let candidate = raw
  try {
    candidate = new URL(raw.includes('://') ? raw : `https://${raw}`).hostname.toLowerCase()
  } catch {
    candidate = raw
  }

  if (candidate === 'facebook' || candidate === 'fb' || candidate === 'fb.com' || candidate === 'fb.me') return 'facebook'
  if (candidate === 'facebook.com' || candidate.endsWith('.facebook.com')) return 'facebook'
  if (candidate === 'google.com' || candidate.endsWith('.google.com')) return 'google'
  if (candidate === 'github.com' || candidate.endsWith('.github.com')) return 'github'
  return candidate.replace(/[^a-z0-9._-]/g, '') || 'direct'
}

function getJourneyAttribution(): JourneyAttribution {
  const query = new URLSearchParams(window.location.search)
  const explicitSource = query.get('utm_source')
  const explicitCampaign = query.get('utm_campaign')
  let stored: JourneyAttribution | null = null

  try {
    stored = JSON.parse(window.sessionStorage.getItem(JOURNEY_ATTRIBUTION_KEY) || 'null') as JourneyAttribution | null
  } catch {
    stored = null
  }

  let referrer = ''
  try {
    referrer = document.referrer ? new URL(document.referrer).hostname : ''
  } catch {
    referrer = ''
  }

  const referrerSource = referrer && referrer !== window.location.hostname
    ? normalizeJourneySource(referrer)
    : ''
  const source = normalizeJourneySource(explicitSource || referrerSource || stored?.source || 'direct')

  const attribution = {
    source,
    campaign: (explicitCampaign || (explicitSource || referrerSource ? 'none' : stored?.campaign) || 'none').trim().toLowerCase(),
  }

  try {
    window.sessionStorage.setItem(JOURNEY_ATTRIBUTION_KEY, JSON.stringify(attribution))
  } catch {
    // Metrics remain best-effort when storage is unavailable.
  }
  return attribution
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
  const { source, campaign } = getJourneyAttribution()
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

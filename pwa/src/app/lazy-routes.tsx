import { lazy, Suspense, type ReactNode } from 'react'

export const LocalizedEntryPage = lazy(() => import('../pages/localized-entry-page').then((module) => ({ default: module.LocalizedEntryPage })))
export const LocaleIndexPage = lazy(() => import('../pages/locale-index-page').then((module) => ({ default: module.LocaleIndexPage })))
export const ToolsPage = lazy(() => import('../pages/tools-page').then((module) => ({ default: module.ToolsPage })))
export const CommunityPage = lazy(() => import('../pages/community-page').then((module) => ({ default: module.CommunityPage })))
export const SettingsPage = lazy(() => import('../pages/settings-page').then((module) => ({ default: module.SettingsPage })))
export const DownloadPage = lazy(() => import('../pages/download-page').then((module) => ({ default: module.DownloadPage })))
export const ChangelogPage = lazy(() => import('../pages/changelog-page').then((module) => ({ default: module.ChangelogPage })))
export const PrivacyPage = lazy(() => import('../pages/legal-pages').then((module) => ({ default: module.PrivacyPage })))
export const TermsPage = lazy(() => import('../pages/legal-pages').then((module) => ({ default: module.TermsPage })))
export const GrowthLandingPage = lazy(() => import('../pages/growth-landing-page').then((module) => ({ default: module.GrowthLandingPage })))
export const ResultsPage = lazy(() => import('../pages/results-page').then((module) => ({ default: module.ResultsPage })))
export const PrivacyCenterPage = lazy(() => import('../pages/privacy-center-page').then((module) => ({ default: module.PrivacyCenterPage })))
export const MinigamePage = lazy(() => import('../pages/minigame-page').then((module) => ({ default: module.MinigamePage })))

export function RouteLoader({ children }: { children: ReactNode }) {
  return <Suspense fallback={<div className="mx-auto mt-10 h-24 w-full max-w-3xl animate-pulse rounded-3xl bg-slate-200/70 dark:bg-slate-800/70" />}>{children}</Suspense>
}

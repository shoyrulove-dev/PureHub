import { createBrowserRouter } from 'react-router-dom'
import { LocaleLayout } from './locale-layout'
import { LocaleRedirectPage } from '../pages/locale-redirect-page'
import { BackendRouteRecovery } from '../pages/backend-route-recovery'
import { GROWTH_LANDING_IDS, growthLandingPages } from '../config/growthLandingPages'
import { ChangelogPage, CommunityPage, DownloadPage, GrowthLandingPage, LocaleIndexPage, LocalizedEntryPage, MinigamePage, PrivacyCenterPage, PrivacyPage, ResultsPage, RouteLoader, SettingsPage, TermsPage, ToolsPage } from './lazy-routes'

export const appRouter = createBrowserRouter([
  {
    path: '/admin/*',
    element: <BackendRouteRecovery />,
  },
  {
    path: '/public-api/*',
    element: <BackendRouteRecovery />,
  },
  {
    path: '/api/*',
    element: <BackendRouteRecovery />,
  },
  {
    path: '/',
    element: <LocaleRedirectPage />,
  },
  {
    path: '/:lang',
    element: <LocaleLayout />,
    children: [
      {
        index: true,
        element: <RouteLoader><LocaleIndexPage /></RouteLoader>,
      },
      {
        path: 'tools',
        element: <RouteLoader><ToolsPage /></RouteLoader>,
      },
      {
        path: 'community',
        element: <RouteLoader><CommunityPage /></RouteLoader>,
      },
      {
        path: 'settings',
        element: <RouteLoader><SettingsPage /></RouteLoader>,
      },
      {
        path: 'results',
        element: <RouteLoader><ResultsPage /></RouteLoader>,
      },
      {
        path: 'privacy-center',
        element: <RouteLoader><PrivacyCenterPage /></RouteLoader>,
      },
      {
        path: 'download',
        element: <RouteLoader><DownloadPage /></RouteLoader>,
      },
      {
        path: 'changelog',
        element: <RouteLoader><ChangelogPage /></RouteLoader>,
      },
      {
        path: 'minigame',
        element: <RouteLoader><MinigamePage /></RouteLoader>,
      },
      {
        path: 'privacy',
        element: <RouteLoader><PrivacyPage /></RouteLoader>,
      },
      {
        path: 'terms',
        element: <RouteLoader><TermsPage /></RouteLoader>,
      },
      ...GROWTH_LANDING_IDS.map((landingId) => ({
        path: growthLandingPages[landingId].slug,
        element: <RouteLoader><GrowthLandingPage landingId={landingId} /></RouteLoader>,
      })),
      {
        path: ':appSlug',
        element: <RouteLoader><LocalizedEntryPage /></RouteLoader>,
      },
    ],
  },
])

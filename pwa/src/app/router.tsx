import { createBrowserRouter } from 'react-router-dom'
import { LocaleLayout } from './locale-layout'
import { LocalizedEntryPage } from '../pages/localized-entry-page'
import { LocaleIndexPage } from '../pages/locale-index-page'
import { LocaleRedirectPage } from '../pages/locale-redirect-page'
import { ToolsPage } from '../pages/tools-page'
import { CommunityPage } from '../pages/community-page'
import { SettingsPage } from '../pages/settings-page'
import { DownloadPage } from '../pages/download-page'
import { ChangelogPage } from '../pages/changelog-page'
import { BackendRouteRecovery } from '../pages/backend-route-recovery'
import { PrivacyPage, TermsPage } from '../pages/legal-pages'
import { GrowthLandingPage } from '../pages/growth-landing-page'
import { GROWTH_LANDING_IDS, growthLandingPages } from '../config/growthLandingPages'

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
        element: <LocaleIndexPage />,
      },
      {
        path: 'tools',
        element: <ToolsPage />,
      },
      {
        path: 'community',
        element: <CommunityPage />,
      },
      {
        path: 'settings',
        element: <SettingsPage />,
      },
      {
        path: 'download',
        element: <DownloadPage />,
      },
      {
        path: 'changelog',
        element: <ChangelogPage />,
      },
      {
        path: 'privacy',
        element: <PrivacyPage />,
      },
      {
        path: 'terms',
        element: <TermsPage />,
      },
      ...GROWTH_LANDING_IDS.map((landingId) => ({
        path: growthLandingPages[landingId].slug,
        element: <GrowthLandingPage landingId={landingId} />,
      })),
      {
        path: ':appSlug',
        element: <LocalizedEntryPage />,
      },
    ],
  },
])

import { createBrowserRouter } from 'react-router-dom'
import { LocaleLayout } from './locale-layout'
import { LocalizedEntryPage } from '../pages/localized-entry-page'
import { LocaleIndexPage } from '../pages/locale-index-page'
import { LocaleRedirectPage } from '../pages/locale-redirect-page'
import { ToolsPage } from '../pages/tools-page'
import { CommunityPage } from '../pages/community-page'
import { SettingsPage } from '../pages/settings-page'

export const appRouter = createBrowserRouter([
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
        path: ':appSlug',
        element: <LocalizedEntryPage />,
      },
    ],
  },
])

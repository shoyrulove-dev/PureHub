import { createBrowserRouter } from 'react-router-dom'
import { LocaleLayout } from './locale-layout'
import { LocalizedEntryPage } from '../pages/localized-entry-page'
import { LocaleIndexPage } from '../pages/locale-index-page'
import { LocaleRedirectPage } from '../pages/locale-redirect-page'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <LocaleRedirectPage />,
  },
  {
    path: '/:locale',
    element: <LocaleLayout />,
    children: [
      {
        index: true,
        element: <LocaleIndexPage />,
      },
      {
        path: ':slug',
        element: <LocalizedEntryPage />,
      },
    ],
  },
])

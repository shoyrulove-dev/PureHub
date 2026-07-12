import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppShell } from './shell'
import { TAB_ITEMS } from '../features/catalog/tabs'
import { TabLandingPage } from '../pages/tab-landing-page'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <Navigate to={TAB_ITEMS[0].path} replace />,
      },
      ...TAB_ITEMS.map((tab) => ({
        path: tab.path,
        element: <TabLandingPage tab={tab} />,
      })),
    ],
  },
])

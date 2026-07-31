import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { HelmetProvider } from 'react-helmet-async'
import { registerSW } from 'virtual:pwa-register'
import './i18n/config'
import './index.css'
import App from './App.tsx'

const isBackendRoute = /^\/(?:admin|public-api|api)(?:\/|$)/.test(window.location.pathname)

if (!isBackendRoute) {
  registerSW({
    immediate: true,
  })
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HelmetProvider>
      <App />
    </HelmetProvider>
  </StrictMode>,
)

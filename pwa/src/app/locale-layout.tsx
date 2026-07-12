import { useEffect } from 'react'
import { Navigate, useLocation, useParams } from 'react-router-dom'
import i18n from 'i18next'
import { AppShell } from './shell'
import { normalizeLocale } from '../i18n/locales'

export function LocaleLayout() {
  const location = useLocation()
  const { locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)

  useEffect(() => {
    void i18n.changeLanguage(normalizedLocale)
    document.documentElement.lang = normalizedLocale
  }, [normalizedLocale])

  if (locale !== normalizedLocale) {
    const suffix = location.pathname.replace(/^\/[^/]+/, '') || ''
    return <Navigate to={`/${normalizedLocale}${suffix}`} replace />
  }

  return <AppShell />
}

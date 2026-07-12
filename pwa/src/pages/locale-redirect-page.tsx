import { Navigate } from 'react-router-dom'
import { TAB_ITEMS } from '../features/catalog/tabs'
import { buildTabPath, detectPreferredLocale } from '../i18n/routing'

export function LocaleRedirectPage() {
  const locale = detectPreferredLocale()
  return <Navigate to={buildTabPath(locale, TAB_ITEMS[0].id)} replace />
}

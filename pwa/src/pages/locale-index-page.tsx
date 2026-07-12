import { Navigate, useParams } from 'react-router-dom'
import { normalizeLocale } from '../i18n/locales'
import { buildTabPath } from '../i18n/routing'

export function LocaleIndexPage() {
  const { locale } = useParams()
  const normalizedLocale = normalizeLocale(locale)
  return <Navigate to={buildTabPath(normalizedLocale, 'zen-time')} replace />
}

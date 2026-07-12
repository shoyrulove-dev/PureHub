export const SUPPORTED_LOCALES = ['en', 'vi', 'zh'] as const

export type LocaleCode = (typeof SUPPORTED_LOCALES)[number]

export const DEFAULT_LOCALE: LocaleCode = 'en'

export function isSupportedLocale(value: string | undefined | null): value is LocaleCode {
  return SUPPORTED_LOCALES.includes((value ?? '') as LocaleCode)
}

export function normalizeLocale(value: string | undefined | null): LocaleCode {
  const shortCode = (value ?? DEFAULT_LOCALE).toLowerCase().split('-')[0]
  return isSupportedLocale(shortCode) ? shortCode : DEFAULT_LOCALE
}

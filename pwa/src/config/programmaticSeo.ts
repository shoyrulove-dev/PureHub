import type { SeoLanguage } from './seoMeta.js'

export type ConverterLocaleText = Record<SeoLanguage, string>

export type ProgrammaticConverter = {
  slug: string
  from: ConverterLocaleText
  to: ConverterLocaleText
  factor: number
  offset: number
  values: number[]
  formula: Record<SeoLanguage, string>
  label: Record<SeoLanguage, string>
}

const units = {
  meter: { en: 'meters', vi: 'mét', zh: '米' },
  kilometer: { en: 'kilometers', vi: 'kilômét', zh: '千米' },
  mile: { en: 'miles', vi: 'dặm', zh: '英里' },
  celsius: { en: 'Celsius', vi: 'độ C', zh: '摄氏度' },
  fahrenheit: { en: 'Fahrenheit', vi: 'độ F', zh: '华氏度' },
  kilogram: { en: 'kilograms', vi: 'kilôgam', zh: '千克' },
  pound: { en: 'pounds', vi: 'pound', zh: '磅' },
  megabyte: { en: 'megabytes', vi: 'megabyte', zh: '兆字节' },
  gigabyte: { en: 'gigabytes', vi: 'gigabyte', zh: '吉字节' },
} as const

type UnitId = keyof typeof units

function pair(slug: string, from: UnitId, to: UnitId, factor: number, offset = 0, values = [1, 5, 10, 50, 100, 1000]): ProgrammaticConverter {
  return {
    slug,
    from: { en: units[from].en, vi: units[from].vi, zh: units[from].zh },
    to: { en: units[to].en, vi: units[to].vi, zh: units[to].zh },
    factor,
    offset,
    values,
    formula: {
      en: `${units[to].en} = (${units[from].en} × ${factor})${offset ? ` + ${offset}` : ''}`,
      vi: `${units[to].vi} = (${units[from].vi} × ${factor})${offset ? ` + ${offset}` : ''}`,
      zh: `${units[to].zh} = (${units[from].zh} × ${factor})${offset ? ` + ${offset}` : ''}`,
    },
    label: {
      en: `${units[from].en} to ${units[to].en}`,
      vi: `${units[from].vi} sang ${units[to].vi}`,
      zh: `${units[from].zh}转${units[to].zh}`,
    },
  }
}

export const programmaticConverters: ProgrammaticConverter[] = [
  pair('meter-to-kilometer', 'meter', 'kilometer', 0.001),
  pair('kilometer-to-meter', 'kilometer', 'meter', 1000, 0, [1, 5, 10, 50, 100]),
  pair('mile-to-kilometer', 'mile', 'kilometer', 1.609344),
  pair('celsius-to-fahrenheit', 'celsius', 'fahrenheit', 1.8, 32, [-40, 0, 10, 20, 37, 100]),
  pair('fahrenheit-to-celsius', 'fahrenheit', 'celsius', 0.5555555556, -17.777777778, [-40, 0, 32, 50, 68, 100]),
  pair('kilogram-to-pound', 'kilogram', 'pound', 2.2046226218),
  pair('pound-to-kilogram', 'pound', 'kilogram', 0.45359237),
  pair('megabyte-to-gigabyte', 'megabyte', 'gigabyte', 0.001, 0, [1, 5, 10, 50, 100, 500, 1000]),
  pair('gigabyte-to-megabyte', 'gigabyte', 'megabyte', 1000, 0, [1, 2, 5, 10, 50, 100]),
]

export function getProgrammaticConverter(slug: string) {
  return programmaticConverters.find((item) => item.slug === slug) ?? null
}

export function convertProgrammaticValue(converter: ProgrammaticConverter, value: number) {
  return value * converter.factor + converter.offset
}

export function programmaticConverterTitle(converter: ProgrammaticConverter, lang: SeoLanguage) {
  const from = converter.from[lang]
  const to = converter.to[lang]
  return lang === 'vi'
    ? `Chuyển đổi ${from} sang ${to} chuẩn nhất`
    : lang === 'zh'
      ? `精准将${from}转换为${to}`
      : `Convert ${from} to ${to} accurately`
}

export function programmaticConverterDescription(converter: ProgrammaticConverter, lang: SeoLanguage) {
  const from = converter.from[lang]
  const to = converter.to[lang]
  return lang === 'vi'
    ? `Bảng chuyển đổi ${from} sang ${to} miễn phí, nhanh và không tạo URL theo số lượng. Nhập giá trị trên thiết bị để xem kết quả.`
    : lang === 'zh'
      ? `免费、快速地将${from}转换为${to}。页面只针对单位组合建立，不会为每个数量生成无限网址。`
      : `Free, fast ${from} to ${to} conversion with a fixed unit-pair page. Enter any amount in the local converter without creating infinite URLs.`
}

export function programmaticConverterQuickAnswer(converter: ProgrammaticConverter, lang: SeoLanguage) {
  const from = converter.from[lang]
  const to = converter.to[lang]
  const one = formatProgrammaticValue(convertProgrammaticValue(converter, 1))
  return lang === 'vi'
    ? `Để đổi 1 ${from} sang ${to}, nhân giá trị với ${converter.factor}${converter.offset ? ` rồi cộng ${converter.offset}` : ''}. Hiện tại, 1 ${from} = ${one} ${to}.`
    : lang === 'zh'
      ? `将${from}转换为${to}时，使用公式 ${converter.formula.zh}。目前 1 ${from} = ${one} ${to}。`
      : `To convert 1 ${from} to ${to}, multiply by ${converter.factor}${converter.offset ? ` and add ${converter.offset}` : ''}. Currently, 1 ${from} = ${one} ${to}.`
}

export function formatProgrammaticValue(value: number) {
  return Number(value.toPrecision(10)).toLocaleString('en-US', { maximumFractionDigits: 8 })
}

export function buildProgrammaticConverterPath(lang: SeoLanguage, slug: string) {
  return `/${lang}/converter/${slug}`
}

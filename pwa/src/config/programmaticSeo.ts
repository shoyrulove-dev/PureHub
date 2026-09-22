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
  centimeter: { en: 'centimeters', vi: 'centimét', zh: '厘米' },
  millimeter: { en: 'millimeters', vi: 'milimét', zh: '毫米' },
  mile: { en: 'miles', vi: 'dặm', zh: '英里' },
  yard: { en: 'yards', vi: 'yard', zh: '码' },
  foot: { en: 'feet', vi: 'feet', zh: '英尺' },
  celsius: { en: 'Celsius', vi: 'độ C', zh: '摄氏度' },
  fahrenheit: { en: 'Fahrenheit', vi: 'độ F', zh: '华氏度' },
  kilogram: { en: 'kilograms', vi: 'kilôgam', zh: '千克' },
  gram: { en: 'grams', vi: 'gam', zh: '克' },
  pound: { en: 'pounds', vi: 'pound', zh: '磅' },
  ounce: { en: 'ounces', vi: 'ounce', zh: '盎司' },
  ton: { en: 'metric tons', vi: 'tấn', zh: '吨' },
  stone: { en: 'stones', vi: 'stone', zh: '英石' },
  liter: { en: 'liters', vi: 'lít', zh: '升' },
  milliliter: { en: 'milliliters', vi: 'mililít', zh: '毫升' },
  gallon: { en: 'US gallons', vi: 'galông Mỹ', zh: '美制加仑' },
  quart: { en: 'US quarts', vi: 'quart Mỹ', zh: '美制夸脱' },
  cup: { en: 'US cups', vi: 'cốc Mỹ', zh: '美制杯' },
  squareMeter: { en: 'square meters', vi: 'mét vuông', zh: '平方米' },
  squareKilometer: { en: 'square kilometers', vi: 'kilômét vuông', zh: '平方千米' },
  squareFoot: { en: 'square feet', vi: 'feet vuông', zh: '平方英尺' },
  acre: { en: 'acres', vi: 'mẫu Anh', zh: '英亩' },
  metersPerSecond: { en: 'meters per second', vi: 'mét mỗi giây', zh: '米每秒' },
  kilometersPerHour: { en: 'kilometers per hour', vi: 'kilômét mỗi giờ', zh: '千米每小时' },
  milesPerHour: { en: 'miles per hour', vi: 'dặm mỗi giờ', zh: '英里每小时' },
  knot: { en: 'knots', vi: 'hải lý mỗi giờ', zh: '节' },
  kilobyte: { en: 'kilobytes', vi: 'kilobyte', zh: '千字节' },
  second: { en: 'seconds', vi: 'giây', zh: '秒' },
  minute: { en: 'minutes', vi: 'phút', zh: '分钟' },
  hour: { en: 'hours', vi: 'giờ', zh: '小时' },
  day: { en: 'days', vi: 'ngày', zh: '天' },
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

const coreConverters: ProgrammaticConverter[] = [
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

const matrixUnits: Array<[UnitId, string, number]> = [
  ['meter', 'length', 1], ['kilometer', 'length', 1000], ['centimeter', 'length', 0.01], ['millimeter', 'length', 0.001], ['mile', 'length', 1609.344], ['yard', 'length', 0.9144], ['foot', 'length', 0.3048],
  ['kilogram', 'mass', 1], ['gram', 'mass', 0.001], ['pound', 'mass', 0.45359237], ['ounce', 'mass', 0.028349523125], ['ton', 'mass', 1000], ['stone', 'mass', 6.35029318],
  ['liter', 'volume', 1], ['milliliter', 'volume', 0.001], ['gallon', 'volume', 3.785411784], ['quart', 'volume', 0.946352946], ['cup', 'volume', 0.2365882365],
  ['squareMeter', 'area', 1], ['squareKilometer', 'area', 1000000], ['squareFoot', 'area', 0.09290304], ['acre', 'area', 4046.8564224],
  ['metersPerSecond', 'speed', 1], ['kilometersPerHour', 'speed', 0.2777777778], ['milesPerHour', 'speed', 0.44704], ['knot', 'speed', 0.5144444444],
  ['kilobyte', 'data', 1], ['megabyte', 'data', 1000], ['gigabyte', 'data', 1000000],
  ['second', 'time', 1], ['minute', 'time', 60], ['hour', 'time', 3600], ['day', 'time', 86400],
]

const coreSlugs = new Set(coreConverters.map((converter) => converter.slug))
const generatedConverters = matrixUnits.flatMap(([from, fromGroup, fromFactor]) =>
  matrixUnits
    .filter(([to, toGroup]) => toGroup === fromGroup && to !== from)
    .map(([to, , toFactor]) => pair(`${from}-to-${to}`, from, to, fromFactor / toFactor, 0, [1, 5, 10, 50, 100])),
)

// Keep the first 125 reviewed matrix pairs so the complete inventory is exactly
// 134 pairs × 3 locales + 98 core/site/growth URLs = 500 sitemap URLs.
export const programmaticConverters: ProgrammaticConverter[] = [
  ...coreConverters,
  ...generatedConverters.filter((converter) => !coreSlugs.has(converter.slug)).slice(0, 125),
]

if (programmaticConverters.length !== 134) {
  throw new Error(`SEO converter inventory must contain 134 fixed pairs; found ${programmaticConverters.length}`)
}

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

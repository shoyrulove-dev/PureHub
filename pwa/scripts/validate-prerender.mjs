import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const checks = [
  ['en/qr-studio/index.html', 'QR'],
  ['en/ocr-text/index.html', 'OCR'],
  ['en/doc-to-pdf/index.html', 'PDF'],
  ['vi/qr-studio/index.html', 'QR'],
  ['zh/er-wei-ma-gong-fang/index.html', 'PureHub'],
  ['en/qr-scanner-no-ads/index.html', 'QR Scanner'],
  ['en/private-ocr/index.html', 'Private OCR'],
  ['en/offline-barcode-scanner/index.html', 'Offline Barcode'],
  ['en/phone-bubble-level/index.html', 'Bubble Level'],
  ['en/wifi-analyzer-android/index.html', 'Android Wi-Fi Analyzer'],
  ['en/converter/meter-to-kilometer/index.html', 'Convert meters to kilometers'],
  ['vi/converter/celsius-to-fahrenheit/index.html', 'Chuyển đổi độ C sang độ F'],
  ['zh/converter/kilogram-to-pound/index.html', '精准将千克转换为磅'],
]

const sitemap = readFileSync(resolve('dist', 'sitemap.xml'), 'utf8')
if (sitemap.includes('google11321854edbe5f72')) {
  throw new Error('Search Console verification file must not be included in sitemap.xml')
}

for (const [relativePath, expected] of checks) {
  const html = readFileSync(resolve('dist', relativePath), 'utf8')
  if (!html.includes(`<title>`) || !html.includes(expected) || !html.includes('<main class="seo-static-content"') || !html.includes('<h1>')) throw new Error(`Prerender content validation failed for ${relativePath}`)
  if (!html.includes('rel="canonical"') || !html.includes('data-seo-page')) throw new Error(`SEO metadata missing from ${relativePath}`)
}

const converterHtml = readFileSync(resolve('dist', 'en/converter/meter-to-kilometer/index.html'), 'utf8')
if (!converterHtml.includes('<table>') || !converterHtml.includes('<code>') || converterHtml.includes('100-meter-to-kilometer')) {
  throw new Error('Programmatic converter thin-content or infinite-crawl guard failed')
}

console.log(`Validated ${checks.length} prerendered SEO routes.`)

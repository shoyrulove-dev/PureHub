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
]

for (const [relativePath, expected] of checks) {
  const html = readFileSync(resolve('dist', relativePath), 'utf8')
  if (!html.includes(`<title>`) || !html.includes(expected)) throw new Error(`Prerender validation failed for ${relativePath}`)
  if (!html.includes('rel="canonical"') || !html.includes('data-seo-page')) throw new Error(`SEO metadata missing from ${relativePath}`)
}

console.log(`Validated ${checks.length} prerendered SEO routes.`)

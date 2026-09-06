const fs = require('fs')
const path = require('path')
const { chromium } = require('../../.tools/tiktok-review-recorder/node_modules/playwright')

const root = path.resolve(__dirname)
const fixtureDir = path.join(root, 'raw', 'ocr-fixtures')
const outputDir = path.join(root, 'raw', 'ocr-campaign')

async function record(previewUrl, name, run) {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({
    viewport: { width: 430, height: 932 },
    deviceScaleFactor: 1,
    recordVideo: { dir: outputDir, size: { width: 430, height: 932 } },
    acceptDownloads: true,
  })
  const page = await context.newPage()
  await page.goto(`${previewUrl}/en/ocr-text`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await run(page)
  await page.waitForTimeout(1800)
  const video = page.video()
  await context.close()
  const recordedPath = await video.path()
  fs.renameSync(recordedPath, path.join(outputDir, name))
  await browser.close()
}

async function waitForResult(page) {
  await page.getByLabel('Recognized text').waitFor({ state: 'visible', timeout: 180000 })
  await page.waitForTimeout(1800)
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true })
  const { preview } = await import('../../pwa/node_modules/vite/dist/node/index.js')
  const server = await preview({
    root: path.resolve(root, '../../pwa'),
    preview: { host: '127.0.0.1', port: 4173, strictPort: true },
  })
  const previewUrl = 'http://127.0.0.1:4173'
  try {
    await record(previewUrl, '01-ocr-receipt-vi.webm', async (page) => {
      await page.getByRole('button', { name: 'Receipt' }).click()
      await page.locator('input[type="file"][multiple]').setInputFiles(path.join(fixtureDir, 'purehub-ocr-receipt-vi.png'))
      await waitForResult(page)
      await page.getByRole('button', { name: 'Save', exact: true }).click()
      await page.getByRole('button', { name: 'library', exact: true }).click()
    })
    await record(previewUrl, '02-ocr-batch-en-vi.webm', async (page) => {
      await page.locator('input[type="file"][multiple]').setInputFiles([
        path.join(fixtureDir, 'purehub-ocr-note-en-vi.png'),
        path.join(fixtureDir, 'purehub-ocr-receipt-vi.png'),
      ])
      await waitForResult(page)
      await page.getByRole('button', { name: 'Previous', exact: true }).click()
      await page.waitForTimeout(1200)
      await page.getByRole('button', { name: 'Next', exact: true }).click()
    })
    await record(previewUrl, '03-ocr-chinese.webm', async (page) => {
      await page.getByLabel('Recognition language').selectOption('chi_sim')
      await page.locator('input[type="file"][multiple]').setInputFiles(path.join(fixtureDir, 'purehub-ocr-note-zh.png'))
      await waitForResult(page)
    })
  } finally {
    await server.close()
  }
  console.log(`Captured OCR browser demos in ${outputDir}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

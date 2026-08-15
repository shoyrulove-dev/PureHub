const { chromium } = require('../../.tools/tiktok-review-recorder/node_modules/playwright')
const path = require('path')

const root = path.resolve(__dirname)

async function orientation(page, gamma, beta) {
  await page.evaluate(({ gamma, beta }) => {
    const event = new Event('deviceorientation')
    Object.defineProperties(event, {
      gamma: { value: gamma },
      beta: { value: beta },
      alpha: { value: 0 },
    })
    window.dispatchEvent(event)
  }, { gamma, beta })
}

async function main() {
  const { preview } = await import('../../pwa/node_modules/vite/dist/node/index.js')
  const previewServer = await preview({
    root: path.resolve(root, '../../pwa'),
    preview: { host: '127.0.0.1', port: 4173, strictPort: true },
  })
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({
    viewport: { width: 430, height: 932 },
    deviceScaleFactor: 1,
    recordVideo: { dir: path.join(root, 'raw', 'playwright'), size: { width: 430, height: 932 } },
  })
  const page = await context.newPage()
  await page.goto('http://127.0.0.1:4173/en/bubble-level', { waitUntil: 'networkidle' })
  await page.waitForTimeout(900)
  await page.getByRole('button', { name: 'Enable level' }).click()
  await orientation(page, 8, -5)
  await page.waitForTimeout(1300)
  await orientation(page, 0.3, 0.2)
  await page.waitForTimeout(2300)
  await page.getByRole('button', { name: 'Edge X' }).click()
  await orientation(page, -5, 7)
  await page.waitForTimeout(1000)
  await orientation(page, 0.2, 7)
  await page.waitForTimeout(2300)
  await page.getByText('Accuracy & cues').click()
  await page.waitForTimeout(1100)
  await page.getByRole('button', { name: '±0.2°' }).click()
  await page.waitForTimeout(900)
  const video = page.video()
  await context.close()
  const videoPath = await video.path()
  console.log(videoPath)
  await browser.close()
  await previewServer.close()
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

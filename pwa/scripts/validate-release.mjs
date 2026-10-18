import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8')
const manifest = JSON.parse(read('../public/manifest.webmanifest'))
const viteConfig = read('../vite.config.ts')
const index = read('../index.html')

const failures = []
const requiredIcons = [192, 512]
for (const size of requiredIcons) {
  if (!manifest.icons?.some((icon) => icon.sizes === `${size}x${size}`)) {
    failures.push(`manifest is missing a ${size}x${size} icon`)
  }
}
if (!manifest.icons?.some((icon) => icon.purpose === 'maskable')) failures.push('manifest is missing a maskable icon')
if (manifest.display !== 'standalone') failures.push('manifest display must remain standalone')
if (manifest.scope !== '/' || manifest.start_url !== '/') failures.push('manifest scope/start_url must remain root-safe')
if (manifest.theme_color.toLowerCase() !== '#6d28d9') failures.push('manifest theme must match the violet app shell')
if (!index.includes('rel="manifest"')) failures.push('index.html does not link the web manifest')
for (const contract of ['cleanupOutdatedCaches: true', 'clientsClaim: true', 'skipWaiting: true']) {
  if (!viteConfig.includes(contract)) failures.push(`service-worker contract missing: ${contract}`)
}
for (const protectedRoute of ['admin', 'public-api', 'api']) {
  if (!viteConfig.includes(`^\\/${protectedRoute}`)) failures.push(`navigate fallback must deny /${protectedRoute}`)
}

if (failures.length) throw new Error(`PWA release contract failed:\n- ${failures.join('\n- ')}`)
console.log('Validated install manifest, violet launch theme, icons, update cleanup, and protected-route fallbacks.')

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import Sitemap from 'vite-plugin-sitemap'
import { VitePWA } from 'vite-plugin-pwa'
import { buildSeoSitemapPaths, SITE_ORIGIN } from './src/config/seoMeta.js'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    Sitemap({
      hostname: SITE_ORIGIN,
      dynamicRoutes: buildSeoSitemapPaths(),
      readable: true,
      generateRobotsTxt: true,
    }),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      strategies: 'generateSW',
      includeAssets: ['favicon.svg', 'icons/*.svg'],
      manifest: false,
      devOptions: {
        enabled: true,
      },
      workbox: {
        cleanupOutdatedCaches: true,
        clientsClaim: true,
        skipWaiting: true,
        navigateFallback: 'index.html',
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webmanifest,woff2,json,txt}'],
      },
    }),
  ],
  server: {
    host: true,
  },
  preview: {
    host: true,
  },
})

# PureHub PWA - Phase 1

## Project setup commands

```bash
npm create vite@latest purehub-pwa -- --template react-ts
cd purehub-pwa
npm install
npm install react-router-dom zustand idb lucide-react vite-plugin-pwa tailwindcss @tailwindcss/vite
npm run dev
```

## Notes

- Use Node.js `20.19+` or `22.12+` for current Vite compatibility.
- The app shell in this repo lives under `pwa/` so it can coexist with the Android app.
- Phase 1 only sets up the shell, routing, PWA plumbing, and IndexedDB storage foundations.

import type { LucideIcon } from 'lucide-react'
import { Barcode, FileScan, Gauge, QrCode, Wifi } from 'lucide-react'

export const GROWTH_LANDING_IDS = [
  'qr-scanner-no-ads',
  'private-ocr',
  'offline-barcode-scanner',
  'bubble-level',
  'wifi-analyzer-android',
] as const

export type GrowthLandingId = (typeof GROWTH_LANDING_IDS)[number]

type LandingFaq = { question: string; answer: string }

export type GrowthLandingPage = {
  id: GrowthLandingId
  slug: string
  toolSlug: string
  title: string
  description: string
  keywords: string[]
  eyebrow: string
  headline: string
  lead: string
  primaryCta: string
  videoUrl: string
  icon: LucideIcon
  steps: string[]
  browserNote: string
  faqs: LandingFaq[]
}

export const growthLandingPages: Record<GrowthLandingId, GrowthLandingPage> = {
  'qr-scanner-no-ads': {
    id: 'qr-scanner-no-ads', slug: 'qr-scanner-no-ads', toolSlug: 'qr-studio', icon: QrCode,
    title: 'QR Scanner Without Ads | Scan QR Codes Privately',
    description: 'Scan QR codes locally with no ad wall or mandatory account. Review the result before opening a link and keep your scan history on your device.',
    keywords: ['QR scanner no ads', 'private QR scanner', 'offline QR code scanner', 'scan QR before opening link'],
    eyebrow: 'Private QR workflow', headline: 'Scan the code. Inspect the result. Decide what opens next.',
    lead: 'PureHub QR Studio scans locally, keeps a private library, and lets you review a result before you open or share it.',
    primaryCta: 'Open private QR Scanner', videoUrl: '/media/landing/qr-scanner-no-ads.mp4',
    steps: ['Open QR Studio and choose Camera or Image.', 'Scan the code locally on your device.', 'Review the result before opening, copying, saving, or sharing it.'],
    browserNote: 'Camera access depends on your browser. The signed Android app offers the native camera workflow and barcode support.',
    faqs: [
      { question: 'Does PureHub upload QR codes?', answer: 'No. QR scanning and the private library stay on your device.' },
      { question: 'Can I scan a code from an image?', answer: 'Yes. Choose Image in QR Studio and select a saved picture.' },
    ],
  },
  'private-ocr': {
    id: 'private-ocr', slug: 'private-ocr', toolSlug: 'ocr-text', icon: FileScan,
    title: 'Private OCR: Turn Photos Into Editable Text Locally',
    description: 'Extract editable text from photos and receipts in a privacy-first OCR workflow. Review the result, export it, or move pages into a PDF.',
    keywords: ['private OCR', 'offline OCR', 'image to text private', 'receipt OCR no upload'],
    eyebrow: 'Private document workflow', headline: 'Turn a photo into editable text without making a cloud account.',
    lead: 'OCR Studio keeps the workflow clear: choose a photo, recognize text, edit it, then export only when you are ready.',
    primaryCta: 'Open private OCR', videoUrl: '/media/landing/private-ocr.mp4',
    steps: ['Add a photo, receipt, or document page.', 'Select the language and run text recognition.', 'Review the text, copy it, or continue into a local PDF workflow.'],
    browserNote: 'Browser OCR saves searchable text and can hand current pages to PDF. The signed Android beta adds four-corner perspective review and an image-backed library that can reopen complete pages.',
    faqs: [
      { question: 'Can I edit the extracted text?', answer: 'Yes. OCR is a starting point; review and correct the result before exporting.' },
      { question: 'Can I make a PDF afterwards?', answer: 'Yes. PureHub includes a separate local Doc to PDF workflow for pages you choose.' },
    ],
  },
  'offline-barcode-scanner': {
    id: 'offline-barcode-scanner', slug: 'offline-barcode-scanner', toolSlug: 'qr-studio', icon: Barcode,
    title: 'Offline Barcode Scanner | QR and Product Codes Without Ads',
    description: 'Use a local-first QR and barcode scanner without ads or forced sign-in. Keep the scanned value private and choose what to do with it next.',
    keywords: ['offline barcode scanner', 'barcode scanner no ads', 'product barcode scanner', 'private barcode reader'],
    eyebrow: 'Barcode workflow', headline: 'Read a barcode locally, then choose the next action yourself.',
    lead: 'PureHub QR Studio handles QR and supported barcode formats with a private history and clear result actions.',
    primaryCta: 'Open barcode scanner', videoUrl: '/media/landing/offline-barcode-scanner.mp4',
    steps: ['Use Camera for a live code or Image for a saved photo.', 'Read and classify the result locally.', 'Copy, save, share, or inspect a product search only when you choose.'],
    browserNote: 'Browser barcode support varies by camera and device. Android is the stronger option for regular barcode scanning.',
    faqs: [
      { question: 'Does scanning a barcode search the web automatically?', answer: 'No. PureHub shows the value first and leaves any product lookup as an explicit action.' },
      { question: 'Is this the same tool as the QR scanner?', answer: 'Yes. QR Studio combines QR creation, QR scanning, and supported barcode workflows.' },
    ],
  },
  'bubble-level': {
    id: 'bubble-level', slug: 'phone-bubble-level', toolSlug: 'bubble-level', icon: Gauge,
    title: 'Bubble Level for Your Phone | Calibrate and Confirm a Surface',
    description: 'Use a private phone bubble level with flat and edge modes, calibration guidance, and a stable confirmation instead of a noisy instant reading.',
    keywords: ['phone bubble level', 'calibrate bubble level', 'offline spirit level app', 'level a shelf with phone'],
    eyebrow: 'Phone sensor workflow', headline: 'Check a shelf or surface with a level that waits for a stable reading.',
    lead: 'PureHub Bubble Level makes the reading, tolerance, mode, and calibration guidance visible instead of pretending every phone sensor is perfect.',
    primaryCta: 'Open Bubble Level', videoUrl: '/media/landing/phone-bubble-level.mp4',
    steps: ['Choose flat or edge mode for the surface.', 'Calibrate against a known reference if needed.', 'Hold the phone steady until the level confirms a stable reading.'],
    browserNote: 'Sensor precision differs by phone, case, and surface. Use the Android app for the strongest native sensor workflow.',
    faqs: [
      { question: 'Is a phone level as precise as a professional tool?', answer: 'It is an estimate based on your device sensors. Calibrate first for work where accuracy matters.' },
      { question: 'Why does the app wait before confirming?', answer: 'A stable confirmation reduces false readings caused by hand movement or sensor noise.' },
    ],
  },
  'wifi-analyzer-android': {
    id: 'wifi-analyzer-android', slug: 'wifi-analyzer-android', toolSlug: 'wifi-analyzer', icon: Wifi,
    title: 'Android Wi-Fi Analyzer | Clear Browser Limits, Useful Local Checks',
    description: 'Run a local connection check and understand why web browsers cannot expose full nearby Wi-Fi scans. Use Android for supported native network details.',
    keywords: ['Android Wi-Fi analyzer', 'Wi-Fi scanner browser limits', 'local Wi-Fi connection check', 'private Wi-Fi analyzer'],
    eyebrow: 'Network clarity', headline: 'Know what your browser can measure - and when Android is the right tool.',
    lead: 'PureHub never pretends browser code can see nearby Wi-Fi networks when it cannot. Start with a local connection check, then use Android for supported scans.',
    primaryCta: 'Open Wi-Fi Analyzer', videoUrl: '/media/landing/wifi-analyzer-android.mp4',
    steps: ['Run a local connection check in the browser.', 'See the browser limitation explained in plain language.', 'Use the signed Android app when you need supported native network information.'],
    browserNote: 'Web browsers intentionally restrict access to nearby Wi-Fi network data. This is a platform limitation, not a missing permission prompt.',
    faqs: [
      { question: 'Why cannot the PWA show nearby Wi-Fi networks?', answer: 'Modern browsers do not expose that data to websites for privacy and security reasons.' },
      { question: 'What does the browser check measure?', answer: 'It measures a local round-trip connection check; it does not claim to be a radio scan.' },
    ],
  },
}

export function getGrowthLandingPage(id: string | undefined) {
  return id && id in growthLandingPages ? growthLandingPages[id as GrowthLandingId] : null
}

export const growthLandingRoutes = GROWTH_LANDING_IDS.map((id) => `/en/${growthLandingPages[id].slug}`)

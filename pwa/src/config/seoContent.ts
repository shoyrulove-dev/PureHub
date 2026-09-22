import { getSeoMetaByAppId, type SeoLanguage, type SeoMiniAppId } from './seoMeta.js'

export type SeoFaq = { question: string; answer: string }

export type SeoContent = {
  intro: string
  benefits: string[]
  steps: string[]
  faqs: SeoFaq[]
  note: string
}

type Profile = Partial<SeoContent> & Partial<Record<SeoLanguage, Partial<SeoContent>>>

const profiles: Partial<Record<SeoMiniAppId, Profile>> = {
  'ocr-text': {
    benefits: ['Extract text from photos, receipts, and document pages.', 'Review and correct the result before exporting.', 'Keep the local OCR workflow available offline when the language pack is ready.'],
    steps: ['Add a clear image or document page.', 'Choose the best recognition language and run OCR.', 'Review, copy, export, or continue into a local PDF workflow.'],
    note: 'OCR output is a draft for review. Accuracy changes with image quality, lighting, handwriting, language, and document layout.',
  },
  'qr-studio': {
    benefits: ['Scan supported QR and barcode formats from camera or image.', 'Review a result before opening, copying, or sharing it.', 'Keep scan history on the device instead of requiring an account.'],
    steps: ['Choose Camera or Image.', 'Scan and inspect the decoded value.', 'Save, copy, share, or open only after checking the result.'],
    note: 'Camera and barcode support varies by browser and device. The Android build provides the strongest native scanning workflow.',
  },
  'unit-converter': {
    benefits: ['Convert common measurements without a network request.', 'Use clear units and reversible calculations.', 'Keep the tool fast on mobile, desktop, and installed PWA.'],
    steps: ['Choose the source and target units.', 'Enter a value and review the result.', 'Reverse the direction or copy the conversion when needed.'],
    note: 'Conversions use standard unit definitions. For regulated, scientific, or financial decisions, verify the unit convention required for your context.',
  },
  'doc-to-pdf': {
    benefits: ['Assemble selected images into a local PDF.', 'Keep page order and document content on the device.', 'Continue to OCR when a searchable text layer is needed.'],
    steps: ['Add or capture document pages.', 'Reorder and review the pages.', 'Export the PDF and verify the saved file before sharing.'],
    note: 'PDF output is generated locally. File size and visual quality depend on the source images and selected export settings.',
  },
  'photo-privacy': {
    benefits: ['Create a shareable copy without unwanted photo metadata.', 'Review the output before sharing it elsewhere.', 'Keep the cleanup workflow local-first.'],
    steps: ['Choose a photo or image.', 'Inspect the metadata and privacy options.', 'Export a cleaned copy and share that copy instead of the original.'],
    note: 'Metadata removal improves privacy but cannot control what a destination service adds after upload.',
  },
  'wifi-analyzer': {
    benefits: ['Run a local connection check in the browser.', 'Understand what web permissions can and cannot expose.', 'Use Android when native network information is required.'],
    steps: ['Run the browser connection check.', 'Read the result and the platform limitation.', 'Use the Android workflow for supported native checks.'],
    note: 'A browser cannot perform a full nearby-radio Wi-Fi scan. PureHub reports that limitation instead of presenting a misleading result.',
  },
  'bubble-level': {
    benefits: ['Use flat and edge measurement modes.', 'Calibrate against a known reference.', 'Wait for a stable reading before acting on it.'],
    steps: ['Choose a measurement mode.', 'Calibrate the device on a known reference if needed.', 'Hold the phone steady and confirm the stable reading.'],
    note: 'A phone level is an estimate based on device sensors. Cases, surfaces, calibration, and hardware differences affect accuracy.',
  },
  'decibel-meter': {
    benefits: ['View local microphone estimates without uploading audio.', 'Compare current, peak, and rolling readings.', 'Use the result as an awareness aid rather than a certified measurement.'],
    steps: ['Grant microphone access when prompted.', 'Keep the device position consistent while measuring.', 'Review the estimate and repeat in the same conditions for comparison.'],
    note: 'Phone microphones and browser permissions are not calibrated sound-level meters. Do not use this estimate as a safety or compliance measurement.',
  },
}

const copy: Record<SeoLanguage, { features: string; steps: string; faq: string; note: string; related: string }> = {
  en: { features: 'What this tool helps you do', steps: 'How to use it', faq: 'Questions people ask', note: 'Important note', related: 'Continue with a related PureHub tool' },
  vi: { features: 'Công cụ này giúp bạn làm gì', steps: 'Cách sử dụng', faq: 'Câu hỏi thường gặp', note: 'Lưu ý quan trọng', related: 'Khám phá công cụ PureHub liên quan' },
  zh: { features: '这个工具可以帮助你做什么', steps: '使用方法', faq: '常见问题', note: '重要说明', related: '继续使用相关 PureHub 工具' },
}

const genericFaqs: Record<SeoLanguage, SeoFaq[]> = {
  en: [
    { question: 'Is this PureHub tool free?', answer: 'Yes. PureHub tools are free to use, without an ad wall or a mandatory account.' },
    { question: 'Does it work offline?', answer: 'The workflow is designed to work locally where the browser or installed app provides the required capability. Some optional resources may be downloaded on demand.' },
    { question: 'Does PureHub upload my files?', answer: 'PureHub keeps private tool data on the device unless you explicitly choose an external action or an optional connected workflow.' },
  ],
  vi: [
    { question: 'Công cụ PureHub có miễn phí không?', answer: 'Có. PureHub miễn phí sử dụng, không có màn hình chặn quảng cáo và không bắt buộc tạo tài khoản.' },
    { question: 'Công cụ có hoạt động offline không?', answer: 'Công cụ ưu tiên xử lý cục bộ khi trình duyệt hoặc ứng dụng đã cài hỗ trợ tính năng cần thiết. Một số tài nguyên tùy chọn có thể được tải khi cần.' },
    { question: 'PureHub có tải tệp của tôi lên không?', answer: 'Dữ liệu riêng tư được giữ trên thiết bị trừ khi bạn chủ động chọn thao tác bên ngoài hoặc một quy trình kết nối tùy chọn.' },
  ],
  zh: [
    { question: 'PureHub 工具免费吗？', answer: '免费。PureHub 不强制登录，也没有广告墙。' },
    { question: '工具支持离线使用吗？', answer: '只要浏览器或已安装的应用支持所需能力，流程会优先在本地运行。部分可选资源可能按需下载。' },
    { question: 'PureHub 会上传我的文件吗？', answer: '私人数据默认保留在设备上，除非你主动选择外部操作或可选的联网流程。' },
  ],
}

export function getSeoContent(appId: SeoMiniAppId, lang: SeoLanguage): SeoContent {
  const meta = getSeoMetaByAppId(appId, lang)
  const profile = profiles[appId] ?? {}
  const title = meta?.title ?? 'PureHub tool'
  const description = meta?.description ?? ''
  const keywords = meta?.keywords ?? []
  return {
    intro: description,
    benefits: profile[lang]?.benefits ?? profile.benefits ?? [
      `${title} is built for a focused, private workflow.`,
      ...(keywords.slice(0, 2).map((keyword) => `Useful for: ${keyword}.`)),
      'Use the result, review it, and export or share only when you choose.',
    ],
    steps: profile[lang]?.steps ?? profile.steps ?? [
      'Open the tool and choose the input or mode that matches your task.',
      'Review the result on this device before taking the next action.',
      'Copy, export, save, or share only when the result is ready.',
    ],
    faqs: profile[lang]?.faqs ?? profile.faqs ?? genericFaqs[lang],
    note: profile[lang]?.note ?? profile.note ?? (lang === 'vi'
      ? 'Kết quả là thông tin hỗ trợ và nên được kiểm tra trước khi dùng cho quyết định quan trọng.'
      : lang === 'zh'
        ? '结果用于辅助参考。用于重要决定前，请先检查输入、设备和结果。'
        : 'Results are provided as practical assistance. Review inputs and outputs before using them for an important decision.'),
  }
}

export function getSeoCopy(lang: SeoLanguage) {
  return copy[lang]
}

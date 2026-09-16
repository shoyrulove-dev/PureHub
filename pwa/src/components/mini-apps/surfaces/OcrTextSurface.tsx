import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import jsQR from 'jsqr'
import {
  Camera, Clipboard, Download, FileImage, FileText, History, ImagePlus,
  LoaderCircle, LockKeyhole, Mail, Phone, RotateCw, ScanText, Search, Share2,
  ShieldCheck, Sparkles, Trash2,
} from 'lucide-react'
import { expenseRepository, ocrDocumentRepository, type ExpenseRecord, type OcrDocumentRecord, type OcrStoredPage, type OcrWord } from '../../../lib/db/purehub-db'
import { parseReceiptText } from '../../../lib/receipt-ocr'
import { ActionButton, FormInput, FormTextArea } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { trackProductEvent } from '../../../lib/community-api'

const OCR_LANGUAGES = [
  { code: 'eng+vie', label: 'English + Vietnamese' },
  { code: 'eng+chi_sim', label: 'English + Chinese' },
  { code: 'eng', label: 'English' },
  { code: 'vie', label: 'Tiếng Việt' },
  { code: 'chi_sim', label: '简体中文' },
] as const
const OCR_PACK_CACHE_KEY = 'purehub.ocr.cached-languages.v1'
const DOCUMENT_HANDOFF_KEY = 'purehub.document-suite.ocr-handoff.v1'
const OCR_CLOUD_ENDPOINT_KEY = 'purehub.ocr.cloud-endpoint.v1'

function languageLabel(code: string, fallback: string) {
  if (code === 'vie') return 'Vietnamese'
  if (code === 'chi_sim') return 'Simplified Chinese'
  return fallback
}

type Tab = 'scan' | 'text' | 'library'
type ScanMode = 'Document' | 'Receipt' | 'Note'
type ImageFilter = 'Original' | 'Clean' | 'B&W'
type ImageQuality = { score: number; issues: string[] }
type OcrPage = { id: string; text: string; previewUrl: string; source: string; confidence: number; quality?: ImageQuality; words?: OcrWord[] }

async function blobUrlToDataUrl(url: string) {
  const blob = await (await fetch(url)).blob()
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

async function assessImageQuality(blob: Blob): Promise<ImageQuality> {
  const url = URL.createObjectURL(blob)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image()
      node.onload = () => resolve(node)
      node.onerror = reject
      node.src = url
    })
    const width = Math.min(160, image.naturalWidth)
    const height = Math.max(1, Math.round(image.naturalHeight * width / Math.max(1, image.naturalWidth)))
    const canvas = document.createElement('canvas')
    canvas.width = width; canvas.height = height
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return { score: 0, issues: ['Image analysis unavailable'] }
    context.drawImage(image, 0, 0, width, height)
    const pixels = context.getImageData(0, 0, width, height).data
    let total = 0; let squared = 0
    for (let index = 0; index < pixels.length; index += 4) {
      const luminance = (pixels[index] * 30 + pixels[index + 1] * 59 + pixels[index + 2] * 11) / 100
      total += luminance; squared += luminance * luminance
    }
    const count = Math.max(1, pixels.length / 4)
    const mean = total / count
    const contrast = Math.sqrt(Math.max(0, squared / count - mean * mean))
    const issues: string[] = []
    let score = 100
    if (mean < 48) { score -= 35; issues.push('too dark') }
    if (mean > 224) { score -= 20; issues.push('overexposed') }
    if (contrast < 22) { score -= 30; issues.push('low contrast') }
    if (Math.max(image.naturalWidth, image.naturalHeight) < 900) { score -= 20; issues.push('low resolution') }
    return { score: Math.max(0, score), issues }
  } finally {
    URL.revokeObjectURL(url)
  }
}

async function estimateAutoCrop(blob: Blob) {
  const url = URL.createObjectURL(blob)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image(); node.onload = () => resolve(node); node.onerror = reject; node.src = url
    })
    const size = 96
    const canvas = document.createElement('canvas'); canvas.width = size; canvas.height = size
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return 0
    context.drawImage(image, 0, 0, size, size)
    const pixels = context.getImageData(0, 0, size, size).data
    const luminanceAt = (x: number, y: number) => {
      const offset = (y * size + x) * 4
      return (pixels[offset] * 30 + pixels[offset + 1] * 59 + pixels[offset + 2] * 11) / 100
    }
    const corners = [luminanceAt(2, 2), luminanceAt(size - 3, 2), luminanceAt(2, size - 3), luminanceAt(size - 3, size - 3)]
    if (corners.reduce((sum, value) => sum + value, 0) / corners.length < 220) return 0
    let left = size; let top = size; let right = -1; let bottom = -1
    for (let y = 0; y < size; y += 1) for (let x = 0; x < size; x += 1) {
      if (luminanceAt(x, y) < 238) { left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y) }
    }
    if (right < 0 || right - left < size * .45 || bottom - top < size * .45) return 0
    const margin = Math.min(left, top, size - right - 1, size - bottom - 1) / size
    return Math.min(.12, Math.max(0, margin - .015))
  } finally { URL.revokeObjectURL(url) }
}

async function recognizeWithCloud(endpoint: string, blob: Blob, language: string, mode: ScanMode) {
  const imageData = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
  const response = await fetch(endpoint, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ image_base64: imageData, language, mode }),
  })
  if (!response.ok) throw new Error(`Cloud OCR returned ${response.status}`)
  const payload = await response.json() as { text?: string; confidence?: number; words?: OcrWord[] }
  if (!payload.text?.trim()) throw new Error('Cloud OCR returned no text')
  return { text: payload.text, confidence: Math.round(payload.confidence ?? 0), words: payload.words }
}

function normalizeWords(value: unknown): OcrWord[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (!item || typeof item !== 'object') return []
    const row = item as { text?: unknown; bbox?: { x0?: unknown; y0?: unknown; x1?: unknown; y1?: unknown }; confidence?: unknown }
    const box = row.bbox
    if (typeof row.text !== 'string' || !box || typeof box.x0 !== 'number' || typeof box.y0 !== 'number' || typeof box.x1 !== 'number' || typeof box.y1 !== 'number') return []
    return [{ text: row.text, left: box.x0, top: box.y0, width: Math.max(0, box.x1 - box.x0), height: Math.max(0, box.y1 - box.y0), confidence: Math.round(typeof row.confidence === 'number' ? row.confidence : 0) }]
  })
}

function cleanText(value: string, mode: ScanMode) {
  const lines = value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  if (mode === 'Note') return lines.join(' ').replace(/\s+/g, ' ').trim()
  return lines.join('\n').replace(/[ \t]+/g, ' ').trim()
}

function combinedText(pages: OcrPage[], current: string) {
  if (!pages.length) return current.trim()
  const values = pages.map((page) => page.text)
  return values.map((value, index) => values.length > 1 ? `Page ${index + 1}\n${value}` : value).join('\n\n')
}

function extractTableRows(text: string) {
  return text.split(/\r?\n/).map((line) => line.trim()).filter((line) => {
    const cells = line.includes('\t') ? line.split('\t') : line.split(/\s{2,}/)
    return cells.length >= 2 && cells.every((cell) => cell.trim().length > 0)
  }).map((line) => (line.includes('\t') ? line.split('\t') : line.split(/\s{2,}/)).map((cell) => cell.trim()))
}

function extractLayoutTable(words: OcrWord[]) {
  if (words.length < 4) return []
  const ordered = [...words].filter((word) => word.text.trim()).sort((a, b) => a.top - b.top || a.left - b.left)
  const rows: OcrWord[][] = []
  ordered.forEach((word) => {
    const center = word.top + word.height / 2
    const row = rows.at(-1)
    const rowCenter = row?.length ? row.reduce((sum, item) => sum + item.top + item.height / 2, 0) / row.length : -999
    const tolerance = Math.max(8, word.height * 0.7)
    if (!row || Math.abs(center - rowCenter) > tolerance) rows.push([word])
    else row.push(word)
  })
  return rows.map((row) => {
    const cells: Array<{ text: string[]; last: OcrWord }> = []
    row.sort((a, b) => a.left - b.left).forEach((word) => {
      const previous = cells.at(-1)
      const gap = previous ? word.left - (previous.last.left + previous.last.width) : 999
      if (!previous || gap > Math.max(24, word.height * 1.4)) cells.push({ text: [word.text], last: word })
      else { previous.text.push(word.text); previous.last = word }
    })
    return cells.map((cell) => cell.text.join(' '))
  }).filter((row) => row.length >= 2)
}

function downloadCsv(rows: string[][], fileName: string) {
  const csv = rows.map((row) => row.map((cell) => `"${cell.replaceAll('"', '""')}"`).join(',')).join('\n')
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), fileName)
}

function downloadLayout(pages: OcrPage[], title: string) {
  const payload = { title, exportedAt: new Date().toISOString(), pages: pages.map((page, index) => ({ page: index + 1, text: page.text, confidence: page.confidence, quality: page.quality, words: page.words ?? [] })) }
  downloadBlob(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' }), `${safeName(title)}-layout.json`)
}

function extractFields(text: string) {
  return text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).flatMap((line) => {
    const match = line.match(/^(.{2,48}?)\s*[:：]\s*(.+)$/)
    return match ? [{ label: match[1].trim(), value: match[2].trim() }] : []
  })
}

function extractContact(text: string) {
  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/iu)?.[0] ?? ''
  const phone = text.match(/(?:\+?\d[\d .-]{7,}\d)/u)?.[0]?.trim() ?? ''
  const lines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const name = lines.find((line) => line.length >= 2 && line.length <= 64 && !/[\d@]/.test(line) && !/^(tel|phone|mobile|email|e-mail|address|电话|手机|邮箱)/iu.test(line)) ?? ''
  return { name, email, phone }
}

async function detectQrOrBarcode(blob: Blob) {
  try {
    const bitmap = await createImageBitmap(blob)
    const scale = Math.min(1, 1400 / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(bitmap.width * scale)); canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) { bitmap.close(); return '' }
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height); bitmap.close()
    const detector = (globalThis as typeof globalThis & { BarcodeDetector?: new (options?: { formats?: string[] }) => { detect(source: CanvasImageSource): Promise<Array<{ rawValue?: string }>> } }).BarcodeDetector
    if (detector) {
      const [result] = await new detector({ formats: ['qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'data_matrix', 'pdf417'] }).detect(canvas)
      if (result?.rawValue) return result.rawValue
    }
    return jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })?.data ?? ''
  } catch { return '' }
}

async function prepareImage(file: File, rotation: number, crop: number, filter: ImageFilter) {
  const sourceUrl = URL.createObjectURL(file)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image()
      node.onload = () => resolve(node)
      node.onerror = reject
      node.src = sourceUrl
    })
    const quarterTurn = rotation % 180 !== 0
    const sourceWidth = image.naturalWidth
    const sourceHeight = image.naturalHeight
    const insetX = Math.round(sourceWidth * crop)
    const insetY = Math.round(sourceHeight * crop)
    const croppedWidth = Math.max(1, sourceWidth - insetX * 2)
    const croppedHeight = Math.max(1, sourceHeight - insetY * 2)
    const scale = Math.min(1, 2200 / Math.max(croppedWidth, croppedHeight))
    const targetWidth = Math.max(1, Math.round(croppedWidth * scale))
    const targetHeight = Math.max(1, Math.round(croppedHeight * scale))
    const canvas = document.createElement('canvas')
    canvas.width = quarterTurn ? targetHeight : targetWidth
    canvas.height = quarterTurn ? targetWidth : targetHeight
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) throw new Error('Canvas is unavailable')
    context.translate(canvas.width / 2, canvas.height / 2)
    context.rotate(rotation * Math.PI / 180)
    context.filter = filter === 'B&W' ? 'grayscale(1) contrast(1.3)' : filter === 'Clean' ? 'grayscale(.72) contrast(1.18) brightness(1.05)' : 'none'
    context.drawImage(image, insetX, insetY, croppedWidth, croppedHeight, -targetWidth / 2, -targetHeight / 2, targetWidth, targetHeight)
    const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('Image conversion failed')), 'image/jpeg', .92))
    return { blob, previewUrl: URL.createObjectURL(blob) }
  } finally {
    URL.revokeObjectURL(sourceUrl)
  }
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 500)
}

function safeName(title: string) {
  return title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'purehub-ocr'
}

export default function OcrTextSurface() {
  const [tab, setTab] = useState<Tab>('scan')
  const [mode, setMode] = useState<ScanMode>('Document')
  const [filter, setFilter] = useState<ImageFilter>('Clean')
  const [language, setLanguage] = useState<(typeof OCR_LANGUAGES)[number]['code']>(() => typeof window !== 'undefined' && window.location.pathname.startsWith('/zh') ? 'chi_sim' : 'eng+vie')
  const [rotation, setRotation] = useState(0)
  const [crop, setCrop] = useState(.02)
  const [pages, setPages] = useState<OcrPage[]>([])
  const [selectedPageIndex, setSelectedPageIndex] = useState(-1)
  const [ocrText, setOcrText] = useState('')
  const [title, setTitle] = useState('My scan')
  const [status, setStatus] = useState('Ready. Capture a page or choose an image.')
  const [running, setRunning] = useState(false)
  const [cloudAssist, setCloudAssist] = useState(false)
  const [cloudEndpoint, setCloudEndpoint] = useState(() => localStorage.getItem(OCR_CLOUD_ENDPOINT_KEY) ?? '')
  const [pdfPassword, setPdfPassword] = useState('')
  const [detectedCode, setDetectedCode] = useState('')
  const [packProgress, setPackProgress] = useState(0)
  const [cachedLanguages, setCachedLanguages] = useState<string[]>(() => {
    try { return JSON.parse(localStorage.getItem(OCR_PACK_CACHE_KEY) ?? '[]') as string[] } catch { return [] }
  })
  const [documents, setDocuments] = useState<OcrDocumentRecord[]>([])
  const [query, setQuery] = useState('')
  const cameraInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const pagesRef = useRef<OcrPage[]>([])

  useEffect(() => { void ocrDocumentRepository.list().then(setDocuments) }, [])
  useEffect(() => { pagesRef.current = pages }, [pages])
  useEffect(() => () => pagesRef.current.forEach((page) => URL.revokeObjectURL(page.previewUrl)), [])

  const currentPage = pages[selectedPageIndex] ?? pages.at(-1)
  const currentPreview = currentPage?.previewUrl
  const allText = useMemo(() => combinedText(pages, ocrText), [pages, ocrText])
  const filteredDocuments = useMemo(() => {
    const normalizedQuery = query.trim().normalize('NFKC').toLocaleLowerCase()
    return documents.filter((item) => !normalizedQuery || `${item.title}\n${item.text}`.normalize('NFKC').toLocaleLowerCase().includes(normalizedQuery))
  }, [documents, query])
  const detectedUrl = ocrText.match(/https?:\/\/\S+/i)?.[0]?.replace(/[.,)]$/, '')
  const detectedEmail = ocrText.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0]
  const detectedPhone = ocrText.match(/(?:\+?\d[\d .-]{7,}\d)/)?.[0]
  const detectedContact = useMemo(() => extractContact(ocrText), [ocrText])
  const detectedReceipt = useMemo(() => mode === 'Receipt' && ocrText.trim() ? parseReceiptText(ocrText) : null, [mode, ocrText])
  const detectedTable = useMemo(() => currentPage?.words?.length ? extractLayoutTable(currentPage.words) : extractTableRows(ocrText), [currentPage, ocrText])
  const detectedFields = useMemo(() => extractFields(ocrText), [ocrText])

  const handleFile = async (event: ChangeEvent<HTMLInputElement>, source: string) => {
    const files = Array.from(event.target.files ?? []).slice(0, Math.max(0, 20 - pages.length))
    if (!files.length) return
    if (pages.length >= 20) {
      setStatus('A scan can contain up to 20 pages. Export this document before starting another.')
      event.target.value = ''
      return
    }
    setRunning(true)
    setStatus(`Preparing ${mode.toLowerCase()} locally...`)
    try {
      const selectedLanguage = OCR_LANGUAGES.find((item) => item.code === language)
      setStatus(`Loading the ${languageLabel(language, selectedLanguage?.label ?? language)} OCR pack...`)
      setPackProgress(0)
      const { createWorker, OEM } = await import('tesseract.js')
      const worker = await createWorker(language, OEM.LSTM_ONLY, {
        logger: (message) => {
          if (typeof message.progress === 'number') setPackProgress(Math.round(message.progress * 100))
          if (message.status) setStatus(`${message.status.replaceAll('_', ' ')} ${Math.round((message.progress ?? 0) * 100)}%`)
        },
      })
      try {
        setCachedLanguages((current) => {
          const next = current.includes(language) ? current : [...current, language]
          localStorage.setItem(OCR_PACK_CACHE_KEY, JSON.stringify(next))
          return next
        })
        const nextPages: OcrPage[] = []
        for (let index = 0; index < files.length; index += 1) {
          setStatus(`Recognizing image ${index + 1}/${files.length} on this device...`)
          const autoCrop = await estimateAutoCrop(files[index])
          const prepared = await prepareImage(files[index], rotation, Math.max(crop, autoCrop), filter)
          const code = await detectQrOrBarcode(prepared.blob)
          if (code) setDetectedCode(code)
          let cloudResult: { text: string; confidence: number; words?: OcrWord[] } | null = null
          if (cloudAssist && cloudEndpoint.trim()) {
            try {
              setStatus(`Cloud Assist processing image ${index + 1}/${files.length}...`)
              cloudResult = await recognizeWithCloud(cloudEndpoint.trim(), prepared.blob, language, mode)
            } catch {
              setStatus('Cloud Assist failed; continuing with on-device OCR...')
            }
          }
          const result = cloudResult ? { data: cloudResult } : await worker.recognize(prepared.blob)
          const text = cleanText(result.data.text, mode)
          if (!text) URL.revokeObjectURL(prepared.previewUrl)
          else nextPages.push({ id: crypto.randomUUID(), text, previewUrl: prepared.previewUrl, source: files.length > 1 ? `Batch image ${index + 1}` : source, confidence: Math.round(result.data.confidence ?? 0), quality: await assessImageQuality(prepared.blob), words: 'words' in result.data ? (cloudResult?.words ?? normalizeWords(result.data.words)) : [] })
        }
        if (nextPages.length) {
          setPages((current) => [...current, ...nextPages])
          const nextIndex = pages.length + nextPages.length - 1
          setSelectedPageIndex(nextIndex)
          setOcrText(nextPages.at(-1)?.text ?? '')
          const lowConfidence = nextPages.filter((page) => page.confidence < 70).length
          setStatus(lowConfidence ? `${nextPages.length}/${files.length} page(s) read. ${lowConfidence} page(s) may need better light or a retake.` : `Batch OCR finished: ${nextPages.length}/${files.length} readable page(s). Review before export.`)
          setTab('text')
          markToolSuccess('ocr-text', { headline: 'Text extracted locally', detail: `${nextPages.length} page(s) recognized locally. Review the text before exporting or sharing.`, shareText: 'I turned images into editable text locally with PureHub.' })
        } else {
          setStatus('No readable text found. Try better light or a tighter crop.')
        }
      } finally {
        await worker.terminate()
      }
    } catch {
      setStatus('OCR could not finish. The selected language pack may need its first download.')
    } finally {
      setRunning(false)
      setPackProgress(0)
      event.target.value = ''
    }
  }

  const selectPage = (index: number) => {
    const page = pages[index]
    if (!page) return
    setSelectedPageIndex(index)
    setOcrText(page.text)
  }

  const updateCurrentText = (value: string) => {
    setOcrText(value)
    if (selectedPageIndex >= 0) setPages((current) => current.map((page, index) => index === selectedPageIndex ? { ...page, text: value } : page))
  }

  const deleteCurrentPage = () => {
    const target = pages[selectedPageIndex]
    if (!target) return
    URL.revokeObjectURL(target.previewUrl)
    const next = pages.filter((_, index) => index !== selectedPageIndex)
    setPages(next)
    const nextIndex = Math.min(selectedPageIndex, next.length - 1)
    setSelectedPageIndex(nextIndex)
    setOcrText(next[nextIndex]?.text ?? '')
    if (!next.length) setTab('scan')
  }

  const movePage = (direction: -1 | 1) => {
    if (selectedPageIndex < 0) return
    const target = selectedPageIndex + direction
    if (target < 0 || target >= pages.length) return
    setPages((current) => {
      const next = [...current]
      const [page] = next.splice(selectedPageIndex, 1)
      next.splice(target, 0, page)
      return next
    })
    setSelectedPageIndex(target)
  }

  const saveReceiptToMoneyStudio = async () => {
    if (!detectedReceipt?.total) return
    const record: ExpenseRecord = {
      id: crypto.randomUUID(), title: detectedReceipt.merchant, amount: detectedReceipt.total,
      category: 'Shopping', transactionType: 'expense', wallet: 'Cash',
      note: [detectedReceipt.tax != null ? `Tax: ${detectedReceipt.tax}` : '', 'Imported from OCR Studio'].filter(Boolean).join(' · '),
      createdAt: new Date().toISOString(),
    }
    await expenseRepository.put(record)
    setStatus('Receipt saved to Money Studio. Review its category and wallet when convenient.')
  }

  const saveDocument = async () => {
    if (!allText) return
    const storedPages: OcrStoredPage[] = await Promise.all(pages.map(async (page) => ({
      id: page.id, text: page.text, source: page.source, confidence: page.confidence, quality: page.quality, words: page.words,
      imageDataUrl: await blobUrlToDataUrl(page.previewUrl),
    })))
    const record: OcrDocumentRecord = {
      id: crypto.randomUUID(), title: title.trim() || 'Untitled scan', text: allText,
      source: pages.length > 1 ? `${pages.length} pages` : pages[0]?.source || 'OCR',
      pageCount: Math.max(1, pages.length), createdAt: new Date().toISOString(), pages: storedPages,
    }
    await ocrDocumentRepository.put(record)
    setDocuments((current) => [record, ...current])
    setStatus('Saved to your private OCR library.')
    markToolSuccess('ocr-text', { headline: 'OCR document saved', detail: 'Searchable text was saved only in this browser library.', shareText: 'I saved a private OCR document with PureHub.' })
  }

  const exportPdf = async () => {
    if (!allText) return
    const { jsPDF } = await import('jspdf')
    const pdf = new jsPDF({ unit: 'pt', format: 'a4', encryption: pdfPassword.trim().length >= 8 ? { userPassword: pdfPassword.trim(), ownerPassword: `${pdfPassword.trim()}-owner`, userPermissions: ['print', 'copy'] } : undefined })
    pdf.setProperties({ title: title || 'PureHub OCR', subject: 'Offline OCR document', creator: 'PureHub OCR Studio', author: 'PureHub' })
    const exportPages = pages.length ? pages : [{ id: 'text', text: allText, previewUrl: '', source: 'OCR', confidence: 0 }]
    for (let index = 0; index < exportPages.length; index += 1) {
      if (index) pdf.addPage()
      const page = exportPages[index]
      const pageText = page.text || allText
      const lines = pdf.splitTextToSize(pageText, 510) as string[]
      pdf.setFontSize(10)
      // Keep selectable OCR text in the PDF while placing the original scan above it.
      lines.slice(0, 120).forEach((line, lineIndex) => pdf.text(line, 42, 42 + lineIndex * 12))
      if (page.previewUrl) {
        pdf.addImage(await blobUrlToDataUrl(page.previewUrl), 'JPEG', 20, 20, 555, 802, undefined, 'FAST')
      } else {
        pdf.setFontSize(18); pdf.text(title || 'PureHub OCR', 42, 52)
        pdf.setFontSize(11)
        lines.forEach((line, lineIndex) => pdf.text(line, 42, 82 + lineIndex * 15))
      }
    }
    pdf.save(`${safeName(title)}.pdf`)
    markToolSuccess('ocr-text', { headline: pdfPassword.trim().length >= 8 ? 'Password-protected OCR PDF exported' : 'OCR text exported as PDF', detail: 'Your OCR document was exported locally with a searchable text layer.', shareText: 'I exported an OCR PDF locally with PureHub.' })
  }

  const sendToDocumentSuite = async () => {
    if (!pages.length) return
    const handoffPages = await Promise.all(pages.map(async (page) => {
      const blob = await (await fetch(page.previewUrl)).blob()
      const dataUrl = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.onerror = reject; reader.readAsDataURL(blob) })
      return { dataUrl, text: page.text, confidence: page.confidence }
    }))
    localStorage.setItem(DOCUMENT_HANDOFF_KEY, JSON.stringify({ title, pages: handoffPages, createdAt: new Date().toISOString() }))
    const locale = window.location.pathname.split('/')[1] || 'en'
    window.location.href = `/${locale}/doc-to-pdf`
  }

  const shareText = async () => {
    if (navigator.share) await navigator.share({ title, text: allText })
    else await navigator.clipboard.writeText(allText)
    void trackProductEvent('ocr-text', 'share')
  }

  const resetDocument = () => {
    pages.forEach((page) => URL.revokeObjectURL(page.previewUrl))
    setPages([]); setSelectedPageIndex(-1); setOcrText(''); setTitle('My scan'); setStatus('Ready for a new document.'); setTab('scan')
  }

  return (
    <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <header className="bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-4 sm:p-5 dark:from-emerald-950/50 dark:via-slate-900 dark:to-sky-950/30">
        <div className="flex items-start gap-3">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-950 text-emerald-300 dark:bg-emerald-300 dark:text-slate-950"><ScanText className="size-6" /></span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-black tracking-[.2em] text-emerald-700 dark:text-emerald-300">PRIVATE BY DESIGN</p>
            <h2 className="text-2xl font-black tracking-tight text-slate-950 dark:text-white">OCR Studio</h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Scan, clean and export text without uploading your documents.</p>
          </div>
          <span className="hidden items-center gap-1 rounded-full border border-emerald-200 bg-white/80 px-2.5 py-1 text-xs font-bold text-emerald-800 sm:flex dark:border-emerald-800 dark:bg-slate-900 dark:text-emerald-200"><ShieldCheck className="size-3.5" /> On-device</span>
        </div>
        <nav className="mt-4 grid grid-cols-3 gap-2" aria-label="OCR Studio sections">
          {([['scan', Camera], ['text', FileText], ['library', History]] as const).map(([value, Icon]) => (
            <button key={value} onClick={() => setTab(value)} className={`flex min-h-11 items-center justify-center gap-2 rounded-xl border px-2 text-sm font-bold capitalize transition ${tab === value ? 'border-emerald-300 bg-emerald-700 text-white shadow-sm dark:border-emerald-500 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-200 bg-white/80 text-slate-700 hover:bg-white dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200'}`}><Icon className="size-4" />{value}</button>
          ))}
        </nav>
      </header>

      <div className="p-4 sm:p-5">
        {tab === 'scan' ? (
          <div className="space-y-4">
            <div className="flex gap-2 overflow-x-auto pb-1">{(['Document', 'Receipt', 'Note'] as ScanMode[]).map((value) => <button key={value} onClick={() => setMode(value)} className={`rounded-full border px-3 py-2 text-xs font-bold ${mode === value ? 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-700 dark:bg-emerald-950 dark:text-emerald-200' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'}`}>{value}</button>)}</div>
            <div className="relative overflow-hidden rounded-[22px] bg-slate-950">
              {currentPreview ? <img src={currentPreview} alt="Latest scanned page" className="aspect-[4/5] w-full object-contain" /> : (
                <div className="grid aspect-[4/5] place-items-center bg-[radial-gradient(circle_at_center,_#193448,_#07111e_68%)] text-center text-white">
                  <div><ScanText className="mx-auto size-14 text-emerald-300" /><p className="mt-3 font-bold">Frame one page at a time</p><p className="mt-1 text-sm text-slate-300">Nothing is uploaded by PureHub.</p></div>
                </div>
              )}
              <span className="absolute left-3 top-3 flex items-center gap-1 rounded-full bg-slate-950/80 px-2.5 py-1.5 text-xs font-bold text-emerald-300"><LockKeyhole className="size-3.5" /> Local processing</span>
              {pages.length ? <span className="absolute right-3 top-3 rounded-full bg-white/90 px-2.5 py-1.5 text-xs font-bold text-slate-900">{pages.length} page{pages.length === 1 ? '' : 's'}</span> : null}
              <button disabled={running} onClick={() => cameraInputRef.current?.click()} className="absolute bottom-4 left-1/2 flex min-h-12 -translate-x-1/2 items-center gap-2 rounded-full bg-emerald-500 px-5 font-black text-slate-950 shadow-lg disabled:opacity-60">{running ? <LoaderCircle className="size-5 animate-spin" /> : <Camera className="size-5" />}{running ? 'Reading...' : 'Capture page'}</button>
              <input ref={cameraInputRef} hidden type="file" accept="image/*" capture="environment" onChange={(event) => void handleFile(event, 'Camera')} />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <ActionButton tone="muted" onClick={() => imageInputRef.current?.click()} disabled={running}><ImagePlus className="mr-2 inline size-4" />Scan image</ActionButton>
              <ActionButton tone="muted" onClick={() => setRotation((value) => (value + 90) % 360)} disabled={running}><RotateCw className="mr-2 inline size-4" />Rotate {rotation}°</ActionButton>
              <input ref={imageInputRef} hidden multiple type="file" accept="image/*" onChange={(event) => void handleFile(event, 'Image')} />
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-700 dark:bg-slate-950">
              <div className="flex items-center gap-2"><Sparkles className="size-4 text-emerald-600" /><p className="text-sm font-black text-slate-900 dark:text-white">Document cleanup</p></div>
              <div className="mt-3 flex gap-2 overflow-x-auto">{(['Original', 'Clean', 'B&W'] as ImageFilter[]).map((value) => <button key={value} onClick={() => setFilter(value)} className={`rounded-full border px-3 py-1.5 text-xs font-bold ${filter === value ? 'border-emerald-400 bg-emerald-100 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200' : 'border-slate-300 dark:border-slate-700'}`}>{value}</button>)}</div>
              <label className="mt-3 block text-xs font-bold text-slate-600 dark:text-slate-300">Edge crop {Math.round(crop * 100)}%<input type="range" min="0" max="0.16" step="0.01" value={crop} onChange={(event) => setCrop(Number(event.target.value))} className="mt-2 w-full accent-emerald-600" /></label>
            </div>
            <label className="block text-sm font-bold text-slate-700 dark:text-slate-200">Recognition language<select value={language} disabled={running} onChange={(event) => setLanguage(event.target.value as typeof language)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 dark:border-slate-600 dark:bg-slate-950">{OCR_LANGUAGES.map((item) => <option key={item.code} value={item.code}>{languageLabel(item.code, item.label)}{cachedLanguages.includes(item.code) ? ' · ready offline' : ' · first download'}</option>)}</select><span className="mt-1 block text-xs font-medium text-slate-500">Usually 10–30 seconds per page after the language pack is ready; first use can take longer.</span></label>
            <div className="rounded-2xl border border-sky-200 bg-sky-50 p-3.5 dark:border-sky-900 dark:bg-sky-950/30"><div className="flex items-center justify-between gap-3"><div><p className="text-sm font-black text-sky-950 dark:text-sky-100">Cloud Assist (optional)</p><p className="mt-1 text-xs text-sky-800 dark:text-sky-200">Use your own OCR endpoint for difficult pages. Nothing is uploaded unless you enable this switch.</p></div><button type="button" role="switch" aria-checked={cloudAssist} onClick={() => setCloudAssist((value) => !value)} className={`min-h-10 rounded-full px-3 text-xs font-black ${cloudAssist ? 'bg-sky-700 text-white' : 'bg-white text-slate-700 dark:bg-slate-800 dark:text-slate-200'}`}>{cloudAssist ? 'Enabled' : 'Offline only'}</button></div>{cloudAssist ? <label className="mt-3 block text-xs font-bold text-sky-900 dark:text-sky-100">OCR endpoint<input value={cloudEndpoint} onChange={(event) => { setCloudEndpoint(event.target.value); localStorage.setItem(OCR_CLOUD_ENDPOINT_KEY, event.target.value) }} placeholder="https://your-server.example/ocr" className="mt-1.5 min-h-11 w-full rounded-xl border border-sky-200 bg-white px-3 text-sm font-normal dark:border-sky-800 dark:bg-slate-950" inputMode="url" /><span className="mt-1 block font-medium">The endpoint should accept JSON with image_base64, language, mode and return {`{ text, confidence }`}.</span></label> : null}</div>
            {running && packProgress > 0 ? <div className="rounded-xl bg-emerald-50 p-3 dark:bg-emerald-950/30"><div className="flex justify-between text-xs font-bold text-emerald-800 dark:text-emerald-200"><span>OCR pack and recognition</span><span>{packProgress}%</span></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-emerald-100 dark:bg-emerald-950"><div className="h-full rounded-full bg-emerald-600 transition-all" style={{ width: `${packProgress}%` }} /></div></div> : null}
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div>
        ) : null}

        {tab === 'text' ? (
          ocrText ? <div className="space-y-3">
            {currentPreview ? <img src={currentPreview} alt="Scanned page" className="h-44 w-full rounded-2xl bg-slate-100 object-contain dark:bg-slate-950" /> : null}
            <div className="flex flex-wrap gap-2 text-xs font-bold text-slate-600 dark:text-slate-300"><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{Math.max(1, pages.length)} page{pages.length === 1 ? '' : 's'}</span><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{ocrText.split(/\s+/).filter(Boolean).length} words</span><span className={`rounded-full px-2.5 py-1 ${(currentPage?.confidence ?? 0) >= 75 ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200' : 'bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200'}`}>{currentPage?.confidence ?? 0}% OCR confidence</span>{currentPage?.quality ? <span className={`rounded-full px-2.5 py-1 ${currentPage.quality.score >= 75 ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200' : 'bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200'}`}>{currentPage.quality.score}% image quality</span> : null}</div>
            {currentPage?.quality?.issues.length ? <p className="rounded-xl bg-amber-50 p-3 text-xs font-semibold text-amber-900 dark:bg-amber-950/30 dark:text-amber-100">Retake suggestion: {currentPage.quality.issues.join(', ')}.</p> : null}
            {pages.length > 1 && selectedPageIndex >= 0 ? <div className="space-y-2"><div className="grid grid-cols-[1fr_auto_1fr] items-center gap-2"><ActionButton tone="muted" disabled={selectedPageIndex === 0} onClick={() => selectPage(selectedPageIndex - 1)}>Previous</ActionButton><span className="text-xs font-black">Page {selectedPageIndex + 1}/{pages.length}</span><ActionButton tone="muted" disabled={selectedPageIndex === pages.length - 1} onClick={() => selectPage(selectedPageIndex + 1)}>Next</ActionButton></div><div className="grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={selectedPageIndex === 0} onClick={() => movePage(-1)}>Move earlier</ActionButton><ActionButton tone="muted" disabled={selectedPageIndex === pages.length - 1} onClick={() => movePage(1)}>Move later</ActionButton></div></div> : null}
            <FormInput value={title} onChange={(event) => setTitle(event.target.value)} aria-label="Document title" />
            <FormTextArea className="min-h-64 resize-y" value={ocrText} onChange={(event) => updateCurrentText(event.target.value)} aria-label="Recognized text" />
            {detectedUrl || detectedEmail || detectedPhone ? <div className="flex flex-wrap gap-2 rounded-2xl bg-sky-50 p-3 dark:bg-sky-950/30"><span className="w-full text-xs font-black text-sky-900 dark:text-sky-200">QUICK ACTIONS</span>{detectedUrl ? <a className="rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={detectedUrl} target="_blank" rel="noreferrer">Open link</a> : null}{detectedEmail ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`mailto:${detectedEmail}`}><Mail className="size-3" />Email</a> : null}{detectedPhone ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`tel:${detectedPhone.replace(/[^+\d]/g, '')}`}><Phone className="size-3" />Call</a> : null}</div> : null}
            {detectedContact.email || detectedContact.phone ? <div className="rounded-2xl border border-violet-200 bg-violet-50 p-3.5 text-violet-950 dark:border-violet-900 dark:bg-violet-950/30 dark:text-violet-100"><p className="text-xs font-black tracking-wide">CONTACT CARD DETECTED</p><p className="mt-1 text-sm">{detectedContact.name || 'Contact'}{detectedContact.email ? ` · ${detectedContact.email}` : ''}{detectedContact.phone ? ` · ${detectedContact.phone}` : ''}</p><ActionButton className="mt-3 w-full justify-center" onClick={() => { const vcard = ['BEGIN:VCARD', 'VERSION:3.0', `FN:${detectedContact.name || 'Contact'}`, detectedContact.phone ? `TEL:${detectedContact.phone.replace(/[^+\d]/g, '')}` : '', detectedContact.email ? `EMAIL:${detectedContact.email}` : '', 'END:VCARD'].filter(Boolean).join('\n'); downloadBlob(new Blob([vcard], { type: 'text/vcard;charset=utf-8' }), `${safeName(detectedContact.name || title)}.vcf`) }}>Export contact card</ActionButton></div> : null}
            {detectedCode ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-3.5 text-emerald-950 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-100"><p className="text-xs font-black tracking-wide">QR / BARCODE DETECTED</p><p className="mt-1 break-all text-sm font-bold">{detectedCode}</p><button className="mt-3 min-h-10 rounded-xl bg-white px-3 text-xs font-black text-emerald-800" onClick={() => void navigator.clipboard.writeText(detectedCode)}>Copy code</button></div> : null}
            {detectedReceipt ? <div className="rounded-2xl border border-amber-200 bg-amber-50 p-3.5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-100"><p className="text-xs font-black tracking-wide">RECEIPT DETECTED</p><p className="mt-1 font-bold">{detectedReceipt.merchant}</p><p className="text-lg font-black">{detectedReceipt.total != null ? `Total ${detectedReceipt.total}` : 'Total needs review'}</p><ActionButton className="mt-3 w-full justify-center" disabled={detectedReceipt.total == null} onClick={() => void saveReceiptToMoneyStudio()}>Save to Money Studio</ActionButton></div> : null}
            {detectedTable.length > 0 ? <div className="rounded-2xl border border-indigo-200 bg-indigo-50 p-3.5 text-indigo-950 dark:border-indigo-900 dark:bg-indigo-950/30 dark:text-indigo-100"><p className="text-xs font-black tracking-wide">TABLE DETECTED</p><p className="mt-1 text-sm">{detectedTable.length} row(s), {Math.max(...detectedTable.map((row) => row.length))} column(s). Review before exporting.</p><ActionButton className="mt-3 w-full justify-center" onClick={() => downloadCsv(detectedTable, `${safeName(title)}.csv`)}>Export CSV</ActionButton></div> : null}
            {detectedFields.length > 0 ? <div className="rounded-2xl border border-sky-200 bg-sky-50 p-3.5 text-sky-950 dark:border-sky-900 dark:bg-sky-950/30 dark:text-sky-100"><p className="text-xs font-black tracking-wide">FORM FIELDS DETECTED</p><p className="mt-1 text-sm">{detectedFields.length} structured field(s) found.</p><ActionButton className="mt-3 w-full justify-center" onClick={() => downloadBlob(new Blob([JSON.stringify(detectedFields, null, 2)], { type: 'application/json;charset=utf-8' }), `${safeName(title)}-fields.json`)}>Export JSON</ActionButton></div> : null}
            {pages.some((page) => page.words?.length) ? <ActionButton tone="muted" className="w-full justify-center" onClick={() => downloadLayout(pages, title)}>Export OCR layout JSON</ActionButton> : null}
            <div className="grid grid-cols-2 gap-2"><ActionButton onClick={() => void navigator.clipboard.writeText(ocrText)}><Clipboard className="mr-2 inline size-4" />Copy</ActionButton><ActionButton tone="muted" onClick={() => void shareText()}><Share2 className="mr-2 inline size-4" />Share</ActionButton></div>
            <label className="block text-xs font-bold text-slate-600 dark:text-slate-300">PDF password (optional, 8+ characters)<FormInput type="password" minLength={8} maxLength={128} value={pdfPassword} onChange={(event) => setPdfPassword(event.target.value)} placeholder="Leave empty for a normal PDF" className="mt-1" /></label>
            <div className="grid grid-cols-3 gap-2"><ActionButton tone="muted" onClick={() => downloadBlob(new Blob([allText], { type: 'text/plain;charset=utf-8' }), `${safeName(title)}.txt`)}><Download className="mr-1 inline size-4" />TXT</ActionButton><ActionButton tone="muted" onClick={() => void exportPdf()}><FileText className="mr-1 inline size-4" />PDF</ActionButton><ActionButton tone="muted" onClick={() => void saveDocument()}><History className="mr-1 inline size-4" />Save</ActionButton></div>
            <ActionButton className="w-full justify-center" onClick={() => void sendToDocumentSuite()}><ScanText className="size-4" />Continue in Doc to PDF</ActionButton>
            <div className="grid grid-cols-3 gap-2"><ActionButton tone="muted" onClick={() => setTab('scan')}>Add page</ActionButton><ActionButton tone="danger" disabled={!pages.length} onClick={deleteCurrentPage}>Delete page</ActionButton><ActionButton tone="danger" onClick={resetDocument}>New document</ActionButton></div>
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div> : <div className="py-12 text-center"><FileText className="mx-auto size-12 text-emerald-600" /><h3 className="mt-3 text-lg font-black">No text yet</h3><p className="mt-1 text-sm text-slate-500">Capture a page or choose an image to begin.</p><ActionButton className="mt-4" onClick={() => setTab('scan')}>Start scanning</ActionButton></div>
        ) : null}

        {tab === 'library' ? <div className="space-y-3">
          <div className="flex items-center justify-between"><div><h3 className="text-lg font-black">Private library</h3><p className="text-sm text-slate-500">Searchable and stored only in this browser.</p></div>{documents.length ? <button title="Clear library" onClick={() => void ocrDocumentRepository.clear().then(() => setDocuments([]))} className="grid size-10 place-items-center rounded-xl border border-rose-200 text-rose-600"><Trash2 className="size-4" /></button> : null}</div>
          <label className="relative block"><Search className="pointer-events-none absolute left-3 top-3.5 size-4 text-slate-400" /><FormInput value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search scans" className="pl-10" /></label>
          {filteredDocuments.length ? filteredDocuments.map((item) => <article key={item.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300"><FileImage className="size-5" /></span><button className="min-w-0 flex-1 text-left" onClick={() => { pages.forEach((page) => URL.revokeObjectURL(page.previewUrl)); const restored = (item.pages ?? []).map((page) => ({ ...page, previewUrl: page.imageDataUrl ?? '' })); setPages(restored); setSelectedPageIndex(restored.length ? 0 : -1); setTitle(item.title); setOcrText(restored[0]?.text ?? item.text); setStatus(restored.length ? 'Opened with original scan pages from your private library.' : 'Opened text-only legacy library item.'); setTab('text') }}><strong className="block truncate text-sm">{item.title}</strong><span className="line-clamp-2 text-xs text-slate-500">{item.text.replace(/\s+/g, ' ')}</span><span className="mt-1 block text-[11px] font-bold text-emerald-700">{item.source}{item.pages?.length ? ' · images saved' : ''}</span></button><button title="Delete" onClick={() => void ocrDocumentRepository.remove(item.id).then(() => setDocuments((current) => current.filter((row) => row.id !== item.id)))} className="grid size-9 place-items-center rounded-lg text-rose-600 hover:bg-rose-50"><Trash2 className="size-4" /></button></article>) : <div className="rounded-2xl border border-dashed border-slate-300 py-10 text-center text-sm text-slate-500 dark:border-slate-700">{documents.length ? 'No matching document.' : 'Saved OCR documents will appear here.'}</div>}
        </div> : null}
      </div>
    </section>
  )
}

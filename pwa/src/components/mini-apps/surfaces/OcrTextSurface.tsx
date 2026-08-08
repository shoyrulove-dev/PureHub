import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import {
  Camera, Clipboard, Download, FileImage, FileText, History, ImagePlus,
  LoaderCircle, LockKeyhole, Mail, Phone, RotateCw, ScanText, Search, Share2,
  ShieldCheck, Sparkles, Trash2,
} from 'lucide-react'
import { ocrDocumentRepository, type OcrDocumentRecord } from '../../../lib/db/purehub-db'
import { ActionButton, FormInput, FormTextArea } from '../MiniAppPrimitives'

const OCR_LANGUAGES = [
  { code: 'eng', label: 'English' },
  { code: 'vie', label: 'Tiếng Việt' },
  { code: 'chi_sim', label: '简体中文' },
] as const

type Tab = 'scan' | 'text' | 'library'
type ScanMode = 'Document' | 'Receipt' | 'Note'
type ImageFilter = 'Original' | 'Clean' | 'B&W'
type OcrPage = { id: string; text: string; previewUrl: string; source: string }

function cleanText(value: string, mode: ScanMode) {
  const lines = value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  if (mode === 'Note') return lines.join(' ').replace(/\s+/g, ' ').trim()
  return lines.join('\n').replace(/[ \t]+/g, ' ').trim()
}

function combinedText(pages: OcrPage[], current: string) {
  if (!pages.length) return current.trim()
  const values = pages.map((page) => page.text)
  if (current.trim()) values[values.length - 1] = current.trim()
  return values.map((value, index) => values.length > 1 ? `Page ${index + 1}\n${value}` : value).join('\n\n')
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
  const [language, setLanguage] = useState<(typeof OCR_LANGUAGES)[number]['code']>('eng')
  const [rotation, setRotation] = useState(0)
  const [crop, setCrop] = useState(.02)
  const [pages, setPages] = useState<OcrPage[]>([])
  const [ocrText, setOcrText] = useState('')
  const [title, setTitle] = useState('My scan')
  const [status, setStatus] = useState('Ready. Capture a page or choose an image.')
  const [running, setRunning] = useState(false)
  const [documents, setDocuments] = useState<OcrDocumentRecord[]>([])
  const [query, setQuery] = useState('')
  const cameraInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const pagesRef = useRef<OcrPage[]>([])

  useEffect(() => { void ocrDocumentRepository.list().then(setDocuments) }, [])
  useEffect(() => { pagesRef.current = pages }, [pages])
  useEffect(() => () => pagesRef.current.forEach((page) => URL.revokeObjectURL(page.previewUrl)), [])

  const currentPreview = pages.at(-1)?.previewUrl
  const allText = useMemo(() => combinedText(pages, ocrText), [pages, ocrText])
  const filteredDocuments = useMemo(() => documents.filter((item) => !query || item.title.toLowerCase().includes(query.toLowerCase()) || item.text.toLowerCase().includes(query.toLowerCase())), [documents, query])
  const detectedUrl = ocrText.match(/https?:\/\/\S+/i)?.[0]?.replace(/[.,)]$/, '')
  const detectedEmail = ocrText.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0]
  const detectedPhone = ocrText.match(/(?:\+?\d[\d .-]{7,}\d)/)?.[0]

  const handleFile = async (event: ChangeEvent<HTMLInputElement>, source: string) => {
    const file = event.target.files?.[0]
    if (!file) return
    if (pages.length >= 20) {
      setStatus('A scan can contain up to 20 pages. Export this document before starting another.')
      event.target.value = ''
      return
    }
    setRunning(true)
    setStatus(`Preparing ${mode.toLowerCase()} locally...`)
    try {
      const prepared = await prepareImage(file, rotation, crop, filter)
      setStatus(`Loading the ${OCR_LANGUAGES.find((item) => item.code === language)?.label} OCR pack...`)
      const { createWorker } = await import('tesseract.js')
      const worker = await createWorker(language)
      try {
        setStatus('Recognizing text on this device...')
        const result = await worker.recognize(prepared.blob)
        const text = cleanText(result.data.text, mode)
        if (!text) {
          URL.revokeObjectURL(prepared.previewUrl)
          setStatus('No readable text found. Try better light or a tighter crop.')
        } else {
          const nextPage = { id: crypto.randomUUID(), text, previewUrl: prepared.previewUrl, source }
          setPages((current) => [...current, nextPage])
          setOcrText(text)
          setStatus('Text extracted privately. Review it before export.')
          setTab('text')
        }
      } finally {
        await worker.terminate()
      }
    } catch {
      setStatus('OCR could not finish. The selected language pack may need its first download.')
    } finally {
      setRunning(false)
      event.target.value = ''
    }
  }

  const saveDocument = async () => {
    if (!allText) return
    const record: OcrDocumentRecord = {
      id: crypto.randomUUID(), title: title.trim() || 'Untitled scan', text: allText,
      source: pages.length > 1 ? `${pages.length} pages` : pages[0]?.source || 'OCR',
      pageCount: Math.max(1, pages.length), createdAt: new Date().toISOString(),
    }
    await ocrDocumentRepository.put(record)
    setDocuments((current) => [record, ...current])
    setStatus('Saved to your private OCR library.')
  }

  const exportPdf = async () => {
    if (!allText) return
    const { jsPDF } = await import('jspdf')
    const pdf = new jsPDF({ unit: 'pt', format: 'a4' })
    pdf.setFontSize(18)
    pdf.text(title || 'PureHub OCR', 42, 52)
    pdf.setFontSize(11)
    const lines = pdf.splitTextToSize(allText, 510) as string[]
    let y = 82
    lines.forEach((line) => {
      if (y > 800) { pdf.addPage(); y = 48 }
      pdf.text(line, 42, y); y += 15
    })
    pdf.save(`${safeName(title)}.pdf`)
  }

  const shareText = async () => {
    if (navigator.share) await navigator.share({ title, text: allText })
    else await navigator.clipboard.writeText(allText)
  }

  const resetDocument = () => {
    pages.forEach((page) => URL.revokeObjectURL(page.previewUrl))
    setPages([]); setOcrText(''); setTitle('My scan'); setStatus('Ready for a new document.'); setTab('scan')
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
              <input ref={imageInputRef} hidden type="file" accept="image/*" onChange={(event) => void handleFile(event, 'Image')} />
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-700 dark:bg-slate-950">
              <div className="flex items-center gap-2"><Sparkles className="size-4 text-emerald-600" /><p className="text-sm font-black text-slate-900 dark:text-white">Document cleanup</p></div>
              <div className="mt-3 flex gap-2 overflow-x-auto">{(['Original', 'Clean', 'B&W'] as ImageFilter[]).map((value) => <button key={value} onClick={() => setFilter(value)} className={`rounded-full border px-3 py-1.5 text-xs font-bold ${filter === value ? 'border-emerald-400 bg-emerald-100 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200' : 'border-slate-300 dark:border-slate-700'}`}>{value}</button>)}</div>
              <label className="mt-3 block text-xs font-bold text-slate-600 dark:text-slate-300">Edge crop {Math.round(crop * 100)}%<input type="range" min="0" max="0.16" step="0.01" value={crop} onChange={(event) => setCrop(Number(event.target.value))} className="mt-2 w-full accent-emerald-600" /></label>
            </div>
            <label className="block text-sm font-bold text-slate-700 dark:text-slate-200">Recognition language<select value={language} disabled={running} onChange={(event) => setLanguage(event.target.value as typeof language)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 dark:border-slate-600 dark:bg-slate-950">{OCR_LANGUAGES.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}</select></label>
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div>
        ) : null}

        {tab === 'text' ? (
          ocrText ? <div className="space-y-3">
            {currentPreview ? <img src={currentPreview} alt="Scanned page" className="h-44 w-full rounded-2xl bg-slate-100 object-contain dark:bg-slate-950" /> : null}
            <div className="flex flex-wrap gap-2 text-xs font-bold text-slate-600 dark:text-slate-300"><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{Math.max(1, pages.length)} page{pages.length === 1 ? '' : 's'}</span><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{ocrText.split(/\s+/).filter(Boolean).length} words</span><span className="rounded-full bg-emerald-50 px-2.5 py-1 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">On-device</span></div>
            <FormInput value={title} onChange={(event) => setTitle(event.target.value)} aria-label="Document title" />
            <FormTextArea className="min-h-64 resize-y" value={ocrText} onChange={(event) => setOcrText(event.target.value)} aria-label="Recognized text" />
            {detectedUrl || detectedEmail || detectedPhone ? <div className="flex flex-wrap gap-2 rounded-2xl bg-sky-50 p-3 dark:bg-sky-950/30"><span className="w-full text-xs font-black text-sky-900 dark:text-sky-200">QUICK ACTIONS</span>{detectedUrl ? <a className="rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={detectedUrl} target="_blank" rel="noreferrer">Open link</a> : null}{detectedEmail ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`mailto:${detectedEmail}`}><Mail className="size-3" />Email</a> : null}{detectedPhone ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`tel:${detectedPhone.replace(/[^+\d]/g, '')}`}><Phone className="size-3" />Call</a> : null}</div> : null}
            <div className="grid grid-cols-2 gap-2"><ActionButton onClick={() => void navigator.clipboard.writeText(ocrText)}><Clipboard className="mr-2 inline size-4" />Copy</ActionButton><ActionButton tone="muted" onClick={() => void shareText()}><Share2 className="mr-2 inline size-4" />Share</ActionButton></div>
            <div className="grid grid-cols-3 gap-2"><ActionButton tone="muted" onClick={() => downloadBlob(new Blob([allText], { type: 'text/plain;charset=utf-8' }), `${safeName(title)}.txt`)}><Download className="mr-1 inline size-4" />TXT</ActionButton><ActionButton tone="muted" onClick={() => void exportPdf()}><FileText className="mr-1 inline size-4" />PDF</ActionButton><ActionButton tone="muted" onClick={() => void saveDocument()}><History className="mr-1 inline size-4" />Save</ActionButton></div>
            <div className="grid grid-cols-2 gap-2"><ActionButton tone="muted" onClick={() => setTab('scan')}>Add page</ActionButton><ActionButton tone="danger" onClick={resetDocument}>New document</ActionButton></div>
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div> : <div className="py-12 text-center"><FileText className="mx-auto size-12 text-emerald-600" /><h3 className="mt-3 text-lg font-black">No text yet</h3><p className="mt-1 text-sm text-slate-500">Capture a page or choose an image to begin.</p><ActionButton className="mt-4" onClick={() => setTab('scan')}>Start scanning</ActionButton></div>
        ) : null}

        {tab === 'library' ? <div className="space-y-3">
          <div className="flex items-center justify-between"><div><h3 className="text-lg font-black">Private library</h3><p className="text-sm text-slate-500">Searchable and stored only in this browser.</p></div>{documents.length ? <button title="Clear library" onClick={() => void ocrDocumentRepository.clear().then(() => setDocuments([]))} className="grid size-10 place-items-center rounded-xl border border-rose-200 text-rose-600"><Trash2 className="size-4" /></button> : null}</div>
          <label className="relative block"><Search className="pointer-events-none absolute left-3 top-3.5 size-4 text-slate-400" /><FormInput value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search scans" className="pl-10" /></label>
          {filteredDocuments.length ? filteredDocuments.map((item) => <article key={item.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300"><FileImage className="size-5" /></span><button className="min-w-0 flex-1 text-left" onClick={() => { pages.forEach((page) => URL.revokeObjectURL(page.previewUrl)); setPages([]); setTitle(item.title); setOcrText(item.text); setStatus('Opened from your private OCR library.'); setTab('text') }}><strong className="block truncate text-sm">{item.title}</strong><span className="line-clamp-2 text-xs text-slate-500">{item.text.replace(/\s+/g, ' ')}</span><span className="mt-1 block text-[11px] font-bold text-emerald-700">{item.source}</span></button><button title="Delete" onClick={() => void ocrDocumentRepository.remove(item.id).then(() => setDocuments((current) => current.filter((row) => row.id !== item.id)))} className="grid size-9 place-items-center rounded-lg text-rose-600 hover:bg-rose-50"><Trash2 className="size-4" /></button></article>) : <div className="rounded-2xl border border-dashed border-slate-300 py-10 text-center text-sm text-slate-500 dark:border-slate-700">{documents.length ? 'No matching document.' : 'Saved OCR documents will appear here.'}</div>}
        </div> : null}
      </div>
    </section>
  )
}

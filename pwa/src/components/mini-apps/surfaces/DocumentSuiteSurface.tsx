import { useEffect, useRef, useState } from 'react'
import { ArrowDown, ArrowUp, Camera, Download, FileImage, FileText, ImagePlus, ScanText, ShieldCheck, Trash2 } from 'lucide-react'
import { ActionButton, FormInput } from '../MiniAppPrimitives'
import { trackProductEvent } from '../../../lib/community-api'

type Page = { id: string; file: File; url: string; rotation: number; fit: 'contain' | 'cover'; crop: number; recognizedText?: string }
const DOCUMENT_HANDOFF_KEY = 'purehub.document-suite.ocr-handoff.v1'

function safeName(value: string) { return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'purehub-document' }

export default function DocumentSuiteSurface() {
  const [pages, setPages] = useState<Page[]>([])
  const [title, setTitle] = useState('My document')
  const [quality, setQuality] = useState(.86)
  const [status, setStatus] = useState('Add images to build a private PDF.')
  const [busy, setBusy] = useState(false)
  const [draggingId, setDraggingId] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const cameraRef = useRef<HTMLInputElement>(null)
  const pagesRef = useRef<Page[]>([])
  useEffect(() => { pagesRef.current = pages }, [pages])
  useEffect(() => () => pagesRef.current.forEach((page) => URL.revokeObjectURL(page.url)), [])
  useEffect(() => {
    const raw = localStorage.getItem(DOCUMENT_HANDOFF_KEY)
    if (!raw) return
    localStorage.removeItem(DOCUMENT_HANDOFF_KEY)
    void (async () => {
      try {
        const handoff = JSON.parse(raw) as { title?: string; pages?: Array<{ dataUrl: string; text: string }> }
        const imported = await Promise.all((handoff.pages ?? []).slice(0, 20).map(async (item, index) => {
          const blob = await (await fetch(item.dataUrl)).blob()
          const file = new File([blob], `ocr-page-${index + 1}.jpg`, { type: blob.type || 'image/jpeg' })
          return { id: crypto.randomUUID(), file, url: URL.createObjectURL(file), rotation: 0, fit: 'contain' as const, crop: 0, recognizedText: item.text }
        }))
        if (imported.length) { setPages(imported); setTitle(handoff.title || 'Searchable document'); setStatus(`${imported.length} OCR page(s) imported. Searchable text will be embedded in the PDF.`) }
      } catch { setStatus('The OCR handoff could not be restored. Add images manually.') }
    })()
  }, [])

  const addFiles = (files: FileList | null) => {
    const accepted = Array.from(files ?? []).filter((file) => file.type.startsWith('image/')).slice(0, 20 - pages.length)
    if (!accepted.length) return
    setPages((current) => [...current, ...accepted.map((file) => ({ id: crypto.randomUUID(), file, url: URL.createObjectURL(file), rotation: 0, fit: 'contain' as const, crop: 0 }))])
    setStatus(`${pages.length + accepted.length} page(s) staged locally.`)
  }
  const update = (id: string, values: Partial<Page>) => setPages((current) => current.map((page) => page.id === id ? { ...page, ...values } : page))
  const move = (index: number, direction: -1 | 1) => setPages((current) => { const next = [...current]; const target = index + direction; if (target < 0 || target >= next.length) return current; [next[index], next[target]] = [next[target], next[index]]; return next })
  const remove = (id: string) => setPages((current) => { const target = current.find((page) => page.id === id); if (target) URL.revokeObjectURL(target.url); return current.filter((page) => page.id !== id) })
  const dropBefore = (targetId: string) => setPages((current) => {
    const from = current.findIndex((page) => page.id === draggingId); const to = current.findIndex((page) => page.id === targetId)
    if (from < 0 || to < 0 || from === to) return current
    const next = [...current]; const [page] = next.splice(from, 1); next.splice(to, 0, page); return next
  })
  const estimatedBytes = Math.round(pages.reduce((sum, page) => sum + page.file.size, 0) * quality * .72)

  const exportPdf = async () => {
    if (!pages.length) { setStatus('Add at least one image first.'); return }
    setBusy(true); setStatus('Building PDF on this device...')
    try {
      const { jsPDF } = await import('jspdf')
      const pdf = new jsPDF({ unit: 'pt', format: 'a4', orientation: 'portrait', compress: true })
      for (let index = 0; index < pages.length; index += 1) {
        const page = pages[index]
        const image = await new Promise<HTMLImageElement>((resolve, reject) => { const node = new Image(); node.onload = () => resolve(node); node.onerror = reject; node.src = page.url })
        const rotated = page.rotation % 180 !== 0
        const canvas = document.createElement('canvas')
        canvas.width = rotated ? image.naturalHeight : image.naturalWidth; canvas.height = rotated ? image.naturalWidth : image.naturalHeight
        const context = canvas.getContext('2d'); if (!context) throw new Error('Canvas unavailable')
        const cropX = image.naturalWidth * page.crop; const cropY = image.naturalHeight * page.crop
        const sourceWidth = image.naturalWidth - cropX * 2; const sourceHeight = image.naturalHeight - cropY * 2
        context.translate(canvas.width / 2, canvas.height / 2); context.rotate(page.rotation * Math.PI / 180); context.drawImage(image, cropX, cropY, sourceWidth, sourceHeight, -image.naturalWidth / 2, -image.naturalHeight / 2, image.naturalWidth, image.naturalHeight)
        const data = canvas.toDataURL('image/jpeg', quality)
        if (index) pdf.addPage()
        const width = pdf.internal.pageSize.getWidth() - 48; const height = pdf.internal.pageSize.getHeight() - 48
        const ratio = page.fit === 'cover' ? Math.max(width / canvas.width, height / canvas.height) : Math.min(width / canvas.width, height / canvas.height)
        const drawWidth = canvas.width * ratio; const drawHeight = canvas.height * ratio
        pdf.addImage(data, 'JPEG', (pdf.internal.pageSize.getWidth() - drawWidth) / 2, (pdf.internal.pageSize.getHeight() - drawHeight) / 2, drawWidth, drawHeight)
        if (page.recognizedText) {
          pdf.setFontSize(1); pdf.setTextColor(255, 255, 255)
          const searchableLines = pdf.splitTextToSize(page.recognizedText, pdf.internal.pageSize.getWidth() - 48) as string[]
          pdf.text(searchableLines.slice(0, 500), 24, 24)
          pdf.setTextColor(0, 0, 0)
        }
      }
      pdf.setProperties({ title, creator: 'PureHub Document Suite' }); pdf.save(`${safeName(title)}.pdf`); setStatus('PDF exported. No document was uploaded.'); void trackProductEvent('doc-to-pdf', 'complete')
    } catch { setStatus('The PDF could not be created. Try fewer or smaller images.') } finally { setBusy(false) }
  }

  return <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
    <header className="bg-gradient-to-br from-violet-50 via-white to-sky-50 p-5 dark:from-violet-950/40 dark:via-slate-900 dark:to-sky-950/30">
      <div className="flex items-start gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-violet-700 text-white dark:bg-violet-300 dark:text-slate-950"><FileText className="size-6" /></span><div className="min-w-0 flex-1"><p className="text-[11px] font-black tracking-[.2em] text-violet-700 dark:text-violet-300">PUREHUB DOCUMENT SUITE</p><h2 className="text-2xl font-black text-slate-950 dark:text-white">Doc to PDF</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Capture, arrange and export clean PDFs without a cloud account.</p></div><ShieldCheck className="hidden size-5 text-emerald-600 sm:block" /></div>
      <div className="mt-4 grid grid-cols-2 gap-2"><div className="rounded-xl bg-white/80 p-3 dark:bg-slate-800"><strong className="block text-xl">{pages.length}</strong><span className="text-xs text-slate-500">Pages staged</span></div><a href="/en/ocr-text" className="flex items-center justify-center gap-2 rounded-xl border border-violet-200 bg-white/80 p-3 text-sm font-black text-violet-800 dark:border-violet-800 dark:bg-slate-800 dark:text-violet-200"><ScanText className="size-4" />Open OCR Studio</a></div>
    </header>
    <div className="space-y-4 p-4 sm:p-5">
      <FormInput value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Document title" aria-label="Document title" />
      <div className="grid grid-cols-2 gap-2"><ActionButton onClick={() => cameraRef.current?.click()}><Camera className="mr-2 inline size-4" />Capture</ActionButton><ActionButton tone="muted" onClick={() => inputRef.current?.click()}><ImagePlus className="mr-2 inline size-4" />Add images</ActionButton><input ref={cameraRef} hidden type="file" accept="image/*" capture="environment" onChange={(event) => { addFiles(event.target.files); event.target.value = '' }} /><input ref={inputRef} hidden multiple type="file" accept="image/*" onChange={(event) => { addFiles(event.target.files); event.target.value = '' }} /></div>
      {pages.length ? <div className="space-y-2">{pages.map((page, index) => <article key={page.id} draggable onDragStart={() => setDraggingId(page.id)} onDragOver={(event) => event.preventDefault()} onDrop={() => { dropBefore(page.id); setDraggingId('') }} className={`rounded-2xl border p-2.5 transition dark:border-slate-700 ${draggingId === page.id ? 'border-violet-400 opacity-60' : 'border-slate-200'}`}><div className="flex items-center gap-3"><img src={page.url} alt={`Page ${index + 1}`} className="size-16 rounded-xl bg-slate-100 object-cover dark:bg-slate-950" /><div className="min-w-0 flex-1"><strong className="block truncate text-sm">Page {index + 1} · {page.file.name}</strong><div className="mt-2 flex flex-wrap gap-1"><button title="Move up" onClick={() => move(index, -1)} className="grid size-8 place-items-center rounded-lg bg-slate-100 dark:bg-slate-800"><ArrowUp className="size-3.5" /></button><button title="Move down" onClick={() => move(index, 1)} className="grid size-8 place-items-center rounded-lg bg-slate-100 dark:bg-slate-800"><ArrowDown className="size-3.5" /></button><button onClick={() => update(page.id, { rotation: (page.rotation + 90) % 360 })} className="rounded-lg bg-slate-100 px-2 text-xs font-bold dark:bg-slate-800">Rotate {page.rotation}°</button><button onClick={() => update(page.id, { fit: page.fit === 'contain' ? 'cover' : 'contain' })} className="rounded-lg bg-slate-100 px-2 text-xs font-bold dark:bg-slate-800">{page.fit}</button></div></div><button title="Remove page" onClick={() => remove(page.id)} className="grid size-9 place-items-center rounded-xl text-rose-600 hover:bg-rose-50"><Trash2 className="size-4" /></button></div><label className="mt-2 block text-[11px] font-bold text-slate-500">Edge crop {Math.round(page.crop * 100)}%<input className="mt-1 w-full accent-violet-600" type="range" min="0" max="0.12" step="0.01" value={page.crop} onChange={(event) => update(page.id, { crop: Number(event.target.value) })} /></label></article>)}</div> : <div className="grid min-h-56 place-items-center rounded-[22px] border border-dashed border-slate-300 bg-slate-50 text-center dark:border-slate-700 dark:bg-slate-950"><div><FileImage className="mx-auto size-12 text-violet-500" /><strong className="mt-3 block">Your document starts here</strong><p className="mt-1 text-sm text-slate-500">Up to 20 local image pages.</p></div></div>}
      <label className="block text-sm font-black">Image quality <span className="float-right text-violet-700 dark:text-violet-300">{Math.round(quality * 100)}%</span><input className="mt-2 w-full accent-violet-600" type="range" min=".55" max=".95" step=".05" value={quality} onChange={(event) => setQuality(Number(event.target.value))} /></label>
      {pages.length ? <p className="rounded-xl bg-violet-50 p-3 text-xs font-bold text-violet-800 dark:bg-violet-950/35 dark:text-violet-200">Drag pages to reorder · estimated PDF size {estimatedBytes < 1_000_000 ? `${Math.max(1, Math.round(estimatedBytes / 1000))} KB` : `${(estimatedBytes / 1_000_000).toFixed(1)} MB`}</p> : null}
      <ActionButton className="flex min-h-14 w-full items-center justify-center" disabled={!pages.length || busy} onClick={() => void exportPdf()}><Download className="mr-2 inline size-5" />{busy ? 'Building PDF...' : `Export ${pages.length || ''} page PDF`}</ActionButton>
      <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
      <p className="rounded-xl bg-emerald-50 p-3 text-xs leading-5 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">Document Suite workflow: use OCR Studio when you need searchable text, then Doc to PDF for image-first page layout and export.</p>
    </div>
  </section>
}

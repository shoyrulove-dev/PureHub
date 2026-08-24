import { useState } from 'react'
import { Download, ImagePlus, MapPinOff, Share2, ShieldCheck } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'
import { shareCard } from '../../../lib/share-card'
import { markToolSuccess } from '../../../lib/tool-success'
import { trackProductEvent } from '../../../lib/community-api'

function size(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), 3)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

export default function PhotoPrivacySurface() {
  const [files, setFiles] = useState<File[]>([])
  const [preview, setPreview] = useState('')
  const [results, setResults] = useState<Array<{ url: string; size: number; name: string; source: string; originalSize: number }>>([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('Choose a photo. The original will never be changed.')

  const select = (next: File[]) => {
    if (preview) URL.revokeObjectURL(preview)
    results.forEach((result) => URL.revokeObjectURL(result.url))
    const selected = next.slice(0, 20)
    setFiles(selected)
    setPreview(selected[0] ? URL.createObjectURL(selected[0]) : '')
    setResults([])
    setMessage(selected.length ? `${selected.length} photo${selected.length === 1 ? '' : 's'} selected · ${size(selected.reduce((sum, file) => sum + file.size, 0))}` : 'Choose photos. Originals will never be changed.')
  }

  const clean = async () => {
    if (!files.length) return
    setBusy(true)
    try {
      results.forEach((result) => URL.revokeObjectURL(result.url))
      const cleaned = []
      for (const file of files) {
        const bitmap = await createImageBitmap(file)
        const scale = Math.min(1, 4096 / Math.max(bitmap.width, bitmap.height))
        const canvas = document.createElement('canvas')
        canvas.width = Math.max(1, Math.round(bitmap.width * scale))
        canvas.height = Math.max(1, Math.round(bitmap.height * scale))
        canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
        const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', .92))
        bitmap.close()
        if (!blob) throw new Error('Export failed')
        cleaned.push({ url: URL.createObjectURL(blob), size: blob.size, name: `${file.name.replace(/\.[^.]+$/, '')}-privacy-clean.jpg`, source: file.name, originalSize: file.size })
      }
      setResults(cleaned)
      setMessage(`${cleaned.length} privacy-clean ${cleaned.length === 1 ? 'copy' : 'copies'} ready · ${size(cleaned.reduce((sum, item) => sum + item.size, 0))}. Original EXIF fields were not copied.`)
      markToolSuccess('photo-privacy', {
        headline: cleaned.length === 1 ? 'Private copy ready' : `${cleaned.length} private copies ready`,
        detail: 'New JPEG copies were created locally without copying GPS, camera, author, or other original EXIF metadata.',
        shareText: `I created ${cleaned.length} separate, metadata-free photo ${cleaned.length === 1 ? 'copy' : 'copies'} locally before sharing.`,
      })
    } catch {
      setMessage('This browser could not decode the selected image. Try JPEG, PNG, or WebP.')
    } finally {
      setBusy(false)
    }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Private media utility" title="Photo Privacy" description="Create a fresh share-ready JPEG without copying GPS, camera, author, or other original EXIF metadata." accent="violet" />
    <div className="grid gap-4 lg:grid-cols-[1.1fr_.9fr]">
      <Panel title="Private photo workspace" subtitle={message}>
        <label className="relative grid min-h-72 cursor-pointer place-items-center overflow-hidden rounded-[24px] border border-dashed border-violet-300 bg-violet-50/50 text-center dark:border-violet-800 dark:bg-violet-950/20">
          {preview ? <img src={preview} alt="Selected photo preview" className="absolute inset-0 size-full object-contain" /> : <div className="p-8"><ImagePlus className="mx-auto size-10 text-violet-600" /><strong className="mt-3 block">Choose a photo</strong><span className="mt-1 block text-xs text-slate-500">Processed locally in this browser</span></div>}
          <input type="file" multiple accept="image/jpeg,image/png,image/webp,image/*" className="sr-only" onChange={(event) => select(Array.from(event.target.files ?? []))} />
        </label>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <ActionButton disabled={!files.length || busy} onClick={() => void clean()}><ShieldCheck className="size-4" />{busy ? 'Removing metadata...' : `Clean ${files.length || ''} photo${files.length === 1 ? '' : 's'}`}</ActionButton>
          {results.length === 1 ? <a href={results[0].url} download={results[0].name} className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-bold dark:border-slate-700"><Download className="size-4" />Download clean JPEG</a> : <ActionButton disabled tone="muted"><Download className="size-4" />{results.length ? 'Download copies below' : 'Download after review'}</ActionButton>}
        </div>
        {results.length > 1 ? <div className="mt-3 max-h-64 space-y-2 overflow-auto">{results.map((result) => <a key={result.name} href={result.url} download={result.name} className="flex min-h-11 items-center justify-between gap-3 rounded-xl border border-slate-200 px-3 text-xs font-bold dark:border-slate-700"><span className="min-w-0 truncate">{result.source}<small className="ml-1 text-slate-500">{size(result.originalSize)} → {size(result.size)}</small></span><Download className="size-4 shrink-0" /></a>)}</div> : null}
        {results.length ? <ActionButton tone="muted" className="mt-2 w-full" onClick={() => { void shareCard({ title: 'Photo Privacy', headline: `${results.length} metadata-free photo ${results.length === 1 ? 'copy is' : 'copies are'} ready`, detail: 'PureHub created separate JPEG copies locally without copying the original EXIF metadata.' }); void trackProductEvent('photo-privacy', 'share') }}><Share2 className="size-4" />Share privacy workflow</ActionButton> : null}
      </Panel>
      <Panel title="What changes" subtitle="A clear privacy boundary before you share.">
        <div className="mb-3 rounded-2xl bg-violet-50 p-3 text-xs leading-5 text-violet-950 dark:bg-violet-950/30 dark:text-violet-100"><strong>Before:</strong> original photo and its metadata. <strong>After:</strong> a new JPEG pixel copy with location, camera, author, and EXIF blocks excluded.</div>
        <div className="space-y-3">
          <Info icon={<MapPinOff className="size-5" />} title="Metadata removed" text="The new JPEG does not copy GPS, camera model, author, or original EXIF blocks." />
          <Info icon={<ShieldCheck className="size-5" />} title="Original protected" text="PureHub creates a separate file and never overwrites or deletes your source photo." />
          <Info icon={<Download className="size-5" />} title="You control sharing" text="Nothing leaves the browser until you download and share the clean copy yourself." />
        </div>
      </Panel>
    </div>
  </div>
}

function Info({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="rounded-2xl border border-slate-200 p-4 dark:border-slate-700"><div className="text-violet-600 dark:text-violet-300">{icon}</div><strong className="mt-2 block text-sm">{title}</strong><p className="mt-1 text-xs leading-5 text-slate-500">{text}</p></div>
}

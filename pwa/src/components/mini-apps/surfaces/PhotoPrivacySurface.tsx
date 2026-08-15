import { useState } from 'react'
import { Download, ImagePlus, MapPinOff, ShieldCheck } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'

function size(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), 3)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

export default function PhotoPrivacySurface() {
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState('')
  const [result, setResult] = useState<{ url: string; size: number; name: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('Choose a photo. The original will never be changed.')

  const select = (next: File | undefined) => {
    if (preview) URL.revokeObjectURL(preview)
    if (result) URL.revokeObjectURL(result.url)
    setFile(next ?? null)
    setPreview(next ? URL.createObjectURL(next) : '')
    setResult(null)
    setMessage(next ? `${next.name} selected · ${size(next.size)}` : 'Choose a photo. The original will never be changed.')
  }

  const clean = async () => {
    if (!file) return
    setBusy(true)
    try {
      const bitmap = await createImageBitmap(file)
      const scale = Math.min(1, 4096 / Math.max(bitmap.width, bitmap.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(bitmap.width * scale))
      canvas.height = Math.max(1, Math.round(bitmap.height * scale))
      canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', .92))
      bitmap.close()
      if (!blob) throw new Error('Export failed')
      if (result) URL.revokeObjectURL(result.url)
      const next = { url: URL.createObjectURL(blob), size: blob.size, name: `${file.name.replace(/\.[^.]+$/, '')}-privacy-clean.jpg` }
      setResult(next)
      setMessage(`Privacy-clean copy ready · ${size(blob.size)}. Original EXIF fields were not copied.`)
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
          <input type="file" accept="image/jpeg,image/png,image/webp,image/*" className="sr-only" onChange={(event) => select(event.target.files?.[0])} />
        </label>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <ActionButton disabled={!file || busy} onClick={() => void clean()}><ShieldCheck className="size-4" />{busy ? 'Removing metadata...' : 'Create private copy'}</ActionButton>
          {result ? <a href={result.url} download={result.name} className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-bold dark:border-slate-700"><Download className="size-4" />Download clean JPEG</a> : <ActionButton disabled tone="muted"><Download className="size-4" />Download after review</ActionButton>}
        </div>
      </Panel>
      <Panel title="What changes" subtitle="A clear privacy boundary before you share.">
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

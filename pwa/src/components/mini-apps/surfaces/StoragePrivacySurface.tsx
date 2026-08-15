import { useMemo, useState } from 'react'
import { Archive, Download, Images, ScanSearch, ShieldCheck } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, Panel } from '../MiniAppPrimitives'

type ReviewedFile = { file: File; hash?: string }

function size(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), 3)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

function download(blob: Blob, name: string) {
  const anchor = document.createElement('a'); anchor.href = URL.createObjectURL(blob); anchor.download = name; anchor.click()
  setTimeout(() => URL.revokeObjectURL(anchor.href), 1000)
}

export default function StoragePrivacySurface() {
  const [files, setFiles] = useState<ReviewedFile[]>([])
  const [running, setRunning] = useState(false)
  const [photoMessage, setPhotoMessage] = useState('Choose a photo to create a fresh copy without embedded EXIF metadata.')
  const duplicates = useMemo(() => {
    const groups = new Map<string, number[]>()
    files.forEach((item, index) => { if (item.hash) groups.set(item.hash, [...(groups.get(item.hash) ?? []), index]) })
    return new Set([...groups.values()].filter((group) => group.length > 1).flat())
  }, [files])

  const analyze = async () => {
    setRunning(true)
    setFiles(await Promise.all(files.map(async (item) => {
      const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', await item.file.arrayBuffer()))
      return { ...item, hash: Array.from(digest, (byte) => byte.toString(16).padStart(2, '0')).join('') }
    })))
    setRunning(false)
  }

  const cleanPhoto = async (file: File) => {
    try {
      const bitmap = await createImageBitmap(file)
      const scale = Math.min(1, 4096 / Math.max(bitmap.width, bitmap.height))
      const canvas = document.createElement('canvas'); canvas.width = Math.round(bitmap.width * scale); canvas.height = Math.round(bitmap.height * scale)
      canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', .92))
      bitmap.close()
      if (!blob) throw new Error('Could not export')
      download(blob, `${file.name.replace(/\.[^.]+$/, '')}-privacy-clean.jpg`)
      setPhotoMessage(`Created a clean ${size(blob.size)} JPEG. The original was not changed.`)
    } catch { setPhotoMessage('This image could not be processed in the current browser.') }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Storage Care flagship" title="Storage Insight & Photo Privacy" description="Find exact duplicate files with SHA-256 and create metadata-free photo copies without silent deletion." accent="emerald" />
    <Panel title="Exact duplicate review" subtitle="Only files you explicitly choose are read. Matching hashes indicate identical bytes.">
      <FormInput type="file" multiple onChange={(event) => setFiles(Array.from(event.target.files ?? [], (file) => ({ file })))} />
      <div className="mt-3 grid grid-cols-3 gap-2"><Metric label="Files" value={String(files.length)} /><Metric label="Total" value={size(files.reduce((sum, item) => sum + item.file.size, 0))} /><Metric label="Exact copies" value={String(duplicates.size)} /></div>
      <ActionButton className="mt-3" disabled={!files.length || running} onClick={() => void analyze()}><ScanSearch className="mr-2 inline size-4" />{running ? 'Hashing...' : 'Find exact duplicates'}</ActionButton>
      <div className="mt-3 max-h-72 space-y-2 overflow-auto">{files.map((item, index) => <div key={`${item.file.name}-${item.file.lastModified}`} className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700"><Archive className="size-4 text-emerald-600" /><div className="min-w-0 flex-1"><strong className="block truncate text-xs">{item.file.name}</strong><span className="text-[11px] text-slate-500">{size(item.file.size)}</span></div>{duplicates.has(index) ? <span className="rounded-full bg-amber-100 px-2 py-1 text-[10px] font-black text-amber-900">Exact duplicate</span> : item.hash ? <ShieldCheck className="size-4 text-emerald-600" /> : null}</div>)}</div>
    </Panel>
    <Panel title="Photo Privacy" subtitle={photoMessage}>
      <label className="flex min-h-24 cursor-pointer items-center justify-center rounded-2xl border border-dashed border-slate-300 text-sm font-bold dark:border-slate-700"><Images className="mr-2 size-5" />Choose photo<input type="file" accept="image/*" className="sr-only" onChange={(event) => { const file = event.target.files?.[0]; if (file) void cleanPhoto(file) }} /></label>
      <p className="mt-3 flex gap-2 rounded-xl bg-emerald-50 p-3 text-xs leading-5 text-emerald-900 dark:bg-emerald-950/35 dark:text-emerald-100"><Download className="size-4 shrink-0" />The exported JPEG is freshly encoded, which removes GPS, camera, author and other original EXIF fields.</p>
    </Panel>
  </div>
}

function Metric({ label, value }: { label: string; value: string }) { return <div className="rounded-xl border border-slate-200 p-3 dark:border-slate-700"><span className="text-[11px] text-slate-500">{label}</span><strong className="block text-lg">{value}</strong></div> }

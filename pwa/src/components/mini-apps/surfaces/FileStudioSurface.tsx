import { useMemo, useState } from 'react'
import { unzipSync, zipSync } from 'fflate'
import { Archive, Download, FileKey, Share2 } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

type FileInfo = { file: File; hash?: string }

function formatBytes(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

function saveBlob(blob: Blob, name: string) {
  const anchor = document.createElement('a')
  anchor.href = URL.createObjectURL(blob)
  anchor.download = name
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(anchor.href), 1000)
}

export default function FileStudioSurface() {
  const [items, setItems] = useState<FileInfo[]>([])
  const [message, setMessage] = useState('Choose local files. PureHub never uploads them.')
  const total = useMemo(() => items.reduce((sum, item) => sum + item.file.size, 0), [items])

  const hashAll = async () => {
    const next = await Promise.all(items.map(async (item) => {
      const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', await item.file.arrayBuffer()))
      return { ...item, hash: Array.from(digest, (value) => value.toString(16).padStart(2, '0')).join('') }
    }))
    setItems(next)
    setMessage('SHA-256 checksums calculated locally.')
    markToolSuccess('file-studio', { headline: 'File checksums calculated', detail: `SHA-256 hashes were generated locally for ${next.length} file${next.length === 1 ? '' : 's'}.`, shareText: 'I verified file checksums locally with PureHub.' })
  }

  const createZip = async () => {
    const input: Record<string, Uint8Array> = {}
    for (const item of items) input[item.file.name] = new Uint8Array(await item.file.arrayBuffer())
    const bytes = zipSync(input, { level: 6 })
    saveBlob(new Blob([bytes as BlobPart], { type: 'application/zip' }), 'purehub-files.zip')
    setMessage(`Created a ${formatBytes(bytes.byteLength)} ZIP locally.`)
    markToolSuccess('file-studio', { headline: 'ZIP archive created', detail: `${items.length} local file${items.length === 1 ? '' : 's'} were archived without upload.`, shareText: 'I created a local ZIP archive with PureHub.' })
  }

  const extract = async (file: File) => {
    try {
      const files = unzipSync(new Uint8Array(await file.arrayBuffer()))
      Object.entries(files).forEach(([name, bytes], index) => {
        window.setTimeout(() => saveBlob(new Blob([bytes as BlobPart]), name.split('/').pop() || `file-${index + 1}`), index * 120)
      })
      setMessage(`Extracted ${Object.keys(files).length} entries as local downloads.`)
      markToolSuccess('file-studio', { headline: 'ZIP extracted locally', detail: `${Object.keys(files).length} file${Object.keys(files).length === 1 ? '' : 's'} were prepared as local downloads.`, shareText: 'I extracted a ZIP locally with PureHub.' })
    } catch { setMessage('This ZIP could not be read.') }
  }

  const share = async () => {
    const files = items.map((item) => item.file)
    if (navigator.share && (!navigator.canShare || navigator.canShare({ files }))) {
      await navigator.share({ title: 'Shared from PureHub', text: 'Files shared locally from PureHub.', files })
      setMessage('Opened the system share sheet.')
    } else setMessage('File sharing is not supported here. Create a ZIP and download it instead.')
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="File utility flagship" title="File Studio" description="Inspect, hash, archive and share files using local browser APIs—without cloud uploads." accent="sky" />
    <Panel title="Workspace" subtitle={message}>
      <FormInput type="file" multiple onChange={(event) => setItems(Array.from(event.target.files ?? [], (file) => ({ file })))} />
      <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <ActionButton disabled={!items.length} onClick={() => void hashAll()}><FileKey className="mr-2 inline size-4" />Hash</ActionButton>
        <ActionButton disabled={!items.length} onClick={() => void createZip()}><Archive className="mr-2 inline size-4" />ZIP</ActionButton>
        <ActionButton disabled={!items.length} onClick={() => void share()}><Share2 className="mr-2 inline size-4" />Share</ActionButton>
        <label className="flex min-h-10 cursor-pointer items-center justify-center rounded-[11px] border border-slate-300 bg-white px-3 text-sm font-bold dark:border-slate-600 dark:bg-slate-800"><Download className="mr-2 size-4" />Extract ZIP<input className="sr-only" type="file" accept=".zip,application/zip" onChange={(event) => { const file = event.target.files?.[0]; if (file) void extract(file) }} /></label>
      </div>
      <div className="mt-3 flex gap-2 text-xs text-slate-500"><span>{items.length} files</span><span>•</span><span>{formatBytes(total)}</span></div>
      <div className="mt-3 space-y-2">{items.slice(0, 40).map(({ file, hash }) => <div key={`${file.name}-${file.lastModified}`} className="rounded-[12px] bg-slate-500/5 px-3 py-2"><div className="flex justify-between gap-3"><strong className="truncate text-sm">{file.name}</strong><span className="text-xs text-slate-500">{formatBytes(file.size)}</span></div>{hash ? <code className="mt-1 block break-all text-[10px] text-slate-500">{hash}</code> : null}</div>)}</div>
    </Panel>
    <p className="rounded-[14px] bg-sky-500/10 p-3 text-xs text-sky-800 dark:text-sky-200">Local Share uses your operating system's share sheet. It does not run an unencrypted PureHub transfer server.</p>
  </div>
}

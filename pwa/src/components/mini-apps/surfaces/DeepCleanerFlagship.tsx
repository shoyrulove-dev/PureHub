import { useCallback, useEffect, useMemo, useState } from 'react'
import { CheckCircle2, Database, Download, FileSearch, HardDrive, RefreshCw, ShieldCheck, Trash2 } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, Panel } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'

type ReviewedFile = { file: File; hash?: string }
type BrowserStorage = {
  cacheNames: string[]
  localKeys: string[]
  databases: string[]
  usage: number
  quota: number
}

const EMPTY_STORAGE: BrowserStorage = { cacheNames: [], localKeys: [], databases: [], usage: 0, quota: 0 }

function formatBytes(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

export default function DeepCleanerFlagship() {
  const [storage, setStorage] = useState<BrowserStorage>(EMPTY_STORAGE)
  const [files, setFiles] = useState<ReviewedFile[]>([])
  const [hashing, setHashing] = useState(false)
  const [cleaning, setCleaning] = useState(false)
  const [reviewFilter, setReviewFilter] = useState<'all' | 'duplicates' | 'large' | 'images'>('all')
  const [message, setMessage] = useState('Nothing is removed until you review and confirm it.')

  const inspectStorage = useCallback(async () => {
    const estimate = await navigator.storage?.estimate?.()
    const cacheNames = 'caches' in window ? await caches.keys() : []
    const databases = typeof indexedDB.databases === 'function'
      ? (await indexedDB.databases()).map((database) => database.name).filter((name): name is string => Boolean(name))
      : []
    const localKeys = Array.from({ length: localStorage.length }, (_, index) => localStorage.key(index))
      .filter((key): key is string => Boolean(key))
      .filter((key) => key.startsWith('purehub.'))
    setStorage({
      cacheNames,
      databases,
      localKeys,
      usage: estimate?.usage ?? 0,
      quota: estimate?.quota ?? 0,
    })
  }, [])

  useEffect(() => { void inspectStorage() }, [inspectStorage])

  const duplicateIndexes = useMemo(() => {
    const grouped = new Map<string, number[]>()
    files.forEach((item, index) => {
      if (item.hash) grouped.set(item.hash, [...(grouped.get(item.hash) ?? []), index])
    })
    return new Set([...grouped.values()].filter((group) => group.length > 1).flatMap((group) => group.slice(1)))
  }, [files])

  const duplicateBytes = [...duplicateIndexes].reduce((total, index) => total + files[index].file.size, 0)
  const largeIndexes = useMemo(() => new Set(files.map((item, index) => item.file.size >= 25 * 1024 * 1024 ? index : -1).filter((index) => index >= 0)), [files])
  const imageIndexes = useMemo(() => new Set(files.map((item, index) => item.file.type.startsWith('image/') ? index : -1).filter((index) => index >= 0)), [files])
  const visibleFiles = files.map((item, index) => ({ item, index })).filter(({ index }) => reviewFilter === 'all' || reviewFilter === 'duplicates' && duplicateIndexes.has(index) || reviewFilter === 'large' && largeIndexes.has(index) || reviewFilter === 'images' && imageIndexes.has(index))

  const analyzeFiles = async () => {
    if (!files.length) return
    setHashing(true)
    setMessage('Calculating exact SHA-256 fingerprints locally...')
    try {
      const reviewed = await Promise.all(files.map(async (item) => {
        const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', await item.file.arrayBuffer()))
        return { ...item, hash: Array.from(digest, (byte) => byte.toString(16).padStart(2, '0')).join('') }
      }))
      setFiles(reviewed)
      setMessage('Review complete. PureHub cannot delete files from your folders; use the list below as a safe guide.')
      markToolSuccess('deep-cleaner', {
        headline: 'Storage review complete',
        detail: `${reviewed.length} selected file${reviewed.length === 1 ? '' : 's'} checked locally; nothing was removed automatically.`,
        shareText: 'I reviewed local browser storage and exact file duplicates without uploading or silently deleting anything.',
      })
    } finally {
      setHashing(false)
    }
  }

  const clearOfflineCache = async () => {
    if (!storage.cacheNames.length || !window.confirm('Remove PureHub offline cache? The app may need the network once to rebuild it. Your saved tool data will stay.')) return
    setCleaning(true)
    const removed = (await Promise.all(storage.cacheNames.map((name) => caches.delete(name)))).filter(Boolean).length
    setMessage(`Removed ${removed} offline cache ${removed === 1 ? 'bundle' : 'bundles'}. Saved tool data was not touched.`)
    markToolSuccess('deep-cleaner', {
      headline: 'Rebuildable cache cleared',
      detail: `${removed} offline cache ${removed === 1 ? 'bundle was' : 'bundles were'} removed. Saved tool data stayed protected.`,
      shareText: 'I cleared rebuildable app cache while keeping saved local data protected.',
    })
    await inspectStorage()
    setCleaning(false)
  }

  const usagePercent = storage.quota ? Math.min(100, Math.round(storage.usage / storage.quota * 100)) : 0
  const exportReview = () => {
    const rows = ['Status,File,Bytes,SHA-256', ...files.map((item, index) => [duplicateIndexes.has(index) ? 'Exact copy' : 'Unique', item.file.name, item.file.size, item.hash ?? ''].map((value) => `"${String(value).replaceAll('"', '""')}"`).join(','))]
    const url = URL.createObjectURL(new Blob([rows.join('\n')], { type: 'text/csv' }))
    const link = document.createElement('a'); link.href = url; link.download = `purehub-duplicate-review-${new Date().toISOString().slice(0, 10)}.csv`; link.click(); URL.revokeObjectURL(url)
    markToolSuccess('deep-cleaner', { headline: 'Duplicate review exported', detail: `${files.length} reviewed files were saved in a local report; no file content was uploaded.`, shareText: 'I exported a private exact-duplicate review without uploading or deleting my files.' })
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Storage Care flagship" title="Deep Cleaner" description="Understand browser storage, verify exact duplicate files, and reclaim space with explicit review at every step." accent="emerald" />

    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <Metric icon={<HardDrive className="size-5" />} label="Browser storage" value={formatBytes(storage.usage)} detail={`${usagePercent}% of available quota`} />
      <Metric icon={<Database className="size-5" />} label="Offline caches" value={String(storage.cacheNames.length)} detail="App shell only" />
      <Metric icon={<ShieldCheck className="size-5" />} label="Private data stores" value={String(storage.databases.length + storage.localKeys.length)} detail="Never auto-selected" />
      <Metric icon={<FileSearch className="size-5" />} label="Exact duplicate space" value={formatBytes(duplicateBytes)} detail={`${duplicateIndexes.size} reviewed copies`} />
    </div>

    <Panel title="Storage control center" subtitle="Cache can be rebuilt. IndexedDB and local preferences may contain your saved tool data, so Deep Cleaner leaves them alone.">
      <div className="h-3 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"><div className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-cyan-400 transition-all" style={{ width: `${Math.max(usagePercent, storage.usage ? 2 : 0)}%` }} /></div>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <StorageRow label="Rebuildable cache" value={`${storage.cacheNames.length} bundles`} safe />
        <StorageRow label="Local preferences" value={`${storage.localKeys.length} keys`} />
        <StorageRow label="Private databases" value={`${storage.databases.length} stores`} />
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <ActionButton onClick={() => void inspectStorage()} tone="muted"><RefreshCw className="size-4" />Refresh audit</ActionButton>
        <ActionButton onClick={() => void clearOfflineCache()} tone="danger" disabled={!storage.cacheNames.length || cleaning}><Trash2 className="size-4" />{cleaning ? 'Cleaning...' : 'Clear offline cache'}</ActionButton>
      </div>
    </Panel>

    <Panel title="Exact duplicate review" subtitle="Choose files yourself. SHA-256 comparison happens in this browser; files are never uploaded or silently removed.">
      <FormInput type="file" multiple onChange={(event) => { setFiles(Array.from(event.target.files ?? [], (file) => ({ file }))); setMessage('Files selected. Start review to compare exact content.') }} />
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <ActionButton onClick={() => void analyzeFiles()} disabled={!files.length || hashing}><FileSearch className="size-4" />{hashing ? 'Comparing...' : 'Find exact duplicates'}</ActionButton>
        <ActionButton tone="muted" onClick={exportReview} disabled={!files.some((item) => item.hash)}><Download className="size-4" />Export review</ActionButton>
        {files.length ? <span className="text-xs font-semibold text-slate-500">{files.length} files · {formatBytes(files.reduce((sum, item) => sum + item.file.size, 0))}</span> : null}
      </div>
      <p className="mt-3 rounded-xl bg-emerald-50 px-3 py-2 text-xs font-semibold leading-5 text-emerald-900 dark:bg-emerald-950/35 dark:text-emerald-100">{message}</p>
      {files.length ? <div className="mt-3 grid grid-cols-4 gap-1 rounded-2xl bg-slate-100 p-1 dark:bg-slate-800">{([
        ['all', `All ${files.length}`],
        ['duplicates', `Copies ${duplicateIndexes.size}`],
        ['large', `Large ${largeIndexes.size}`],
        ['images', `Images ${imageIndexes.size}`],
      ] as const).map(([value, label]) => <button key={value} type="button" onClick={() => setReviewFilter(value)} className={`min-h-10 rounded-xl px-1 text-[11px] font-black transition ${reviewFilter === value ? 'bg-white text-emerald-800 shadow-sm dark:bg-slate-950 dark:text-emerald-300' : 'text-slate-500'}`}>{label}</button>)}</div> : null}
      <div className="mt-3 max-h-80 space-y-2 overflow-auto">
        {visibleFiles.map(({ item, index }) => <div key={`${item.file.name}-${item.file.lastModified}-${index}`} className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700">
          {duplicateIndexes.has(index) ? <FileSearch className="size-4 shrink-0 text-amber-600" /> : <CheckCircle2 className={`size-4 shrink-0 ${item.hash ? 'text-emerald-600' : 'text-slate-400'}`} />}
          <div className="min-w-0 flex-1"><strong className="block truncate text-xs">{item.file.name}</strong><span className="text-[11px] text-slate-500">{formatBytes(item.file.size)} · {item.file.type || 'Unknown type'}</span></div>
          {largeIndexes.has(index) ? <span className="rounded-full bg-violet-100 px-2 py-1 text-[10px] font-black text-violet-900 dark:bg-violet-950 dark:text-violet-200">Large</span> : null}
          {duplicateIndexes.has(index) ? <span className="rounded-full bg-amber-100 px-2 py-1 text-[10px] font-black text-amber-900">Exact copy</span> : null}
        </div>)}
        {files.length && !visibleFiles.length ? <p className="rounded-xl border border-dashed border-slate-300 py-8 text-center text-xs text-slate-500 dark:border-slate-700">No file matches this review filter.</p> : null}
      </div>
    </Panel>
  </div>
}

function Metric({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return <div className="rounded-[22px] border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900"><div className="text-emerald-600 dark:text-emerald-300">{icon}</div><span className="mt-3 block text-xs font-bold text-slate-500">{label}</span><strong className="mt-1 block text-2xl text-slate-950 dark:text-white">{value}</strong><span className="text-[11px] text-slate-500">{detail}</span></div>
}

function StorageRow({ label, value, safe = false }: { label: string; value: string; safe?: boolean }) {
  return <div className="rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><div className="flex items-center justify-between gap-2"><strong className="text-sm">{label}</strong>{safe ? <span className="rounded-full bg-emerald-100 px-2 py-1 text-[10px] font-black text-emerald-800">Rebuildable</span> : <span className="rounded-full bg-slate-100 px-2 py-1 text-[10px] font-black text-slate-600 dark:bg-slate-800 dark:text-slate-300">Protected</span>}</div><p className="mt-2 text-xs text-slate-500">{value}</p></div>
}

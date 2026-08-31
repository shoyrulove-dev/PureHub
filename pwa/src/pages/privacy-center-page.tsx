import { useEffect, useState } from 'react'
import type { ChangeEvent } from 'react'
import { Database, Download, HardDrive, KeyRound, LockKeyhole, Mic, RefreshCw, ShieldCheck, Trash2, Upload, Video } from 'lucide-react'
import { ActionButton, FormInput } from '../components/mini-apps/MiniAppPrimitives'
import { clearPureHubDb, restorePureHubDb, snapshotPureHubDb, type PureHubDbSnapshot } from '../lib/db/purehub-db'
import { decryptBackup, downloadBackup, encryptBackup } from '../lib/encrypted-backup'

type PermissionView = { name: string; state: string }
type SuiteBackup = { localStorage: Record<string, string>; database: PureHubDbSnapshot }

function readPrivateLocalStorage() {
  const data: Record<string, string> = {}
  for (let index = 0; index < localStorage.length; index += 1) {
    const key = localStorage.key(index)
    if (key?.startsWith('purehub')) data[key] = localStorage.getItem(key) ?? ''
  }
  return data
}

export function PrivacyCenterPage() {
  const [storage, setStorage] = useState({ usage: 0, quota: 0, keys: 0, databases: 0 })
  const [permissions, setPermissions] = useState<PermissionView[]>([])
  const [passphrase, setPassphrase] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('Nothing is uploaded. Backups are encrypted in this browser before download.')

  const inspect = async () => {
    const estimate = await navigator.storage?.estimate?.()
    const databases = typeof indexedDB.databases === 'function' ? await indexedDB.databases() : []
    setStorage({ usage: estimate?.usage ?? 0, quota: estimate?.quota ?? 0, keys: Object.keys(readPrivateLocalStorage()).length, databases: databases.filter((item) => item.name?.startsWith('purehub')).length })
    const states = await Promise.all(['camera', 'microphone', 'geolocation'].map(async (name) => {
      try { return { name, state: (await navigator.permissions.query({ name: name as PermissionName })).state } } catch { return { name, state: 'ask when needed' } }
    }))
    setPermissions(states)
  }

  useEffect(() => { void inspect() }, [])

  const exportAll = async () => {
    setBusy(true)
    try {
      const payload: SuiteBackup = { localStorage: readPrivateLocalStorage(), database: await snapshotPureHubDb() }
      downloadBackup(await encryptBackup('purehub-suite-v1', payload, passphrase), `purehub-suite-backup-${new Date().toISOString().slice(0, 10)}.purehub`)
      setNotice(`Encrypted backup created with ${Object.keys(payload.localStorage).length} local settings and ${Object.values(payload.database).reduce((sum, rows) => sum + rows.length, 0)} database records.`)
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not create the encrypted backup.') } finally { setBusy(false) }
  }

  const restoreAll = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = ''; if (!file) return
    setBusy(true)
    try {
      const payload = await decryptBackup<SuiteBackup>(await file.text(), 'purehub-suite-v1', passphrase)
      if (!payload.localStorage || !payload.database || !Array.isArray(payload.database.habits) || !Array.isArray(payload.database.expenses)) throw new Error('The suite backup is malformed.')
      Object.entries(payload.localStorage).forEach(([key, value]) => { if (key.startsWith('purehub') && typeof value === 'string') localStorage.setItem(key, value) })
      await restorePureHubDb(payload.database)
      setNotice('Encrypted suite backup restored. Reload PureHub to refresh every tool.')
      await inspect()
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not restore this backup.') } finally { setBusy(false) }
  }

  const clearAll = async () => {
    if (!window.confirm('Delete all PureHub data stored by this browser? Export a backup first if you may need it. This cannot be undone.')) return
    setBusy(true)
    Object.keys(readPrivateLocalStorage()).forEach((key) => localStorage.removeItem(key))
    await clearPureHubDb()
    setNotice('All PureHub local tool data was removed from this browser. Offline app files remain available.')
    await inspect(); setBusy(false)
  }

  const percent = storage.quota ? Math.min(100, Math.round(storage.usage / storage.quota * 100)) : 0
  return <section className="space-y-5">
    <header className="rounded-[24px] bg-slate-950 p-5 text-white shadow-lg">
      <p className="text-[11px] font-black uppercase tracking-[.2em] text-emerald-300">Privacy & Trust Center</p><h1 className="mt-2 text-3xl font-black">Your data, visible and controllable</h1><p className="mt-2 text-sm leading-6 text-slate-300">Inspect storage and permissions, make one encrypted suite backup, restore it with preview-safe validation, or remove local tool data.</p>
      <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/10"><div className="h-full rounded-full bg-emerald-400" style={{ width: `${Math.max(storage.usage ? 2 : 0, percent)}%` }} /></div>
    </header>
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4"><Metric icon={<HardDrive />} label="Usage" value={formatBytes(storage.usage)} /><Metric icon={<Database />} label="Databases" value={String(storage.databases)} /><Metric icon={<KeyRound />} label="Local keys" value={String(storage.keys)} /><Metric icon={<ShieldCheck />} label="Tool data upload" value="Never" /></div>
    <section className="app-surface rounded-[18px] p-4"><div className="flex items-center justify-between"><div><h2 className="font-black">Permission status</h2><p className="text-xs text-slate-500">Browsers still ask only when a matching tool starts.</p></div><button onClick={() => void inspect()} className="grid size-10 place-items-center rounded-xl" aria-label="Refresh privacy status"><RefreshCw className="size-4" /></button></div><div className="mt-3 grid gap-2 sm:grid-cols-3">{permissions.map((item) => <div key={item.name} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700">{item.name === 'camera' ? <Video className="size-5" /> : item.name === 'microphone' ? <Mic className="size-5" /> : <LockKeyhole className="size-5" />}<span><strong className="block capitalize">{item.name}</strong><small className="capitalize text-slate-500">{item.state}</small></span></div>)}</div></section>
    <section className="app-surface rounded-[18px] p-4"><h2 className="font-black">Unified encrypted backup</h2><p className="mt-1 text-xs leading-5 text-slate-500">Includes local preferences, Result Center, habits, expenses and saved OCR text. File contents and browser permission grants are not included.</p><FormInput className="mt-3" type="password" minLength={12} maxLength={256} autoComplete="new-password" value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="Backup passphrase (12+ characters)" /><div className="mt-3 grid grid-cols-2 gap-2"><ActionButton disabled={busy || passphrase.length < 12} onClick={() => void exportAll()}><Download className="size-4" />Export encrypted</ActionButton><label className={`flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 text-sm font-bold dark:border-slate-700 ${busy || passphrase.length < 12 ? 'pointer-events-none opacity-40' : 'cursor-pointer'}`}><Upload className="size-4" />Restore<input type="file" accept=".purehub,application/json" className="sr-only" onChange={(event) => void restoreAll(event)} /></label></div><p role="status" className="mt-3 rounded-xl bg-emerald-50 p-3 text-xs leading-5 text-emerald-900 dark:bg-emerald-950/35 dark:text-emerald-100">{notice}</p></section>
    <section className="rounded-[18px] border border-rose-200 bg-rose-50 p-4 dark:border-rose-900 dark:bg-rose-950/20"><h2 className="font-black text-rose-950 dark:text-rose-100">Delete local tool data</h2><p className="mt-1 text-xs leading-5 text-rose-800 dark:text-rose-200">Clears PureHub local settings and IndexedDB records after confirmation. It does not revoke browser permissions or delete files already downloaded to your device.</p><ActionButton tone="danger" className="mt-3" disabled={busy} onClick={() => void clearAll()}><Trash2 className="size-4" />Delete all local data</ActionButton></section>
  </section>
}

function formatBytes(value: number) { if (!value) return '0 B'; const units = ['B', 'KB', 'MB', 'GB']; const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), 3); return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}` }
function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) { return <div className="rounded-[18px] border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900"><span className="text-emerald-600 [&>svg]:size-5">{icon}</span><span className="mt-2 block text-xs text-slate-500">{label}</span><strong className="block truncate">{value}</strong></div> }

import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { ActionButton, FormInput, Panel } from '../MiniAppPrimitives'

type VaultItem = {
  id: string
  label: string
  payload: string
  iv: string
  salt?: string
  iterations?: number
  version?: number
}

const STORAGE_KEY = 'purehub-vault-items'
const CURRENT_ITERATIONS = 600_000
const LEGACY_ITERATIONS = 310_000
const MAX_ITEMS = 100

export default function PasswordVaultSurface() {
  const [passphrase, setPassphrase] = useState('')
  const [label, setLabel] = useState('')
  const [secret, setSecret] = useState('')
  const [items, setItems] = useState<VaultItem[]>([])
  const [preview, setPreview] = useState('')
  const [notice, setNotice] = useState('Locked. Enter your master passphrase when you need it.')
  const previewTimer = useRef<number | undefined>(undefined)

  useEffect(() => {
    setItems(parseVaultBackup(window.localStorage.getItem(STORAGE_KEY) ?? '[]'))
    return () => window.clearTimeout(previewTimer.current)
  }, [])

  useEffect(() => {
    if (!passphrase) return
    const timer = window.setTimeout(() => {
      setPassphrase('')
      setPreview('')
      setNotice('Vault locked automatically after five minutes.')
    }, 5 * 60 * 1000)
    return () => window.clearTimeout(timer)
  }, [passphrase])

  const persist = (nextItems: VaultItem[]) => {
    const safeItems = nextItems.slice(0, MAX_ITEMS)
    setItems(safeItems)
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(safeItems))
  }

  const showPreview = (value: string) => {
    window.clearTimeout(previewTimer.current)
    setPreview(value)
    previewTimer.current = window.setTimeout(() => setPreview(''), 30_000)
  }

  const exportBackup = () => {
    const blob = new Blob([JSON.stringify(items, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `purehub-vault-backup-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
  }

  const importBackup = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    try {
      const imported = parseVaultBackup(await file.text())
      if (!imported.length) throw new Error('empty')
      persist(imported)
      setNotice(`Imported ${imported.length} encrypted entries.`)
    } catch {
      setNotice('Backup rejected: the file is empty, malformed, or not a PureHub vault backup.')
    } finally {
      event.target.value = ''
    }
  }

  return (
    <Panel title="Password Vault" subtitle="Versioned PBKDF2 and AES-GCM encryption protect secret values before local storage.">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-[12px] border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-950">
        <p role="status" className="text-xs font-semibold text-slate-600 dark:text-slate-300">{notice}</p>
        <span className="rounded-full bg-emerald-100 px-2 py-1 text-[11px] font-bold text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">Auto-lock: 5 min</span>
      </div>
      <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-3">
          <FormInput type="password" autoComplete="current-password" minLength={12} maxLength={256} value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="Master passphrase (12+ characters)" />
          <FormInput maxLength={120} value={label} onChange={(event) => setLabel(event.target.value)} placeholder="Entry label (not encrypted)" />
          <FormInput type="password" autoComplete="off" maxLength={4096} value={secret} onChange={(event) => setSecret(event.target.value)} placeholder="Secret value" />
          <ActionButton
            disabled={passphrase.length < 12 || !label.trim() || !secret || items.length >= MAX_ITEMS}
            onClick={async () => {
              const encrypted = await encryptSecret(secret, passphrase, CURRENT_ITERATIONS)
              persist([{ id: crypto.randomUUID(), label: label.trim(), ...encrypted, iterations: CURRENT_ITERATIONS, version: 2 }, ...items])
              setLabel('')
              setSecret('')
              setNotice('Encrypted entry saved. The label remains visible; the secret does not.')
            }}
          >Save encrypted entry</ActionButton>
          <div className="grid grid-cols-2 gap-2">
            <ActionButton tone="muted" disabled={!items.length} onClick={exportBackup}>Export backup</ActionButton>
            <label className="grid min-h-11 cursor-pointer place-items-center rounded-[12px] border border-slate-300 bg-white px-3 text-sm font-bold text-slate-800 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
              Import backup
              <input type="file" accept="application/json,.json" className="sr-only" onChange={importBackup} />
            </label>
          </div>
        </div>

        <div className="space-y-3">
          {items.length ? items.map((item) => (
            <div key={item.id} className="rounded-[14px] border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-950">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-bold text-slate-950 dark:text-white">{item.label}</p>
                <div className="flex gap-2">
                  <ActionButton
                    className="min-h-9 px-3 py-1.5 text-xs"
                    tone="muted"
                    onClick={async () => {
                      if (!passphrase) return setNotice('Enter your master passphrase first.')
                      try {
                        const decrypted = await decryptSecret(item, passphrase)
                        showPreview(decrypted)
                        setNotice('Secret revealed for 30 seconds.')
                      } catch {
                        setNotice('Decryption failed. Check the passphrase or backup integrity.')
                      }
                    }}
                  >Reveal</ActionButton>
                  <ActionButton className="min-h-9 px-3 py-1.5 text-xs" tone="danger" onClick={() => persist(items.filter((entry) => entry.id !== item.id))}>Delete</ActionButton>
                </div>
              </div>
            </div>
          )) : <p className="rounded-[14px] border border-dashed border-slate-300 p-5 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">No encrypted entries yet.</p>}
          {preview ? (
            <div className="rounded-[14px] border border-emerald-300 bg-emerald-50 p-4 dark:border-emerald-800 dark:bg-emerald-950/35">
              <p className="break-all font-mono text-sm text-emerald-950 dark:text-emerald-100">{preview}</p>
              <ActionButton className="mt-3" tone="muted" onClick={() => setPreview('')}>Hide now</ActionButton>
            </div>
          ) : null}
        </div>
      </div>
      <p className="mt-4 rounded-[12px] bg-amber-50 p-3 text-xs font-medium leading-5 text-amber-900 dark:bg-amber-950/35 dark:text-amber-100">
        Security boundary: encrypted data is local, but browser extensions, malware, a compromised page, forgotten passphrases, and device loss remain risks. Keep a tested backup and do not use this experimental vault as the only copy of critical credentials before an independent audit.
      </p>
    </Panel>
  )
}

function parseVaultBackup(raw: string): VaultItem[] {
  try {
    const value: unknown = JSON.parse(raw)
    if (!Array.isArray(value) || value.length > MAX_ITEMS) return []
    return value.filter((item): item is VaultItem => {
      if (!item || typeof item !== 'object') return false
      const candidate = item as Partial<VaultItem>
      return [candidate.id, candidate.label, candidate.payload, candidate.iv].every((field) => typeof field === 'string' && field.length > 0)
        && (!candidate.salt || typeof candidate.salt === 'string')
        && (!candidate.iterations || (Number.isInteger(candidate.iterations) && candidate.iterations >= 100_000 && candidate.iterations <= 2_000_000))
    })
  } catch {
    return []
  }
}

async function deriveCryptoKey(passphrase: string, salt: ArrayBuffer, iterations: number) {
  const baseKey = await crypto.subtle.importKey('raw', new TextEncoder().encode(passphrase), 'PBKDF2', false, ['deriveKey'])
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt, iterations, hash: 'SHA-256' },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  )
}

async function encryptSecret(secret: string, passphrase: string, iterations: number) {
  const salt = crypto.getRandomValues(new Uint8Array(16))
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const key = await deriveCryptoKey(passphrase, salt.buffer as ArrayBuffer, iterations)
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(secret))
  return { iv: toBase64(iv.buffer), salt: toBase64(salt.buffer), payload: toBase64(encrypted) }
}

async function decryptSecret(item: VaultItem, passphrase: string) {
  const salt = item.salt ? fromBase64(item.salt) : new TextEncoder().encode('purehub-vault-salt').buffer as ArrayBuffer
  const key = await deriveCryptoKey(passphrase, salt, item.iterations ?? LEGACY_ITERATIONS)
  const decrypted = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: new Uint8Array(fromBase64(item.iv)) }, key, fromBase64(item.payload))
  return new TextDecoder().decode(decrypted)
}

function toBase64(buffer: ArrayBuffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
}

function fromBase64(base64: string) {
  const binary = atob(base64)
  return Uint8Array.from(binary, (character) => character.charCodeAt(0)).buffer
}

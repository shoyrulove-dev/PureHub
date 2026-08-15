import { useEffect, useMemo, useState } from 'react'
import { Check, Clipboard, KeyRound, LockKeyhole, Plus, Trash2 } from 'lucide-react'
import { ActionButton, FlagshipHero, FormInput, Panel } from '../MiniAppPrimitives'

type StoredAccount = {
  id: string
  label: string
  issuer: string
  account: string
  salt: string
  iv: string
  secret: string
}

type OpenAccount = StoredAccount & { plainSecret: string; code: string }

const STORE_KEY = 'purehub.authenticator.v1'
const ITERATIONS = 600_000

function bytesToBase64(bytes: Uint8Array) {
  let value = ''
  bytes.forEach((byte) => { value += String.fromCharCode(byte) })
  return btoa(value)
}

function base64ToBytes(value: string) {
  return Uint8Array.from(atob(value), (character) => character.charCodeAt(0))
}

async function deriveKey(passphrase: string, salt: Uint8Array) {
  const material = await crypto.subtle.importKey('raw', new TextEncoder().encode(passphrase), 'PBKDF2', false, ['deriveKey'])
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', hash: 'SHA-256', salt: salt as BufferSource, iterations: ITERATIONS },
    material,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  )
}

async function encryptSecret(secret: string, passphrase: string) {
  const salt = crypto.getRandomValues(new Uint8Array(16))
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const key = await deriveKey(passphrase, salt)
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(secret))
  return { salt: bytesToBase64(salt), iv: bytesToBase64(iv), secret: bytesToBase64(new Uint8Array(encrypted)) }
}

async function decryptSecret(account: StoredAccount, passphrase: string) {
  const key = await deriveKey(passphrase, base64ToBytes(account.salt))
  const plain = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: base64ToBytes(account.iv) },
    key,
    base64ToBytes(account.secret),
  )
  return new TextDecoder().decode(plain)
}

function decodeBase32(value: string) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  const clean = value.toUpperCase().replace(/[^A-Z2-7]/g, '')
  let bits = ''
  for (const character of clean) {
    const index = alphabet.indexOf(character)
    if (index < 0) throw new Error('Invalid Base32 secret')
    bits += index.toString(2).padStart(5, '0')
  }
  const output: number[] = []
  for (let index = 0; index + 8 <= bits.length; index += 8) output.push(Number.parseInt(bits.slice(index, index + 8), 2))
  return new Uint8Array(output)
}

async function createTotp(secret: string) {
  const counter = Math.floor(Date.now() / 30_000)
  const message = new Uint8Array(8)
  let remaining = counter
  for (let index = 7; index >= 0; index -= 1) {
    message[index] = remaining & 0xff
    remaining = Math.floor(remaining / 256)
  }
  const key = await crypto.subtle.importKey('raw', decodeBase32(secret), { name: 'HMAC', hash: 'SHA-1' }, false, ['sign'])
  const digest = new Uint8Array(await crypto.subtle.sign('HMAC', key, message))
  const offset = digest[digest.length - 1] & 0x0f
  const binary = ((digest[offset] & 0x7f) << 24) | (digest[offset + 1] << 16) | (digest[offset + 2] << 8) | digest[offset + 3]
  return String(binary % 1_000_000).padStart(6, '0')
}

function parseInput(raw: string) {
  const trimmed = raw.trim()
  if (!trimmed.toLowerCase().startsWith('otpauth://')) return { secret: trimmed, issuer: '', account: '' }
  const url = new URL(trimmed)
  const label = decodeURIComponent(url.pathname.replace(/^\//, ''))
  const [labelIssuer = '', account = ''] = label.split(':')
  return {
    secret: url.searchParams.get('secret') ?? '',
    issuer: url.searchParams.get('issuer') ?? labelIssuer,
    account,
  }
}

export default function AuthenticatorVaultSurface() {
  const [stored, setStored] = useState<StoredAccount[]>(() => {
    try { return JSON.parse(localStorage.getItem(STORE_KEY) ?? '[]') as StoredAccount[] } catch { return [] }
  })
  const [opened, setOpened] = useState<OpenAccount[]>([])
  const [passphrase, setPassphrase] = useState('')
  const [label, setLabel] = useState('')
  const [issuer, setIssuer] = useState('')
  const [account, setAccount] = useState('')
  const [secretInput, setSecretInput] = useState('')
  const [message, setMessage] = useState('Locked. Your secrets remain encrypted on this device.')
  const [copied, setCopied] = useState('')
  const [now, setNow] = useState(Date.now())
  const remaining = 30 - (Math.floor(now / 1000) % 30)

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!opened.length) return
    void Promise.all(opened.map(async (item) => ({ ...item, code: await createTotp(item.plainSecret) }))).then(setOpened)
    // Codes only need to be regenerated at the start of a TOTP window.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [Math.floor(now / 30_000)])

  const unlocked = opened.length > 0 || (stored.length === 0 && message.startsWith('Unlocked'))
  const progress = useMemo(() => `${Math.round((remaining / 30) * 100)}%`, [remaining])

  const persist = (next: StoredAccount[]) => {
    setStored(next)
    localStorage.setItem(STORE_KEY, JSON.stringify(next))
  }

  const unlock = async () => {
    if (passphrase.length < 8) return setMessage('Use a passphrase with at least 8 characters.')
    if (!stored.length) {
      setMessage('Unlocked. Add your first account below.')
      return
    }
    try {
      const next = await Promise.all(stored.map(async (item) => {
        const plainSecret = await decryptSecret(item, passphrase)
        return { ...item, plainSecret, code: await createTotp(plainSecret) }
      }))
      setOpened(next)
      setMessage('Unlocked locally. Nothing was sent to a server.')
    } catch {
      setOpened([])
      setMessage('Could not unlock. Check your passphrase.')
    }
  }

  const add = async () => {
    if (!unlocked || passphrase.length < 8) return setMessage('Unlock the vault first.')
    try {
      const parsed = parseInput(secretInput)
      if (!parsed.secret) throw new Error('Missing secret')
      await createTotp(parsed.secret)
      const encrypted = await encryptSecret(parsed.secret, passphrase)
      const item: StoredAccount = {
        id: crypto.randomUUID(),
        label: label || parsed.account || parsed.issuer || 'Authenticator',
        issuer: issuer || parsed.issuer,
        account: account || parsed.account,
        ...encrypted,
      }
      persist([...stored, item])
      setOpened([...opened, { ...item, plainSecret: parsed.secret, code: await createTotp(parsed.secret) }])
      setLabel(''); setIssuer(''); setAccount(''); setSecretInput('')
      setMessage('Account encrypted and saved on this device.')
    } catch {
      setMessage('Enter a valid Base32 secret or otpauth:// URI.')
    }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow="Security flagship" title="Authenticator Vault" description="Generate 2FA codes offline. Secrets are protected with AES-GCM and a key derived from your passphrase." accent="violet" />
    <Panel title="Private vault" subtitle={message}>
      <div className="grid gap-2 sm:grid-cols-[1fr_auto_auto]">
        <FormInput type="password" value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="Master passphrase" autoComplete="current-password" />
        <ActionButton onClick={() => void unlock()}><LockKeyhole className="mr-2 inline size-4" />Unlock</ActionButton>
        <ActionButton tone="muted" onClick={() => { setOpened([]); setPassphrase(''); setMessage('Locked.') }}>Lock</ActionButton>
      </div>
    </Panel>
    {unlocked ? <>
      <Panel title="Codes" subtitle="Codes refresh every 30 seconds and never leave your device.">
        <div className="mb-3 h-1.5 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700"><div className="h-full bg-violet-500 transition-[width]" style={{ width: progress }} /></div>
        <div className="grid gap-2 md:grid-cols-2">
          {opened.map((item) => <div key={item.id} className="flex items-center gap-3 rounded-[14px] border border-slate-200 p-3 dark:border-slate-700">
            <KeyRound className="size-5 text-violet-600" />
            <div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{item.label}</p><p className="truncate text-xs text-slate-500">{item.issuer || item.account || 'Local account'}</p></div>
            <button type="button" className="font-mono text-xl font-black tracking-[.16em]" onClick={async () => { await navigator.clipboard.writeText(item.code); setCopied(item.id) }}>{item.code}</button>
            <button type="button" title="Copy code" onClick={async () => { await navigator.clipboard.writeText(item.code); setCopied(item.id) }}>{copied === item.id ? <Check className="size-4 text-emerald-600" /> : <Clipboard className="size-4" />}</button>
            <button type="button" title="Delete account" onClick={() => { persist(stored.filter((entry) => entry.id !== item.id)); setOpened(opened.filter((entry) => entry.id !== item.id)) }}><Trash2 className="size-4 text-rose-600" /></button>
          </div>)}
          {!opened.length ? <p className="text-sm text-slate-500">No accounts yet.</p> : null}
        </div>
      </Panel>
      <Panel title="Add account" subtitle="Paste a Base32 secret or an otpauth:// URI exported by another authenticator.">
        <div className="grid gap-2 sm:grid-cols-2"><FormInput value={label} onChange={(event) => setLabel(event.target.value)} placeholder="Label" /><FormInput value={issuer} onChange={(event) => setIssuer(event.target.value)} placeholder="Issuer (optional)" /><FormInput value={account} onChange={(event) => setAccount(event.target.value)} placeholder="Account (optional)" /><FormInput value={secretInput} onChange={(event) => setSecretInput(event.target.value)} placeholder="Base32 or otpauth://" /></div>
        <ActionButton className="mt-3" onClick={() => void add()}><Plus className="mr-2 inline size-4" />Encrypt & add</ActionButton>
      </Panel>
    </> : null}
    <p className="rounded-[14px] bg-amber-500/10 p-3 text-xs leading-5 text-amber-800 dark:text-amber-200">Keep an independent recovery copy of your 2FA setup keys. PureHub cannot recover a forgotten passphrase or deleted browser data.</p>
  </div>
}

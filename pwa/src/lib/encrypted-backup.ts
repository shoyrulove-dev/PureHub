const MAGIC = 'PUREHUB_WEB_BACKUP'
const ITERATIONS = 310_000

type BackupEnvelope = {
  magic: typeof MAGIC
  version: 1
  kind: string
  createdAt: string
  iterations: number
  salt: string
  iv: string
  ciphertext: string
}

function toBase64(value: ArrayBuffer | Uint8Array) {
  const bytes = value instanceof Uint8Array ? value : new Uint8Array(value)
  let binary = ''
  for (let offset = 0; offset < bytes.length; offset += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000))
  }
  return btoa(binary)
}

function fromBase64(value: string) {
  const binary = atob(value)
  return Uint8Array.from(binary, (character) => character.charCodeAt(0))
}

async function deriveKey(passphrase: string, salt: Uint8Array, iterations: number) {
  if (passphrase.length < 12) throw new Error('Use a backup passphrase with at least 12 characters.')
  const material = await crypto.subtle.importKey('raw', new TextEncoder().encode(passphrase), 'PBKDF2', false, ['deriveKey'])
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', hash: 'SHA-256', salt: salt as BufferSource, iterations },
    material,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  )
}

export async function encryptBackup(kind: string, payload: unknown, passphrase: string) {
  const salt = crypto.getRandomValues(new Uint8Array(16))
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const key = await deriveKey(passphrase, salt, ITERATIONS)
  const plaintext = new TextEncoder().encode(JSON.stringify({ kind, payload }))
  const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plaintext)
  const envelope: BackupEnvelope = {
    magic: MAGIC,
    version: 1,
    kind,
    createdAt: new Date().toISOString(),
    iterations: ITERATIONS,
    salt: toBase64(salt),
    iv: toBase64(iv),
    ciphertext: toBase64(ciphertext),
  }
  return JSON.stringify(envelope, null, 2)
}

export async function decryptBackup<T>(source: string, expectedKind: string, passphrase: string): Promise<T> {
  const envelope = JSON.parse(source) as Partial<BackupEnvelope>
  if (envelope.magic !== MAGIC || envelope.version !== 1 || envelope.kind !== expectedKind) {
    throw new Error('This is not a compatible PureHub backup.')
  }
  if (!envelope.salt || !envelope.iv || !envelope.ciphertext || !envelope.iterations) {
    throw new Error('The backup is incomplete or damaged.')
  }
  const salt = fromBase64(envelope.salt)
  const iv = fromBase64(envelope.iv)
  const key = await deriveKey(passphrase, salt, envelope.iterations)
  const plaintext = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    key,
    fromBase64(envelope.ciphertext),
  )
  const decoded = JSON.parse(new TextDecoder().decode(plaintext)) as { kind?: string; payload?: T }
  if (decoded.kind !== expectedKind || decoded.payload == null) throw new Error('Backup contents do not match this tool.')
  return decoded.payload
}

export function downloadBackup(contents: string, filename: string) {
  const url = URL.createObjectURL(new Blob([contents], { type: 'application/vnd.purehub.backup+json' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 500)
}

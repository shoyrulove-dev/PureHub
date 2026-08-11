export type ReceiptScan = {
  merchant: string
  total: number | null
  tax: number | null
  lines: Array<{ label: string; amount: number }>
  rawText: string
}

const amountPattern = /(?:^|\s)(?:[$€£¥₫]\s*)?(-?\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})|-?\d+[.,]\d{2})(?:\s|$)/g

function parseAmount(value: string) {
  const trimmed = value.replace(/[^\d,.-]/g, '')
  const lastComma = trimmed.lastIndexOf(',')
  const lastDot = trimmed.lastIndexOf('.')
  const decimalIndex = Math.max(lastComma, lastDot)
  const normalized = decimalIndex >= 0
    ? `${trimmed.slice(0, decimalIndex).replace(/[.,]/g, '')}.${trimmed.slice(decimalIndex + 1)}`
    : trimmed
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? Math.abs(parsed) : null
}

export function parseReceiptText(rawText: string): ReceiptScan {
  const rows = rawText.split(/\r?\n/).map((row) => row.trim()).filter(Boolean)
  const candidates = rows.map((row) => {
    const matches = [...row.matchAll(amountPattern)]
    const amount = matches.length ? parseAmount(matches.at(-1)?.[1] ?? '') : null
    const label = matches.length ? row.slice(0, matches.at(-1)?.index ?? row.length).replace(/[:\s-]+$/, '').trim() : row
    return { row, label, amount }
  })
  const totalRows = candidates.filter((item) => item.amount != null && /\b(grand\s*total|amount\s*due|balance|total)\b/i.test(item.row) && !/sub\s*total/i.test(item.row))
  const allAmounts = candidates.flatMap((item) => item.amount == null ? [] : [item.amount])
  const total = totalRows.at(-1)?.amount ?? (allAmounts.length ? Math.max(...allAmounts) : null)
  const tax = candidates.find((item) => item.amount != null && /\b(tax|vat|gst)\b/i.test(item.row))?.amount ?? null
  const merchant = rows.find((row) => /[A-Za-z\p{L}]{3}/u.test(row) && !/receipt|invoice|tax|total|date/i.test(row))?.slice(0, 80) || 'Receipt'
  const lines = candidates
    .filter((item) => item.amount != null && item.label.length >= 2 && !/total|tax|vat|gst|cash|change|balance|subtotal/i.test(item.row))
    .slice(0, 30)
    .map((item) => ({ label: item.label.slice(0, 80), amount: item.amount as number }))
  return { merchant, total, tax, lines, rawText }
}

export async function recognizeReceipt(file: File, onProgress?: (progress: number) => void) {
  const { createWorker, OEM } = await import('tesseract.js')
  const worker = await createWorker('eng', OEM.LSTM_ONLY, {
    logger: (message) => {
      if (message.status === 'recognizing text' && typeof message.progress === 'number') onProgress?.(message.progress)
    },
  })
  try {
    const result = await worker.recognize(file)
    return parseReceiptText(result.data.text)
  } finally {
    await worker.terminate()
  }
}

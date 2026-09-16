export type ReceiptScan = {
  merchant: string
  total: number | null
  tax: number | null
  lines: Array<{ label: string; amount: number }>
  rawText: string
}

const amountPattern = /(?:^|\s)(?:[$\u20ac\u00a3\u00a5\u00ab]\s*)?(-?\d{1,3}(?:[., ]\d{3})*(?:[.,]\d{2})?|-?\d+)(?:\s*(?:VND|USD|RMB|CNY|\u0111|d))?(?:\s|$)/giu
const totalLabelPattern = /\b(grand\s*total|amount\s*due|balance|total|payable|payment)\b|t\u1ed5ng\s*c\u1ed9ng|thanh\s*to\u00e1n|\u5408\u8ba1|\u5408\u8a08|\u603b\u8ba1|\u7e3d\u8a08|\u5e94\u4ed8|\u61c9\u4ed8|\u5b9e\u4ed8|\u5be6\u4ed8/iu
const subtotalLabelPattern = /\b(sub\s*total|subtotal)\b|t\u1ea1m\s*t\u00ednh|ti\u1ec1n\s*h\u00e0ng|\u5c0f\u8ba1|\u5c0f\u8a08/iu
const taxLabelPattern = /\b(tax|vat|gst)\b|thu\u1ebf|\u7a0e|\u7a05/iu
const excludedLinePattern = /\b(total|tax|vat|gst|cash|change|balance|subtotal|payable|payment)\b|t\u1ed5ng\s*c\u1ed9ng|thanh\s*to\u00e1n|thu\u1ebf|t\u1ea1m\s*t\u00ednh|\u5c0f\u8ba1|\u5c0f\u8a08|\u5408\u8ba1|\u5408\u8a08|\u603b\u8ba1|\u7e3d\u8a08|\u5e94\u4ed8|\u61c9\u4ed8|\u7a0e|\u7a05/iu

function parseAmount(value: string) {
  const trimmed = value.replace(/[^\d,.-]/g, '')
  const lastComma = trimmed.lastIndexOf(',')
  const lastDot = trimmed.lastIndexOf('.')
  const separatorIndex = Math.max(lastComma, lastDot)
  const decimalIndex = separatorIndex >= 0 && trimmed.length - separatorIndex - 1 === 2 ? separatorIndex : -1
  const normalized = decimalIndex >= 0
    ? `${trimmed.slice(0, decimalIndex).replace(/[.,]/g, '')}.${trimmed.slice(decimalIndex + 1)}`
    : trimmed.replace(/[.,]/g, '')
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? Math.abs(parsed) : null
}

export function parseReceiptText(rawText: string): ReceiptScan {
  const rows = rawText.split(/\r?\n/).map((row) => row.trim()).filter(Boolean)
  const candidates = rows.map((row) => {
    const matches = [...row.matchAll(amountPattern)]
    const lastMatch = matches.at(-1)
    const amount = lastMatch ? parseAmount(lastMatch[1] ?? '') : null
    const label = lastMatch ? row.slice(0, lastMatch.index ?? row.length).replace(/[:\s-]+$/, '').trim() : row
    return { row, label, amount }
  })
  const totalRows = candidates.filter((item) => item.amount != null && totalLabelPattern.test(item.row) && !subtotalLabelPattern.test(item.row))
  const allAmounts = candidates.flatMap((item) => item.amount == null ? [] : [item.amount])
  const total = totalRows.at(-1)?.amount ?? (allAmounts.length ? Math.max(...allAmounts) : null)
  const tax = candidates.find((item) => item.amount != null && taxLabelPattern.test(item.row))?.amount ?? null
  const merchant = rows.find((row) => /[A-Za-z\p{L}]{3}/u.test(row) && !/receipt|invoice|tax|total|date/iu.test(row))?.slice(0, 80) || 'Receipt'
  const lines = candidates
    .filter((item) => item.amount != null && item.label.length >= 2 && !excludedLinePattern.test(item.row))
    .slice(0, 30)
    .map((item) => ({ label: item.label.slice(0, 80), amount: item.amount as number }))
  return { merchant, total, tax, lines, rawText }
}

export async function recognizeReceipt(file: File, languageOrProgress: string | ((progress: number) => void) = 'eng', progressCallback?: (progress: number) => void) {
  const language = typeof languageOrProgress === 'string' ? languageOrProgress : 'eng'
  const onProgress = typeof languageOrProgress === 'function' ? languageOrProgress : progressCallback
  const { createWorker, OEM } = await import('tesseract.js')
  const worker = await createWorker(language, OEM.LSTM_ONLY, {
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

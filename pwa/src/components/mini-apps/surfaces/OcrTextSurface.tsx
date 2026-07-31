import { useState } from 'react'
import type { ChangeEvent } from 'react'
import { ActionButton, FormInput, FormTextArea, Panel } from '../MiniAppPrimitives'

const OCR_LANGUAGES = [
  { code: 'eng', label: 'English' },
  { code: 'vie', label: 'Tiếng Việt' },
  { code: 'chi_sim', label: '简体中文' },
] as const

export default function OcrTextSurface() {
  const [ocrText, setOcrText] = useState('')
  const [language, setLanguage] = useState<(typeof OCR_LANGUAGES)[number]['code']>('eng')
  const [status, setStatus] = useState('Choose an image to start.')
  const [running, setRunning] = useState(false)

  const handleFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    setRunning(true)
    setOcrText('')
    setStatus(`Loading the ${OCR_LANGUAGES.find((item) => item.code === language)?.label} OCR pack…`)
    let worker: Awaited<ReturnType<(typeof import('tesseract.js'))['createWorker']>> | undefined
    try {
      const { createWorker } = await import('tesseract.js')
      worker = await createWorker(language)
      setStatus('Recognizing text on this device…')
      const result = await worker.recognize(file)
      setOcrText(result.data.text.trim())
      setStatus(result.data.text.trim() ? 'Text extracted successfully.' : 'No readable text was found.')
    } catch {
      setStatus('OCR could not finish. Check storage/network availability for the first language-pack download and try again.')
    } finally {
      await worker?.terminate()
      setRunning(false)
      event.target.value = ''
    }
  }

  return (
    <Panel title="OCR Text" subtitle="The OCR engine and selected language pack load only when you use this tool; images stay on your device.">
      <div className="grid gap-3 sm:grid-cols-[0.7fr_1.3fr]">
        <label className="space-y-1.5 text-sm font-bold text-slate-700 dark:text-slate-200">
          Recognition language
          <select
            value={language}
            disabled={running}
            onChange={(event) => setLanguage(event.target.value as typeof language)}
            className="min-h-11 w-full rounded-[12px] border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-slate-600 dark:bg-slate-950 dark:text-white"
          >
            {OCR_LANGUAGES.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="space-y-1.5 text-sm font-bold text-slate-700 dark:text-slate-200">
          Image
          <FormInput type="file" accept="image/*" disabled={running} onChange={handleFile} />
        </label>
      </div>
      <div className="mt-4 rounded-[14px] border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-950">
        <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
        {ocrText ? (
          <>
            <FormTextArea className="mt-3 min-h-48 resize-y" value={ocrText} onChange={(event) => setOcrText(event.target.value)} aria-label="Extracted text" />
            <ActionButton className="mt-3" tone="muted" onClick={() => void navigator.clipboard.writeText(ocrText)}>Copy text</ActionButton>
          </>
        ) : null}
      </div>
    </Panel>
  )
}

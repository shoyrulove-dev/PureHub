import { useEffect, useState } from 'react'
import type { MiniAppId } from '../features/catalog/tabs'

export type ToolResultRecord = {
  id: string
  miniAppId: MiniAppId
  headline: string
  createdAt: string
}

const STORAGE_KEY = 'purehub.result-center.v1'
const CHANGE_EVENT = 'purehub:result-center-change'
const MAX_RESULTS = 60

export function readToolResults(): ToolResultRecord[] {
  if (typeof window === 'undefined') return []
  try {
    const value = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? '[]') as ToolResultRecord[]
    return Array.isArray(value) ? value.filter((item) => item?.id && item?.miniAppId && item?.createdAt).slice(0, MAX_RESULTS) : []
  } catch {
    return []
  }
}

function writeToolResults(results: ToolResultRecord[]) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(results.slice(0, MAX_RESULTS)))
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function rememberToolResult(miniAppId: MiniAppId, headline: string) {
  if (typeof window === 'undefined') return
  const recent = readToolResults()
  const last = recent[0]
  if (last?.miniAppId === miniAppId && last.headline === headline && Date.now() - new Date(last.createdAt).getTime() < 30_000) return
  writeToolResults([{ id: crypto.randomUUID(), miniAppId, headline, createdAt: new Date().toISOString() }, ...recent])
}

export function removeToolResult(id: string) {
  writeToolResults(readToolResults().filter((item) => item.id !== id))
}

export function clearToolResults() {
  writeToolResults([])
}

export function useToolResults() {
  const [results, setResults] = useState(readToolResults)
  useEffect(() => {
    const refresh = () => setResults(readToolResults())
    window.addEventListener(CHANGE_EVENT, refresh)
    window.addEventListener('storage', refresh)
    return () => { window.removeEventListener(CHANGE_EVENT, refresh); window.removeEventListener('storage', refresh) }
  }, [])
  return results
}

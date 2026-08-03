import type { MiniAppId } from '../catalog/tabs'

export type MiniAppRuntime = {
  storageNamespace: string
  offline: boolean
  capabilities: Array<'camera' | 'microphone' | 'motion' | 'files' | 'local-storage' | 'network-optional'>
  isolation: 'lazy' | 'shared'
}

const cameraTools = new Set<MiniAppId>(['qr-studio', 'ocr-text', 'color-grabber', 'smart-flashlight'])
const microphoneTools = new Set<MiniAppId>(['decibel-meter', 'speaker-cleaner'])
const motionTools = new Set<MiniAppId>(['compass', 'bubble-level'])
const fileTools = new Set<MiniAppId>(['doc-to-pdf', 'ocr-text', 'color-grabber', 'wallpaper-changer'])
const lazyTools = new Set<MiniAppId>(['qr-studio', 'zen-pomodoro', 'zen-breath', 'ocr-text', 'password-vault'])

export function getMiniAppRuntime(id: MiniAppId): MiniAppRuntime {
  const capabilities: MiniAppRuntime['capabilities'] = ['local-storage']
  if (cameraTools.has(id)) capabilities.push('camera')
  if (microphoneTools.has(id)) capabilities.push('microphone')
  if (motionTools.has(id)) capabilities.push('motion')
  if (fileTools.has(id)) capabilities.push('files')
  if (id === 'community-pro-unlock' || id === 'ocr-text') capabilities.push('network-optional')
  return {
    storageNamespace: `purehub.${id}.v1`,
    offline: id !== 'community-pro-unlock',
    capabilities,
    isolation: lazyTools.has(id) ? 'lazy' : 'shared',
  }
}

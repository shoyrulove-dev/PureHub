import type { LucideIcon } from 'lucide-react'
import {
  Camera,
  CreditCard,
  ShieldCheck,
  Sparkles,
  Wrench,
} from 'lucide-react'

export type TabDefinition = {
  id: 'zen-time' | 'measure-tools' | 'vision' | 'security-audio' | 'finance-community'
  path: string
  label: string
  shortLabel: string
  description: string
  icon: LucideIcon
  miniApps: string[]
}

export const TAB_ITEMS: TabDefinition[] = [
  {
    id: 'zen-time',
    path: '/zen-time',
    label: 'Zen & Time',
    shortLabel: 'Zen',
    description: 'Offline time, habit, focus, and breathing tools grouped in one calm tab.',
    icon: Sparkles,
    miniApps: ['Lunar Calendar', 'Zen Habit', 'Zen Pomodoro', 'Zen Breath'],
  },
  {
    id: 'measure-tools',
    path: '/measure-tools',
    label: 'Measure & Tools',
    shortLabel: 'Tools',
    description: 'Fast browser utility tools powered by local sensors and zero-latency math.',
    icon: Wrench,
    miniApps: ['Compass', 'Bubble Level', 'Decibel Meter', 'Unit Converter'],
  },
  {
    id: 'vision',
    path: '/vision',
    label: 'Vision',
    shortLabel: 'Vision',
    description: 'Camera-first offline tools for scanning, snapshots, OCR, and color picking.',
    icon: Camera,
    miniApps: ['QR Studio', 'Doc to PDF', 'OCR Text', 'Color Grabber'],
  },
  {
    id: 'security-audio',
    path: '/security-audio',
    label: 'Security & Audio',
    shortLabel: 'Secure',
    description: 'Private device-side tools for sound cleanup and encrypted secrets.',
    icon: ShieldCheck,
    miniApps: ['Speaker Cleaner', 'Password Vault'],
  },
  {
    id: 'finance-community',
    path: '/finance-community',
    label: 'Finance & Community',
    shortLabel: 'Finance',
    description: 'Local money utilities plus the community unlock growth surface.',
    icon: CreditCard,
    miniApps: ['Bill Splitter', 'Expense Tracker', 'Decision Wheel', 'Community Pro Unlock'],
  },
]

import type { LucideIcon } from 'lucide-react'
import {
  BookOpenCheck,
  BrushCleaning,
  Camera,
  Compass,
  KeyRound,
  Palette,
  QrCode,
  WalletCards,
} from 'lucide-react'
import type { LocaleCode } from '../../i18n/locales'
import type { MiniAppId } from './tabs'

export type WorkspaceId =
  | 'scan-documents'
  | 'qr-codes'
  | 'sensor-lab'
  | 'device-care'
  | 'money'
  | 'focus-time'
  | 'security-vault'
  | 'creative-everyday'

type LocalizedText = Record<LocaleCode, string>

export type WorkspaceDefinition = {
  id: WorkspaceId
  icon: LucideIcon
  title: LocalizedText
  description: LocalizedText
  toolIds: MiniAppId[]
  featuredToolId: MiniAppId
  accentClass: string
  surfaceClass: string
  stage: 'flagship' | 'growing'
}

export const WORKSPACE_ITEMS: WorkspaceDefinition[] = [
  {
    id: 'scan-documents', icon: Camera,
    title: { en: 'Scan & Documents', vi: 'Quét & Tài liệu', zh: '扫描与文档' },
    description: { en: 'Capture, extract, organize and export.', vi: 'Chụp, trích xuất, sắp xếp và xuất file.', zh: '拍摄、提取、整理和导出。' },
    toolIds: ['ocr-text', 'doc-to-pdf', 'file-studio', 'screen-recorder'], featuredToolId: 'ocr-text',
    accentClass: 'text-violet-700 dark:text-violet-300', surfaceClass: 'from-violet-500/16 to-fuchsia-500/5', stage: 'flagship',
  },
  {
    id: 'qr-codes', icon: QrCode,
    title: { en: 'QR & Codes', vi: 'QR & Mã vạch', zh: '二维码与条码' },
    description: { en: 'Scan, verify, create and keep a private library.', vi: 'Quét, kiểm tra, tạo và lưu thư viện riêng tư.', zh: '扫描、验证、创建并保存私人资料库。' },
    toolIds: ['qr-studio'], featuredToolId: 'qr-studio',
    accentClass: 'text-emerald-700 dark:text-emerald-300', surfaceClass: 'from-emerald-500/16 to-cyan-500/5', stage: 'flagship',
  },
  {
    id: 'sensor-lab', icon: Compass,
    title: { en: 'Sensor Lab', vi: 'Phòng đo cảm biến', zh: '传感器实验室' },
    description: { en: 'Calibrated live readings from your phone.', vi: 'Số đo trực tiếp có hiệu chỉnh từ điện thoại.', zh: '来自手机的校准实时读数。' },
    toolIds: ['compass', 'bubble-level', 'decibel-meter'], featuredToolId: 'bubble-level',
    accentClass: 'text-sky-700 dark:text-sky-300', surfaceClass: 'from-sky-500/16 to-indigo-500/5', stage: 'flagship',
  },
  {
    id: 'device-care', icon: BrushCleaning,
    title: { en: 'Device Care', vi: 'Chăm sóc thiết bị', zh: '设备维护' },
    description: { en: 'Understand storage, privacy, sound and connections.', vi: 'Kiểm tra bộ nhớ, riêng tư, âm thanh và kết nối.', zh: '了解存储、隐私、声音和连接。' },
    toolIds: ['wifi-analyzer', 'deep-cleaner', 'photo-privacy', 'speaker-cleaner'], featuredToolId: 'wifi-analyzer',
    accentClass: 'text-cyan-700 dark:text-cyan-300', surfaceClass: 'from-cyan-500/16 to-teal-500/5', stage: 'flagship',
  },
  {
    id: 'money', icon: WalletCards,
    title: { en: 'Money', vi: 'Tài chính', zh: '财务' },
    description: { en: 'Track spending and settle shared bills locally.', vi: 'Theo dõi chi tiêu và chia hóa đơn cục bộ.', zh: '本地记录支出并分摊账单。' },
    toolIds: ['expense-tracker', 'bill-splitter'], featuredToolId: 'expense-tracker',
    accentClass: 'text-amber-700 dark:text-amber-300', surfaceClass: 'from-amber-500/16 to-orange-500/5', stage: 'flagship',
  },
  {
    id: 'focus-time', icon: BookOpenCheck,
    title: { en: 'Focus & Time', vi: 'Tập trung & Thời gian', zh: '专注与时间' },
    description: { en: 'Build calm routines that work offline.', vi: 'Xây thói quen bình tĩnh, hoạt động offline.', zh: '建立可离线使用的平静习惯。' },
    toolIds: ['zen-habit', 'zen-pomodoro', 'zen-breath', 'lunar-calendar'], featuredToolId: 'zen-pomodoro',
    accentClass: 'text-teal-700 dark:text-teal-300', surfaceClass: 'from-teal-500/16 to-emerald-500/5', stage: 'flagship',
  },
  {
    id: 'security-vault', icon: KeyRound,
    title: { en: 'Security Vault', vi: 'Kho bảo mật', zh: '安全保险库' },
    description: { en: 'Protect passwords and two-factor codes on-device.', vi: 'Bảo vệ mật khẩu và mã 2FA ngay trên thiết bị.', zh: '在设备上保护密码和双重验证码。' },
    toolIds: ['authenticator-vault', 'password-vault'], featuredToolId: 'authenticator-vault',
    accentClass: 'text-rose-700 dark:text-rose-300', surfaceClass: 'from-rose-500/14 to-violet-500/5', stage: 'flagship',
  },
  {
    id: 'creative-everyday', icon: Palette,
    title: { en: 'Creative & Everyday', vi: 'Sáng tạo & Hằng ngày', zh: '创意与日常' },
    description: { en: 'Fast utilities for everyday decisions and creation.', vi: 'Tiện ích nhanh cho sáng tạo và quyết định hằng ngày.', zh: '用于日常创作和决策的快捷工具。' },
    toolIds: ['color-grabber', 'wallpaper-changer', 'smart-flashlight', 'unit-converter', 'decision-wheel', 'community-pro-unlock'], featuredToolId: 'color-grabber',
    accentClass: 'text-fuchsia-700 dark:text-fuchsia-300', surfaceClass: 'from-fuchsia-500/14 to-amber-500/5', stage: 'growing',
  },
]

export const WORKSPACE_BY_TOOL = new Map<MiniAppId, WorkspaceDefinition>(
  WORKSPACE_ITEMS.flatMap((workspace) => workspace.toolIds.map((toolId) => [toolId, workspace] as const)),
)

export function workspaceText(text: LocalizedText, locale: LocaleCode) {
  return text[locale] ?? text.en
}

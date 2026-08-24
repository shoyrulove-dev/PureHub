import type { MiniAppDefinition, MiniAppId } from './tabs'

const SEARCH_INTENTS: Record<MiniAppId, string> = {
  'lunar-calendar': 'moon lunar vietnamese date am lich calendar can chi ngay tot',
  'zen-habit': 'habit streak routine goal tracker thoi quen muc tieu',
  'zen-pomodoro': 'focus timer study work productivity tap trung hoc',
  'zen-breath': 'breathing calm relax stress meditation tho giam cang thang',
  compass: 'direction bearing north navigation huong la ban',
  'bubble-level': 'level shelf angle tilt spirit level can bang ke',
  'decibel-meter': 'sound noise microphone db loudness tieng on do am',
  'smart-flashlight': 'torch light sos emergency den pin',
  'unit-converter': 'convert length weight temperature units doi don vi',
  'qr-studio': 'scan barcode qr link url wifi contact create code quet ma vach',
  'doc-to-pdf': 'document image photo pdf merge reorder export tai lieu anh',
  'ocr-text': 'photo image to text receipt document extract recognize scan chu',
  'color-grabber': 'color picker camera hex rgb palette mau',
  'speaker-cleaner': 'water eject tone speaker sound loa nuoc',
  'deep-cleaner': 'storage large duplicate junk files cleanup bo nho don dep',
  'photo-privacy': 'remove exif gps metadata private photo location xoa vi tri anh',
  'wifi-analyzer': 'network signal channel router internet scan mang song wifi',
  'password-vault': 'password credential secret encrypted secure mat khau',
  'wallpaper-changer': 'background image home lock screen hinh nen',
  'bill-splitter': 'split bill tax tip restaurant friends chia hoa don tien tip',
  'expense-tracker': 'money budget spending ledger finance chi tieu ngan sach',
  'decision-wheel': 'random picker choice roulette decide boc tham',
  'community-pro-unlock': 'support feedback roadmap github telegram community cong dong',
  'authenticator-vault': '2fa otp totp authenticator code security xac thuc',
  'file-studio': 'zip archive hash checksum share file compress nen tep',
  'screen-recorder': 'record screen video capture quay man hinh',
}

function normalizeSearch(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase()
    .trim()
}

export function matchesToolSearch(
  tool: MiniAppDefinition,
  title: string,
  summary: string,
  query: string,
) {
  const normalizedQuery = normalizeSearch(query)
  if (!normalizedQuery) return true
  const haystack = normalizeSearch(`${title} ${summary} ${SEARCH_INTENTS[tool.id]}`)
  return normalizedQuery.split(/\s+/).every((term) => haystack.includes(term))
}

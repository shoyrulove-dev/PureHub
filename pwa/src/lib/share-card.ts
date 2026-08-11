type ShareCardOptions = { title: string; headline: string; detail?: string; accent?: string; url?: string }

function wrapText(context: CanvasRenderingContext2D, text: string, maxWidth: number) {
  const lines: string[] = []
  let line = ''
  for (const word of text.trim().split(/\s+/)) {
    const candidate = line ? `${line} ${word}` : word
    if (context.measureText(candidate).width > maxWidth && line) { lines.push(line); line = word } else line = candidate
  }
  if (line) lines.push(line)
  return lines
}

export async function createShareCard(options: ShareCardOptions) {
  const canvas = document.createElement('canvas')
  canvas.width = 1200; canvas.height = 630
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas is unavailable')
  const gradient = context.createLinearGradient(0, 0, 1200, 630)
  gradient.addColorStop(0, '#071526'); gradient.addColorStop(0.62, '#10243a'); gradient.addColorStop(1, options.accent || '#7c3aed')
  context.fillStyle = gradient; context.fillRect(0, 0, 1200, 630)
  context.globalAlpha = 0.18; context.fillStyle = '#fff'
  context.beginPath(); context.arc(1040, 80, 260, 0, Math.PI * 2); context.fill()
  context.beginPath(); context.arc(110, 650, 300, 0, Math.PI * 2); context.fill(); context.globalAlpha = 1
  context.fillStyle = '#5eead4'; context.font = '800 27px system-ui, sans-serif'; context.fillText('PUREHUB  •  FREE  •  NO ADS  •  PRIVATE', 70, 82)
  context.fillStyle = '#fff'; context.font = '900 68px system-ui, sans-serif'; context.fillText(options.title, 70, 175)
  context.font = '800 48px system-ui, sans-serif'
  wrapText(context, options.headline, 920).slice(0, 3).forEach((line, index) => context.fillText(line, 70, 265 + index * 62))
  if (options.detail) {
    context.fillStyle = '#cbd5e1'; context.font = '600 29px system-ui, sans-serif'
    wrapText(context, options.detail, 960).slice(0, 2).forEach((line, index) => context.fillText(line, 70, 480 + index * 38))
  }
  context.fillStyle = '#99f6e4'; context.font = '800 27px system-ui, sans-serif'
  context.fillText((options.url || window.location.href).replace(/^https?:\/\//, ''), 70, 580)
  const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('Could not create card')), 'image/png', 0.95))
  return new File([blob], `purehub-${options.title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.png`, { type: 'image/png' })
}

export async function shareCard(options: ShareCardOptions) {
  const url = options.url || window.location.href
  const text = `${options.headline} — ${options.title} on PureHub. Free, ad-free, and privacy-first.`
  const file = await createShareCard({ ...options, url })
  if (navigator.canShare?.({ files: [file] }) && navigator.share) {
    await navigator.share({ title: `${options.title} · PureHub`, text, url, files: [file] }); return 'Shared'
  }
  await navigator.clipboard.writeText(`${text} ${url}`)
  const downloadUrl = URL.createObjectURL(file); const anchor = document.createElement('a')
  anchor.href = downloadUrl; anchor.download = file.name; anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000)
  return 'Card saved'
}

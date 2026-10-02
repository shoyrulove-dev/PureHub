import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'

const sources = [
  ['https://standards-oui.ieee.org/oui/oui.csv', 6],
  ['https://standards-oui.ieee.org/oui28/mam.csv', 7],
  ['https://standards-oui.ieee.org/oui36/oui36.csv', 9],
]
const output = resolve('app/src/main/assets/ieee_oui.tsv')
const parseCsv = (line) => {
  const values = []; let value = ''; let quoted = false
  for (let index = 0; index < line.length; index++) {
    const char = line[index]
    if (char === '"' && quoted && line[index + 1] === '"') { value += '"'; index++ }
    else if (char === '"') quoted = !quoted
    else if (char === ',' && !quoted) { values.push(value); value = '' }
    else value += char
  }
  values.push(value); return values
}

const rows = []
for (const [url, length] of sources) {
  const response = await fetch(url, { headers: { 'User-Agent': 'PureHub OUI builder (https://github.com/shoyrulove-dev/PureHub)' } })
  if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`)
  const lines = (await response.text()).replace(/^\uFEFF/, '').split(/\r?\n/).slice(1)
  for (const line of lines) {
    if (!line.trim()) continue
    const fields = parseCsv(line)
    const assignment = (fields[1] ?? '').replace(/[^0-9A-F]/gi, '').toUpperCase()
    const organization = (fields[2] ?? '').replace(/[\t\r\n|]+/g, ' ').trim()
    if (assignment.length === length && organization) rows.push(`${assignment}\t${organization}`)
  }
}
rows.sort((left, right) => right.split('\t')[0].length - left.split('\t')[0].length || left.localeCompare(right))
await mkdir(dirname(output), { recursive: true })
await writeFile(output, `# IEEE Registration Authority public listings\n# Generated ${new Date().toISOString()}\n${rows.join('\n')}\n`, 'utf8')
console.log(`Wrote ${rows.length} assignments to ${output}`)

import fs from 'node:fs'
import ts from 'typescript'

const file = new URL('../src/i18n/resources.ts', import.meta.url)
const sourceText = fs.readFileSync(file, 'utf8')
const source = ts.createSourceFile(file.pathname, sourceText, ts.ScriptTarget.Latest, true)
let resources

function visit(node) {
  if (ts.isVariableDeclaration(node) && node.name.getText(source) === 'resources') resources = node.initializer
  ts.forEachChild(node, visit)
}

function unwrap(node) {
  if (ts.isAsExpression(node) || ts.isSatisfiesExpression(node)) return unwrap(node.expression)
  return node
}

function collect(node, prefix = '', output = []) {
  const value = unwrap(node)
  if (!ts.isObjectLiteralExpression(value)) return output.push(prefix)
  for (const property of value.properties) {
    if (!ts.isPropertyAssignment(property)) continue
    const key = property.name.getText(source).replace(/^['"]|['"]$/g, '')
    collect(property.initializer, prefix ? `${prefix}.${key}` : key, output)
  }
  return output
}

visit(source)
if (!resources) throw new Error('Could not find the PWA locale resources object.')

const root = unwrap(resources)
if (!ts.isObjectLiteralExpression(root)) throw new Error('Locale resources must be an object literal.')
const locales = Object.fromEntries(root.properties.filter(ts.isPropertyAssignment).map((property) => [property.name.getText(source).replace(/^['"]|['"]$/g, ''), property.initializer]))
const requiredLocales = ['en', 'vi', 'zh', 'es']
for (const locale of requiredLocales) if (!locales[locale]) throw new Error(`Missing locale: ${locale}`)

const english = new Set(collect(locales.en))
const failures = []
for (const locale of requiredLocales.slice(1)) {
  const translated = new Set(collect(locales[locale]))
  for (const key of english) if (!translated.has(key)) failures.push(`${locale}: ${key}`)
}

if (failures.length) throw new Error(`Incomplete PWA locales:\n${failures.join('\n')}`)
console.log(`Validated ${english.size} translation keys across ${requiredLocales.join(', ')}.`)

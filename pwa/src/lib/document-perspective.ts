export type Point = { x: number; y: number }
export type DocumentQuad = { topLeft: Point; topRight: Point; bottomRight: Point; bottomLeft: Point }

function median(values: number[]) {
  if (!values.length) return 0
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)]
}

function distance(a: Point, b: Point) { return Math.hypot(a.x - b.x, a.y - b.y) }

/** Find a high-contrast document area. Returns null rather than risk cutting content. */
export async function detectDocumentQuad(image: HTMLImageElement): Promise<DocumentQuad | null> {
  const scale = Math.min(1, 360 / Math.max(image.naturalWidth, image.naturalHeight))
  const width = Math.max(32, Math.round(image.naturalWidth * scale))
  const height = Math.max(32, Math.round(image.naturalHeight * scale))
  const canvas = document.createElement('canvas'); canvas.width = width; canvas.height = height
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return null
  context.drawImage(image, 0, 0, width, height)
  const pixels = context.getImageData(0, 0, width, height).data
  const luminance = (x: number, y: number) => {
    const offset = (y * width + x) * 4
    return (pixels[offset] * 30 + pixels[offset + 1] * 59 + pixels[offset + 2] * 11) / 100
  }
  const border: number[] = []; const center: number[] = []
  for (let x = 0; x < width; x += 2) border.push(luminance(x, 0), luminance(x, height - 1))
  for (let y = 0; y < height; y += 2) border.push(luminance(0, y), luminance(width - 1, y))
  for (let y = Math.floor(height * .35); y < Math.ceil(height * .65); y += 2) for (let x = Math.floor(width * .35); x < Math.ceil(width * .65); x += 2) center.push(luminance(x, y))
  const borderMedian = median(border); const centerMedian = median(center)
  if (Math.abs(centerMedian - borderMedian) < 18) return null
  const brightDocument = centerMedian > borderMedian
  const threshold = brightDocument ? borderMedian + 14 : borderMedian - 14
  const rows: number[] = []; const left: number[] = []; const right: number[] = []
  for (let y = 0; y < height; y += 1) {
    let first = -1; let last = -1
    for (let x = 0; x < width; x += 1) {
      const matches = brightDocument ? luminance(x, y) >= threshold : luminance(x, y) <= threshold
      if (matches) { if (first < 0) first = x; last = x }
    }
    if (first >= 0 && last - first >= width * .42) { rows.push(y); left.push(first); right.push(last) }
  }
  if (rows.length < height * .32) return null
  const top = rows[0]; const bottom = rows.at(-1) ?? top
  if (bottom - top < height * .32) return null
  const band = Math.max(3, Math.round(rows.length * .12))
  const topLeft = median(left.slice(0, band)); const topRight = median(right.slice(0, band))
  const bottomLeft = median(left.slice(-band)); const bottomRight = median(right.slice(-band))
  if (topRight - topLeft < width * .42 || bottomRight - bottomLeft < width * .42) return null
  const inverse = 1 / scale
  return {
    topLeft: { x: topLeft * inverse, y: top * inverse }, topRight: { x: topRight * inverse, y: top * inverse },
    bottomRight: { x: bottomRight * inverse, y: bottom * inverse }, bottomLeft: { x: bottomLeft * inverse, y: bottom * inverse },
  }
}

function solve(matrix: number[][]) {
  const size = matrix.length
  for (let column = 0; column < size; column += 1) {
    let pivot = column
    for (let row = column + 1; row < size; row += 1) if (Math.abs(matrix[row][column]) > Math.abs(matrix[pivot][column])) pivot = row
    if (Math.abs(matrix[pivot][column]) < 1e-8) return null
    ;[matrix[column], matrix[pivot]] = [matrix[pivot], matrix[column]]
    const divisor = matrix[column][column]
    for (let index = column; index <= size; index += 1) matrix[column][index] /= divisor
    for (let row = 0; row < size; row += 1) if (row !== column) {
      const factor = matrix[row][column]
      for (let index = column; index <= size; index += 1) matrix[row][index] -= factor * matrix[column][index]
    }
  }
  return matrix.map((row) => row[size])
}

/** Projectively rectifies a detected page. The output is capped to keep on-device OCR responsive. */
export function warpDocument(image: HTMLImageElement, quad: DocumentQuad) {
  const baseWidth = Math.max(distance(quad.topLeft, quad.topRight), distance(quad.bottomLeft, quad.bottomRight))
  const baseHeight = Math.max(distance(quad.topLeft, quad.bottomLeft), distance(quad.topRight, quad.bottomRight))
  const scale = Math.min(1, 1800 / Math.max(baseWidth, baseHeight))
  const width = Math.max(1, Math.round(baseWidth * scale)); const height = Math.max(1, Math.round(baseHeight * scale))
  const destination = [{ x: 0, y: 0 }, { x: width - 1, y: 0 }, { x: width - 1, y: height - 1 }, { x: 0, y: height - 1 }]
  const source = [quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft]
  const equations: number[][] = []
  destination.forEach((point, index) => {
    const target = source[index]
    equations.push([point.x, point.y, 1, 0, 0, 0, -target.x * point.x, -target.x * point.y, target.x])
    equations.push([0, 0, 0, point.x, point.y, 1, -target.y * point.x, -target.y * point.y, target.y])
  })
  const h = solve(equations)
  if (!h) return null
  const input = document.createElement('canvas'); input.width = image.naturalWidth; input.height = image.naturalHeight
  const inputContext = input.getContext('2d', { willReadFrequently: true }); if (!inputContext) return null
  inputContext.drawImage(image, 0, 0)
  const sourcePixels = inputContext.getImageData(0, 0, input.width, input.height).data
  const output = document.createElement('canvas'); output.width = width; output.height = height
  const outputContext = output.getContext('2d', { willReadFrequently: true }); if (!outputContext) return null
  const targetPixels = outputContext.createImageData(width, height)
  for (let y = 0; y < height; y += 1) for (let x = 0; x < width; x += 1) {
    const denominator = h[6] * x + h[7] * y + 1
    const sourceX = Math.round((h[0] * x + h[1] * y + h[2]) / denominator)
    const sourceY = Math.round((h[3] * x + h[4] * y + h[5]) / denominator)
    if (sourceX < 0 || sourceY < 0 || sourceX >= input.width || sourceY >= input.height) continue
    const sourceOffset = (sourceY * input.width + sourceX) * 4; const targetOffset = (y * width + x) * 4
    targetPixels.data[targetOffset] = sourcePixels[sourceOffset]; targetPixels.data[targetOffset + 1] = sourcePixels[sourceOffset + 1]; targetPixels.data[targetOffset + 2] = sourcePixels[sourceOffset + 2]; targetPixels.data[targetOffset + 3] = 255
  }
  outputContext.putImageData(targetPixels, 0, 0)
  return output
}

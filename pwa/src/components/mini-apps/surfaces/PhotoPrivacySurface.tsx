import { useState } from 'react'
import { Download, ImagePlus, MapPinOff, Share2, ShieldCheck } from 'lucide-react'
import { ActionButton, FlagshipHero, Panel } from '../MiniAppPrimitives'
import { shareCard } from '../../../lib/share-card'
import { markToolSuccess } from '../../../lib/tool-success'
import { trackProductEvent } from '../../../lib/community-api'
import { normalizeLocale, type LocaleCode } from '../../../i18n/locales'

const PHOTO_COPY: Record<LocaleCode, Record<string, string>> = {
  en: { choose: 'Choose photos. Originals will never be changed.', selected: 'selected', ready: 'privacy-clean copies ready', failed: 'This browser could not decode the selected image. Try JPEG, PNG, or WebP.', eyebrow: 'Private media utility', title: 'Photo Privacy', description: 'Create a clean JPEG copy without the original GPS, camera, author, or EXIF data.', workspace: 'Private photo workspace', preview: 'Selected photo preview', choosePhoto: 'Choose photos', local: 'Processed locally in this browser', removing: 'Removing metadata…', clean: 'Clean photos', download: 'Download clean JPEG', downloads: 'Download copies below', review: 'Download after review', share: 'Share privacy workflow', changes: 'What changes', boundary: 'A clear privacy boundary before sharing.', before: 'Before: original photo and metadata. After: a new JPEG copy without location, camera, author, or EXIF blocks.', removed: 'Metadata removed', removedText: 'The new JPEG excludes GPS, camera model, author, and original EXIF blocks.', protected: 'Original protected', protectedText: 'PureHub creates a separate file and never overwrites or deletes the source.', control: 'You control sharing', controlText: 'Nothing leaves the browser until you download and share the clean copy.' },
  vi: { choose: 'Chọn ảnh. Ảnh gốc sẽ không bị thay đổi.', selected: 'đã chọn', ready: 'bản sao sạch siêu dữ liệu đã sẵn sàng', failed: 'Trình duyệt không đọc được ảnh. Hãy thử JPEG, PNG hoặc WebP.', eyebrow: 'Tiện ích ảnh riêng tư', title: 'Quyền riêng tư ảnh', description: 'Tạo bản JPEG sạch, không sao chép GPS, máy ảnh, tác giả hoặc EXIF gốc.', workspace: 'Không gian xử lý ảnh', preview: 'Xem trước ảnh đã chọn', choosePhoto: 'Chọn ảnh', local: 'Xử lý cục bộ trong trình duyệt', removing: 'Đang xóa siêu dữ liệu…', clean: 'Làm sạch ảnh', download: 'Tải JPEG sạch', downloads: 'Tải các bản sao bên dưới', review: 'Tải sau khi kiểm tra', share: 'Chia sẻ quy trình', changes: 'Những gì thay đổi', boundary: 'Ranh giới riêng tư rõ ràng trước khi chia sẻ.', before: 'Trước: ảnh gốc cùng siêu dữ liệu. Sau: bản JPEG mới không có vị trí, máy ảnh, tác giả hoặc EXIF.', removed: 'Đã loại siêu dữ liệu', removedText: 'JPEG mới không sao chép GPS, mẫu máy ảnh, tác giả hoặc EXIF gốc.', protected: 'Bảo vệ ảnh gốc', protectedText: 'PureHub tạo tệp riêng, không ghi đè hoặc xóa ảnh nguồn.', control: 'Bạn kiểm soát chia sẻ', controlText: 'Không có dữ liệu nào rời trình duyệt trước khi bạn tự tải và chia sẻ.' },
  zh: { choose: '选择照片，原图不会被更改。', selected: '已选择', ready: '份隐私清理副本已就绪', failed: '浏览器无法解码所选图片，请尝试 JPEG、PNG 或 WebP。', eyebrow: '隐私图片工具', title: '照片隐私', description: '创建干净的 JPEG 副本，不复制原始 GPS、相机、作者或 EXIF 数据。', workspace: '私密照片工作区', preview: '所选照片预览', choosePhoto: '选择照片', local: '仅在此浏览器中处理', removing: '正在移除元数据…', clean: '清理照片', download: '下载干净 JPEG', downloads: '在下方下载副本', review: '检查后下载', share: '分享隐私流程', changes: '变化说明', boundary: '分享前建立清晰的隐私边界。', before: '处理前：原图及其元数据。处理后：不含位置、相机、作者或 EXIF 的新 JPEG。', removed: '元数据已移除', removedText: '新 JPEG 不复制 GPS、相机型号、作者或原始 EXIF。', protected: '保护原图', protectedText: 'PureHub 创建独立文件，绝不覆盖或删除源照片。', control: '分享由您控制', controlText: '只有您下载并分享后，文件才会离开浏览器。' },
  es: { choose: 'Elige fotos. Los originales nunca se modificarán.', selected: 'seleccionadas', ready: 'copias limpias listas', failed: 'El navegador no pudo leer la imagen. Prueba JPEG, PNG o WebP.', eyebrow: 'Utilidad multimedia privada', title: 'Privacidad de fotos', description: 'Crea una copia JPEG limpia sin GPS, cámara, autor ni EXIF originales.', workspace: 'Espacio privado de fotos', preview: 'Vista previa de la foto', choosePhoto: 'Elegir fotos', local: 'Procesado localmente en este navegador', removing: 'Eliminando metadatos…', clean: 'Limpiar fotos', download: 'Descargar JPEG limpio', downloads: 'Descargar copias abajo', review: 'Descargar tras revisar', share: 'Compartir flujo privado', changes: 'Qué cambia', boundary: 'Un límite de privacidad claro antes de compartir.', before: 'Antes: foto original y metadatos. Después: un JPEG nuevo sin ubicación, cámara, autor ni EXIF.', removed: 'Metadatos eliminados', removedText: 'El JPEG nuevo no copia GPS, modelo de cámara, autor ni EXIF original.', protected: 'Original protegido', protectedText: 'PureHub crea otro archivo y nunca sobrescribe ni borra la foto original.', control: 'Tú controlas el uso compartido', controlText: 'Nada sale del navegador hasta que descargas y compartes la copia.' },
}

function size(value: number) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), 3)
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

export default function PhotoPrivacySurface() {
  const copy = PHOTO_COPY[normalizeLocale(window.location.pathname.split('/')[1])]
  const [files, setFiles] = useState<File[]>([])
  const [preview, setPreview] = useState('')
  const [results, setResults] = useState<Array<{ url: string; size: number; name: string; source: string; originalSize: number }>>([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(copy.choose)

  const select = (next: File[]) => {
    if (preview) URL.revokeObjectURL(preview)
    results.forEach((result) => URL.revokeObjectURL(result.url))
    const selected = next.slice(0, 20)
    setFiles(selected)
    setPreview(selected[0] ? URL.createObjectURL(selected[0]) : '')
    setResults([])
    setMessage(selected.length ? `${selected.length} ${copy.selected} · ${size(selected.reduce((sum, file) => sum + file.size, 0))}` : copy.choose)
  }

  const clean = async () => {
    if (!files.length) return
    setBusy(true)
    try {
      results.forEach((result) => URL.revokeObjectURL(result.url))
      const cleaned = []
      for (const file of files) {
        const bitmap = await createImageBitmap(file)
        const scale = Math.min(1, 4096 / Math.max(bitmap.width, bitmap.height))
        const canvas = document.createElement('canvas')
        canvas.width = Math.max(1, Math.round(bitmap.width * scale))
        canvas.height = Math.max(1, Math.round(bitmap.height * scale))
        canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
        const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', .92))
        bitmap.close()
        if (!blob) throw new Error('Export failed')
        cleaned.push({ url: URL.createObjectURL(blob), size: blob.size, name: `${file.name.replace(/\.[^.]+$/, '')}-privacy-clean.jpg`, source: file.name, originalSize: file.size })
      }
      setResults(cleaned)
      setMessage(`${cleaned.length} ${copy.ready} · ${size(cleaned.reduce((sum, item) => sum + item.size, 0))}`)
      markToolSuccess('photo-privacy', {
        headline: cleaned.length === 1 ? 'Private copy ready' : `${cleaned.length} private copies ready`,
        detail: 'New JPEG copies were created locally without copying GPS, camera, author, or other original EXIF metadata.',
        shareText: `I created ${cleaned.length} separate, metadata-free photo ${cleaned.length === 1 ? 'copy' : 'copies'} locally before sharing.`,
      })
    } catch {
      setMessage(copy.failed)
    } finally {
      setBusy(false)
    }
  }

  return <div className="space-y-4">
    <FlagshipHero eyebrow={copy.eyebrow} title={copy.title} description={copy.description} accent="violet" />
    <div className="grid gap-4 lg:grid-cols-[1.1fr_.9fr]">
      <Panel title={copy.workspace} subtitle={message}>
        <label className="relative grid min-h-72 cursor-pointer place-items-center overflow-hidden rounded-[24px] border border-dashed border-violet-300 bg-violet-50/50 text-center dark:border-violet-800 dark:bg-violet-950/20">
          {preview ? <img src={preview} alt={copy.preview} className="absolute inset-0 size-full object-contain" /> : <div className="p-8"><ImagePlus className="mx-auto size-10 text-violet-600" /><strong className="mt-3 block">{copy.choosePhoto}</strong><span className="mt-1 block text-xs text-slate-500">{copy.local}</span></div>}
          <input type="file" multiple accept="image/jpeg,image/png,image/webp,image/*" className="sr-only" onChange={(event) => select(Array.from(event.target.files ?? []))} />
        </label>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <ActionButton disabled={!files.length || busy} onClick={() => void clean()}><ShieldCheck className="size-4" />{busy ? copy.removing : copy.clean}</ActionButton>
          {results.length === 1 ? <a href={results[0].url} download={results[0].name} className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-bold dark:border-slate-700"><Download className="size-4" />{copy.download}</a> : <ActionButton disabled tone="muted"><Download className="size-4" />{results.length ? copy.downloads : copy.review}</ActionButton>}
        </div>
        {results.length > 1 ? <div className="mt-3 max-h-64 space-y-2 overflow-auto">{results.map((result) => <a key={result.name} href={result.url} download={result.name} className="flex min-h-11 items-center justify-between gap-3 rounded-xl border border-slate-200 px-3 text-xs font-bold dark:border-slate-700"><span className="min-w-0 truncate">{result.source}<small className="ml-1 text-slate-500">{size(result.originalSize)} → {size(result.size)}</small></span><Download className="size-4 shrink-0" /></a>)}</div> : null}
        {results.length ? <ActionButton tone="muted" className="mt-2 w-full" onClick={() => { void shareCard({ title: copy.title, headline: `${results.length} ${copy.ready}`, detail: copy.removedText }); void trackProductEvent('photo-privacy', 'share') }}><Share2 className="size-4" />{copy.share}</ActionButton> : null}
      </Panel>
      <Panel title={copy.changes} subtitle={copy.boundary}>
        <div className="mb-3 rounded-2xl bg-violet-50 p-3 text-xs leading-5 text-violet-950 dark:bg-violet-950/30 dark:text-violet-100">{copy.before}</div>
        <div className="space-y-3">
          <Info icon={<MapPinOff className="size-5" />} title={copy.removed} text={copy.removedText} />
          <Info icon={<ShieldCheck className="size-5" />} title={copy.protected} text={copy.protectedText} />
          <Info icon={<Download className="size-5" />} title={copy.control} text={copy.controlText} />
        </div>
      </Panel>
    </div>
  </div>
}

function Info({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="rounded-2xl border border-slate-200 p-4 dark:border-slate-700"><div className="text-violet-600 dark:text-violet-300">{icon}</div><strong className="mt-2 block text-sm">{title}</strong><p className="mt-1 text-xs leading-5 text-slate-500">{text}</p></div>
}

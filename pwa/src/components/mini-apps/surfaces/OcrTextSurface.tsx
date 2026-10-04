import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import jsQR from 'jsqr'
import {
  Camera, Clipboard, Download, FileImage, FileText, History, ImagePlus,
  LoaderCircle, LockKeyhole, Mail, Phone, RotateCw, ScanText, Search, Share2,
  ShieldCheck, Sparkles, Trash2,
} from 'lucide-react'
import { expenseRepository, ocrDocumentRepository, type ExpenseRecord, type OcrDocumentRecord, type OcrStoredPage, type OcrWord } from '../../../lib/db/purehub-db'
import { parseReceiptText } from '../../../lib/receipt-ocr'
import { ActionButton, FormInput, FormTextArea } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { trackProductEvent } from '../../../lib/community-api'
import { detectDocumentQuad, warpDocument } from '../../../lib/document-perspective'
import { normalizeLocale } from '../../../i18n/locales'

const OCR_UI = {
  en: { title: 'OCR Studio', private: 'PRIVATE BY DESIGN', description: 'Scan, clean, and export text without uploading documents.', device: 'On-device', scan: 'Scan', text: 'Text', library: 'Library', frame: 'Frame one page at a time', noUpload: 'Nothing is uploaded by PureHub.', local: 'Local processing', reading: 'Reading…', capture: 'Capture page', image: 'Scan image', rotate: 'Rotate', cleanup: 'Document cleanup', edges: 'Automatic edges and perspective correction run when the boundary is clear.', language: 'Recognition language', ready: 'ready offline', download: 'first download', cloud: 'Cloud Assist (optional)', cloudHint: 'Use your own OCR endpoint for difficult pages. Upload only occurs when enabled.', enabled: 'Enabled', offline: 'Offline only' },
  vi: { title: 'Xưởng OCR', private: 'XỬ LÝ RIÊNG TƯ', description: 'Quét, làm sạch và xuất văn bản mà không tải tài liệu lên.', device: 'Trên thiết bị', scan: 'Quét', text: 'Văn bản', library: 'Thư viện', frame: 'Căn từng trang vào khung', noUpload: 'PureHub không tải tài liệu lên.', local: 'Xử lý cục bộ', reading: 'Đang đọc…', capture: 'Chụp trang', image: 'Chọn ảnh', rotate: 'Xoay', cleanup: 'Làm sạch tài liệu', edges: 'Tự tìm cạnh và chỉnh phối cảnh khi đường biên đủ rõ.', language: 'Ngôn ngữ nhận dạng', ready: 'sẵn sàng ngoại tuyến', download: 'tải lần đầu', cloud: 'Hỗ trợ đám mây (tùy chọn)', cloudHint: 'Dùng máy chủ OCR của bạn cho trang khó. Chỉ tải lên khi bật.', enabled: 'Đã bật', offline: 'Chỉ ngoại tuyến' },
  zh: { title: 'OCR 文字识别', private: '隐私设计', description: '扫描、清理并导出文字，无需上传文档。', device: '设备端处理', scan: '扫描', text: '文字', library: '资料库', frame: '每次对准一页', noUpload: 'PureHub 不会上传文档。', local: '本地处理', reading: '正在识别…', capture: '拍摄页面', image: '选择图片', rotate: '旋转', cleanup: '文档清理', edges: '边界清晰时自动检测页面边缘并校正透视。', language: '识别语言', ready: '离线包已就绪', download: '首次下载', cloud: '云端辅助（可选）', cloudHint: '困难页面可使用您自己的 OCR 服务，仅在开启后上传。', enabled: '已开启', offline: '仅离线' },
  es: { title: 'Estudio OCR', private: 'PRIVADO POR DISEÑO', description: 'Escanea, limpia y exporta texto sin subir tus documentos.', device: 'En el dispositivo', scan: 'Escanear', text: 'Texto', library: 'Biblioteca', frame: 'Encuadra una página cada vez', noUpload: 'PureHub no sube ningún documento.', local: 'Proceso local', reading: 'Leyendo…', capture: 'Capturar página', image: 'Elegir imagen', rotate: 'Girar', cleanup: 'Limpieza del documento', edges: 'Los bordes y la perspectiva se corrigen cuando el límite es claro.', language: 'Idioma de reconocimiento', ready: 'listo sin conexión', download: 'primera descarga', cloud: 'Asistencia en la nube (opcional)', cloudHint: 'Usa tu propio servidor OCR para páginas difíciles. Solo se sube al activarlo.', enabled: 'Activado', offline: 'Solo sin conexión' },
} as const

const OCR_DEEP = {
  en: { sections: 'OCR Studio sections', document: 'Document', receipt: 'Receipt', note: 'Note', original: 'Original', clean: 'Clean', bw: 'B&W', edgeCrop: 'Edge crop', endpoint: 'OCR endpoint', endpointHint: 'The endpoint should accept image, language and mode, then return text and confidence.', progress: 'OCR pack and recognition', page: 'page', pages: 'pages', words: 'words', confidence: 'OCR confidence', imageQuality: 'image quality', previous: 'Previous', next: 'Next', moveEarlier: 'Move earlier', moveLater: 'Move later', documentTitle: 'Document title', recognizedText: 'Recognized text', quick: 'Quick actions', openLink: 'Open link', call: 'Call', contactDetected: 'Contact card detected', contact: 'Contact', exportContact: 'Export contact card', codeDetected: 'QR / barcode detected', copyCode: 'Copy code', receiptDetected: 'Receipt detected', total: 'Total', totalReview: 'Total needs review', saveMoney: 'Save to Money Studio', tableDetected: 'Table detected', rows: 'rows', columns: 'columns', reviewExport: 'Review before exporting.', formDetected: 'Form fields detected', fieldsFound: 'structured fields found.', exportLayout: 'Export OCR layout JSON', copy: 'Copy', share: 'Share', pdfPassword: 'PDF password (optional, 8+ characters)', pdfPlaceholder: 'Leave empty for a normal PDF', save: 'Save', continuePdf: 'Continue in Doc to PDF', addPage: 'Add page', deletePage: 'Delete page', newDocument: 'New document', noText: 'No text yet', noTextHint: 'Capture a page or choose an image to begin.', start: 'Start scanning', privateLibrary: 'Private library', libraryHint: 'Searchable and stored only in this browser.', clearLibrary: 'Clear library', search: 'Search scans' },
  vi: { sections: 'Các mục OCR', document: 'Tài liệu', receipt: 'Hóa đơn', note: 'Ghi chú', original: 'Gốc', clean: 'Sạch', bw: 'Đen trắng', edgeCrop: 'Cắt viền', endpoint: 'Máy chủ OCR', endpointHint: 'Máy chủ nhận ảnh, ngôn ngữ và chế độ rồi trả về văn bản cùng độ tin cậy.', progress: 'Gói OCR và nhận dạng', page: 'trang', pages: 'trang', words: 'từ', confidence: 'độ tin cậy OCR', imageQuality: 'chất lượng ảnh', previous: 'Trước', next: 'Sau', moveEarlier: 'Chuyển lên', moveLater: 'Chuyển xuống', documentTitle: 'Tên tài liệu', recognizedText: 'Văn bản nhận dạng', quick: 'Thao tác nhanh', openLink: 'Mở liên kết', call: 'Gọi', contactDetected: 'Đã nhận dạng danh thiếp', contact: 'Liên hệ', exportContact: 'Xuất danh thiếp', codeDetected: 'Đã nhận dạng QR / mã vạch', copyCode: 'Sao chép mã', receiptDetected: 'Đã nhận dạng hóa đơn', total: 'Tổng', totalReview: 'Cần kiểm tra tổng tiền', saveMoney: 'Lưu vào Sổ chi tiêu', tableDetected: 'Đã nhận dạng bảng', rows: 'hàng', columns: 'cột', reviewExport: 'Hãy kiểm tra trước khi xuất.', formDetected: 'Đã nhận dạng trường biểu mẫu', fieldsFound: 'trường có cấu trúc được tìm thấy.', exportLayout: 'Xuất bố cục OCR JSON', copy: 'Sao chép', share: 'Chia sẻ', pdfPassword: 'Mật khẩu PDF (không bắt buộc, từ 8 ký tự)', pdfPlaceholder: 'Để trống nếu dùng PDF thường', save: 'Lưu', continuePdf: 'Tiếp tục trong Tài liệu sang PDF', addPage: 'Thêm trang', deletePage: 'Xóa trang', newDocument: 'Tài liệu mới', noText: 'Chưa có văn bản', noTextHint: 'Chụp trang hoặc chọn ảnh để bắt đầu.', start: 'Bắt đầu quét', privateLibrary: 'Thư viện riêng tư', libraryHint: 'Có thể tìm kiếm và chỉ lưu trong trình duyệt này.', clearLibrary: 'Xóa thư viện', search: 'Tìm bản quét' },
  zh: { sections: 'OCR 栏目', document: '文档', receipt: '收据', note: '笔记', original: '原图', clean: '清晰', bw: '黑白', edgeCrop: '边缘裁剪', endpoint: 'OCR 服务地址', endpointHint: '服务应接收图片、语言和模式，并返回文字与置信度。', progress: 'OCR 包与识别进度', page: '页', pages: '页', words: '个词', confidence: 'OCR 置信度', imageQuality: '图像质量', previous: '上一页', next: '下一页', moveEarlier: '前移', moveLater: '后移', documentTitle: '文档标题', recognizedText: '识别文字', quick: '快捷操作', openLink: '打开链接', call: '拨打', contactDetected: '检测到联系人卡片', contact: '联系人', exportContact: '导出联系人卡片', codeDetected: '检测到二维码 / 条码', copyCode: '复制代码', receiptDetected: '检测到收据', total: '总计', totalReview: '总额需要检查', saveMoney: '保存到账本', tableDetected: '检测到表格', rows: '行', columns: '列', reviewExport: '导出前请检查。', formDetected: '检测到表单字段', fieldsFound: '个结构化字段。', exportLayout: '导出 OCR 布局 JSON', copy: '复制', share: '分享', pdfPassword: 'PDF 密码（可选，至少 8 个字符）', pdfPlaceholder: '普通 PDF 请留空', save: '保存', continuePdf: '继续到文档转 PDF', addPage: '添加页面', deletePage: '删除页面', newDocument: '新建文档', noText: '暂无文字', noTextHint: '拍摄页面或选择图片即可开始。', start: '开始扫描', privateLibrary: '私密资料库', libraryHint: '可搜索且仅保存在此浏览器中。', clearLibrary: '清空资料库', search: '搜索扫描' },
  es: { sections: 'Secciones de OCR', document: 'Documento', receipt: 'Recibo', note: 'Nota', original: 'Original', clean: 'Limpio', bw: 'B/N', edgeCrop: 'Recorte de bordes', endpoint: 'Servidor OCR', endpointHint: 'El servidor debe recibir imagen, idioma y modo, y devolver texto y confianza.', progress: 'Paquete OCR y reconocimiento', page: 'página', pages: 'páginas', words: 'palabras', confidence: 'confianza OCR', imageQuality: 'calidad de imagen', previous: 'Anterior', next: 'Siguiente', moveEarlier: 'Mover antes', moveLater: 'Mover después', documentTitle: 'Título del documento', recognizedText: 'Texto reconocido', quick: 'Acciones rápidas', openLink: 'Abrir enlace', call: 'Llamar', contactDetected: 'Tarjeta de contacto detectada', contact: 'Contacto', exportContact: 'Exportar contacto', codeDetected: 'QR / código de barras detectado', copyCode: 'Copiar código', receiptDetected: 'Recibo detectado', total: 'Total', totalReview: 'El total necesita revisión', saveMoney: 'Guardar en Registro de gastos', tableDetected: 'Tabla detectada', rows: 'filas', columns: 'columnas', reviewExport: 'Revisa antes de exportar.', formDetected: 'Campos de formulario detectados', fieldsFound: 'campos estructurados encontrados.', exportLayout: 'Exportar diseño OCR JSON', copy: 'Copiar', share: 'Compartir', pdfPassword: 'Contraseña PDF (opcional, 8+ caracteres)', pdfPlaceholder: 'Déjalo vacío para un PDF normal', save: 'Guardar', continuePdf: 'Continuar en Documento a PDF', addPage: 'Añadir página', deletePage: 'Eliminar página', newDocument: 'Documento nuevo', noText: 'Aún no hay texto', noTextHint: 'Captura una página o elige una imagen para empezar.', start: 'Empezar a escanear', privateLibrary: 'Biblioteca privada', libraryHint: 'Se puede buscar y solo se guarda en este navegador.', clearLibrary: 'Vaciar biblioteca', search: 'Buscar escaneos' },
} as const

const OCR_QUALITY = {
  en: { retake: 'Retake suggestion', brightness: 'Brightness', contrast: 'contrast', sharpness: 'sharpness', latestPage: 'Latest scanned page', scannedPage: 'Scanned page', unavailable: 'Image analysis unavailable', dark: 'too dark', bright: 'overexposed', lowContrast: 'low contrast', blurred: 'blurred or out of focus', soft: 'slightly soft focus', resolution: 'low resolution' },
  vi: { retake: 'Gợi ý chụp lại', brightness: 'Độ sáng', contrast: 'độ tương phản', sharpness: 'độ nét', latestPage: 'Trang quét mới nhất', scannedPage: 'Trang đã quét', unavailable: 'Không thể phân tích ảnh', dark: 'ảnh quá tối', bright: 'ảnh bị cháy sáng', lowContrast: 'độ tương phản thấp', blurred: 'ảnh mờ hoặc mất nét', soft: 'ảnh hơi thiếu nét', resolution: 'độ phân giải thấp' },
  zh: { retake: '重拍建议', brightness: '亮度', contrast: '对比度', sharpness: '清晰度', latestPage: '最新扫描页面', scannedPage: '已扫描页面', unavailable: '无法分析图片', dark: '图片过暗', bright: '图片过曝', lowContrast: '对比度过低', blurred: '图片模糊或失焦', soft: '图片稍显模糊', resolution: '分辨率过低' },
  es: { retake: 'Sugerencia para repetir', brightness: 'Brillo', contrast: 'contraste', sharpness: 'nitidez', latestPage: 'Última página escaneada', scannedPage: 'Página escaneada', unavailable: 'No se pudo analizar la imagen', dark: 'imagen demasiado oscura', bright: 'imagen sobreexpuesta', lowContrast: 'contraste bajo', blurred: 'imagen borrosa o desenfocada', soft: 'enfoque algo suave', resolution: 'resolución baja' },
} as const

const OCR_EMAIL = { en: 'Email', vi: 'Email', zh: '邮件', es: 'Correo' } as const

function localizedQualityIssue(issue: string, locale: keyof typeof OCR_QUALITY) {
  const labels: Record<string, keyof typeof OCR_QUALITY.en> = {
    'Image analysis unavailable': 'unavailable',
    'too dark': 'dark',
    overexposed: 'bright',
    'low contrast': 'lowContrast',
    'blurred or out of focus': 'blurred',
    'slightly soft focus': 'soft',
    'low resolution': 'resolution',
  }
  const key = labels[issue]
  return key ? OCR_QUALITY[locale][key] : issue
}

const OCR_STATUS = {
  en: { title: 'My scan', ready: 'Ready. Capture a page or choose an image.', limit: 'A scan can contain up to 20 pages. Export it before starting another.', cloudFail: 'Cloud Assist failed; continuing with on-device OCR…', noText: 'No readable text found. Try better light or a tighter crop.', failed: 'OCR could not finish. The selected language pack may need its first download.', receipt: 'Receipt saved to Money Studio.', saved: 'Saved to your private OCR library.', reset: 'Ready for a new document.' },
  vi: { title: 'Bản quét của tôi', ready: 'Sẵn sàng. Hãy chụp trang hoặc chọn ảnh.', limit: 'Mỗi bản quét tối đa 20 trang. Hãy xuất tài liệu trước khi tạo bản mới.', cloudFail: 'Hỗ trợ đám mây thất bại; đang tiếp tục OCR trên thiết bị…', noText: 'Không tìm thấy văn bản rõ. Hãy tăng ánh sáng hoặc cắt sát hơn.', failed: 'OCR chưa hoàn tất. Gói ngôn ngữ có thể cần tải lần đầu.', receipt: 'Đã lưu hóa đơn vào Money Studio.', saved: 'Đã lưu vào thư viện OCR riêng tư.', reset: 'Sẵn sàng cho tài liệu mới.' },
  zh: { title: '我的扫描', ready: '准备就绪。请拍摄页面或选择图片。', limit: '一次扫描最多 20 页，请先导出当前文档。', cloudFail: '云端辅助失败，正在继续设备端 OCR…', noText: '未找到清晰文字，请改善光线或缩小裁剪范围。', failed: 'OCR 未能完成，所选语言包可能需要首次下载。', receipt: '收据已保存到 Money Studio。', saved: '已保存到私密 OCR 资料库。', reset: '已准备好扫描新文档。' },
  es: { title: 'Mi escaneo', ready: 'Listo. Captura una página o elige una imagen.', limit: 'Un escaneo admite hasta 20 páginas. Expórtalo antes de empezar otro.', cloudFail: 'Falló la asistencia en la nube; se continúa con OCR local…', noText: 'No se encontró texto legible. Mejora la luz o recorta más cerca.', failed: 'El OCR no pudo terminar. Puede que el paquete de idioma necesite su primera descarga.', receipt: 'Recibo guardado en Money Studio.', saved: 'Guardado en tu biblioteca OCR privada.', reset: 'Listo para un documento nuevo.' },
} as const

const OCR_DYNAMIC = {
  en: { preparing: (mode: string) => `Preparing ${mode.toLowerCase()} locally...`, pack: (name: string) => `Loading the ${name} OCR pack...`, recognizing: (page: number, total: number) => `Recognizing image ${page}/${total} on this device...`, corrected: (page: number, total: number) => `Detected page edges and corrected perspective for image ${page}/${total}...`, cloud: (page: number, total: number) => `Cloud Assist processing image ${page}/${total}...`, optimizing: (page: number, total: number) => `Optimizing difficult image ${page}/${total}...`, result: (read: number, total: number, low: number) => low ? `${read}/${total} page(s) read. ${low} page(s) may need better light or a retake.` : `Batch OCR finished: ${read}/${total} readable page(s). Review before export.`, opened: 'Opened with original scan pages from your private library.', legacy: 'Opened text-only legacy library item.', noMatch: 'No matching document.', empty: 'Saved OCR documents will appear here.', imagesSaved: 'images saved', delete: 'Delete' },
  vi: { preparing: (mode: string) => `Đang chuẩn bị chế độ ${mode.toLowerCase()} trên thiết bị...`, pack: (name: string) => `Đang tải gói OCR ${name}...`, recognizing: (page: number, total: number) => `Đang nhận dạng ảnh ${page}/${total} trên thiết bị...`, corrected: (page: number, total: number) => `Đã tìm cạnh và chỉnh phối cảnh ảnh ${page}/${total}...`, cloud: (page: number, total: number) => `Hỗ trợ đám mây đang xử lý ảnh ${page}/${total}...`, optimizing: (page: number, total: number) => `Đang tối ưu ảnh khó ${page}/${total}...`, result: (read: number, total: number, low: number) => low ? `Đã đọc ${read}/${total} trang. ${low} trang có thể cần thêm sáng hoặc chụp lại.` : `Đã OCR xong ${read}/${total} trang. Hãy kiểm tra trước khi xuất.`, opened: 'Đã mở các trang quét gốc từ thư viện riêng tư.', legacy: 'Đã mở mục thư viện cũ chỉ có văn bản.', noMatch: 'Không có tài liệu phù hợp.', empty: 'Tài liệu OCR đã lưu sẽ xuất hiện tại đây.', imagesSaved: 'đã lưu ảnh', delete: 'Xóa' },
  zh: { preparing: (mode: string) => `正在本机准备${mode}模式...`, pack: (name: string) => `正在加载 ${name} OCR 包...`, recognizing: (page: number, total: number) => `正在设备上识别图片 ${page}/${total}...`, corrected: (page: number, total: number) => `已检测边缘并校正图片 ${page}/${total} 的透视...`, cloud: (page: number, total: number) => `云端辅助正在处理图片 ${page}/${total}...`, optimizing: (page: number, total: number) => `正在优化困难图片 ${page}/${total}...`, result: (read: number, total: number, low: number) => low ? `已读取 ${read}/${total} 页，其中 ${low} 页可能需要更好光线或重新拍摄。` : `批量 OCR 已完成：${read}/${total} 页可读。导出前请检查。`, opened: '已从私密资料库打开原始扫描页面。', legacy: '已打开仅含文字的旧资料库项目。', noMatch: '没有匹配的文档。', empty: '保存的 OCR 文档会显示在这里。', imagesSaved: '图片已保存', delete: '删除' },
  es: { preparing: (mode: string) => `Preparando ${mode.toLowerCase()} localmente...`, pack: (name: string) => `Cargando el paquete OCR ${name}...`, recognizing: (page: number, total: number) => `Reconociendo imagen ${page}/${total} en este dispositivo...`, corrected: (page: number, total: number) => `Bordes detectados y perspectiva corregida en la imagen ${page}/${total}...`, cloud: (page: number, total: number) => `La asistencia en la nube procesa la imagen ${page}/${total}...`, optimizing: (page: number, total: number) => `Optimizando imagen difícil ${page}/${total}...`, result: (read: number, total: number, low: number) => low ? `Se leyeron ${read}/${total} página(s). ${low} pueden necesitar más luz o repetirse.` : `OCR múltiple terminado: ${read}/${total} página(s) legibles. Revisa antes de exportar.`, opened: 'Se abrieron las páginas originales desde tu biblioteca privada.', legacy: 'Se abrió un elemento antiguo que solo contiene texto.', noMatch: 'No hay documentos coincidentes.', empty: 'Los documentos OCR guardados aparecerán aquí.', imagesSaved: 'imágenes guardadas', delete: 'Eliminar' },
} as const

const OCR_LANGUAGES = [
  { code: 'eng+vie', label: 'English + Vietnamese' },
  { code: 'eng+chi_sim', label: 'English + Chinese' },
  { code: 'eng+spa', label: 'English + Spanish' },
  { code: 'eng', label: 'English' },
  { code: 'vie', label: 'Tiếng Việt' },
  { code: 'chi_sim', label: '简体中文' },
  { code: 'spa', label: 'Español' },
] as const
const OCR_PACK_CACHE_KEY = 'purehub.ocr.cached-languages.v1'
const DOCUMENT_HANDOFF_KEY = 'purehub.document-suite.ocr-handoff.v1'
const OCR_CLOUD_ENDPOINT_KEY = 'purehub.ocr.cloud-endpoint.v1'

function languageLabel(code: string, fallback: string, locale: keyof typeof OCR_DEEP) {
  const names = {
    en: { eng: 'English', vie: 'Vietnamese', chi_sim: 'Simplified Chinese', spa: 'Spanish', 'eng+vie': 'English + Vietnamese', 'eng+chi_sim': 'English + Chinese', 'eng+spa': 'English + Spanish' },
    vi: { eng: 'Tiếng Anh', vie: 'Tiếng Việt', chi_sim: 'Tiếng Trung giản thể', spa: 'Tiếng Tây Ban Nha', 'eng+vie': 'Tiếng Anh + Tiếng Việt', 'eng+chi_sim': 'Tiếng Anh + Tiếng Trung', 'eng+spa': 'Tiếng Anh + Tiếng Tây Ban Nha' },
    zh: { eng: '英语', vie: '越南语', chi_sim: '简体中文', spa: '西班牙语', 'eng+vie': '英语 + 越南语', 'eng+chi_sim': '英语 + 中文', 'eng+spa': '英语 + 西班牙语' },
    es: { eng: 'Inglés', vie: 'Vietnamita', chi_sim: 'Chino simplificado', spa: 'Español', 'eng+vie': 'Inglés + vietnamita', 'eng+chi_sim': 'Inglés + chino', 'eng+spa': 'Inglés + español' },
  } as const
  return names[locale][code as keyof typeof names.en] ?? fallback
}

function preferredOcrLanguage() {
  if (typeof window === 'undefined') return 'eng+vie' as const
  const locale = `${window.location.pathname} ${window.navigator.languages?.join(' ') ?? window.navigator.language}`.toLowerCase()
  if (/(^|[\s/])zh(?:[-_/\s]|$)|chi_sim/.test(locale)) return 'chi_sim' as const
  if (/(^|[\s/])es(?:[-_/\s]|$)|spa/.test(locale)) return 'spa' as const
  if (/(^|[\s/])vi(?:[-_/\s]|$)|vie/.test(locale)) return 'eng+vie' as const
  return 'eng' as const
}

type Tab = 'scan' | 'text' | 'library'
type ScanMode = 'Document' | 'Receipt' | 'Note'
type ImageFilter = 'Original' | 'Clean' | 'B&W'
type ImageQuality = { score: number; issues: string[]; brightness?: number; contrast?: number; sharpness?: number }
type OcrPage = { id: string; text: string; previewUrl: string; source: string; confidence: number; quality?: ImageQuality; words?: OcrWord[] }

async function blobUrlToDataUrl(url: string) {
  const blob = await (await fetch(url)).blob()
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

async function assessImageQuality(blob: Blob): Promise<ImageQuality> {
  const url = URL.createObjectURL(blob)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image()
      node.onload = () => resolve(node)
      node.onerror = reject
      node.src = url
    })
    const width = Math.min(160, image.naturalWidth)
    const height = Math.max(1, Math.round(image.naturalHeight * width / Math.max(1, image.naturalWidth)))
    const canvas = document.createElement('canvas')
    canvas.width = width; canvas.height = height
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return { score: 0, issues: ['Image analysis unavailable'] }
    context.drawImage(image, 0, 0, width, height)
    const pixels = context.getImageData(0, 0, width, height).data
    const luminance = new Float32Array(width * height)
    let total = 0; let squared = 0
    for (let index = 0; index < pixels.length; index += 4) {
      const value = (pixels[index] * 30 + pixels[index + 1] * 59 + pixels[index + 2] * 11) / 100
      luminance[index / 4] = value
      total += value; squared += value * value
    }
    const count = Math.max(1, pixels.length / 4)
    const mean = total / count
    const contrast = Math.sqrt(Math.max(0, squared / count - mean * mean))
    let laplacianSquared = 0; let laplacianCount = 0
    for (let y = 1; y < height - 1; y += 1) for (let x = 1; x < width - 1; x += 1) {
      const offset = y * width + x
      const edge = 4 * luminance[offset] - luminance[offset - 1] - luminance[offset + 1] - luminance[offset - width] - luminance[offset + width]
      laplacianSquared += edge * edge
      laplacianCount += 1
    }
    const sharpness = Math.sqrt(laplacianSquared / Math.max(1, laplacianCount))
    const issues: string[] = []
    let score = 100
    if (mean < 48) { score -= 35; issues.push('too dark') }
    if (mean > 224) { score -= 20; issues.push('overexposed') }
    if (contrast < 22) { score -= 30; issues.push('low contrast') }
    if (sharpness < 12) { score -= 35; issues.push('blurred or out of focus') }
    else if (sharpness < 20) { score -= 15; issues.push('slightly soft focus') }
    if (Math.max(image.naturalWidth, image.naturalHeight) < 900) { score -= 20; issues.push('low resolution') }
    return {
      score: Math.max(0, score), issues,
      brightness: Math.round(mean), contrast: Math.round(contrast), sharpness: Math.round(sharpness),
    }
  } finally {
    URL.revokeObjectURL(url)
  }
}

async function estimateAutoCrop(blob: Blob) {
  const url = URL.createObjectURL(blob)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image(); node.onload = () => resolve(node); node.onerror = reject; node.src = url
    })
    const size = 96
    const canvas = document.createElement('canvas'); canvas.width = size; canvas.height = size
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return 0
    context.drawImage(image, 0, 0, size, size)
    const pixels = context.getImageData(0, 0, size, size).data
    const luminanceAt = (x: number, y: number) => {
      const offset = (y * size + x) * 4
      return (pixels[offset] * 30 + pixels[offset + 1] * 59 + pixels[offset + 2] * 11) / 100
    }
    const corners = [luminanceAt(2, 2), luminanceAt(size - 3, 2), luminanceAt(2, size - 3), luminanceAt(size - 3, size - 3)]
    if (corners.reduce((sum, value) => sum + value, 0) / corners.length < 220) return 0
    let left = size; let top = size; let right = -1; let bottom = -1
    for (let y = 0; y < size; y += 1) for (let x = 0; x < size; x += 1) {
      if (luminanceAt(x, y) < 238) { left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y) }
    }
    if (right < 0 || right - left < size * .45 || bottom - top < size * .45) return 0
    const margin = Math.min(left, top, size - right - 1, size - bottom - 1) / size
    return Math.min(.12, Math.max(0, margin - .015))
  } finally { URL.revokeObjectURL(url) }
}

async function recognizeWithCloud(endpoint: string, blob: Blob, language: string, mode: ScanMode) {
  const imageData = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
  const response = await fetch(endpoint, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ image_base64: imageData, language, mode }),
  })
  if (!response.ok) throw new Error(`Cloud OCR returned ${response.status}`)
  const payload = await response.json() as { text?: string; confidence?: number; words?: OcrWord[] }
  if (!payload.text?.trim()) throw new Error('Cloud OCR returned no text')
  return { text: payload.text, confidence: Math.round(payload.confidence ?? 0), words: payload.words }
}

function normalizeWords(value: unknown): OcrWord[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    if (!item || typeof item !== 'object') return []
    const row = item as { text?: unknown; bbox?: { x0?: unknown; y0?: unknown; x1?: unknown; y1?: unknown }; confidence?: unknown }
    const box = row.bbox
    if (typeof row.text !== 'string' || !box || typeof box.x0 !== 'number' || typeof box.y0 !== 'number' || typeof box.x1 !== 'number' || typeof box.y1 !== 'number') return []
    return [{ text: row.text, left: box.x0, top: box.y0, width: Math.max(0, box.x1 - box.x0), height: Math.max(0, box.y1 - box.y0), confidence: Math.round(typeof row.confidence === 'number' ? row.confidence : 0) }]
  })
}

function cleanText(value: string, mode: ScanMode) {
  const lines = value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  if (mode === 'Note') return lines.join(' ').replace(/\s+/g, ' ').trim()
  return lines.join('\n').replace(/[ \t]+/g, ' ').trim()
}

function combinedText(pages: OcrPage[], current: string) {
  if (!pages.length) return current.trim()
  const values = pages.map((page) => page.text)
  return values.map((value, index) => values.length > 1 ? `Page ${index + 1}\n${value}` : value).join('\n\n')
}

function extractTableRows(text: string) {
  return text.split(/\r?\n/).map((line) => line.trim()).filter((line) => {
    const cells = line.includes('\t') ? line.split('\t') : line.split(/\s{2,}/)
    return cells.length >= 2 && cells.every((cell) => cell.trim().length > 0)
  }).map((line) => (line.includes('\t') ? line.split('\t') : line.split(/\s{2,}/)).map((cell) => cell.trim()))
}

function extractLayoutTable(words: OcrWord[]) {
  if (words.length < 4) return []
  const ordered = [...words].filter((word) => word.text.trim()).sort((a, b) => a.top - b.top || a.left - b.left)
  const rows: OcrWord[][] = []
  ordered.forEach((word) => {
    const center = word.top + word.height / 2
    const row = rows.at(-1)
    const rowCenter = row?.length ? row.reduce((sum, item) => sum + item.top + item.height / 2, 0) / row.length : -999
    const tolerance = Math.max(8, word.height * 0.7)
    if (!row || Math.abs(center - rowCenter) > tolerance) rows.push([word])
    else row.push(word)
  })
  return rows.map((row) => {
    const cells: Array<{ text: string[]; last: OcrWord }> = []
    row.sort((a, b) => a.left - b.left).forEach((word) => {
      const previous = cells.at(-1)
      const gap = previous ? word.left - (previous.last.left + previous.last.width) : 999
      if (!previous || gap > Math.max(24, word.height * 1.4)) cells.push({ text: [word.text], last: word })
      else { previous.text.push(word.text); previous.last = word }
    })
    return cells.map((cell) => cell.text.join(' '))
  }).filter((row) => row.length >= 2)
}

function downloadCsv(rows: string[][], fileName: string) {
  const csv = rows.map((row) => row.map((cell) => `"${cell.replaceAll('"', '""')}"`).join(',')).join('\n')
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), fileName)
}

function downloadLayout(pages: OcrPage[], title: string) {
  const payload = { title, exportedAt: new Date().toISOString(), pages: pages.map((page, index) => ({ page: index + 1, text: page.text, confidence: page.confidence, quality: page.quality, words: page.words ?? [] })) }
  downloadBlob(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' }), `${safeName(title)}-layout.json`)
}

function extractFields(text: string) {
  return text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).flatMap((line) => {
    const match = line.match(/^(.{2,48}?)\s*[:：]\s*(.+)$/)
    return match ? [{ label: match[1].trim(), value: match[2].trim() }] : []
  })
}

function extractContact(text: string) {
  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/iu)?.[0] ?? ''
  const phone = text.match(/(?:\+?\d[\d .-]{7,}\d)/u)?.[0]?.trim() ?? ''
  const lines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const name = lines.find((line) => line.length >= 2 && line.length <= 64 && !/[\d@]/.test(line) && !/^(tel|phone|mobile|email|e-mail|address|电话|手机|邮箱)/iu.test(line)) ?? ''
  return { name, email, phone }
}

async function detectQrOrBarcode(blob: Blob) {
  try {
    const bitmap = await createImageBitmap(blob)
    const scale = Math.min(1, 1400 / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(bitmap.width * scale)); canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) { bitmap.close(); return '' }
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height); bitmap.close()
    const detector = (globalThis as typeof globalThis & { BarcodeDetector?: new (options?: { formats?: string[] }) => { detect(source: CanvasImageSource): Promise<Array<{ rawValue?: string }>> } }).BarcodeDetector
    if (detector) {
      const [result] = await new detector({ formats: ['qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'data_matrix', 'pdf417'] }).detect(canvas)
      if (result?.rawValue) return result.rawValue
    }
    return jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })?.data ?? ''
  } catch { return '' }
}

async function prepareImage(file: File, rotation: number, crop: number, filter: ImageFilter, autoPerspective: boolean) {
  const sourceUrl = URL.createObjectURL(file)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const node = new Image()
      node.onload = () => resolve(node)
      node.onerror = reject
      node.src = sourceUrl
    })
    const quad = autoPerspective ? await detectDocumentQuad(image).catch(() => null) : null
    const corrected = quad ? warpDocument(image, quad) : null
    const source = corrected ?? image
    const quarterTurn = rotation % 180 !== 0
    const sourceWidth = source instanceof HTMLCanvasElement ? source.width : source.naturalWidth
    const sourceHeight = source instanceof HTMLCanvasElement ? source.height : source.naturalHeight
    const insetX = Math.round(sourceWidth * crop)
    const insetY = Math.round(sourceHeight * crop)
    const croppedWidth = Math.max(1, sourceWidth - insetX * 2)
    const croppedHeight = Math.max(1, sourceHeight - insetY * 2)
    const scale = Math.min(1, 2200 / Math.max(croppedWidth, croppedHeight))
    const targetWidth = Math.max(1, Math.round(croppedWidth * scale))
    const targetHeight = Math.max(1, Math.round(croppedHeight * scale))
    const canvas = document.createElement('canvas')
    canvas.width = quarterTurn ? targetHeight : targetWidth
    canvas.height = quarterTurn ? targetWidth : targetHeight
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) throw new Error('Canvas is unavailable')
    context.translate(canvas.width / 2, canvas.height / 2)
    context.rotate(rotation * Math.PI / 180)
    context.filter = filter === 'B&W' ? 'grayscale(1) contrast(1.3)' : filter === 'Clean' ? 'grayscale(.72) contrast(1.18) brightness(1.05)' : 'none'
    context.drawImage(source, insetX, insetY, croppedWidth, croppedHeight, -targetWidth / 2, -targetHeight / 2, targetWidth, targetHeight)
    const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('Image conversion failed')), 'image/jpeg', .92))
    return { blob, previewUrl: URL.createObjectURL(blob), autoCorrected: Boolean(corrected) }
  } finally {
    URL.revokeObjectURL(sourceUrl)
  }
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 500)
}

function safeName(title: string) {
  return title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'purehub-ocr'
}

export default function OcrTextSurface() {
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const ui = OCR_UI[locale]
  const deep = OCR_DEEP[locale]
  const qualityCopy = OCR_QUALITY[locale]
  const emailCopy = OCR_EMAIL[locale]
  const statusCopy = OCR_STATUS[locale]
  const dynamic = OCR_DYNAMIC[locale]
  const [tab, setTab] = useState<Tab>('scan')
  const [mode, setMode] = useState<ScanMode>('Document')
  const [filter, setFilter] = useState<ImageFilter>('Clean')
  const [language, setLanguage] = useState<(typeof OCR_LANGUAGES)[number]['code']>(preferredOcrLanguage)
  const [rotation, setRotation] = useState(0)
  const [crop, setCrop] = useState(.02)
  const [pages, setPages] = useState<OcrPage[]>([])
  const [selectedPageIndex, setSelectedPageIndex] = useState(-1)
  const [ocrText, setOcrText] = useState('')
  const [title, setTitle] = useState<string>(statusCopy.title)
  const [status, setStatus] = useState<string>(statusCopy.ready)
  const [running, setRunning] = useState(false)
  const [cloudAssist, setCloudAssist] = useState(false)
  const [cloudEndpoint, setCloudEndpoint] = useState(() => localStorage.getItem(OCR_CLOUD_ENDPOINT_KEY) ?? '')
  const [pdfPassword, setPdfPassword] = useState('')
  const [detectedCode, setDetectedCode] = useState('')
  const [packProgress, setPackProgress] = useState(0)
  const [cachedLanguages, setCachedLanguages] = useState<string[]>(() => {
    try { return JSON.parse(localStorage.getItem(OCR_PACK_CACHE_KEY) ?? '[]') as string[] } catch { return [] }
  })
  const [documents, setDocuments] = useState<OcrDocumentRecord[]>([])
  const [query, setQuery] = useState('')
  const cameraInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const pagesRef = useRef<OcrPage[]>([])

  useEffect(() => { void ocrDocumentRepository.list().then(setDocuments) }, [])
  useEffect(() => { pagesRef.current = pages }, [pages])
  useEffect(() => () => pagesRef.current.forEach((page) => URL.revokeObjectURL(page.previewUrl)), [])

  const currentPage = pages[selectedPageIndex] ?? pages.at(-1)
  const currentPreview = currentPage?.previewUrl
  const allText = useMemo(() => combinedText(pages, ocrText), [pages, ocrText])
  const filteredDocuments = useMemo(() => {
    const normalizedQuery = query.trim().normalize('NFKC').toLocaleLowerCase()
    return documents.filter((item) => !normalizedQuery || `${item.title}\n${item.text}`.normalize('NFKC').toLocaleLowerCase().includes(normalizedQuery))
  }, [documents, query])
  const detectedUrl = ocrText.match(/https?:\/\/\S+/i)?.[0]?.replace(/[.,)]$/, '')
  const detectedEmail = ocrText.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0]
  const detectedPhone = ocrText.match(/(?:\+?\d[\d .-]{7,}\d)/)?.[0]
  const detectedContact = useMemo(() => extractContact(ocrText), [ocrText])
  const detectedReceipt = useMemo(() => mode === 'Receipt' && ocrText.trim() ? parseReceiptText(ocrText) : null, [mode, ocrText])
  const detectedTable = useMemo(() => currentPage?.words?.length ? extractLayoutTable(currentPage.words) : extractTableRows(ocrText), [currentPage, ocrText])
  const detectedFields = useMemo(() => extractFields(ocrText), [ocrText])

  const handleFile = async (event: ChangeEvent<HTMLInputElement>, source: string) => {
    const files = Array.from(event.target.files ?? []).slice(0, Math.max(0, 20 - pages.length))
    if (!files.length) return
    if (pages.length >= 20) {
      setStatus(statusCopy.limit)
      event.target.value = ''
      return
    }
    setRunning(true)
    setStatus(dynamic.preparing(mode))
    try {
      const selectedLanguage = OCR_LANGUAGES.find((item) => item.code === language)
      setStatus(dynamic.pack(languageLabel(language, selectedLanguage?.label ?? language, locale)))
      setPackProgress(0)
      const { createWorker, OEM } = await import('tesseract.js')
      const worker = await createWorker(language, OEM.LSTM_ONLY, {
        logger: (message) => {
          if (typeof message.progress === 'number') setPackProgress(Math.round(message.progress * 100))
          if (message.status) setStatus(`${message.status.replaceAll('_', ' ')} ${Math.round((message.progress ?? 0) * 100)}%`)
        },
      })
      try {
        setCachedLanguages((current) => {
          const next = current.includes(language) ? current : [...current, language]
          localStorage.setItem(OCR_PACK_CACHE_KEY, JSON.stringify(next))
          return next
        })
        const nextPages: OcrPage[] = []
        for (let index = 0; index < files.length; index += 1) {
          setStatus(dynamic.recognizing(index + 1, files.length))
          const autoCrop = mode === 'Note' ? await estimateAutoCrop(files[index]) : 0
          let prepared = await prepareImage(files[index], rotation, Math.max(crop, autoCrop), filter, mode !== 'Note')
          if (prepared.autoCorrected) setStatus(dynamic.corrected(index + 1, files.length))
          const code = await detectQrOrBarcode(prepared.blob)
          if (code) setDetectedCode(code)
          let cloudResult: { text: string; confidence: number; words?: OcrWord[] } | null = null
          if (cloudAssist && cloudEndpoint.trim()) {
            try {
              setStatus(dynamic.cloud(index + 1, files.length))
              cloudResult = await recognizeWithCloud(cloudEndpoint.trim(), prepared.blob, language, mode)
            } catch {
              setStatus(statusCopy.cloudFail)
            }
          }
          let recognized: { text: string; confidence: number; words: OcrWord[] }
          if (cloudResult) {
            recognized = { text: cloudResult.text, confidence: cloudResult.confidence, words: cloudResult.words ?? [] }
          } else {
            const initial = await worker.recognize(prepared.blob)
            recognized = {
              text: initial.data.text,
              confidence: Math.round(initial.data.confidence ?? 0),
              words: normalizeWords('words' in initial.data ? initial.data.words : []),
            }
            if (recognized.confidence < 70 && filter !== 'B&W') {
              setStatus(dynamic.optimizing(index + 1, files.length))
              const retryPrepared = await prepareImage(files[index], rotation, Math.max(crop, autoCrop), 'B&W', mode !== 'Note')
              const retry = await worker.recognize(retryPrepared.blob)
              const retryText = cleanText(retry.data.text, mode)
              const retryConfidence = Math.round(retry.data.confidence ?? 0)
              if (retryText && retryConfidence >= recognized.confidence + 3) {
                URL.revokeObjectURL(prepared.previewUrl)
                prepared = retryPrepared
                recognized = { text: retry.data.text, confidence: retryConfidence, words: normalizeWords('words' in retry.data ? retry.data.words : []) }
              } else {
                URL.revokeObjectURL(retryPrepared.previewUrl)
              }
            }
          }
          const text = cleanText(recognized.text, mode)
          if (!text) URL.revokeObjectURL(prepared.previewUrl)
          else nextPages.push({ id: crypto.randomUUID(), text, previewUrl: prepared.previewUrl, source: files.length > 1 ? `Batch image ${index + 1}` : source, confidence: recognized.confidence, quality: await assessImageQuality(prepared.blob), words: recognized.words })
        }
        if (nextPages.length) {
          setPages((current) => [...current, ...nextPages])
          const nextIndex = pages.length + nextPages.length - 1
          setSelectedPageIndex(nextIndex)
          setOcrText(nextPages.at(-1)?.text ?? '')
          const lowConfidence = nextPages.filter((page) => page.confidence < 70).length
          setStatus(dynamic.result(nextPages.length, files.length, lowConfidence))
          setTab('text')
          markToolSuccess('ocr-text', { headline: 'Text extracted locally', detail: `${nextPages.length} page(s) recognized locally. Review the text before exporting or sharing.`, shareText: 'I turned images into editable text locally with PureHub.' })
        } else {
          setStatus(statusCopy.noText)
        }
      } finally {
        await worker.terminate()
      }
    } catch {
      setStatus(statusCopy.failed)
    } finally {
      setRunning(false)
      setPackProgress(0)
      event.target.value = ''
    }
  }

  const selectPage = (index: number) => {
    const page = pages[index]
    if (!page) return
    setSelectedPageIndex(index)
    setOcrText(page.text)
  }

  const updateCurrentText = (value: string) => {
    setOcrText(value)
    if (selectedPageIndex >= 0) setPages((current) => current.map((page, index) => index === selectedPageIndex ? { ...page, text: value } : page))
  }

  const deleteCurrentPage = () => {
    const target = pages[selectedPageIndex]
    if (!target) return
    URL.revokeObjectURL(target.previewUrl)
    const next = pages.filter((_, index) => index !== selectedPageIndex)
    setPages(next)
    const nextIndex = Math.min(selectedPageIndex, next.length - 1)
    setSelectedPageIndex(nextIndex)
    setOcrText(next[nextIndex]?.text ?? '')
    if (!next.length) setTab('scan')
  }

  const movePage = (direction: -1 | 1) => {
    if (selectedPageIndex < 0) return
    const target = selectedPageIndex + direction
    if (target < 0 || target >= pages.length) return
    setPages((current) => {
      const next = [...current]
      const [page] = next.splice(selectedPageIndex, 1)
      next.splice(target, 0, page)
      return next
    })
    setSelectedPageIndex(target)
  }

  const saveReceiptToMoneyStudio = async () => {
    if (!detectedReceipt?.total) return
    const record: ExpenseRecord = {
      id: crypto.randomUUID(), title: detectedReceipt.merchant, amount: detectedReceipt.total,
      category: 'Shopping', transactionType: 'expense', wallet: 'Cash',
      note: [detectedReceipt.tax != null ? `Tax: ${detectedReceipt.tax}` : '', 'Imported from OCR Studio'].filter(Boolean).join(' · '),
      createdAt: new Date().toISOString(),
    }
    await expenseRepository.put(record)
    setStatus(statusCopy.receipt)
  }

  const saveDocument = async () => {
    if (!allText) return
    const storedPages: OcrStoredPage[] = await Promise.all(pages.map(async (page) => ({
      id: page.id, text: page.text, source: page.source, confidence: page.confidence, quality: page.quality, words: page.words,
      imageDataUrl: await blobUrlToDataUrl(page.previewUrl),
    })))
    const record: OcrDocumentRecord = {
      id: crypto.randomUUID(), title: title.trim() || 'Untitled scan', text: allText,
      source: pages.length > 1 ? `${pages.length} pages` : pages[0]?.source || 'OCR',
      pageCount: Math.max(1, pages.length), createdAt: new Date().toISOString(), pages: storedPages,
    }
    await ocrDocumentRepository.put(record)
    setDocuments((current) => [record, ...current])
    setStatus(statusCopy.saved)
    markToolSuccess('ocr-text', { headline: 'OCR document saved', detail: 'Searchable text was saved only in this browser library.', shareText: 'I saved a private OCR document with PureHub.' })
  }

  const exportPdf = async () => {
    if (!allText) return
    const { jsPDF } = await import('jspdf')
    const pdf = new jsPDF({ unit: 'pt', format: 'a4', encryption: pdfPassword.trim().length >= 8 ? { userPassword: pdfPassword.trim(), ownerPassword: `${pdfPassword.trim()}-owner`, userPermissions: ['print', 'copy'] } : undefined })
    pdf.setProperties({ title: title || 'PureHub OCR', subject: 'Offline OCR document', creator: 'PureHub OCR Studio', author: 'PureHub' })
    const exportPages = pages.length ? pages : [{ id: 'text', text: allText, previewUrl: '', source: 'OCR', confidence: 0 }]
    for (let index = 0; index < exportPages.length; index += 1) {
      if (index) pdf.addPage()
      const page = exportPages[index]
      const pageText = page.text || allText
      const lines = pdf.splitTextToSize(pageText, 510) as string[]
      pdf.setFontSize(10)
      // Keep selectable OCR text in the PDF while placing the original scan above it.
      lines.slice(0, 120).forEach((line, lineIndex) => pdf.text(line, 42, 42 + lineIndex * 12))
      if (page.previewUrl) {
        pdf.addImage(await blobUrlToDataUrl(page.previewUrl), 'JPEG', 20, 20, 555, 802, undefined, 'FAST')
      } else {
        pdf.setFontSize(18); pdf.text(title || 'PureHub OCR', 42, 52)
        pdf.setFontSize(11)
        lines.forEach((line, lineIndex) => pdf.text(line, 42, 82 + lineIndex * 15))
      }
    }
    pdf.save(`${safeName(title)}.pdf`)
    markToolSuccess('ocr-text', { headline: pdfPassword.trim().length >= 8 ? 'Password-protected OCR PDF exported' : 'OCR text exported as PDF', detail: 'Your OCR document was exported locally with a searchable text layer.', shareText: 'I exported an OCR PDF locally with PureHub.' })
  }

  const sendToDocumentSuite = async () => {
    if (!pages.length) return
    const handoffPages = await Promise.all(pages.map(async (page) => {
      const blob = await (await fetch(page.previewUrl)).blob()
      const dataUrl = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.onerror = reject; reader.readAsDataURL(blob) })
      return { dataUrl, text: page.text, confidence: page.confidence }
    }))
    localStorage.setItem(DOCUMENT_HANDOFF_KEY, JSON.stringify({ title, pages: handoffPages, createdAt: new Date().toISOString() }))
    const locale = window.location.pathname.split('/')[1] || 'en'
    window.location.href = `/${locale}/doc-to-pdf`
  }

  const shareText = async () => {
    if (navigator.share) await navigator.share({ title, text: allText })
    else await navigator.clipboard.writeText(allText)
    void trackProductEvent('ocr-text', 'share')
  }

  const resetDocument = () => {
    pages.forEach((page) => URL.revokeObjectURL(page.previewUrl))
    setPages([]); setSelectedPageIndex(-1); setOcrText(''); setTitle(statusCopy.title); setStatus(statusCopy.reset); setTab('scan')
  }

  return (
    <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <header className="bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-4 sm:p-5 dark:from-emerald-950/50 dark:via-slate-900 dark:to-sky-950/30">
        <div className="flex items-start gap-3">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-950 text-emerald-300 dark:bg-emerald-300 dark:text-slate-950"><ScanText className="size-6" /></span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-black tracking-[.2em] text-emerald-700 dark:text-emerald-300">{ui.private}</p>
            <h2 className="text-2xl font-black tracking-tight text-slate-950 dark:text-white">{ui.title}</h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{ui.description}</p>
          </div>
          <span className="hidden items-center gap-1 rounded-full border border-emerald-200 bg-white/80 px-2.5 py-1 text-xs font-bold text-emerald-800 sm:flex dark:border-emerald-800 dark:bg-slate-900 dark:text-emerald-200"><ShieldCheck className="size-3.5" /> {ui.device}</span>
        </div>
        <nav className="mt-4 grid grid-cols-3 gap-2" aria-label={deep.sections}>
          {([['scan', Camera], ['text', FileText], ['library', History]] as const).map(([value, Icon]) => (
            <button key={value} onClick={() => setTab(value)} className={`flex min-h-11 items-center justify-center gap-2 rounded-xl border px-2 text-sm font-bold transition ${tab === value ? 'border-emerald-300 bg-emerald-700 text-white shadow-sm dark:border-emerald-500 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-200 bg-white/80 text-slate-700 hover:bg-white dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200'}`}><Icon className="size-4" />{{ scan: ui.scan, text: ui.text, library: ui.library }[value]}</button>
          ))}
        </nav>
      </header>

      <div className="p-4 sm:p-5">
        {tab === 'scan' ? (
          <div className="space-y-4">
            <div className="flex gap-2 overflow-x-auto pb-1">{(['Document', 'Receipt', 'Note'] as ScanMode[]).map((value) => <button key={value} onClick={() => setMode(value)} className={`rounded-full border px-3 py-2 text-xs font-bold ${mode === value ? 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-700 dark:bg-emerald-950 dark:text-emerald-200' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'}`}>{{ Document: deep.document, Receipt: deep.receipt, Note: deep.note }[value]}</button>)}</div>
            <div className="relative overflow-hidden rounded-[22px] bg-slate-950">
              {currentPreview ? <img src={currentPreview} alt={qualityCopy.latestPage} className="aspect-[4/5] w-full object-contain" /> : (
                <div className="grid aspect-[4/5] place-items-center bg-[radial-gradient(circle_at_center,_#193448,_#07111e_68%)] text-center text-white">
                  <div><ScanText className="mx-auto size-14 text-emerald-300" /><p className="mt-3 font-bold">{ui.frame}</p><p className="mt-1 text-sm text-slate-300">{ui.noUpload}</p></div>
                </div>
              )}
              <span className="absolute left-3 top-3 flex items-center gap-1 rounded-full bg-slate-950/80 px-2.5 py-1.5 text-xs font-bold text-emerald-300"><LockKeyhole className="size-3.5" /> {ui.local}</span>
              {pages.length ? <span className="absolute right-3 top-3 rounded-full bg-white/90 px-2.5 py-1.5 text-xs font-bold text-slate-900">{pages.length} {pages.length === 1 ? deep.page : deep.pages}</span> : null}
              <button disabled={running} onClick={() => cameraInputRef.current?.click()} className="absolute bottom-4 left-1/2 flex min-h-12 -translate-x-1/2 items-center gap-2 rounded-full bg-emerald-500 px-5 font-black text-slate-950 shadow-lg disabled:opacity-60">{running ? <LoaderCircle className="size-5 animate-spin" /> : <Camera className="size-5" />}{running ? ui.reading : ui.capture}</button>
              <input ref={cameraInputRef} hidden type="file" accept="image/*" capture="environment" onChange={(event) => void handleFile(event, 'Camera')} />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <ActionButton tone="muted" onClick={() => imageInputRef.current?.click()} disabled={running}><ImagePlus className="mr-2 inline size-4" />{ui.image}</ActionButton>
              <ActionButton tone="muted" onClick={() => setRotation((value) => (value + 90) % 360)} disabled={running}><RotateCw className="mr-2 inline size-4" />{ui.rotate} {rotation}°</ActionButton>
              <input ref={imageInputRef} hidden multiple type="file" accept="image/*" onChange={(event) => void handleFile(event, 'Image')} />
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-700 dark:bg-slate-950">
              <div className="flex items-center gap-2"><Sparkles className="size-4 text-emerald-600" /><p className="text-sm font-black text-slate-900 dark:text-white">{ui.cleanup}</p></div>
              {mode !== 'Note' ? <p className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">{ui.edges}</p> : null}
              <div className="mt-3 flex gap-2 overflow-x-auto">{(['Original', 'Clean', 'B&W'] as ImageFilter[]).map((value) => <button key={value} onClick={() => setFilter(value)} className={`rounded-full border px-3 py-1.5 text-xs font-bold ${filter === value ? 'border-emerald-400 bg-emerald-100 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200' : 'border-slate-300 dark:border-slate-700'}`}>{{ Original: deep.original, Clean: deep.clean, 'B&W': deep.bw }[value]}</button>)}</div>
              <label className="mt-3 block text-xs font-bold text-slate-600 dark:text-slate-300">{deep.edgeCrop} {Math.round(crop * 100)}%<input type="range" min="0" max="0.16" step="0.01" value={crop} onChange={(event) => setCrop(Number(event.target.value))} className="mt-2 w-full accent-emerald-600" /></label>
            </div>
            <label className="block text-sm font-bold text-slate-700 dark:text-slate-200">{ui.language}<select value={language} disabled={running} onChange={(event) => setLanguage(event.target.value as typeof language)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 dark:border-slate-600 dark:bg-slate-950">{OCR_LANGUAGES.map((item) => <option key={item.code} value={item.code}>{languageLabel(item.code, item.label, locale)} · {cachedLanguages.includes(item.code) ? ui.ready : ui.download}</option>)}</select></label>
            <div className="rounded-2xl border border-sky-200 bg-sky-50 p-3.5 dark:border-sky-900 dark:bg-sky-950/30"><div className="flex items-center justify-between gap-3"><div><p className="text-sm font-black text-sky-950 dark:text-sky-100">{ui.cloud}</p><p className="mt-1 text-xs text-sky-800 dark:text-sky-200">{ui.cloudHint}</p></div><button type="button" role="switch" aria-checked={cloudAssist} onClick={() => setCloudAssist((value) => !value)} className={`min-h-10 rounded-full px-3 text-xs font-black ${cloudAssist ? 'bg-sky-700 text-white' : 'bg-white text-slate-700 dark:bg-slate-800 dark:text-slate-200'}`}>{cloudAssist ? ui.enabled : ui.offline}</button></div>{cloudAssist ? <label className="mt-3 block text-xs font-bold text-sky-900 dark:text-sky-100">{deep.endpoint}<input value={cloudEndpoint} onChange={(event) => { setCloudEndpoint(event.target.value); localStorage.setItem(OCR_CLOUD_ENDPOINT_KEY, event.target.value) }} placeholder="https://your-server.example/ocr" className="mt-1.5 min-h-11 w-full rounded-xl border border-sky-200 bg-white px-3 text-sm font-normal dark:border-sky-800 dark:bg-slate-950" inputMode="url" /><span className="mt-1 block font-medium">{deep.endpointHint}</span></label> : null}</div>
            {running && packProgress > 0 ? <div className="rounded-xl bg-emerald-50 p-3 dark:bg-emerald-950/30"><div className="flex justify-between text-xs font-bold text-emerald-800 dark:text-emerald-200"><span>{deep.progress}</span><span>{packProgress}%</span></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-emerald-100 dark:bg-emerald-950"><div className="h-full rounded-full bg-emerald-600 transition-all" style={{ width: `${packProgress}%` }} /></div></div> : null}
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div>
        ) : null}

        {tab === 'text' ? (
          ocrText ? <div className="space-y-3">
            {currentPreview ? <img src={currentPreview} alt={qualityCopy.scannedPage} className="h-44 w-full rounded-2xl bg-slate-100 object-contain dark:bg-slate-950" /> : null}
            <div className="flex flex-wrap gap-2 text-xs font-bold text-slate-600 dark:text-slate-300"><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{Math.max(1, pages.length)} {pages.length === 1 ? deep.page : deep.pages}</span><span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{ocrText.split(/\s+/).filter(Boolean).length} {deep.words}</span><span className={`rounded-full px-2.5 py-1 ${(currentPage?.confidence ?? 0) >= 75 ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200' : 'bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200'}`}>{currentPage?.confidence ?? 0}% {deep.confidence}</span>{currentPage?.quality ? <span className={`rounded-full px-2.5 py-1 ${currentPage.quality.score >= 75 ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200' : 'bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200'}`}>{currentPage.quality.score}% {deep.imageQuality}</span> : null}</div>
            {currentPage?.quality?.issues.length ? <p className="rounded-xl bg-amber-50 p-3 text-xs font-semibold text-amber-900 dark:bg-amber-950/30 dark:text-amber-100">{qualityCopy.retake}: {currentPage.quality.issues.map((issue) => localizedQualityIssue(issue, locale)).join(', ')}.{currentPage.quality.brightness != null ? ` ${qualityCopy.brightness} ${currentPage.quality.brightness}/255 · ${qualityCopy.contrast} ${currentPage.quality.contrast} · ${qualityCopy.sharpness} ${currentPage.quality.sharpness}.` : ''}</p> : null}
            {pages.length > 1 && selectedPageIndex >= 0 ? <div className="space-y-2"><div className="grid grid-cols-[1fr_auto_1fr] items-center gap-2"><ActionButton tone="muted" disabled={selectedPageIndex === 0} onClick={() => selectPage(selectedPageIndex - 1)}>{deep.previous}</ActionButton><span className="text-xs font-black">{deep.page} {selectedPageIndex + 1}/{pages.length}</span><ActionButton tone="muted" disabled={selectedPageIndex === pages.length - 1} onClick={() => selectPage(selectedPageIndex + 1)}>{deep.next}</ActionButton></div><div className="grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={selectedPageIndex === 0} onClick={() => movePage(-1)}>{deep.moveEarlier}</ActionButton><ActionButton tone="muted" disabled={selectedPageIndex === pages.length - 1} onClick={() => movePage(1)}>{deep.moveLater}</ActionButton></div></div> : null}
            <FormInput value={title} onChange={(event) => setTitle(event.target.value)} aria-label={deep.documentTitle} />
            <FormTextArea className="min-h-64 resize-y" value={ocrText} onChange={(event) => updateCurrentText(event.target.value)} aria-label={deep.recognizedText} />
            {detectedUrl || detectedEmail || detectedPhone ? <div className="flex flex-wrap gap-2 rounded-2xl bg-sky-50 p-3 dark:bg-sky-950/30"><span className="w-full text-xs font-black text-sky-900 dark:text-sky-200">{deep.quick}</span>{detectedUrl ? <a className="rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={detectedUrl} target="_blank" rel="noreferrer">{deep.openLink}</a> : null}{detectedEmail ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`mailto:${detectedEmail}`}><Mail className="size-3" />{emailCopy}</a> : null}{detectedPhone ? <a className="flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-sky-800" href={`tel:${detectedPhone.replace(/[^+\d]/g, '')}`}><Phone className="size-3" />{deep.call}</a> : null}</div> : null}
            {detectedContact.email || detectedContact.phone ? <div className="rounded-2xl border border-violet-200 bg-violet-50 p-3.5 text-violet-950 dark:border-violet-900 dark:bg-violet-950/30 dark:text-violet-100"><p className="text-xs font-black tracking-wide">{deep.contactDetected}</p><p className="mt-1 text-sm">{detectedContact.name || deep.contact}{detectedContact.email ? ` · ${detectedContact.email}` : ''}{detectedContact.phone ? ` · ${detectedContact.phone}` : ''}</p><ActionButton className="mt-3 w-full justify-center" onClick={() => { const vcard = ['BEGIN:VCARD', 'VERSION:3.0', `FN:${detectedContact.name || deep.contact}`, detectedContact.phone ? `TEL:${detectedContact.phone.replace(/[^+\d]/g, '')}` : '', detectedContact.email ? `EMAIL:${detectedContact.email}` : '', 'END:VCARD'].filter(Boolean).join('\n'); downloadBlob(new Blob([vcard], { type: 'text/vcard;charset=utf-8' }), `${safeName(detectedContact.name || title)}.vcf`) }}>{deep.exportContact}</ActionButton></div> : null}
            {detectedCode ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-3.5 text-emerald-950 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-100"><p className="text-xs font-black tracking-wide">{deep.codeDetected}</p><p className="mt-1 break-all text-sm font-bold">{detectedCode}</p><button className="mt-3 min-h-10 rounded-xl bg-white px-3 text-xs font-black text-emerald-800" onClick={() => void navigator.clipboard.writeText(detectedCode)}>{deep.copyCode}</button></div> : null}
            {detectedReceipt ? <div className="rounded-2xl border border-amber-200 bg-amber-50 p-3.5 text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-100"><p className="text-xs font-black tracking-wide">{deep.receiptDetected}</p><p className="mt-1 font-bold">{detectedReceipt.merchant}</p><p className="text-lg font-black">{detectedReceipt.total != null ? `${deep.total} ${detectedReceipt.total}` : deep.totalReview}</p><ActionButton className="mt-3 w-full justify-center" disabled={detectedReceipt.total == null} onClick={() => void saveReceiptToMoneyStudio()}>{deep.saveMoney}</ActionButton></div> : null}
            {detectedTable.length > 0 ? <div className="rounded-2xl border border-indigo-200 bg-indigo-50 p-3.5 text-indigo-950 dark:border-indigo-900 dark:bg-indigo-950/30 dark:text-indigo-100"><p className="text-xs font-black tracking-wide">{deep.tableDetected}</p><p className="mt-1 text-sm">{detectedTable.length} {deep.rows}, {Math.max(...detectedTable.map((row) => row.length))} {deep.columns}. {deep.reviewExport}</p><ActionButton className="mt-3 w-full justify-center" onClick={() => downloadCsv(detectedTable, `${safeName(title)}.csv`)}>CSV</ActionButton></div> : null}
            {detectedFields.length > 0 ? <div className="rounded-2xl border border-sky-200 bg-sky-50 p-3.5 text-sky-950 dark:border-sky-900 dark:bg-sky-950/30 dark:text-sky-100"><p className="text-xs font-black tracking-wide">{deep.formDetected}</p><p className="mt-1 text-sm">{detectedFields.length} {deep.fieldsFound}</p><ActionButton className="mt-3 w-full justify-center" onClick={() => downloadBlob(new Blob([JSON.stringify(detectedFields, null, 2)], { type: 'application/json;charset=utf-8' }), `${safeName(title)}-fields.json`)}>JSON</ActionButton></div> : null}
            {pages.some((page) => page.words?.length) ? <ActionButton tone="muted" className="w-full justify-center" onClick={() => downloadLayout(pages, title)}>{deep.exportLayout}</ActionButton> : null}
            <div className="grid grid-cols-2 gap-2"><ActionButton onClick={() => void navigator.clipboard.writeText(ocrText)}><Clipboard className="mr-2 inline size-4" />{deep.copy}</ActionButton><ActionButton tone="muted" onClick={() => void shareText()}><Share2 className="mr-2 inline size-4" />{deep.share}</ActionButton></div>
            <label className="block text-xs font-bold text-slate-600 dark:text-slate-300">{deep.pdfPassword}<FormInput type="password" minLength={8} maxLength={128} value={pdfPassword} onChange={(event) => setPdfPassword(event.target.value)} placeholder={deep.pdfPlaceholder} className="mt-1" /></label>
            <div className="grid grid-cols-3 gap-2"><ActionButton tone="muted" onClick={() => downloadBlob(new Blob([allText], { type: 'text/plain;charset=utf-8' }), `${safeName(title)}.txt`)}><Download className="mr-1 inline size-4" />TXT</ActionButton><ActionButton tone="muted" onClick={() => void exportPdf()}><FileText className="mr-1 inline size-4" />PDF</ActionButton><ActionButton tone="muted" onClick={() => void saveDocument()}><History className="mr-1 inline size-4" />{deep.save}</ActionButton></div>
            <ActionButton className="w-full justify-center" onClick={() => void sendToDocumentSuite()}><ScanText className="size-4" />{deep.continuePdf}</ActionButton>
            <div className="grid grid-cols-3 gap-2"><ActionButton tone="muted" onClick={() => setTab('scan')}>{deep.addPage}</ActionButton><ActionButton tone="danger" disabled={!pages.length} onClick={deleteCurrentPage}>{deep.deletePage}</ActionButton><ActionButton tone="danger" onClick={resetDocument}>{deep.newDocument}</ActionButton></div>
            <p role="status" className="text-sm font-semibold text-slate-600 dark:text-slate-300">{status}</p>
          </div> : <div className="py-12 text-center"><FileText className="mx-auto size-12 text-emerald-600" /><h3 className="mt-3 text-lg font-black">{deep.noText}</h3><p className="mt-1 text-sm text-slate-500">{deep.noTextHint}</p><ActionButton className="mt-4" onClick={() => setTab('scan')}>{deep.start}</ActionButton></div>
        ) : null}

        {tab === 'library' ? <div className="space-y-3">
          <div className="flex items-center justify-between"><div><h3 className="text-lg font-black">{deep.privateLibrary}</h3><p className="text-sm text-slate-500">{deep.libraryHint}</p></div>{documents.length ? <button title={deep.clearLibrary} aria-label={deep.clearLibrary} onClick={() => void ocrDocumentRepository.clear().then(() => setDocuments([]))} className="grid size-10 place-items-center rounded-xl border border-rose-200 text-rose-600"><Trash2 className="size-4" /></button> : null}</div>
          <label className="relative block"><Search className="pointer-events-none absolute left-3 top-3.5 size-4 text-slate-400" /><FormInput value={query} onChange={(event) => setQuery(event.target.value)} placeholder={deep.search} className="pl-10" /></label>
          {filteredDocuments.length ? filteredDocuments.map((item) => <article key={item.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-700"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300"><FileImage className="size-5" /></span><button className="min-w-0 flex-1 text-left" onClick={() => { pages.forEach((page) => URL.revokeObjectURL(page.previewUrl)); const restored = (item.pages ?? []).map((page) => ({ ...page, previewUrl: page.imageDataUrl ?? '' })); setPages(restored); setSelectedPageIndex(restored.length ? 0 : -1); setTitle(item.title); setOcrText(restored[0]?.text ?? item.text); setStatus(restored.length ? dynamic.opened : dynamic.legacy); setTab('text') }}><strong className="block truncate text-sm">{item.title}</strong><span className="line-clamp-2 text-xs text-slate-500">{item.text.replace(/\s+/g, ' ')}</span><span className="mt-1 block text-[11px] font-bold text-emerald-700">{item.source}{item.pages?.length ? ` · ${dynamic.imagesSaved}` : ''}</span></button><button title={dynamic.delete} aria-label={dynamic.delete} onClick={() => void ocrDocumentRepository.remove(item.id).then(() => setDocuments((current) => current.filter((row) => row.id !== item.id)))} className="grid size-9 place-items-center rounded-lg text-rose-600 hover:bg-rose-50"><Trash2 className="size-4" /></button></article>) : <div className="rounded-2xl border border-dashed border-slate-300 py-10 text-center text-sm text-slate-500 dark:border-slate-700">{documents.length ? dynamic.noMatch : dynamic.empty}</div>}
        </div> : null}
      </div>
    </section>
  )
}

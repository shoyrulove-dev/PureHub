import { useEffect, useRef, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  Camera,
  Download,
  FileImage,
  FileText,
  ImagePlus,
  Files,
  Minimize2,
  PenLine,
  ScanText,
  Scissors,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import { ActionButton, FormInput } from "../MiniAppPrimitives";
import { markToolSuccess } from "../../../lib/tool-success";
import { buildMiniAppPath } from "../../../i18n/routing";
import { normalizeLocale } from "../../../i18n/locales";

type Page = {
  id: string;
  file: File;
  url: string;
  rotation: number;
  fit: "contain" | "cover";
  crop: number;
  filter: "original" | "document";
  recognizedText?: string;
};
type ImportedPdf = { id: string; file: File; pageCount: number };
const DOCUMENT_HANDOFF_KEY = "purehub.document-suite.ocr-handoff.v1";

const DOC_UI = {
  en: { eyebrow: 'PUREHUB DOCUMENT SUITE', description: 'Capture, arrange, and export clean PDFs without a cloud account.', staged: 'Pages staged', openOcr: 'Open OCR Studio', title: 'Document title', toolbox: 'PDF toolbox', toolboxHint: 'Merge, extract, optimize, or add a visual signature in this browser.', capture: 'Capture', images: 'Add images', cleanHint: 'One-tap grayscale and contrast for scanned pages', cropAll: 'Auto crop all', cleanAll: 'Clean all', empty: 'Your document starts here', emptyHint: 'Up to 20 local image pages.', quality: 'Image quality', building: 'Building PDF…', export: 'Export PDF', workflow: 'Use OCR Studio for searchable text, then Doc to PDF for page layout and export.', myDocument: 'My document', initial: 'Add images to build a private PDF.', atLeast: 'Add at least one image first.', buildingStatus: 'Building PDF on this device…', exported: 'PDF exported. No document was uploaded.', failed: 'The PDF could not be created. Try fewer or smaller images.' },
  vi: { eyebrow: 'BỘ TÀI LIỆU PUREHUB', description: 'Chụp, sắp xếp và xuất PDF sạch mà không cần tài khoản đám mây.', staged: 'Trang đã thêm', openOcr: 'Mở OCR Studio', title: 'Tên tài liệu', toolbox: 'Bộ công cụ PDF', toolboxHint: 'Gộp, trích xuất, tối ưu hoặc thêm chữ ký hiển thị ngay trong trình duyệt.', capture: 'Chụp trang', images: 'Thêm ảnh', cleanHint: 'Làm xám và tăng tương phản cho trang quét chỉ với một chạm', cropAll: 'Tự cắt tất cả', cleanAll: 'Làm sạch tất cả', empty: 'Bắt đầu tài liệu tại đây', emptyHint: 'Tối đa 20 trang ảnh cục bộ.', quality: 'Chất lượng ảnh', building: 'Đang tạo PDF…', export: 'Xuất PDF', workflow: 'Dùng OCR Studio khi cần văn bản tìm kiếm, sau đó dùng Doc to PDF để dàn trang và xuất.', myDocument: 'Tài liệu của tôi', initial: 'Thêm ảnh để tạo PDF riêng tư.', atLeast: 'Hãy thêm ít nhất một ảnh.', buildingStatus: 'Đang tạo PDF trên thiết bị…', exported: 'Đã xuất PDF. Không có tài liệu nào được tải lên.', failed: 'Không thể tạo PDF. Hãy thử ít ảnh hơn hoặc ảnh nhỏ hơn.' },
  zh: { eyebrow: 'PUREHUB 文档套件', description: '无需云账户即可拍摄、排序并导出清晰 PDF。', staged: '已添加页面', openOcr: '打开 OCR Studio', title: '文档标题', toolbox: 'PDF 工具箱', toolboxHint: '在浏览器中合并、提取、优化或添加可视签名。', capture: '拍摄', images: '添加图片', cleanHint: '一键为扫描页应用灰度和对比度', cropAll: '自动裁剪全部', cleanAll: '清理全部', empty: '从这里开始创建文档', emptyHint: '最多 20 个本地图片页面。', quality: '图片质量', building: '正在创建 PDF…', export: '导出 PDF', workflow: '需要可搜索文字时使用 OCR Studio，再用 Doc to PDF 排版和导出。', myDocument: '我的文档', initial: '添加图片以创建私密 PDF。', atLeast: '请先添加至少一张图片。', buildingStatus: '正在设备上创建 PDF…', exported: 'PDF 已导出，没有上传任何文档。', failed: '无法创建 PDF，请减少图片数量或尺寸。' },
  es: { eyebrow: 'SUITE DE DOCUMENTOS PUREHUB', description: 'Captura, ordena y exporta PDF limpios sin una cuenta en la nube.', staged: 'Páginas añadidas', openOcr: 'Abrir OCR Studio', title: 'Título del documento', toolbox: 'Herramientas PDF', toolboxHint: 'Une, extrae, optimiza o añade una firma visual en este navegador.', capture: 'Capturar', images: 'Añadir imágenes', cleanHint: 'Escala de grises y contraste con un toque para páginas escaneadas', cropAll: 'Recortar todo', cleanAll: 'Limpiar todo', empty: 'Tu documento empieza aquí', emptyHint: 'Hasta 20 páginas de imágenes locales.', quality: 'Calidad de imagen', building: 'Creando PDF…', export: 'Exportar PDF', workflow: 'Usa OCR Studio para texto buscable y Doc to PDF para maquetar y exportar.', myDocument: 'Mi documento', initial: 'Añade imágenes para crear un PDF privado.', atLeast: 'Añade al menos una imagen.', buildingStatus: 'Creando PDF en este dispositivo…', exported: 'PDF exportado. No se subió ningún documento.', failed: 'No se pudo crear el PDF. Prueba con menos imágenes o más pequeñas.' },
} as const;

const DOC_MORE = {
  en: { addPdfs: 'Add PDFs', merge: 'Merge', pages: 'pages', removePdf: 'Remove PDF', from: 'From page', to: 'To page', extract: 'Extract', compress: 'Compress', signer: 'Signer name', page: 'Page', signaturePage: 'Signature page', sign: 'Add visual signature', signatureHint: 'Visual signature only; it is not a certificate-backed digital signature. Compression optimizes PDF structure and may not shrink files that are already optimized.', moveUp: 'Move up', moveDown: 'Move down', rotate: 'Rotate', clean: 'Clean', original: 'Original', autoCrop: 'Auto crop', removePage: 'Remove page', searchable: 'Searchable document' },
  vi: { addPdfs: 'Thêm PDF', merge: 'Gộp', pages: 'trang', removePdf: 'Xóa PDF', from: 'Từ trang', to: 'Đến trang', extract: 'Tách trang', compress: 'Nén', signer: 'Tên người ký', page: 'Trang', signaturePage: 'Trang ký', sign: 'Thêm chữ ký hiển thị', signatureHint: 'Chỉ là chữ ký hiển thị, không phải chữ ký số có chứng thư. Việc tối ưu cấu trúc có thể không giảm thêm PDF đã được tối ưu.', moveUp: 'Đưa lên', moveDown: 'Đưa xuống', rotate: 'Xoay', clean: 'Sạch', original: 'Gốc', autoCrop: 'Tự cắt', removePage: 'Xóa trang', searchable: 'Tài liệu có thể tìm kiếm' },
  zh: { addPdfs: '添加 PDF', merge: '合并', pages: '页', removePdf: '移除 PDF', from: '起始页', to: '结束页', extract: '提取', compress: '压缩', signer: '签名人', page: '页码', signaturePage: '签名页', sign: '添加可视签名', signatureHint: '仅为可视签名，不是由证书支持的数字签名。结构优化可能无法进一步缩小已优化的 PDF。', moveUp: '上移', moveDown: '下移', rotate: '旋转', clean: '清理', original: '原图', autoCrop: '自动裁剪', removePage: '移除页面', searchable: '可搜索文档' },
  es: { addPdfs: 'Añadir PDF', merge: 'Unir', pages: 'páginas', removePdf: 'Quitar PDF', from: 'Desde página', to: 'Hasta página', extract: 'Extraer', compress: 'Comprimir', signer: 'Nombre del firmante', page: 'Página', signaturePage: 'Página de firma', sign: 'Añadir firma visual', signatureHint: 'Es solo una firma visual, no una firma digital con certificado. La optimización puede no reducir un PDF ya optimizado.', moveUp: 'Subir', moveDown: 'Bajar', rotate: 'Girar', clean: 'Limpio', original: 'Original', autoCrop: 'Recorte automático', removePage: 'Quitar página', searchable: 'Documento buscable' },
} as const;

const DOC_DYNAMIC = {
  en: { imported: (n: number) => `${n} OCR page(s) imported. Searchable text will be embedded in the PDF.`, handoffFail: 'The OCR handoff could not be restored. Add images manually.', staged: (n: number) => `${n} page(s) staged locally.`, opening: (n: number) => `Opening ${n} PDF file(s) locally...`, opened: (n: number) => `${n} PDF file(s) opened locally. No document was uploaded.`, openFail: 'One of the PDFs could not be opened.', running: (label: string) => `${label} on this device...`, complete: (label: string) => `${label} complete. No PDF was uploaded.`, operationFail: 'The PDF operation failed. Password-protected or damaged files may not be supported.', framed: (name: string) => `Auto-framed ${name}. Review before export.`, noFrame: (name: string) => `No reliable frame found for ${name}; it was left unchanged.`, finding: (n: number) => `Finding document edges in ${n} page(s) locally...`, framedAll: (found: number, total: number) => `Auto-framed ${found}/${total} page(s). Uncertain pages were left unchanged.`, edgeFail: 'Document edges could not be detected. Your pages were left unchanged.', merging: 'Merging PDFs', extracting: 'Extracting pages', optimizing: 'Optimizing PDF', signing: 'Adding visual signature' },
  vi: { imported: (n: number) => `Đã nhập ${n} trang OCR. Văn bản tìm kiếm được sẽ được nhúng vào PDF.`, handoffFail: 'Không thể khôi phục dữ liệu từ OCR. Hãy thêm ảnh thủ công.', staged: (n: number) => `Đã thêm ${n} trang trên thiết bị.`, opening: (n: number) => `Đang mở ${n} tệp PDF trên thiết bị...`, opened: (n: number) => `Đã mở ${n} tệp PDF trên thiết bị. Không có tài liệu nào được tải lên.`, openFail: 'Không thể mở một trong các tệp PDF.', running: (label: string) => `${label} trên thiết bị...`, complete: (label: string) => `${label} hoàn tất. Không có PDF nào được tải lên.`, operationFail: 'Thao tác PDF thất bại. Tệp có mật khẩu hoặc bị hỏng có thể không được hỗ trợ.', framed: (name: string) => `Đã tự căn ${name}. Hãy kiểm tra trước khi xuất.`, noFrame: (name: string) => `Không tìm thấy khung tin cậy cho ${name}; trang được giữ nguyên.`, finding: (n: number) => `Đang tìm cạnh tài liệu trong ${n} trang...`, framedAll: (found: number, total: number) => `Đã tự căn ${found}/${total} trang. Các trang chưa chắc chắn được giữ nguyên.`, edgeFail: 'Không thể nhận diện cạnh tài liệu. Các trang được giữ nguyên.', merging: 'Đang gộp PDF', extracting: 'Đang tách trang', optimizing: 'Đang tối ưu PDF', signing: 'Đang thêm chữ ký hiển thị' },
  zh: { imported: (n: number) => `已导入 ${n} 个 OCR 页面，可搜索文字将嵌入 PDF。`, handoffFail: '无法恢复 OCR 交接内容，请手动添加图片。', staged: (n: number) => `已在本机添加 ${n} 页。`, opening: (n: number) => `正在本机打开 ${n} 个 PDF 文件...`, opened: (n: number) => `已在本机打开 ${n} 个 PDF 文件，未上传任何文档。`, openFail: '其中一个 PDF 无法打开。', running: (label: string) => `正在设备上${label}...`, complete: (label: string) => `${label}完成，未上传任何 PDF。`, operationFail: 'PDF 操作失败，可能不支持受密码保护或损坏的文件。', framed: (name: string) => `已自动校正 ${name}，导出前请检查。`, noFrame: (name: string) => `未找到 ${name} 的可靠边框，页面保持不变。`, finding: (n: number) => `正在本机查找 ${n} 页文档的边缘...`, framedAll: (found: number, total: number) => `已自动校正 ${found}/${total} 页，不确定的页面保持不变。`, edgeFail: '无法检测文档边缘，页面保持不变。', merging: '合并 PDF', extracting: '提取页面', optimizing: '优化 PDF', signing: '添加可视签名' },
  es: { imported: (n: number) => `Se importaron ${n} página(s) OCR. El texto buscable se incluirá en el PDF.`, handoffFail: 'No se pudo recuperar el envío desde OCR. Añade las imágenes manualmente.', staged: (n: number) => `${n} página(s) preparadas localmente.`, opening: (n: number) => `Abriendo ${n} archivo(s) PDF localmente...`, opened: (n: number) => `${n} archivo(s) PDF abiertos localmente. No se subió ningún documento.`, openFail: 'No se pudo abrir uno de los PDF.', running: (label: string) => `${label} en este dispositivo...`, complete: (label: string) => `${label} completado. No se subió ningún PDF.`, operationFail: 'Falló la operación PDF. Es posible que no se admitan archivos dañados o con contraseña.', framed: (name: string) => `Encuadre automático aplicado a ${name}. Revisa antes de exportar.`, noFrame: (name: string) => `No se encontró un marco fiable para ${name}; no se modificó.`, finding: (n: number) => `Buscando bordes en ${n} página(s) localmente...`, framedAll: (found: number, total: number) => `Encuadre aplicado a ${found}/${total} página(s). Las páginas dudosas no se modificaron.`, edgeFail: 'No se pudieron detectar los bordes. Las páginas no se modificaron.', merging: 'Uniendo PDF', extracting: 'Extrayendo páginas', optimizing: 'Optimizando PDF', signing: 'Añadiendo firma visual' },
} as const;

function safeName(value: string) {
  return (
    value
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "") || "purehub-document"
  );
}

async function estimateSymmetricCrop(url: string) {
  const image = await new Promise<HTMLImageElement>((resolve, reject) => {
    const node = new Image(); node.onload = () => resolve(node); node.onerror = reject; node.src = url;
  });
  const scale = Math.min(1, 360 / Math.max(image.naturalWidth, image.naturalHeight));
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(24, Math.round(image.naturalWidth * scale));
  canvas.height = Math.max(24, Math.round(image.naturalHeight * scale));
  const context = canvas.getContext("2d", { willReadFrequently: true });
  if (!context) return 0;
  context.drawImage(image, 0, 0, canvas.width, canvas.height);
  const data = context.getImageData(0, 0, canvas.width, canvas.height).data;
  const luminance = (x: number, y: number) => {
    const index = (y * canvas.width + x) * 4;
    return data[index] * .2126 + data[index + 1] * .7152 + data[index + 2] * .0722;
  };
  let borderTotal = 0; let borderCount = 0;
  for (let x = 0; x < canvas.width; x += 2) { borderTotal += luminance(x, 0) + luminance(x, canvas.height - 1); borderCount += 2; }
  for (let y = 1; y < canvas.height - 1; y += 2) { borderTotal += luminance(0, y) + luminance(canvas.width - 1, y); borderCount += 2; }
  const background = borderTotal / Math.max(1, borderCount);
  const rowScore = (y: number) => { let changed = 0; let count = 0; for (let x = 0; x < canvas.width; x += 3) { if (Math.abs(luminance(x, y) - background) > 30) changed += 1; count += 1; } return changed / count; };
  const columnScore = (x: number) => { let changed = 0; let count = 0; for (let y = 0; y < canvas.height; y += 3) { if (Math.abs(luminance(x, y) - background) > 30) changed += 1; count += 1; } return changed / count; };
  let top = 0; while (top < canvas.height * .28 && rowScore(top) < .18) top += 2;
  let bottom = canvas.height - 1; while (bottom > canvas.height * .72 && rowScore(bottom) < .18) bottom -= 2;
  let left = 0; while (left < canvas.width * .28 && columnScore(left) < .18) left += 2;
  let right = canvas.width - 1; while (right > canvas.width * .72 && columnScore(right) < .18) right -= 2;
  const crop = Math.min(left / canvas.width, top / canvas.height, (canvas.width - 1 - right) / canvas.width, (canvas.height - 1 - bottom) / canvas.height);
  return crop >= .012 && crop <= .12 ? Math.max(0, crop - .01) : 0;
}

export default function DocumentSuiteSurface() {
  const locale = normalizeLocale(window.location.pathname.split("/")[1]);
  const ui = DOC_UI[locale];
  const more = DOC_MORE[locale];
  const dynamic = DOC_DYNAMIC[locale];
  const [pages, setPages] = useState<Page[]>([]);
  const [title, setTitle] = useState<string>(ui.myDocument);
  const [quality, setQuality] = useState(0.86);
  const [status, setStatus] = useState<string>(ui.initial);
  const [busy, setBusy] = useState(false);
  const [pdfFiles, setPdfFiles] = useState<ImportedPdf[]>([]);
  const [rangeStart, setRangeStart] = useState("1");
  const [rangeEnd, setRangeEnd] = useState("1");
  const [signer, setSigner] = useState("");
  const [signaturePage, setSignaturePage] = useState("1");
  const [draggingId, setDraggingId] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const cameraRef = useRef<HTMLInputElement>(null);
  const pdfInputRef = useRef<HTMLInputElement>(null);
  const pagesRef = useRef<Page[]>([]);
  useEffect(() => {
    pagesRef.current = pages;
  }, [pages]);
  useEffect(
    () => () =>
      pagesRef.current.forEach((page) => URL.revokeObjectURL(page.url)),
    [],
  );
  useEffect(() => {
    const raw = localStorage.getItem(DOCUMENT_HANDOFF_KEY);
    if (!raw) return;
    localStorage.removeItem(DOCUMENT_HANDOFF_KEY);
    void (async () => {
      try {
        const handoff = JSON.parse(raw) as {
          title?: string;
          pages?: Array<{ dataUrl: string; text: string }>;
        };
        const imported = await Promise.all(
          (handoff.pages ?? []).slice(0, 20).map(async (item, index) => {
            const blob = await (await fetch(item.dataUrl)).blob();
            const file = new File([blob], `ocr-page-${index + 1}.jpg`, {
              type: blob.type || "image/jpeg",
            });
            return {
              id: crypto.randomUUID(),
              file,
              url: URL.createObjectURL(file),
              rotation: 0,
              fit: "contain" as const,
              crop: 0,
              filter: "original" as const,
              recognizedText: item.text,
            };
          }),
        );
        if (imported.length) {
          setPages(imported);
          setTitle(handoff.title || more.searchable);
          setStatus(dynamic.imported(imported.length));
        }
      } catch {
        setStatus(dynamic.handoffFail);
      }
    })();
  }, [dynamic, more.searchable]);

  const addFiles = (files: FileList | null) => {
    const accepted = Array.from(files ?? [])
      .filter((file) => file.type.startsWith("image/"))
      .slice(0, 20 - pages.length);
    if (!accepted.length) return;
    setPages((current) => [
      ...current,
      ...accepted.map((file) => ({
        id: crypto.randomUUID(),
        file,
        url: URL.createObjectURL(file),
        rotation: 0,
        fit: "contain" as const,
        crop: 0,
        filter: "original" as const,
      })),
    ]);
    setStatus(dynamic.staged(pages.length + accepted.length));
  };
  const addPdfs = async (files: FileList | null) => {
    const selected = Array.from(files ?? [])
      .filter((file) => file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf"))
      .slice(0, 8 - pdfFiles.length);
    if (!selected.length) return;
    setBusy(true);
    setStatus(dynamic.opening(selected.length));
    try {
      const { PDFDocument } = await import("pdf-lib");
      const opened: ImportedPdf[] = [];
      for (const file of selected) {
        if (file.size > 60 * 1024 * 1024) throw new Error(`${file.name} is larger than the 60 MB safety limit.`);
        const source = await PDFDocument.load(await file.arrayBuffer());
        opened.push({ id: crypto.randomUUID(), file, pageCount: source.getPageCount() });
      }
      setPdfFiles((current) => [...current, ...opened]);
      if (!pdfFiles.length && opened[0]) setRangeEnd(String(opened[0].pageCount));
      setStatus(dynamic.opened(opened.length));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : dynamic.openFail);
    } finally {
      setBusy(false);
    }
  };
  const downloadPdfBytes = (bytes: Uint8Array, suffix: string) => {
    const blob = new Blob([bytes.slice().buffer], { type: "application/pdf" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${safeName(title)}-${suffix}.pdf`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 800);
  };
  const runPdfTool = async (label: string, operation: () => Promise<Uint8Array>, suffix: string) => {
    setBusy(true);
    setStatus(dynamic.running(label));
    try {
      const bytes = await operation();
      downloadPdfBytes(bytes, suffix);
      setStatus(dynamic.complete(label));
    } catch {
      setStatus(dynamic.operationFail);
    } finally {
      setBusy(false);
    }
  };
  const mergePdfs = () => runPdfTool(dynamic.merging, async () => {
    const { PDFDocument } = await import("pdf-lib");
    const output = await PDFDocument.create();
    for (const item of pdfFiles) {
      const source = await PDFDocument.load(await item.file.arrayBuffer());
      const copied = await output.copyPages(source, source.getPageIndices());
      copied.forEach((page) => output.addPage(page));
    }
    output.setTitle(title); output.setCreator("PureHub Document Suite");
    return output.save({ useObjectStreams: true });
  }, "merged");
  const extractPages = () => runPdfTool(dynamic.extracting, async () => {
    const { PDFDocument } = await import("pdf-lib");
    const item = pdfFiles[0];
    const source = await PDFDocument.load(await item.file.arrayBuffer());
    const first = Math.max(1, Math.min(source.getPageCount(), Number(rangeStart) || 1));
    const last = Math.max(first, Math.min(source.getPageCount(), Number(rangeEnd) || source.getPageCount()));
    const output = await PDFDocument.create();
    const copied = await output.copyPages(source, Array.from({ length: last - first + 1 }, (_, index) => first - 1 + index));
    copied.forEach((page) => output.addPage(page));
    return output.save({ useObjectStreams: true });
  }, "pages");
  const compressPdf = () => runPdfTool(dynamic.optimizing, async () => {
    const { PDFDocument } = await import("pdf-lib");
    const source = await PDFDocument.load(await pdfFiles[0].file.arrayBuffer(), { updateMetadata: false });
    source.setProducer("PureHub local PDF optimizer");
    return source.save({ useObjectStreams: true, addDefaultPage: false, objectsPerTick: 40 });
  }, "compressed");
  const signPdf = () => runPdfTool(dynamic.signing, async () => {
    const { PDFDocument, StandardFonts, rgb } = await import("pdf-lib");
    const source = await PDFDocument.load(await pdfFiles[0].file.arrayBuffer());
    const index = Math.max(0, Math.min(source.getPageCount() - 1, (Number(signaturePage) || 1) - 1));
    const page = source.getPage(index);
    const font = await source.embedFont(StandardFonts.HelveticaOblique);
    const label = signer.trim().slice(0, 42);
    const width = Math.min(220, page.getWidth() * .42);
    const x = page.getWidth() - width - 28;
    const y = 30;
    page.drawRectangle({ x, y, width, height: 70, borderColor: rgb(.06, .46, .43), borderWidth: 1.2, opacity: .96 });
    page.drawText("SIGNED LOCALLY BY PUREHUB", { x: x + 10, y: y + 51, size: 7, color: rgb(.06, .46, .43) });
    page.drawText(label, { x: x + 10, y: y + 27, size: 17, font, color: rgb(.06, .09, .16), maxWidth: width - 20 });
    page.drawText(new Date().toISOString().slice(0, 10), { x: x + 10, y: y + 9, size: 7, color: rgb(.06, .46, .43) });
    return source.save({ useObjectStreams: true });
  }, "signed");
  const update = (id: string, values: Partial<Page>) =>
    setPages((current) =>
      current.map((page) => (page.id === id ? { ...page, ...values } : page)),
    );
  const move = (index: number, direction: -1 | 1) =>
    setPages((current) => {
      const next = [...current];
      const target = index + direction;
      if (target < 0 || target >= next.length) return current;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  const remove = (id: string) =>
    setPages((current) => {
      const target = current.find((page) => page.id === id);
      if (target) URL.revokeObjectURL(target.url);
      return current.filter((page) => page.id !== id);
    });
  const dropBefore = (targetId: string) =>
    setPages((current) => {
      const from = current.findIndex((page) => page.id === draggingId);
      const to = current.findIndex((page) => page.id === targetId);
      if (from < 0 || to < 0 || from === to) return current;
      const next = [...current];
      const [page] = next.splice(from, 1);
      next.splice(to, 0, page);
      return next;
    });
  const estimatedBytes = Math.round(
    pages.reduce((sum, page) => sum + page.file.size, 0) * quality * 0.72,
  );
  const autoCropPage = async (page: Page) => {
    const crop = await estimateSymmetricCrop(page.url);
    update(page.id, { crop, filter: crop > 0 ? "document" : page.filter });
    setStatus(crop > 0 ? dynamic.framed(page.file.name) : dynamic.noFrame(page.file.name));
  };
  const autoCropAll = async () => {
    setBusy(true);
    setStatus(dynamic.finding(pages.length));
    try {
      const crops = await Promise.all(
        pages.map((page) => estimateSymmetricCrop(page.url)),
      );
      setPages((current) =>
        current.map((page, index) => ({
          ...page,
          crop: crops[index] ?? page.crop,
          filter:
            (crops[index] ?? 0) > 0 ? "document" : page.filter,
        })),
      );
      setStatus(dynamic.framedAll(crops.filter((value) => value > 0).length, pages.length));
    } catch {
      setStatus(dynamic.edgeFail);
    } finally {
      setBusy(false);
    }
  };

  const exportPdf = async () => {
    if (!pages.length) {
      setStatus(ui.atLeast);
      return;
    }
    setBusy(true);
    setStatus(ui.buildingStatus);
    try {
      const { jsPDF } = await import("jspdf");
      const pdf = new jsPDF({
        unit: "pt",
        format: "a4",
        orientation: "portrait",
        compress: true,
      });
      for (let index = 0; index < pages.length; index += 1) {
        const page = pages[index];
        const image = await new Promise<HTMLImageElement>((resolve, reject) => {
          const node = new Image();
          node.onload = () => resolve(node);
          node.onerror = reject;
          node.src = page.url;
        });
        const rotated = page.rotation % 180 !== 0;
        const canvas = document.createElement("canvas");
        canvas.width = rotated ? image.naturalHeight : image.naturalWidth;
        canvas.height = rotated ? image.naturalWidth : image.naturalHeight;
        const context = canvas.getContext("2d");
        if (!context) throw new Error("Canvas unavailable");
        const cropX = image.naturalWidth * page.crop;
        const cropY = image.naturalHeight * page.crop;
        const sourceWidth = image.naturalWidth - cropX * 2;
        const sourceHeight = image.naturalHeight - cropY * 2;
        context.filter =
          page.filter === "document"
            ? "grayscale(1) contrast(1.45) brightness(1.08)"
            : "none";
        context.translate(canvas.width / 2, canvas.height / 2);
        context.rotate((page.rotation * Math.PI) / 180);
        context.drawImage(
          image,
          cropX,
          cropY,
          sourceWidth,
          sourceHeight,
          -image.naturalWidth / 2,
          -image.naturalHeight / 2,
          image.naturalWidth,
          image.naturalHeight,
        );
        const data = canvas.toDataURL("image/jpeg", quality);
        if (index) pdf.addPage();
        const width = pdf.internal.pageSize.getWidth() - 48;
        const height = pdf.internal.pageSize.getHeight() - 48;
        const ratio =
          page.fit === "cover"
            ? Math.max(width / canvas.width, height / canvas.height)
            : Math.min(width / canvas.width, height / canvas.height);
        const drawWidth = canvas.width * ratio;
        const drawHeight = canvas.height * ratio;
        pdf.addImage(
          data,
          "JPEG",
          (pdf.internal.pageSize.getWidth() - drawWidth) / 2,
          (pdf.internal.pageSize.getHeight() - drawHeight) / 2,
          drawWidth,
          drawHeight,
        );
        if (page.recognizedText) {
          pdf.setFontSize(1);
          pdf.setTextColor(255, 255, 255);
          const searchableLines = pdf.splitTextToSize(
            page.recognizedText,
            pdf.internal.pageSize.getWidth() - 48,
          ) as string[];
          pdf.text(searchableLines.slice(0, 500), 24, 24);
          pdf.setTextColor(0, 0, 0);
        }
      }
      pdf.setProperties({ title, creator: "PureHub Document Suite" });
      pdf.save(`${safeName(title)}.pdf`);
      setStatus(ui.exported);
      markToolSuccess("doc-to-pdf", {
        headline: "PDF exported locally",
        detail: `${pages.length} page${pages.length === 1 ? "" : "s"} were packaged into a private PDF.`,
        shareText: "I exported a PDF locally with PureHub.",
      });
    } catch {
      setStatus(ui.failed);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <header className="bg-gradient-to-br from-violet-50 via-white to-sky-50 p-5 dark:from-violet-950/40 dark:via-slate-900 dark:to-sky-950/30">
        <div className="flex items-start gap-3">
          <span className="grid size-12 place-items-center rounded-2xl bg-violet-700 text-white dark:bg-violet-300 dark:text-slate-950">
            <FileText className="size-6" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-black tracking-[.2em] text-violet-700 dark:text-violet-300">
              {ui.eyebrow}
            </p>
            <h2 className="text-2xl font-black text-slate-950 dark:text-white">
              Doc to PDF
            </h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
              {ui.description}
            </p>
          </div>
          <ShieldCheck className="hidden size-5 text-emerald-600 sm:block" />
        </div>
        <div className="mt-4 grid grid-cols-2 gap-2">
          <div className="rounded-xl bg-white/80 p-3 dark:bg-slate-800">
            <strong className="block text-xl">{pages.length}</strong>
            <span className="text-xs text-slate-500">{ui.staged}</span>
          </div>
          <a
            href={buildMiniAppPath(locale, "ocr-text")}
            className="flex items-center justify-center gap-2 rounded-xl border border-violet-200 bg-white/80 p-3 text-sm font-black text-violet-800 dark:border-violet-800 dark:bg-slate-800 dark:text-violet-200"
          >
            <ScanText className="size-4" />
            {ui.openOcr}
          </a>
        </div>
      </header>
      <div className="space-y-4 p-4 sm:p-5">
        <FormInput
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder={ui.title}
          aria-label={ui.title}
        />
        <section className="space-y-3 rounded-2xl border border-violet-200 bg-violet-50/60 p-3.5 dark:border-violet-900 dark:bg-violet-950/25">
          <div><strong className="flex items-center gap-2"><Files className="size-4 text-violet-700" />{ui.toolbox}</strong><p className="mt-1 text-xs leading-5 text-slate-600 dark:text-slate-300">{ui.toolboxHint}</p></div>
          <div className="grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={busy || pdfFiles.length >= 8} onClick={() => pdfInputRef.current?.click()}>{more.addPdfs}</ActionButton><ActionButton tone="muted" disabled={busy || pdfFiles.length < 2} onClick={() => void mergePdfs()}><Files className="size-4" />{more.merge}</ActionButton></div>
          <input ref={pdfInputRef} hidden multiple type="file" accept="application/pdf,.pdf" onChange={(event) => { void addPdfs(event.target.files); event.target.value = ""; }} />
          {pdfFiles.map((item, index) => <div key={item.id} className="flex items-center gap-2 rounded-xl bg-white p-2.5 text-xs dark:bg-slate-900"><FileText className="size-4 shrink-0 text-violet-600" /><span className="min-w-0 flex-1 truncate"><b>{index + 1}. {item.file.name}</b><br />{item.pageCount} {more.pages} · {Math.ceil(item.file.size / 1024)} KB</span><button title={more.removePdf} aria-label={more.removePdf} onClick={() => setPdfFiles((current) => current.filter((row) => row.id !== item.id))} className="grid size-8 place-items-center text-rose-600"><Trash2 className="size-4" /></button></div>)}
          {pdfFiles[0] ? <><div className="grid grid-cols-2 gap-2"><FormInput inputMode="numeric" value={rangeStart} onChange={(event) => setRangeStart(event.target.value.replace(/\D/g, "").slice(0, 4))} placeholder={more.from} aria-label={more.from} /><FormInput inputMode="numeric" value={rangeEnd} onChange={(event) => setRangeEnd(event.target.value.replace(/\D/g, "").slice(0, 4))} placeholder={more.to} aria-label={more.to} /></div><div className="grid grid-cols-2 gap-2"><ActionButton tone="muted" disabled={busy} onClick={() => void extractPages()}><Scissors className="size-4" />{more.extract}</ActionButton><ActionButton tone="muted" disabled={busy} onClick={() => void compressPdf()}><Minimize2 className="size-4" />{more.compress}</ActionButton></div><div className="grid grid-cols-[1fr_5rem] gap-2"><FormInput value={signer} onChange={(event) => setSigner(event.target.value.slice(0, 42))} placeholder={more.signer} aria-label={more.signer} /><FormInput inputMode="numeric" value={signaturePage} onChange={(event) => setSignaturePage(event.target.value.replace(/\D/g, "").slice(0, 4))} placeholder={more.page} aria-label={more.signaturePage} /></div><ActionButton className="w-full justify-center" disabled={busy || !signer.trim()} onClick={() => void signPdf()}><PenLine className="size-4" />{more.sign}</ActionButton><p className="text-[11px] leading-4 text-slate-500">{more.signatureHint}</p></> : null}
        </section>
        <div className="grid grid-cols-2 gap-2">
          <ActionButton onClick={() => cameraRef.current?.click()}>
            <Camera className="mr-2 inline size-4" />
            {ui.capture}
          </ActionButton>
          <ActionButton tone="muted" onClick={() => inputRef.current?.click()}>
            <ImagePlus className="mr-2 inline size-4" />
            {ui.images}
          </ActionButton>
          <input
            ref={cameraRef}
            hidden
            type="file"
            accept="image/*"
            capture="environment"
            onChange={(event) => {
              addFiles(event.target.files);
              event.target.value = "";
            }}
          />
          <input
            ref={inputRef}
            hidden
            multiple
            type="file"
            accept="image/*"
            onChange={(event) => {
              addFiles(event.target.files);
              event.target.value = "";
            }}
          />
        </div>
        {pages.length ? (
          <>
          <div className="flex items-center justify-between gap-3 rounded-xl bg-violet-50 p-3 dark:bg-violet-950/30">
            <span className="text-xs font-bold text-violet-800 dark:text-violet-200">{ui.cleanHint}</span>
            <div className="flex shrink-0 gap-2"><button type="button" disabled={busy} onClick={() => void autoCropAll()} className="rounded-lg border border-violet-300 bg-white px-3 py-2 text-xs font-black text-violet-800">{ui.cropAll}</button><button type="button" onClick={() => setPages((current) => current.map((page) => ({ ...page, filter: "document" })))} className="rounded-lg bg-violet-700 px-3 py-2 text-xs font-black text-white">{ui.cleanAll}</button></div>
          </div>
          <div className="space-y-2">
            {pages.map((page, index) => (
              <article
                key={page.id}
                draggable
                onDragStart={() => setDraggingId(page.id)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => {
                  dropBefore(page.id);
                  setDraggingId("");
                }}
                className={`rounded-2xl border p-2.5 transition dark:border-slate-700 ${draggingId === page.id ? "border-violet-400 opacity-60" : "border-slate-200"}`}
              >
                <div className="flex items-center gap-3">
                  <img
                    src={page.url}
                    alt={`Page ${index + 1}`}
                    className={`size-16 rounded-xl bg-slate-100 object-cover dark:bg-slate-950 ${page.filter === "document" ? "grayscale contrast-125" : ""}`}
                  />
                  <div className="min-w-0 flex-1">
                    <strong className="block truncate text-sm">
                      Page {index + 1} · {page.file.name}
                    </strong>
                    <div className="mt-2 flex flex-wrap gap-1">
                      <button
                        title={more.moveUp}
                        aria-label={more.moveUp}
                        onClick={() => move(index, -1)}
                        className="grid size-8 place-items-center rounded-lg bg-slate-100 dark:bg-slate-800"
                      >
                        <ArrowUp className="size-3.5" />
                      </button>
                      <button
                        title={more.moveDown}
                        aria-label={more.moveDown}
                        onClick={() => move(index, 1)}
                        className="grid size-8 place-items-center rounded-lg bg-slate-100 dark:bg-slate-800"
                      >
                        <ArrowDown className="size-3.5" />
                      </button>
                      <button
                        onClick={() =>
                          update(page.id, {
                            rotation: (page.rotation + 90) % 360,
                          })
                        }
                        className="rounded-lg bg-slate-100 px-2 text-xs font-bold dark:bg-slate-800"
                      >
                        {more.rotate} {page.rotation}°
                      </button>
                      <button
                        onClick={() =>
                          update(page.id, {
                            fit: page.fit === "contain" ? "cover" : "contain",
                          })
                        }
                        className="rounded-lg bg-slate-100 px-2 text-xs font-bold dark:bg-slate-800"
                      >
                        {page.fit}
                      </button>
                      <button
                        onClick={() => update(page.id, { filter: page.filter === "document" ? "original" : "document" })}
                        className="rounded-lg bg-slate-100 px-2 text-xs font-bold dark:bg-slate-800"
                      >
                        {page.filter === "document" ? more.clean : more.original}
                      </button>
                      <button onClick={() => void autoCropPage(page)} className="rounded-lg bg-violet-100 px-2 text-xs font-bold text-violet-900 dark:bg-violet-950 dark:text-violet-100">{more.autoCrop}</button>
                    </div>
                  </div>
                  <button
                    title={more.removePage}
                    aria-label={more.removePage}
                    onClick={() => remove(page.id)}
                    className="grid size-9 place-items-center rounded-xl text-rose-600 hover:bg-rose-50"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
                <label className="mt-2 block text-[11px] font-bold text-slate-500">
                  Edge crop {Math.round(page.crop * 100)}%
                  <input
                    className="mt-1 w-full accent-violet-600"
                    type="range"
                    min="0"
                    max="0.12"
                    step="0.01"
                    value={page.crop}
                    onChange={(event) =>
                      update(page.id, { crop: Number(event.target.value) })
                    }
                  />
                </label>
              </article>
            ))}
          </div>
          </>
        ) : (
          <div className="grid min-h-56 place-items-center rounded-[22px] border border-dashed border-slate-300 bg-slate-50 text-center dark:border-slate-700 dark:bg-slate-950">
            <div>
              <FileImage className="mx-auto size-12 text-violet-500" />
              <strong className="mt-3 block">{ui.empty}</strong>
              <p className="mt-1 text-sm text-slate-500">
                {ui.emptyHint}
              </p>
            </div>
          </div>
        )}
        <label className="block text-sm font-black">
          {ui.quality}{" "}
          <span className="float-right text-violet-700 dark:text-violet-300">
            {Math.round(quality * 100)}%
          </span>
          <input
            className="mt-2 w-full accent-violet-600"
            type="range"
            min=".55"
            max=".95"
            step=".05"
            value={quality}
            onChange={(event) => setQuality(Number(event.target.value))}
          />
        </label>
        {pages.length ? (
          <p className="rounded-xl bg-violet-50 p-3 text-xs font-bold text-violet-800 dark:bg-violet-950/35 dark:text-violet-200">
            Drag pages to reorder · estimated PDF size{" "}
            {estimatedBytes < 1_000_000
              ? `${Math.max(1, Math.round(estimatedBytes / 1000))} KB`
              : `${(estimatedBytes / 1_000_000).toFixed(1)} MB`}
          </p>
        ) : null}
        <ActionButton
          className="flex min-h-14 w-full items-center justify-center"
          disabled={!pages.length || busy}
          onClick={() => void exportPdf()}
        >
          <Download className="mr-2 inline size-5" />
          {busy ? ui.building : `${ui.export} ${pages.length || ""}`}
        </ActionButton>
        <p
          role="status"
          className="text-sm font-semibold text-slate-600 dark:text-slate-300"
        >
          {status}
        </p>
        <p className="rounded-xl bg-emerald-50 p-3 text-xs leading-5 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">
          {ui.workflow}
        </p>
      </div>
    </section>
  );
}

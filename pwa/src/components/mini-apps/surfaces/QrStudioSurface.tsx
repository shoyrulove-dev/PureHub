import { useEffect, useMemo, useRef, useState } from 'react'
import jsQR from 'jsqr'
import {
  Camera, Check, Clipboard, Download, ExternalLink, Flashlight, History, ImageUp,
  FolderOpen, NotebookPen, QrCode, RefreshCw, ScanLine, Search, Share2, ShieldCheck, ShoppingBag, Sparkles, Star, Trash2,
} from 'lucide-react'
import { ActionButton, FormInput, FormTextArea } from '../MiniAppPrimitives'
import { markToolSuccess } from '../../../lib/tool-success'
import { trackProductEvent } from '../../../lib/community-api'
import { normalizeLocale, type LocaleCode } from '../../../i18n/locales'

const QR_UI = {
  en: { title: 'QR Studio', private: 'Private by design', description: 'Scan, inspect, create, and save useful codes without uploads.', scan: 'Scan', create: 'Create', library: 'Library', cameraOff: 'Camera stays off until you start it', imageHint: 'You can also scan a screenshot or saved image.', libraryTitle: 'Private library', libraryHint: 'Stored only in this browser. Nothing is synced.', noSaved: 'No saved codes yet', noSavedHint: 'Scans and saved codes will appear here.' },
  vi: { title: 'Xưởng QR', private: 'Thiết kế riêng tư', description: 'Quét, kiểm tra, tạo và lưu mã mà không tải dữ liệu lên.', scan: 'Quét', create: 'Tạo mã', library: 'Thư viện', cameraOff: 'Camera chỉ bật khi bạn yêu cầu', imageHint: 'Bạn cũng có thể quét ảnh chụp màn hình hoặc ảnh đã lưu.', libraryTitle: 'Thư viện riêng tư', libraryHint: 'Chỉ lưu trong trình duyệt này, không đồng bộ.', noSaved: 'Chưa có mã đã lưu', noSavedHint: 'Mã quét và mã đã lưu sẽ xuất hiện tại đây.' },
  zh: { title: '二维码工坊', private: '隐私设计', description: '无需上传即可扫描、检查、创建并保存实用代码。', scan: '扫描', create: '创建', library: '资料库', cameraOff: '只有开始扫描后才会开启相机', imageHint: '也可以扫描截图或已保存的图片。', libraryTitle: '私密资料库', libraryHint: '仅保存在此浏览器中，不会同步。', noSaved: '尚无保存的代码', noSavedHint: '扫描和保存的代码会显示在这里。' },
  es: { title: 'Estudio QR', private: 'Privado por diseño', description: 'Escanea, revisa, crea y guarda códigos sin subir datos.', scan: 'Escanear', create: 'Crear', library: 'Biblioteca', cameraOff: 'La cámara permanece apagada hasta que la inicies', imageHint: 'También puedes escanear una captura o imagen guardada.', libraryTitle: 'Biblioteca privada', libraryHint: 'Solo se guarda en este navegador. No se sincroniza.', noSaved: 'Aún no hay códigos guardados', noSavedHint: 'Los códigos escaneados y guardados aparecerán aquí.' },
} as const

const QR_STATUS = {
  en: { ready: 'Ready when you are. Nothing is uploaded.', hold: 'Hold a code inside the frame. Detection is automatic.', unavailable: 'Camera unavailable. Allow permission or choose an image.', zoom: 'This camera does not expose zoom control in the browser.', reading: 'Reading the image locally…', none: 'No readable code found. Try a sharper image, more light, or a closer crop.', failed: 'The image could not be read. Try PNG, JPG, or a camera photo.' },
  vi: { ready: 'Sẵn sàng. Không có dữ liệu nào được tải lên.', hold: 'Đưa mã vào trong khung. Hệ thống sẽ tự động nhận diện.', unavailable: 'Không dùng được camera. Hãy cấp quyền hoặc chọn ảnh.', zoom: 'Camera này không cho trình duyệt điều khiển thu phóng.', reading: 'Đang đọc ảnh trên thiết bị…', none: 'Không tìm thấy mã rõ. Hãy dùng ảnh nét hơn, đủ sáng hoặc cắt gần hơn.', failed: 'Không đọc được ảnh. Hãy thử PNG, JPG hoặc ảnh chụp từ camera.' },
  zh: { ready: '准备就绪，不会上传任何数据。', hold: '将代码放入框内，系统会自动识别。', unavailable: '相机不可用，请授权或选择图片。', zoom: '此相机不允许浏览器控制缩放。', reading: '正在本地读取图片…', none: '未找到可读代码，请使用更清晰、更明亮或裁剪更近的图片。', failed: '无法读取图片，请尝试 PNG、JPG 或相机照片。' },
  es: { ready: 'Listo. No se sube ningún dato.', hold: 'Coloca el código dentro del marco. La detección es automática.', unavailable: 'Cámara no disponible. Concede permiso o elige una imagen.', zoom: 'Esta cámara no permite controlar el zoom desde el navegador.', reading: 'Leyendo la imagen localmente…', none: 'No se encontró un código legible. Prueba una imagen más nítida, con más luz o un recorte cercano.', failed: 'No se pudo leer la imagen. Prueba PNG, JPG o una foto de cámara.' },
} as const

const QR_ACTIONS = {
  en: { stop: 'Stop', start: 'Start camera', image: 'Choose image', batch: 'Batch', torch: 'Torch', torchOff: 'Torch off', saveHistory: 'Save scan history', saveHint: 'Turn off for a private session with no QR payload retained.', zoom: 'Camera zoom', local: 'local', copy: 'Copy', copied: 'Copied', share: 'Share', another: 'Scan another', createTitle: 'Create a code', createHint: 'Pick a format and fill in only the information people need.', more: 'More types', less: 'Less types', search: 'Search codes', all: 'All', scanned: 'Scanned', created: 'Created', noMatch: 'No matching codes.', exportLibrary: 'Export library', clearLibrary: 'Clear library' },
  vi: { stop: 'Dừng', start: 'Mở camera', image: 'Chọn ảnh', batch: 'Quét nhiều ảnh', torch: 'Đèn pin', torchOff: 'Tắt đèn', saveHistory: 'Lưu lịch sử quét', saveHint: 'Tắt để không lưu nội dung QR của phiên riêng tư này.', zoom: 'Thu phóng camera', local: 'trên máy', copy: 'Sao chép', copied: 'Đã sao chép', share: 'Chia sẻ', another: 'Quét mã khác', createTitle: 'Tạo mã', createHint: 'Chọn định dạng và chỉ nhập thông tin cần thiết.', more: 'Thêm loại', less: 'Thu gọn', search: 'Tìm mã', all: 'Tất cả', scanned: 'Đã quét', created: 'Đã tạo', noMatch: 'Không có mã phù hợp.', exportLibrary: 'Xuất thư viện', clearLibrary: 'Xóa thư viện' },
  zh: { stop: '停止', start: '打开相机', image: '选择图片', batch: '批量扫描', torch: '手电筒', torchOff: '关闭手电筒', saveHistory: '保存扫描历史', saveHint: '关闭后，本次私密会话不会保留二维码内容。', zoom: '相机缩放', local: '本机', copy: '复制', copied: '已复制', share: '分享', another: '继续扫描', createTitle: '创建代码', createHint: '选择格式，只填写必要信息。', more: '更多类型', less: '收起类型', search: '搜索代码', all: '全部', scanned: '已扫描', created: '已创建', noMatch: '没有匹配的代码。', exportLibrary: '导出资料库', clearLibrary: '清空资料库' },
  es: { stop: 'Detener', start: 'Abrir cámara', image: 'Elegir imagen', batch: 'Escaneo múltiple', torch: 'Linterna', torchOff: 'Apagar linterna', saveHistory: 'Guardar historial', saveHint: 'Desactívalo para no conservar el contenido QR de esta sesión privada.', zoom: 'Zoom de cámara', local: 'local', copy: 'Copiar', copied: 'Copiado', share: 'Compartir', another: 'Escanear otro', createTitle: 'Crear un código', createHint: 'Elige un formato y completa solo la información necesaria.', more: 'Más tipos', less: 'Menos tipos', search: 'Buscar códigos', all: 'Todos', scanned: 'Escaneados', created: 'Creados', noMatch: 'No hay códigos coincidentes.', exportLibrary: 'Exportar biblioteca', clearLibrary: 'Vaciar biblioteca' },
} as const

const QR_DYNAMIC = {
  en: { complete: (source: string, format: string) => `${source} ${format} scan complete. Review the result before taking action.`, reading: (count: number) => `Reading ${count} image(s) locally...`, found: (found: number, total: number, saved: boolean) => `${found} code(s) found in ${total} image(s). ${saved ? 'Saved to your private library.' : 'Kept only for this private session.'}`, batchDone: (saved: boolean) => `Batch scan complete. Review the first result${saved ? ' or open the library' : ''}.`, none: 'No readable codes were found in these images.', another: 'Ready for another scan.' },
  vi: { complete: (source: string, format: string) => `Đã quét ${format} từ ${source}. Hãy kiểm tra kết quả trước khi thao tác.`, reading: (count: number) => `Đang đọc ${count} ảnh trên thiết bị...`, found: (found: number, total: number, saved: boolean) => `Tìm thấy ${found} mã trong ${total} ảnh. ${saved ? 'Đã lưu vào thư viện riêng tư.' : 'Chỉ giữ trong phiên riêng tư này.'}`, batchDone: (saved: boolean) => `Đã quét xong. Hãy xem kết quả đầu tiên${saved ? ' hoặc mở thư viện' : ''}.`, none: 'Không tìm thấy mã nào có thể đọc trong các ảnh này.', another: 'Sẵn sàng quét mã khác.' },
  zh: { complete: (source: string, format: string) => `已完成从${source}扫描 ${format}。操作前请检查结果。`, reading: (count: number) => `正在本机读取 ${count} 张图片...`, found: (found: number, total: number, saved: boolean) => `在 ${total} 张图片中找到 ${found} 个代码。${saved ? '已保存到私密资料库。' : '仅保留在本次私密会话中。'}`, batchDone: (saved: boolean) => `批量扫描完成。请检查第一个结果${saved ? '或打开资料库' : ''}。`, none: '这些图片中没有可读取的代码。', another: '可以继续扫描。' },
  es: { complete: (source: string, format: string) => `Escaneo ${format} desde ${source} completado. Revisa el resultado antes de continuar.`, reading: (count: number) => `Leyendo ${count} imagen(es) localmente...`, found: (found: number, total: number, saved: boolean) => `Se encontraron ${found} código(s) en ${total} imagen(es). ${saved ? 'Guardados en tu biblioteca privada.' : 'Conservados solo durante esta sesión privada.'}`, batchDone: (saved: boolean) => `Escaneo múltiple completado. Revisa el primer resultado${saved ? ' o abre la biblioteca' : ''}.`, none: 'No se encontraron códigos legibles en estas imágenes.', another: 'Listo para otro escaneo.' },
} as const

const QR_DEEP: Record<LocaleCode, Record<string, string>> = {
  en: {},
  vi: {
    'QR Studio sections': 'Các mục QR Studio', 'Website': 'Trang web', 'Text': 'Văn bản', 'Email': 'Email', 'Phone': 'Điện thoại', 'Location': 'Vị trí', 'Calendar': 'Lịch', 'Contact': 'Liên hệ',
    'Camera': 'Camera', 'Image': 'Ảnh', 'Created': 'Đã tạo', 'CODE': 'MÃ', 'Website result': 'Kết quả trang web', 'Wi-Fi network': 'Mạng Wi-Fi', 'Phone number': 'Số điện thoại', 'Message': 'Tin nhắn', 'Calendar event': 'Sự kiện lịch', 'Contact card': 'Danh thiếp', 'Product barcode': 'Mã vạch sản phẩm', 'Plain text': 'Văn bản thường', 'Invalid link': 'Liên kết không hợp lệ',
    'Library folder': 'Thư mục thư viện', 'Folder (optional)': 'Thư mục (không bắt buộc)', 'Private library note': 'Ghi chú riêng tư', 'Private note (optional)': 'Ghi chú (không bắt buộc)', 'Save details': 'Lưu chi tiết', 'Private session active · this QR payload is not in the library.': 'Phiên riêng tư đang bật · nội dung QR này không được lưu vào thư viện.',
    'Lookup after confirmation': 'Tra cứu sau khi xác nhận', 'Product lookup provider': 'Nguồn tra cứu sản phẩm', 'Compare prices': 'So sánh giá', 'Private web search': 'Tìm kiếm web riêng tư', 'Download calendar file': 'Tải tệp lịch', 'Save contact payload': 'Lưu dữ liệu liên hệ', 'Compare prices online': 'So sánh giá trực tuyến', 'Confirm external product lookup': 'Xác nhận tra cứu sản phẩm bên ngoài',
    'Write something useful': 'Nhập nội dung hữu ích', 'Network name': 'Tên mạng', 'Contact name': 'Tên liên hệ', 'Email address': 'Địa chỉ email', 'Coordinates': 'Tọa độ', 'Event title': 'Tên sự kiện', 'Website address': 'Địa chỉ trang web', 'Wi-Fi name': 'Tên Wi-Fi', 'Full name': 'Họ tên', 'Wi-Fi password': 'Mật khẩu Wi-Fi', 'Wi-Fi security': 'Bảo mật Wi-Fi', 'None': 'Không', 'Email subject': 'Tiêu đề email', 'Subject': 'Tiêu đề', 'Message (optional)': 'Tin nhắn (không bắt buộc)', 'Start time': 'Thời gian bắt đầu', 'Location (optional)': 'Vị trí (không bắt buộc)', 'Contact phone': 'Điện thoại liên hệ', 'Contact email': 'Email liên hệ',
    'Code': 'Mã', 'QR foreground color': 'Màu mã QR', 'Background': 'Nền', 'QR background color': 'Màu nền QR', 'Reset': 'Đặt lại', 'Self-tested locally · ready to scan': 'Đã tự kiểm tra trên máy · sẵn sàng quét', 'Checking generated code locally…': 'Đang kiểm tra mã trên máy…', 'Download': 'Tải xuống', 'Share': 'Chia sẻ', 'Save to library': 'Lưu vào thư viện', 'Pin code': 'Ghim mã', 'Unpin code': 'Bỏ ghim mã', 'Open website': 'Mở trang web', 'Open email': 'Mở email', 'Open dialer': 'Mở gọi điện', 'Open messages': 'Mở tin nhắn', 'Open map': 'Mở bản đồ', 'Search product online': 'Tìm sản phẩm trực tuyến', 'The link is not encrypted (HTTP).': 'Liên kết không được mã hóa (HTTP).', 'The destination uses a raw IP address.': 'Đích đến dùng địa chỉ IP trực tiếp.', 'The domain contains an internationalized/punycode name.': 'Tên miền chứa ký tự quốc tế/punycode.', 'The link embeds sign-in information.': 'Liên kết có chứa thông tin đăng nhập.', 'The link uses an unusual port.': 'Liên kết dùng cổng mạng bất thường.', 'The destination is unusually long.': 'Địa chỉ đích dài bất thường.', 'This looks like a web link but is not valid.': 'Đây có vẻ là liên kết web nhưng không hợp lệ.', 'Passwords remain visible in the raw QR content.': 'Mật khẩu vẫn hiển thị trong nội dung QR thô.', 'Product lookup opens a search engine and shares this barcode only after you confirm.': 'Tra cứu sản phẩm sẽ mở công cụ tìm kiếm và chỉ gửi mã vạch sau khi bạn xác nhận.',
  },
  zh: {
    'QR Studio sections': '二维码工坊栏目', 'Website': '网站', 'Text': '文本', 'Email': '电子邮件', 'Phone': '电话', 'Location': '位置', 'Calendar': '日历', 'Contact': '联系人',
    'Camera': '相机', 'Image': '图片', 'Created': '已创建', 'CODE': '代码', 'Website result': '网站结果', 'Wi-Fi network': 'Wi-Fi 网络', 'Phone number': '电话号码', 'Message': '短信', 'Calendar event': '日历事件', 'Contact card': '联系人卡片', 'Product barcode': '商品条码', 'Plain text': '纯文本', 'Invalid link': '无效链接',
    'Library folder': '资料库文件夹', 'Folder (optional)': '文件夹（可选）', 'Private library note': '私密备注', 'Private note (optional)': '私密备注（可选）', 'Save details': '保存详情', 'Private session active · this QR payload is not in the library.': '私密会话已启用 · 此二维码内容不会保存到资料库。',
    'Lookup after confirmation': '确认后查询', 'Product lookup provider': '商品查询服务', 'Compare prices': '比较价格', 'Private web search': '隐私网页搜索', 'Download calendar file': '下载日历文件', 'Save contact payload': '保存联系人数据', 'Compare prices online': '在线比较价格', 'Confirm external product lookup': '确认外部商品查询',
    'Write something useful': '输入有用内容', 'Network name': '网络名称', 'Contact name': '联系人姓名', 'Email address': '电子邮件地址', 'Coordinates': '坐标', 'Event title': '事件标题', 'Website address': '网站地址', 'Wi-Fi name': 'Wi-Fi 名称', 'Full name': '姓名', 'Wi-Fi password': 'Wi-Fi 密码', 'Wi-Fi security': 'Wi-Fi 安全类型', 'None': '无', 'Email subject': '邮件主题', 'Subject': '主题', 'Message (optional)': '短信（可选）', 'Start time': '开始时间', 'Location (optional)': '位置（可选）', 'Contact phone': '联系人电话', 'Contact email': '联系人邮箱',
    'Code': '二维码', 'QR foreground color': '二维码颜色', 'Background': '背景', 'QR background color': '二维码背景色', 'Reset': '重置', 'Self-tested locally · ready to scan': '已在本机自检 · 可以扫描', 'Checking generated code locally…': '正在本机检查生成的二维码…', 'Download': '下载', 'Share': '分享', 'Save to library': '保存到资料库', 'Pin code': '置顶代码', 'Unpin code': '取消置顶', 'Open website': '打开网站', 'Open email': '打开邮件', 'Open dialer': '打开拨号', 'Open messages': '打开短信', 'Open map': '打开地图', 'Search product online': '在线搜索商品', 'The link is not encrypted (HTTP).': '此链接未加密（HTTP）。', 'The destination uses a raw IP address.': '目标使用原始 IP 地址。', 'The domain contains an internationalized/punycode name.': '域名包含国际化/punycode 名称。', 'The link embeds sign-in information.': '链接中包含登录信息。', 'The link uses an unusual port.': '链接使用了非常规端口。', 'The destination is unusually long.': '目标地址异常长。', 'This looks like a web link but is not valid.': '这看起来像网页链接，但格式无效。', 'Passwords remain visible in the raw QR content.': '密码仍会显示在二维码原始内容中。', 'Product lookup opens a search engine and shares this barcode only after you confirm.': '商品查询会打开搜索引擎，仅在确认后发送此条码。',
  },
  es: {
    'QR Studio sections': 'Secciones de Estudio QR', 'Website': 'Sitio web', 'Text': 'Texto', 'Email': 'Correo', 'Phone': 'Teléfono', 'Location': 'Ubicación', 'Calendar': 'Calendario', 'Contact': 'Contacto',
    'Camera': 'Cámara', 'Image': 'Imagen', 'Created': 'Creado', 'CODE': 'CÓDIGO', 'Website result': 'Resultado web', 'Wi-Fi network': 'Red Wi-Fi', 'Phone number': 'Número de teléfono', 'Message': 'Mensaje', 'Calendar event': 'Evento de calendario', 'Contact card': 'Tarjeta de contacto', 'Product barcode': 'Código de producto', 'Plain text': 'Texto sin formato', 'Invalid link': 'Enlace no válido',
    'Library folder': 'Carpeta de biblioteca', 'Folder (optional)': 'Carpeta (opcional)', 'Private library note': 'Nota privada', 'Private note (optional)': 'Nota privada (opcional)', 'Save details': 'Guardar detalles', 'Private session active · this QR payload is not in the library.': 'Sesión privada activa · este contenido QR no se guarda en la biblioteca.',
    'Lookup after confirmation': 'Buscar tras confirmar', 'Product lookup provider': 'Proveedor de búsqueda', 'Compare prices': 'Comparar precios', 'Private web search': 'Búsqueda web privada', 'Download calendar file': 'Descargar calendario', 'Save contact payload': 'Guardar contacto', 'Compare prices online': 'Comparar precios en línea', 'Confirm external product lookup': 'Confirmar búsqueda externa',
    'Write something useful': 'Escribe algo útil', 'Network name': 'Nombre de red', 'Contact name': 'Nombre del contacto', 'Email address': 'Correo electrónico', 'Coordinates': 'Coordenadas', 'Event title': 'Título del evento', 'Website address': 'Dirección web', 'Wi-Fi name': 'Nombre de Wi-Fi', 'Full name': 'Nombre completo', 'Wi-Fi password': 'Contraseña Wi-Fi', 'Wi-Fi security': 'Seguridad Wi-Fi', 'None': 'Ninguna', 'Email subject': 'Asunto del correo', 'Subject': 'Asunto', 'Message (optional)': 'Mensaje (opcional)', 'Start time': 'Hora de inicio', 'Location (optional)': 'Ubicación (opcional)', 'Contact phone': 'Teléfono del contacto', 'Contact email': 'Correo del contacto',
    'Code': 'Código', 'QR foreground color': 'Color del código QR', 'Background': 'Fondo', 'QR background color': 'Color de fondo QR', 'Reset': 'Restablecer', 'Self-tested locally · ready to scan': 'Verificado localmente · listo para escanear', 'Checking generated code locally…': 'Comprobando el código localmente…', 'Download': 'Descargar', 'Share': 'Compartir', 'Save to library': 'Guardar en biblioteca', 'Pin code': 'Fijar código', 'Unpin code': 'Desfijar código', 'Open website': 'Abrir sitio web', 'Open email': 'Abrir correo', 'Open dialer': 'Abrir teléfono', 'Open messages': 'Abrir mensajes', 'Open map': 'Abrir mapa', 'Search product online': 'Buscar producto en línea', 'The link is not encrypted (HTTP).': 'El enlace no está cifrado (HTTP).', 'The destination uses a raw IP address.': 'El destino usa una dirección IP directa.', 'The domain contains an internationalized/punycode name.': 'El dominio contiene un nombre internacionalizado/punycode.', 'The link embeds sign-in information.': 'El enlace incluye datos de inicio de sesión.', 'The link uses an unusual port.': 'El enlace utiliza un puerto poco habitual.', 'The destination is unusually long.': 'La dirección de destino es inusualmente larga.', 'This looks like a web link but is not valid.': 'Parece un enlace web, pero no es válido.', 'Passwords remain visible in the raw QR content.': 'La contraseña sigue visible en el contenido QR sin procesar.', 'Product lookup opens a search engine and shares this barcode only after you confirm.': 'La búsqueda abre un buscador y comparte el código solo después de confirmar.',
  },
}

const QR_RESULT = {
  en: { scan: 'QR code scanned locally', scanSaved: 'The result is saved in your private library.', scanPrivate: 'The result is available only for this private session.', batch: 'Batch scan complete', exported: 'QR image exported', libraryExported: 'QR library exported', safe: 'Scan-safe contrast', low: 'Low contrast. Darken the code or lighten the background.' },
  vi: { scan: 'Đã quét QR trên thiết bị', scanSaved: 'Kết quả đã lưu trong thư viện riêng tư.', scanPrivate: 'Kết quả chỉ có trong phiên riêng tư này.', batch: 'Đã quét hàng loạt', exported: 'Đã xuất ảnh QR', libraryExported: 'Đã xuất thư viện QR', safe: 'Độ tương phản an toàn', low: 'Độ tương phản thấp. Hãy làm mã đậm hơn hoặc nền sáng hơn.' },
  zh: { scan: '已在本机扫描二维码', scanSaved: '结果已保存到私密资料库。', scanPrivate: '结果仅保留在本次私密会话中。', batch: '批量扫描完成', exported: '二维码图片已导出', libraryExported: '二维码资料库已导出', safe: '对比度适合扫描', low: '对比度偏低，请加深代码颜色或调亮背景。' },
  es: { scan: 'Código QR escaneado localmente', scanSaved: 'El resultado se guardó en tu biblioteca privada.', scanPrivate: 'El resultado solo está disponible en esta sesión privada.', batch: 'Escaneo múltiple completado', exported: 'Imagen QR exportada', libraryExported: 'Biblioteca QR exportada', safe: 'Contraste apto para escanear', low: 'Contraste bajo. Oscurece el código o aclara el fondo.' },
} as const

const STORAGE_KEY = 'purehub.qr-studio.history.v2'
const MAX_HISTORY = 120

type StudioTab = 'scan' | 'create' | 'library'
type ScanSource = 'Camera' | 'Image' | 'Created'
type ScanEntry = { value: string; savedAt: string; source: ScanSource; format?: string; pinned?: boolean; folder?: string; note?: string }
type QrTemplate = 'url' | 'text' | 'wifi' | 'email' | 'phone' | 'sms' | 'location' | 'calendar' | 'contact'
type CreatorFields = { primary: string; secondary: string; tertiary: string }
type DetectedCode = { value: string; format: string }
type NativeBarcode = { rawValue: string; format: string }
type NativeBarcodeDetector = { detect: (source: CanvasImageSource) => Promise<NativeBarcode[]> }
type NativeBarcodeDetectorConstructor = new (options?: { formats?: string[] }) => NativeBarcodeDetector

const templates: Record<QrTemplate, { label: string; value: string }> = {
  url: { label: 'Website', value: 'https://hub.blissbiovn.com' },
  text: { label: 'Text', value: 'PureHub — free, private, and ad-free tools' },
  wifi: { label: 'Wi-Fi', value: 'WIFI:T:WPA;S:Network name;P:Password;H:false;;' },
  email: { label: 'Email', value: 'mailto:hello@example.com?subject=Hello' },
  phone: { label: 'Phone', value: 'tel:+10000000000' },
  sms: { label: 'SMS', value: 'SMSTO:+10000000000:Hello' },
  location: { label: 'Location', value: 'geo:10.7769,106.7009' },
  calendar: { label: 'Calendar', value: 'BEGIN:VEVENT\nSUMMARY:PureHub event\nDTSTART:20260821T090000\nEND:VEVENT' },
  contact: { label: 'Contact', value: 'MECARD:N:PureHub;URL:https://hub.blissbiovn.com;;' },
}

const defaultFields: Record<QrTemplate, CreatorFields> = {
  url: { primary: 'https://hub.blissbiovn.com', secondary: '', tertiary: '' },
  text: { primary: 'PureHub — free, private, and ad-free tools', secondary: '', tertiary: '' },
  wifi: { primary: '', secondary: '', tertiary: 'WPA' },
  email: { primary: '', secondary: '', tertiary: '' },
  phone: { primary: '', secondary: '', tertiary: '' },
  sms: { primary: '', secondary: '', tertiary: '' },
  location: { primary: '', secondary: '', tertiary: '' },
  calendar: { primary: '', secondary: '', tertiary: '' },
  contact: { primary: '', secondary: '', tertiary: '' },
}

const escapeQrField = (value: string) => value.trim().replaceAll('\\', '\\\\').replaceAll(';', '\\;').replaceAll(',', '\\,').replaceAll(':', '\\:')

function buildQrValue(template: QrTemplate, fields: CreatorFields) {
  if (template === 'url' || template === 'text') return fields.primary.trim()
  if (template === 'wifi') return `WIFI:T:${fields.tertiary === 'None' ? '' : fields.tertiary};S:${escapeQrField(fields.primary)};P:${escapeQrField(fields.secondary)};H:false;;`
  if (template === 'email') return `mailto:${fields.primary.trim()}?subject=${encodeURIComponent(fields.secondary.trim())}`
  if (template === 'phone') return `tel:${fields.primary.replaceAll(/\s/g, '')}`
  if (template === 'sms') return `SMSTO:${fields.primary.replaceAll(/\s/g, '')}:${fields.secondary.trim()}`
  if (template === 'location') return `geo:${fields.primary.trim()}`
  if (template === 'calendar') return `BEGIN:VEVENT\nSUMMARY:${fields.primary.trim()}\nDTSTART:${fields.secondary.replaceAll(/[-:]/g, '').replace('T', 'T')}\n${fields.tertiary.trim() ? `LOCATION:${fields.tertiary.trim()}\n` : ''}END:VEVENT`
  return `MECARD:N:${escapeQrField(fields.primary)}${fields.secondary ? `;TEL:${escapeQrField(fields.secondary)}` : ''}${fields.tertiary ? `;EMAIL:${escapeQrField(fields.tertiary)}` : ''};;`
}

function loadHistory(): ScanEntry[] {
  try {
    const rows = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as Partial<ScanEntry>[]
    return rows.filter((row) => row.value).map((row) => ({
      value: String(row.value),
      savedAt: String(row.savedAt ?? new Date().toISOString()),
      source: row.source === 'Image' || row.source === 'Created' ? row.source : 'Camera',
      format: typeof row.format === 'string' ? row.format : undefined,
      pinned: Boolean(row.pinned),
      folder: typeof row.folder === 'string' ? row.folder.slice(0, 48) : '',
      note: typeof row.note === 'string' ? row.note.slice(0, 240) : '',
    }))
  } catch {
    return []
  }
}

let zxingReaderPromise: Promise<InstanceType<(typeof import('@zxing/browser'))['BrowserMultiFormatReader']>> | undefined

function nativeBarcodeDetector() {
  try {
    const Detector = (globalThis as typeof globalThis & { BarcodeDetector?: NativeBarcodeDetectorConstructor }).BarcodeDetector
    return Detector ? new Detector({ formats: ['qr_code', 'aztec', 'data_matrix', 'pdf417', 'code_128', 'code_39', 'code_93', 'ean_13', 'ean_8', 'upc_a', 'upc_e', 'itf'] }) : null
  } catch {
    // Some browsers expose BarcodeDetector but reject one or more requested formats.
    // Returning null guarantees the local jsQR/ZXing fallbacks still get a chance.
    return null
  }
}

async function decodeCanvas(canvas: HTMLCanvasElement, context: CanvasRenderingContext2D): Promise<DetectedCode | null> {
  const native = nativeBarcodeDetector()
  if (native) {
    try {
      const [result] = await native.detect(canvas)
      if (result?.rawValue) return { value: result.rawValue, format: result.format.replaceAll('_', ' ').toUpperCase() }
    } catch { /* Native support can be partial; continue with local fallbacks. */ }
  }
  const qr = jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })
  if (qr?.data) return { value: qr.data, format: 'QR CODE' }
  try {
    zxingReaderPromise ??= import('@zxing/browser').then(({ BrowserMultiFormatReader }) => new BrowserMultiFormatReader())
    const result = (await zxingReaderPromise).decodeFromCanvas(canvas)
    return result.getText() ? { value: result.getText(), format: result.getBarcodeFormat().toString().replaceAll('_', ' ') } : null
  } catch {
    return null
  }
}

async function decodeImage(file: File) {
  const bitmap = await createImageBitmap(file)
  const longest = Math.max(bitmap.width, bitmap.height)
  const scale = Math.min(1, 1600 / longest)
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bitmap.width * scale))
  canvas.height = Math.max(1, Math.round(bitmap.height * scale))
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return ''
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  return decodeCanvas(canvas, context)
}

function payloadDetails(value: string) {
  const trimmed = value.trim().replace(/^URL:/i, '').trim()
  const webCandidate = /^https?:\/\//i.test(trimmed) ? trimmed : /^(?:www\.|[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,}(?:[/:?#]|$)/i.test(trimmed) ? `https://${trimmed}` : ''
  if (webCandidate) {
    try {
      const url = new URL(webCandidate)
      const risks = [
        url.protocol !== 'https:' ? 'The link is not encrypted (HTTP).' : '',
        /^\d{1,3}(?:\.\d{1,3}){3}$/i.test(url.hostname) ? 'The destination uses a raw IP address.' : '',
        /xn--/i.test(url.hostname) ? 'The domain contains an internationalized/punycode name.' : '',
        url.username || url.password ? 'The link embeds sign-in information.' : '',
        url.port && !['80', '443'].includes(url.port) ? 'The link uses an unusual port.' : '',
        webCandidate.length > 500 ? 'The destination is unusually long.' : '',
      ].filter(Boolean)
      return {
        kind: 'Website',
        destination: url.href,
        action: 'Open website',
        warning: risks.length ? risks.join(' ') : '',
      }
    } catch {
      return { kind: 'Invalid link', destination: '', action: '', warning: 'This looks like a web link but is not valid.' }
    }
  }
  if (/^WIFI:/i.test(trimmed)) return { kind: 'Wi-Fi network', destination: '', action: '', warning: 'Passwords remain visible in the raw QR content.' }
  if (/^mailto:/i.test(trimmed)) return { kind: 'Email', destination: trimmed, action: 'Open email', warning: '' }
  if (/^tel:/i.test(trimmed)) return { kind: 'Phone number', destination: trimmed, action: 'Open dialer', warning: '' }
  if (/^sms:/i.test(trimmed)) return { kind: 'Message', destination: trimmed, action: 'Open messages', warning: '' }
  if (/^geo:/i.test(trimmed)) return { kind: 'Location', destination: trimmed, action: 'Open map', warning: '' }
  if (/^BEGIN:VEVENT/i.test(trimmed)) return { kind: 'Calendar event', destination: '', action: '', warning: '' }
  if (/^(MECARD:|BEGIN:VCARD)/i.test(trimmed)) return { kind: 'Contact card', destination: '', action: '', warning: '' }
  if (/^(?:\d{8}|\d{12,14})$/.test(trimmed)) return { kind: 'Product barcode', destination: `https://www.google.com/search?q=${encodeURIComponent(trimmed)}`, action: 'Search product online', warning: 'Product lookup opens a search engine and shares this barcode only after you confirm.' }
  return { kind: 'Plain text', destination: '', action: '', warning: '' }
}

function contrastRatio(foreground: string, background: string) {
  const luminance = (hex: string) => {
    const channels = hex.slice(1).match(/.{2}/g)?.map((value) => parseInt(value, 16) / 255) ?? [0, 0, 0]
    const [r, g, b] = channels.map((value) => value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  }
  const first = luminance(foreground); const second = luminance(background)
  return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05)
}

export default function QrStudioSurface() {
  const locale = normalizeLocale(window.location.pathname.split('/')[1])
  const actions = QR_ACTIONS[locale]
  const dynamic = QR_DYNAMIC[locale]
  const ui = QR_UI[locale]
  const resultCopy = QR_RESULT[locale]
  const statusCopy = QR_STATUS[locale]
  const q = (text: string) => QR_DEEP[locale][text] ?? text
  const qrCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const scanCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const frameRef = useRef<number | null>(null)
  const lastFrameRef = useRef(0)
  const scanLockedRef = useRef(false)
  const scanBusyRef = useRef(false)
  const [tab, setTab] = useState<StudioTab>('scan')
  const [template, setTemplate] = useState<QrTemplate>('url')
  const [creatorFields, setCreatorFields] = useState<CreatorFields>(defaultFields.url)
  const [foreground, setForeground] = useState('#0f172a')
  const [background, setBackground] = useState('#ffffff')
  const [batchStatus, setBatchStatus] = useState('')
  const [saveScans, setSaveScans] = useState(() => localStorage.getItem('purehub.qr-studio.save-history') !== 'false')
  const [scanResult, setScanResult] = useState('')
  const [scanFormat, setScanFormat] = useState('')
  const [scanStatus, setScanStatus] = useState<string>(statusCopy.ready)
  const [cameraActive, setCameraActive] = useState(false)
  const [torchAvailable, setTorchAvailable] = useState(false)
  const [torchOn, setTorchOn] = useState(false)
  const [zoomRange, setZoomRange] = useState<{ min: number; max: number; step: number } | null>(null)
  const [zoom, setZoom] = useState(1)
  const [copied, setCopied] = useState(false)
  const [openConfirmed, setOpenConfirmed] = useState(false)
  const [creatorVerified, setCreatorVerified] = useState(false)
  const [showMoreTypes, setShowMoreTypes] = useState(false)
  const [historyQuery, setHistoryQuery] = useState('')
  const [historyFilter, setHistoryFilter] = useState<'all' | 'scanned' | 'created'>('all')
  const [libraryFolder, setLibraryFolder] = useState('')
  const [libraryNote, setLibraryNote] = useState('')
  const [productProvider, setProductProvider] = useState<'shopping' | 'web'>('shopping')
  const [history, setHistory] = useState<ScanEntry[]>(loadHistory)
  const qrValue = useMemo(() => buildQrValue(template, creatorFields), [creatorFields, template])
  const resultDetails = useMemo(() => payloadDetails(scanResult), [scanResult])
  const productDestination = useMemo(() => {
    if (resultDetails.kind !== 'Product barcode') return resultDetails.destination
    const query = encodeURIComponent(scanResult.trim())
    return productProvider === 'shopping'
      ? `https://www.google.com/search?tbm=shop&q=${query}`
      : `https://duckduckgo.com/?q=${query}`
  }, [productProvider, resultDetails.destination, resultDetails.kind, scanResult])
  const contrast = useMemo(() => contrastRatio(foreground, background), [background, foreground])

  useEffect(() => {
    if (!qrCanvasRef.current) return
    setCreatorVerified(false)
    void import('qrcode').then(async ({ default: QRCode }) => {
      await QRCode.toCanvas(qrCanvasRef.current, qrValue || ' ', { width: 320, margin: 3, errorCorrectionLevel: 'M', color: { dark: foreground, light: background } })
      const canvas = qrCanvasRef.current
      const context = canvas?.getContext('2d', { willReadFrequently: true })
      if (!canvas || !context || !qrValue.trim()) return
      const result = jsQR(context.getImageData(0, 0, canvas.width, canvas.height).data, canvas.width, canvas.height, { inversionAttempts: 'attemptBoth' })
      setCreatorVerified(result?.data === qrValue)
    })
  }, [background, foreground, qrValue])

  const saveEntry = (value: string, source: ScanSource, format = '') => {
    if (!value.trim()) return
    setHistory((current) => {
      const existing = current.find((item) => item.value === value.trim())
      const next = [{ value: value.trim(), savedAt: new Date().toISOString(), source, format: format || undefined, pinned: existing?.pinned, folder: existing?.folder, note: existing?.note }, ...current.filter((item) => item.value !== value.trim())].slice(0, MAX_HISTORY)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }

  const rememberResult = (code: DetectedCode | null, source: ScanSource) => {
    if (!code?.value || scanLockedRef.current) return
    scanLockedRef.current = true
    setScanResult(code.value)
    setScanFormat(code.format)
    setOpenConfirmed(false)
    setLibraryFolder('')
    setLibraryNote('')
    setScanStatus(dynamic.complete(source, code.format.toLowerCase()))
    if (saveScans) saveEntry(code.value, source, code.format)
    navigator.vibrate?.(45)
    markToolSuccess('qr-studio', { headline: resultCopy.scan, detail: saveScans ? resultCopy.scanSaved : resultCopy.scanPrivate, shareText: resultCopy.scan })
  }

  const stopCamera = () => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current)
    frameRef.current = null
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setCameraActive(false)
    setTorchAvailable(false)
    setTorchOn(false)
    setZoomRange(null)
    setZoom(1)
  }

  useEffect(() => stopCamera, [])

  const scanFrame = (timestamp: number) => {
    const video = videoRef.current
    const canvas = scanCanvasRef.current
    if (!scanLockedRef.current && !scanBusyRef.current && video && canvas && video.readyState >= 2 && timestamp - lastFrameRef.current > 180) {
      lastFrameRef.current = timestamp
      const width = Math.min(video.videoWidth, 1280)
      const scale = width / video.videoWidth
      canvas.width = width
      canvas.height = Math.round(video.videoHeight * scale)
      const context = canvas.getContext('2d', { willReadFrequently: true })
      if (context) {
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        scanBusyRef.current = true
        void decodeCanvas(canvas, context).then((code) => rememberResult(code, 'Camera')).finally(() => { scanBusyRef.current = false })
      }
    }
    frameRef.current = requestAnimationFrame(scanFrame)
  }

  const startCamera = async () => {
    try {
      stopCamera()
      scanLockedRef.current = false
      setScanResult('')
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      })
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
      const capabilities = stream.getVideoTracks()[0]?.getCapabilities() as MediaTrackCapabilities & { torch?: boolean; zoom?: { min: number; max: number; step?: number } }
      setTorchAvailable(Boolean(capabilities?.torch))
      const zoomCapabilities = capabilities?.zoom
      if (zoomCapabilities && typeof zoomCapabilities.min === 'number' && typeof zoomCapabilities.max === 'number' && zoomCapabilities.max > zoomCapabilities.min) {
        setZoomRange({ min: zoomCapabilities.min, max: zoomCapabilities.max, step: zoomCapabilities.step || .1 })
        setZoom(zoomCapabilities.min)
      }
      setCameraActive(true)
      setScanStatus(statusCopy.hold)
      frameRef.current = requestAnimationFrame(scanFrame)
    } catch {
      setScanStatus(statusCopy.unavailable)
    }
  }

  const toggleTorch = async () => {
    const track = streamRef.current?.getVideoTracks()[0]
    if (!track) return
    const next = !torchOn
    await track.applyConstraints({ advanced: [{ torch: next } as MediaTrackConstraintSet] })
    setTorchOn(next)
  }

  const setCameraZoom = async (next: number) => {
    const track = streamRef.current?.getVideoTracks()[0]
    if (!track) return
    try {
      await track.applyConstraints({ advanced: [{ zoom: next } as MediaTrackConstraintSet] })
      setZoom(next)
    } catch { setScanStatus(statusCopy.zoom) }
  }

  const handleScanFile = async (file?: File) => {
    if (!file) return
    scanLockedRef.current = false
    setScanStatus(statusCopy.reading)
    try {
      const code = await decodeImage(file)
      if (code) rememberResult(code, 'Image')
      else setScanStatus(statusCopy.none)
    } catch {
      setScanStatus(statusCopy.failed)
    }
  }

  const handleScanFiles = async (files?: FileList | null) => {
    const selected = Array.from(files ?? []).slice(0, 20)
    if (!selected.length) return
    let found = 0
    setScanStatus(dynamic.reading(selected.length))
    for (const file of selected) {
      try {
        const code = await decodeImage(file)
        if (code) { if (saveScans) saveEntry(code.value, 'Image', code.format); found += 1; if (!scanResult) { setScanResult(code.value); setScanFormat(code.format) } }
      } catch { /* continue with the remaining local files */ }
    }
    scanLockedRef.current = found > 0
    setBatchStatus(dynamic.found(found, selected.length, saveScans))
    setScanStatus(found ? dynamic.batchDone(saveScans) : dynamic.none)
    if (found) markToolSuccess('qr-studio', { headline: resultCopy.batch, detail: dynamic.found(found, selected.length, saveScans), shareText: `${resultCopy.batch}: ${found}` })
  }

  const resetScanner = () => {
    scanLockedRef.current = false
    setScanResult('')
    setScanFormat('')
    setOpenConfirmed(false)
    setCopied(false)
    setLibraryFolder('')
    setLibraryNote('')
    setScanStatus(cameraActive ? statusCopy.hold : dynamic.another)
  }

  const qrPng = () => qrCanvasRef.current?.toDataURL('image/png') ?? ''
  const downloadQr = () => {
    const href = qrPng()
    if (!href) return
    const link = document.createElement('a')
    link.href = href
    link.download = `purehub-${template}-qr.png`
    link.click()
    markToolSuccess('qr-studio', { headline: resultCopy.exported, detail: resultCopy.exported, shareText: resultCopy.exported })
  }

  const exportHistory = () => {
    const csvField = (value: string | undefined) => `"${(value ?? '').replaceAll('"', '""')}"`
    const csv = ['value,format,source,folder,note,saved_at', ...history.map((item) => [csvField(item.value), csvField(item.format), csvField(item.source), csvField(item.folder), csvField(item.note), item.savedAt].join(','))].join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'purehub-qr-library.csv'; anchor.click(); URL.revokeObjectURL(url)
    markToolSuccess('qr-studio', { headline: resultCopy.libraryExported, detail: resultCopy.libraryExported, shareText: resultCopy.libraryExported })
  }

  const downloadPayload = (extension: 'ics' | 'vcf' | 'txt') => {
    const mime = extension === 'ics' ? 'text/calendar;charset=utf-8' : extension === 'vcf' ? 'text/vcard;charset=utf-8' : 'text/plain;charset=utf-8'
    const url = URL.createObjectURL(new Blob([scanResult], { type: mime }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = `purehub-scanned-code.${extension}`; anchor.click(); URL.revokeObjectURL(url)
  }

  const shareQr = async () => {
    const href = qrPng()
    if (!href) return
    const blob = await (await fetch(href)).blob()
    const file = new File([blob], 'purehub-qr.png', { type: 'image/png' })
    if (navigator.canShare?.({ files: [file] })) await navigator.share({ title: 'PureHub QR', files: [file] })
    else downloadQr()
    void trackProductEvent('qr-studio', 'share')
  }

  const shareResult = async () => {
    if (navigator.share) {
      await navigator.share({ text: scanResult })
      void trackProductEvent('qr-studio', 'share')
      return
    }
    await navigator.clipboard.writeText(scanResult)
    setCopied(true)
    void trackProductEvent('qr-studio', 'share')
  }

  const visibleHistory = history.filter((item) => {
    const matchesQuery = !historyQuery.trim() || `${item.value} ${item.format ?? ''} ${item.source} ${item.folder ?? ''} ${item.note ?? ''}`.toLowerCase().includes(historyQuery.trim().toLowerCase())
    const matchesFilter = historyFilter === 'all' || (historyFilter === 'created' ? item.source === 'Created' : item.source !== 'Created')
    return matchesQuery && matchesFilter
  }).sort((first, second) => Number(Boolean(second.pinned)) - Number(Boolean(first.pinned)))

  const togglePin = (value: string) => setHistory((current) => {
    const next = current.map((item) => item.value === value ? { ...item, pinned: !item.pinned } : item)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    return next
  })

  const updateEntry = (value: string, patch: Pick<ScanEntry, 'folder' | 'note'>) => setHistory((current) => {
    const next = current.map((item) => item.value === value ? { ...item, ...patch } : item)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    return next
  })

  return (
    <section className="overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-[0_24px_70px_-45px_rgba(15,23,42,.5)] dark:border-slate-700 dark:bg-slate-900">
      <header className="bg-gradient-to-br from-emerald-50 via-white to-cyan-50 px-4 py-5 sm:px-6 dark:from-emerald-950/45 dark:via-slate-900 dark:to-cyan-950/30">
        <div className="flex items-start justify-between gap-4">
<div><p className="text-xs font-black uppercase tracking-[.2em] text-emerald-700 dark:text-emerald-300">{ui.private}</p><h2 className="mt-1 text-2xl font-black tracking-tight text-slate-950 dark:text-white">{ui.title}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{ui.description}</p></div>
          <div className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-950 text-white shadow-lg dark:bg-emerald-400 dark:text-slate-950"><QrCode className="size-6" /></div>
        </div>
        <nav className="mt-5 grid grid-cols-3 rounded-2xl border border-slate-200 bg-white/80 p-1 dark:border-slate-700 dark:bg-slate-950/70" aria-label={q('QR Studio sections')}>
          {([
            ['scan', ScanLine, ui.scan], ['create', Sparkles, ui.create], ['library', History, ui.library],
          ] as const).map(([value, Icon, label]) => <button key={value} type="button" onClick={() => setTab(value)} className={`flex min-h-11 items-center justify-center gap-2 rounded-xl text-sm font-bold transition ${tab === value ? 'bg-slate-950 text-white shadow-sm dark:bg-emerald-400 dark:text-slate-950' : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}><Icon className="size-4" />{label}</button>)}
        </nav>
      </header>

      <div className="p-4 sm:p-6">
        {tab === 'scan' ? <div className="mx-auto max-w-2xl">
          <div className="relative overflow-hidden rounded-[24px] bg-slate-950 shadow-inner">
            <video ref={videoRef} playsInline muted className={`aspect-[4/3] w-full object-cover ${cameraActive ? 'block' : 'hidden'}`} />
            {!cameraActive ? <div className="grid aspect-[4/3] place-items-center px-8 text-center text-slate-300"><div><div className="mx-auto grid size-16 place-items-center rounded-full bg-white/10"><Camera className="size-7 text-emerald-300" /></div><p className="mt-4 font-bold text-white">{ui.cameraOff}</p><p className="mt-1 text-sm text-slate-400">{ui.imageHint}</p></div></div> : null}
            <div className="pointer-events-none absolute inset-0 grid place-items-center"><div className={`relative aspect-square w-[58%] max-w-64 rounded-[28px] border-2 ${scanResult ? 'border-emerald-400' : 'border-white/75'} shadow-[0_0_0_999px_rgba(2,6,23,.28)]`}><span className="absolute -left-0.5 -top-0.5 size-8 rounded-tl-[26px] border-l-4 border-t-4 border-emerald-400"/><span className="absolute -right-0.5 -top-0.5 size-8 rounded-tr-[26px] border-r-4 border-t-4 border-emerald-400"/><span className="absolute -bottom-0.5 -left-0.5 size-8 rounded-bl-[26px] border-b-4 border-l-4 border-emerald-400"/><span className="absolute -bottom-0.5 -right-0.5 size-8 rounded-br-[26px] border-b-4 border-r-4 border-emerald-400"/>{cameraActive && !scanResult ? <span className="absolute left-[8%] right-[8%] top-1/2 h-0.5 bg-emerald-300 shadow-[0_0_16px_3px_rgba(110,231,183,.7)]"/> : null}{scanResult ? <span className="absolute inset-0 grid place-items-center"><span className="grid size-12 place-items-center rounded-full bg-emerald-400 text-slate-950"><Check className="size-6" /></span></span> : null}</div></div>
            <canvas ref={scanCanvasRef} className="hidden" />
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2 rounded-2xl border border-slate-200 bg-white/90 p-2 shadow-lg dark:border-slate-700 dark:bg-slate-950/90 sm:flex sm:justify-center">
            <ActionButton className="justify-center" onClick={cameraActive ? stopCamera : startCamera}><Camera className="size-4" />{cameraActive ? actions.stop : actions.start}</ActionButton>
            <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-800 dark:border-slate-600 dark:text-slate-100"><ImageUp className="size-4" />{actions.image}<input className="sr-only" type="file" accept="image/*" onChange={(event) => void handleScanFile(event.target.files?.[0])} /></label>
            <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-800 dark:border-slate-600 dark:text-slate-100"><ImageUp className="size-4" />{actions.batch}<input className="sr-only" multiple type="file" accept="image/*" onChange={(event) => void handleScanFiles(event.target.files)} /></label>
            {torchAvailable ? <ActionButton className="justify-center" tone="muted" onClick={() => void toggleTorch()}><Flashlight className="size-4" />{torchOn ? actions.torchOff : actions.torch}</ActionButton> : null}
          </div>
          <label className="mt-3 flex min-h-11 items-center justify-between gap-3 rounded-2xl border border-slate-200 px-3 text-xs font-bold text-slate-700 dark:border-slate-700 dark:text-slate-200"><span><strong className="block">{actions.saveHistory}</strong><small className="font-medium text-slate-500">{actions.saveHint}</small></span><input type="checkbox" checked={saveScans} onChange={(event) => { setSaveScans(event.target.checked); localStorage.setItem('purehub.qr-studio.save-history', String(event.target.checked)) }} className="size-5 accent-emerald-600" /></label>
          {zoomRange ? <label className="mt-3 block rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-black text-slate-700 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200">{actions.zoom} <span className="float-right text-emerald-700 dark:text-emerald-300">{zoom.toFixed(1)}×</span><input aria-label={actions.zoom} className="mt-2 w-full accent-emerald-600" type="range" min={zoomRange.min} max={zoomRange.max} step={zoomRange.step} value={zoom} onChange={(event) => void setCameraZoom(Number(event.target.value))} /></label> : null}
          <p aria-live="polite" className="mt-3 text-center text-sm font-semibold text-slate-500 dark:text-slate-400">{scanStatus}</p>
          {batchStatus ? <p className="mt-2 text-center text-xs font-bold text-emerald-700 dark:text-emerald-300">{batchStatus}</p> : null}
          {scanResult ? <article className="mt-4 rounded-t-[28px] border border-emerald-200 bg-emerald-50/95 p-4 shadow-[0_-16px_45px_-30px_rgba(5,150,105,.8)] dark:border-emerald-800 dark:bg-emerald-950/80 sm:rounded-[24px]">
            <div className="mx-auto mb-3 h-1.5 w-10 rounded-full bg-emerald-200 dark:bg-emerald-800 sm:hidden"/><div className="flex items-center justify-between gap-3"><span className="rounded-full bg-emerald-700 px-2.5 py-1 text-[11px] font-black uppercase tracking-wide text-white dark:bg-emerald-400 dark:text-slate-950">{q(resultDetails.kind)}</span><span className="flex items-center gap-1 text-xs font-bold text-emerald-800 dark:text-emerald-200"><ShieldCheck className="size-3.5" />{scanFormat || q('CODE')} · {actions.local}</span></div>
            {resultDetails.kind === 'Website' && resultDetails.destination ? <p className="mt-3 truncate rounded-xl border border-emerald-200 bg-white px-3 py-2 text-base font-black text-slate-950 dark:border-emerald-900 dark:bg-slate-950 dark:text-white">{new URL(resultDetails.destination).hostname}</p> : null}
            <p className="mt-3 max-h-32 overflow-auto break-all rounded-xl bg-white/80 p-3 text-sm font-semibold text-slate-900 dark:bg-slate-950/70 dark:text-white">{scanResult}</p>
            {resultDetails.warning ? <p className="mt-2 text-xs font-bold text-amber-700 dark:text-amber-300">{q(resultDetails.warning)}</p> : null}
            {saveScans ? <div className="mt-3 grid gap-2 sm:grid-cols-[1fr_1fr_auto]">
              <label className="relative"><FolderOpen className="pointer-events-none absolute left-2.5 top-2.5 size-3.5 text-slate-400" /><input aria-label={q('Library folder')} maxLength={48} value={libraryFolder} onChange={(event) => setLibraryFolder(event.target.value)} placeholder={q('Folder (optional)')} className="min-h-9 w-full rounded-lg border border-emerald-200 bg-white px-2 py-1.5 pl-8 text-xs font-semibold text-slate-700 outline-none focus:border-emerald-500 dark:border-emerald-900 dark:bg-slate-950 dark:text-slate-200" /></label>
              <label className="relative"><NotebookPen className="pointer-events-none absolute left-2.5 top-2.5 size-3.5 text-slate-400" /><input aria-label={q('Private library note')} maxLength={240} value={libraryNote} onChange={(event) => setLibraryNote(event.target.value)} placeholder={q('Private note (optional)')} className="min-h-9 w-full rounded-lg border border-emerald-200 bg-white px-2 py-1.5 pl-8 text-xs font-semibold text-slate-700 outline-none focus:border-emerald-500 dark:border-emerald-900 dark:bg-slate-950 dark:text-slate-200" /></label>
              <ActionButton tone="muted" className="justify-center" onClick={() => updateEntry(scanResult, { folder: libraryFolder.trim(), note: libraryNote.trim() })}><History className="size-4" />{q('Save details')}</ActionButton>
            </div> : <p className="mt-3 rounded-xl border border-emerald-200 bg-white/70 px-3 py-2 text-xs font-bold text-emerald-800 dark:border-emerald-900 dark:bg-slate-950/70 dark:text-emerald-200">{q('Private session active · this QR payload is not in the library.')}</p>}
            {resultDetails.kind === 'Product barcode' ? <label className="mt-3 flex items-center gap-2 rounded-xl border border-emerald-200 bg-white/75 px-3 py-2 text-xs font-bold text-slate-700 dark:border-emerald-900 dark:bg-slate-950/70 dark:text-slate-200"><ShoppingBag className="size-4 shrink-0 text-emerald-700 dark:text-emerald-300" />{q('Lookup after confirmation')}<select aria-label={q('Product lookup provider')} value={productProvider} onChange={(event) => { setProductProvider(event.target.value as 'shopping' | 'web'); setOpenConfirmed(false) }} className="ml-auto min-h-8 rounded-lg border border-slate-300 bg-white px-2 text-xs font-bold dark:border-slate-600 dark:bg-slate-900"><option value="shopping">{q('Compare prices')}</option><option value="web">{q('Private web search')}</option></select></label> : null}
            <div className="mt-3 grid grid-cols-2 gap-2 sm:flex">
              <ActionButton tone="muted" className="justify-center" onClick={() => { void navigator.clipboard.writeText(scanResult); setCopied(true) }}>{copied ? <Check className="size-4" /> : <Clipboard className="size-4" />}{copied ? actions.copied : actions.copy}</ActionButton>
              <ActionButton tone="muted" className="justify-center" onClick={() => void shareResult()}><Share2 className="size-4" />{actions.share}</ActionButton>
              {resultDetails.kind === 'Calendar event' ? <ActionButton tone="muted" className="col-span-2 justify-center" onClick={() => downloadPayload('ics')}><Download className="size-4" />{q('Download calendar file')}</ActionButton> : null}
              {resultDetails.kind === 'Contact card' ? <ActionButton tone="muted" className="col-span-2 justify-center" onClick={() => downloadPayload(/^BEGIN:VCARD/i.test(scanResult) ? 'vcf' : 'txt')}><Download className="size-4" />{q('Save contact payload')}</ActionButton> : null}
              {productDestination ? <ActionButton className="col-span-2 justify-center" onClick={() => { if (openConfirmed) window.open(productDestination, '_blank', 'noopener,noreferrer'); else setOpenConfirmed(true) }}><ExternalLink className="size-4" />{openConfirmed ? resultDetails.kind === 'Product barcode' && productProvider === 'shopping' ? q('Compare prices online') : q(resultDetails.action) : resultDetails.kind === 'Product barcode' ? q('Confirm external product lookup') : q(resultDetails.action)}</ActionButton> : null}
              <ActionButton tone="muted" className="col-span-2 justify-center" onClick={resetScanner}><RefreshCw className="size-4" />{actions.another}</ActionButton>
            </div>
          </article> : null}
        </div> : null}

        {tab === 'create' ? <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
          <div><h3 className="text-lg font-black text-slate-950 dark:text-white">{actions.createTitle}</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{actions.createHint}</p>
            <div className="mt-4 flex flex-wrap gap-2">{(['url', 'text', 'wifi'] as QrTemplate[]).map((item) => <button key={item} type="button" onClick={() => { setTemplate(item); setCreatorFields(defaultFields[item]) }} className={`min-h-11 rounded-full border px-3 text-xs font-black ${template === item ? 'border-emerald-700 bg-emerald-700 text-white dark:border-emerald-400 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-300 text-slate-600 dark:border-slate-600 dark:text-slate-300'}`}>{q(templates[item].label)}</button>)}<button type="button" onClick={() => setShowMoreTypes((value) => !value)} className="min-h-11 rounded-full border border-slate-300 px-3 text-xs font-black text-slate-600 dark:border-slate-600 dark:text-slate-300">{showMoreTypes ? actions.less : actions.more}</button></div>
            {showMoreTypes ? <div className="mt-2 flex flex-wrap gap-2 rounded-2xl bg-slate-50 p-2 dark:bg-slate-950">{(['email', 'phone', 'sms', 'location', 'calendar', 'contact'] as QrTemplate[]).map((item) => <button key={item} type="button" onClick={() => { setTemplate(item); setCreatorFields(defaultFields[item]) }} className={`min-h-10 rounded-full border px-3 text-xs font-black ${template === item ? 'border-emerald-700 bg-emerald-700 text-white dark:border-emerald-400 dark:bg-emerald-400 dark:text-slate-950' : 'border-slate-300 text-slate-600 dark:border-slate-600 dark:text-slate-300'}`}>{q(templates[item].label)}</button>)}</div> : null}
            <div className="mt-4 grid gap-3">
              {template === 'text'
                ? <FormTextArea className="min-h-32" aria-label={q('Text')} placeholder={q('Write something useful')} value={creatorFields.primary} onChange={(event) => setCreatorFields({ ...creatorFields, primary: event.target.value })} />
                : <FormInput aria-label={q(template === 'wifi' ? 'Network name' : template === 'contact' ? 'Contact name' : template === 'email' ? 'Email address' : template === 'phone' || template === 'sms' ? 'Phone number' : template === 'location' ? 'Coordinates' : template === 'calendar' ? 'Event title' : 'Website address')} placeholder={template === 'wifi' ? q('Wi-Fi name') : template === 'contact' ? q('Full name') : template === 'email' ? 'hello@example.com' : template === 'phone' || template === 'sms' ? '+1 000 000 0000' : template === 'location' ? '10.7769,106.7009' : template === 'calendar' ? q('Event title') : 'https://example.com'} value={creatorFields.primary} onChange={(event) => setCreatorFields({ ...creatorFields, primary: event.target.value })} />}
              {template === 'wifi' ? <><FormInput aria-label={q('Wi-Fi password')} placeholder={q('Wi-Fi password')} type="password" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><select aria-label={q('Wi-Fi security')} className="min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold dark:border-slate-600 dark:bg-slate-900" value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })}><option>WPA</option><option>WEP</option><option value="None">{q('None')}</option></select></> : null}
              {template === 'email' ? <FormInput aria-label={q('Email subject')} placeholder={q('Subject')} value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /> : null}
              {template === 'sms' ? <FormInput aria-label={q('Message')} placeholder={q('Message (optional)')} value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /> : null}
              {template === 'calendar' ? <><FormInput aria-label={q('Start time')} type="datetime-local" value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><FormInput aria-label={q('Location')} placeholder={q('Location (optional)')} value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })} /></> : null}
              {template === 'contact' ? <><FormInput aria-label={q('Contact phone')} placeholder={q('Phone')} value={creatorFields.secondary} onChange={(event) => setCreatorFields({ ...creatorFields, secondary: event.target.value })} /><FormInput aria-label={q('Contact email')} placeholder={q('Email')} value={creatorFields.tertiary} onChange={(event) => setCreatorFields({ ...creatorFields, tertiary: event.target.value })} /></> : null}
            </div>
            <div className="mt-3 flex flex-wrap items-center gap-3"><label className="text-sm font-bold text-slate-600 dark:text-slate-300">{q('Code')}<input aria-label={q('QR foreground color')} type="color" value={foreground} onChange={(event) => setForeground(event.target.value)} className="ml-2 size-10 cursor-pointer rounded-lg border-0 bg-transparent align-middle" /></label><label className="text-sm font-bold text-slate-600 dark:text-slate-300">{q('Background')}<input aria-label={q('QR background color')} type="color" value={background} onChange={(event) => setBackground(event.target.value)} className="ml-2 size-10 cursor-pointer rounded-lg border-0 bg-transparent align-middle" /></label><button type="button" className="text-xs font-bold text-slate-500" onClick={() => { setForeground('#0f172a'); setBackground('#ffffff') }}>{q('Reset')}</button></div>
            <p className={`mt-2 rounded-xl px-3 py-2 text-xs font-bold ${contrast >= 4.5 ? 'bg-emerald-500/10 text-emerald-700' : 'bg-amber-500/15 text-amber-800'}`}>{contrast >= 4.5 ? `${resultCopy.safe} · ${contrast.toFixed(1)}:1` : `${resultCopy.low} · ${contrast.toFixed(1)}:1`}</p>
          </div>
          <div className="flex flex-col items-center rounded-[24px] border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-950"><div className="rounded-[20px] bg-white p-3 shadow-sm"><canvas ref={qrCanvasRef} className="h-auto w-full max-w-[300px]" /></div><p className={`mt-3 text-center text-xs font-semibold ${creatorVerified ? 'text-emerald-700 dark:text-emerald-300' : 'text-amber-700 dark:text-amber-300'}`}>{q(creatorVerified ? 'Self-tested locally · ready to scan' : 'Checking generated code locally…')}</p><div className="mt-4 grid w-full grid-cols-2 gap-2"><ActionButton className="justify-center" disabled={!creatorVerified} onClick={downloadQr}><Download className="size-4" />{q('Download')}</ActionButton><ActionButton tone="muted" disabled={!creatorVerified} className="justify-center" onClick={() => void shareQr()}><Share2 className="size-4" />{q('Share')}</ActionButton><ActionButton tone="muted" disabled={!creatorVerified} className="col-span-2 justify-center" onClick={() => saveEntry(qrValue, 'Created', 'QR CODE')}><History className="size-4" />{q('Save to library')}</ActionButton></div></div>
        </div> : null}

        {tab === 'library' ? <div><div className="flex items-center justify-between gap-3"><div><h3 className="text-lg font-black text-slate-950 dark:text-white">{ui.libraryTitle}</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{ui.libraryHint}</p></div>{history.length ? <div className="flex gap-1"><button type="button" title={actions.exportLibrary} aria-label={actions.exportLibrary} className="grid size-10 place-items-center rounded-xl text-emerald-700 hover:bg-emerald-50" onClick={exportHistory}><Download className="size-4" /></button><button type="button" title={actions.clearLibrary} aria-label={actions.clearLibrary} className="grid size-10 place-items-center rounded-xl text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30" onClick={() => { localStorage.removeItem(STORAGE_KEY); setHistory([]) }}><Trash2 className="size-4" /></button></div> : null}</div>
          {history.length ? <><label className="relative mt-4 block"><Search className="pointer-events-none absolute left-3 top-3.5 size-4 text-slate-400"/><FormInput aria-label={actions.search} value={historyQuery} onChange={(event) => setHistoryQuery(event.target.value)} placeholder={actions.search} className="pl-10" /></label><div className="mt-2 flex gap-2">{(['all', 'scanned', 'created'] as const).map((filter) => <button key={filter} type="button" onClick={() => setHistoryFilter(filter)} className={`min-h-9 rounded-full px-3 text-xs font-black ${historyFilter === filter ? 'bg-emerald-700 text-white dark:bg-emerald-400 dark:text-slate-950' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'}`}>{filter === 'all' ? actions.all : filter === 'scanned' ? actions.scanned : actions.created}</button>)}</div><div className="mt-3 grid gap-2 sm:grid-cols-2">{visibleHistory.map((item) => <div key={`${item.savedAt}-${item.value}`} className="group flex min-w-0 items-center gap-2 rounded-2xl border border-slate-200 p-3 transition hover:border-emerald-300 hover:bg-emerald-50/40 dark:border-slate-700 dark:hover:border-emerald-700 dark:hover:bg-emerald-950/20"><button type="button" className="flex min-w-0 flex-1 items-center gap-3 text-left" onClick={() => { setScanResult(item.value); setScanFormat(item.format ?? ''); setTab('scan'); scanLockedRef.current = true }}><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-700 group-hover:bg-emerald-100 group-hover:text-emerald-800 dark:bg-slate-800 dark:text-slate-200"><QrCode className="size-5" /></span><span className="min-w-0"><span className="block truncate text-sm font-bold text-slate-900 dark:text-white">{item.value}</span><span className="mt-0.5 block text-xs text-slate-500">{item.format ? `${item.format} · ` : ''}{q(item.source)} · {new Date(item.savedAt).toLocaleString(locale)}</span></span></button><button type="button" aria-label={q(item.pinned ? 'Unpin code' : 'Pin code')} onClick={() => togglePin(item.value)} className={`grid size-10 shrink-0 place-items-center rounded-xl ${item.pinned ? 'text-amber-500' : 'text-slate-400'}`}><Star className={`size-4 ${item.pinned ? 'fill-current' : ''}`}/></button></div>)}{!visibleHistory.length ? <p className="col-span-full py-6 text-center text-sm text-slate-500">{actions.noMatch}</p> : null}</div></> : <div className="mt-5 grid min-h-48 place-items-center rounded-[22px] border border-dashed border-slate-300 text-center dark:border-slate-700"><div><History className="mx-auto size-8 text-slate-400"/><p className="mt-2 font-bold text-slate-700 dark:text-slate-200">{ui.noSaved}</p><p className="mt-1 text-sm text-slate-500">{ui.noSavedHint}</p></div></div>}
        </div> : null}
      </div>
    </section>
  )
}

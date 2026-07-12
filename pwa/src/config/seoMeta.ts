export const SITE_ORIGIN = 'https://hub.blissbiovn.com'

export const SEO_LANGUAGES = ['en', 'vi', 'zh'] as const

export type SeoLanguage = (typeof SEO_LANGUAGES)[number]

export const SEO_MINI_APP_IDS = [
  'lunar-calendar',
  'zen-habit',
  'zen-pomodoro',
  'zen-breath',
  'compass',
  'bubble-level',
  'decibel-meter',
  'unit-converter',
  'qr-studio',
  'doc-to-pdf',
  'ocr-text',
  'color-grabber',
  'speaker-cleaner',
  'password-vault',
  'bill-splitter',
  'expense-tracker',
  'decision-wheel',
] as const

export type SeoMiniAppId = (typeof SEO_MINI_APP_IDS)[number]

export type SeoMetaEntry = {
  slug: string
  title: string
  description: string
  keywords: string[]
}

export type SeoMetaDictionary = Record<SeoMiniAppId, Record<SeoLanguage, SeoMetaEntry>>

export const seoMeta: SeoMetaDictionary = {
  'lunar-calendar': {
    en: {
      slug: 'lunar-calendar',
      title: 'Free Offline Lunar Calendar | No Ads',
      description:
        'Use PureHub Lunar Calendar for fast offline solar-to-lunar conversion, month browsing, and privacy-first date planning without ads.',
      keywords: ['offline lunar calendar', 'solar to lunar', 'no ads calendar', 'private calendar pwa'],
    },
    vi: {
      slug: 'lich-am',
      title: 'Lịch Âm Offline Miễn Phí | Không Quảng Cáo',
      description:
        'Xem lịch âm, đổi ngày dương sang âm và tra cứu tháng nhanh ngay trên trình duyệt, hoàn toàn offline và không quảng cáo.',
      keywords: ['lich am offline', 'doi ngay duong am', 'lich am khong quang cao', 'pwa tien ich'],
    },
    zh: {
      slug: 'nong-li',
      title: '免费离线农历 | 无广告',
      description:
        '使用 PureHub 离线农历快速查看公历转农历、月视图与本地日期工具，全程无广告且注重隐私。',
      keywords: ['离线农历', '公历转农历', '无广告日历', '隐私工具'],
    },
  },
  'zen-habit': {
    en: {
      slug: 'zen-habit',
      title: 'Offline Habit Tracker | Private Streaks',
      description:
        'Track habits and daily streaks with PureHub Zen Habit using private IndexedDB storage, smooth mobile UX, and zero ads.',
      keywords: ['offline habit tracker', 'streak tracker pwa', 'private habit tracker', 'no ads habits'],
    },
    vi: {
      slug: 'thoi-quen-zen',
      title: 'Theo Dõi Thói Quen Offline | Streak Riêng Tư',
      description:
        'Theo dõi thói quen, streak hằng ngày và tiến độ cá nhân bằng lưu trữ cục bộ, không quảng cáo và không cần mạng.',
      keywords: ['theo doi thoi quen offline', 'streak offline', 'thoi quen rieng tu', 'khong quang cao'],
    },
    zh: {
      slug: 'chan-xi-guan',
      title: '离线习惯追踪器 | 私密连击记录',
      description:
        'PureHub 习惯追踪器支持本地保存、每日连击与移动端流畅体验，无广告且不依赖云端。',
      keywords: ['离线习惯追踪', '连击记录', '隐私习惯应用', '无广告工具'],
    },
  },
  'zen-pomodoro': {
    en: {
      slug: 'zen-pomodoro',
      title: 'Offline Pomodoro Timer | Focus With No Ads',
      description:
        'Stay focused with an offline Pomodoro timer, calm UX, and local white-noise sessions in PureHub.',
      keywords: ['offline pomodoro', 'focus timer no ads', 'pwa pomodoro', 'white noise timer'],
    },
    vi: {
      slug: 'pomodoro-zen',
      title: 'Pomodoro Offline | Hẹn Giờ Tập Trung Không Quảng Cáo',
      description:
        'Hẹn giờ Pomodoro offline với giao diện mượt, tập trung sâu và âm nền cục bộ ngay trên thiết bị.',
      keywords: ['pomodoro offline', 'hen gio tap trung', 'dong ho pomodoro pwa', 'khong quang cao'],
    },
    zh: {
      slug: 'chan-fan-qie-zhong',
      title: '离线番茄钟 | 无广告专注计时',
      description:
        'PureHub 离线番茄钟提供顺滑专注计时、白噪音体验与本地使用流程，无广告更轻松。',
      keywords: ['离线番茄钟', '专注计时器', '无广告效率工具', '白噪音'],
    },
  },
  'zen-breath': {
    en: {
      slug: 'zen-breath',
      title: 'Breathing Exercise App Offline | Calm Focus',
      description:
        'Follow a smooth breathing guide with offline animations, minimalist design, and privacy-first wellness flows.',
      keywords: ['breathing exercise offline', 'calm breathing app', 'breath guide pwa', 'mindfulness no ads'],
    },
    vi: {
      slug: 'tho-zen',
      title: 'Hướng Dẫn Hít Thở Offline | Bình Tâm Không Quảng Cáo',
      description:
        'Tập hít thở theo nhịp với hoạt ảnh mượt, nhẹ nhàng và hoàn toàn offline trên PureHub.',
      keywords: ['hit tho offline', 'huong dan hit tho', 'ung dung binh tam', 'khong quang cao'],
    },
    zh: {
      slug: 'chan-hu-xi',
      title: '离线呼吸训练 | 平静专注',
      description:
        '使用顺滑动画进行呼吸训练，享受安静、私密且完全离线的放松体验。',
      keywords: ['离线呼吸训练', '呼吸引导', '平静专注', '无广告冥想工具'],
    },
  },
  compass: {
    en: {
      slug: 'compass',
      title: 'Free Offline Compass | No Ads',
      description:
        'Use a smooth offline compass in PureHub with browser sensor support, private processing, and no ads.',
      keywords: ['offline compass', 'web compass no ads', 'private compass pwa', 'device orientation compass'],
    },
    vi: {
      slug: 'la-ban',
      title: 'La Bàn Tiếng Việt Offline | Không Quảng Cáo',
      description:
        'La bàn offline mượt mà, không quảng cáo, dùng cảm biến thiết bị ngay trong trình duyệt.',
      keywords: ['la ban offline', 'la ban khong quang cao', 'la ban pwa', 'cam bien trinh duyet'],
    },
    zh: {
      slug: 'zhinan-zhen',
      title: '免费离线指南针 | 无广告',
      description:
        'PureHub 指南针支持设备方向感应、离线使用与顺滑旋转体验，无广告更纯净。',
      keywords: ['离线指南针', '网页指南针', '无广告指南针', '设备方向传感器'],
    },
  },
  'bubble-level': {
    en: {
      slug: 'bubble-level',
      title: 'Offline Bubble Level | Fast Alignment Tool',
      description:
        'Level surfaces quickly with an offline bubble level powered by device motion and private on-device logic.',
      keywords: ['bubble level offline', 'alignment tool pwa', 'device motion level', 'no ads level app'],
    },
    vi: {
      slug: 'thuoc-thuy',
      title: 'Thước Thủy Offline | Căn Chỉnh Nhanh',
      description:
        'Căn chỉnh bề mặt nhanh bằng thước thủy offline sử dụng cảm biến thiết bị và không cần mạng.',
      keywords: ['thuoc thuy offline', 'can chinh mat phang', 'thiet bi cam bien', 'khong quang cao'],
    },
    zh: {
      slug: 'shui-ping-yi',
      title: '离线水平仪 | 快速校准工具',
      description:
        '借助设备运动传感器快速测平，PureHub 水平仪离线运行且体验轻巧。',
      keywords: ['离线水平仪', '校准工具', '设备传感器', '无广告工具'],
    },
  },
  'decibel-meter': {
    en: {
      slug: 'decibel-meter',
      title: 'Offline Decibel Meter | Local Sound Level Tool',
      description:
        'Measure local sound levels with a browser-based decibel meter using private microphone processing and no ads.',
      keywords: ['decibel meter offline', 'sound level pwa', 'microphone meter no ads', 'privacy audio tool'],
    },
    vi: {
      slug: 'do-on',
      title: 'Đo Độ Ồn Offline | Công Cụ Decibel Riêng Tư',
      description:
        'Đo âm lượng môi trường bằng microphone ngay trên máy, xử lý cục bộ và không quảng cáo.',
      keywords: ['do on offline', 'do decibel tren web', 'microphone offline', 'khong quang cao'],
    },
    zh: {
      slug: 'fen-bei-yi',
      title: '离线分贝仪 | 本地声音检测',
      description:
        '通过浏览器麦克风在本地测量环境音量，PureHub 分贝仪无广告且注重隐私。',
      keywords: ['离线分贝仪', '声音检测', '麦克风工具', '隐私音频工具'],
    },
  },
  'unit-converter': {
    en: {
      slug: 'unit-converter',
      title: 'Offline Unit Converter | Fast No Ads Tool',
      description:
        'Convert length, weight, and more instantly with a fast offline unit converter built for mobile and desktop.',
      keywords: ['unit converter offline', 'measurement converter', 'no ads calculator', 'fast conversion tool'],
    },
    vi: {
      slug: 'doi-don-vi',
      title: 'Đổi Đơn Vị Offline | Nhanh Và Không Quảng Cáo',
      description:
        'Đổi chiều dài, khối lượng và nhiều đơn vị khác tức thì bằng công cụ offline nhẹ và nhanh.',
      keywords: ['doi don vi offline', 'cong cu doi don vi', 'khong quang cao', 'doi chieu dai khoi luong'],
    },
    zh: {
      slug: 'dan-wei-huan-suan',
      title: '离线单位换算 | 快速无广告',
      description:
        '快速换算长度、重量等常用单位，PureHub 提供顺滑、离线且无广告的体验。',
      keywords: ['离线单位换算', '长度重量换算', '无广告工具', '快速换算'],
    },
  },
  'qr-studio': {
    en: {
      slug: 'qr-studio',
      title: 'Offline QR Scanner & Generator | PureHub',
      description:
        'Scan and generate QR codes offline with a privacy-first workflow for mobile and desktop browsers.',
      keywords: ['offline qr scanner', 'qr code generator', 'privacy qr tool', 'no ads qr studio'],
    },
    vi: {
      slug: 'qr-studio',
      title: 'Quét Và Tạo QR Offline | PureHub',
      description:
        'Quét mã QR và tạo QR offline ngay trên trình duyệt với trải nghiệm riêng tư và không quảng cáo.',
      keywords: ['quet qr offline', 'tao qr offline', 'qr studio pwa', 'khong quang cao'],
    },
    zh: {
      slug: 'er-wei-ma-gong-fang',
      title: '离线二维码扫描与生成 | PureHub',
      description:
        '在浏览器中离线扫描和生成二维码，提供隐私优先且无广告的使用体验。',
      keywords: ['离线二维码', '二维码生成器', '二维码扫描', '无广告工具'],
    },
  },
  'doc-to-pdf': {
    en: {
      slug: 'doc-to-pdf',
      title: 'Document to PDF Offline | Camera to PDF Tool',
      description:
        'Capture pages and turn them into PDFs offline with PureHub, without uploads, ads, or cloud processing.',
      keywords: ['document to pdf offline', 'camera to pdf', 'private pdf tool', 'no upload pdf'],
    },
    vi: {
      slug: 'tai-lieu-pdf',
      title: 'Tài Liệu Sang PDF Offline | Chụp Và Xuất PDF',
      description:
        'Chụp tài liệu, ghép trang và xuất PDF offline mà không tải dữ liệu lên máy chủ.',
      keywords: ['tai lieu sang pdf', 'chup tai lieu pdf', 'pdf offline', 'khong upload'],
    },
    zh: {
      slug: 'wen-dang-zhuan-pdf',
      title: '离线文档转 PDF | 相机生成 PDF',
      description:
        '使用 PureHub 离线拍摄文档并生成 PDF，不上传云端、无广告且更私密。',
      keywords: ['离线文档转pdf', '相机转pdf', '本地pdf工具', '无上传'],
    },
  },
  'ocr-text': {
    en: {
      slug: 'ocr-text',
      title: 'Offline OCR Text Extractor | No Ads',
      description:
        'Extract text from images offline using browser-side OCR in PureHub with no ads and privacy-first processing.',
      keywords: ['offline ocr', 'text extractor pwa', 'image to text no ads', 'browser ocr'],
    },
    vi: {
      slug: 'trich-xuat-van-ban',
      title: 'OCR Trích Xuất Văn Bản Offline | Không Quảng Cáo',
      description:
        'Nhận diện và trích xuất chữ từ ảnh offline ngay trên trình duyệt, riêng tư và không quảng cáo.',
      keywords: ['ocr offline', 'trich xuat van ban', 'anh sang chu', 'khong quang cao'],
    },
    zh: {
      slug: 'ocr-wen-ben',
      title: '离线 OCR 文字提取 | 无广告',
      description:
        '通过浏览器端 OCR 离线提取图片文字，PureHub 保持私密处理且无广告。',
      keywords: ['离线ocr', '文字提取', '图片转文字', '无广告ocr'],
    },
  },
  'color-grabber': {
    en: {
      slug: 'color-grabber',
      title: 'Color Picker From Camera | Offline Color Grabber',
      description:
        'Sample HEX and RGB colors from camera frames offline with a smooth color grabber built for mobile browsers.',
      keywords: ['offline color picker', 'camera color grabber', 'hex rgb picker', 'pwa color tool'],
    },
    vi: {
      slug: 'lay-mau',
      title: 'Lấy Màu Từ Camera Offline | HEX Và RGB',
      description:
        'Lấy mã màu HEX và RGB trực tiếp từ camera bằng công cụ offline mượt và nhẹ.',
      keywords: ['lay mau offline', 'hex rgb camera', 'color picker pwa', 'khong quang cao'],
    },
    zh: {
      slug: 'qu-se-qi',
      title: '离线取色器 | 相机 HEX 与 RGB',
      description:
        '从相机画面中离线提取 HEX 与 RGB 颜色，适合移动端快速取色。',
      keywords: ['离线取色器', '相机取色', 'hex rgb', '颜色采样'],
    },
  },
  'speaker-cleaner': {
    en: {
      slug: 'speaker-cleaner',
      title: 'Speaker Cleaner Online Offline Safe Tone Tool',
      description:
        'Play a speaker cleaning tone locally with PureHub to help clear dust and water using private audio processing.',
      keywords: ['speaker cleaner', 'tone generator', 'audio cleaning tool', 'offline speaker utility'],
    },
    vi: {
      slug: 'lam-sach-loa',
      title: 'Làm Sạch Loa Offline | Phát Tần Số Hỗ Trợ',
      description:
        'Phát tần số hỗ trợ làm sạch loa ngay trên thiết bị, riêng tư và không cần kết nối mạng.',
      keywords: ['lam sach loa', 'tan so loa offline', 'audio tool', 'khong quang cao'],
    },
    zh: {
      slug: 'yang-sheng-qi-qing-jie',
      title: '离线扬声器清理 | 安全音频工具',
      description:
        '使用本地音频频率帮助清理扬声器积水或灰尘，过程私密且无需联网。',
      keywords: ['扬声器清理', '离线音频工具', '频率发生器', '无广告'],
    },
  },
  'password-vault': {
    en: {
      slug: 'password-vault',
      title: 'Private Password Vault Offline | PureHub',
      description:
        'Store passwords locally with a privacy-first vault designed for offline access and zero ads.',
      keywords: ['offline password vault', 'private password manager', 'local password storage', 'no ads vault'],
    },
    vi: {
      slug: 'kho-mat-khau',
      title: 'Kho Mật Khẩu Offline | Lưu Cục Bộ Riêng Tư',
      description:
        'Lưu mật khẩu cục bộ với trải nghiệm riêng tư, offline và không quảng cáo trên PureHub.',
      keywords: ['kho mat khau offline', 'luu mat khau cuc bo', 'password vault pwa', 'rieng tu'],
    },
    zh: {
      slug: 'mi-ma-bao-xian-ku',
      title: '离线密码保险库 | 私密本地保存',
      description:
        'PureHub 密码保险库支持本地离线保存，隐私优先且没有广告干扰。',
      keywords: ['离线密码库', '本地密码管理', '隐私保险库', '无广告密码工具'],
    },
  },
  'bill-splitter': {
    en: {
      slug: 'bill-splitter',
      title: 'Bill Splitter Offline | Group Expense Tool',
      description:
        'Split bills, tips, and shared costs instantly with a privacy-first offline bill splitter for groups and trips.',
      keywords: ['bill splitter offline', 'group expense tool', 'split bills no ads', 'trip bill calculator'],
    },
    vi: {
      slug: 'chia-hoa-don',
      title: 'Chia Hóa Đơn Offline | Tính Tiền Nhóm Nhanh',
      description:
        'Chia hóa đơn, tip và chi phí nhóm tức thì bằng công cụ offline gọn nhẹ và không quảng cáo.',
      keywords: ['chia hoa don offline', 'tinh tien nhom', 'split bill pwa', 'khong quang cao'],
    },
    zh: {
      slug: 'fen-zhang-qi',
      title: '离线分账器 | 群体费用计算',
      description:
        '快速拆分账单、小费和多人费用，PureHub 提供顺滑且离线的分账体验。',
      keywords: ['离线分账器', '账单拆分', '多人费用计算', '无广告工具'],
    },
  },
  'expense-tracker': {
    en: {
      slug: 'expense-tracker',
      title: 'Offline Expense Tracker | Private Budget Ledger',
      description:
        'Track spending locally with a private expense tracker using IndexedDB, mobile-friendly UX, and no ads.',
      keywords: ['expense tracker offline', 'budget ledger pwa', 'private spending tracker', 'no ads finance tool'],
    },
    vi: {
      slug: 'so-chi-tieu',
      title: 'Sổ Chi Tiêu Offline | Ghi Thu Chi Riêng Tư',
      description:
        'Ghi chép thu chi cục bộ, theo dõi ngân sách và giữ toàn bộ dữ liệu trên thiết bị của bạn.',
      keywords: ['so chi tieu offline', 'ghi thu chi', 'quan ly chi tieu pwa', 'rieng tu'],
    },
    zh: {
      slug: 'ji-zhang-ben',
      title: '离线记账本 | 私密预算追踪',
      description:
        '使用 PureHub 本地记录收支、跟踪预算，所有数据保留在设备中且无广告。',
      keywords: ['离线记账本', '预算追踪', '本地记账', '无广告财务工具'],
    },
  },
  'decision-wheel': {
    en: {
      slug: 'decision-wheel',
      title: 'Decision Wheel Offline | Spin Picker Tool',
      description:
        'Spin a decision wheel offline for quick picks, random choices, and lightweight game-like utility moments.',
      keywords: ['decision wheel offline', 'spin picker', 'random choice tool', 'no ads wheel'],
    },
    vi: {
      slug: 'vong-quay-quyet-dinh',
      title: 'Vòng Quay Quyết Định Offline | Chọn Nhanh',
      description:
        'Quay để chọn ngẫu nhiên, ra quyết định nhanh và dùng hoàn toàn offline trên PureHub.',
      keywords: ['vong quay quyet dinh', 'chon ngau nhien', 'spin wheel offline', 'khong quang cao'],
    },
    zh: {
      slug: 'jue-ce-zhuan-pan',
      title: '离线决策转盘 | 快速随机选择',
      description:
        '通过顺滑转盘快速做出随机选择，PureHub 提供轻量、离线且无广告的体验。',
      keywords: ['离线决策转盘', '随机选择', '转盘工具', '无广告'],
    },
  },
}

export type SeoRouteEntry = {
  appId: SeoMiniAppId
  lang: SeoLanguage
  slug: string
  path: string
}

export const seoRouteEntries: SeoRouteEntry[] = SEO_MINI_APP_IDS.flatMap((appId) =>
  SEO_LANGUAGES.map((lang) => {
    const slug = seoMeta[appId][lang].slug
    return {
      appId,
      lang,
      slug,
      path: `/${lang}/${slug}`,
    }
  }),
)

export function buildSeoSitemapPaths() {
  return seoRouteEntries.map((entry) => entry.path)
}

export function getSeoMetaBySlug(lang: SeoLanguage, slug: string) {
  const normalizedSlug = decodeURIComponent(slug)
  const appId = SEO_MINI_APP_IDS.find((item) => seoMeta[item][lang].slug === normalizedSlug)
  return appId
    ? {
        appId,
        lang,
        ...seoMeta[appId][lang],
      }
    : null
}

export function buildCanonicalUrl(lang: SeoLanguage, slug: string) {
  return `${SITE_ORIGIN}/${lang}/${slug}`
}

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
  'smart-flashlight',
  'unit-converter',
  'qr-studio',
  'doc-to-pdf',
  'ocr-text',
  'color-grabber',
  'speaker-cleaner',
  'deep-cleaner',
  'wifi-analyzer',
  'password-vault',
  'wallpaper-changer',
  'bill-splitter',
  'expense-tracker',
  'decision-wheel',
  'community-pro-unlock',
  'authenticator-vault',
  'file-studio',
  'screen-recorder',
] as const

export type SeoMiniAppId = (typeof SEO_MINI_APP_IDS)[number]

export type SeoMetaEntry = {
  slug: string
  title: string
  description: string
  keywords: string[]
}

export type SeoMetaDictionary = Record<SeoMiniAppId, Record<SeoLanguage, SeoMetaEntry>>

export const SEO_SITE_PAGE_IDS = ['home', 'tools', 'community', 'download', 'changelog'] as const

export type SeoSitePageId = (typeof SEO_SITE_PAGE_IDS)[number]

export type SeoSiteMetaEntry = SeoMetaEntry & { segment: string }

export const seoSiteMeta: Record<SeoSitePageId, Record<SeoLanguage, SeoSiteMetaEntry>> = {
  home: {
    en: { segment: '', slug: '', title: 'PureHub – 25 Free Offline Mini Apps, No Ads', description: 'Use 25 free, ad-free mini apps for focus, files, scanning, privacy, finance and more. PureHub works offline and keeps data on your device.', keywords: ['free mini apps', 'offline tools', 'no ads apps', 'privacy-first PWA'] },
    vi: { segment: '', slug: '', title: 'PureHub – 25 Mini App Miễn Phí, Offline, Không Quảng Cáo', description: 'Dùng 25 mini app miễn phí cho tập trung, tệp, quét tài liệu, bảo mật và tài chính. PureHub chạy offline và giữ dữ liệu trên thiết bị.', keywords: ['mini app mien phi', 'cong cu offline', 'khong quang cao', 'PWA rieng tu'] },
    zh: { segment: '', slug: '', title: 'PureHub – 25 个免费离线迷你应用，无广告', description: '使用 25 个免费迷你应用完成专注、文件、扫描、隐私和财务任务。PureHub 支持离线运行，数据保留在设备中。', keywords: ['免费迷你应用', '离线工具', '无广告应用', '隐私PWA'] },
  },
  tools: {
    en: { segment: 'tools', slug: 'tools', title: 'All 25 Free Offline Tools | PureHub', description: 'Explore all 25 PureHub mini apps for focus, files, vision, security, audio, and finance—free, private, offline, and without ads.', keywords: ['free online tools', 'offline utility apps', 'privacy tools', 'no ads mini apps'] },
    vi: { segment: 'tools', slug: 'tools', title: 'Tất Cả 25 Công Cụ Offline Miễn Phí | PureHub', description: 'Khám phá 25 mini app PureHub cho tập trung, tệp, hình ảnh, bảo mật, âm thanh và tài chính—miễn phí, riêng tư, không quảng cáo.', keywords: ['cong cu online mien phi', 'mini app offline', 'cong cu rieng tu', 'khong quang cao'] },
    zh: { segment: 'tools', slug: 'tools', title: '全部 25 个免费离线工具 | PureHub', description: '探索 PureHub 的 25 个专注、文件、视觉、安全、音频和财务工具，免费、私密、离线且无广告。', keywords: ['免费在线工具', '离线应用', '隐私工具', '无广告迷你应用'] },
  },
  community: {
    en: { segment: 'community', slug: 'community', title: 'PureHub Open-Source Community', description: 'Join the PureHub community to get support, suggest mini apps, report issues, and help build useful tools that remain free and ad-free.', keywords: ['PureHub community', 'open source community', 'suggest mini app', 'free no ads tools'] },
    vi: { segment: 'community', slug: 'community', title: 'Cộng Đồng Mã Nguồn Mở PureHub', description: 'Tham gia cộng đồng PureHub để nhận hỗ trợ, đề xuất mini app, báo lỗi và cùng xây dựng công cụ hữu ích luôn miễn phí, không quảng cáo.', keywords: ['cong dong PureHub', 'ma nguon mo', 'de xuat mini app', 'cong cu mien phi'] },
    zh: { segment: 'community', slug: 'community', title: 'PureHub 开源社区', description: '加入 PureHub 社区，获取支持、提出迷你应用建议、报告问题，并共同打造始终免费且无广告的实用工具。', keywords: ['PureHub社区', '开源社区', '迷你应用建议', '免费工具'] },
  },
  download: {
    en: { segment: 'download', slug: 'download', title: 'Download PureHub for Android | Free, No Ads', description: 'Download the signed PureHub Android app or install the PWA to access 25 private, ad-free mini apps with strong offline support.', keywords: ['download PureHub Android', 'free no ads APK', 'offline PWA', 'signed Android app'] },
    vi: { segment: 'download', slug: 'download', title: 'Tải PureHub Cho Android | Miễn Phí, Không Quảng Cáo', description: 'Tải ứng dụng Android PureHub đã ký hoặc cài PWA để dùng 25 mini app riêng tư, không quảng cáo và hỗ trợ offline mạnh.', keywords: ['tai PureHub Android', 'APK mien phi', 'PWA offline', 'khong quang cao'] },
    zh: { segment: 'download', slug: 'download', title: '下载 PureHub Android 版 | 免费无广告', description: '下载已签名的 PureHub Android 应用或安装 PWA，使用 25 个私密、无广告且支持离线的迷你应用。', keywords: ['下载PureHub', '免费Android应用', '离线PWA', '无广告APK'] },
  },
  changelog: {
    en: { segment: 'changelog', slug: 'changelog', title: 'PureHub Changelog & Release Notes', description: 'Follow PureHub releases, UX improvements, bug fixes, and new mini apps across the free, ad-free, open-source utility platform.', keywords: ['PureHub changelog', 'release notes', 'mini app updates', 'open source releases'] },
    vi: { segment: 'changelog', slug: 'changelog', title: 'Nhật Ký Cập Nhật & Phiên Bản PureHub', description: 'Theo dõi phiên bản PureHub, cải tiến UX, sửa lỗi và mini app mới trên nền tảng tiện ích miễn phí, không quảng cáo, mã nguồn mở.', keywords: ['cap nhat PureHub', 'nhat ky phien ban', 'mini app moi', 'ma nguon mo'] },
    zh: { segment: 'changelog', slug: 'changelog', title: 'PureHub 更新日志与发行说明', description: '查看 PureHub 的版本发布、体验改进、错误修复和新迷你应用，持续保持免费、无广告与开源。', keywords: ['PureHub更新日志', '发行说明', '迷你应用更新', '开源版本'] },
  },
}

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
        'Use a free offline bubble level with flat and edge modes, saved zero calibration, adjustable tolerance, and no ads or tracking.',
      keywords: ['bubble level offline', 'free bubble level no ads', 'phone spirit level', 'surface alignment tool', 'private sensor app'],
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
      title: 'Free Sound Meter & Decibel Meter | Private Sensor Suite',
      description:
        'Check current, peak, and rolling sound estimates with PureHub Sensor Suite. Microphone processing stays local and the tool has no ads.',
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
  'smart-flashlight': {
    en: {
      slug: 'smart-flashlight',
      title: 'Smart Flashlight Online | Free, No Ads',
      description:
        'Use a bright screen flashlight with adjustable color and quick controls. PureHub Smart Flashlight is free, lightweight, and ad-free.',
      keywords: ['smart flashlight online', 'screen flashlight', 'free flashlight no ads', 'browser flashlight'],
    },
    vi: {
      slug: 'den-pin-thong-minh',
      title: 'Đèn Pin Thông Minh Online | Miễn Phí, Không Quảng Cáo',
      description:
        'Dùng màn hình làm đèn pin sáng với màu sắc tùy chỉnh và điều khiển nhanh, miễn phí và không quảng cáo.',
      keywords: ['den pin thong minh', 'den pin man hinh', 'den pin online', 'khong quang cao'],
    },
    zh: {
      slug: 'zhi-neng-shou-dian',
      title: '智能在线手电筒 | 免费无广告',
      description:
        '使用可调颜色的明亮屏幕手电筒和快捷控制，PureHub 免费、轻量且无广告。',
      keywords: ['智能手电筒', '屏幕手电筒', '在线手电筒', '无广告工具'],
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
      title: 'Free QR & Barcode Scanner and Generator | PureHub',
      description:
        'Scan QR codes and barcodes, inspect links safely, create useful QR codes, and keep a private local history—free and without ads.',
      keywords: ['free qr scanner', 'barcode scanner', 'offline qr scanner', 'qr code generator', 'privacy qr tool', 'no ads qr studio'],
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
      title: 'Free Document Scanner to PDF | Private Document Suite',
      description:
        'Capture, reorder, rotate, frame, and export document pages to PDF locally. Pair with OCR Studio, with no uploads or ads.',
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
      title: 'Private OCR Studio & Document Scanner | No Ads',
      description:
        'Scan, clean, edit and export text from documents with PureHub OCR Studio, a private no-ads OCR workflow with a local library.',
      keywords: ['offline ocr', 'private document scanner', 'image to text no ads', 'browser ocr'],
    },
    vi: {
      slug: 'trich-xuat-van-ban',
      title: 'OCR Studio Riêng Tư | Quét Văn Bản Không Quảng Cáo',
      description:
        'Quét, làm sạch, chỉnh sửa và xuất văn bản với OCR Studio riêng tư, không quảng cáo và có thư viện lưu cục bộ.',
      keywords: ['ocr offline', 'trich xuat van ban', 'anh sang chu', 'khong quang cao'],
    },
    zh: {
      slug: 'ocr-wen-ben',
      title: '私密 OCR Studio | 无广告文档扫描',
      description:
        '使用 PureHub OCR Studio 在本地扫描、清理、编辑和导出文字，无广告并提供私密资料库。',
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
      title: 'Free Speaker Cleaner | Water Eject Tone, No Ads',
      description:
        'Run timed 150–185 Hz speaker-cleaning presets that may help move light moisture. Local playback, clear safety guidance, and no ads.',
      keywords: ['speaker cleaner', 'tone generator', 'audio cleaning tool', 'offline speaker utility'],
    },
    vi: {
      slug: 'lam-sach-loa',
      title: 'Làm Sạch Loa Offline | Phát Tần Số Hỗ Trợ',
      description:
        'Phát tần số cục bộ có thể hỗ trợ đẩy nước còn lại khỏi loa, kèm hướng dẫn an toàn và không tải dữ liệu lên mạng.',
      keywords: ['lam sach loa', 'tan so loa offline', 'audio tool', 'khong quang cao'],
    },
    zh: {
      slug: 'yang-sheng-qi-qing-jie',
      title: '离线扬声器清理 | 安全音频工具',
      description:
        '使用本地音频频率帮助移动扬声器中的残余水分，提供安全提示且无需上传数据。',
      keywords: ['扬声器清理', '离线音频工具', '频率发生器', '无广告'],
    },
  },
  'deep-cleaner': {
    en: {
      slug: 'deep-cleaner',
      title: 'Browser Storage Cleaner | Private Device Cleanup',
      description:
        'Review and clear PureHub local cache, temporary data, and offline storage with transparent controls and no invasive device permissions.',
      keywords: ['browser storage cleaner', 'clear pwa cache', 'private device cleanup', 'offline storage manager'],
    },
    vi: {
      slug: 'don-dep-thiet-bi',
      title: 'Dọn Dẹp Bộ Nhớ Trình Duyệt | Riêng Tư, An Toàn',
      description:
        'Kiểm tra và dọn cache, dữ liệu tạm cùng bộ nhớ offline của PureHub bằng điều khiển rõ ràng, không cần quyền xâm nhập thiết bị.',
      keywords: ['don dep bo nho trinh duyet', 'xoa cache pwa', 'don du lieu offline', 'cong cu rieng tu'],
    },
    zh: {
      slug: 'shen-du-qing-li',
      title: '浏览器存储清理 | 私密设备整理',
      description:
        '透明检查并清理 PureHub 本地缓存、临时数据与离线存储，无需侵入性设备权限。',
      keywords: ['浏览器存储清理', '清除PWA缓存', '离线数据整理', '隐私工具'],
    },
  },
  'wifi-analyzer': {
    en: {
      slug: 'wifi-analyzer',
      title: 'Wi-Fi Connection Analyzer | Private Browser Test',
      description:
        'Inspect browser-visible Wi-Fi connection quality, latency, and network status without ads or collecting your network data.',
      keywords: ['wifi analyzer browser', 'network latency test', 'connection quality checker', 'private wifi tool'],
    },
    vi: {
      slug: 'phan-tich-wifi',
      title: 'Phân Tích Kết Nối Wi-Fi | Kiểm Tra Riêng Tư',
      description:
        'Kiểm tra chất lượng kết nối, độ trễ và trạng thái mạng mà trình duyệt cho phép, không quảng cáo và không thu thập dữ liệu Wi-Fi.',
      keywords: ['phan tich wifi', 'kiem tra do tre mang', 'chat luong ket noi', 'wifi rieng tu'],
    },
    zh: {
      slug: 'wifi-fen-xi',
      title: 'Wi-Fi 连接分析 | 私密浏览器测试',
      description:
        '检查浏览器可见的网络质量、延迟与连接状态，不显示广告，也不收集 Wi-Fi 数据。',
      keywords: ['WiFi连接分析', '网络延迟测试', '连接质量检查', '隐私网络工具'],
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
  'wallpaper-changer': {
    en: {
      slug: 'wallpaper-changer',
      title: 'Wallpaper Preview & Changer | Free Browser Tool',
      description:
        'Preview wallpapers, crop images for your screen, and prepare a clean background locally with no uploads, ads, or tracking.',
      keywords: ['wallpaper changer browser', 'wallpaper preview tool', 'crop phone wallpaper', 'private image tool'],
    },
    vi: {
      slug: 'doi-hinh-nen',
      title: 'Xem Trước & Đổi Hình Nền | Công Cụ Miễn Phí',
      description:
        'Xem trước, cắt ảnh theo màn hình và chuẩn bị hình nền ngay trên thiết bị, không tải lên mạng và không quảng cáo.',
      keywords: ['doi hinh nen', 'cat anh hinh nen', 'xem truoc wallpaper', 'cong cu anh rieng tu'],
    },
    zh: {
      slug: 'bi-zhi-geng-huan',
      title: '壁纸预览与更换 | 免费浏览器工具',
      description:
        '在设备本地预览并裁剪适合屏幕的壁纸，无需上传、无广告，也不跟踪用户。',
      keywords: ['壁纸更换', '壁纸预览', '手机壁纸裁剪', '本地图像工具'],
    },
  },
  'bill-splitter': {
    en: {
      slug: 'bill-splitter',
      title: 'Free Bill Splitter | Group Expense Calculator, No Ads',
      description:
        'Split bills, tax, tips, and shared costs instantly with a private group expense calculator that works without sign-up or ads.',
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
      title: 'Free Offline Expense Tracker | Private Budget Ledger',
      description:
        'Track spending locally, review category trends, and export CSV with a mobile-friendly private expense ledger and no ads.',
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
  'community-pro-unlock': {
    en: {
      slug: 'community-pro-unlock',
      title: 'PureHub Community | Free Open-Source Mini Apps',
      description:
        'Join the PureHub community, suggest useful mini apps, report issues, and help keep private everyday tools free, open source, and ad-free.',
      keywords: ['PureHub community', 'open source mini apps', 'free no ads tools', 'community built apps'],
    },
    vi: {
      slug: 'mo-khoa-cong-dong',
      title: 'Cộng Đồng PureHub | Mini App Mã Nguồn Mở Miễn Phí',
      description:
        'Tham gia cộng đồng PureHub để đề xuất mini app, báo lỗi và cùng duy trì các công cụ riêng tư miễn phí, mã nguồn mở, không quảng cáo.',
      keywords: ['cong dong PureHub', 'mini app ma nguon mo', 'cong cu mien phi', 'khong quang cao'],
    },
    zh: {
      slug: 'she-qu-jie-suo',
      title: 'PureHub 社区 | 免费开源迷你应用',
      description:
        '加入 PureHub 社区，提出应用建议、报告问题，并共同维护免费、开源、无广告的私密日常工具。',
      keywords: ['PureHub社区', '开源迷你应用', '免费无广告工具', '社区共建应用'],
    },
  },
  'authenticator-vault': {
    en: { slug: 'authenticator-vault', title: 'Offline Authenticator App | Encrypted 2FA Vault', description: 'Generate TOTP two-factor authentication codes offline with encrypted on-device secrets, no account, no ads, and no tracking.', keywords: ['offline authenticator', 'TOTP generator', 'encrypted 2FA vault', 'authenticator no ads'] },
    vi: { slug: 'kho-xac-thuc', title: 'Trình Xác Thực Offline | Kho 2FA Mã Hóa', description: 'Tạo mã TOTP offline với khóa 2FA được mã hóa ngay trên thiết bị, không tài khoản, không quảng cáo và không theo dõi.', keywords: ['trinh xac thuc offline', 'ma TOTP', 'kho 2FA ma hoa', 'xac thuc khong quang cao'] },
    zh: { slug: 'yan-zheng-qi-bao-xian-ku', title: '离线验证器 | 加密 2FA 保险库', description: '离线生成 TOTP 双重验证码，密钥仅加密保存在设备上，无账户、无广告、无跟踪。', keywords: ['离线验证器', 'TOTP生成器', '加密2FA', '无广告验证器'] },
  },
  'file-studio': {
    en: { slug: 'file-studio', title: 'Private File Studio | ZIP, SHA-256 and Local Share', description: 'Hash, archive, extract and share local files without uploading them to a cloud service. Free, private and ad-free.', keywords: ['local zip tool', 'SHA-256 file hash', 'private file share', 'offline file utility'] },
    vi: { slug: 'xuong-tep', title: 'Xưởng Tệp Riêng Tư | ZIP, SHA-256 và Chia Sẻ', description: 'Băm, nén, giải nén và chia sẻ tệp cục bộ mà không tải chúng lên dịch vụ đám mây.', keywords: ['nen zip offline', 'bam tep SHA-256', 'chia se tep rieng tu', 'cong cu tep'] },
    zh: { slug: 'wen-jian-gong-zuo-shi', title: '私密文件工作室 | ZIP、SHA-256 与本地分享', description: '无需上传云端即可在本地计算哈希、压缩、解压和分享文件。', keywords: ['本地ZIP工具', 'SHA-256文件哈希', '私密文件分享', '离线文件工具'] },
  },
  'screen-recorder': {
    en: { slug: 'screen-recorder', title: 'Free Private Screen Recorder | No Uploads', description: 'Record a browser tab, window or screen with explicit permission, then preview and download locally without cloud uploads.', keywords: ['free screen recorder', 'browser screen recorder', 'private screen capture', 'screen recorder no ads'] },
    vi: { slug: 'quay-man-hinh', title: 'Quay Màn Hình Miễn Phí | Không Tải Lên Cloud', description: 'Quay tab, cửa sổ hoặc màn hình với quyền rõ ràng rồi xem trước và tải cục bộ, không tải lên cloud.', keywords: ['quay man hinh mien phi', 'quay man hinh trinh duyet', 'quay rieng tu', 'khong quang cao'] },
    zh: { slug: 'ping-mu-lu-zhi', title: '免费私密屏幕录制 | 不上传云端', description: '经明确授权录制浏览器标签页、窗口或屏幕，并在本地预览和下载。', keywords: ['免费屏幕录制', '浏览器录屏', '私密屏幕捕获', '无广告录屏'] },
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
  const sitePaths = SEO_SITE_PAGE_IDS.flatMap((pageId) =>
    SEO_LANGUAGES.map((lang) => {
      const segment = seoSiteMeta[pageId][lang].segment
      return `/${lang}${segment ? `/${segment}` : ''}`
    }),
  )
  return [...sitePaths, ...seoRouteEntries.map((entry) => entry.path)]
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

export function buildSitePageUrl(lang: SeoLanguage, pageId: SeoSitePageId) {
  const segment = seoSiteMeta[pageId][lang].segment
  return `${SITE_ORIGIN}/${lang}${segment ? `/${segment}` : ''}`
}

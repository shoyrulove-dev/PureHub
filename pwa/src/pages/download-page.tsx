import { useEffect, useState } from 'react'
import { CheckCircle2, Code2, Download, FileArchive, Globe2, RefreshCw, ShieldCheck, Smartphone } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { normalizeLocale } from '../i18n/locales'
import { trackJourneyEvent } from '../lib/community-api'

const DOWNLOAD_COPY = {
  en: { eyebrow: 'Android preview', title: 'PureHub for Android', intro: 'Free, ad-free, and open source. This page always shows the latest signed GitHub build and its SHA-256 checksum.', checking: 'Checking the latest signed build…', unavailable: 'Release service is temporarily unavailable.', github: 'Check GitHub releases', preparing: 'The first signed preview is being prepared', preparingHint: 'The web app remains available. Android downloads appear here after signing and checksum verification.', follow: 'Follow releases on GitHub', preview: 'Preview release', stable: 'Stable release', download: 'Download signed APK', pending: 'APK upload pending', view: 'View on GitHub', android: 'Android beta: 26 native tools', androidHint: 'The lightweight signed APK includes the full Android catalog and is optimized for modern ARM64 phones.', pwa: 'PWA: 26 web tools', pwaHint: 'Install from your browser for the same catalog, with browser limits explained inside each tool.', signed: 'Signed release builds', private: 'No ads or data sales', source: 'Source available on GitHub', history: 'Want the release history?', changelog: 'Open changelog' },
  vi: { eyebrow: 'Bản thử Android', title: 'PureHub cho Android', intro: 'Miễn phí, không quảng cáo và mã nguồn mở. Trang này luôn hiển thị bản GitHub đã ký mới nhất cùng mã kiểm SHA-256.', checking: 'Đang kiểm tra bản đã ký mới nhất…', unavailable: 'Dịch vụ phát hành tạm thời không khả dụng.', github: 'Xem bản phát hành GitHub', preparing: 'Bản thử đã ký đầu tiên đang được chuẩn bị', preparingHint: 'Ứng dụng web vẫn hoạt động. Bản tải Android sẽ xuất hiện sau khi ký và xác minh mã kiểm.', follow: 'Theo dõi bản phát hành trên GitHub', preview: 'Bản thử nghiệm', stable: 'Bản ổn định', download: 'Tải APK đã ký', pending: 'APK đang được tải lên', view: 'Xem trên GitHub', android: 'Android beta: 26 công cụ gốc', androidHint: 'APK nhẹ đã ký gồm đầy đủ công cụ Android và được tối ưu cho điện thoại ARM64 hiện đại.', pwa: 'PWA: 26 công cụ web', pwaHint: 'Cài từ trình duyệt để dùng cùng danh mục; giới hạn trình duyệt được ghi rõ trong từng công cụ.', signed: 'Bản phát hành đã ký', private: 'Không quảng cáo hay bán dữ liệu', source: 'Mã nguồn có trên GitHub', history: 'Bạn muốn xem lịch sử phát hành?', changelog: 'Mở nhật ký thay đổi' },
  zh: { eyebrow: 'Android 预览版', title: 'PureHub Android 版', intro: '免费、无广告且开源。本页始终显示最新的 GitHub 签名版本及其 SHA-256 校验值。', checking: '正在检查最新签名版本…', unavailable: '发布服务暂时不可用。', github: '查看 GitHub 版本', preparing: '首个签名预览版正在准备中', preparingHint: '网页应用仍可使用。Android 下载将在签名和校验完成后显示。', follow: '在 GitHub 上关注版本', preview: '预览版', stable: '稳定版', download: '下载签名 APK', pending: 'APK 正在上传', view: '在 GitHub 查看', android: 'Android 测试版：26 个原生工具', androidHint: '轻量签名 APK 包含完整 Android 工具集，并针对现代 ARM64 手机优化。', pwa: 'PWA：26 个网页工具', pwaHint: '可从浏览器安装相同工具集；每个工具内会说明浏览器限制。', signed: '签名发布版本', private: '无广告，不出售数据', source: 'GitHub 提供源代码', history: '想查看版本历史？', changelog: '打开更新日志' },
  es: { eyebrow: 'Vista previa de Android', title: 'PureHub para Android', intro: 'Gratis, sin anuncios y de código abierto. Esta página siempre muestra la compilación firmada más reciente y su suma SHA-256.', checking: 'Comprobando la última versión firmada…', unavailable: 'El servicio de versiones no está disponible temporalmente.', github: 'Ver versiones en GitHub', preparing: 'Se está preparando la primera versión firmada', preparingHint: 'La aplicación web sigue disponible. Las descargas Android aparecerán tras la firma y la verificación.', follow: 'Seguir versiones en GitHub', preview: 'Versión preliminar', stable: 'Versión estable', download: 'Descargar APK firmado', pending: 'Carga del APK pendiente', view: 'Ver en GitHub', android: 'Android beta: 26 herramientas nativas', androidHint: 'El APK firmado y ligero incluye todo el catálogo Android y está optimizado para móviles ARM64 modernos.', pwa: 'PWA: 26 herramientas web', pwaHint: 'Instálala desde el navegador con el mismo catálogo; cada herramienta explica sus límites web.', signed: 'Versiones firmadas', private: 'Sin anuncios ni venta de datos', source: 'Código disponible en GitHub', history: '¿Quieres ver el historial?', changelog: 'Abrir registro de cambios' },
} as const

type Release = {
  release_id: string
  version: string
  title: string
  summary: string
  github_url: string
  apk_url: string
  aab_url: string
  sha256: string
  prerelease: boolean
  published_at?: string
}

async function loadReleases(signal: AbortSignal): Promise<Release[]> {
  const response = await fetch('/public-api/releases', { signal })
  if (!response.ok) throw new Error('Release service is temporarily unavailable.')
  const payload = (await response.json()) as { items?: Release[] }
  return payload.items ?? []
}

export function DownloadPage() {
  const { lang } = useParams()
  const locale = normalizeLocale(lang)
  const copy = DOWNLOAD_COPY[locale]
  const [releases, setReleases] = useState<Release[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    loadReleases(controller.signal)
      .then(setReleases)
      .catch(() => {
        if (!controller.signal.aborted) setError(copy.unavailable)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [copy.unavailable])

  const latest = releases[0]

  return (
    <section className="space-y-6">
      <div className="hero-panel">
        <span className="eyebrow"><Download className="size-4" /> {copy.eyebrow}</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          {copy.title}
        </h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-600 dark:text-slate-300">
          {copy.intro}
        </p>
      </div>

      {loading && (
        <div className="app-surface flex items-center gap-3 rounded-[18px] p-5 text-slate-500">
          <RefreshCw className="size-5 animate-spin" /> {copy.checking}
        </div>
      )}

      {!loading && error && (
        <div className="app-surface rounded-[18px] p-5">
          <p className="font-semibold text-rose-600 dark:text-rose-300">{error}</p>
          <a className="text-link mt-3" href="https://github.com/shoyrulove-dev/PureHub/releases" target="_blank" rel="noreferrer">
            <Code2 className="size-4" /> {copy.github}
          </a>
        </div>
      )}

      {!loading && !error && !latest && (
        <div className="app-surface rounded-[18px] p-6">
          <h2 className="text-xl font-bold text-slate-950 dark:text-white">{copy.preparing}</h2>
          <p className="mt-2 leading-7 text-slate-500 dark:text-slate-400">
            {copy.preparingHint}
          </p>
          <a className="text-link mt-4" href="https://github.com/shoyrulove-dev/PureHub/releases" target="_blank" rel="noreferrer">
            {copy.follow}
          </a>
        </div>
      )}

      {latest && (
        <article className="app-surface rounded-[20px] p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="eyebrow">{latest.prerelease ? copy.preview : copy.stable}</span>
              <h2 className="mt-3 text-2xl font-bold text-slate-950 dark:text-white">{latest.title}</h2>
              <p className="mt-2 max-w-2xl leading-7 text-slate-500 dark:text-slate-400">{latest.summary}</p>
            </div>
            <span className="rounded-full bg-emerald-500/10 px-3 py-1.5 text-sm font-bold text-emerald-700 dark:text-emerald-300">
              v{latest.version}
            </span>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {latest.apk_url ? (
              <a className="primary-button justify-center" href={latest.apk_url} onClick={() => void trackJourneyEvent('apk_download_click')}>
                <Download className="size-4" /> {copy.download}
              </a>
            ) : (
              <span className="flex min-h-11 items-center justify-center rounded-[14px] bg-slate-500/8 px-4 text-sm font-semibold text-slate-500">
                {copy.pending}
              </span>
            )}
            {latest.github_url && (
              <a className="secondary-button justify-center" href={latest.github_url} target="_blank" rel="noreferrer">
                <Code2 className="size-4" /> {copy.view}
              </a>
            )}
          </div>

          {latest.sha256 && (
            <div className="mt-5 rounded-[16px] bg-slate-500/7 p-4">
              <p className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                <ShieldCheck className="size-4 text-emerald-500" /> APK SHA-256
              </p>
              <code className="mt-2 block break-all text-xs leading-5 text-slate-500 dark:text-slate-400">{latest.sha256}</code>
            </div>
          )}
        </article>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="app-surface rounded-[18px] p-4">
          <p className="flex items-center gap-2 text-sm font-bold text-slate-950 dark:text-white"><Smartphone className="size-4 text-emerald-500" /> {copy.android}</p>
          <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{copy.androidHint}</p>
        </div>
        <div className="app-surface rounded-[18px] p-4">
          <p className="flex items-center gap-2 text-sm font-bold text-slate-950 dark:text-white"><Globe2 className="size-4 text-sky-500" /> {copy.pwa}</p>
          <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{copy.pwaHint}</p>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="promise-card"><ShieldCheck className="size-5 text-emerald-500" /><span>{copy.signed}</span></div>
        <div className="promise-card"><CheckCircle2 className="size-5 text-sky-500" /><span>{copy.private}</span></div>
        <div className="promise-card"><FileArchive className="size-5 text-violet-500" /><span>{copy.source}</span></div>
      </div>

      <p className="text-center text-sm text-slate-500">
        {copy.history} <Link className="font-semibold text-emerald-600 dark:text-emerald-300" to={`/${locale}/changelog`}>{copy.changelog}</Link>
      </p>
    </section>
  )
}

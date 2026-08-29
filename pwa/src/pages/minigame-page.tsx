import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CalendarClock, CircleCheck, Gift, Smartphone, Trophy, X } from 'lucide-react'

declare global { interface Window { turnstile?: { render: (element: HTMLElement, options: { sitekey: string; callback: (token: string) => void; 'expired-callback': () => void }) => void } } }
type Campaign = { state: string; day: string; valid_entries: number; closes_at: string; winner_limit: number; prize_vnd: number; turnstile_site_key: string }
type Winner = { entry_id: string; phone: string; gmail: string; payout_status?: 'pending' | 'paid' | 'failed' }
type Result = { status: string; winning_number?: string; winner_count: number; winners: Winner[] }

const exampleWinners: Winner[] = [
  { entry_id: 'PH-8A31', phone: '*** 4821', gmail: '***nh@gmail.com', payout_status: 'paid' },
  { entry_id: 'PH-6C92', phone: '*** 1976', gmail: '***minh@gmail.com', payout_status: 'paid' },
  { entry_id: 'PH-2F18', phone: '*** 8305', gmail: '***linh@gmail.com', payout_status: 'pending' },
]

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/public-api/minigame${path}`, init)
  const data = await response.json().catch(() => ({})) as T & { detail?: string }
  if (!response.ok) throw new Error(data.detail || 'Không thể kết nối Minigame.')
  return data
}

function WinnerList({ winners }: { winners: Winner[] }) {
  return <ul className="mt-4 divide-y divide-slate-100 overflow-hidden rounded-2xl border border-slate-100 dark:divide-slate-800 dark:border-slate-800">
    {winners.map((winner, index) => <li key={winner.entry_id} className="flex items-center gap-3 bg-slate-50 px-3 py-3 text-sm dark:bg-slate-950/40">
      <span className="grid size-7 shrink-0 place-items-center rounded-full bg-amber-100 text-xs font-black text-amber-700">{index + 1}</span>
      <span className="min-w-0 flex-1"><b className="block text-slate-800 dark:text-white">{winner.phone}</b><span className="block truncate text-xs text-slate-500">{winner.gmail}</span></span>
      <span className={`rounded-lg px-2 py-1 text-[11px] font-bold ${winner.payout_status === 'paid' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>{winner.payout_status === 'paid' ? 'Đã nạp thẻ' : 'Đang nạp thẻ'}</span>
    </li>)}
  </ul>
}

export function MinigamePage() {
  const [params] = useSearchParams()
  const ticket = params.get('ticket') || ''
  const [campaign, setCampaign] = useState<Campaign | null>(null)
  const [result, setResult] = useState<Result | null>(null)
  const [number, setNumber] = useState(''); const [phone, setPhone] = useState(''); const [gmail, setGmail] = useState('')
  const [notice, setNotice] = useState(''); const [turnstileToken, setTurnstileToken] = useState(''); const [showAndroidPrompt, setShowAndroidPrompt] = useState(false)
  const turnstileRef = useRef<HTMLDivElement>(null)
  const canEnter = Boolean(ticket && campaign?.state === 'open')
  const prize = useMemo(() => new Intl.NumberFormat('vi-VN').format(campaign?.prize_vnd ?? 10000), [campaign])

  useEffect(() => {
    const sections = Array.from(document.querySelectorAll<HTMLElement>('main[data-minigame-reveal] > section'))
    if (!sections.length) return

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
      sections.forEach(section => section.classList.add('is-visible'))
      return
    }

    document.documentElement.classList.add('reveal-ready')
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        observer.unobserve(entry.target)
      })
    }, { threshold: 0.08, rootMargin: '0px 0px -4% 0px' })

    sections.forEach((section, index) => {
      section.dataset.scrollReveal = ''
      section.style.setProperty('--reveal-delay', `${Math.min(index % 3, 2) * 45}ms`)
      observer.observe(section)
    })

    return () => observer.disconnect()
  }, [])

  useEffect(() => { void api<Campaign>('').then(setCampaign).catch((error: Error) => setNotice(error.message)) }, [])
  useEffect(() => { if (campaign?.day) void api<Result>(`/results/${campaign.day}`).then(setResult).catch(() => undefined) }, [campaign?.day])
  useEffect(() => {
    if (!campaign?.turnstile_site_key || !turnstileRef.current) return
    const render = () => window.turnstile?.render(turnstileRef.current!, { sitekey: campaign.turnstile_site_key, callback: setTurnstileToken, 'expired-callback': () => setTurnstileToken('') })
    const existing = document.querySelector('script[data-purehub-turnstile]')
    if (existing) render()
    else { const script = document.createElement('script'); script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'; script.async = true; script.dataset.purehubTurnstile = 'true'; script.onload = render; document.head.appendChild(script) }
  }, [campaign?.turnstile_site_key])

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!canEnter) { setShowAndroidPrompt(true); return }
    setNotice('Đang gửi lượt dự đoán…')
    try { const entry = await api<{ entry_id: string; picked_number: string }>('/entries', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ticket, picked_number: number, phone_number: phone, gmail, turnstile_token: turnstileToken }) }); setNotice(`Đã chốt số ${entry.picked_number}. Mã lượt chơi: ${entry.entry_id}`) }
    catch (error) { setNotice(error instanceof Error ? error.message : 'Không thể gửi lượt chơi.') }
  }

  const settled = result?.status === 'settled'
  return <main data-minigame-reveal className="mx-auto min-h-screen max-w-2xl px-4 py-8 text-slate-900 dark:text-white">
    <section className="overflow-hidden rounded-[28px] bg-gradient-to-br from-violet-700 via-fuchsia-600 to-amber-500 p-6 text-white shadow-xl"><Gift className="size-10" /><p className="mt-5 text-xs font-black uppercase tracking-[.18em] text-white/75">PureHub beta tester</p><h1 className="mt-2 text-3xl font-black leading-tight">Dự đoán 2 số<br />Nhận thẻ điện thoại mỗi ngày</h1><p className="mt-3 leading-7 text-white/90">Chọn số từ 00–99 trước 18:00. 5 bạn đoán đúng sớm nhất nhận thẻ điện thoại mệnh giá {prize}đ.</p>{!ticket && <Link to="/vi/download" className="mt-5 flex min-h-12 items-center justify-center rounded-2xl bg-emerald-400 px-4 text-center font-black text-emerald-950 shadow-lg transition hover:bg-emerald-300">📲 Tải App PureHub (APK) ngay để chốt số</Link>}</section>
    <section className="mt-5 grid gap-3 sm:grid-cols-3"><div className="rounded-2xl border border-violet-100 bg-violet-50 p-4 text-slate-900 dark:border-violet-900 dark:bg-violet-950/40 dark:text-white"><Trophy className="size-5 text-violet-600 dark:text-violet-300" /><b className="mt-3 block text-lg">{campaign?.winner_limit ?? 5} người</b><span className="text-sm text-slate-600 dark:text-slate-300">đoán đúng sớm nhất</span></div><div className="rounded-2xl border border-amber-100 bg-amber-50 p-4 text-slate-900 dark:border-amber-900 dark:bg-amber-950/35 dark:text-white"><Gift className="size-5 text-amber-600 dark:text-amber-300" /><b className="mt-3 block text-lg">{prize}đ</b><span className="text-sm text-slate-600 dark:text-slate-300">thẻ điện thoại / người</span></div><div className="rounded-2xl border border-emerald-100 bg-emerald-50 p-4 text-slate-900 dark:border-emerald-900 dark:bg-emerald-950/35 dark:text-white"><CalendarClock className="size-5 text-emerald-600 dark:text-emerald-300" /><b className="mt-3 block text-lg">{campaign?.closes_at ?? '18:00'}</b><span className="text-sm text-slate-600 dark:text-slate-300">đóng dự đoán</span></div></section>
    <section className="mt-5 rounded-[22px] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900"><div className="flex gap-3"><Trophy className="size-6 shrink-0 text-amber-500" /><div><strong>Thể lệ minh bạch</strong><ol className="mt-2 list-decimal space-y-1 pl-5 text-sm leading-6 text-slate-600 dark:text-slate-300"><li>Dự đoán 2 số cuối giải đặc biệt XSMB.</li><li>Mỗi SĐT và Gmail có một lượt mỗi ngày.</li><li>5 người đoán đúng sớm nhất nhận thẻ điện thoại {prize}đ.</li><li>Gmail chỉ dùng để nhận diện lượt beta, không đăng ký marketing.</li></ol></div></div><p className="mt-4 rounded-xl bg-slate-50 px-3 py-2 text-sm dark:bg-slate-950">Hôm nay: <b>{campaign?.valid_entries ?? 0}</b> lượt hợp lệ · chốt lúc <b>{campaign?.closes_at ?? '18:00'}</b>.</p></section>
    {!ticket && <section className="mt-5 rounded-[22px] border border-amber-200 bg-amber-50 p-5 text-amber-950"><Smartphone className="size-6" /><h2 className="mt-2 font-bold">Tham gia từ ứng dụng PureHub trên Android</h2><p className="mt-1 text-sm leading-6">Mở ứng dụng PureHub đã cài trên điện thoại Android, vào mục Cộng đồng và chọn Minigame để chốt số.</p><button type="button" onClick={() => setShowAndroidPrompt(true)} className="mt-3 w-full bg-emerald-600">📲 Tải App PureHub để tham gia</button></section>}
    <section className="relative mt-5 rounded-[22px] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900"><form onSubmit={submit} className={!canEnter ? 'pointer-events-none opacity-40 blur-[1px]' : 'space-y-4'}><h2 className="font-bold text-slate-950 dark:text-white">Chọn số hôm nay</h2><input value={number} onChange={e => setNumber(e.target.value.replace(/\D/g, '').slice(0, 2))} placeholder="00 – 99" inputMode="numeric" required disabled={!canEnter} className="mt-4 w-full rounded-xl border border-slate-300 bg-white p-3 text-slate-950 caret-violet-600 placeholder:text-slate-500 focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-500/30 dark:border-slate-600 dark:bg-slate-950 dark:text-white dark:placeholder:text-slate-300"/><input value={phone} onChange={e => setPhone(e.target.value)} placeholder="Số điện thoại nhận thẻ" inputMode="tel" required disabled={!canEnter} className="mt-4 w-full rounded-xl border border-slate-300 bg-white p-3 text-slate-950 caret-violet-600 placeholder:text-slate-500 focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-500/30 dark:border-slate-600 dark:bg-slate-950 dark:text-white dark:placeholder:text-slate-300"/><input value={gmail} onChange={e => setGmail(e.target.value)} placeholder="Gmail" type="email" required disabled={!canEnter} className="mt-4 w-full rounded-xl border border-slate-300 bg-white p-3 text-slate-950 caret-violet-600 placeholder:text-slate-500 focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-500/30 dark:border-slate-600 dark:bg-slate-950 dark:text-white dark:placeholder:text-slate-300"/><div className="mt-4 overflow-hidden rounded-xl" ref={turnstileRef} /><button disabled={!canEnter || !turnstileToken} className="mt-4 w-full rounded-xl bg-violet-600 p-3 font-black text-white disabled:bg-slate-600 disabled:text-slate-200 disabled:opacity-70">Chốt số dự đoán</button></form>{!canEnter && <button type="button" aria-label="Mở hướng dẫn tham gia trên Android" onClick={() => setShowAndroidPrompt(true)} className="absolute inset-0 grid h-full w-full place-items-center rounded-[22px] bg-transparent text-base font-black text-violet-700"><span className="rounded-2xl bg-white/95 px-5 py-4 shadow-xl">🔒 Chạm để mở app PureHub trên Android</span></button>}{notice && <p role="status" className="mt-3 text-sm leading-6 text-slate-700 dark:text-slate-200">{notice}</p>}</section>
    <section className="mt-5 rounded-[22px] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900"><div className="flex items-start gap-3"><CircleCheck className="mt-0.5 size-6 shrink-0 text-emerald-500" /><div><h2 className="font-bold">Kết quả ngày {campaign?.day ?? '…'}</h2><p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{settled ? `Số trúng: ${result.winning_number} · ${result.winner_count} người nhận thẻ` : 'Kết quả sẽ được công bố sau 18:30.'}</p></div></div>{settled ? <WinnerList winners={result.winners} /> : <div className="mt-4 rounded-2xl border border-dashed border-slate-300 p-4 dark:border-slate-700"><p className="text-xs font-bold uppercase tracking-wide text-slate-500">Ví dụ hiển thị sau khi công bố</p><p className="mt-1 text-sm"><b>Số trúng: 45</b> · 5 người nhận thẻ điện thoại 10.000đ</p><WinnerList winners={exampleWinners} /><p className="mt-3 text-xs text-slate-500">SĐT và Gmail luôn được che một phần; chỉ người quản trị thấy thông tin đầy đủ để nạp thẻ.</p></div>}</section>
    {showAndroidPrompt && <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 grid place-items-center bg-slate-950/65 p-5"><section className="relative w-full max-w-sm rounded-[28px] bg-white p-6 text-slate-900 shadow-2xl"><button onClick={() => setShowAndroidPrompt(false)} className="absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-slate-100 p-0 text-slate-600"><X className="size-5" /></button><Smartphone className="size-10 text-emerald-600" /><h2 className="mt-4 text-xl font-black">Vui lòng mở App PureHub trên Android để tham gia nhận lộc!</h2><p className="mt-3 text-sm leading-6 text-slate-600">Cài đặt app trên điện thoại Android, vào <b>Cộng đồng → Minigame</b>. App sẽ mở trang chốt số hợp lệ cho bạn.</p><Link to="/vi/download" className="mt-5 flex min-h-12 items-center justify-center rounded-2xl bg-emerald-500 px-4 text-center font-black text-emerald-950">📲 Tải App PureHub (APK) ngay</Link><button onClick={() => setShowAndroidPrompt(false)} className="mt-3 w-full border-0 bg-transparent text-slate-500">Để xem kết quả thôi</button></section></div>}
  </main>
}

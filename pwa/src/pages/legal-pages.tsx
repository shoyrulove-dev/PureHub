import { ExternalLink, FileText, ShieldCheck } from 'lucide-react'

const GITHUB_URL = 'https://github.com/shoyrulove-dev/PureHub'
const EFFECTIVE_DATE = 'August 2, 2026'

function LegalPage({ kind }: { kind: 'privacy' | 'terms' }) {
  const privacy = kind === 'privacy'

  return (
    <article className="space-y-5">
      <header className="hero-panel">
        <span className="eyebrow">
          {privacy ? <ShieldCheck className="size-4" /> : <FileText className="size-4" />}
          {privacy ? 'Privacy first' : 'Open and fair'}
        </span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
          {privacy ? 'Privacy Policy' : 'Terms of Service'}
        </h1>
        <p className="mt-3 text-sm text-slate-500">Effective {EFFECTIVE_DATE}</p>
      </header>

      {privacy ? <PrivacyContent /> : <TermsContent />}

      <section className="app-surface rounded-[18px] p-5">
        <h2 className="font-bold text-slate-950 dark:text-white">Questions</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
          Open a public issue or discussion in the PureHub repository for privacy, terms, or product questions.
        </p>
        <a className="text-link mt-3" href={`${GITHUB_URL}/discussions`} target="_blank" rel="noreferrer">
          Contact PureHub on GitHub <ExternalLink className="size-4" />
        </a>
      </section>
    </article>
  )
}

function PrivacyContent() {
  return (
    <section className="app-surface space-y-5 rounded-[18px] p-5 text-sm leading-7 text-slate-600 dark:text-slate-300">
      <div><h2 className="font-bold text-slate-950 dark:text-white">Our approach</h2><p className="mt-1">PureHub is a free, ad-free, open-source collection of mini tools. The public app does not require an account, sell personal data, or include advertising trackers.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Information and local storage</h2><p className="mt-1">Tool preferences and content are designed to remain in your browser or Android device whenever possible. You can remove local data by using PureHub settings, clearing browser storage, or uninstalling the app.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Device permissions</h2><p className="mt-1">Some tools may request camera, microphone, motion, location, files, or notification access. Permission is requested only when a feature needs it and can be withdrawn in your device or browser settings.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Website infrastructure</h2><p className="mt-1">Our hosting and security providers may process standard request data such as IP address, browser type, timestamps, and error logs to deliver and protect the service. PureHub does not use this information for advertising profiles.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Community and external services</h2><p className="mt-1">Links to GitHub, Telegram, TikTok, YouTube, and other community services are governed by those services' own policies. Information you submit there is shared with the service you choose.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Changes</h2><p className="mt-1">Material updates will be reflected on this page with a revised effective date and may also be announced in the public changelog.</p></div>
    </section>
  )
}

function TermsContent() {
  return (
    <section className="app-surface space-y-5 rounded-[18px] p-5 text-sm leading-7 text-slate-600 dark:text-slate-300">
      <div><h2 className="font-bold text-slate-950 dark:text-white">Using PureHub</h2><p className="mt-1">You may use PureHub for lawful personal or professional purposes. Do not use the service to harm others, bypass security, distribute unlawful material, or disrupt the service.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">No account or payment required</h2><p className="mt-1">The public mini tools are provided without mandatory registration, advertising, or a paid Pro tier. Optional third-party community services may have separate terms.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Open-source license</h2><p className="mt-1">PureHub source code is available under the license included in its GitHub repository. Brand assets, third-party libraries, and platform services may have their own licenses and rules.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">No professional advice</h2><p className="mt-1">Measurements, calculations, wellness tools, security utilities, and financial helpers are informational tools. They are not medical, legal, financial, safety, or other professional advice.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Availability and warranty</h2><p className="mt-1">PureHub is provided on an “as is” and “as available” basis. We work to keep it useful and reliable but cannot guarantee uninterrupted operation, exact measurements, or suitability for every purpose.</p></div>
      <div><h2 className="font-bold text-slate-950 dark:text-white">Changes</h2><p className="mt-1">We may improve, replace, or discontinue features and update these terms. Continuing to use PureHub after an update means you accept the revised terms.</p></div>
    </section>
  )
}

export function PrivacyPage() { return <LegalPage kind="privacy" /> }
export function TermsPage() { return <LegalPage kind="terms" /> }

# PureHub SEO operations

## Current inventory

The PWA publishes a controlled inventory of exactly 500 static SEO pages:

- 26 miniapp pages × 3 locales.
- 5 site pages × 3 locales.
- Growth landing pages.
- 134 reviewed fixed converter pairs × 3 locales.

The sitemap is generated from these allowlists during the production build. It is not generated from arbitrary user input, search queries, or converter values.

## Thin-content rule

Every indexable landing page must contain meaningful server-rendered HTML before JavaScript runs:

1. A specific H1.
2. A useful quick answer or introduction.
3. A static table, worked example, or workflow-specific explanation when the page is a calculator/converter.
4. A formula or method explanation where applicable.
5. FAQ content that is visible on the page and matches the FAQ JSON-LD.
6. A clear link to the real PureHub tool.

Do not create pages such as `100-usd-to-vnd`, `101-usd-to-vnd`, or URLs with `?amount=`. Values belong in the client-side tool; SEO URLs represent stable topics or unit pairs.

## Release checks

`npm run build` runs the prerender validator. It rejects:

- Search Console verification files in the sitemap.
- Duplicate sitemap URLs.
- Parameterized SEO URLs.
- Quantity-like converter URLs.
- Anything other than exactly 500 indexable URLs in the current inventory.
- Representative pages without static H1/content, tables, formulas, or schema.

When adding a page, add it to the relevant typed allowlist and add one representative route to `scripts/validate-prerender.mjs`.

## Backlinks and user sharing

PureHub must not auto-create backlinks, reciprocal links, paid links, comment links, or links on third-party platforms. A user may choose to share a useful tool URL, and an editorial site may link to a page because it is useful. That is a natural referral signal, not an automated backlink system.

Share links from the app should be user-initiated and use the canonical page URL. User-generated external links should be treated as `ugc` where PureHub controls the outbound markup.

## Monitoring

- Submit the root sitemap once in Search Console: `https://hub.blissbiovn.com/sitemap.xml`.
- Review Page indexing and URL Inspection after each meaningful batch, not after every URL.
- Use GA4 to compare landing-page views, tool starts, completed actions, and return visits by locale.
- Keep pages that show impressions and useful engagement; improve or remove pages that remain low-value.
- Do not treat a larger sitemap as growth by itself. Indexing, impressions, tool starts, and completed workflows are the decision signals.

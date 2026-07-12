from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Iterable

from openai import OpenAI

from database import OUTPUT_DIR, create_article_job, get_config_value

DEFAULT_KEYWORDS = [
    "offline compass app",
    "best offline utility apps",
    "private expense tracker pwa",
    "offline qr scanner no ads",
    "offline lunar calendar app",
]


def build_openai_client() -> OpenAI:
    api_key = get_config_value("grok_api_key")
    if not api_key:
        raise RuntimeError("Missing Grok API key in config table.")

    return OpenAI(
        api_key=api_key,
        base_url="https://api.x.ai/v1",
    )


def article_prompt(keyword: str, site_url: str) -> str:
    return f"""
Write a 400-word SEO-optimized markdown article for PureHub.

Context:
- Product: PureHub
- Product URL: {site_url}
- Theme: privacy-first offline utility apps, zero-ads, progressive web app tools
- Keyword: {keyword}

Requirements:
- Output clean markdown only
- Start with one H1 title
- Mention PureHub naturally
- Add one short intro, 3-4 subheadings, and one short conclusion
- Keep tone helpful and organic
- Do not invent fake benchmarks or fake testimonials
- Include one short CTA that points readers to {site_url}
""".strip()


def generate_articles(keywords: Iterable[str] | None = None) -> list[dict[str, str]]:
    client = build_openai_client()
    keyword_list = list(keywords or DEFAULT_KEYWORDS)
    model_name = get_config_value("grok_model", "grok-2")
    site_url = get_config_value("site_url", "https://hub.blissbiovn.com")

    results: list[dict[str, str]] = []

    for keyword in keyword_list:
        response = client.responses.create(
            model=model_name or "grok-2",
            input=article_prompt(keyword, site_url),
        )

        markdown = response.output_text.strip()
        title_line = next(
            (line.replace("#", "").strip() for line in markdown.splitlines() if line.strip().startswith("#")),
            keyword.title(),
        )
        slug = keyword.lower().replace(" ", "-")
        timestamp = datetime.utcnow().strftime("%Y%m%d-%H%M%S")
        filename = f"{timestamp}-{slug}.mdx"
        output_path = OUTPUT_DIR / filename
        output_path.write_text(markdown, encoding="utf-8")
        create_article_job(filename, title_line, keyword, "generated")

        results.append(
            {
                "keyword": keyword,
                "title": title_line,
                "filename": filename,
                "path": str(output_path),
            }
        )

    return results

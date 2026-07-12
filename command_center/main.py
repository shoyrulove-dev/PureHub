from __future__ import annotations

from pathlib import Path
from typing import Literal
from urllib.parse import quote_plus

from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates

from content_generator import DEFAULT_KEYWORDS, generate_articles
from database import (
    CONFIG_DEFAULTS,
    get_dashboard_metrics,
    get_user_stats,
    init_database,
    list_article_jobs,
    list_config,
    list_top_referrers,
    update_config,
)
from devto_publisher import publish_articles
from telegram_bot_worker import telegram_bot_manager

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

app = FastAPI(
    title="PureHub Command Center",
    summary="Admin panel and automation control surface for PureHub growth systems.",
    version="0.2.0",
)

ADMIN_PREFIX = "/admin"
API_PREFIX = f"{ADMIN_PREFIX}/api"


@app.on_event("startup")
def on_startup() -> None:
    init_database()


@app.get("/", include_in_schema=False)
def root_redirect() -> RedirectResponse:
    return RedirectResponse(url=ADMIN_PREFIX, status_code=307)


@app.get(ADMIN_PREFIX, response_class=HTMLResponse)
def dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info", "error"] = "success",
) -> HTMLResponse:
    return templates.TemplateResponse(
        request=request,
        name="index.html",
        context={
            "config": list_config(),
            "defaults": CONFIG_DEFAULTS,
            "stats": get_user_stats(),
            "metrics": get_dashboard_metrics(),
            "jobs": list_article_jobs(),
            "top_referrers": list_top_referrers(),
            "bot_state": telegram_bot_manager.state,
            "message": message,
            "message_type": message_type,
            "admin_prefix": ADMIN_PREFIX,
            "api_prefix": API_PREFIX,
            "default_keywords": "\n".join(DEFAULT_KEYWORDS),
        },
    )


@app.post(f"{ADMIN_PREFIX}/config")
def save_config(
    grok_api_key: str = Form(default=""),
    grok_model: str = Form(default="grok-2"),
    devto_api_key: str = Form(default=""),
    devto_publish_as_draft: str = Form(default="true"),
    telegram_bot_token: str = Form(default=""),
    telegram_bot_username: str = Form(default=""),
    telegram_notify_chat_id: str = Form(default=""),
    pro_unlock_code: str = Form(default="PUREHUB-PRO-2026"),
    site_url: str = Form(default="https://hub.blissbiovn.com"),
) -> RedirectResponse:
    update_config(
        {
            "grok_api_key": grok_api_key.strip(),
            "grok_model": grok_model.strip() or "grok-2",
            "devto_api_key": devto_api_key.strip(),
            "devto_publish_as_draft": "true" if devto_publish_as_draft.strip().lower() == "true" else "false",
            "telegram_bot_token": telegram_bot_token.strip(),
            "telegram_bot_username": telegram_bot_username.strip().lstrip("@"),
            "telegram_notify_chat_id": telegram_notify_chat_id.strip(),
            "pro_unlock_code": pro_unlock_code.strip() or "PUREHUB-PRO-2026",
            "site_url": site_url.strip() or "https://hub.blissbiovn.com",
        }
    )
    return _redirect_with_message("Configuration saved successfully.", "success")


@app.post(f"{ADMIN_PREFIX}/actions/generate")
def trigger_generator(
    keywords: str = Form(default=""),
) -> RedirectResponse:
    try:
        keyword_list = [item.strip() for item in keywords.splitlines() if item.strip()] or DEFAULT_KEYWORDS
        generated = generate_articles(keyword_list)
        return _redirect_with_message(
            f"Generated {len(generated)} markdown article(s).",
            "success",
        )
    except Exception as exc:
        return _redirect_with_message(f"Generator failed: {exc}", "error")


@app.post(f"{ADMIN_PREFIX}/actions/publish")
def trigger_publisher() -> RedirectResponse:
    try:
        published = publish_articles()
        return _redirect_with_message(
            f"Published {len(published)} article(s) to Dev.to.",
            "success",
        )
    except Exception as exc:
        return _redirect_with_message(f"Publisher failed: {exc}", "error")


@app.post(f"{ADMIN_PREFIX}/actions/bot/start")
def start_bot() -> RedirectResponse:
    try:
        state = telegram_bot_manager.start()
        return _redirect_with_message(
            f"Telegram bot started ({state.thread_name or 'worker'}).",
            "success",
        )
    except Exception as exc:
        return _redirect_with_message(f"Telegram bot failed to start: {exc}", "error")


@app.post(f"{ADMIN_PREFIX}/actions/bot/stop")
def stop_bot() -> RedirectResponse:
    telegram_bot_manager.stop()
    return _redirect_with_message("Telegram bot stopped.", "info")


@app.get(f"{API_PREFIX}/health")
def healthcheck() -> dict[str, str]:
    return {"status": "ok", "service": "purehub-command-center", "admin_path": ADMIN_PREFIX}


@app.get(f"{API_PREFIX}/config")
def config_api() -> dict[str, object]:
    config = list_config()
    masked = {}
    for key, value in config.items():
        if "key" in key or "token" in key:
            masked[key] = mask_secret(value)
        else:
            masked[key] = value
    return {"config": masked}


@app.get(f"{API_PREFIX}/stats")
def stats_api() -> dict[str, object]:
    return {
        "users": get_user_stats(),
        "articles": get_dashboard_metrics(),
        "bot": {
            "running": telegram_bot_manager.state.running,
            "thread_name": telegram_bot_manager.state.thread_name,
            "last_error": telegram_bot_manager.state.last_error,
        },
    }


@app.get(f"{API_PREFIX}/articles")
def articles_api() -> dict[str, object]:
    return {"items": list_article_jobs(100)}


@app.get(f"{API_PREFIX}/referrers")
def referrers_api() -> dict[str, object]:
    return {"items": list_top_referrers(25)}


def mask_secret(value: str) -> str:
    if not value:
        return ""
    if len(value) <= 8:
        return "*" * len(value)
    return f"{value[:4]}...{value[-4:]}"


def _redirect_with_message(message: str, message_type: Literal["success", "info", "error"]) -> RedirectResponse:
    url = f"{ADMIN_PREFIX}?message={quote_plus(message)}&message_type={message_type}"
    return RedirectResponse(url=url, status_code=303)

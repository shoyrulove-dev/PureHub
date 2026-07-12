from __future__ import annotations

from pathlib import Path
from typing import Literal
from urllib.parse import quote_plus

from fastapi import APIRouter, FastAPI, Form, HTTPException, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from starlette.middleware.sessions import SessionMiddleware

try:
    from .content_generator import DEFAULT_KEYWORDS, generate_articles
    from .database import (
        CONFIG_DEFAULTS,
        get_admin_profile,
        get_dashboard_metrics,
        get_env_value,
        get_user_stats,
        init_database,
        list_api_catalog,
        list_article_jobs,
        list_config,
        list_miniapps,
        list_top_referrers,
        update_admin_credentials,
        update_api_catalog,
        update_config,
        update_miniapp,
        verify_admin_credentials,
    )
    from .devto_publisher import publish_articles
    from .telegram_bot_worker import telegram_bot_manager
except ImportError:
    from content_generator import DEFAULT_KEYWORDS, generate_articles
    from database import (
        CONFIG_DEFAULTS,
        get_admin_profile,
        get_dashboard_metrics,
        get_env_value,
        get_user_stats,
        init_database,
        list_api_catalog,
        list_article_jobs,
        list_config,
        list_miniapps,
        list_top_referrers,
        update_admin_credentials,
        update_api_catalog,
        update_config,
        update_miniapp,
        verify_admin_credentials,
    )
    from devto_publisher import publish_articles
    from telegram_bot_worker import telegram_bot_manager

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

PUBLIC_ADMIN_PREFIX = "/admin"
INTERNAL_ADMIN_PREFIX = "/api/admin"
PUBLIC_API_PREFIX = f"{PUBLIC_ADMIN_PREFIX}/api"

admin_router = APIRouter()
admin_api_router = APIRouter(prefix="/api")

app = FastAPI(
    title="PureHub Command Center",
    summary="Admin panel and automation control surface for PureHub growth systems.",
    version="0.4.0",
)

app.add_middleware(
    SessionMiddleware,
    secret_key=get_env_value("SESSION_SECRET", "change-me-in-command-center-env"),
    same_site="lax",
    https_only=False,
)


@app.on_event("startup")
def on_startup() -> None:
    init_database()


@app.get("/", include_in_schema=False)
def root_redirect() -> RedirectResponse:
    return RedirectResponse(url=PUBLIC_ADMIN_PREFIX, status_code=307)


@admin_router.get("/login", response_class=HTMLResponse)
def login_page(request: Request, message: str = "") -> HTMLResponse:
    if request.session.get("admin_username"):
        return RedirectResponse(url=PUBLIC_ADMIN_PREFIX, status_code=303)
    return templates.TemplateResponse(
        request=request,
        name="login.html",
        context={"message": message, "admin_prefix": PUBLIC_ADMIN_PREFIX},
    )


@admin_router.post("/login")
def login_action(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
) -> RedirectResponse:
    if not verify_admin_credentials(username.strip(), password):
        return RedirectResponse(
            url=f"{PUBLIC_ADMIN_PREFIX}/login?message={quote_plus('Invalid admin credentials.')}",
            status_code=303,
        )

    request.session["admin_username"] = username.strip()
    return RedirectResponse(url=PUBLIC_ADMIN_PREFIX, status_code=303)


@admin_router.post("/logout")
def logout_action(request: Request) -> RedirectResponse:
    request.session.clear()
    return RedirectResponse(url=f"{PUBLIC_ADMIN_PREFIX}/login", status_code=303)


@admin_router.get("", response_class=HTMLResponse)
def dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info", "error"] = "success",
) -> HTMLResponse:
    admin_username = request.session.get("admin_username")
    if not admin_username:
        return RedirectResponse(url=f"{PUBLIC_ADMIN_PREFIX}/login", status_code=303)

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
            "miniapps": list_miniapps(),
            "api_catalog": list_api_catalog(),
            "message": message,
            "message_type": message_type,
            "admin_prefix": PUBLIC_ADMIN_PREFIX,
            "api_prefix": PUBLIC_API_PREFIX,
            "default_keywords": "\n".join(DEFAULT_KEYWORDS),
            "admin_username": admin_username,
            "admin_profile": get_admin_profile(str(admin_username)),
            "mongo_db_name": get_env_value("MONGO_DB_NAME", "purehub_command_center"),
        },
    )


@admin_router.post("/config")
def save_config(
    request: Request,
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
    require_admin_session(request)
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


@admin_router.post("/security")
def save_admin_security(
    request: Request,
    current_password: str = Form(...),
    next_username: str = Form(...),
    next_password: str = Form(default=""),
) -> RedirectResponse:
    current_username = require_admin_session(request)
    updated, result = update_admin_credentials(
        current_username,
        next_username=next_username,
        current_password=current_password,
        next_password=next_password.strip() or None,
    )
    if not updated:
        return _redirect_with_message(result, "error")

    request.session["admin_username"] = result
    return _redirect_with_message("Admin security updated successfully.", "success")


@admin_router.post("/miniapps/{miniapp_id}")
def save_miniapp(
    request: Request,
    miniapp_id: str,
    name: str = Form(...),
    tab: str = Form(...),
    route_en: str = Form(...),
    route_vi: str = Form(...),
    route_zh: str = Form(...),
    traffic_priority: int = Form(...),
    notes: str = Form(default=""),
    enabled: str | None = Form(default=None),
) -> RedirectResponse:
    require_admin_session(request)
    update_miniapp(
        miniapp_id,
        {
            "name": name.strip(),
            "tab": tab.strip(),
            "route_en": route_en.strip(),
            "route_vi": route_vi.strip(),
            "route_zh": route_zh.strip(),
            "traffic_priority": int(traffic_priority),
            "notes": notes.strip(),
            "enabled": enabled == "true",
        },
    )
    return _redirect_with_message(f"Saved mini-app {miniapp_id}.", "success")


@admin_router.post("/apis/{api_key}")
def save_api_catalog(
    request: Request,
    api_key: str,
    method: str = Form(...),
    path: str = Form(...),
    group: str = Form(...),
    description: str = Form(default=""),
    enabled: str | None = Form(default=None),
    auth_required: str | None = Form(default=None),
) -> RedirectResponse:
    require_admin_session(request)
    update_api_catalog(
        api_key,
        {
            "method": method.strip().upper(),
            "path": path.strip(),
            "group": group.strip(),
            "description": description.strip(),
            "enabled": enabled == "true",
            "auth_required": auth_required == "true",
        },
    )
    return _redirect_with_message(f"Saved API config {api_key}.", "success")


@admin_router.post("/actions/generate")
def trigger_generator(
    request: Request,
    keywords: str = Form(default=""),
) -> RedirectResponse:
    require_admin_session(request)
    try:
        keyword_list = [item.strip() for item in keywords.splitlines() if item.strip()] or DEFAULT_KEYWORDS
        generated = generate_articles(keyword_list)
        return _redirect_with_message(f"Generated {len(generated)} markdown article(s).", "success")
    except Exception as exc:
        return _redirect_with_message(f"Generator failed: {exc}", "error")


@admin_router.post("/actions/publish")
def trigger_publisher(request: Request) -> RedirectResponse:
    require_admin_session(request)
    try:
        published = publish_articles()
        return _redirect_with_message(f"Published {len(published)} article(s) to Dev.to.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Publisher failed: {exc}", "error")


@admin_router.post("/actions/bot/start")
def start_bot(request: Request) -> RedirectResponse:
    require_admin_session(request)
    try:
        state = telegram_bot_manager.start()
        return _redirect_with_message(f"Telegram bot started ({state.thread_name or 'worker'}).", "success")
    except Exception as exc:
        return _redirect_with_message(f"Telegram bot failed to start: {exc}", "error")


@admin_router.post("/actions/bot/stop")
def stop_bot(request: Request) -> RedirectResponse:
    require_admin_session(request)
    telegram_bot_manager.stop()
    return _redirect_with_message("Telegram bot stopped.", "info")


@admin_api_router.get("/health")
def healthcheck(request: Request) -> dict[str, str]:
    require_admin_session(request)
    return {"status": "ok", "service": "purehub-command-center", "admin_path": PUBLIC_ADMIN_PREFIX}


@admin_api_router.get("/config")
def config_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    config = list_config()
    masked = {}
    for key, value in config.items():
        if "key" in key or "token" in key:
            masked[key] = mask_secret(value)
        else:
            masked[key] = value
    return {"config": masked}


@admin_api_router.get("/stats")
def stats_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {
        "users": get_user_stats(),
        "articles": get_dashboard_metrics(),
        "bot": {
            "running": telegram_bot_manager.state.running,
            "thread_name": telegram_bot_manager.state.thread_name,
            "last_error": telegram_bot_manager.state.last_error,
        },
        "mongo_db_name": get_env_value("MONGO_DB_NAME", "purehub_command_center"),
    }


@admin_api_router.get("/articles")
def articles_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_article_jobs(100)}


@admin_api_router.get("/referrers")
def referrers_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_top_referrers(25)}


@admin_api_router.get("/miniapps")
def miniapps_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_miniapps()}


@admin_api_router.get("/catalog")
def api_catalog_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_api_catalog()}


def require_admin_session(request: Request) -> str:
    admin_username = request.session.get("admin_username")
    if not admin_username:
        raise HTTPException(status_code=401, detail="Admin authentication required.")
    return str(admin_username)


def mask_secret(value: str) -> str:
    if not value:
        return ""
    if len(value) <= 8:
        return "*" * len(value)
    return f"{value[:4]}...{value[-4:]}"


def _redirect_with_message(message: str, message_type: Literal["success", "info", "error"]) -> RedirectResponse:
    url = f"{PUBLIC_ADMIN_PREFIX}?message={quote_plus(message)}&message_type={message_type}"
    return RedirectResponse(url=url, status_code=303)


app.include_router(admin_router, prefix=PUBLIC_ADMIN_PREFIX)
app.include_router(admin_api_router, prefix=PUBLIC_ADMIN_PREFIX)
app.include_router(admin_router, prefix=INTERNAL_ADMIN_PREFIX)
app.include_router(admin_api_router, prefix=INTERNAL_ADMIN_PREFIX)

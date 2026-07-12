from __future__ import annotations

import json
from pathlib import Path
from typing import Literal
from urllib.parse import quote_plus

from fastapi import APIRouter, FastAPI, Form, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from starlette.middleware.sessions import SessionMiddleware

try:
    from .content_generator import DEFAULT_KEYWORDS, generate_articles
    from .database import (
        ADMIN_ROLES,
        CONFIG_DEFAULTS,
        create_admin_account,
        create_api_catalog_entry,
        create_miniapp,
        delete_admin_account,
        delete_api_catalog_entry,
        delete_miniapp,
        export_control_bundle,
        get_admin_profile,
        get_analytics_snapshot,
        get_dashboard_metrics,
        get_env_value,
        get_schema_status,
        get_user_stats,
        import_control_bundle,
        init_database,
        list_admin_accounts,
        list_audit_logs,
        list_api_catalog,
        list_article_jobs,
        list_config,
        list_miniapps,
        list_top_referrers,
        record_audit_log,
        update_admin_account,
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
        ADMIN_ROLES,
        CONFIG_DEFAULTS,
        create_admin_account,
        create_api_catalog_entry,
        create_miniapp,
        delete_admin_account,
        delete_api_catalog_entry,
        delete_miniapp,
        export_control_bundle,
        get_admin_profile,
        get_analytics_snapshot,
        get_dashboard_metrics,
        get_env_value,
        get_schema_status,
        get_user_stats,
        import_control_bundle,
        init_database,
        list_admin_accounts,
        list_audit_logs,
        list_api_catalog,
        list_article_jobs,
        list_config,
        list_miniapps,
        list_top_referrers,
        record_audit_log,
        update_admin_account,
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
    version="0.5.0",
)

app.add_middleware(
    SessionMiddleware,
    secret_key=get_env_value("SESSION_SECRET", "change-me-in-command-center-env"),
    same_site="lax",
    https_only=get_env_value("VERCEL_ENV") in {"production", "preview"},
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
    record_audit_log(
        actor=username.strip(),
        action="login",
        target_type="admin_session",
        target_id=username.strip(),
        details={"status": "success"},
        request_meta=request_meta(request),
    )
    return RedirectResponse(url=PUBLIC_ADMIN_PREFIX, status_code=303)


@admin_router.post("/logout")
def logout_action(request: Request) -> RedirectResponse:
    actor = request.session.get("admin_username", "unknown")
    record_audit_log(
        actor=str(actor),
        action="logout",
        target_type="admin_session",
        target_id=str(actor),
        request_meta=request_meta(request),
    )
    request.session.clear()
    return RedirectResponse(url=f"{PUBLIC_ADMIN_PREFIX}/login", status_code=303)


@admin_router.get("", response_class=HTMLResponse)
def dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info", "error"] = "success",
    miniapp_query: str = "",
    miniapp_tab: str = "",
    api_query: str = "",
    api_group: str = "",
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
            "miniapps": list_miniapps(miniapp_query, miniapp_tab),
            "api_catalog": list_api_catalog(api_query, api_group),
            "audit_logs": list_audit_logs(),
            "schema_status": get_schema_status(),
            "analytics": get_analytics_snapshot(),
            "admins": list_admin_accounts(),
            "admin_roles": ADMIN_ROLES,
            "export_bundle_json": json.dumps(export_control_bundle(), indent=2, ensure_ascii=False),
            "message": message,
            "message_type": message_type,
            "admin_prefix": PUBLIC_ADMIN_PREFIX,
            "api_prefix": PUBLIC_API_PREFIX,
            "default_keywords": "\n".join(DEFAULT_KEYWORDS),
            "admin_username": admin_username,
            "admin_profile": get_admin_profile(str(admin_username)),
            "mongo_db_name": get_env_value("MONGO_DB_NAME", "purehub_command_center"),
            "miniapp_query": miniapp_query,
            "miniapp_tab": miniapp_tab,
            "api_query": api_query,
            "api_group": api_group,
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
    actor = require_admin_role(request, "superadmin", "editor")["username"]
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
    record_audit_log(
        actor=actor,
        action="update_config",
        target_type="config",
        target_id="global",
        details={"keys": sorted(CONFIG_DEFAULTS.keys())},
        request_meta=request_meta(request),
    )
    return _redirect_with_message("Configuration saved successfully.", "success")


@admin_router.post("/security")
def save_admin_security(
    request: Request,
    current_password: str = Form(...),
    next_username: str = Form(...),
    next_password: str = Form(default=""),
) -> RedirectResponse:
    current_admin = require_admin_role(request, "superadmin")
    current_username = current_admin["username"]
    updated, result = update_admin_credentials(
        current_username,
        next_username=next_username,
        current_password=current_password,
        next_password=next_password.strip() or None,
    )
    if not updated:
        return _redirect_with_message(result, "error")

    request.session["admin_username"] = result
    record_audit_log(
        actor=result,
        action="update_admin_security",
        target_type="admin",
        target_id=result,
        details={"previous_username": current_username, "password_rotated": bool(next_password.strip())},
        request_meta=request_meta(request),
    )
    return _redirect_with_message("Admin security updated successfully.", "success")


@admin_router.post("/admins")
def create_admin_user(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    role: str = Form(...),
    active: str | None = Form(default=None),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    try:
        create_admin_account(username.strip(), password, role.strip(), active=active == "true")
    except Exception as exc:
        return _redirect_with_message(f"Create admin failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="create_admin_account",
        target_type="admin",
        target_id=username.strip(),
        details={"role": role.strip(), "active": active == "true"},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Created admin account {username.strip()}.", "success")


@admin_router.post("/admins/{username}")
def update_admin_user(
    request: Request,
    username: str,
    role: str = Form(...),
    active: str | None = Form(default=None),
    next_password: str = Form(default=""),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    try:
        update_admin_account(
            username,
            role=role.strip(),
            active=active == "true",
            next_password=next_password.strip() or None,
        )
    except Exception as exc:
        return _redirect_with_message(f"Update admin failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="update_admin_account",
        target_type="admin",
        target_id=username,
        details={"role": role.strip(), "active": active == "true", "password_rotated": bool(next_password.strip())},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Updated admin account {username}.", "success")


@admin_router.post("/admins/{username}/delete")
def delete_admin_user(request: Request, username: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    try:
        delete_admin_account(username)
    except Exception as exc:
        return _redirect_with_message(f"Delete admin failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="delete_admin_account",
        target_type="admin",
        target_id=username,
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Deleted admin account {username}.", "info")


@admin_router.get("/export/json")
def export_json_bundle(request: Request) -> JSONResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    bundle = export_control_bundle()
    record_audit_log(
        actor=actor,
        action="export_control_bundle",
        target_type="bundle",
        target_id="control_bundle",
        details={"miniapps": len(bundle.get("miniapps", [])), "api_catalog": len(bundle.get("api_catalog", []))},
        request_meta=request_meta(request),
    )
    return JSONResponse(bundle)


@admin_router.post("/import/json")
def import_json_bundle(
    request: Request,
    bundle_json: str = Form(...),
    mode: str = Form(default="merge"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    try:
        bundle = json.loads(bundle_json)
        result = import_control_bundle(bundle, mode=mode.strip().lower())
    except Exception as exc:
        return _redirect_with_message(f"Import bundle failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="import_control_bundle",
        target_type="bundle",
        target_id="control_bundle",
        details={"mode": mode.strip().lower(), **result},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(
        f"Imported bundle: {result['miniapps']} mini-apps, {result['api_catalog']} APIs.",
        "success",
    )


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
    actor = require_admin_role(request, "superadmin", "editor")["username"]
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
    record_audit_log(
        actor=actor,
        action="update_miniapp",
        target_type="miniapp",
        target_id=miniapp_id,
        details={"name": name.strip(), "enabled": enabled == "true", "traffic_priority": int(traffic_priority)},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Saved mini-app {miniapp_id}.", "success")


@admin_router.post("/miniapps")
def create_miniapp_entry(
    request: Request,
    miniapp_id: str = Form(...),
    name: str = Form(...),
    tab: str = Form(...),
    route_en: str = Form(...),
    route_vi: str = Form(...),
    route_zh: str = Form(...),
    traffic_priority: int = Form(5),
    notes: str = Form(default=""),
    enabled: str | None = Form(default=None),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        create_miniapp(
            {
                "miniapp_id": miniapp_id.strip(),
                "name": name.strip(),
                "tab": tab.strip(),
                "route_en": route_en.strip(),
                "route_vi": route_vi.strip(),
                "route_zh": route_zh.strip(),
                "traffic_priority": int(traffic_priority),
                "notes": notes.strip(),
                "enabled": enabled == "true",
            }
        )
    except Exception as exc:
        return _redirect_with_message(f"Create mini-app failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="create_miniapp",
        target_type="miniapp",
        target_id=miniapp_id.strip(),
        details={"name": name.strip(), "tab": tab.strip()},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Created mini-app {miniapp_id.strip()}.", "success")


@admin_router.post("/miniapps/{miniapp_id}/delete")
def delete_miniapp_entry(request: Request, miniapp_id: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    delete_miniapp(miniapp_id)
    record_audit_log(
        actor=actor,
        action="delete_miniapp",
        target_type="miniapp",
        target_id=miniapp_id,
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Deleted mini-app {miniapp_id}.", "info")


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
    actor = require_admin_role(request, "superadmin", "editor")["username"]
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
    record_audit_log(
        actor=actor,
        action="update_api_catalog",
        target_type="api_catalog",
        target_id=api_key,
        details={"path": path.strip(), "enabled": enabled == "true", "auth_required": auth_required == "true"},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Saved API config {api_key}.", "success")


@admin_router.post("/apis")
def create_api_catalog(
    request: Request,
    api_key: str = Form(...),
    method: str = Form(...),
    path: str = Form(...),
    group: str = Form(...),
    description: str = Form(default=""),
    enabled: str | None = Form(default=None),
    auth_required: str | None = Form(default=None),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        create_api_catalog_entry(
            {
                "api_key": api_key.strip(),
                "method": method.strip().upper(),
                "path": path.strip(),
                "group": group.strip(),
                "description": description.strip(),
                "enabled": enabled == "true",
                "auth_required": auth_required == "true",
            }
        )
    except Exception as exc:
        return _redirect_with_message(f"Create API config failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="create_api_catalog",
        target_type="api_catalog",
        target_id=api_key.strip(),
        details={"path": path.strip(), "group": group.strip()},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Created API config {api_key.strip()}.", "success")


@admin_router.post("/apis/{api_key}/delete")
def delete_api_catalog(request: Request, api_key: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    delete_api_catalog_entry(api_key)
    record_audit_log(
        actor=actor,
        action="delete_api_catalog",
        target_type="api_catalog",
        target_id=api_key,
        request_meta=request_meta(request),
    )
    return _redirect_with_message(f"Deleted API config {api_key}.", "info")


@admin_router.post("/actions/generate")
def trigger_generator(
    request: Request,
    keywords: str = Form(default=""),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        keyword_list = [item.strip() for item in keywords.splitlines() if item.strip()] or DEFAULT_KEYWORDS
        generated = generate_articles(keyword_list)
        record_audit_log(
            actor=actor,
            action="generate_articles",
            target_type="article_jobs",
            target_id="batch",
            details={"keywords_count": len(keyword_list), "generated_count": len(generated)},
            request_meta=request_meta(request),
        )
        return _redirect_with_message(f"Generated {len(generated)} markdown article(s).", "success")
    except Exception as exc:
        return _redirect_with_message(f"Generator failed: {exc}", "error")


@admin_router.post("/actions/publish")
def trigger_publisher(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        published = publish_articles()
        record_audit_log(
            actor=actor,
            action="publish_articles",
            target_type="article_jobs",
            target_id="batch",
            details={"published_count": len(published)},
            request_meta=request_meta(request),
        )
        return _redirect_with_message(f"Published {len(published)} article(s) to Dev.to.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Publisher failed: {exc}", "error")


@admin_router.post("/actions/bot/start")
def start_bot(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        state = telegram_bot_manager.start()
        record_audit_log(
            actor=actor,
            action="start_telegram_bot",
            target_type="telegram_bot",
            target_id=state.thread_name or "worker",
            details={"running": state.running},
            request_meta=request_meta(request),
        )
        return _redirect_with_message(f"Telegram bot started ({state.thread_name or 'worker'}).", "success")
    except Exception as exc:
        return _redirect_with_message(f"Telegram bot failed to start: {exc}", "error")


@admin_router.post("/actions/bot/stop")
def stop_bot(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    telegram_bot_manager.stop()
    record_audit_log(
        actor=actor,
        action="stop_telegram_bot",
        target_type="telegram_bot",
        target_id="worker",
        request_meta=request_meta(request),
    )
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


@admin_api_router.get("/admins")
def admins_api(request: Request) -> dict[str, object]:
    require_admin_role(request, "superadmin")
    return {"items": list_admin_accounts()}


@admin_api_router.get("/audit-logs")
def audit_logs_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_audit_logs(100)}


@admin_api_router.get("/schema")
def schema_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return get_schema_status()


@admin_api_router.get("/analytics")
def analytics_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return get_analytics_snapshot()


@admin_api_router.get("/export")
def export_api(request: Request) -> dict[str, object]:
    require_admin_role(request, "superadmin", "editor")
    return export_control_bundle()


def require_admin_session(request: Request) -> str:
    admin_username = request.session.get("admin_username")
    if not admin_username:
        raise HTTPException(status_code=401, detail="Admin authentication required.")
    return str(admin_username)


def require_admin_role(request: Request, *allowed_roles: str) -> dict[str, str]:
    username = require_admin_session(request)
    profile = get_admin_profile(username)
    if not profile:
        raise HTTPException(status_code=401, detail="Admin profile not found.")
    if allowed_roles and str(profile.get("role", "")) not in allowed_roles:
        raise HTTPException(status_code=403, detail="Admin role is not allowed for this action.")
    return {
        "username": str(profile["username"]),
        "role": str(profile.get("role", "viewer")),
    }


def request_meta(request: Request) -> dict[str, str]:
    return {
        "path": str(request.url.path),
        "client_ip": request.client.host if request.client else "",
        "user_agent": request.headers.get("user-agent", ""),
    }


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

from __future__ import annotations

import json
import secrets
from datetime import datetime, timezone
from html import escape
from pathlib import Path
from typing import Literal
from urllib.parse import quote_plus

from fastapi import APIRouter, FastAPI, Form, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse, Response
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
        clear_login_guards,
        delete_admin_account,
        delete_api_catalog_entry,
        delete_miniapp,
        export_control_bundle,
        get_admin_profile,
        get_analytics_snapshot,
        get_dashboard_metrics,
        get_env_value,
        get_login_guard_state,
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
        register_failed_login,
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
        clear_login_guards,
        delete_admin_account,
        delete_api_catalog_entry,
        delete_miniapp,
        export_control_bundle,
        get_admin_profile,
        get_analytics_snapshot,
        get_dashboard_metrics,
        get_env_value,
        get_login_guard_state,
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
        register_failed_login,
        update_admin_account,
        update_admin_credentials,
        update_api_catalog,
        update_config,
        update_miniapp,
        verify_admin_credentials,
    )
    from devto_publisher import publish_articles
    from telegram_bot_worker import telegram_bot_manager

try:
    from .database import (
        create_release,
        get_release,
        list_release_publications,
        list_releases,
        update_release,
        update_release_publication,
        upsert_release_publication,
    )
    from .release_hub import generate_release_bundle, generate_reply_draft, publish_release
except ImportError:
    from database import (
        create_release,
        get_release,
        list_release_publications,
        list_releases,
        update_release,
        update_release_publication,
        upsert_release_publication,
    )
    from release_hub import generate_release_bundle, generate_reply_draft, publish_release

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

PUBLIC_ADMIN_PREFIX = "/admin"
INTERNAL_ADMIN_PREFIX = "/api/admin"
PUBLIC_API_PREFIX = f"{PUBLIC_ADMIN_PREFIX}/api"

admin_router = APIRouter()
admin_api_router = APIRouter(prefix="/api")
public_api_router = APIRouter()

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


def get_client_ip(request: Request) -> str:
    forwarded_for = request.headers.get("x-forwarded-for", "")
    if forwarded_for.strip():
        return forwarded_for.split(",")[0].strip()
    return request.client.host if request.client else "unknown"


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
    normalized_username = username.strip()
    client_ip = get_client_ip(request)
    guard_state = get_login_guard_state(username=normalized_username, ip_address=client_ip)
    if not guard_state["allowed"]:
        record_audit_log(
            actor=normalized_username or "unknown",
            action="login_blocked",
            target_type="admin_session",
            target_id=normalized_username or "unknown",
            details={
                "scope": guard_state["scope"],
                "remaining_seconds": guard_state["remaining_seconds"],
            },
            request_meta=request_meta(request),
        )
        return RedirectResponse(
            url=f"{PUBLIC_ADMIN_PREFIX}/login?message={quote_plus(str(guard_state['message']))}",
            status_code=303,
        )

    if not verify_admin_credentials(normalized_username, password):
        failed_state = register_failed_login(username=normalized_username, ip_address=client_ip)
        record_audit_log(
            actor=normalized_username or "unknown",
            action="login_failed",
            target_type="admin_session",
            target_id=normalized_username or "unknown",
            details={
                "locked": failed_state["locked"],
                "remaining_seconds": failed_state["remaining_seconds"],
            },
            request_meta=request_meta(request),
        )
        return RedirectResponse(
            url=f"{PUBLIC_ADMIN_PREFIX}/login?message={quote_plus(str(failed_state['message']))}",
            status_code=303,
        )

    clear_login_guards(username=normalized_username, ip_address=client_ip)
    request.session["admin_username"] = normalized_username
    record_audit_log(
        actor=normalized_username,
        action="login",
        target_type="admin_session",
        target_id=normalized_username,
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
            "analytics_json": json.dumps(get_analytics_snapshot(), ensure_ascii=False),
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
            "releases": list_releases(),
            "release_publications": list_release_publications(),
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
    site_url: str = Form(default="https://hub.blissbiovn.com"),
    ai_provider: str = Form(default="deepseek"),
    groq_api_key: str = Form(default=""),
    groq_model: str = Form(default="llama-3.3-70b-versatile"),
    deepseek_api_key: str = Form(default=""),
    deepseek_model: str = Form(default="deepseek-chat"),
    github_repo: str = Form(default="shoyrulove-dev/PureHub"),
    bluesky_handle: str = Form(default=""),
    bluesky_app_password: str = Form(default=""),
    mastodon_base_url: str = Form(default=""),
    mastodon_access_token: str = Form(default=""),
    release_auto_channels: str = Form(default="telegram,devto,bluesky,mastodon"),
    community_reply_mode: str = Form(default="draft"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    current_config = list_config()
    update_config(
        {
            "grok_api_key": grok_api_key.strip() or current_config.get("grok_api_key", ""),
            "grok_model": grok_model.strip() or "grok-2",
            "devto_api_key": devto_api_key.strip() or current_config.get("devto_api_key", ""),
            "devto_publish_as_draft": "true" if devto_publish_as_draft.strip().lower() == "true" else "false",
            "telegram_bot_token": telegram_bot_token.strip() or current_config.get("telegram_bot_token", ""),
            "telegram_bot_username": telegram_bot_username.strip().lstrip("@"),
            "telegram_notify_chat_id": telegram_notify_chat_id.strip(),
            "site_url": site_url.strip() or "https://hub.blissbiovn.com",
            "ai_provider": ai_provider.strip().lower() or "deepseek",
            "groq_api_key": groq_api_key.strip() or current_config.get("groq_api_key", ""),
            "groq_model": groq_model.strip() or "llama-3.3-70b-versatile",
            "deepseek_api_key": deepseek_api_key.strip() or current_config.get("deepseek_api_key", ""),
            "deepseek_model": deepseek_model.strip() or "deepseek-chat",
            "github_repo": github_repo.strip() or "shoyrulove-dev/PureHub",
            "bluesky_handle": bluesky_handle.strip(),
            "bluesky_app_password": bluesky_app_password.strip() or current_config.get("bluesky_app_password", ""),
            "mastodon_base_url": mastodon_base_url.strip(),
            "mastodon_access_token": mastodon_access_token.strip() or current_config.get("mastodon_access_token", ""),
            "release_auto_channels": release_auto_channels.strip(),
            "community_reply_mode": "auto" if community_reply_mode.strip().lower() == "auto" else "draft",
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


@admin_router.post("/actions/bot/announce")
def announce_bot_release(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        result = telegram_bot_manager.publish_release_update()
        record_audit_log(
            actor=actor,
            action="publish_telegram_release",
            target_type="telegram_bot",
            target_id=result["message_id"],
            details={"chat_id": result["chat_id"]},
            request_meta=request_meta(request),
        )
        return _redirect_with_message(
            f"Telegram profile synced and release message #{result['message_id']} published.",
            "success",
        )
    except Exception as exc:
        return _redirect_with_message(f"Telegram release update failed: {exc}", "error")


@admin_router.post("/releases/create")
def create_release_action(
    request: Request,
    version: str = Form(...),
    title: str = Form(default=""),
    summary: str = Form(default=""),
    changelog: str = Form(default=""),
    github_url: str = Form(default=""),
    apk_url: str = Form(default=""),
    aab_url: str = Form(default=""),
    sha256: str = Form(default=""),
    prerelease: str = Form(default="true"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        release = create_release(
            version=version,
            title=title,
            summary=summary,
            changelog=changelog,
            github_url=github_url,
            apk_url=apk_url,
            aab_url=aab_url,
            sha256=sha256,
            prerelease=prerelease.lower() == "true",
        )
        record_audit_log(actor=actor, action="create_release", target_type="release", target_id=release["release_id"])
        return _redirect_with_message(f"Created release {release['release_id']}.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Create release failed: {exc}", "error")


@admin_router.post("/releases/{release_id}/generate")
def generate_release_action(request: Request, release_id: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        rows = generate_release_bundle(release_id)
        record_audit_log(
            actor=actor,
            action="generate_release_content",
            target_type="release",
            target_id=release_id,
            details={"draft_count": len(rows)},
        )
        return _redirect_with_message(f"Generated {len(rows)} release drafts.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Release content generation failed: {exc}", "error")


@admin_router.post("/releases/{release_id}/update")
def update_release_action(
    request: Request,
    release_id: str,
    title: str = Form(default=""),
    summary: str = Form(default=""),
    changelog: str = Form(default=""),
    github_url: str = Form(default=""),
    apk_url: str = Form(default=""),
    aab_url: str = Form(default=""),
    sha256: str = Form(default=""),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    if not get_release(release_id):
        return _redirect_with_message("Release not found.", "error")
    update_release(
        release_id,
        {
            "title": title.strip(),
            "summary": summary.strip(),
            "changelog": changelog.strip(),
            "github_url": github_url.strip(),
            "apk_url": apk_url.strip(),
            "aab_url": aab_url.strip(),
            "sha256": sha256.strip(),
        },
    )
    record_audit_log(actor=actor, action="update_release", target_type="release", target_id=release_id)
    return _redirect_with_message(f"Updated {release_id}.", "success")


@admin_router.post("/releases/{release_id}/publish")
def publish_release_action(request: Request, release_id: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        rows = publish_release(release_id)
        record_audit_log(
            actor=actor,
            action="publish_release_channels",
            target_type="release",
            target_id=release_id,
            details={"channel_count": len(rows)},
        )
        return _redirect_with_message(f"Release publish workflow processed {len(rows)} channel(s).", "success")
    except Exception as exc:
        return _redirect_with_message(f"Release publishing failed: {exc}", "error")


@admin_router.post("/releases/{release_id}/publications/{channel}/{language}")
def update_release_publication_action(
    request: Request,
    release_id: str,
    channel: str,
    language: str,
    content: str = Form(...),
    status: str = Form(default="draft"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    allowed_statuses = {"draft", "approved", "ready_manual"}
    if status not in allowed_statuses:
        return _redirect_with_message("Invalid publication status.", "error")
    update_release_publication(
        release_id,
        channel,
        language,
        {"content": content.strip(), "status": status, "error_message": ""},
    )
    record_audit_log(
        actor=actor,
        action="review_release_publication",
        target_type="release_publication",
        target_id=f"{release_id}:{channel}:{language}",
        details={"status": status},
    )
    return _redirect_with_message(f"Saved {channel}/{language} as {status}.", "success")


@admin_router.post("/community/reply-draft")
def community_reply_draft_action(
    request: Request,
    message: str = Form(...),
    context: str = Form(default=""),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        draft = generate_reply_draft(message, context)
        reply_id = datetime.now(timezone.utc).strftime("community-reply-%Y%m%d-%H%M%S-%f")
        upsert_release_publication(
            release_id=reply_id,
            channel="reply",
            language="en",
            content=draft,
            status="ready_manual",
        )
        record_audit_log(actor=actor, action="generate_reply_draft", target_type="community", target_id="reply")
        return _redirect_with_message("AI community reply draft generated for review.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Reply draft generation failed: {exc}", "error")


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
        if "key" in key or "token" in key or "password" in key or "secret" in key:
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


@admin_api_router.get("/releases")
def releases_api(request: Request) -> dict[str, object]:
    require_admin_session(request)
    return {"items": list_releases(), "publications": list_release_publications()}


@public_api_router.get("/releases")
def public_releases_api() -> dict[str, object]:
    return {"items": list_releases(30, published_only=True)}


@public_api_router.get("/releases.xml")
def public_releases_rss() -> Response:
    site_url = get_env_value("PUBLIC_SITE_URL", "https://hub.blissbiovn.com").rstrip("/")
    items = []
    for release in list_releases(30, published_only=True):
        link = release.get("github_url") or f"{site_url}/en/download"
        items.append(
            "<item>"
            f"<title>{escape(str(release.get('title', '')))}</title>"
            f"<link>{escape(str(link))}</link>"
            f"<guid>{escape(str(release.get('release_id', '')))}</guid>"
            f"<description>{escape(str(release.get('summary', '')))}</description>"
            "</item>"
        )
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        "<rss version=\"2.0\"><channel><title>PureHub Releases</title>"
        f"<link>{escape(site_url)}/en/download</link><description>Signed PureHub Android releases</description>"
        + "".join(items)
        + "</channel></rss>"
    )
    return Response(content=xml, media_type="application/rss+xml")


@public_api_router.post("/release-hook")
async def github_release_hook(request: Request) -> dict[str, object]:
    expected_secret = get_env_value("RELEASE_WEBHOOK_SECRET")
    supplied_secret = request.headers.get("x-purehub-release-secret", "")
    if not expected_secret or not secrets.compare_digest(supplied_secret, expected_secret):
        raise HTTPException(status_code=401, detail="Invalid release hook secret.")

    payload = await request.json()
    version = str(payload.get("version", "")).strip().lstrip("v")
    if not version or len(version) > 64:
        raise HTTPException(status_code=422, detail="A valid release version is required.")
    release_id = f"v{version}"
    values = {
        "title": str(payload.get("title", "")).strip() or f"PureHub {version}",
        "summary": str(payload.get("summary", "")).strip(),
        "changelog": str(payload.get("changelog", "")).strip(),
        "github_url": str(payload.get("github_url", "")).strip(),
        "apk_url": str(payload.get("apk_url", "")).strip(),
        "aab_url": str(payload.get("aab_url", "")).strip(),
        "sha256": str(payload.get("sha256", "")).strip(),
        "prerelease": bool(payload.get("prerelease", True)),
    }
    if get_release(release_id):
        update_release(release_id, values)
    else:
        create_release(version=version, **values)
    drafts = generate_release_bundle(release_id)
    record_audit_log(
        actor="github-actions",
        action="ingest_github_release",
        target_type="release",
        target_id=release_id,
        details={"draft_count": len(drafts)},
        request_meta=request_meta(request),
    )
    return {"ok": True, "release_id": release_id, "draft_count": len(drafts)}


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
app.include_router(public_api_router, prefix="/public-api")

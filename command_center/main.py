from __future__ import annotations

import json
import secrets
from contextlib import asynccontextmanager
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from html import escape
from pathlib import Path
from typing import Any, Literal
from urllib.parse import quote_plus

from fastapi import APIRouter, FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates
from starlette.middleware.sessions import SessionMiddleware

try:
    from .community_goal import build_august_growth_goal
except ImportError:
    from community_goal import build_august_growth_goal

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
        get_config_value,
        get_dashboard_metrics,
        get_env_value,
        get_login_guard_state,
        get_product_growth_snapshot,
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
        list_roadmap_options,
        list_top_referrers,
        record_audit_log,
        record_growth_funnel_event,
        record_miniapp_event,
        record_roadmap_vote,
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
        get_config_value,
        get_dashboard_metrics,
        get_env_value,
        get_login_guard_state,
        get_product_growth_snapshot,
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
        list_roadmap_options,
        list_top_referrers,
        record_audit_log,
        record_growth_funnel_event,
        record_miniapp_event,
        record_roadmap_vote,
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
        claim_release_publication,
        create_release,
        get_release,
        list_distribution_submissions,
        list_release_publications,
        list_releases,
        DISTRIBUTION_STATUSES,
        update_release,
        update_distribution_submission,
        update_release_publication,
        upsert_release_publication,
    )
    from .release_hub import (
        format_reddit_draft,
        generate_reddit_draft,
        generate_release_bundle,
        generate_reply_draft,
        parse_reddit_draft,
        publish_release,
    )
except ImportError:
    from database import (
        claim_release_publication,
        create_release,
        get_release,
        list_distribution_submissions,
        list_release_publications,
        list_releases,
        DISTRIBUTION_STATUSES,
        update_release,
        update_distribution_submission,
        update_release_publication,
        upsert_release_publication,
    )
    from release_hub import (
        format_reddit_draft,
        generate_reddit_draft,
        generate_release_bundle,
        generate_reply_draft,
        parse_reddit_draft,
        publish_release,
    )

try:
    from .community_support import (
        generate_support_draft,
        generate_support_drafts,
        ingest_telegram_update,
        send_support_reply,
        sync_support_channels,
    )
    from .database import (
        get_support_message,
        get_support_metrics,
        count_support_messages,
        delete_support_message,
        list_community_metrics,
        list_support_messages,
        list_support_sync_states,
        update_support_message,
        upsert_support_message,
    )
except ImportError:
    from community_support import (
        generate_support_draft,
        generate_support_drafts,
        ingest_telegram_update,
        send_support_reply,
        sync_support_channels,
    )
    from database import (
        get_support_message,
        get_support_metrics,
        count_support_messages,
        delete_support_message,
        list_community_metrics,
        list_support_messages,
        list_support_sync_states,
        update_support_message,
        upsert_support_message,
    )

try:
    from .database import claim_growth_post, get_growth_post, get_growth_summary, list_growth_posts, update_growth_post
    from .growth_automation import retry_growth_post, run_growth_automation, sync_growth_post_metrics
    from .youtube_connector import (
        build_authorization_url,
        complete_upload,
        connect_youtube,
        create_upload_session,
        disconnect_youtube,
        sync_youtube_metrics,
        youtube_connection_state,
    )
    from .tiktok_connector import (
        build_authorization_url as build_tiktok_authorization_url,
        complete_upload as complete_tiktok_upload,
        connect_tiktok,
        create_upload_session as create_tiktok_upload_session,
        disconnect_tiktok,
        fetch_creator_info as fetch_tiktok_creator_info,
        fetch_publish_status as fetch_tiktok_publish_status,
        tiktok_connection_state,
    )
except ImportError:
    from database import claim_growth_post, get_growth_post, get_growth_summary, list_growth_posts, update_growth_post
    from growth_automation import retry_growth_post, run_growth_automation, sync_growth_post_metrics
    from youtube_connector import (
        build_authorization_url,
        complete_upload,
        connect_youtube,
        create_upload_session,
        disconnect_youtube,
        sync_youtube_metrics,
        youtube_connection_state,
    )
    from tiktok_connector import (
        build_authorization_url as build_tiktok_authorization_url,
        complete_upload as complete_tiktok_upload,
        connect_tiktok,
        create_upload_session as create_tiktok_upload_session,
        disconnect_tiktok,
        fetch_creator_info as fetch_tiktok_creator_info,
        fetch_publish_status as fetch_tiktok_publish_status,
        tiktok_connection_state,
    )

try:
    from .reddit_connector import (
        build_authorization_url as build_reddit_authorization_url,
        connect_reddit,
        disconnect_reddit,
        normalize_subreddit,
        submit_reddit_post,
    )
except ImportError:
    from reddit_connector import (
        build_authorization_url as build_reddit_authorization_url,
        connect_reddit,
        disconnect_reddit,
        normalize_subreddit,
        submit_reddit_post,
    )

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

PUBLIC_ADMIN_PREFIX = "/admin"
INTERNAL_ADMIN_PREFIX = "/api/admin"
PUBLIC_API_PREFIX = f"{PUBLIC_ADMIN_PREFIX}/api"

admin_router = APIRouter()
admin_api_router = APIRouter(prefix="/api")
public_api_router = APIRouter()


@asynccontextmanager
async def lifespan(_: FastAPI):
    init_database()
    yield


app = FastAPI(
    title="PureHub Command Center",
    summary="Admin panel and automation control surface for PureHub growth systems.",
    version="0.5.0",
    lifespan=lifespan,
)

app.add_middleware(
    SessionMiddleware,
    secret_key=get_env_value("SESSION_SECRET", "change-me-in-command-center-env"),
    same_site="lax",
    https_only=get_env_value("VERCEL_ENV") in {"production", "preview"},
)


@app.middleware("http")
async def protect_admin_responses(request: Request, call_next):
    response = await call_next(request)
    if request.url.path.startswith((PUBLIC_ADMIN_PREFIX, INTERNAL_ADMIN_PREFIX)):
        response.headers["Cache-Control"] = "no-store, max-age=0"
        response.headers["Pragma"] = "no-cache"
        response.headers["X-Robots-Tag"] = "noindex, nofollow"
    return response


def get_client_ip(request: Request) -> str:
    forwarded_for = request.headers.get("x-forwarded-for", "")
    if forwarded_for.strip():
        return forwarded_for.split(",")[0].strip()
    return request.client.host if request.client else "unknown"


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


def _dashboard_context(
    request: Request,
    *,
    view: Literal["overview", "advanced"],
    message: str,
    message_type: Literal["success", "info", "error"],
    miniapp_query: str,
    miniapp_tab: str,
    api_query: str,
    api_group: str,
    support_page: int,
    support_filter: str,
) -> dict[str, Any]:
    admin_username = str(request.session["admin_username"])
    active_support_statuses = ("new", "draft_ready", "approved", "failed", "manual_required")
    support_page_size = 20
    support_page = max(1, support_page)
    support_filter = support_filter if support_filter in {"all", "bugs", "purehub_post", "product_feedback", "direct_support", "social_mention", "social_opportunity"} else "all"
    common_loaders = {
        "config": list_config,
        "schema_status": get_schema_status,
        "analytics": get_analytics_snapshot,
        "releases": list_releases,
    }
    overview_loaders = {
        "stats": get_user_stats,
        "release_publications": list_release_publications,
        "support_messages": lambda: list_support_messages(limit=100),
        "active_support_messages": lambda: list_support_messages(
            statuses=active_support_statuses,
            limit=support_page_size,
            skip=(support_page - 1) * support_page_size,
            inbox_filter=support_filter,
        ),
        "active_support_count": lambda: count_support_messages(statuses=active_support_statuses, inbox_filter=support_filter),
        "support_metrics": get_support_metrics,
        "support_sync_states": list_support_sync_states,
        "community_metrics_list": list_community_metrics,
        "growth_posts": lambda: list_growth_posts(40),
        "growth_summary": get_growth_summary,
        "product_growth": get_product_growth_snapshot,
    }
    advanced_loaders = {
        "metrics": get_dashboard_metrics,
        "miniapps": lambda: list_miniapps(miniapp_query, miniapp_tab),
        "admins": list_admin_accounts,
        "admin_profile": lambda: get_admin_profile(admin_username),
        "jobs": list_article_jobs,
        "top_referrers": list_top_referrers,
        "api_catalog": lambda: list_api_catalog(api_query, api_group),
        "audit_logs": list_audit_logs,
        "release_publications": list_release_publications,
    }
    loaders = {**common_loaders, **(overview_loaders if view == "overview" else advanced_loaders)}
    # Dashboard sections are independent Mongo reads. Running them concurrently avoids
    # paying one network round trip after another on a serverless cold start.
    with ThreadPoolExecutor(max_workers=min(8, len(loaders))) as executor:
        futures = {key: executor.submit(loader) for key, loader in loaders.items()}
        loaded = {key: future.result() for key, future in futures.items()}

    support_messages = loaded.get("support_messages", [])
    active_support_count = int(loaded.get("active_support_count", 0))
    support_page_count = max(1, (active_support_count + support_page_size - 1) // support_page_size)
    if support_page > support_page_count:
        support_page = support_page_count
        active_support_messages = list_support_messages(
            statuses=active_support_statuses,
            limit=support_page_size,
            skip=(support_page - 1) * support_page_size,
            inbox_filter=support_filter,
        )
    else:
        active_support_messages = loaded.get("active_support_messages", [])
    thread_rows: dict[str, list[dict[str, Any]]] = {}
    for support_item in support_messages:
        thread_key = f"{support_item.get('platform', '')}:{support_item.get('thread_id') or support_item.get('external_id') or support_item.get('id')}"
        thread_rows.setdefault(thread_key, []).append(support_item)
    for support_item in active_support_messages:
        thread_key = f"{support_item.get('platform', '')}:{support_item.get('thread_id') or support_item.get('external_id') or support_item.get('id')}"
        parent_external_id = str(support_item.get("parent_external_id") or "")
        conversation = list(thread_rows.get(thread_key, [support_item]))
        for candidate in support_messages:
            if candidate.get("id") == support_item.get("id"):
                continue
            candidate_ids = {
                str(candidate.get("external_id") or ""),
                str(candidate.get("external_reply_id") or ""),
                str(candidate.get("parent_external_id") or ""),
                str(candidate.get("thread_id") or ""),
            }
            if parent_external_id and parent_external_id in candidate_ids and candidate not in conversation:
                conversation.append(candidate)
        has_previous_reply = any(
            row.get("id") != support_item.get("id") and row.get("status") in {"replied", "manual_required"}
            for row in conversation
        )
        support_item["conversation_count"] = len(conversation)
        support_item["conversation_state"] = "reopened" if has_previous_reply else "awaiting_us"
        previous_reply = next(
            (
                row for row in conversation
                if parent_external_id
                and str(row.get("external_reply_id") or "") == parent_external_id
                and (row.get("reply_text") or row.get("ai_draft"))
            ),
            None,
        )
        support_item["previous_reply_text"] = (
            str(previous_reply.get("reply_text") or previous_reply.get("ai_draft") or "") if previous_reply else ""
        )
    support_history = [item for item in support_messages if item.get("status") in {"replied", "ignored"}][:12]
    for support_item in support_history:
        support_item["conversation_state"] = "awaiting_user" if support_item.get("status") == "replied" else "resolved"
    releases = loaded["releases"]
    current_release_id = str(releases[0].get("release_id", "")) if releases else ""
    distribution_submissions = list_distribution_submissions(current_release_id) if current_release_id else []
    completed_distribution_statuses = {"listed", "not_applicable"}
    distribution_completed = sum(
        1 for item in distribution_submissions if item.get("status") in completed_distribution_statuses
    )
    release_publications = loaded.get("release_publications", [])
    actionable_publications = [
        item
        for item in release_publications
        if item.get("status") in {"draft", "approved", "waiting_credentials", "failed"}
        and item.get("channel") != "reply"
        and item.get("language") == "en"
        and item.get("channel") in {"telegram", "devto", "bluesky", "mastodon"}
        and item.get("release_id") == current_release_id
    ]
    reddit_publication = next(
        (
            item
            for item in release_publications
            if item.get("channel") == "reddit"
            and item.get("language") == "en"
            and item.get("release_id") == current_release_id
        ),
        {},
    )
    config = loaded["config"]
    analytics = loaded["analytics"]
    youtube_connection = {
        "client_configured": bool(config.get("youtube_client_id") and config.get("youtube_client_secret")),
        "connected": bool(config.get("youtube_refresh_token")),
        "channel_id": config.get("youtube_channel_id"),
        "channel_title": config.get("youtube_channel_title"),
        "privacy": config.get("youtube_default_privacy", "unlisted"),
    }
    tiktok_connection = {
        "client_configured": bool(config.get("tiktok_client_key") and config.get("tiktok_client_secret")),
        "connected": bool(config.get("tiktok_refresh_token")),
        "display_name": config.get("tiktok_display_name", ""),
        "open_id": config.get("tiktok_open_id", ""),
        "environment": config.get("tiktok_environment", "sandbox"),
    }
    reddit_connection = {
        "client_configured": bool(config.get("reddit_client_id") and config.get("reddit_client_secret")),
        "connected": bool(config.get("reddit_refresh_token")),
        "username": config.get("reddit_username", ""),
        "default_subreddit": config.get("reddit_default_subreddit", "droidappshowcase"),
    }
    august_goal = build_august_growth_goal(
        config=config,
        community_metrics=loaded.get("community_metrics_list", []),
        growth_posts=loaded.get("growth_posts", []),
        support_messages=support_messages,
        support_metrics=loaded.get("support_metrics", {}),
        reddit_connected=reddit_connection["connected"],
    ) if view == "overview" else {}
    support_sync_states = loaded.get("support_sync_states", [])
    opportunity_sync_state = next(
        (item for item in support_sync_states if item.get("platform") == "opportunities"),
        {},
    )
    return {
        "config": config,
        "defaults": CONFIG_DEFAULTS,
        "stats": loaded.get("stats", {}),
        "metrics": loaded.get("metrics", {}),
        "jobs": loaded.get("jobs", []),
        "top_referrers": loaded.get("top_referrers", []),
        "bot_state": telegram_bot_manager.state,
        "miniapps": loaded.get("miniapps", []),
        "api_catalog": loaded.get("api_catalog", []),
        "audit_logs": loaded.get("audit_logs", []),
        "schema_status": loaded["schema_status"],
        "analytics": analytics,
        "analytics_json": json.dumps(analytics, ensure_ascii=False),
        "admins": loaded.get("admins", []),
        "admin_roles": ADMIN_ROLES,
        "message": message,
        "message_type": message_type,
        "admin_prefix": PUBLIC_ADMIN_PREFIX,
        "api_prefix": PUBLIC_API_PREFIX,
        "default_keywords": "\n".join(DEFAULT_KEYWORDS),
        "admin_username": admin_username,
        "admin_profile": loaded.get("admin_profile", {}),
        "releases": releases,
        "distribution_submissions": distribution_submissions,
        "distribution_statuses": DISTRIBUTION_STATUSES,
        "distribution_completed": distribution_completed,
        "release_publications": release_publications,
        "actionable_publications": actionable_publications,
        "reddit_publication": reddit_publication,
        "reddit_draft": parse_reddit_draft(str(reddit_publication.get("content", ""))),
        "active_support_messages": active_support_messages,
        "support_page": support_page,
        "support_page_count": support_page_count,
        "active_support_count": active_support_count,
        "support_page_size": support_page_size,
        "support_filter": support_filter,
        "support_history": support_history,
        "support_metrics": loaded.get("support_metrics", {}),
        "support_sync_states": support_sync_states,
        "opportunity_sync_state": opportunity_sync_state,
        "community_metrics": {item["platform"]: item for item in loaded.get("community_metrics_list", [])},
        "growth_posts": loaded.get("growth_posts", []),
        "growth_summary": loaded.get("growth_summary", {}),
        "youtube_connection": youtube_connection,
        "tiktok_connection": tiktok_connection,
        "reddit_connection": reddit_connection,
        "august_goal": august_goal,
        "product_growth": loaded.get("product_growth", {}),
        "mongo_db_name": get_env_value("MONGO_DB_NAME", "purehub_command_center"),
        "miniapp_query": miniapp_query,
        "miniapp_tab": miniapp_tab,
        "api_query": api_query,
        "api_group": api_group,
    }


@admin_router.get("", response_class=HTMLResponse)
def dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info", "error"] = "success",
    miniapp_query: str = "",
    miniapp_tab: str = "",
    api_query: str = "",
    api_group: str = "",
    support_page: int = 1,
    support_filter: str = "all",
) -> HTMLResponse:
    if not request.session.get("admin_username"):
        return RedirectResponse(url=f"{PUBLIC_ADMIN_PREFIX}/login", status_code=303)

    return templates.TemplateResponse(
        request=request,
        name="index.html",
        context=_dashboard_context(
            request,
            view="overview",
            message=message,
            message_type=message_type,
            miniapp_query=miniapp_query,
            miniapp_tab=miniapp_tab,
            api_query=api_query,
            api_group=api_group,
            support_page=support_page,
            support_filter=support_filter,
        ),
    )


@admin_router.get("/advanced", response_class=HTMLResponse)
def advanced_dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info", "error"] = "success",
    miniapp_query: str = "",
    miniapp_tab: str = "",
    api_query: str = "",
    api_group: str = "",
    support_page: int = 1,
) -> HTMLResponse:
    if not request.session.get("admin_username"):
        return RedirectResponse(url=f"{PUBLIC_ADMIN_PREFIX}/login", status_code=303)

    return templates.TemplateResponse(
        request=request,
        name="advanced.html",
        context=_dashboard_context(
            request,
            view="advanced",
            message=message,
            message_type=message_type,
            miniapp_query=miniapp_query,
            miniapp_tab=miniapp_tab,
            api_query=api_query,
            api_group=api_group,
            support_page=support_page,
            support_filter="all",
        ),
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
    telegram_support_chat_id: str = Form(default="-1003762178712"),
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
    support_monitor_enabled: str = Form(default="true"),
    opportunity_monitor_enabled: str = Form(default="true"),
    opportunity_keywords: str = Form(default="best offline app,app without ads,privacy first app,open source Android app,offline OCR scanner,QR scanner no ads,simple Pomodoro app,password manager offline,expense tracker offline,unit converter app,habit tracker no ads,note app offline,flashlight app no ads,bubble level app,document scanner offline,wifi analyzer app"),
    opportunity_daily_minimum: int = Form(default=10),
    opportunity_daily_limit: int = Form(default=30),
    opportunity_scan_runs_per_day: int = Form(default=4),
    growth_automation_enabled: str = Form(default="false"),
    growth_auto_publish: str = Form(default="true"),
    growth_campaign_start_date: str = Form(default=""),
    growth_timezone: str = Form(default="Asia/Bangkok"),
    youtube_client_id: str = Form(default=""),
    youtube_client_secret: str = Form(default=""),
    youtube_default_privacy: str = Form(default="unlisted"),
    tiktok_client_key: str = Form(default=""),
    tiktok_client_secret: str = Form(default=""),
    tiktok_environment: str = Form(default="sandbox"),
    reddit_client_id: str = Form(default=""),
    reddit_client_secret: str = Form(default=""),
    reddit_default_subreddit: str = Form(default="droidappshowcase"),
    reddit_user_agent: str = Form(default="web:PureHub.CommandCenter:v1.0 (by /u/PureHubAAA)"),
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
            "telegram_support_chat_id": telegram_support_chat_id.strip() or "-1003762178712",
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
            "support_monitor_enabled": "true" if support_monitor_enabled.strip().lower() == "true" else "false",
            "opportunity_monitor_enabled": "true" if opportunity_monitor_enabled.strip().lower() == "true" else "false",
            "opportunity_keywords": opportunity_keywords.strip(),
            "opportunity_daily_minimum": str(max(1, min(int(opportunity_daily_minimum), 30))),
            "opportunity_daily_limit": str(max(1, min(int(opportunity_daily_minimum), 30), min(int(opportunity_daily_limit), 30))),
            "opportunity_scan_runs_per_day": str(max(3, min(int(opportunity_scan_runs_per_day), 5))),
            "growth_automation_enabled": "true" if growth_automation_enabled.strip().lower() == "true" else "false",
            "growth_auto_publish": "true" if growth_auto_publish.strip().lower() == "true" else "false",
            "growth_campaign_start_date": growth_campaign_start_date.strip(),
            "growth_timezone": growth_timezone.strip() or "Asia/Bangkok",
            "youtube_client_id": youtube_client_id.strip() or current_config.get("youtube_client_id", ""),
            "youtube_client_secret": youtube_client_secret.strip() or current_config.get("youtube_client_secret", ""),
            "youtube_default_privacy": youtube_default_privacy if youtube_default_privacy in {"private", "unlisted", "public"} else "unlisted",
            "tiktok_client_key": tiktok_client_key.strip() or current_config.get("tiktok_client_key", ""),
            "tiktok_client_secret": tiktok_client_secret.strip() or current_config.get("tiktok_client_secret", ""),
            "tiktok_environment": "production" if tiktok_environment == "production" else "sandbox",
            "reddit_client_id": reddit_client_id.strip() or current_config.get("reddit_client_id", ""),
            "reddit_client_secret": reddit_client_secret.strip() or current_config.get("reddit_client_secret", ""),
            "reddit_default_subreddit": reddit_default_subreddit.strip().removeprefix("r/") or "droidappshowcase",
            "reddit_user_agent": reddit_user_agent.strip() or "web:PureHub.CommandCenter:v1.0 (by /u/PureHubAAA)",
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


@admin_router.get("/export/backup")
def download_control_backup(request: Request) -> Response:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    bundle = export_control_bundle()
    record_audit_log(
        actor=actor,
        action="download_control_backup",
        target_type="bundle",
        target_id="control_bundle",
        details={"miniapps": len(bundle.get("miniapps", [])), "api_catalog": len(bundle.get("api_catalog", []))},
        request_meta=request_meta(request),
    )
    filename = f"purehub-backup-{datetime.now(timezone.utc):%Y%m%d}.json"
    return Response(
        content=json.dumps(bundle, ensure_ascii=False, indent=2).encode("utf-8"),
        media_type="application/octet-stream",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


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


@admin_router.post("/import/backup")
async def restore_control_backup(
    request: Request,
    backup_file: UploadFile = File(...),
    mode: str = Form(default="merge"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    try:
        payload = await backup_file.read(2 * 1024 * 1024 + 1)
        if len(payload) > 2 * 1024 * 1024:
            raise ValueError("Backup file is larger than 2 MB.")
        bundle = json.loads(payload.decode("utf-8-sig"))
        result = import_control_bundle(bundle, mode=mode.strip().lower())
    except Exception as exc:
        return _redirect_with_message(f"Restore failed: {exc}", "error")
    record_audit_log(
        actor=actor,
        action="restore_control_backup",
        target_type="bundle",
        target_id="control_bundle",
        details={"mode": mode.strip().lower(), "filename": backup_file.filename or "backup", **result},
        request_meta=request_meta(request),
    )
    return _redirect_with_message(
        f"Backup restored: {result['miniapps']} mini-apps, {result['api_catalog']} APIs.",
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


@admin_router.post("/releases/{release_id}/distribution/{stage}")
def update_distribution_action(
    request: Request,
    release_id: str,
    stage: str,
    status: str = Form(...),
    url: str = Form(default=""),
    note: str = Form(default=""),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    if not get_release(release_id):
        return _redirect_with_message("Release not found.", "error")
    try:
        update_distribution_submission(release_id, stage, status=status, url=url, note=note)
        record_audit_log(
            actor=actor,
            action="update_distribution_submission",
            target_type="distribution_submission",
            target_id=f"{release_id}:{stage}",
            details={"status": status, "url": url.strip()},
        )
        return _redirect_with_message(f"Distribution stage updated to {status}.", "success", anchor="distribution")
    except ValueError as exc:
        return _redirect_with_message(str(exc), "error", anchor="distribution")


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


@admin_router.post("/releases/{release_id}/reddit/generate")
def generate_reddit_draft_action(request: Request, release_id: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        generate_reddit_draft(release_id)
        record_audit_log(
            actor=actor,
            action="generate_reddit_draft",
            target_type="release_publication",
            target_id=f"{release_id}:reddit:en",
        )
        return _redirect_with_message("Reddit review draft generated.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Reddit draft generation failed: {exc}", "error")


@admin_router.post("/releases/{release_id}/reddit/save")
def save_reddit_draft_action(
    request: Request,
    release_id: str,
    title: str = Form(...),
    body: str = Form(...),
    communities: str = Form(default=""),
    status: str = Form(default="ready_manual"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    if status not in {"draft", "ready_manual"}:
        return _redirect_with_message("Invalid Reddit draft status.", "error")
    update_release_publication(
        release_id,
        "reddit",
        "en",
        {
            "content": format_reddit_draft(title, body, communities),
            "status": status,
            "error_message": "",
        },
    )
    record_audit_log(
        actor=actor,
        action="save_reddit_draft",
        target_type="release_publication",
        target_id=f"{release_id}:reddit:en",
        details={"status": status},
    )
    return _redirect_with_message("Reddit draft saved for manual review.", "success")


@admin_router.post("/releases/{release_id}/reddit/publish")
def publish_reddit_draft_action(
    request: Request,
    release_id: str,
    title: str = Form(...),
    body: str = Form(...),
    communities: str = Form(...),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    publication = next(
        (
            item
            for item in list_release_publications(release_id)
            if item.get("channel") == "reddit" and item.get("language") == "en"
        ),
        None,
    )
    if not publication:
        return _redirect_with_message("Generate and review the Reddit draft first.", "error")
    if publication.get("status") == "published" and publication.get("external_url"):
        return _redirect_with_message("This Reddit draft was already published.", "info")
    if not claim_release_publication(release_id, "reddit", "en"):
        return _redirect_with_message("This Reddit draft is already published or currently publishing.", "info")
    try:
        subreddit = normalize_subreddit(communities)
        external_id, external_url = submit_reddit_post(
            subreddit=subreddit,
            title=title,
            body=body,
        )
        update_release_publication(
            release_id,
            "reddit",
            "en",
            {
                "content": format_reddit_draft(title, body, subreddit),
                "status": "published",
                "external_id": external_id,
                "external_url": external_url,
                "error_message": "",
                "published_at": datetime.now(timezone.utc),
                "increment_attempts": True,
            },
        )
        record_audit_log(
            actor=actor,
            action="publish_reddit_post",
            target_type="release_publication",
            target_id=f"{release_id}:reddit:en",
            details={"subreddit": subreddit, "url": external_url},
        )
        return _redirect_with_message(f"Published to r/{subreddit} successfully.", "success")
    except Exception as exc:
        update_release_publication(
            release_id,
            "reddit",
            "en",
            {"status": "failed", "error_message": str(exc)[:500], "increment_attempts": True},
        )
        return _redirect_with_message(f"Reddit publishing failed: {exc}", "error")


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


@admin_router.post("/support/sync")
def support_sync_action(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    result = sync_support_channels(generate_drafts=True)
    created = sum(int(item.get("created", 0)) for item in result.get("channels", {}).values())
    failures = [name for name, item in result.get("channels", {}).items() if not item.get("ok")]
    record_audit_log(
        actor=actor,
        action="sync_support_channels",
        target_type="community_support",
        target_id="all",
        details={"created": created, "failures": failures, "drafts": result.get("drafts", {})},
    )
    if failures:
        return _redirect_with_message(f"Support sync completed with errors: {', '.join(failures)}.", "error")
    return _redirect_with_message(f"Support sync complete: {created} new messages.", "success")


def _support_return_query(return_page: int, return_filter: str) -> dict[str, Any]:
    allowed = {"all", "bugs", "purehub_post", "product_feedback", "direct_support", "social_mention", "social_opportunity"}
    return {
        "support_page": max(1, return_page),
        "support_filter": return_filter if return_filter in allowed else "all",
    }


@admin_router.post("/support/{message_id}/draft")
def support_draft_action(
    request: Request,
    message_id: str,
    reply_text: str = Form(default=""),
    regeneration_note: str = Form(default=""),
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    row = get_support_message(message_id)
    if not row:
        return _redirect_with_message("Support message not found.", "error", anchor="support")
    previous_draft = reply_text.strip() or str(row.get("reply_text") or row.get("ai_draft") or "").strip()
    try:
        result = generate_support_draft(
            message_id,
            previous_draft=previous_draft,
            guidance=regeneration_note,
        )
        record_audit_log(
            actor=actor,
            action="regenerate_support_draft" if previous_draft else "generate_support_draft",
            target_type="support_message",
            target_id=message_id,
            details={"platform": row.get("platform"), "operator_guidance": bool(regeneration_note.strip())},
        )
        message = "A new AI reply alternative is ready for review." if previous_draft else f"AI draft generated as {result.get('category', 'support')}"
        return _redirect_with_message(message, "success", anchor="support", query=_support_return_query(return_page, return_filter))
    except Exception as exc:
        return _redirect_with_message(
            f"AI reply generation failed: {exc}",
            "error",
            anchor="support",
            query=_support_return_query(return_page, return_filter),
        )


@admin_router.post("/support/{message_id}/review")
def support_review_action(
    request: Request,
    message_id: str,
    reply_text: str = Form(default=""),
    action: str = Form(default="save"),
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    row = get_support_message(message_id)
    if not row:
        return _redirect_with_message("Support message not found.", "error", anchor="support")
    if action not in {"save", "approve", "ignore"}:
        return _redirect_with_message("Unsupported support action.", "error", anchor="support")
    status = {"save": "draft_ready", "approve": "approved", "ignore": "ignored"}[action]
    if action != "ignore" and not reply_text.strip():
        return _redirect_with_message("Reply text cannot be empty.", "error", anchor="support")
    update_support_message(message_id, {"reply_text": reply_text.strip(), "status": status, "error_message": ""})
    record_audit_log(
        actor=actor,
        action=f"{action}_support_reply",
        target_type="support_message",
        target_id=message_id,
        details={"platform": row.get("platform")},
    )
    return _redirect_with_message(
        f"Support reply {action} completed.",
        "success",
        anchor="support",
        query=_support_return_query(return_page, return_filter),
    )


@admin_router.post("/support/bulk-approve")
def support_bulk_approve_action(
    request: Request,
    message_ids: list[str] | None = Form(default=None),
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    selected_ids = list(dict.fromkeys(message_ids or []))[:100]
    approved_ids: list[str] = []
    skipped = 0
    for message_id in selected_ids:
        row = get_support_message(message_id)
        reply_text = str((row or {}).get("reply_text") or (row or {}).get("ai_draft") or "").strip()
        if not row or row.get("status") not in {"draft_ready", "failed"} or not reply_text:
            skipped += 1
            continue
        update_support_message(message_id, {"reply_text": reply_text, "status": "approved", "error_message": ""})
        approved_ids.append(message_id)
    if approved_ids:
        record_audit_log(
            actor=actor,
            action="bulk_approve_support_replies",
            target_type="support_message_batch",
            target_id=f"page-{max(1, return_page)}",
            details={"approved_ids": approved_ids, "approved_count": len(approved_ids), "skipped_count": skipped},
        )
    if not selected_ids:
        message, message_type = "Select at least one AI draft to approve.", "info"
    elif not approved_ids:
        message, message_type = "No selected replies were eligible for approval.", "info"
    else:
        suffix = f" {skipped} ineligible item(s) skipped." if skipped else ""
        noun = "reply" if len(approved_ids) == 1 else "replies"
        message, message_type = f"Approved {len(approved_ids)} support {noun}.{suffix}", "success"
    return _redirect_with_message(
        message,
        message_type,
        anchor="support",
        query=_support_return_query(return_page, return_filter),
    )


@admin_router.post("/support/bulk-send")
def support_bulk_send_action(
    request: Request,
    message_ids: list[str] | None = Form(default=None),
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    selected_ids = list(dict.fromkeys(message_ids or []))[:100]
    replied_ids: list[str] = []
    manual_ids: list[str] = []
    failed_ids: list[str] = []
    skipped = 0
    for message_id in selected_ids:
        row = get_support_message(message_id)
        if not row or row.get("status") != "approved":
            skipped += 1
            continue
        try:
            result = send_support_reply(message_id)
            if result.get("status") == "manual_required":
                manual_ids.append(message_id)
            elif result.get("status") == "replied":
                replied_ids.append(message_id)
            else:
                failed_ids.append(message_id)
        except Exception:
            failed_ids.append(message_id)
    processed_ids = replied_ids + manual_ids
    if selected_ids:
        record_audit_log(
            actor=actor,
            action="bulk_send_support_replies",
            target_type="support_message_batch",
            target_id=f"page-{max(1, return_page)}",
            details={
                "replied_ids": replied_ids,
                "manual_ids": manual_ids,
                "failed_ids": failed_ids,
                "skipped_count": skipped,
            },
        )
    if not selected_ids:
        message, message_type = "Select at least one approved reply to send.", "info"
    elif not processed_ids and not failed_ids:
        message, message_type = "No selected replies were approved and ready to send.", "info"
    else:
        parts = []
        if replied_ids:
            parts.append(f"sent {len(replied_ids)}")
        if manual_ids:
            parts.append(f"prepared {len(manual_ids)} DEV manual repl{'y' if len(manual_ids) == 1 else 'ies'}")
        if failed_ids:
            parts.append(f"failed {len(failed_ids)}")
        if skipped:
            parts.append(f"skipped {skipped} ineligible")
        message = "Bulk support result: " + ", ".join(parts) + "."
        message_type = "error" if failed_ids and not processed_ids else ("info" if failed_ids or manual_ids else "success")
    return _redirect_with_message(
        message,
        message_type,
        anchor="support",
        query=_support_return_query(return_page, return_filter),
    )


@admin_router.post("/support/{message_id}/send")
def support_send_action(
    request: Request,
    message_id: str,
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        row = send_support_reply(message_id)
        record_audit_log(
            actor=actor,
            action="send_support_reply",
            target_type="support_message",
            target_id=message_id,
            details={"platform": row.get("platform"), "status": row.get("status")},
        )
        if row.get("status") == "manual_required":
            return _redirect_with_message(
                "DEV draft approved. Open the source comment and paste the prepared reply.",
                "info",
                anchor="support",
                query=_support_return_query(return_page, return_filter),
            )
        return _redirect_with_message(
            "Support reply sent successfully.",
            "success",
            anchor="support",
            query=_support_return_query(return_page, return_filter),
        )
    except Exception as exc:
        return _redirect_with_message(
            f"Support reply failed: {exc}",
            "error",
            anchor="support",
            query=_support_return_query(return_page, return_filter),
        )


@admin_router.post("/support/{message_id}/complete-manual")
def support_complete_manual_action(
    request: Request,
    message_id: str,
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    row = get_support_message(message_id)
    if not row:
        return _redirect_with_message(
            "Support message not found.",
            "error",
            anchor="support",
            query=_support_return_query(return_page, return_filter),
        )
    if row.get("status") != "manual_required":
        return _redirect_with_message(
            "This reply does not require manual completion.",
            "info",
            anchor="support",
            query=_support_return_query(return_page, return_filter),
        )
    completed_at = datetime.now(timezone.utc)
    values: dict[str, Any] = {
        "status": "replied",
        "replied_at": completed_at,
        "manual_completed_at": completed_at,
        "error_message": "",
    }
    if not row.get("external_reply_url") and row.get("source_url"):
        values["external_reply_url"] = row["source_url"]
    update_support_message(message_id, values)
    record_audit_log(
        actor=actor,
        action="complete_manual_support_reply",
        target_type="support_message",
        target_id=message_id,
        details={"platform": row.get("platform")},
    )
    return _redirect_with_message(
        "Manual reply marked as completed.",
        "success",
        anchor="support",
        query=_support_return_query(return_page, return_filter),
    )


@admin_router.post("/support/{message_id}/delete")
def support_delete_action(
    request: Request,
    message_id: str,
    return_page: int = Form(default=1),
    return_filter: str = Form(default="all"),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    row = get_support_message(message_id)
    if not row:
        return _redirect_with_message("Support message not found.", "error", anchor="support")
    if not delete_support_message(message_id):
        return _redirect_with_message("Support message could not be deleted.", "error", anchor="support")
    record_audit_log(
        actor=actor,
        action="delete_support_message",
        target_type="support_message",
        target_id=message_id,
        details={"platform": row.get("platform"), "status": row.get("status")},
    )
    return _redirect_with_message(
        "Support message deleted permanently.",
        "success",
        anchor="support",
        query=_support_return_query(return_page, return_filter),
    )


@admin_router.post("/growth/run")
def growth_run_action(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        result = run_growth_automation(force=True, actor=actor)
        return _redirect_with_message(
            f"Growth Autopilot day {result.get('cycle_day', 1)} processed: {result.get('published', 0)} published.",
            "success",
        )
    except Exception as exc:
        return _redirect_with_message(f"Growth Autopilot failed: {exc}", "error")


@admin_router.post("/growth/metrics")
def growth_metrics_action(request: Request) -> RedirectResponse:
    require_admin_role(request, "superadmin", "editor")
    social = sync_growth_post_metrics()
    try:
        youtube = sync_youtube_metrics()
    except Exception:
        youtube = {}
    return _redirect_with_message(f"Growth metrics refreshed for {len(social) + len(youtube)} post(s).", "success")


@admin_router.post("/growth/{post_id}/retry")
def growth_retry_action(request: Request, post_id: str) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    try:
        row = retry_growth_post(post_id)
        record_audit_log(
            actor=actor,
            action="retry_growth_post",
            target_type="growth_post",
            target_id=post_id,
            details={"status": row.get("status"), "channel": row.get("channel")},
        )
        return _redirect_with_message(f"{str(row.get('channel', 'Post')).title()} retry: {row.get('status', 'processed')}.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Growth retry failed: {exc}", "error")


@admin_router.post("/growth/{post_id}/reddit/publish")
def growth_reddit_publish_action(
    request: Request,
    post_id: str,
    subreddit: str = Form(...),
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    row = get_growth_post(post_id)
    if not row or row.get("channel") != "reddit":
        return _redirect_with_message("Reddit campaign draft not found.", "error")
    if row.get("status") == "published" and row.get("external_url"):
        return _redirect_with_message("This Reddit campaign post was already published.", "info")
    if not claim_growth_post(post_id, "reddit"):
        return _redirect_with_message("This Reddit campaign post is already published or currently publishing.", "info")
    draft = parse_reddit_draft(str(row.get("content", "")))
    try:
        target = normalize_subreddit(subreddit)
        external_id, external_url = submit_reddit_post(
            subreddit=target,
            title=draft["title"],
            body=draft["body"],
        )
        update_growth_post(
            post_id,
            {
                "status": "published",
                "external_id": external_id,
                "external_url": external_url,
                "error_message": "",
                "published_at": datetime.now(timezone.utc),
                "increment_attempts": True,
            },
        )
        record_audit_log(
            actor=actor,
            action="publish_growth_reddit_post",
            target_type="growth_post",
            target_id=post_id,
            details={"subreddit": target, "url": external_url},
        )
        return _redirect_with_message(f"Published campaign post to r/{target}.", "success")
    except Exception as exc:
        update_growth_post(
            post_id,
            {"status": "failed", "error_message": str(exc)[:500], "increment_attempts": True},
        )
        return _redirect_with_message(f"Reddit publishing failed: {exc}", "error")


@admin_router.get("/reddit/connect")
def reddit_connect_action(request: Request) -> RedirectResponse:
    require_admin_role(request, "superadmin")
    state = secrets.token_urlsafe(32)
    request.session["reddit_oauth_state"] = state
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/reddit/callback"
    try:
        return RedirectResponse(
            url=build_reddit_authorization_url(state=state, redirect_uri=redirect_uri),
            status_code=302,
        )
    except Exception as exc:
        return _redirect_with_message(f"Reddit connection failed: {exc}", "error")


@admin_router.get("/reddit/callback")
def reddit_callback_action(request: Request, code: str = "", state: str = "", error: str = "") -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    expected = str(request.session.pop("reddit_oauth_state", ""))
    if error:
        return _redirect_with_message(f"Reddit authorization was cancelled: {error}", "error")
    if not expected or not secrets.compare_digest(expected, state) or not code:
        return _redirect_with_message("Invalid Reddit OAuth response.", "error")
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/reddit/callback"
    try:
        connection = connect_reddit(code=code, redirect_uri=redirect_uri)
        record_audit_log(
            actor=actor,
            action="connect_reddit",
            target_type="social_connection",
            target_id=str(connection.get("username") or "reddit"),
        )
        return _redirect_with_message(f"Reddit connected: u/{connection.get('username') or 'account ready'}.", "success")
    except Exception as exc:
        return _redirect_with_message(f"Reddit OAuth failed: {exc}", "error")


@admin_router.post("/reddit/disconnect")
def reddit_disconnect_action(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    disconnect_reddit()
    record_audit_log(actor=actor, action="disconnect_reddit", target_type="social_connection", target_id="reddit")
    return _redirect_with_message("Reddit disconnected.", "success")


@admin_router.get("/youtube/connect")
def youtube_connect_action(request: Request) -> RedirectResponse:
    require_admin_role(request, "superadmin")
    state = secrets.token_urlsafe(32)
    request.session["youtube_oauth_state"] = state
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/youtube/callback"
    try:
        return RedirectResponse(url=build_authorization_url(state=state, redirect_uri=redirect_uri), status_code=302)
    except Exception as exc:
        return _redirect_with_message(f"YouTube connection failed: {exc}", "error")


@admin_router.get("/youtube/callback")
def youtube_callback_action(request: Request, code: str = "", state: str = "", error: str = "") -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    expected = str(request.session.pop("youtube_oauth_state", ""))
    if error:
        return _redirect_with_message(f"YouTube authorization was cancelled: {error}", "error")
    if not expected or not secrets.compare_digest(expected, state) or not code:
        return _redirect_with_message("Invalid YouTube OAuth response.", "error")
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/youtube/callback"
    try:
        connection = connect_youtube(code=code, redirect_uri=redirect_uri)
        record_audit_log(
            actor=actor,
            action="connect_youtube",
            target_type="social_connection",
            target_id=str(connection.get("channel_id", "youtube")),
            details={"channel_title": connection.get("channel_title", "")},
        )
        return _redirect_with_message(f"YouTube connected: {connection.get('channel_title') or 'channel ready'}.", "success")
    except Exception as exc:
        return _redirect_with_message(f"YouTube OAuth failed: {exc}", "error")


@admin_router.post("/youtube/disconnect")
def youtube_disconnect_action(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    disconnect_youtube()
    record_audit_log(actor=actor, action="disconnect_youtube", target_type="social_connection", target_id="youtube")
    return _redirect_with_message("YouTube disconnected.", "success")


@admin_api_router.post("/youtube/upload-session")
async def youtube_upload_session_api(request: Request) -> dict[str, str]:
    require_admin_role(request, "superadmin", "editor")
    payload = await request.json()
    try:
        return create_upload_session(
            str(payload.get("post_id", "")),
            content_type=str(payload.get("content_type", "")),
            content_length=int(payload.get("content_length", 0)),
        )
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@admin_api_router.post("/youtube/complete")
async def youtube_complete_api(request: Request) -> dict[str, Any]:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    payload = await request.json()
    try:
        row = complete_upload(str(payload.get("post_id", "")), dict(payload.get("youtube") or {}))
        record_audit_log(
            actor=actor,
            action="upload_youtube_video",
            target_type="growth_post",
            target_id=str(row.get("id", "")),
            details={"video_id": row.get("external_id", ""), "url": row.get("external_url", "")},
        )
        return {"ok": True, "item": row}
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@admin_router.get("/tiktok/connect")
def tiktok_connect_action(request: Request) -> RedirectResponse:
    require_admin_role(request, "superadmin")
    state = secrets.token_urlsafe(32)
    request.session["tiktok_oauth_state"] = state
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/tiktok/callback"
    try:
        return RedirectResponse(
            url=build_tiktok_authorization_url(state=state, redirect_uri=redirect_uri),
            status_code=302,
        )
    except Exception as exc:
        return _redirect_with_message(f"TikTok connection failed: {exc}", "error")


@admin_router.get("/tiktok/callback")
def tiktok_callback_action(
    request: Request,
    code: str = "",
    state: str = "",
    error: str = "",
    error_description: str = "",
) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    expected = str(request.session.pop("tiktok_oauth_state", ""))
    if error:
        return _redirect_with_message(f"TikTok authorization was cancelled: {error_description or error}", "error")
    if not expected or not secrets.compare_digest(expected, state) or not code:
        return _redirect_with_message("Invalid TikTok OAuth response.", "error")
    redirect_uri = f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/admin/tiktok/callback"
    try:
        connection = connect_tiktok(code=code, redirect_uri=redirect_uri)
        record_audit_log(
            actor=actor,
            action="connect_tiktok",
            target_type="social_connection",
            target_id=str(connection.get("open_id") or "tiktok"),
            details={"display_name": connection.get("display_name", ""), "environment": connection.get("environment", "sandbox")},
        )
        return _redirect_with_message(f"TikTok connected: {connection.get('display_name') or 'creator ready'}.", "success")
    except Exception as exc:
        return _redirect_with_message(f"TikTok OAuth failed: {exc}", "error")


@admin_router.post("/tiktok/disconnect")
def tiktok_disconnect_action(request: Request) -> RedirectResponse:
    actor = require_admin_role(request, "superadmin")["username"]
    disconnect_tiktok()
    record_audit_log(actor=actor, action="disconnect_tiktok", target_type="social_connection", target_id="tiktok")
    return _redirect_with_message("TikTok disconnected.", "success")


@admin_api_router.get("/tiktok/creator-info")
def tiktok_creator_info_api(request: Request) -> dict[str, Any]:
    require_admin_role(request, "superadmin", "editor")
    try:
        return {"ok": True, "creator": fetch_tiktok_creator_info()}
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@admin_api_router.post("/tiktok/upload-session")
async def tiktok_upload_session_api(request: Request) -> dict[str, Any]:
    require_admin_role(request, "superadmin", "editor")
    payload = await request.json()
    if not bool(payload.get("consent")):
        raise HTTPException(status_code=422, detail="Approve this specific TikTok upload before continuing.")
    try:
        return create_tiktok_upload_session(
            caption=str(payload.get("caption", "")),
            mode=str(payload.get("mode", "draft")),
            privacy_level=str(payload.get("privacy_level", "SELF_ONLY")),
            disable_comment=bool(payload.get("disable_comment", False)),
            disable_duet=bool(payload.get("disable_duet", False)),
            disable_stitch=bool(payload.get("disable_stitch", False)),
            content_type=str(payload.get("content_type", "")),
            content_length=int(payload.get("content_length", 0)),
        )
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@admin_api_router.post("/tiktok/complete")
async def tiktok_complete_api(request: Request) -> dict[str, Any]:
    actor = require_admin_role(request, "superadmin", "editor")["username"]
    payload = await request.json()
    try:
        row = complete_tiktok_upload(
            post_id=str(payload.get("post_id", "")),
            publish_id=str(payload.get("publish_id", "")),
        )
        record_audit_log(
            actor=actor,
            action="upload_tiktok_video",
            target_type="growth_post",
            target_id=str(row.get("id", "")),
            details={"publish_id": str(payload.get("publish_id", ""))},
        )
        return {"ok": True, "item": row}
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@admin_api_router.post("/tiktok/status")
async def tiktok_status_api(request: Request) -> dict[str, Any]:
    require_admin_role(request, "superadmin", "editor")
    payload = await request.json()
    post_id = str(payload.get("post_id", ""))
    publish_id = str(payload.get("publish_id", ""))
    try:
        status = fetch_tiktok_publish_status(publish_id)
        row = get_growth_post(post_id)
        if row and row.get("channel") == "tiktok":
            metadata = dict(row.get("metadata") or {})
            metadata["tiktok_status"] = status
            state = str(status.get("status") or "").upper()
            values: dict[str, Any] = {"metadata": metadata}
            if state == "PUBLISH_COMPLETE":
                public_ids = status.get("publicaly_available_post_id") or status.get("publicly_available_post_id") or []
                values.update({"status": "published", "published_at": datetime.now(timezone.utc), "external_id": str(public_ids[0] if public_ids else publish_id)})
            elif state == "FAILED":
                values.update({"status": "failed", "error_message": str(status.get("fail_reason") or "TikTok processing failed")[:500]})
            else:
                values["status"] = "processing"
            update_growth_post(post_id, values)
        return {"ok": True, "status": status}
    except Exception as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


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


@admin_api_router.get("/support")
def support_api(request: Request, status: str = "", platform: str = "") -> dict[str, object]:
    require_admin_session(request)
    return {
        "items": list_support_messages(status=status, platform=platform, limit=100),
        "metrics": get_support_metrics(),
        "sync_states": list_support_sync_states(),
        "engagement": {item["platform"]: item for item in list_community_metrics()},
    }


@admin_api_router.post("/support/sync")
def support_sync_api(request: Request) -> dict[str, Any]:
    require_admin_role(request, "superadmin", "editor")
    return sync_support_channels(generate_drafts=True)


@public_api_router.post("/product-event")
async def public_product_event(request: Request) -> dict[str, bool]:
    payload = await request.json()
    try:
        record_miniapp_event(str(payload.get("miniapp_id", "")), str(payload.get("event", "")))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"ok": True}


@public_api_router.post("/journey-event")
async def public_journey_event(request: Request) -> dict[str, bool]:
    payload = await request.json()
    try:
        record_growth_funnel_event(
            str(payload.get("stage", "")),
            str(payload.get("source", "direct")),
            str(payload.get("campaign", "none")),
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"ok": True}


@public_api_router.post("/feedback")
async def public_feedback(request: Request) -> dict[str, bool]:
    payload = await request.json()
    if str(payload.get("website", "")).strip():
        return {"ok": True}
    miniapp_id = str(payload.get("miniapp_id", "")).strip()
    category = str(payload.get("category", "feedback")).strip()
    message = " ".join(str(payload.get("message", "")).split()).strip()
    if category not in {"feedback", "bug", "feature_request", "device_report"}:
        raise HTTPException(status_code=422, detail="Unsupported feedback category.")
    if len(message) < 10 or len(message) > 1000:
        raise HTTPException(status_code=422, detail="Feedback must contain 10 to 1,000 characters.")
    try:
        record_miniapp_event(miniapp_id, "feedback")
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    feedback_id = secrets.token_urlsafe(12)
    upsert_support_message(
        {
            "source_key": f"pwa:{feedback_id}",
            "platform": "pwa",
            "inbox_type": "product_feedback",
            "external_id": feedback_id,
            "thread_id": miniapp_id,
            "author_id": "",
            "author_name": "Anonymous early tester" if category == "device_report" else "Anonymous product feedback",
            "author_handle": "",
            "content": message,
            "category": category,
            "priority": "high" if category in {"bug", "device_report"} else "normal",
            "status": "new",
            "source_url": f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/en/tools",
            "received_at": datetime.now(timezone.utc),
            "reply_context": {"miniapp_id": miniapp_id, "privacy": "aggregate-no-identifier"},
        }
    )
    if category == "device_report":
        record_growth_funnel_event("device_report", "early-testers", miniapp_id)
    return {"ok": True}


@public_api_router.get("/roadmap")
def public_roadmap() -> dict[str, object]:
    items = list_roadmap_options()
    return {"items": items, "total_votes": sum(int(item.get("votes", 0) or 0) for item in items)}


@public_api_router.post("/roadmap/vote")
async def public_roadmap_vote(request: Request) -> dict[str, object]:
    payload = await request.json()
    try:
        item = record_roadmap_vote(str(payload.get("option_id", "")).strip())
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"ok": True, "item": item}


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


@public_api_router.post("/telegram-webhook")
async def telegram_webhook(request: Request) -> dict[str, bool]:
    expected_secret = get_env_value("TELEGRAM_WEBHOOK_SECRET")
    supplied_secret = request.headers.get("x-telegram-bot-api-secret-token", "")
    if not expected_secret or not secrets.compare_digest(supplied_secret, expected_secret):
        raise HTTPException(status_code=401, detail="Invalid Telegram webhook secret.")
    payload = await request.json()
    support_row = ingest_telegram_update(payload)
    telegram_bot_manager.process_webhook(payload)
    if support_row and support_row.get("status") == "new":
        try:
            generate_support_draft(str(support_row["id"]))
        except Exception:
            pass
    return {"ok": True}


@public_api_router.get("/support-sync")
def scheduled_support_sync(request: Request) -> dict[str, Any]:
    expected_secret = get_env_value("CRON_SECRET")
    supplied_secret = request.headers.get("authorization", "")
    if not expected_secret or not secrets.compare_digest(supplied_secret, f"Bearer {expected_secret}"):
        raise HTTPException(status_code=401, detail="Invalid cron authorization.")
    return sync_support_channels(generate_drafts=True)


@public_api_router.get("/growth-automation")
def scheduled_growth_automation(request: Request) -> dict[str, Any]:
    expected_secret = get_env_value("CRON_SECRET")
    supplied_secret = request.headers.get("authorization", "")
    if not expected_secret or not secrets.compare_digest(supplied_secret, f"Bearer {expected_secret}"):
        raise HTTPException(status_code=401, detail="Invalid cron authorization.")
    # Keep publishing isolated from monitoring so one slow social API cannot make
    # the entire daily posting job exceed the serverless execution window.
    return run_growth_automation(actor="vercel-cron", sync_support=False)


@public_api_router.get("/growth-metrics")
def scheduled_growth_metrics(request: Request) -> dict[str, Any]:
    expected_secret = get_env_value("CRON_SECRET")
    supplied_secret = request.headers.get("authorization", "")
    if not expected_secret or not secrets.compare_digest(supplied_secret, f"Bearer {expected_secret}"):
        raise HTTPException(status_code=401, detail="Invalid cron authorization.")
    result: dict[str, Any] = {"post_metrics": sync_growth_post_metrics()}
    try:
        result["youtube_metrics"] = sync_youtube_metrics()
    except Exception as exc:
        result["youtube_metrics"] = {"error": str(exc)[:300]}
    return result


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


def _redirect_with_message(
    message: str,
    message_type: Literal["success", "info", "error"],
    *,
    anchor: str = "",
    query: dict[str, Any] | None = None,
) -> RedirectResponse:
    fragment = f"#{anchor}" if anchor else ""
    extra_query = "".join(f"&{quote_plus(str(key))}={quote_plus(str(value))}" for key, value in (query or {}).items())
    url = f"{PUBLIC_ADMIN_PREFIX}?message={quote_plus(message)}&message_type={message_type}{extra_query}{fragment}"
    return RedirectResponse(url=url, status_code=303)


app.include_router(admin_router, prefix=PUBLIC_ADMIN_PREFIX)
app.include_router(admin_api_router, prefix=PUBLIC_ADMIN_PREFIX)
app.include_router(admin_router, prefix=INTERNAL_ADMIN_PREFIX)
app.include_router(admin_api_router, prefix=INTERNAL_ADMIN_PREFIX)
app.include_router(public_api_router, prefix="/public-api")

from __future__ import annotations

from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Callable

from dotenv import load_dotenv
from passlib.context import CryptContext
from pymongo import ASCENDING, DESCENDING, MongoClient, ReturnDocument
from pymongo.collection import Collection
from pymongo.database import Database
import os

BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "output_md"

load_dotenv(BASE_DIR / ".env")

ADMIN_ROLES = ("superadmin", "editor", "viewer")

CONFIG_DEFAULTS = {
    "grok_api_key": "",
    "grok_model": "grok-2",
    "devto_api_key": "",
    "devto_publish_as_draft": "true",
    "telegram_bot_token": "",
    "telegram_bot_username": "",
    "telegram_notify_chat_id": "",
    "telegram_support_chat_id": "-1003762178712",
    "pro_unlock_code": "PUREHUB-PRO-2026",
    "site_url": "https://hub.blissbiovn.com",
    "ai_provider": "deepseek",
    "groq_api_key": "",
    "groq_model": "llama-3.3-70b-versatile",
    "deepseek_api_key": "",
    "deepseek_model": "deepseek-chat",
    "github_repo": "shoyrulove-dev/PureHub",
    "bluesky_handle": "",
    "bluesky_app_password": "",
    "mastodon_base_url": "",
    "mastodon_access_token": "",
    "release_auto_channels": "telegram,devto,bluesky,mastodon",
    "community_reply_mode": "auto",
    "support_monitor_enabled": "true",
    "opportunity_monitor_enabled": "true",
    "opportunity_keywords": "best offline app,app without ads,privacy first app,open source Android app,offline OCR scanner,QR scanner no ads,simple Pomodoro app,password manager offline,expense tracker offline,unit converter app,habit tracker no ads,note app offline,flashlight app no ads,bubble level app,document scanner offline,wifi analyzer app",
    "opportunity_daily_minimum": "10",
    "opportunity_daily_limit": "30",
    "opportunity_scan_runs_per_day": "4",
    "growth_automation_enabled": "false",
    "growth_auto_publish": "true",
    "growth_campaign_start_date": "",
    "growth_timezone": "Asia/Bangkok",
    "youtube_client_id": "",
    "youtube_client_secret": "",
    "youtube_refresh_token": "",
    "youtube_channel_id": "",
    "youtube_channel_title": "",
    "youtube_default_privacy": "unlisted",
    "tiktok_client_key": "",
    "tiktok_client_secret": "",
    "tiktok_refresh_token": "",
    "tiktok_open_id": "",
    "tiktok_display_name": "",
    "tiktok_environment": "sandbox",
    "reddit_client_id": "",
    "reddit_client_secret": "",
    "reddit_refresh_token": "",
    "reddit_username": "",
    "reddit_default_subreddit": "droidappshowcase",
    "reddit_user_agent": "web:PureHub.CommandCenter:v1.0 (by /u/PureHubAAA)",
}

CURRENT_SCHEMA_VERSION = 20
DEFAULTS_BOOTSTRAP_VERSION = 8
LOGIN_ATTEMPT_WINDOW_MINUTES = 15
LOGIN_MAX_ATTEMPTS = 5
LOGIN_LOCKOUT_MINUTES = 20

MINIAPP_DEFAULTS = [
    {
        "miniapp_id": "lunar-calendar",
        "name": "Lunar Calendar",
        "tab": "Zen & Time",
        "route_en": "/en/lunar-calendar",
        "route_vi": "/vi/lich-am",
        "route_zh": "/zh/nong-li",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Calendar Suite flagship with private solar-lunar navigation.",
    },
    {
        "miniapp_id": "zen-habit",
        "name": "Zen Habit",
        "tab": "Zen & Time",
        "route_en": "/en/zen-habit",
        "route_vi": "/vi/thoi-quen-zen",
        "route_zh": "/zh/chan-xi-guan",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Flagship private habit tracker with weekly insights and local history.",
    },
    {
        "miniapp_id": "zen-pomodoro",
        "name": "Zen Pomodoro",
        "tab": "Zen & Time",
        "route_en": "/en/zen-pomodoro",
        "route_vi": "/vi/pomodoro-zen",
        "route_zh": "/zh/chan-fan-qie-zhong",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Flagship focus timer with local white noise.",
    },
    {
        "miniapp_id": "zen-breath",
        "name": "Zen Breath",
        "tab": "Zen & Time",
        "route_en": "/en/zen-breath",
        "route_vi": "/vi/tho-zen",
        "route_zh": "/zh/chan-hu-xi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Zen Suite flagship with guided pacing, private goals, haptics, and reduced-motion support.",
    },
    {
        "miniapp_id": "compass",
        "name": "Compass",
        "tab": "Measure & Tools",
        "route_en": "/en/compass",
        "route_vi": "/vi/la-ban",
        "route_zh": "/zh/zhinan-zhen",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Sensor Suite flagship: calibrated compass with private live readings.",
    },
    {
        "miniapp_id": "bubble-level",
        "name": "Bubble Level",
        "tab": "Measure & Tools",
        "route_en": "/en/bubble-level",
        "route_vi": "/vi/thuoc-thuy",
        "route_zh": "/zh/shui-ping-yi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Sensor Suite flagship: visual two-axis bubble level.",
    },
    {
        "miniapp_id": "decibel-meter",
        "name": "Decibel Meter",
        "tab": "Measure & Tools",
        "route_en": "/en/decibel-meter",
        "route_vi": "/vi/do-on",
        "route_zh": "/zh/fen-bei-yi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Sensor Suite flagship: private estimated sound-level meter.",
    },
    {
        "miniapp_id": "unit-converter",
        "name": "Unit Converter",
        "tab": "Measure & Tools",
        "route_en": "/en/unit-converter",
        "route_vi": "/vi/doi-don-vi",
        "route_zh": "/zh/dan-wei-huan-suan",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Everyday Tools flagship with multi-category offline conversions and history.",
    },
    {
        "miniapp_id": "smart-flashlight",
        "name": "Smart Flashlight",
        "tab": "Measure & Tools",
        "route_en": "/en/smart-flashlight",
        "route_vi": "/vi/den-pin-thong-minh",
        "route_zh": "/zh/zhi-neng-shou-dian",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Light Suite flagship with dimming, pulse, SOS and capability-safe fallback.",
    },
    {
        "miniapp_id": "qr-studio",
        "name": "QR Studio",
        "tab": "Vision",
        "route_en": "/en/qr-studio",
        "route_vi": "/vi/qr-studio",
        "route_zh": "/zh/er-wei-ma-gong-fang",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Flagship tool to scan and generate QR offline.",
    },
    {
        "miniapp_id": "doc-to-pdf",
        "name": "Doc to PDF",
        "tab": "Vision",
        "route_en": "/en/doc-to-pdf",
        "route_vi": "/vi/tai-lieu-pdf",
        "route_zh": "/zh/wen-dang-zhuan-pdf",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Document Suite flagship: reorder, rotate, frame, and export local PDF pages.",
    },
    {
        "miniapp_id": "ocr-text",
        "name": "OCR Studio",
        "tab": "Vision",
        "route_en": "/en/ocr-text",
        "route_vi": "/vi/trich-xuat-van-ban",
        "route_zh": "/zh/ocr-wen-ben",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Flagship private document scanner, OCR editor, export, and local library.",
    },
    {
        "miniapp_id": "color-grabber",
        "name": "Color Grabber",
        "tab": "Vision",
        "route_en": "/en/color-grabber",
        "route_vi": "/vi/lay-mau",
        "route_zh": "/zh/qu-se-qi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Creative Suite flagship with palette extraction and contrast guidance.",
    },
    {
        "miniapp_id": "speaker-cleaner",
        "name": "Speaker Cleaner",
        "tab": "Security & Audio",
        "route_en": "/en/speaker-cleaner",
        "route_vi": "/vi/lam-sach-loa",
        "route_zh": "/zh/yang-sheng-qi-qing-jie",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Audio Care flagship with timed presets and safe local playback.",
    },
    {
        "miniapp_id": "deep-cleaner",
        "name": "Deep Cleaner",
        "tab": "Security & Audio",
        "route_en": "/en/deep-cleaner",
        "route_vi": "/vi/don-dep-thiet-bi",
        "route_zh": "/zh/shen-du-qing-li",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Storage Care flagship with transparent review and recoverable cleanup guidance.",
    },
    {
        "miniapp_id": "wifi-analyzer",
        "name": "Wi-Fi Analyzer",
        "tab": "Security & Audio",
        "route_en": "/en/wifi-analyzer",
        "route_vi": "/vi/phan-tich-wifi",
        "route_zh": "/zh/wifi-fen-xi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Connection Care flagship with honest browser and Android diagnostics.",
    },
    {
        "miniapp_id": "password-vault",
        "name": "Password Vault",
        "tab": "Security & Audio",
        "route_en": "/en/password-vault",
        "route_vi": "/vi/kho-mat-khau",
        "route_zh": "/zh/mi-ma-bao-xian-ku",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Security Suite flagship with encrypted local storage and timed secret handling.",
    },
    {
        "miniapp_id": "wallpaper-changer",
        "name": "Wallpaper Changer",
        "tab": "Security & Audio",
        "route_en": "/en/wallpaper-changer",
        "route_vi": "/vi/doi-hinh-nen",
        "route_zh": "/zh/bi-zhi-geng-huan",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Creative Suite flagship with local preview, framing and Android apply workflow.",
    },
    {
        "miniapp_id": "bill-splitter",
        "name": "Bill Splitter",
        "tab": "Finance & Community",
        "route_en": "/en/bill-splitter",
        "route_vi": "/vi/chia-hoa-don",
        "route_zh": "/zh/fen-zhang-qi",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Finance Suite flagship for transparent private group settlement.",
    },
    {
        "miniapp_id": "expense-tracker",
        "name": "Expense Tracker",
        "tab": "Finance & Community",
        "route_en": "/en/expense-tracker",
        "route_vi": "/vi/so-chi-tieu",
        "route_zh": "/zh/ji-zhang-ben",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Finance Suite flagship with local ledger, trends, and CSV export.",
    },
    {
        "miniapp_id": "decision-wheel",
        "name": "Decision Wheel",
        "tab": "Finance & Community",
        "route_en": "/en/decision-wheel",
        "route_vi": "/vi/vong-quay-quyet-dinh",
        "route_zh": "/zh/jue-ce-zhuan-pan",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Decision Suite flagship with reusable lists and private result history.",
    },
    {
        "miniapp_id": "community-pro-unlock",
        "name": "PureHub Community",
        "tab": "Finance & Community",
        "route_en": "/en/community-pro-unlock",
        "route_vi": "/vi/mo-khoa-cong-dong",
        "route_zh": "/zh/she-qu-jie-suo",
        "enabled": True,
        "traffic_priority": 10,
        "flagship": True,
        "notes": "Community flagship for support, feedback, roadmap and contribution without feature gating.",
    },
]

API_CATALOG_DEFAULTS = [
    {
        "api_key": "admin_dashboard",
        "method": "GET",
        "path": "/admin",
        "enabled": True,
        "auth_required": True,
        "group": "ui",
        "description": "Primary admin dashboard HTML surface.",
    },
    {
        "api_key": "admin_login",
        "method": "POST",
        "path": "/admin/login",
        "enabled": True,
        "auth_required": False,
        "group": "auth",
        "description": "Admin sign-in endpoint backed by Mongo-stored credentials.",
    },
    {
        "api_key": "admin_logout",
        "method": "POST",
        "path": "/admin/logout",
        "enabled": True,
        "auth_required": True,
        "group": "auth",
        "description": "Clears the admin session cookie.",
    },
    {
        "api_key": "admin_config_save",
        "method": "POST",
        "path": "/admin/config",
        "enabled": True,
        "auth_required": True,
        "group": "config",
        "description": "Updates Grok, Dev.to, Telegram, and site config values.",
    },
    {
        "api_key": "admin_security_save",
        "method": "POST",
        "path": "/admin/security",
        "enabled": True,
        "auth_required": True,
        "group": "auth",
        "description": "Rotates the admin username and password.",
    },
    {
        "api_key": "admin_miniapps_save",
        "method": "POST",
        "path": "/admin/miniapps/{miniapp_id}",
        "enabled": True,
        "auth_required": True,
        "group": "miniapps",
        "description": "Updates a mini-app route, status, and traffic priority.",
    },
    {
        "api_key": "admin_catalog_save",
        "method": "POST",
        "path": "/admin/apis/{api_key}",
        "enabled": True,
        "auth_required": True,
        "group": "catalog",
        "description": "Updates API catalog metadata shown in the admin panel.",
    },
    {
        "api_key": "admin_generate",
        "method": "POST",
        "path": "/admin/actions/generate",
        "enabled": True,
        "auth_required": True,
        "group": "content",
        "description": "Runs the Grok markdown content generator.",
    },
    {
        "api_key": "admin_publish",
        "method": "POST",
        "path": "/admin/actions/publish",
        "enabled": True,
        "auth_required": True,
        "group": "content",
        "description": "Publishes generated articles to Dev.to.",
    },
    {
        "api_key": "admin_bot_start",
        "method": "POST",
        "path": "/admin/actions/bot/start",
        "enabled": True,
        "auth_required": True,
        "group": "telegram",
        "description": "Starts the Telegram community and referral worker.",
    },
    {
        "api_key": "admin_bot_stop",
        "method": "POST",
        "path": "/admin/actions/bot/stop",
        "enabled": True,
        "auth_required": True,
        "group": "telegram",
        "description": "Stops the Telegram community and referral worker.",
    },
    {
        "api_key": "admin_health",
        "method": "GET",
        "path": "/admin/api/health",
        "enabled": True,
        "auth_required": True,
        "group": "system",
        "description": "Basic healthcheck for the admin backend.",
    },
    {
        "api_key": "admin_stats",
        "method": "GET",
        "path": "/admin/api/stats",
        "enabled": True,
        "auth_required": True,
        "group": "dashboard",
        "description": "Aggregate bot, user, and article metrics.",
    },
    {
        "api_key": "admin_articles",
        "method": "GET",
        "path": "/admin/api/articles",
        "enabled": True,
        "auth_required": True,
        "group": "content",
        "description": "List generated and published content jobs.",
    },
    {
        "api_key": "admin_referrers",
        "method": "GET",
        "path": "/admin/api/referrers",
        "enabled": True,
        "auth_required": True,
        "group": "telegram",
        "description": "List referral leaders and reward state.",
    },
    {
        "api_key": "admin_config",
        "method": "GET",
        "path": "/admin/api/config",
        "enabled": True,
        "auth_required": True,
        "group": "config",
        "description": "Return masked runtime config values.",
    },
    {
        "api_key": "admin_miniapps_api",
        "method": "GET",
        "path": "/admin/api/miniapps",
        "enabled": True,
        "auth_required": True,
        "group": "miniapps",
        "description": "Returns the editable mini-app catalog.",
    },
    {
        "api_key": "admin_catalog_api",
        "method": "GET",
        "path": "/admin/api/catalog",
        "enabled": True,
        "auth_required": True,
        "group": "catalog",
        "description": "Returns API catalog metadata for the UI.",
    },
    {
        "api_key": "admin_audit_api",
        "method": "GET",
        "path": "/admin/api/audit-logs",
        "enabled": True,
        "auth_required": True,
        "group": "security",
        "description": "Returns recent audit events for admin actions.",
    },
    {
        "api_key": "admin_schema_api",
        "method": "GET",
        "path": "/admin/api/schema",
        "enabled": True,
        "auth_required": True,
        "group": "system",
        "description": "Returns Mongo schema version and migration history.",
    },
    {
        "api_key": "admin_support_api",
        "method": "GET",
        "path": "/admin/api/support",
        "enabled": True,
        "auth_required": True,
        "group": "support",
        "description": "Returns the unified support inbox, metrics, and channel sync state.",
    },
    {
        "api_key": "admin_support_sync_api",
        "method": "POST",
        "path": "/admin/api/support/sync",
        "enabled": True,
        "auth_required": True,
        "group": "support",
        "description": "Synchronizes DEV, Bluesky, and Mastodon and generates pending AI drafts.",
    },
    {
        "api_key": "support_cron_api",
        "method": "GET",
        "path": "/public-api/support-sync",
        "enabled": True,
        "auth_required": True,
        "group": "support",
        "description": "Runs the protected scheduled support monitor using CRON_SECRET.",
    },
]

PASSWORD_CONTEXT = CryptContext(schemes=["pbkdf2_sha256", "bcrypt"], deprecated="auto")

_CLIENT: MongoClient[Any] | None = None
_INITIALIZED = False


def get_env_value(key: str, default: str = "") -> str:
    return os.getenv(key, default).strip()


def get_client() -> MongoClient[Any]:
    global _CLIENT
    if _CLIENT is None:
        mongo_uri = get_env_value("MONGO_URI")
        if not mongo_uri:
            raise RuntimeError("Missing MONGO_URI in command_center/.env")
        _CLIENT = MongoClient(
            mongo_uri,
            serverSelectionTimeoutMS=5000,
            connectTimeoutMS=5000,
            socketTimeoutMS=10000,
            maxPoolSize=10,
            maxIdleTimeMS=45000,
            appname="purehub-command-center",
        )
    return _CLIENT


def get_database() -> Database[Any]:
    db_name = get_env_value("MONGO_DB_NAME", "purehub_command_center")
    return get_client()[db_name]


def collection(name: str) -> Collection[Any]:
    return get_database()[name]


def utcnow() -> datetime:
    return datetime.now(UTC)


def _normalize_datetime(value: Any) -> datetime | None:
    if not isinstance(value, datetime):
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)


def init_database() -> None:
    global _INITIALIZED
    if _INITIALIZED:
        return

    OUTPUT_DIR.mkdir(exist_ok=True)
    db = get_database()

    db.config.create_index([("key", ASCENDING)], unique=True)
    db.users.create_index([("user_id", ASCENDING)], unique=True)
    db.users.create_index([("referral_code", ASCENDING)], unique=True)
    db.article_jobs.create_index([("created_at", DESCENDING)])
    db.admins.create_index([("username", ASCENDING)], unique=True)
    db.miniapps.create_index([("miniapp_id", ASCENDING)], unique=True)
    db.api_catalog.create_index([("api_key", ASCENDING)], unique=True)
    db.audit_logs.create_index([("created_at", DESCENDING)])
    db.audit_logs.create_index([("actor", ASCENDING), ("created_at", DESCENDING)])
    db.login_guards.create_index([("scope", ASCENDING)], unique=True)
    db.login_guards.create_index([("locked_until", DESCENDING)])
    db.schema_migrations.create_index([("version", ASCENDING)], unique=True)
    db.system_meta.create_index([("key", ASCENDING)], unique=True)
    db.releases.create_index([("version", ASCENDING)], unique=True)
    db.release_publications.create_index(
        [("release_id", ASCENDING), ("channel", ASCENDING), ("language", ASCENDING)],
        unique=True,
    )
    db.release_publications.create_index([("updated_at", DESCENDING)])
    db.distribution_submissions.create_index(
        [("release_id", ASCENDING), ("stage", ASCENDING)],
        unique=True,
    )
    db.distribution_submissions.create_index([("updated_at", DESCENDING)])
    db.support_messages.create_index([("source_key", ASCENDING)], unique=True)
    db.support_messages.create_index([("status", ASCENDING), ("received_at", DESCENDING)])
    db.support_messages.create_index([("platform", ASCENDING), ("received_at", DESCENDING)])
    db.support_messages.create_index([("inbox_type", ASCENDING), ("status", ASCENDING), ("received_at", DESCENDING)])
    db.support_sync_state.create_index([("platform", ASCENDING)], unique=True)
    db.community_metrics.create_index([("platform", ASCENDING)], unique=True)
    db.growth_posts.create_index(
        [("campaign_id", ASCENDING), ("day_number", ASCENDING), ("channel", ASCENDING)],
        unique=True,
    )
    db.growth_posts.create_index([("scheduled_at", DESCENDING)])
    db.growth_posts.create_index([("status", ASCENDING), ("scheduled_at", DESCENDING)])
    db.miniapp_events_daily.create_index(
        [("day", ASCENDING), ("miniapp_id", ASCENDING), ("event", ASCENDING)],
        unique=True,
    )
    db.growth_funnel_daily.create_index(
        [("day", ASCENDING), ("stage", ASCENDING), ("source", ASCENDING), ("campaign", ASCENDING)],
        unique=True,
    )
    db.roadmap_options.create_index([("option_id", ASCENDING)], unique=True)
    db.roadmap_options.create_index([("active", ASCENDING), ("priority", DESCENDING)])

    run_schema_migrations()
    seed_default_documents()
    ensure_admin_account()
    _INITIALIZED = True


def seed_default_documents() -> None:
    state = collection("system_meta").find_one({"key": "defaults_bootstrap"})
    current_version = int(state.get("version", 0)) if state else 0
    if current_version >= DEFAULTS_BOOTSTRAP_VERSION:
        return

    for key, value in CONFIG_DEFAULTS.items():
        collection("config").update_one(
            {"key": key},
            {
                "$setOnInsert": {
                    "key": key,
                    "value": value,
                    "updated_at": utcnow(),
                }
            },
            upsert=True,
        )

    for item in MINIAPP_DEFAULTS:
        collection("miniapps").update_one(
            {"miniapp_id": item["miniapp_id"]},
            {
                "$setOnInsert": {
                    **item,
                    "created_at": utcnow(),
                    "updated_at": utcnow(),
                }
            },
            upsert=True,
        )

    for item in API_CATALOG_DEFAULTS:
        collection("api_catalog").update_one(
            {"api_key": item["api_key"]},
            {
                "$setOnInsert": {
                    **item,
                    "created_at": utcnow(),
                    "updated_at": utcnow(),
                }
            },
            upsert=True,
        )

    collection("system_meta").update_one(
        {"key": "defaults_bootstrap"},
        {"$set": {"version": DEFAULTS_BOOTSTRAP_VERSION, "updated_at": utcnow()}},
        upsert=True,
    )


def ensure_admin_account() -> None:
    username = get_env_value("ADMIN_USERNAME", "admin")
    password = get_env_value("ADMIN_PASSWORD")
    if not password:
        raise RuntimeError("Missing ADMIN_PASSWORD in command_center/.env")

    admins = collection("admins")
    existing = admins.find_one({"username": username})
    if existing:
        return

    admins.insert_one(
        {
            "username": username,
            "password_hash": PASSWORD_CONTEXT.hash(password),
            "role": "superadmin",
            "active": True,
            "created_at": utcnow(),
            "updated_at": utcnow(),
        }
    )


def run_schema_migrations() -> None:
    migrations: list[tuple[int, str, Callable[[], None]]] = [
        (1, "seed-default-admin-and-config", _migration_seed_defaults),
        (2, "ensure-audit-log-indexes", _migration_audit_logs),
        (3, "ensure-schema-catalog-entries", _migration_schema_catalog),
        (4, "ensure-admin-active-flag", _migration_admin_active_flag),
        (5, "ensure-admin-role-values", _migration_admin_roles),
        (6, "ensure-login-guard-collection", _migration_login_guards),
        (7, "ensure-release-hub-collections", _migration_release_hub),
        (8, "ensure-community-support-inbox", _migration_community_support),
        (9, "ensure-community-engagement-metrics", _migration_community_metrics),
        (10, "ensure-growth-autopilot", _migration_growth_autopilot),
        (11, "ensure-product-growth-signals", _migration_product_growth_signals),
        (12, "ensure-privacy-growth-funnel", _migration_privacy_growth_funnel),
        (13, "classify-support-inbox-sources", _migration_support_inbox_sources),
        (14, "promote-ocr-studio-flagship", _migration_promote_ocr_studio),
        (15, "promote-zen-habit-flagship", _migration_promote_zen_habit),
        (16, "promote-utility-suites-flagship", _migration_promote_utility_suites),
        (17, "normalize-flagship-priorities", _migration_normalize_flagship_priorities),
        (18, "promote-complete-catalog-flagship", _migration_promote_complete_catalog),
        (19, "ensure-distribution-tracker", _migration_distribution_tracker),
        (20, "schedule-social-discovery-throughout-day", _migration_social_discovery_schedule),
    ]
    applied_versions = {
        item["version"] for item in collection("schema_migrations").find({}, {"version": 1, "_id": 0})
    }
    for version, name, migration in migrations:
        if version in applied_versions:
            continue
        migration()
        collection("schema_migrations").insert_one(
            {
                "version": version,
                "name": name,
                "applied_at": utcnow(),
            }
        )


def get_schema_status() -> dict[str, Any]:
    rows = collection("schema_migrations").find({}, {"_id": 0}).sort("version", ASCENDING)
    items = [_serialize(item) for item in rows]
    latest = items[-1]["version"] if items else 0
    return {
        "current_version": CURRENT_SCHEMA_VERSION,
        "applied_version": latest,
        "migrations": items,
    }


def _migration_social_discovery_schedule() -> None:
    values = {
        "opportunity_daily_minimum": "10",
        "opportunity_daily_limit": "30",
        "opportunity_scan_runs_per_day": "4",
    }
    for key, value in values.items():
        collection("config").update_one(
            {"key": key},
            {"$set": {"value": value, "updated_at": utcnow()}, "$setOnInsert": {"key": key}},
            upsert=True,
        )


def _migration_product_growth_signals() -> None:
    defaults = (
        ("ocr-workflow", "Improve OCR workflow", "Better crop, rotate, scan history, and text export.", 10),
        ("qr-toolkit", "Expand QR Studio", "Scan history plus Wi-Fi, contact, and batch QR templates.", 9),
        ("money-tools", "Deepen money tools", "Categories, recurring expenses, CSV export, and clearer bill settlement.", 8),
        ("focus-insights", "Add focus insights", "Weekly Pomodoro and habit progress with calm reminders.", 7),
    )
    now = utcnow()
    for option_id, title, description, priority in defaults:
        collection("roadmap_options").update_one(
            {"option_id": option_id},
            {"$setOnInsert": {"title": title, "description": description, "priority": priority, "votes": 0, "active": True, "created_at": now}, "$set": {"updated_at": now}},
            upsert=True,
        )


def _migration_privacy_growth_funnel() -> None:
    collection("growth_funnel_daily").create_index(
        [("day", ASCENDING), ("stage", ASCENDING), ("source", ASCENDING), ("campaign", ASCENDING)],
        unique=True,
    )
    priorities = {"zen-pomodoro": 10, "zen-breath": 9, "qr-studio": 10}
    for miniapp_id, priority in priorities.items():
        collection("miniapps").update_one(
            {"miniapp_id": miniapp_id},
            {"$set": {"traffic_priority": priority, "flagship": True, "updated_at": utcnow()}},
        )


def _migration_support_inbox_sources() -> None:
    messages = collection("support_messages")
    messages.create_index([("inbox_type", ASCENDING), ("status", ASCENDING), ("received_at", DESCENDING)])
    for row in messages.find({"inbox_type": {"$exists": False}}, {"platform": 1, "parent_external_id": 1, "reply_context": 1}):
        messages.update_one(
            {"_id": row["_id"]},
            {"$set": {"inbox_type": infer_support_inbox_type(row), "updated_at": utcnow()}},
        )


def _migration_promote_ocr_studio() -> None:
    now = utcnow()
    collection("miniapps").update_one(
        {"miniapp_id": "ocr-text"},
        {
            "$set": {
                "name": "OCR Studio",
                "flagship": True,
                "traffic_priority": 10,
                "notes": "Flagship private document scanner, OCR editor, export, and local library.",
                "updated_at": now,
            }
        },
    )
    collection("roadmap_options").update_one(
        {"option_id": "ocr-workflow"},
        {"$set": {"active": False, "status": "shipped", "completed_at": now, "updated_at": now}},
    )


def _migration_promote_zen_habit() -> None:
    now = utcnow()
    collection("miniapps").update_one(
        {"miniapp_id": "zen-habit"},
        {
            "$set": {
                "flagship": True,
                "traffic_priority": 10,
                "notes": "Flagship private habit tracker with weekly insights and local history.",
                "updated_at": now,
            }
        },
    )
    collection("roadmap_options").update_one(
        {"option_id": "focus-insights"},
        {"$set": {"active": False, "status": "shipped", "completed_at": now, "updated_at": now}},
    )


def _migration_promote_utility_suites() -> None:
    notes = {
        "speaker-cleaner": "Audio Care flagship with timed presets and safe local playback.",
        "doc-to-pdf": "Document Suite flagship paired with OCR Studio for a private document workflow.",
        "expense-tracker": "Finance Suite flagship with local ledger, trends, and CSV export.",
        "bill-splitter": "Finance Suite flagship for transparent private group settlement.",
        "compass": "Sensor Suite flagship: calibrated compass with private live readings.",
        "bubble-level": "Sensor Suite flagship: visual two-axis bubble level.",
        "decibel-meter": "Sensor Suite flagship: private estimated sound-level meter.",
    }
    for miniapp_id, description in notes.items():
        collection("miniapps").update_one(
            {"miniapp_id": miniapp_id},
            {"$set": {"traffic_priority": 10, "flagship": True, "notes": description, "updated_at": utcnow()}},
        )
    collection("roadmap_options").update_one(
        {"option_id": "money-tools"},
        {"$set": {"active": False, "shipped_at": utcnow(), "updated_at": utcnow()}},
    )


def _migration_normalize_flagship_priorities() -> None:
    now = utcnow()
    collection("miniapps").update_many(
        {"miniapp_id": {"$in": sorted(FLAGSHIP_MINIAPP_IDS)}},
        {"$set": {"traffic_priority": 10, "flagship": True, "updated_at": now}},
    )


def _migration_promote_complete_catalog() -> None:
    now = utcnow()
    collection("miniapps").update_many(
        {"miniapp_id": {"$in": sorted(FLAGSHIP_MINIAPP_IDS)}},
        {"$set": {"flagship": True, "traffic_priority": 10, "updated_at": now}},
    )
    collection("miniapps").update_one(
        {"miniapp_id": "zen-breath"},
        {"$set": {"notes": "Zen Suite flagship with guided pacing, private goals, haptics, and reduced-motion support.", "updated_at": now}},
    )


def _migration_seed_defaults() -> None:
    return None


def _migration_audit_logs() -> None:
    return None


def _migration_schema_catalog() -> None:
    return None


def _migration_admin_active_flag() -> None:
    collection("admins").update_many({"active": {"$exists": False}}, {"$set": {"active": True, "updated_at": utcnow()}})


def _migration_admin_roles() -> None:
    collection("admins").update_many({"role": {"$nin": list(ADMIN_ROLES)}}, {"$set": {"role": "editor", "updated_at": utcnow()}})


def _migration_login_guards() -> None:
    return None


def _migration_release_hub() -> None:
    collection("releases").create_index([("version", ASCENDING)], unique=True)
    collection("release_publications").create_index(
        [("release_id", ASCENDING), ("channel", ASCENDING), ("language", ASCENDING)],
        unique=True,
    )


def _migration_distribution_tracker() -> None:
    collection("distribution_submissions").create_index(
        [("release_id", ASCENDING), ("stage", ASCENDING)],
        unique=True,
    )
    collection("distribution_submissions").create_index([("updated_at", DESCENDING)])


def _migration_community_support() -> None:
    collection("support_messages").create_index([("source_key", ASCENDING)], unique=True)
    collection("support_messages").create_index([("status", ASCENDING), ("received_at", DESCENDING)])
    collection("support_messages").create_index([("platform", ASCENDING), ("received_at", DESCENDING)])
    collection("support_sync_state").create_index([("platform", ASCENDING)], unique=True)


def _migration_community_metrics() -> None:
    collection("community_metrics").create_index([("platform", ASCENDING)], unique=True)


def _migration_growth_autopilot() -> None:
    collection("growth_posts").create_index(
        [("campaign_id", ASCENDING), ("day_number", ASCENDING), ("channel", ASCENDING)],
        unique=True,
    )
    collection("growth_posts").create_index([("scheduled_at", DESCENDING)])
    collection("growth_posts").create_index([("status", ASCENDING), ("scheduled_at", DESCENDING)])


def verify_admin_credentials(username: str, password: str) -> bool:
    admin = collection("admins").find_one({"username": username})
    if not admin:
        return False
    if not admin.get("active", True):
        return False
    return PASSWORD_CONTEXT.verify(password, admin["password_hash"])


def get_admin_profile(username: str) -> dict[str, Any] | None:
    admin = collection("admins").find_one({"username": username}, {"password_hash": 0})
    return _serialize(admin) if admin else None


def list_admin_accounts() -> list[dict[str, Any]]:
    rows = collection("admins").find({}, {"password_hash": 0}).sort([("role", ASCENDING), ("username", ASCENDING)])
    return [_serialize(item) for item in rows]


def create_admin_account(username: str, password: str, role: str, *, active: bool = True) -> None:
    normalized_username = username.strip()
    normalized_role = role.strip()
    if not normalized_username:
        raise ValueError("Admin username cannot be empty.")
    if normalized_role not in ADMIN_ROLES:
        raise ValueError(f"Role must be one of: {', '.join(ADMIN_ROLES)}.")
    if collection("admins").find_one({"username": normalized_username}):
        raise ValueError(f"Admin account {normalized_username} already exists.")

    collection("admins").insert_one(
        {
            "username": normalized_username,
            "password_hash": PASSWORD_CONTEXT.hash(password),
            "role": normalized_role,
            "active": active,
            "created_at": utcnow(),
            "updated_at": utcnow(),
        }
    )


def update_admin_account(
    username: str,
    *,
    role: str,
    active: bool,
    next_password: str | None = None,
) -> None:
    normalized_role = role.strip()
    if normalized_role not in ADMIN_ROLES:
        raise ValueError(f"Role must be one of: {', '.join(ADMIN_ROLES)}.")
    update_payload: dict[str, Any] = {
        "role": normalized_role,
        "active": active,
        "updated_at": utcnow(),
    }
    if next_password:
        update_payload["password_hash"] = PASSWORD_CONTEXT.hash(next_password)
    collection("admins").update_one({"username": username}, {"$set": update_payload})


def delete_admin_account(username: str) -> None:
    if count_superadmins() <= 1:
        target = collection("admins").find_one({"username": username}, {"role": 1})
        if target and target.get("role") == "superadmin":
            raise ValueError("You cannot delete the last superadmin account.")
    collection("admins").delete_one({"username": username})


def count_superadmins() -> int:
    return collection("admins").count_documents({"role": "superadmin", "active": True})


def update_admin_credentials(
    current_username: str,
    *,
    next_username: str,
    current_password: str,
    next_password: str | None = None,
) -> tuple[bool, str]:
    admins = collection("admins")
    admin = admins.find_one({"username": current_username})
    if not admin:
        return False, "Admin account was not found."
    if not PASSWORD_CONTEXT.verify(current_password, admin["password_hash"]):
        return False, "Current password is incorrect."

    normalized_username = next_username.strip()
    if not normalized_username:
        return False, "Admin username cannot be empty."

    conflict = admins.find_one({"username": normalized_username, "_id": {"$ne": admin["_id"]}})
    if conflict:
        return False, "That admin username is already in use."

    update_payload: dict[str, Any] = {
        "username": normalized_username,
        "updated_at": utcnow(),
    }
    if next_password:
        update_payload["password_hash"] = PASSWORD_CONTEXT.hash(next_password)

    admins.update_one({"_id": admin["_id"]}, {"$set": update_payload})
    return True, normalized_username


def list_config() -> dict[str, str]:
    rows = collection("config").find({}, {"_id": 0, "key": 1, "value": 1}).sort("key", ASCENDING)
    return {row["key"]: row["value"] for row in rows}


def get_config_value(key: str, default: str = "") -> str:
    row = collection("config").find_one({"key": key}, {"value": 1, "_id": 0})
    return str(row["value"]) if row else default


def update_config(values: dict[str, str]) -> None:
    now = utcnow()
    for key, value in values.items():
        collection("config").update_one(
            {"key": key},
            {
                "$set": {
                    "value": value,
                    "updated_at": now,
                }
            },
            upsert=True,
        )


def get_user_stats() -> dict[str, int]:
    users = list(collection("users").find({}, {"invites_count": 1, "reward_sent_at": 1}))
    return {
        "total_users": len(users),
        "total_invites": sum(int(item.get("invites_count", 0)) for item in users),
        "rewarded_users": sum(1 for item in users if item.get("reward_sent_at")),
    }


def get_dashboard_metrics() -> dict[str, int]:
    jobs = list(collection("article_jobs").find({}, {"status": 1}))
    return {
        "total_articles": len(jobs),
        "published_articles": sum(1 for item in jobs if item.get("status") == "published"),
        "generated_articles": sum(1 for item in jobs if item.get("status") == "generated"),
        "failed_articles": sum(1 for item in jobs if item.get("status") == "failed"),
    }


def upsert_user(user_id: int, referral_code: str, referred_by: int | None = None) -> dict[str, Any]:
    users = collection("users")
    existing = users.find_one({"user_id": user_id})
    if existing:
        return _serialize(existing)

    payload = {
        "user_id": user_id,
        "invites_count": 0,
        "referral_code": referral_code,
        "referred_by": referred_by,
        "reward_sent_at": None,
        "created_at": utcnow(),
        "updated_at": utcnow(),
    }
    users.insert_one(payload)
    return _serialize(payload)


def get_user(user_id: int) -> dict[str, Any] | None:
    row = collection("users").find_one({"user_id": user_id})
    return _serialize(row) if row else None


def increment_invites(referrer_id: int) -> dict[str, Any] | None:
    row = collection("users").find_one_and_update(
        {"user_id": referrer_id},
        {
            "$inc": {"invites_count": 1},
            "$set": {"updated_at": utcnow()},
        },
        return_document=ReturnDocument.AFTER,
    )
    return _serialize(row) if row else None


def mark_reward_sent(user_id: int) -> None:
    collection("users").update_one(
        {"user_id": user_id},
        {"$set": {"reward_sent_at": utcnow(), "updated_at": utcnow()}},
    )


def list_top_referrers(limit: int = 10) -> list[dict[str, Any]]:
    rows = (
        collection("users")
        .find({}, {"_id": 0})
        .sort([("invites_count", DESCENDING), ("updated_at", DESCENDING)])
        .limit(limit)
    )
    return [_serialize(item) for item in rows]


def create_article_job(source_filename: str, title: str, keyword: str, status: str = "generated") -> str:
    payload = {
        "source_filename": source_filename,
        "title": title,
        "keyword": keyword,
        "status": status,
        "remote_url": None,
        "error_message": None,
        "created_at": utcnow(),
        "updated_at": utcnow(),
    }
    inserted = collection("article_jobs").insert_one(payload)
    return str(inserted.inserted_id)


def update_article_job(
    job_id: str,
    *,
    status: str,
    remote_url: str | None = None,
    error_message: str | None = None,
) -> None:
    from bson import ObjectId

    collection("article_jobs").update_one(
        {"_id": ObjectId(job_id)},
        {
            "$set": {
                "status": status,
                "remote_url": remote_url,
                "error_message": error_message,
                "updated_at": utcnow(),
            }
        },
    )


def list_article_jobs(limit: int = 20) -> list[dict[str, Any]]:
    rows = (
        collection("article_jobs")
        .find({})
        .sort([("updated_at", DESCENDING), ("created_at", DESCENDING)])
        .limit(limit)
    )
    return [_serialize(item) for item in rows]


def list_miniapps(query: str = "", tab: str = "") -> list[dict[str, Any]]:
    filters: dict[str, Any] = {}
    if query.strip():
        filters["$or"] = [
            {"miniapp_id": {"$regex": query.strip(), "$options": "i"}},
            {"name": {"$regex": query.strip(), "$options": "i"}},
            {"notes": {"$regex": query.strip(), "$options": "i"}},
            {"route_en": {"$regex": query.strip(), "$options": "i"}},
            {"route_vi": {"$regex": query.strip(), "$options": "i"}},
            {"route_zh": {"$regex": query.strip(), "$options": "i"}},
        ]
    if tab.strip():
        filters["tab"] = tab.strip()
    rows = collection("miniapps").find(filters, {"_id": 0}).sort([("tab", ASCENDING), ("traffic_priority", DESCENDING)])
    return [_serialize(item) for item in rows]


def update_miniapp(miniapp_id: str, values: dict[str, Any]) -> None:
    values["updated_at"] = utcnow()
    collection("miniapps").update_one({"miniapp_id": miniapp_id}, {"$set": values})


def create_miniapp(values: dict[str, Any]) -> None:
    if collection("miniapps").find_one({"miniapp_id": values["miniapp_id"]}):
        raise ValueError(f"Mini-app {values['miniapp_id']} already exists.")
    payload = {
        **values,
        "created_at": utcnow(),
        "updated_at": utcnow(),
    }
    collection("miniapps").insert_one(payload)


def delete_miniapp(miniapp_id: str) -> None:
    collection("miniapps").delete_one({"miniapp_id": miniapp_id})


def list_api_catalog(query: str = "", group: str = "") -> list[dict[str, Any]]:
    filters: dict[str, Any] = {}
    if query.strip():
        filters["$or"] = [
            {"api_key": {"$regex": query.strip(), "$options": "i"}},
            {"path": {"$regex": query.strip(), "$options": "i"}},
            {"description": {"$regex": query.strip(), "$options": "i"}},
            {"method": {"$regex": query.strip(), "$options": "i"}},
        ]
    if group.strip():
        filters["group"] = group.strip()
    rows = collection("api_catalog").find(filters, {"_id": 0}).sort([("group", ASCENDING), ("path", ASCENDING)])
    return [_serialize(item) for item in rows]


def update_api_catalog(api_key: str, values: dict[str, Any]) -> None:
    values["updated_at"] = utcnow()
    collection("api_catalog").update_one({"api_key": api_key}, {"$set": values})


def create_api_catalog_entry(values: dict[str, Any]) -> None:
    if collection("api_catalog").find_one({"api_key": values["api_key"]}):
        raise ValueError(f"API catalog entry {values['api_key']} already exists.")
    payload = {
        **values,
        "created_at": utcnow(),
        "updated_at": utcnow(),
    }
    collection("api_catalog").insert_one(payload)


def delete_api_catalog_entry(api_key: str) -> None:
    collection("api_catalog").delete_one({"api_key": api_key})


def record_audit_log(
    *,
    actor: str,
    action: str,
    target_type: str,
    target_id: str,
    details: dict[str, Any] | None = None,
    request_meta: dict[str, Any] | None = None,
) -> None:
    collection("audit_logs").insert_one(
        {
            "actor": actor,
            "action": action,
            "target_type": target_type,
            "target_id": target_id,
            "details": details or {},
            "request_meta": request_meta or {},
            "created_at": utcnow(),
        }
    )


def list_audit_logs(limit: int = 50) -> list[dict[str, Any]]:
    rows = collection("audit_logs").find({}).sort("created_at", DESCENDING).limit(limit)
    return [_serialize(item) for item in rows]


def _guard_scope(kind: str, value: str) -> str:
    return f"{kind}:{value.strip().lower()}"


def _load_guard(scope: str) -> dict[str, Any] | None:
    return collection("login_guards").find_one({"scope": scope})


def _is_lock_active(guard: dict[str, Any] | None) -> tuple[bool, int]:
    if not guard:
        return False, 0
    locked_until = _normalize_datetime(guard.get("locked_until"))
    if locked_until is None:
        return False, 0
    now = utcnow()
    if locked_until <= now:
        return False, 0
    remaining_seconds = max(1, int((locked_until - now).total_seconds()))
    return True, remaining_seconds


def _format_lockout_message(remaining_seconds: int) -> str:
    minutes, seconds = divmod(remaining_seconds, 60)
    if minutes:
        return f"Too many login attempts. Try again in {minutes}m {seconds:02d}s."
    return f"Too many login attempts. Try again in {seconds}s."


def get_login_guard_state(*, username: str, ip_address: str) -> dict[str, Any]:
    scopes = [_guard_scope("username", username), _guard_scope("ip", ip_address)]
    highest_remaining = 0
    active_scope = ""
    for scope in scopes:
        locked, remaining = _is_lock_active(_load_guard(scope))
        if locked and remaining > highest_remaining:
            highest_remaining = remaining
            active_scope = scope
    if highest_remaining:
        return {
            "allowed": False,
            "remaining_seconds": highest_remaining,
            "message": _format_lockout_message(highest_remaining),
            "scope": active_scope,
        }
    return {
        "allowed": True,
        "remaining_seconds": 0,
        "message": "",
        "scope": "",
    }


def register_failed_login(*, username: str, ip_address: str) -> dict[str, Any]:
    now = utcnow()
    window_start = now - timedelta(minutes=LOGIN_ATTEMPT_WINDOW_MINUTES)
    scopes = [_guard_scope("username", username), _guard_scope("ip", ip_address)]
    highest_remaining = 0
    lowest_remaining_attempts = LOGIN_MAX_ATTEMPTS

    for scope in scopes:
        guards = collection("login_guards")
        guard = guards.find_one({"scope": scope})
        attempts = []
        if guard:
            attempts = []
            for item in guard.get("attempts", []):
                normalized_item = _normalize_datetime(item)
                if normalized_item is not None and normalized_item >= window_start:
                    attempts.append(normalized_item)
        attempts.append(now)
        lowest_remaining_attempts = min(lowest_remaining_attempts, max(0, LOGIN_MAX_ATTEMPTS - len(attempts)))
        update_payload: dict[str, Any] = {
            "attempts": attempts,
            "updated_at": now,
        }
        locked_until = None
        if len(attempts) >= LOGIN_MAX_ATTEMPTS:
            locked_until = now + timedelta(minutes=LOGIN_LOCKOUT_MINUTES)
            update_payload["locked_until"] = locked_until
        else:
            update_payload["locked_until"] = None

        guards.update_one(
            {"scope": scope},
            {
                "$set": update_payload,
                "$setOnInsert": {"created_at": now},
            },
            upsert=True,
        )
        if locked_until is not None:
            highest_remaining = max(highest_remaining, int((locked_until - now).total_seconds()))

    if highest_remaining:
        return {
            "locked": True,
            "remaining_seconds": highest_remaining,
            "message": _format_lockout_message(highest_remaining),
        }
    return {
        "locked": False,
        "remaining_seconds": 0,
        "message": f"Invalid admin credentials. {lowest_remaining_attempts} attempts remaining before lockout.",
    }


def clear_login_guards(*, username: str, ip_address: str) -> None:
    scopes = [_guard_scope("username", username), _guard_scope("ip", ip_address)]
    collection("login_guards").delete_many({"scope": {"$in": scopes}})


def get_analytics_snapshot() -> dict[str, Any]:
    users = list(collection("users").find({}, {"invites_count": 1, "reward_sent_at": 1, "_id": 0}))
    jobs = list(collection("article_jobs").find({}, {"status": 1, "_id": 0}))
    miniapps = list(collection("miniapps").find({}, {"tab": 1, "enabled": 1, "traffic_priority": 1, "_id": 0}))

    tab_counts: dict[str, dict[str, int]] = {}
    for item in miniapps:
        tab = str(item.get("tab", "Unknown"))
        payload = tab_counts.setdefault(tab, {"total": 0, "enabled": 0, "priority": 0})
        payload["total"] += 1
        payload["priority"] += int(item.get("traffic_priority", 0))
        if item.get("enabled", False):
            payload["enabled"] += 1

    total_articles = len(jobs)
    published_articles = sum(1 for item in jobs if item.get("status") == "published")
    generated_articles = sum(1 for item in jobs if item.get("status") == "generated")
    failed_articles = sum(1 for item in jobs if item.get("status") == "failed")
    conversion_rate = round((published_articles / total_articles) * 100, 1) if total_articles else 0.0

    total_users = len(users)
    total_invites = sum(int(item.get("invites_count", 0)) for item in users)
    rewarded_users = sum(1 for item in users if item.get("reward_sent_at"))
    invite_goal_hits = sum(1 for item in users if int(item.get("invites_count", 0)) >= 3)

    return {
        "content": {
            "total_articles": total_articles,
            "published_articles": published_articles,
            "generated_articles": generated_articles,
            "failed_articles": failed_articles,
            "conversion_rate": conversion_rate,
        },
        "referrals": {
            "total_users": total_users,
            "total_invites": total_invites,
            "rewarded_users": rewarded_users,
            "invite_goal_hits": invite_goal_hits,
            "avg_invites_per_user": round(total_invites / total_users, 2) if total_users else 0.0,
        },
        "miniapps": {
            "total": len(miniapps),
            "enabled": sum(1 for item in miniapps if item.get("enabled", False)),
            "tabs": [
                {
                    "tab": tab,
                    "total": values["total"],
                    "enabled": values["enabled"],
                    "priority": values["priority"],
                }
                for tab, values in sorted(tab_counts.items())
            ],
        },
    }


PUBLIC_MINIAPP_EVENTS = {"open", "helpful", "share", "feedback"}
PUBLIC_FUNNEL_STAGES = {"visit", "download", "first_open", "tester_join", "device_report"}
FLAGSHIP_MINIAPP_IDS = {
    "zen-habit", "zen-pomodoro", "zen-breath", "qr-studio", "ocr-text",
    "speaker-cleaner", "doc-to-pdf", "expense-tracker", "bill-splitter",
    "compass", "bubble-level", "decibel-meter", "lunar-calendar",
    "unit-converter", "smart-flashlight", "color-grabber", "deep-cleaner",
    "wifi-analyzer", "password-vault", "wallpaper-changer", "decision-wheel",
    "community-pro-unlock",
}


def record_miniapp_event(miniapp_id: str, event: str) -> None:
    valid_ids = {str(item["miniapp_id"]) for item in MINIAPP_DEFAULTS}
    if miniapp_id not in valid_ids or event not in PUBLIC_MINIAPP_EVENTS:
        raise ValueError("Unsupported product event.")
    day = utcnow().date().isoformat()
    collection("miniapp_events_daily").update_one(
        {"day": day, "miniapp_id": miniapp_id, "event": event},
        {"$inc": {"count": 1}, "$set": {"updated_at": utcnow()}},
        upsert=True,
    )


def _clean_funnel_dimension(value: str, fallback: str, allowed: set[str]) -> str:
    cleaned = "".join(character for character in value.strip().lower() if character.isalnum() or character in {"-", "_", "."})
    aliases = {"www.google.com": "google", "google.com": "google", "github.com": "github", "t.co": "other"}
    cleaned = aliases.get(cleaned, cleaned)
    return cleaned if cleaned in allowed else fallback


def record_growth_funnel_event(stage: str, source: str = "direct", campaign: str = "none") -> None:
    if stage not in PUBLIC_FUNNEL_STAGES:
        raise ValueError("Unsupported journey stage.")
    day = utcnow().date().isoformat()
    allowed_sources = {"direct", "telegram", "devto", "bluesky", "mastodon", "youtube", "reddit", "github", "tiktok", "google", "facebook", "whatsapp", "zalo", "early-testers", "other"}
    allowed_campaigns = {"none", "august", "community-foundation-30d-v1", *FLAGSHIP_MINIAPP_IDS}
    safe_source = _clean_funnel_dimension(source, "other", allowed_sources)
    safe_campaign = _clean_funnel_dimension(campaign, "none", allowed_campaigns)
    collection("growth_funnel_daily").update_one(
        {"day": day, "stage": stage, "source": safe_source, "campaign": safe_campaign},
        {"$inc": {"count": 1}, "$set": {"updated_at": utcnow()}},
        upsert=True,
    )


def list_roadmap_options() -> list[dict[str, Any]]:
    rows = collection("roadmap_options").find(
        {"active": True},
        {"_id": 0, "option_id": 1, "title": 1, "description": 1, "votes": 1},
    ).sort([("votes", DESCENDING), ("priority", DESCENDING)])
    return [_serialize(row) for row in rows]


def record_roadmap_vote(option_id: str) -> dict[str, Any]:
    row = collection("roadmap_options").find_one_and_update(
        {"option_id": option_id, "active": True},
        {"$inc": {"votes": 1}, "$set": {"updated_at": utcnow()}},
        projection={"_id": 0, "option_id": 1, "title": 1, "description": 1, "votes": 1},
        return_document=True,
    )
    if not row:
        raise ValueError("Roadmap option not found.")
    return _serialize(row)


def get_product_growth_snapshot(days: int = 30) -> dict[str, Any]:
    safe_days = max(1, min(int(days), 90))
    start_day = (utcnow() - timedelta(days=safe_days - 1)).date().isoformat()
    rows = list(collection("miniapp_events_daily").find({"day": {"$gte": start_day}}, {"_id": 0}))
    totals = {event: 0 for event in PUBLIC_MINIAPP_EVENTS}
    tools: dict[str, dict[str, int]] = {}
    for row in rows:
        event = str(row.get("event", ""))
        count = max(0, int(row.get("count", 0) or 0))
        if event in totals:
            totals[event] += count
        tool = tools.setdefault(str(row.get("miniapp_id", "unknown")), {item: 0 for item in PUBLIC_MINIAPP_EVENTS})
        if event in tool:
            tool[event] += count
    top_tools = [
        {"miniapp_id": miniapp_id, **values}
        for miniapp_id, values in sorted(tools.items(), key=lambda item: (-item[1]["open"], item[0]))[:5]
    ]
    roadmap = list_roadmap_options()
    funnel_rows = list(collection("growth_funnel_daily").find({"day": {"$gte": start_day}}, {"_id": 0}))
    funnel = {stage: 0 for stage in PUBLIC_FUNNEL_STAGES}
    sources: dict[str, int] = {}
    for row in funnel_rows:
        stage = str(row.get("stage", ""))
        count = max(0, int(row.get("count", 0) or 0))
        if stage not in funnel:
            continue
        funnel[stage] += count
        if stage == "visit":
            source = str(row.get("source", "direct"))
            sources[source] = sources.get(source, 0) + count
    flagship = [
        {"miniapp_id": miniapp_id, **values}
        for miniapp_id, values in sorted(tools.items(), key=lambda item: (-item[1]["open"], item[0]))
        if miniapp_id in FLAGSHIP_MINIAPP_IDS
    ]
    flagship_start_day = (utcnow() - timedelta(days=13)).date().isoformat()
    flagship_window = {
        miniapp_id: {item: 0 for item in PUBLIC_MINIAPP_EVENTS}
        for miniapp_id in sorted(FLAGSHIP_MINIAPP_IDS)
    }
    for row in rows:
        miniapp_id = str(row.get("miniapp_id", ""))
        event = str(row.get("event", ""))
        if miniapp_id in flagship_window and event in flagship_window[miniapp_id] and str(row.get("day", "")) >= flagship_start_day:
            flagship_window[miniapp_id][event] += max(0, int(row.get("count", 0) or 0))
    return {
        "days": safe_days,
        "totals": totals,
        "helpful_rate": round((totals["helpful"] / totals["open"]) * 100, 1) if totals["open"] else 0.0,
        "top_tools": top_tools,
        "roadmap": roadmap,
        "roadmap_votes": sum(int(item.get("votes", 0) or 0) for item in roadmap),
        "funnel": funnel,
        "funnel_rates": {
            "tool_open": round((totals["open"] / funnel["visit"]) * 100, 1) if funnel["visit"] else 0.0,
            "download": round((funnel["download"] / funnel["visit"]) * 100, 1) if funnel["visit"] else 0.0,
            "first_open": round((funnel["first_open"] / funnel["download"]) * 100, 1) if funnel["download"] else 0.0,
        },
        "top_sources": [
            {"source": source, "visits": visits}
            for source, visits in sorted(sources.items(), key=lambda item: (-item[1], item[0]))[:5]
        ],
        "flagship": flagship,
        "flagship_monitor": {
            "days": 14,
            "start_day": flagship_start_day,
            "tools": [{"miniapp_id": miniapp_id, **values} for miniapp_id, values in flagship_window.items()],
            "selection_rule": "Highest useful-use score: opens + 3x helpful + 2x shares after 14 days.",
        },
    }


def export_control_bundle() -> dict[str, Any]:
    return {
        "exported_at": utcnow().isoformat(),
        "schema": get_schema_status(),
        "config": list_config(),
        "miniapps": list_miniapps(),
        "api_catalog": list_api_catalog(),
    }


def import_control_bundle(bundle: dict[str, Any], mode: str = "merge") -> dict[str, int]:
    imported_config = int(bool(bundle.get("config")))
    miniapps = bundle.get("miniapps", [])
    api_catalog = bundle.get("api_catalog", [])

    if mode == "replace":
        collection("miniapps").delete_many({})
        collection("api_catalog").delete_many({})

    if isinstance(bundle.get("config"), dict):
        update_config({str(key): str(value) for key, value in bundle["config"].items()})

    miniapps_count = 0
    for item in miniapps:
        miniapp_id = str(item["miniapp_id"]).strip()
        payload = {
            "miniapp_id": miniapp_id,
            "name": str(item.get("name", miniapp_id)).strip(),
            "tab": str(item.get("tab", "")).strip(),
            "route_en": str(item.get("route_en", "")).strip(),
            "route_vi": str(item.get("route_vi", "")).strip(),
            "route_zh": str(item.get("route_zh", "")).strip(),
            "traffic_priority": int(item.get("traffic_priority", 0)),
            "flagship": bool(item.get("flagship", False)),
            "notes": str(item.get("notes", "")).strip(),
            "enabled": bool(item.get("enabled", True)),
            "updated_at": utcnow(),
        }
        collection("miniapps").update_one(
            {"miniapp_id": miniapp_id},
            {
                "$set": payload,
                "$setOnInsert": {"created_at": utcnow()},
            },
            upsert=True,
        )
        miniapps_count += 1

    api_count = 0
    for item in api_catalog:
        api_key = str(item["api_key"]).strip()
        payload = {
            "api_key": api_key,
            "method": str(item.get("method", "GET")).strip().upper(),
            "path": str(item.get("path", "")).strip(),
            "enabled": bool(item.get("enabled", True)),
            "auth_required": bool(item.get("auth_required", True)),
            "group": str(item.get("group", "")).strip(),
            "description": str(item.get("description", "")).strip(),
            "updated_at": utcnow(),
        }
        collection("api_catalog").update_one(
            {"api_key": api_key},
            {
                "$set": payload,
                "$setOnInsert": {"created_at": utcnow()},
            },
            upsert=True,
        )
        api_count += 1

    return {
        "config_blocks": imported_config,
        "miniapps": miniapps_count,
        "api_catalog": api_count,
    }


def create_release(
    *,
    version: str,
    title: str,
    summary: str,
    changelog: str,
    github_url: str = "",
    apk_url: str = "",
    aab_url: str = "",
    sha256: str = "",
    prerelease: bool = True,
) -> dict[str, Any]:
    normalized_version = version.strip().lstrip("v")
    if not normalized_version:
        raise ValueError("Release version is required.")
    now = utcnow()
    payload = {
        "release_id": f"v{normalized_version}",
        "version": normalized_version,
        "title": title.strip() or f"PureHub {normalized_version}",
        "summary": summary.strip(),
        "changelog": changelog.strip(),
        "github_url": github_url.strip(),
        "apk_url": apk_url.strip(),
        "aab_url": aab_url.strip(),
        "sha256": sha256.strip(),
        "prerelease": bool(prerelease),
        "status": "draft",
        "created_at": now,
        "updated_at": now,
        "published_at": None,
    }
    collection("releases").insert_one(payload)
    return _serialize(payload)


def update_release(release_id: str, values: dict[str, Any]) -> None:
    payload = {**values, "updated_at": utcnow()}
    if payload.get("status") == "published":
        payload["published_at"] = utcnow()
    collection("releases").update_one({"release_id": release_id}, {"$set": payload})


def get_release(release_id: str) -> dict[str, Any] | None:
    row = collection("releases").find_one({"release_id": release_id})
    return _serialize(row) if row else None


def list_releases(limit: int = 30, *, published_only: bool = False) -> list[dict[str, Any]]:
    filters = {"status": "published"} if published_only else {}
    rows = collection("releases").find(filters).sort("created_at", DESCENDING).limit(limit)
    return [_serialize(item) for item in rows]


def upsert_release_publication(
    *,
    release_id: str,
    channel: str,
    language: str,
    content: str,
    status: str = "draft",
    external_id: str = "",
    external_url: str = "",
    error_message: str = "",
) -> dict[str, Any]:
    now = utcnow()
    key = {"release_id": release_id, "channel": channel, "language": language}
    collection("release_publications").update_one(
        key,
        {
            "$set": {
                **key,
                "content": content,
                "status": status,
                "external_id": external_id,
                "external_url": external_url,
                "error_message": error_message,
                "updated_at": now,
            },
            "$setOnInsert": {"created_at": now, "attempts": 0},
        },
        upsert=True,
    )
    return _serialize(collection("release_publications").find_one(key))


def update_release_publication(release_id: str, channel: str, language: str, values: dict[str, Any]) -> None:
    increments = {"attempts": 1} if values.pop("increment_attempts", False) else {}
    update: dict[str, Any] = {"$set": {**values, "updated_at": utcnow()}}
    if increments:
        update["$inc"] = increments
    collection("release_publications").update_one(
        {"release_id": release_id, "channel": channel, "language": language},
        update,
    )


def claim_release_publication(release_id: str, channel: str, language: str) -> bool:
    result = collection("release_publications").update_one(
        {
            "release_id": release_id,
            "channel": channel,
            "language": language,
            "status": {"$nin": ["published", "publishing"]},
        },
        {"$set": {"status": "publishing", "error_message": "", "updated_at": utcnow()}},
    )
    return result.modified_count == 1


def list_release_publications(release_id: str = "", limit: int = 100) -> list[dict[str, Any]]:
    filters = {"release_id": release_id} if release_id else {}
    rows = collection("release_publications").find(filters).sort("updated_at", DESCENDING).limit(limit)
    return [_serialize(item) for item in rows]


DISTRIBUTION_STAGES = (
    {
        "stage": "github_release",
        "title": "GitHub release",
        "next_action": "Keep the signed F-Droid APK, source tag, checksums, and metadata public.",
        "default_url": "",
    },
    {
        "stage": "izzy_request",
        "title": "IzzyOnDroid request",
        "next_action": "Open one app-inclusion issue in IzzyOnDroid/repodata and paste the prepared dossier.",
        "default_url": "https://codeberg.org/IzzyOnDroid/repodata/issues/new",
    },
    {
        "stage": "izzy_listing",
        "title": "Izzy review & listing",
        "next_action": "Record scanner findings, resolve blockers, and save the public listing URL.",
        "default_url": "https://apt.izzysoft.de/fdroid/index/apk/com.purehub.app",
    },
    {
        "stage": "fdroid_submission",
        "title": "F-Droid submission",
        "next_action": "After Izzy feedback is clear, open an RFP or submit metadata to fdroiddata.",
        "default_url": "https://gitlab.com/fdroid/rfp/-/issues/new",
    },
    {
        "stage": "fdroid_listing",
        "title": "F-Droid review & listing",
        "next_action": "Track the RFP/MR, build logs, reviewer feedback, and final package page.",
        "default_url": "https://f-droid.org/packages/com.purehub.app/",
    },
)

DISTRIBUTION_STATUSES = (
    "pending",
    "ready",
    "submitted",
    "in_review",
    "changes_requested",
    "listed",
    "blocked",
    "not_applicable",
)


def list_distribution_submissions(release_id: str) -> list[dict[str, Any]]:
    normalized_release_id = release_id.strip()
    if not normalized_release_id:
        return []
    release = get_release(normalized_release_id) or {}
    now = utcnow()
    for index, definition in enumerate(DISTRIBUTION_STAGES):
        status = "pending"
        url = definition["default_url"]
        if definition["stage"] == "github_release":
            status = "listed" if release.get("github_url") else "ready"
            url = str(release.get("github_url") or "")
        key = {"release_id": normalized_release_id, "stage": definition["stage"]}
        collection("distribution_submissions").update_one(
            key,
            {
                "$setOnInsert": {
                    **key,
                    "title": definition["title"],
                    "position": index,
                    "status": status,
                    "url": url,
                    "note": "",
                    "next_action": definition["next_action"],
                    "created_at": now,
                    "updated_at": now,
                }
            },
            upsert=True,
        )
    rows = collection("distribution_submissions").find({"release_id": normalized_release_id}).sort("position", ASCENDING)
    return [_serialize(item) for item in rows]


def update_distribution_submission(
    release_id: str,
    stage: str,
    *,
    status: str,
    url: str = "",
    note: str = "",
) -> None:
    if stage not in {item["stage"] for item in DISTRIBUTION_STAGES}:
        raise ValueError("Unknown distribution stage.")
    if status not in DISTRIBUTION_STATUSES:
        raise ValueError("Unknown distribution status.")
    list_distribution_submissions(release_id)
    collection("distribution_submissions").update_one(
        {"release_id": release_id, "stage": stage},
        {"$set": {"status": status, "url": url.strip(), "note": note.strip(), "updated_at": utcnow()}},
    )


SUPPORT_INBOX_TYPES = ("purehub_post", "product_feedback", "direct_support", "social_mention", "social_opportunity")


def infer_support_inbox_type(values: dict[str, Any]) -> str:
    explicit = str(values.get("inbox_type", "")).strip().lower()
    if explicit in SUPPORT_INBOX_TYPES:
        return explicit
    platform = str(values.get("platform", "")).strip().lower()
    context = values.get("reply_context") or {}
    if platform == "pwa":
        return "product_feedback"
    if str(context.get("source_kind", "")).lower() == "discovery":
        return "social_opportunity"
    if platform == "devto":
        return "purehub_post"
    if platform in {"bluesky", "mastodon"}:
        interaction = str(context.get("interaction_kind", "")).lower()
        if interaction in {"reply", "quote", "purehub_post"} or values.get("parent_external_id"):
            return "purehub_post"
        return "social_mention"
    return "direct_support"


def upsert_support_message(values: dict[str, Any]) -> tuple[dict[str, Any], bool]:
    now = utcnow()
    source_key = str(values["source_key"])
    received_at = values.get("received_at") or now
    payload = {
        "source_key": source_key,
        "platform": str(values.get("platform", "")),
        "inbox_type": infer_support_inbox_type(values),
        "external_id": str(values.get("external_id", "")),
        "thread_id": str(values.get("thread_id", "")),
        "parent_external_id": str(values.get("parent_external_id", "")),
        "author_id": str(values.get("author_id", "")),
        "author_name": str(values.get("author_name", "")),
        "author_handle": str(values.get("author_handle", "")),
        "content": str(values.get("content", "")).strip(),
        "source_url": str(values.get("source_url", "")),
        "reply_context": values.get("reply_context", {}),
        "received_at": received_at,
        "created_at": now,
        "updated_at": now,
        "status": "new",
        "category": str(values.get("category", "unclassified")),
        "priority": str(values.get("priority", "normal")),
        "language": str(values.get("language", "en")),
        "requires_reply": bool(values.get("requires_reply", True)),
        "ai_draft": "",
        "reply_text": "",
        "external_reply_id": "",
        "external_reply_url": "",
        "error_message": "",
        "notified_at": None,
    }
    insert_payload = dict(payload)
    # MongoDB rejects an upsert when the same field appears in both $setOnInsert
    # and $set, even if both values match. source_url is refreshed on every sighting.
    insert_payload.pop("source_url")
    result = collection("support_messages").update_one(
        {"source_key": source_key},
        {
            "$setOnInsert": insert_payload,
            "$set": {"last_seen_at": now, "source_url": payload["source_url"]},
        },
        upsert=True,
    )
    row = collection("support_messages").find_one({"source_key": source_key})
    return _serialize(row), result.upserted_id is not None


def get_support_message(message_id: str) -> dict[str, Any] | None:
    from bson import ObjectId

    try:
        row = collection("support_messages").find_one({"_id": ObjectId(message_id)})
    except Exception:
        return None
    return _serialize(row) if row else None


def list_support_messages(
    status: str = "",
    platform: str = "",
    limit: int = 100,
    *,
    statuses: list[str] | tuple[str, ...] | None = None,
    skip: int = 0,
    inbox_filter: str = "all",
) -> list[dict[str, Any]]:
    filters: dict[str, Any] = {}
    if statuses:
        filters["status"] = {"$in": list(statuses)}
    elif status:
        filters["status"] = status
    if platform:
        filters["platform"] = platform
    if inbox_filter == "bugs":
        filters["category"] = {"$in": ["bug", "device_report"]}
    elif inbox_filter in SUPPORT_INBOX_TYPES:
        filters["inbox_type"] = inbox_filter
    rows = (
        collection("support_messages")
        .find(filters)
        .sort("received_at", DESCENDING)
        .skip(max(0, skip))
        .limit(max(1, limit))
    )
    return [_serialize(item) for item in rows]


def count_support_messages(
    *,
    statuses: list[str] | tuple[str, ...] | None = None,
    inbox_filter: str = "all",
) -> int:
    filters: dict[str, Any] = {}
    if statuses:
        filters["status"] = {"$in": list(statuses)}
    if inbox_filter == "bugs":
        filters["category"] = {"$in": ["bug", "device_report"]}
    elif inbox_filter in SUPPORT_INBOX_TYPES:
        filters["inbox_type"] = inbox_filter
    return collection("support_messages").count_documents(filters)


def update_support_message(message_id: str, values: dict[str, Any]) -> None:
    from bson import ObjectId

    collection("support_messages").update_one(
        {"_id": ObjectId(message_id)},
        {"$set": {**values, "updated_at": utcnow()}},
    )


def delete_support_message(message_id: str) -> bool:
    from bson import ObjectId

    try:
        object_id = ObjectId(message_id)
    except Exception:
        return False
    return collection("support_messages").delete_one({"_id": object_id}).deleted_count == 1


def get_support_metrics() -> dict[str, Any]:
    messages = collection("support_messages")
    open_statuses = ["new", "draft_ready", "approved", "failed", "manual_required"]
    now = utcnow()
    day_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    by_platform = {
        platform: messages.count_documents({"platform": platform, "status": {"$in": open_statuses}})
        for platform in ("telegram", "devto", "bluesky", "mastodon")
    }
    by_inbox_type = {
        inbox_type: messages.count_documents({"inbox_type": inbox_type, "status": {"$in": open_statuses}})
        for inbox_type in SUPPORT_INBOX_TYPES
    }
    return {
        "open": messages.count_documents({"status": {"$in": open_statuses}}),
        "new": messages.count_documents({"status": "new"}),
        "draft_ready": messages.count_documents({"status": "draft_ready"}),
        "approved": messages.count_documents({"status": "approved"}),
        "replied": messages.count_documents({"status": "replied"}),
        "replied_month": messages.count_documents({"status": "replied", "replied_at": {"$gte": month_start}}),
        "manual_required": messages.count_documents({"status": "manual_required"}),
        "failed": messages.count_documents({"status": "failed"}),
        "opportunities": messages.count_documents({"category": "opportunity", "status": {"$in": open_statuses}}),
        "social_leads_month": messages.count_documents({"inbox_type": "social_opportunity", "status": {"$ne": "ignored"}, "created_at": {"$gte": month_start}}),
        "social_replied_month": messages.count_documents({"inbox_type": "social_opportunity", "status": "replied", "replied_at": {"$gte": month_start}}),
        "social_leads_today": messages.count_documents({"inbox_type": "social_opportunity", "status": {"$ne": "ignored"}, "created_at": {"$gte": day_start}}),
        "social_replied_today": messages.count_documents({"inbox_type": "social_opportunity", "status": "replied", "replied_at": {"$gte": day_start}}),
        "bugs": messages.count_documents({"category": {"$in": ["bug", "device_report"]}, "status": {"$in": open_statuses}}),
        "by_platform": by_platform,
        "by_inbox_type": by_inbox_type,
    }


def count_social_opportunities(start_at: datetime, end_at: datetime) -> int:
    return collection("support_messages").count_documents(
        {
            "inbox_type": "social_opportunity",
            "status": {"$ne": "ignored"},
            "created_at": {"$gte": start_at, "$lt": end_at},
        }
    )


def get_support_sync_state(platform: str) -> dict[str, Any]:
    return _serialize(collection("support_sync_state").find_one({"platform": platform}))


def update_support_sync_state(platform: str, values: dict[str, Any]) -> None:
    collection("support_sync_state").update_one(
        {"platform": platform},
        {"$set": {"platform": platform, **values, "updated_at": utcnow()}},
        upsert=True,
    )


def list_support_sync_states() -> list[dict[str, Any]]:
    return [_serialize(item) for item in collection("support_sync_state").find({}).sort("platform", ASCENDING)]


def upsert_community_metrics(
    platform: str,
    metrics: dict[str, int] | None = None,
    error_message: str = "",
) -> None:
    values: dict[str, Any] = {
        "platform": platform,
        "error_message": error_message,
        "updated_at": utcnow(),
    }
    if metrics is not None:
        values["metrics"] = {key: max(0, int(value or 0)) for key, value in metrics.items()}
        values["fetched_at"] = utcnow()
    collection("community_metrics").update_one(
        {"platform": platform},
        {"$set": values},
        upsert=True,
    )


def list_community_metrics() -> list[dict[str, Any]]:
    rows = collection("community_metrics").find({}).sort("platform", ASCENDING)
    return [_serialize(item) for item in rows]


def upsert_growth_post(
    *,
    campaign_id: str,
    day_number: int,
    channel: str,
    topic: str,
    content: str,
    status: str = "draft",
    scheduled_at: datetime | None = None,
    metadata: dict[str, Any] | None = None,
) -> dict[str, Any]:
    now = utcnow()
    key = {
        "campaign_id": campaign_id.strip(),
        "day_number": max(1, int(day_number)),
        "channel": channel.strip().lower(),
    }
    collection("growth_posts").update_one(
        key,
        {
            "$setOnInsert": {
                **key,
                "topic": topic.strip(),
                "content": content.strip(),
                "status": status,
                "scheduled_at": scheduled_at or now,
                "metadata": metadata or {},
                "external_id": "",
                "external_url": "",
                "error_message": "",
                "attempts": 0,
                "created_at": now,
                "updated_at": now,
            }
        },
        upsert=True,
    )
    return _serialize(collection("growth_posts").find_one(key))


def get_growth_post(post_id: str) -> dict[str, Any] | None:
    from bson import ObjectId

    try:
        object_id = ObjectId(post_id)
    except Exception:
        return None
    row = collection("growth_posts").find_one({"_id": object_id})
    return _serialize(row) if row else None


def list_growth_posts(limit: int = 100, *, status: str = "", channel: str = "") -> list[dict[str, Any]]:
    filters: dict[str, Any] = {}
    if status:
        filters["status"] = status
    if channel:
        filters["channel"] = channel
    rows = collection("growth_posts").find(filters).sort("scheduled_at", DESCENDING).limit(limit)
    return [_serialize(item) for item in rows]


def update_growth_post(post_id: str, values: dict[str, Any]) -> None:
    from bson import ObjectId

    try:
        object_id = ObjectId(post_id)
    except Exception:
        return
    payload = {**values, "updated_at": utcnow()}
    increments = {"attempts": 1} if payload.pop("increment_attempts", False) else {}
    update: dict[str, Any] = {"$set": payload}
    if increments:
        update["$inc"] = increments
    collection("growth_posts").update_one({"_id": object_id}, update)


def claim_growth_post(post_id: str, channel: str) -> bool:
    from bson import ObjectId

    try:
        object_id = ObjectId(post_id)
    except Exception:
        return False
    result = collection("growth_posts").update_one(
        {
            "_id": object_id,
            "channel": channel,
            "status": {"$nin": ["published", "publishing"]},
        },
        {"$set": {"status": "publishing", "error_message": "", "updated_at": utcnow()}},
    )
    return result.modified_count == 1


def get_growth_summary() -> dict[str, Any]:
    rows = list(collection("growth_posts").find({}, {"status": 1, "channel": 1, "day_number": 1}))
    statuses: dict[str, int] = {}
    channels: dict[str, int] = {}
    for row in rows:
        status = str(row.get("status", "draft"))
        channel = str(row.get("channel", "unknown"))
        statuses[status] = statuses.get(status, 0) + 1
        channels[channel] = channels.get(channel, 0) + 1
    return {
        "total": len(rows),
        "statuses": statuses,
        "channels": channels,
        "latest_day": max((int(row.get("day_number", 0)) for row in rows), default=0),
    }


def _serialize(document: dict[str, Any] | None) -> dict[str, Any]:
    if not document:
        return {}

    result: dict[str, Any] = {}
    for key, value in document.items():
        if key == "_id":
            result["id"] = str(value)
        elif isinstance(value, datetime):
            result[key] = value.isoformat()
        else:
            result[key] = value
    return result

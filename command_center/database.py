from __future__ import annotations

from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Callable

from dotenv import load_dotenv
from passlib.context import CryptContext
from pymongo import ASCENDING, DESCENDING, MongoClient
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
    "pro_unlock_code": "PUREHUB-PRO-2026",
    "site_url": "https://hub.blissbiovn.com",
}

CURRENT_SCHEMA_VERSION = 6
DEFAULTS_BOOTSTRAP_VERSION = 1
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
        "traffic_priority": 9,
        "notes": "High-value SEO page for offline calendar intent.",
    },
    {
        "miniapp_id": "zen-habit",
        "name": "Zen Habit",
        "tab": "Zen & Time",
        "route_en": "/en/zen-habit",
        "route_vi": "/vi/thoi-quen-zen",
        "route_zh": "/zh/chan-xi-guan",
        "enabled": True,
        "traffic_priority": 6,
        "notes": "Habit tracking and streak use case.",
    },
    {
        "miniapp_id": "zen-pomodoro",
        "name": "Zen Pomodoro",
        "tab": "Zen & Time",
        "route_en": "/en/zen-pomodoro",
        "route_vi": "/vi/pomodoro-zen",
        "route_zh": "/zh/chan-fan-qie-zhong",
        "enabled": True,
        "traffic_priority": 7,
        "notes": "Focus timer with local white noise.",
    },
    {
        "miniapp_id": "zen-breath",
        "name": "Zen Breath",
        "tab": "Zen & Time",
        "route_en": "/en/zen-breath",
        "route_vi": "/vi/tho-zen",
        "route_zh": "/zh/chan-hu-xi",
        "enabled": True,
        "traffic_priority": 5,
        "notes": "Breathing and calm UX surface.",
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
        "notes": "Strong organic intent target.",
    },
    {
        "miniapp_id": "bubble-level",
        "name": "Bubble Level",
        "tab": "Measure & Tools",
        "route_en": "/en/bubble-level",
        "route_vi": "/vi/thuoc-thuy",
        "route_zh": "/zh/shui-ping-yi",
        "enabled": True,
        "traffic_priority": 6,
        "notes": "Sensor-based utility tool.",
    },
    {
        "miniapp_id": "decibel-meter",
        "name": "Decibel Meter",
        "tab": "Measure & Tools",
        "route_en": "/en/decibel-meter",
        "route_vi": "/vi/do-on",
        "route_zh": "/zh/fen-bei-yi",
        "enabled": True,
        "traffic_priority": 7,
        "notes": "Microphone-powered measurement tool.",
    },
    {
        "miniapp_id": "unit-converter",
        "name": "Unit Converter",
        "tab": "Measure & Tools",
        "route_en": "/en/unit-converter",
        "route_vi": "/vi/doi-don-vi",
        "route_zh": "/zh/dan-wei-huan-suan",
        "enabled": True,
        "traffic_priority": 8,
        "notes": "High-frequency utility search intent.",
    },
    {
        "miniapp_id": "qr-studio",
        "name": "QR Studio",
        "tab": "Vision",
        "route_en": "/en/qr-studio",
        "route_vi": "/vi/qr-studio",
        "route_zh": "/zh/er-wei-ma-gong-fang",
        "enabled": True,
        "traffic_priority": 9,
        "notes": "Scan and generate QR offline.",
    },
    {
        "miniapp_id": "doc-to-pdf",
        "name": "Doc to PDF",
        "tab": "Vision",
        "route_en": "/en/doc-to-pdf",
        "route_vi": "/vi/tai-lieu-pdf",
        "route_zh": "/zh/wen-dang-zhuan-pdf",
        "enabled": True,
        "traffic_priority": 9,
        "notes": "Document capture to PDF workflow.",
    },
    {
        "miniapp_id": "ocr-text",
        "name": "OCR Text",
        "tab": "Vision",
        "route_en": "/en/ocr-text",
        "route_vi": "/vi/trich-xuat-van-ban",
        "route_zh": "/zh/ocr-wen-ben",
        "enabled": True,
        "traffic_priority": 8,
        "notes": "Image-to-text extractor.",
    },
    {
        "miniapp_id": "color-grabber",
        "name": "Color Grabber",
        "tab": "Vision",
        "route_en": "/en/color-grabber",
        "route_vi": "/vi/lay-mau",
        "route_zh": "/zh/qu-se-qi",
        "enabled": True,
        "traffic_priority": 5,
        "notes": "Visual utility, more niche traffic.",
    },
    {
        "miniapp_id": "speaker-cleaner",
        "name": "Speaker Cleaner",
        "tab": "Security & Audio",
        "route_en": "/en/speaker-cleaner",
        "route_vi": "/vi/lam-sach-loa",
        "route_zh": "/zh/yang-sheng-qi-qing-jie",
        "enabled": True,
        "traffic_priority": 8,
        "notes": "Strong device-fix search intent.",
    },
    {
        "miniapp_id": "password-vault",
        "name": "Password Vault",
        "tab": "Security & Audio",
        "route_en": "/en/password-vault",
        "route_vi": "/vi/kho-mat-khau",
        "route_zh": "/zh/mi-ma-bao-xian-ku",
        "enabled": True,
        "traffic_priority": 7,
        "notes": "Privacy-first storage utility.",
    },
    {
        "miniapp_id": "bill-splitter",
        "name": "Bill Splitter",
        "tab": "Finance & Community",
        "route_en": "/en/bill-splitter",
        "route_vi": "/vi/chia-hoa-don",
        "route_zh": "/zh/fen-zhang-qi",
        "enabled": True,
        "traffic_priority": 8,
        "notes": "Good travel/group expense intent.",
    },
    {
        "miniapp_id": "expense-tracker",
        "name": "Expense Tracker",
        "tab": "Finance & Community",
        "route_en": "/en/expense-tracker",
        "route_vi": "/vi/so-chi-tieu",
        "route_zh": "/zh/ji-zhang-ben",
        "enabled": True,
        "traffic_priority": 8,
        "notes": "Budget and personal finance intent.",
    },
    {
        "miniapp_id": "decision-wheel",
        "name": "Decision Wheel",
        "tab": "Finance & Community",
        "route_en": "/en/decision-wheel",
        "route_vi": "/vi/vong-quay-quyet-dinh",
        "route_zh": "/zh/jue-ce-zhuan-pan",
        "enabled": True,
        "traffic_priority": 4,
        "notes": "Fun utility, lower SEO priority.",
    },
    {
        "miniapp_id": "community-pro-unlock",
        "name": "Community Pro Unlock",
        "tab": "Finance & Community",
        "route_en": "/en/community-pro-unlock",
        "route_vi": "/vi/mo-khoa-cong-dong",
        "route_zh": "/zh/she-qu-jie-suo",
        "enabled": True,
        "traffic_priority": 3,
        "notes": "Growth engine and referral bridge.",
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
        "description": "Starts the Telegram viral loop worker.",
    },
    {
        "api_key": "admin_bot_stop",
        "method": "POST",
        "path": "/admin/actions/bot/stop",
        "enabled": True,
        "auth_required": True,
        "group": "telegram",
        "description": "Stops the Telegram viral loop worker.",
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
        return_document=True,
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

from __future__ import annotations

from datetime import UTC, datetime
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

CURRENT_SCHEMA_VERSION = 3

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


def get_env_value(key: str, default: str = "") -> str:
    return os.getenv(key, default).strip()


def get_client() -> MongoClient[Any]:
    global _CLIENT
    if _CLIENT is None:
        mongo_uri = get_env_value("MONGO_URI")
        if not mongo_uri:
            raise RuntimeError("Missing MONGO_URI in command_center/.env")
        _CLIENT = MongoClient(mongo_uri, serverSelectionTimeoutMS=10000)
    return _CLIENT


def get_database() -> Database[Any]:
    db_name = get_env_value("MONGO_DB_NAME", "purehub_command_center")
    return get_client()[db_name]


def collection(name: str) -> Collection[Any]:
    return get_database()[name]


def utcnow() -> datetime:
    return datetime.now(UTC)


def init_database() -> None:
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
    db.schema_migrations.create_index([("version", ASCENDING)], unique=True)

    for key, value in CONFIG_DEFAULTS.items():
        db.config.update_one(
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
        db.miniapps.update_one(
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
        db.api_catalog.update_one(
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

    run_schema_migrations()
    ensure_admin_account()


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
            "created_at": utcnow(),
            "updated_at": utcnow(),
        }
    )


def run_schema_migrations() -> None:
    migrations: list[tuple[int, str, Callable[[], None]]] = [
        (1, "seed-default-admin-and-config", _migration_seed_defaults),
        (2, "ensure-audit-log-indexes", _migration_audit_logs),
        (3, "ensure-schema-catalog-entries", _migration_schema_catalog),
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


def verify_admin_credentials(username: str, password: str) -> bool:
    admin = collection("admins").find_one({"username": username})
    if not admin:
        return False
    return PASSWORD_CONTEXT.verify(password, admin["password_hash"])


def get_admin_profile(username: str) -> dict[str, Any] | None:
    admin = collection("admins").find_one({"username": username}, {"password_hash": 0})
    return _serialize(admin) if admin else None


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

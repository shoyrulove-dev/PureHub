from __future__ import annotations

import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

BASE_DIR = Path(__file__).resolve().parent
DB_PATH = BASE_DIR / "command_center.db"
OUTPUT_DIR = BASE_DIR / "output_md"

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


def get_connection() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    return connection


@contextmanager
def db_cursor() -> Iterator[sqlite3.Cursor]:
    connection = get_connection()
    try:
        cursor = connection.cursor()
        yield cursor
        connection.commit()
    finally:
        connection.close()


def ensure_column(cursor: sqlite3.Cursor, table_name: str, column_name: str, definition: str) -> None:
    columns = cursor.execute(f"PRAGMA table_info({table_name})").fetchall()
    if column_name not in {column["name"] for column in columns}:
        cursor.execute(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {definition}")


def init_database() -> None:
    OUTPUT_DIR.mkdir(exist_ok=True)

    with db_cursor() as cursor:
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS config (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY,
                invites_count INTEGER NOT NULL DEFAULT 0,
                referral_code TEXT UNIQUE,
                referred_by INTEGER,
                reward_sent_at TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS article_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_filename TEXT NOT NULL,
                title TEXT NOT NULL,
                keyword TEXT,
                status TEXT NOT NULL DEFAULT 'generated',
                remote_url TEXT,
                error_message TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        )

        ensure_column(cursor, "users", "referred_by", "INTEGER")
        ensure_column(cursor, "users", "reward_sent_at", "TEXT")
        ensure_column(cursor, "article_jobs", "remote_url", "TEXT")
        ensure_column(cursor, "article_jobs", "error_message", "TEXT")

        for key, value in CONFIG_DEFAULTS.items():
            cursor.execute(
                """
                INSERT INTO config(key, value)
                VALUES(?, ?)
                ON CONFLICT(key) DO NOTHING
                """,
                (key, value),
            )


def list_config() -> dict[str, str]:
    with db_cursor() as cursor:
        rows = cursor.execute("SELECT key, value FROM config ORDER BY key ASC").fetchall()
    return {row["key"]: row["value"] for row in rows}


def get_config_value(key: str, default: str = "") -> str:
    with db_cursor() as cursor:
        row = cursor.execute("SELECT value FROM config WHERE key = ?", (key,)).fetchone()
    return row["value"] if row else default


def update_config(values: dict[str, str]) -> None:
    with db_cursor() as cursor:
        for key, value in values.items():
            cursor.execute(
                """
                INSERT INTO config(key, value, updated_at)
                VALUES(?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(key) DO UPDATE SET
                    value = excluded.value,
                    updated_at = CURRENT_TIMESTAMP
                """,
                (key, value),
            )


def get_user_stats() -> dict[str, int]:
    with db_cursor() as cursor:
        row = cursor.execute(
            """
            SELECT
                COUNT(*) AS total_users,
                COALESCE(SUM(invites_count), 0) AS total_invites,
                COUNT(CASE WHEN reward_sent_at IS NOT NULL THEN 1 END) AS rewarded_users
            FROM users
            """
        ).fetchone()

    return {
        "total_users": int(row["total_users"]),
        "total_invites": int(row["total_invites"]),
        "rewarded_users": int(row["rewarded_users"]),
    }


def get_dashboard_metrics() -> dict[str, int]:
    with db_cursor() as cursor:
        row = cursor.execute(
            """
            SELECT
                COUNT(*) AS total_articles,
                COUNT(CASE WHEN status = 'published' THEN 1 END) AS published_articles,
                COUNT(CASE WHEN status = 'generated' THEN 1 END) AS generated_articles,
                COUNT(CASE WHEN status = 'failed' THEN 1 END) AS failed_articles
            FROM article_jobs
            """
        ).fetchone()

    return {
        "total_articles": int(row["total_articles"]),
        "published_articles": int(row["published_articles"]),
        "generated_articles": int(row["generated_articles"]),
        "failed_articles": int(row["failed_articles"]),
    }


def upsert_user(user_id: int, referral_code: str, referred_by: int | None = None) -> dict[str, object]:
    with db_cursor() as cursor:
        existing = cursor.execute(
            "SELECT * FROM users WHERE user_id = ?",
            (user_id,),
        ).fetchone()

        if existing:
            return dict(existing)

        cursor.execute(
            """
            INSERT INTO users(user_id, invites_count, referral_code, referred_by, created_at, updated_at)
            VALUES(?, 0, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            (user_id, referral_code, referred_by),
        )

        created = cursor.execute(
            "SELECT * FROM users WHERE user_id = ?",
            (user_id,),
        ).fetchone()
        return dict(created)


def get_user(user_id: int) -> dict[str, object] | None:
    with db_cursor() as cursor:
        row = cursor.execute("SELECT * FROM users WHERE user_id = ?", (user_id,)).fetchone()
    return dict(row) if row else None


def increment_invites(referrer_id: int) -> dict[str, object] | None:
    with db_cursor() as cursor:
        cursor.execute(
            """
            UPDATE users
            SET invites_count = invites_count + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
            """,
            (referrer_id,),
        )
        row = cursor.execute("SELECT * FROM users WHERE user_id = ?", (referrer_id,)).fetchone()
    return dict(row) if row else None


def mark_reward_sent(user_id: int) -> None:
    with db_cursor() as cursor:
        cursor.execute(
            """
            UPDATE users
            SET reward_sent_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
            """,
            (user_id,),
        )


def list_top_referrers(limit: int = 10) -> list[dict[str, object]]:
    with db_cursor() as cursor:
        rows = cursor.execute(
            """
            SELECT user_id, invites_count, referral_code, reward_sent_at, updated_at
            FROM users
            ORDER BY invites_count DESC, updated_at DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [dict(row) for row in rows]


def create_article_job(source_filename: str, title: str, keyword: str, status: str = "generated") -> int:
    with db_cursor() as cursor:
        cursor.execute(
            """
            INSERT INTO article_jobs(source_filename, title, keyword, status, created_at, updated_at)
            VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            (source_filename, title, keyword, status),
        )
        return int(cursor.lastrowid)


def update_article_job(
    job_id: int,
    *,
    status: str,
    remote_url: str | None = None,
    error_message: str | None = None,
) -> None:
    with db_cursor() as cursor:
        cursor.execute(
            """
            UPDATE article_jobs
            SET status = ?,
                remote_url = ?,
                error_message = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (status, remote_url, error_message, job_id),
        )


def list_article_jobs(limit: int = 20) -> list[dict[str, object]]:
    with db_cursor() as cursor:
        rows = cursor.execute(
            """
            SELECT *
            FROM article_jobs
            ORDER BY updated_at DESC, id DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [dict(row) for row in rows]

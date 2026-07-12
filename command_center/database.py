from __future__ import annotations

import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

BASE_DIR = Path(__file__).resolve().parent
DB_PATH = BASE_DIR / "command_center.db"

CONFIG_DEFAULTS = {
    "grok_api_key": "",
    "devto_api_key": "",
    "telegram_bot_token": "",
    "telegram_bot_username": "",
    "pro_unlock_code": "PUREHUB-PRO-2026",
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


def init_database() -> None:
    BASE_DIR.joinpath("output_md").mkdir(exist_ok=True)

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
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        )

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
        rows = cursor.execute(
            "SELECT key, value FROM config ORDER BY key ASC"
        ).fetchall()
    return {row["key"]: row["value"] for row in rows}


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
                COALESCE(SUM(invites_count), 0) AS total_invites
            FROM users
            """
        ).fetchone()

    return {
        "total_users": int(row["total_users"]),
        "total_invites": int(row["total_invites"]),
    }

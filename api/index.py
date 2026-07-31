from __future__ import annotations

from pathlib import Path
import sys
from urllib.parse import parse_qsl, urlencode

ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from command_center.main import app as purehub_app


async def app(scope, receive, send):
    """Restore friendly /admin paths forwarded to this Vercel function."""
    if scope["type"] in {"http", "websocket"}:
        query_items = parse_qsl(scope.get("query_string", b"").decode("utf-8"), keep_blank_values=True)
        forwarded_path = next(
            (value for key, value in query_items if key == "__purehub_path"),
            "",
        )
        if forwarded_path.startswith("/admin"):
            normalized_path = forwarded_path.rstrip("/") or "/admin"
            scope = {
                **scope,
                "path": normalized_path,
                "raw_path": normalized_path.encode("utf-8"),
                "query_string": urlencode(
                    [(key, value) for key, value in query_items if key != "__purehub_path"],
                    doseq=True,
                ).encode("utf-8"),
            }

    await purehub_app(scope, receive, send)

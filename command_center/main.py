from __future__ import annotations

from pathlib import Path
from typing import Literal

from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates

from database import CONFIG_DEFAULTS, get_user_stats, init_database, list_config, update_config

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

app = FastAPI(
    title="PureHub Command Center",
    summary="Admin panel and automation control surface for PureHub growth systems.",
    version="0.1.0",
)


@app.on_event("startup")
def on_startup() -> None:
    init_database()


@app.get("/", response_class=HTMLResponse)
def dashboard(
    request: Request,
    message: str = "",
    message_type: Literal["success", "info"] = "success",
) -> HTMLResponse:
    return templates.TemplateResponse(
        request=request,
        name="index.html",
        context={
            "config": list_config(),
            "defaults": CONFIG_DEFAULTS,
            "stats": get_user_stats(),
            "message": message,
            "message_type": message_type,
        },
    )


@app.post("/config", response_class=HTMLResponse)
def save_config(
    grok_api_key: str = Form(default=""),
    devto_api_key: str = Form(default=""),
    telegram_bot_token: str = Form(default=""),
    telegram_bot_username: str = Form(default=""),
    pro_unlock_code: str = Form(default="PUREHUB-PRO-2026"),
) -> RedirectResponse:
    update_config(
        {
            "grok_api_key": grok_api_key.strip(),
            "devto_api_key": devto_api_key.strip(),
            "telegram_bot_token": telegram_bot_token.strip(),
            "telegram_bot_username": telegram_bot_username.strip().lstrip("@"),
            "pro_unlock_code": pro_unlock_code.strip() or "PUREHUB-PRO-2026",
        }
    )
    return RedirectResponse(
        url="/?message=Configuration+saved+successfully&message_type=success",
        status_code=303,
    )


@app.post("/actions/{action_name}")
def trigger_action(action_name: str) -> RedirectResponse:
    action_messages = {
        "generate": "Content generator endpoint is ready for Phase 2 wiring.",
        "publish": "Dev.to publisher endpoint is ready for Phase 2 wiring.",
        "bot": "Telegram bot worker endpoint is ready for Phase 2 wiring.",
    }
    message = action_messages.get(
        action_name,
        "Unknown action requested. Nothing was triggered.",
    )
    return RedirectResponse(
        url=f"/?message={message.replace(' ', '+')}&message_type=info",
        status_code=303,
    )


@app.get("/health")
def healthcheck() -> dict[str, str]:
    return {"status": "ok", "service": "purehub-command-center"}

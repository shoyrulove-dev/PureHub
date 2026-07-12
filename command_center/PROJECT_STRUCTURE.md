# PureHub Command Center

```text
command_center/
|-- .env.example
|-- database.py
|-- main.py
|-- content_generator.py
|-- devto_publisher.py
|-- telegram_bot_worker.py
|-- requirements.txt
|-- PROJECT_STRUCTURE.md
|-- output_md/
|   `-- .gitkeep
`-- templates/
    |-- index.html
    `-- login.html
```

## Current Scope

- `database.py`
  MongoDB bootstrap, config seeding, admin account hashing, mini-app catalog, API catalog, referral data, and article job persistence.
- `main.py`
  FastAPI server, admin session auth, `/admin` dashboard, `/admin/api/*` JSON endpoints, config updates, security rotation, and manual automation triggers.
- `content_generator.py`
  Grok-powered markdown generation that writes to `output_md/` and records article jobs.
- `devto_publisher.py`
  Dev.to publishing pipeline that reads generated files and updates article job status.
- `telegram_bot_worker.py`
  Background Telegram viral loop manager with invite tracking and Pro unlock reward logic.
- `templates/index.html`
  Tailwind admin dashboard for credentials, mini-app controls, API exposure, referrals, and jobs.
- `templates/login.html`
  Admin sign-in screen for the control center.

# PureHub Command Center

```text
command_center/
├── database.py
├── main.py
├── requirements.txt
├── output_md/
│   └── .gitkeep
└── templates/
    └── index.html
```

## Phase 1 Scope

- `database.py`: SQLite setup for `config` and `users`
- `main.py`: FastAPI app, admin dashboard routes, config CRUD, manual trigger endpoints
- `templates/index.html`: Jinja2 admin UI
- `output_md/`: local article output folder for Phase 2 generator/publisher flow

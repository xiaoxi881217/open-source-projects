# Reading Room MCP

A self-hosted FastAPI service that provides a small reading-room web UI and MCP tools for listing books, continuing a reading session, and recording reflections.

## Privacy model

This repository intentionally contains no books, uploads, covers, user notes, credentials, environment files, deployment history, or production configuration. Runtime content is stored locally under `data/`, `uploads/`, and `covers/`; all three paths are ignored by Git.

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn server:app --host 127.0.0.1 --port 19200
```

On Windows PowerShell, activate the virtual environment with `.\.venv\Scripts\Activate.ps1`.

Open `http://127.0.0.1:19200/` for the UI. The MCP endpoint is available at `/mcp`.

## Add your own content

Create `data/books.json` and place any imported text files in `uploads/`. The service creates missing runtime directories automatically. Do not commit those directories if they contain private reading material.

## Deployment

Run the service behind a reverse proxy that forwards `/mcp/` to its local port. Keep the service bound to loopback unless you intentionally need a public endpoint, and add authentication at the proxy layer for internet-facing deployments.

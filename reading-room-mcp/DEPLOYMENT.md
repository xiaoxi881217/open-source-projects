# Deployment

Run the application behind a reverse proxy and keep the Python service on a loopback address.

```ini
[Service]
WorkingDirectory=/srv/reading-room-mcp
ExecStart=/srv/reading-room-mcp/.venv/bin/uvicorn server:app --host 127.0.0.1 --port 19200
Restart=always
```

Proxy the public MCP path to `http://127.0.0.1:19200`. Configure authentication and TLS in your own reverse proxy. Do not commit runtime content, credentials, or proxy configuration.

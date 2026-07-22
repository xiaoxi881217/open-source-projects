#!/usr/bin/env python3
import hashlib
import json
import mimetypes
import os
import re
import shutil
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from fastapi.responses import FileResponse, JSONResponse, Response
from fastapi.staticfiles import StaticFiles


BASE = Path(__file__).parent
DATA_DIR = BASE / "data"
UPLOAD_DIR = BASE / "uploads"
COVER_DIR = BASE / "covers"
STATIC_DIR = BASE / "static"
BOOKS_PATH = DATA_DIR / "books.json"
INDEX_PATH = STATIC_DIR / "reading-index.html"
RECENT_PLOT_LIMIT = 10
RECENT_COMMENT_LIMIT = 6
MAX_BOOK_OVERVIEW_CHARS = 6000

for folder in (DATA_DIR, UPLOAD_DIR, COVER_DIR, STATIC_DIR):
    folder.mkdir(parents=True, exist_ok=True)

app = FastAPI(title="Reading Room MCP")
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
app.mount("/covers", StaticFiles(directory=str(COVER_DIR)), name="covers")

MCP_TOOLS = [
    {
        "name": "reading_list_books",
        "description": "列出阅读空间中的书籍，返回 book_id、标题、进度与读书笔记数量。",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
    {
        "name": "reading_continue",
        "description": "继续阅读一本书。返回当前正文 text 与有明确预算的 readingContext（长期总览、最近剧情和最近感受）。",
        "inputSchema": {
            "type": "object",
            "properties": {
                "book_id": {"type": "string", "description": "书本 id"},
                "offset": {"type": "integer", "description": "可选，从指定位置继续读"},
                "chars": {"type": "integer", "description": "读取字数，默认 1800"},
                "commentLimit": {"type": "integer", "description": "带回多少条旧评论，默认 12"},
            },
            "required": ["book_id"],
            "additionalProperties": False,
        },
    },
    {
        "name": "reading_reflect",
        "description": "读完一段后，把段落感想写回阅读空间，并推进阅读进度。",
        "inputSchema": {
            "type": "object",
            "properties": {
                "book_id": {"type": "string", "description": "书本 id"},
                "offset": {"type": "integer", "description": "本段开始位置"},
                "nextOffset": {"type": "integer", "description": "读完后推进到的位置"},
                "comment": {"type": "string", "description": "对这一段的感想"},
                "summary": {"type": "string", "description": "可选，目前阅读总结"},
            },
            "required": ["book_id", "comment"],
            "additionalProperties": False,
        },
    },
    {
        "name": "reading_history",
        "description": "查看阅读空间中留下过的段落评论历史。",
        "inputSchema": {
            "type": "object",
            "properties": {"limit": {"type": "integer", "description": "返回条数，默认 30"}},
            "additionalProperties": False,
        },
    },
]

MCP_TOOLS.append({
    "name": "reading_summaries",
    "description": "Read accumulated concise plot summaries for a book, in reading order.",
    "inputSchema": {
        "type": "object",
        "properties": {"book_id": {"type": "string", "description": "Book id."}},
        "required": ["book_id"],
        "additionalProperties": False,
    },
})

for tool in MCP_TOOLS:
    if tool.get("name") == "reading_reflect":
        props = tool.setdefault("inputSchema", {}).setdefault("properties", {})
        props.setdefault("segmentSummary", {
            "type": "string",
            "description": "Concise plot summary for only this newly read segment, about 80-160 Chinese characters.",
        })
        props.setdefault("longTermOverview", {
            "type": "string",
            "description": "Optional rolling whole-book overview, capped at 6000 Chinese characters. Preserve major relationships, setting, unresolved conflicts, and current story state.",
        })


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def safe_name(name: str) -> str:
    name = Path(name or "book").name
    stem = re.sub(r"[^A-Za-z0-9_.\-\u4e00-\u9fff]+", "_", name).strip("._")
    return (stem or "book")[:100]


def read_books() -> list[dict]:
    if not BOOKS_PATH.exists():
        return []
    try:
        raw = json.loads(BOOKS_PATH.read_text("utf-8"))
    except Exception:
        return []
    books = raw.get("books") if isinstance(raw, dict) else raw
    if not isinstance(books, list):
        return []
    return [normalize_book(b) for b in books if isinstance(b, dict)]


def write_books(books: list[dict]) -> None:
    BOOKS_PATH.write_text(json.dumps({"books": books}, ensure_ascii=False, indent=2), "utf-8")


def normalize_book(book: dict) -> dict:
    book_id = str(book.get("id") or "").strip()
    path = str(book.get("path") or book.get("file") or "").strip()
    if not book_id:
        seed = path or book.get("title") or str(time.time())
        book_id = hashlib.sha1(str(seed).encode("utf-8")).hexdigest()[:16]
    title = str(book.get("title") or Path(path).stem or "未命名书籍").strip()
    length = int(book.get("length") or 0)
    if path:
        file_path = BASE / path if not Path(path).is_absolute() else Path(path)
        if file_path.exists():
            try:
                length = len(file_path.read_text("utf-8", errors="ignore"))
            except Exception:
                pass
    comments = book.get("comments") if isinstance(book.get("comments"), list) else []
    summaries = book.get("summaries") if isinstance(book.get("summaries"), list) else []
    return {
        **book,
        "id": book_id,
        "title": title,
        "author": str(book.get("author") or "未知作者"),
        "path": path,
        "offset": max(0, int(book.get("offset") or 0)),
        "length": max(0, length),
        "note": str(book.get("note") or ""),
        "summary": str(book.get("summary") or ""),
        "longTermOverview": str(book.get("longTermOverview") or "")[:MAX_BOOK_OVERVIEW_CHARS],
        "phaseSummaries": [s for s in (book.get("phaseSummaries") or []) if isinstance(s, dict)],
        "cover": str(book.get("cover") or ""),
        "comments": [c for c in comments if isinstance(c, dict)],
        "summaries": [s for s in summaries if isinstance(s, dict)],
        "createdAt": str(book.get("createdAt") or now_iso()),
        "updatedAt": str(book.get("updatedAt") or book.get("createdAt") or now_iso()),
    }


def find_book(book_id: str) -> tuple[list[dict], dict]:
    books = read_books()
    for book in books:
        if book["id"] == book_id:
            return books, book
    raise HTTPException(status_code=404, detail="book not found")


def book_text(book: dict) -> str:
    path = book.get("path") or ""
    file_path = BASE / path if path and not Path(path).is_absolute() else Path(path)
    if not path or not file_path.exists():
        raise HTTPException(status_code=404, detail="book file not found")
    return file_path.read_text("utf-8", errors="ignore")


def public_book(book: dict) -> dict:
    item = normalize_book(book)
    item["percent"] = round((item["offset"] / item["length"] * 100), 2) if item["length"] else 0
    item["commentCount"] = len(item.get("comments") or [])
    item["summaryCount"] = len(unique_summaries(item))
    return item


def save_book_file(upload: UploadFile, data: bytes) -> tuple[str, str]:
    original = safe_name(upload.filename or "book.txt")
    ext = Path(original).suffix.lower() or ".txt"
    digest = hashlib.sha1(data + str(time.time()).encode()).hexdigest()[:16]
    stored = f"{digest}{ext}"
    path = UPLOAD_DIR / stored
    path.write_bytes(data)
    title = Path(original).stem or "未命名书籍"
    return str(path.relative_to(BASE)).replace("\\", "/"), title


def split_paragraphs(text: str) -> list[str]:
    chunks = [p.strip() for p in re.split(r"\n\s*\n+", text) if p.strip()]
    if len(chunks) <= 1:
        chunks = [p.strip() for p in re.split(r"(?<=[。！？!?])\s*", text) if p.strip()]
    return chunks


def surrounding_page(text: str, offset: int, chars: int) -> tuple[int, str]:
    offset = max(0, min(len(text), offset))
    end = max(offset, min(len(text), offset + max(400, min(chars, 9000))))
    return offset, text[offset:end]


CHAPTER_RE = re.compile(
    r"^\s*((?:第[零〇一二两三四五六七八九十百千万\d]+[章节回卷部篇])|(?:chapter\s+\d+)|(?:序章|楔子|引子|前言|番外|尾声|后记))[\s:：、.\-]*(.{0,80})\s*$",
    re.IGNORECASE,
)


def book_chapters(text: str) -> list[dict]:
    lines = text.splitlines(True)
    chapters = []
    cursor = 0
    for idx, line in enumerate(lines):
        plain = line.strip()
        prev_plain = lines[idx - 1].strip() if idx > 0 else ""
        next_plain = lines[idx + 1].strip() if idx + 1 < len(lines) else ""
        if plain and len(plain) <= 60 and CHAPTER_RE.match(plain):
            title = re.sub(r"\s+", " ", plain)[:100]
            has_gap = (not prev_plain) or (not next_plain)
            looks_like_heading = len(plain) <= 32 or "第" in plain[:2] or plain.lower().startswith("chapter")
            far_enough = (not chapters) or (cursor - int(chapters[-1]["offset"]) >= 240)
            not_repeat = (not chapters) or (chapters[-1]["offset"] != cursor and chapters[-1]["title"] != title)
            if next_plain and not_repeat and far_enough and (has_gap or looks_like_heading):
                chapters.append({"title": title, "offset": cursor})
        cursor += len(line)
    if not chapters:
        chapters = [{"title": "正文", "offset": 0}]
    elif chapters[0]["offset"] > 0:
        chapters.insert(0, {"title": "开篇", "offset": 0})
    return chapters[:500]


def chapter_index_for_offset(chapters: list[dict], offset: int) -> int:
    idx = 0
    for i, item in enumerate(chapters or []):
        try:
            if int(item.get("offset") or 0) <= offset:
                idx = i
        except Exception:
            pass
    return idx


def auto_comment(text: str, offset: int) -> str:
    page = text[offset:offset + 1200].strip()
    paragraphs = split_paragraphs(page)
    sample = paragraphs[0] if paragraphs else page[:160]
    sample = re.sub(r"\s+", " ", sample)[:90]
    if not sample:
        return "阅读在这里暂停，等待下一段内容。"
    return f"阅读暂停于此：{sample}……这段内容会留给下次继续阅读。"


def append_comment(book: dict, text: str, offset: int, comment: str) -> dict:
    offset = max(0, min(len(text), int(offset or 0)))
    page = text[offset:offset + 500].strip()
    item = {
        "id": hashlib.sha1((book["id"] + str(offset) + comment + now_iso()).encode()).hexdigest()[:14],
        "offset": offset,
        "percent": round((offset / len(text) * 100), 2) if text else 0,
        "quote": re.sub(r"\s+", " ", page)[:180],
        "comment": comment,
        "createdAt": now_iso(),
    }
    comments = book.get("comments") if isinstance(book.get("comments"), list) else []
    comments.append(item)
    book["comments"] = comments[-300:]
    book["offset"] = offset
    book["note"] = comment
    book["updatedAt"] = now_iso()
    return item


def append_segment_summary(book: dict, text: str, offset: int, next_offset: int, summary: str) -> dict:
    offset = max(0, min(len(text), int(offset or 0)))
    next_offset = max(offset, min(len(text), int(next_offset or offset)))
    summary = re.sub(r"\s+", " ", str(summary or "")).strip()
    stable_key = summary_key(book["id"], offset, next_offset)
    # A reading segment is identified by its book and covered range, rather
    # than by its generated wording or save time. Retrying reading_reflect must
    # be idempotent; otherwise the same range is rendered as duplicate cards.
    for existing in book.get("summaries") or []:
        if isinstance(existing, dict) and str(existing.get("key") or summary_key(book["id"], existing.get("offset"), existing.get("nextOffset"))) == stable_key:
            return existing
    item = {
        "id": hashlib.sha1(stable_key.encode()).hexdigest()[:14],
        "key": stable_key,
        "offset": offset,
        "nextOffset": next_offset,
        "percent": round((next_offset / len(text) * 100), 2) if text else 0,
        "summary": summary,
        "createdAt": now_iso(),
    }
    summaries = book.get("summaries") if isinstance(book.get("summaries"), list) else []
    summaries.append(item)
    book["summaries"] = summaries[-800:]
    book["summary"] = summary
    book["updatedAt"] = now_iso()
    return item


def summary_key(book_id: str, offset, next_offset) -> str:
    return f"{book_id}:{int(offset or 0)}:{int(next_offset or 0)}"


def unique_summaries(book: dict) -> list[dict]:
    """Return historical summaries in reading order, one row per text range."""
    seen = set()
    rows = []
    for item in sorted((s for s in (book.get("summaries") or []) if isinstance(s, dict)), key=lambda x: (int(x.get("offset") or 0), int(x.get("nextOffset") or 0), str(x.get("createdAt") or ""))):
        key = str(item.get("key") or summary_key(book["id"], item.get("offset"), item.get("nextOffset")))
        if key in seen:
            continue
        seen.add(key)
        rows.append({**item, "key": key})
    return rows


def estimate_tokens(value) -> int:
    """A transparent, conservative mixed Chinese/ASCII token estimate."""
    text = json.dumps(value, ensure_ascii=False) if not isinstance(value, str) else value
    ascii_run = 0
    tokens = 0
    for char in text:
        if ord(char) < 128 and not char.isspace():
            ascii_run += 1
            continue
        if ascii_run:
            tokens += (ascii_run + 3) // 4
            ascii_run = 0
        if not char.isspace():
            tokens += 1
    return tokens + ((ascii_run + 3) // 4)


def reading_context(book: dict, page: str, offset: int) -> dict:
    summaries = unique_summaries(book)
    recent_plots = summaries[-RECENT_PLOT_LIMIT:]
    recent_feelings = comments_before(book, offset, RECENT_COMMENT_LIMIT)
    overview = str(book.get("longTermOverview") or "")[:MAX_BOOK_OVERVIEW_CHARS]
    phases = [p for p in (book.get("phaseSummaries") or []) if isinstance(p, dict)]
    context = {
        "longTermOverview": overview,
        "relevantPhaseSummaries": phases[-3:],
        "recentPlots": recent_plots,
        "recentFeelings": recent_feelings,
    }
    return {
        **context,
        "tokenBudget": {
            "currentText": estimate_tokens(page),
            "longTermOverview": estimate_tokens(overview),
            "recentPlots": estimate_tokens(recent_plots),
            "recentFeelings": estimate_tokens(recent_feelings),
            "phaseSummaries": estimate_tokens(context["relevantPhaseSummaries"]),
            "conversationHistory": 0,
            "toolResult": estimate_tokens({**context, "currentText": page}),
        },
    }


def comments_before(book: dict, offset: int, limit: int = 12) -> list[dict]:
    rows = []
    for item in book.get("comments") or []:
        try:
            if int(item.get("offset") or 0) <= offset:
                rows.append(item)
        except Exception:
            pass
    rows.sort(key=lambda x: int(x.get("offset") or 0))
    return rows[-max(1, min(limit, 50)):]


def mcp_text(payload) -> dict:
    if not isinstance(payload, str):
        payload = json.dumps(payload, ensure_ascii=False, indent=2)
    return {"content": [{"type": "text", "text": payload}]}


def mcp_error(req_id, code: int, message: str) -> dict:
    return {"jsonrpc": "2.0", "id": req_id, "error": {"code": code, "message": message}}


def mcp_read_payload(book_id: str, args: dict) -> dict:
    _, book = find_book(book_id)
    text = book_text(book)
    offset = int(args.get("offset") if args.get("offset") is not None else book.get("offset") or 0)
    chars = int(args.get("chars") or 1800)
    offset, page = surrounding_page(text, offset, chars)
    next_offset = min(len(text), offset + len(page))
    context = reading_context(book, page, offset)
    return {
        "book": public_book(book),
        "offset": offset,
        "nextOffset": next_offset,
        "length": len(text),
        "percent": round((offset / len(text) * 100), 2) if text else 0,
        "readingContext": context,
        "text": page,
    }


def mcp_reflect_payload(book_id: str, args: dict) -> dict:
    books, book = find_book(book_id)
    text = book_text(book)
    offset = int(args.get("offset") if args.get("offset") is not None else book.get("offset") or 0)
    next_offset = max(offset, min(len(text), int(args.get("nextOffset") or offset)))
    comment = str(args.get("comment") or "").strip()
    if not comment:
        raise HTTPException(status_code=400, detail="comment is required")
    item = append_comment(book, text, offset, comment)
    segment_summary = str(args.get("segmentSummary") or args.get("summary") or "").strip()
    summary_item = append_segment_summary(book, text, offset, next_offset, segment_summary) if segment_summary else None
    overview = args.get("longTermOverview")
    if isinstance(overview, str) and overview.strip():
        book["longTermOverview"] = re.sub(r"\s+", " ", overview).strip()[:MAX_BOOK_OVERVIEW_CHARS]
    book["offset"] = next_offset
    book["updatedAt"] = now_iso()
    write_books(books)
    return {"ok": True, "comment": item, "segmentSummary": summary_item, "book": public_book(book)}


def call_mcp_tool(name: str, args: dict) -> dict:
    args = args if isinstance(args, dict) else {}
    if name == "reading_list_books":
        return mcp_text({"books": [public_book(b) for b in read_books()]})
    if name == "reading_continue":
        return mcp_text(mcp_read_payload(str(args.get("book_id") or ""), args))
    if name == "reading_reflect":
        return mcp_text(mcp_reflect_payload(str(args.get("book_id") or ""), args))
    if name == "reading_summaries":
        _, book = find_book(str(args.get("book_id") or ""))
        return mcp_text({"book": public_book(book), "summaries": unique_summaries(book)})
    if name == "reading_history":
        limit = max(1, min(int(args.get("limit") or 30), 200))
        rows = []
        for book in read_books():
            for comment in book.get("comments") or []:
                rows.append({"bookId": book["id"], "bookTitle": book["title"], **comment})
        rows.sort(key=lambda x: x.get("createdAt", ""), reverse=True)
        return mcp_text({"history": rows[:limit]})
    raise HTTPException(status_code=404, detail=f"unknown tool: {name}")


def handle_mcp_request(message: dict) -> dict | None:
    method = str(message.get("method") or "")
    req_id = message.get("id")
    params = message.get("params") if isinstance(message.get("params"), dict) else {}
    if method.startswith("notifications/"):
        return None
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": params.get("protocolVersion") or "2024-11-05",
                "capabilities": {"tools": {"listChanged": False}, "resources": {}, "prompts": {}},
                "serverInfo": {"name": "reading-room-mcp", "version": "1.0.0"},
        "instructions": "For normal reading, call reading_continue only. It returns the current text once plus bounded readingContext (longTermOverview, recentPlots, and at most 6 recentFeelings). Do not call reading_summaries unless the user explicitly asks for historical records. After reading, save a feeling comment and a concise segmentSummary for only the new text; update longTermOverview when the story state materially changes.",
            },
        }
    if method == "ping":
        return {"jsonrpc": "2.0", "id": req_id, "result": {}}
    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": req_id, "result": {"tools": MCP_TOOLS}}
    if method == "tools/call":
        try:
            result = call_mcp_tool(str(params.get("name") or ""), params.get("arguments") or {})
            return {"jsonrpc": "2.0", "id": req_id, "result": result}
        except HTTPException as exc:
            return mcp_error(req_id, -32000, str(exc.detail))
        except Exception as exc:
            return mcp_error(req_id, -32603, str(exc))
    return mcp_error(req_id, -32601, f"method not found: {method}")


@app.get("/health")
async def health():
    return {"ok": True, "service": "reading-room-mcp", "books": len(read_books())}


@app.get("/")
async def index():
    return FileResponse(INDEX_PATH, media_type="text/html")


@app.options("/")
async def mcp_options():
    return Response(headers={
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "content-type, authorization, mcp-session-id",
        "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
        "Access-Control-Expose-Headers": "mcp-session-id",
    })


def mcp_headers(request: Request) -> dict:
    session_id = request.headers.get("mcp-session-id") or uuid.uuid4().hex
    return {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Expose-Headers": "mcp-session-id",
        "Mcp-Session-Id": session_id,
        "mcp-session-id": session_id,
    }


@app.post("/")
async def mcp_rpc(request: Request):
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="invalid JSON-RPC request")
    if isinstance(body, list):
        results = [r for r in (handle_mcp_request(item) for item in body if isinstance(item, dict)) if r is not None]
        return JSONResponse(results, headers=mcp_headers(request))
    if not isinstance(body, dict):
        raise HTTPException(status_code=400, detail="invalid JSON-RPC request")
    result = handle_mcp_request(body)
    if result is None:
        return Response(status_code=202, headers=mcp_headers(request))
    return JSONResponse(result, headers=mcp_headers(request))


@app.get("/api/books")
async def api_books():
    return {"books": [public_book(b) for b in read_books()]}


@app.post("/api/books/upload")
async def upload_book(file: UploadFile = File(...)):
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="empty file")
    path, title = save_book_file(file, data)
    text = data.decode("utf-8", errors="ignore")
    book = normalize_book({
        "id": hashlib.sha1((path + now_iso()).encode()).hexdigest()[:16],
        "title": title,
        "author": "未知作者",
        "path": path,
        "length": len(text),
        "offset": 0,
        "comments": [],
        "createdAt": now_iso(),
        "updatedAt": now_iso(),
    })
    books = read_books()
    books.insert(0, book)
    write_books(books)
    return {"book": public_book(book)}


@app.get("/api/books/{book_id}")
async def get_book(book_id: str):
    _, book = find_book(book_id)
    item = public_book(book)
    item["comments"] = book.get("comments") or []
    item["summaries"] = book.get("summaries") or []
    item["chapters"] = book_chapters(book_text(book))
    return {"book": item}


@app.get("/api/books/{book_id}/chapters")
async def get_book_chapters(book_id: str):
    _, book = find_book(book_id)
    return {"book": public_book(book), "chapters": book_chapters(book_text(book))}


@app.delete("/api/books/{book_id}")
async def delete_book(book_id: str):
    books, book = find_book(book_id)
    books = [b for b in books if b["id"] != book_id]
    write_books(books)
    for key in ("path", "cover"):
        value = book.get(key)
        if value:
            target = BASE / value.lstrip("/") if not value.startswith("/") else BASE / value.lstrip("/")
            try:
                if target.exists() and target.is_file() and BASE in target.resolve().parents:
                    target.unlink()
            except Exception:
                pass
    return {"ok": True}


@app.post("/api/books/{book_id}/cover")
async def upload_cover(book_id: str, file: UploadFile = File(...)):
    books, book = find_book(book_id)
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="empty cover")
    ext = mimetypes.guess_extension(file.content_type or "") or Path(file.filename or "").suffix or ".jpg"
    if ext.lower() not in {".jpg", ".jpeg", ".png", ".webp", ".gif"}:
        ext = ".jpg"
    stored = f"{book_id}-{hashlib.sha1(data).hexdigest()[:10]}{ext}"
    path = COVER_DIR / stored
    path.write_bytes(data)
    book["cover"] = f"covers/{stored}"
    book["updatedAt"] = now_iso()
    write_books(books)
    return {"book": public_book(book)}


@app.get("/api/books/{book_id}/next")
async def read_next(book_id: str, offset: int = 0, chars: int = 2600):
    books, book = find_book(book_id)
    text = book_text(book)
    chapters = book_chapters(text)
    offset, page = surrounding_page(text, offset, chars)
    end = min(len(text), offset + len(page))
    percent = (offset / len(text) * 100) if text else 0
    chapter_index = chapter_index_for_offset(chapters, offset)
    chapter_title = str((chapters[chapter_index] or {}).get("title") or "") if chapters else ""
    comments = []
    for item in book.get("comments") or []:
        try:
            pos = int(item.get("offset") or 0)
        except Exception:
            continue
        if offset <= pos < end:
            comments.append(item)
    comments.sort(key=lambda item: int(item.get("offset") or 0))
    next_offset = min(len(text), offset + len(page))
    return {"offset": offset, "nextOffset": next_offset, "length": len(text), "percent": percent, "text": page, "comments": comments, "chapterIndex": chapter_index, "chapterTitle": chapter_title}


@app.post("/api/books/{book_id}/mcp/read")
async def mcp_read(book_id: str, request: Request):
    body = await request.json()
    data = mcp_read_payload(book_id, body)
    data["instruction"] = "Read readingContext and then text exactly once. Do not fetch reading_summaries during normal reading. Call /api/books/{book_id}/mcp/reflect with offset, nextOffset, comment, segmentSummary, and (when needed) longTermOverview. segmentSummary must only summarize this new segment."
    return data


@app.post("/api/books/{book_id}/mcp/reflect")
async def mcp_reflect(book_id: str, request: Request):
    body = await request.json()
    return mcp_reflect_payload(book_id, body)


@app.post("/api/books/{book_id}/progress")
async def save_progress(book_id: str, request: Request):
    body = await request.json()
    books, book = find_book(book_id)
    text = book_text(book)
    book["offset"] = max(0, min(len(text), int(body.get("offset") or 0)))
    book["note"] = str(body.get("note") or "")
    book["updatedAt"] = now_iso()
    write_books(books)
    return {"book": public_book(book)}


@app.get("/api/books/{book_id}/summaries")
async def get_book_summaries(book_id: str):
    _, book = find_book(book_id)
    rows = unique_summaries(book)
    return {"book": public_book(book), "summaries": rows}


@app.get("/api/books/{book_id}/comments")
async def get_comments(book_id: str):
    _, book = find_book(book_id)
    return {"comments": book.get("comments") or []}


@app.post("/api/books/{book_id}/comments")
async def add_comment(book_id: str, request: Request):
    body = await request.json()
    books, book = find_book(book_id)
    text = book_text(book)
    offset = max(0, min(len(text), int(body.get("offset") or book.get("offset") or 0)))
    page = text[offset:offset + 500].strip()
    comment = str(body.get("comment") or "").strip() or auto_comment(text, offset)
    item = {
        "id": hashlib.sha1((book_id + str(offset) + comment + now_iso()).encode()).hexdigest()[:14],
        "offset": offset,
        "percent": round((offset / len(text) * 100), 2) if text else 0,
        "quote": re.sub(r"\s+", " ", page)[:180],
        "comment": comment,
        "createdAt": now_iso(),
    }
    comments = book.get("comments") if isinstance(book.get("comments"), list) else []
    comments.append(item)
    book["comments"] = comments[-300:]
    book["offset"] = offset
    book["note"] = comment
    book["updatedAt"] = now_iso()
    write_books(books)
    return {"comment": item, "book": public_book(book)}


@app.get("/api/history")
async def history():
    rows = []
    for book in read_books():
        for comment in book.get("comments") or []:
            rows.append({"bookId": book["id"], "bookTitle": book["title"], **comment})
    rows.sort(key=lambda x: x.get("createdAt", ""), reverse=True)
    return {"history": rows[:200]}


@app.get("/api/summaries")
async def all_summaries():
    rows = []
    for book in read_books():
        for item in unique_summaries(book):
            rows.append({"bookId": book["id"], "bookTitle": book["title"], **item})
    rows.sort(key=lambda x: (x.get("bookTitle", ""), int(x.get("offset") or 0)))
    return {"summaries": rows}


@app.get("/{path:path}")
async def fallback(path: str):
    if path.startswith(("static/", "covers/")):
        raise HTTPException(status_code=404)
    return FileResponse(INDEX_PATH, media_type="text/html")




if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=19200)



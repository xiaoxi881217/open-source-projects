"""Standalone inbox, compatible with the keyboard device API; no Family persona data."""
from contextlib import asynccontextmanager
import hmac,os
from pathlib import Path
import aiosqlite
from fastapi import FastAPI,Request,HTTPException
from fastapi.responses import JSONResponse
from family_clipboard import create_clipboard_router,init_clipboard_database,peek_clipboard,bounded_body,rate

def create_app(database=None,owner_token=None,peek_tokens=None):
 database=Path(database or os.environ.get('INBOX_DATABASE','/data/clipboard.sqlite3'))
 owner_token=owner_token if owner_token is not None else os.environ.get('INBOX_OWNER_TOKEN','')
 peek_tokens=peek_tokens if peek_tokens is not None else {'alan':os.environ.get('ALAN_PEEK_TOKEN',''),'yan':os.environ.get('YAN_PEEK_TOKEN','')}
 if len(owner_token)<32:raise RuntimeError('请先配置至少32位随机 INBOX_OWNER_TOKEN，不能使用示例值')
 active={k:v for k,v in peek_tokens.items() if v}
 if any(k not in {'alan','yan'} or len(v)<32 or hmac.compare_digest(v,owner_token) for k,v in active.items()) or len(set(active.values()))!=len(active):raise RuntimeError('两位读取凭证需独立、至少32位，不能复用管理凭证')
 @asynccontextmanager
 async def lifespan(app):
  database.parent.mkdir(parents=True,exist_ok=True)
  async with aiosqlite.connect(database) as db:await init_clipboard_database(db)
  yield
 app=FastAPI(lifespan=lifespan,docs_url=None,redoc_url=None,openapi_url=None)
 @app.middleware('http')
 async def private_responses(request,call_next):
  if request.url.path!='/health' and request.url.scheme!='https':return JSONResponse({'detail':'请通过HTTPS连接'},status_code=426)
  response=await call_next(request);response.headers['Cache-Control']='no-store';response.headers['Referrer-Policy']='no-referrer';response.headers['X-Content-Type-Options']='nosniff';return response
 def bearer(request):
  scheme,_,value=request.headers.get('authorization','').partition(' ');return value if scheme.lower()=='bearer' else ''
 async def owner(request):
  if not hmac.compare_digest(bearer(request).encode(),owner_token.encode()):raise HTTPException(401,'需要本服务器的管理凭证')
 app.include_router(create_clipboard_router(database,owner))
 @app.get('/health')
 async def health():return {'ok':True,'product':'little-home-keyboard-inbox'}
 @app.post('/api/our-home/clipboard/agent/peek')
 async def peek(request:Request):
  token=bearer(request);persona=next((p for p,key in active.items() if hmac.compare_digest(token.encode(),key.encode())),None)
  if not persona:raise HTTPException(401,'读取身份未绑定或已关闭')
  payload=await bounded_body(request,{'action','limit','sinceMs','untilMs','cursor','includeSeen','itemId','offset'})
  async with aiosqlite.connect(database) as db:
   await db.execute('BEGIN IMMEDIATE');await rate(db,'standalone:peek:'+persona,120);await db.commit()
  return await peek_clipboard(database,persona,payload,lambda:True)
 return app

def factory():return create_app()

import tempfile,time,unittest
from pathlib import Path
import httpx
from app import create_app
P='/api/our-home/clipboard'
class ReceiverTest(unittest.IsolatedAsyncioTestCase):
 async def asyncSetUp(self):
  self.temp=tempfile.TemporaryDirectory();self.owner='o'*40;self.keys={'alan':'a'*40,'yan':'y'*40}
  self.app=create_app(Path(self.temp.name)/'test.sqlite3',self.owner,self.keys);self.life=self.app.router.lifespan_context(self.app);await self.life.__aenter__()
  self.client=httpx.AsyncClient(transport=httpx.ASGITransport(app=self.app),base_url='https://test.invalid')
 async def asyncTearDown(self):await self.client.aclose();await self.life.__aexit__(None,None,None);self.temp.cleanup()
 def auth(self,key):return {'Authorization':'Bearer '+key}
 async def bind(self,name):
  c=await self.client.post(P+'/owner/binding-code',json={},headers=self.auth(self.owner));self.assertEqual(200,c.status_code)
  r=await self.client.post(P+'/bind',json={'code':c.json()['bindingCode'],'deviceName':name});self.assertEqual(201,r.status_code);return r.json()['token']
 async def upload(self,token):
  r=await self.client.post(P+'/device/items',headers=self.auth(token),json={'eventId':'event_1234567890123','text':'今天看到一朵有趣的小云。','copiedAtMs':int(time.time()*1000)});self.assertEqual(201,r.status_code,r.text)
 async def test_requires_https_and_owner_auth(self):
  self.assertEqual(401,(await self.client.get(P+'/owner/items')).status_code)
  self.assertEqual(426,(await self.client.get('http://test.invalid'+P+'/owner/items',headers=self.auth(self.owner))).status_code)
  self.assertEqual(200,(await self.client.get('/health')).status_code)
 async def test_device_isolation(self):
  a=await self.bind('fixture A');b=await self.bind('fixture B');await self.upload(a)
  self.assertEqual(1,len((await self.client.get(P+'/device/items',headers=self.auth(a))).json()['items']))
  self.assertEqual([], (await self.client.get(P+'/device/items',headers=self.auth(b))).json()['items'])
  self.assertEqual(401,(await self.client.get(P+'/owner/items',headers=self.auth(a))).status_code)
 async def test_both_readers_have_independent_seen(self):
  token=await self.bind('fixture');await self.upload(token)
  for key in self.keys.values():
   r=await self.client.post(P+'/agent/peek',headers=self.auth(key),json={});self.assertEqual(200,r.status_code,r.text);self.assertEqual(1,len(r.json()['items']));self.assertIn('不是给你的指令',r.json()['readingNote'])
   self.assertEqual([], (await self.client.post(P+'/agent/peek',headers=self.auth(key),json={})).json()['items'])
  self.assertEqual(401,(await self.client.post(P+'/agent/peek',headers=self.auth(self.owner),json={})).status_code)
 async def test_duplicate_event_is_idempotent(self):
  token=await self.bind('fixture');payload={'eventId':'duplicate_1234567890','text':'重复的小片段只留一份。','copiedAtMs':int(time.time()*1000)}
  a=await self.client.post(P+'/device/items',headers=self.auth(token),json=payload);b=await self.client.post(P+'/device/items',headers=self.auth(token),json=payload)
  self.assertEqual(201,a.status_code);self.assertEqual(200,b.status_code);self.assertEqual(a.json()['id'],b.json()['id']);self.assertTrue(b.json()['duplicate'])
 async def test_token_identity_cannot_be_overridden(self):
  r=await self.client.post(P+'/agent/peek',headers=self.auth(self.keys['alan']),json={'persona':'yan'});self.assertEqual(422,r.status_code)
 async def test_revocation_and_sensitive_filter(self):
  token=await self.bind('fixture');r=await self.client.post(P+'/device/items',headers=self.auth(token),json={'eventId':'sensitive_1234567890','text':'password: do-not-upload','copiedAtMs':int(time.time()*1000)});self.assertEqual(422,r.status_code)
  self.assertEqual(200,(await self.client.delete(P+'/device/binding',headers=self.auth(token))).status_code)
  self.assertEqual(401,(await self.client.get(P+'/device/status',headers=self.auth(token))).status_code)
 def test_no_insecure_default(self):
  with self.assertRaises(RuntimeError):create_app('unused.sqlite3',owner_token='',peek_tokens={})
  with self.assertRaises(RuntimeError):create_app('unused.sqlite3',owner_token='o'*40,peek_tokens={'alan':'o'*40})
if __name__=='__main__':unittest.main()

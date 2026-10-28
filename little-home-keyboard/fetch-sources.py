"""Fetch locked upstream sources; never replaces an existing directory."""
from pathlib import Path
import json,subprocess,re,sys
root=Path(__file__).resolve().parent
items=json.loads((root/'SOURCE_DEPENDENCIES.json').read_text())
for item in items:
 rel=Path(item['path'])
 if rel.is_absolute() or '..' in rel.parts or not str(rel).replace('\\','/').startswith('app/src/main/'):
  raise SystemExit('Unsafe source path')
 if not re.fullmatch('[0-9a-f]{40}',item['revision']) or not item['url'].startswith('https://github.com/'):
  raise SystemExit('Invalid locked source')
if '--check' in sys.argv:
 print('Source dependency locks validated:',len(items));raise SystemExit(0)
for item in items:
 target=root/item['path']
 if not target.exists():
  target.parent.mkdir(parents=True,exist_ok=True)
  subprocess.run(['git','clone','--no-checkout',item['url'],str(target)],check=True)
  subprocess.run(['git','-C',str(target),'checkout','--detach',item['revision']],check=True)
 actual=subprocess.check_output(['git','-C',str(target),'rev-parse','HEAD'],text=True).strip()
 if actual!=item['revision']:raise SystemExit('Existing source differs from lock: '+item['path'])
 subprocess.run(['git','-C',str(target),'submodule','update','--init','--recursive'],check=True)
print('Locked source repositories are ready.')

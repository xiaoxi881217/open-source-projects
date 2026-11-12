"""准备固定版本的Android流式ASR运行库；校验下载内容并可重复生成AAR。"""
from pathlib import Path
import argparse,hashlib,json,subprocess,urllib.request,zipfile

def prepare(archive: Path, target: Path, lock: dict):
    assert hashlib.sha256(archive.read_bytes()).hexdigest()==lock['upstreamSha256'], '上游运行库摘要不符'
    target.parent.mkdir(parents=True,exist_ok=True)
    temporary=target.with_suffix('.aar.part')
    try:
        with zipfile.ZipFile(archive) as a,zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
            for name in sorted(a.namelist()):
                if name.endswith('/') or name.endswith('/libonnxruntime.so'):continue
                item=zipfile.ZipInfo(name,date_time=(1980,1,1,0,0,0));item.compress_type=zipfile.ZIP_DEFLATED
                z.writestr(item,a.read(name))
        assert verified(temporary,lock), '运行库内容摘要不符'
        temporary.replace(target)
    finally:
        if temporary.exists():temporary.unlink()

def verified(path,lock):
    try:
        with zipfile.ZipFile(path) as z:
            return set(z.namelist())==set(lock['entries']) and all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in lock['entries'].items())
    except (OSError,zipfile.BadZipFile):return False

def main():
    root=Path(__file__).resolve().parents[2];lock=json.loads((Path(__file__).with_name('runtime-lock.json')).read_text())
    parser=argparse.ArgumentParser();parser.add_argument('--archive',type=Path);args=parser.parse_args()
    target=root/'app/libs/sherpa-onnx-2.0.1.aar'
    if target.exists() and verified(target,lock):
        print('ASR runtime verified');return
    archive=args.archive or root/'app/build/asr-runtime-upstream.aar'
    if not archive.exists():
        archive.parent.mkdir(parents=True,exist_ok=True)
        with urllib.request.urlopen(lock['url'],timeout=120) as response,archive.open('wb') as output:
            while chunk:=response.read(65536):output.write(chunk)
    prepare(archive,target,lock);print('ASR runtime prepared and verified')

if __name__=='__main__':main()

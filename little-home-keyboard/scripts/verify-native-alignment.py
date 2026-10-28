"""Check the development APK's native ELF alignment; this is not device acceptance."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--readelf", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    extracted = root / "build" / "apk-native-verification"
    extracted.mkdir(parents=True, exist_ok=True)
    libraries = []
    with zipfile.ZipFile(args.apk) as archive:
        entries = [entry for entry in archive.infolist()
                   if entry.filename.startswith("lib/arm64-v8a/") and entry.filename.endswith(".so")]
        assert entries, "No ARM64 native libraries in the APK"
        assert any(entry.filename.endswith("/librime_jni.so") for entry in entries), "Missing Rime JNI"
        for entry in entries:
            # Do not use extractall: archive paths never choose output directories.
            path = extracted / Path(entry.filename).name
            data = archive.read(entry)
            path.write_bytes(data)
            headers = subprocess.check_output(
                [str(args.readelf), "--program-headers", "--wide", str(path)],
                text=True, encoding="utf-8"
            )
            alignment = [int(line.split()[-1], 16) for line in headers.splitlines()
                         if line.strip().startswith("LOAD ")]
            libraries.append({"path": entry.filename, "bytes": len(data),
                              "sha256": hashlib.sha256(data).hexdigest(),
                              "compressed": entry.compress_type != zipfile.ZIP_STORED,
                              "loadAlignments": alignment,
                              "elf16kAligned": bool(alignment) and min(alignment) >= 16384})
    passed = all(item["elf16kAligned"] for item in libraries)
    result = {"apkSha256": hashlib.sha256(args.apk.read_bytes()).hexdigest(),
              "apkBytes": args.apk.stat().st_size, "abi": "arm64-v8a",
              "elf16kAligned": passed, "deviceAcceptance": "NOT_TESTED",
              "libraries": libraries}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"libraries": len(libraries), "elf16kAligned": passed,
                      "notAligned": [item["path"] for item in libraries if not item["elf16kAligned"]]}))
    if not passed:
        raise SystemExit(2)


if __name__ == "__main__":
    main()

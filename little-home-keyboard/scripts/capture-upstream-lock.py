"""Capture source and license-file identities, never private runtime data."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path


REPOSITORIES = {
    "YuyanIme": "gurecn/YuyanIme",
    "Xime": "ximeiorg/Xime",
    "trime": "osfans/trime",
    "fcitx5-android": "fcitx5-android/fcitx5-android",
    "HeliBoard": "HeliBorg/HeliBoard",
    "LianAi-Keyboard": "1213109176kgy-png/LianAi-Keyboard",
}


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(root), *args], text=True, encoding="utf-8"
    ).strip()


def identity(root: Path, repository: str) -> dict:
    commit, date, subject = git(root, "log", "-1", "--format=%H%n%aI%n%s").split("\n", 2)
    files = []
    for name in ("LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING", "TRADEMARKS.md", "THIRD_PARTY_NOTICES.md"):
        path = root / name
        if path.is_file():
            files.append({"path": name, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    return {"repository": repository, "commit": commit, "authorDate": date,
            "subject": subject, "licenseFiles": files}


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--research-root", type=Path, default=project.parent / "research" / "upstreams")
    parser.add_argument("--output", type=Path, default=project / "docs" / "upstream-lock.json")
    args = parser.parse_args()
    records = [identity(args.research_root / name, repo) for name, repo in REPOSITORIES.items()]
    records.append(identity(args.research_root / "YuyanIme" / "yuyansdk", "gurecn/yuyansdk"))
    lock = {"schemaVersion": 1, "capturedAt": datetime.now(timezone.utc).isoformat(),
            "purpose": "Research provenance; license completeness and runtime acceptance are NOT established",
            "research": records, "selectedNativeGitlinks": git(project, "submodule", "status", "--recursive").splitlines()}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Captured {len(records)} research repositories and {len(lock['selectedNativeGitlinks'])} gitlinks")


if __name__ == "__main__":
    main()

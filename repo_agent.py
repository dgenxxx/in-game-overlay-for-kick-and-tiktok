#!/usr/bin/env python3
"""
repo_agent.py
=============

A local "repo housekeeping" agent you run from inside any of your git
repositories — e.g. whatever project you currently have open in TRAE IDE.
It does not connect to TRAE IDE itself; you run it from that project's
terminal, and it acts on "whatever directory you're standing in."

What it does, in order:
  1. Scans the repo via `git` (so it automatically respects your existing
     .gitignore) for duplicate files and duplicate folders.
  2. Flags/cleans common junk (OS cruft, editor backups, caches, empty
     folders, etc.).
  3. Keeps .gitignore up to date for your project type (Node / Python /
     both), so junk stops reappearing.
  4. Optionally stages, commits, and pushes the result to GitHub.

SAFETY MODEL
------------
- Dry run by default. Nothing is deleted, changed, or committed unless you
  pass --apply.
- Every file/folder it's about to delete is copied first into
  .repo_agent_backup/<timestamp>/ (auto-added to .gitignore), so a bad call
  is always recoverable.
- It will never delete anything that looks like a secret/credential (.env,
  *.pem, *.key, id_rsa, etc.) — those are only ever flagged for
  .gitignore, never removed. Same for anything matching --protect.
- If node_modules/ (or a similar dependency folder) is tracked by git, it's
  called out as a warning, not mass-deleted automatically — that's a
  bigger decision you should make explicitly.
- push is a separate opt-in flag from commit, so you can review a local
  commit before it goes anywhere.
- It does NOT do AI-level semantic code editing (it won't decide "this
  function is dead code"). The optional --format pass only runs standard
  formatters/linters you already have installed in that project
  (black/isort/autoflake, or eslint/prettier).

USAGE
-----
    python3 repo_agent.py                        # report only, changes nothing
    python3 repo_agent.py --apply                # clean + commit locally
    python3 repo_agent.py --apply --push          # clean + commit + push
    python3 repo_agent.py --apply --push -y       # skip confirmation prompt
    python3 repo_agent.py --path ../other-repo    # target a different repo
    python3 repo_agent.py --format                # also run project formatters
    python3 repo_agent.py --protect "legacy/*"    # never touch paths matching this

Run with no arguments first, read the report, then decide on --apply.
"""

from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time
from collections import defaultdict
from pathlib import Path

# ---------------------------------------------------------------------------
# Defaults — override extras via --config some.json (see config.example.json)
# ---------------------------------------------------------------------------

DEFAULT_JUNK_PATTERNS = [
    ".DS_Store", "Thumbs.db", "desktop.ini",
    "*.pyc", "*.pyo",
    "*.log", "*.tmp", "*.temp", "*.bak", "*.swp", "*.swo",
    "*.orig", "*.rej",
    ".coverage",
]

# Any ancestor directory named one of these marks a file as junk too
# (e.g. src/mod/__pycache__/x.pyc), independent of the file's own name.
DIR_JUNK_NAMES = {"__pycache__", ".pytest_cache", ".mypy_cache"}

# Dependency-style directories: if these show up TRACKED by git, we warn
# loudly instead of silently deleting a potentially huge tree.
DEPENDENCY_DIR_NAMES = {"node_modules", "vendor", ".venv", "venv"}

# Never deleted, no matter what — only ever flagged for .gitignore.
NEVER_DELETE_PATTERNS = [
    ".env", ".env.*", "*.pem", "*.key", "id_rsa", "id_rsa.*",
    "*.pfx", "*.p12", "secrets*.json", "credentials*.json",
]

NODE_GITIGNORE_LINES = [
    "node_modules/", "dist/", "build/", ".next/", ".turbo/",
    "*.log", ".env", ".env.local",
]

PYTHON_GITIGNORE_LINES = [
    "__pycache__/", "*.pyc", ".venv/", "venv/", "env/",
    "*.egg-info/", ".pytest_cache/", ".mypy_cache/", ".env",
]

ALWAYS_GITIGNORE_LINES = [
    ".DS_Store", "Thumbs.db", ".repo_agent_backup/",
]


# ---------------------------------------------------------------------------
# Small helpers
# ---------------------------------------------------------------------------

def sha256_of_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def run_git(args, cwd, check=True, capture=True):
    try:
        result = subprocess.run(["git", *args], cwd=cwd, text=True, capture_output=capture)
    except FileNotFoundError:
        print("ERROR: git is not installed or not on PATH.", file=sys.stderr)
        sys.exit(1)
    if check and result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed:\n{result.stderr}")
    return result


def is_git_repo(path: Path) -> bool:
    r = run_git(["rev-parse", "--is-inside-work-tree"], cwd=path, check=False)
    return r.returncode == 0 and r.stdout.strip() == "true"


def git_file_lists(root: Path):
    """Return (tracked, untracked_not_ignored) relative-path lists."""
    tracked = [f for f in run_git(["ls-files", "-z"], cwd=root).stdout.split("\0") if f]
    untracked = [
        f for f in run_git(["ls-files", "--others", "--exclude-standard", "-z"], cwd=root).stdout.split("\0") if f
    ]
    return tracked, untracked


def matches_any(name: str, patterns) -> bool:
    return any(fnmatch.fnmatch(name, pat) for pat in patterns)


def human_size(num_bytes: float) -> str:
    for unit in ["B", "KB", "MB", "GB"]:
        if num_bytes < 1024:
            return f"{num_bytes:.0f}{unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f}TB"


# ---------------------------------------------------------------------------
# Detectors
# ---------------------------------------------------------------------------

def find_duplicate_files(root: Path, rel_files, hash_cache: dict):
    by_hash = defaultdict(list)
    for rel in rel_files:
        p = root / rel
        try:
            if p.is_file():
                h = hash_cache.setdefault(rel, sha256_of_file(p))
                by_hash[h].append(rel)
        except (OSError, PermissionError):
            continue
    groups = []
    for file_hash, paths in by_hash.items():
        if len(paths) > 1:
            ordered = sorted(paths, key=lambda p: (p.count("/"), len(p), p))
            groups.append({"keep": ordered[0], "remove": ordered[1:]})
    return groups


def find_duplicate_folders(root: Path, rel_files, hash_cache: dict):
    folder_contents = defaultdict(dict)  # folder -> {path-within-folder: hash}
    for rel in rel_files:
        p = root / rel
        if not p.is_file():
            continue
        h = hash_cache.get(rel)
        if h is None:
            try:
                h = sha256_of_file(p)
            except (OSError, PermissionError):
                continue
            hash_cache[rel] = h
        parts = Path(rel).parts
        for i in range(1, len(parts)):
            folder = "/".join(parts[:i])
            inner = "/".join(parts[i:])
            folder_contents[folder][inner] = h

    sig_to_folders = defaultdict(list)
    for folder, contents in folder_contents.items():
        if contents:
            sig_to_folders[tuple(sorted(contents.items()))].append(folder)

    groups = []
    for sig, folders in sig_to_folders.items():
        if len(folders) < 2:
            continue
        ordered = sorted(folders, key=lambda f: (f.count("/"), len(f), f))
        keep = ordered[0]
        remove = [f for f in ordered[1:] if not f.startswith(keep + "/") and not keep.startswith(f + "/")]
        if remove:
            groups.append({"keep": keep, "remove": remove, "file_count": len(sig)})
    return groups


def find_junk(rel_files, junk_patterns):
    junk = []
    for rel in rel_files:
        name = Path(rel).name
        ancestor_parts = Path(rel).parts[:-1]
        hit = matches_any(name, junk_patterns) or any(part in DIR_JUNK_NAMES for part in ancestor_parts)
        if hit:
            junk.append(rel)
    return junk


def find_secret_like(rel_files, never_delete_patterns):
    return [rel for rel in rel_files if matches_any(Path(rel).name, never_delete_patterns)]


def find_tracked_dependency_dirs(tracked_files):
    hits = set()
    for rel in tracked_files:
        for part in Path(rel).parts[:-1]:
            if part in DEPENDENCY_DIR_NAMES:
                hits.add(part)
    return sorted(hits)


def detect_project_types(root: Path):
    types = set()
    if (root / "package.json").exists():
        types.add("node")
    if any((root / f).exists() for f in ("requirements.txt", "pyproject.toml", "setup.py")) or any(root.glob("*.py")):
        types.add("python")
    return types


def plan_gitignore_update(root: Path, project_types):
    gi_path = root / ".gitignore"
    existing_lines = []
    if gi_path.exists():
        existing_lines = gi_path.read_text(encoding="utf-8", errors="ignore").splitlines()
    existing_set = {ln.strip() for ln in existing_lines if ln.strip() and not ln.strip().startswith("#")}

    wanted = list(ALWAYS_GITIGNORE_LINES)
    if "node" in project_types:
        wanted += NODE_GITIGNORE_LINES
    if "python" in project_types:
        wanted += PYTHON_GITIGNORE_LINES

    missing = [w for w in dict.fromkeys(wanted) if w not in existing_set]
    return gi_path, existing_lines, missing


# ---------------------------------------------------------------------------
# Apply-side actions
# ---------------------------------------------------------------------------

def backup_paths(root: Path, rel_files, rel_folders, backup_root: Path):
    for rel in rel_files:
        src = root / rel
        if src.is_file():
            dst = backup_root / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
    for rel in rel_folders:
        src = root / rel
        if src.is_dir():
            dst = backup_root / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(src, dst, dirs_exist_ok=True)


def prune_empty_dirs(root: Path):
    removed = []
    for dirpath, _, _ in os.walk(root, topdown=False):
        rel_dir = Path(dirpath).relative_to(root)
        if ".git" in rel_dir.parts:
            continue
        try:
            if Path(dirpath) != root and not os.listdir(dirpath):
                os.rmdir(dirpath)
                removed.append(str(rel_dir))
        except OSError:
            pass
    return removed


def run_formatters(root: Path, project_types):
    print("\n=== Formatting pass (--format) ===")

    if "python" in project_types:
        for tool, cmd in [
            ("black", ["black", str(root)]),
            ("isort", ["isort", str(root)]),
            ("autoflake", ["autoflake", "--remove-all-unused-imports", "--in-place", "--recursive", str(root)]),
        ]:
            if shutil.which(tool):
                print(f"  running {tool} ...")
                subprocess.run(cmd, cwd=root)
            else:
                print(f"  {tool} not found on PATH — skipped")

    if "node" in project_types:
        local_prettier = root / "node_modules" / ".bin" / "prettier"
        local_eslint = root / "node_modules" / ".bin" / "eslint"
        if local_prettier.exists():
            print("  running prettier --write ...")
            subprocess.run([str(local_prettier), "--write", "."], cwd=root)
        else:
            print("  local prettier not found in node_modules/.bin — skipped")
        if local_eslint.exists():
            print("  running eslint --fix ...")
            subprocess.run([str(local_eslint), "--fix", "."], cwd=root)
        else:
            print("  local eslint not found in node_modules/.bin — skipped")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Local repo housekeeping + sync agent.")
    parser.add_argument("--path", default=".", help="Path to the git repo (default: current directory)")
    parser.add_argument("--apply", action="store_true", help="Actually make changes (default is dry run)")
    parser.add_argument("--push", action="store_true", help="git push after committing (implies --apply)")
    parser.add_argument("-y", "--yes", action="store_true", help="Don't prompt for confirmation")
    parser.add_argument("--format", action="store_true", help="Also run black/isort/autoflake or eslint/prettier if installed")
    parser.add_argument("--config", default=None, help="Path to a JSON file with extra_junk_patterns / extra_never_delete_patterns")
    parser.add_argument("--protect", action="append", default=[], help="Glob pattern for paths to never remove (repeatable)")
    args = parser.parse_args()

    if args.push:
        args.apply = True

    root = Path(args.path).resolve()
    if not root.exists():
        print(f"ERROR: {root} does not exist.", file=sys.stderr)
        sys.exit(1)
    if not is_git_repo(root):
        print(f"ERROR: {root} is not inside a git repository.", file=sys.stderr)
        sys.exit(1)

    junk_patterns = list(DEFAULT_JUNK_PATTERNS)
    never_delete_patterns = list(NEVER_DELETE_PATTERNS)
    if args.config:
        cfg = json.loads(Path(args.config).read_text())
        junk_patterns += cfg.get("extra_junk_patterns", [])
        never_delete_patterns += cfg.get("extra_never_delete_patterns", [])

    print(f"Scanning {root} ...")
    tracked, untracked = git_file_lists(root)
    rel_files = sorted(set(tracked + untracked))
    tracked_set = set(tracked)
    print(f"  {len(rel_files)} tracked/untracked files (already-gitignored files are skipped automatically)")

    pre_existing_status = run_git(["status", "--porcelain"], cwd=root).stdout.strip()
    pre_existing_count = len(pre_existing_status.splitlines()) if pre_existing_status else 0

    hash_cache: dict = {}
    dup_files = find_duplicate_files(root, rel_files, hash_cache)
    dup_folders = find_duplicate_folders(root, rel_files, hash_cache)

    # Collapse nested duplicate-folder hits: if both a folder and a folder
    # inside it are flagged for removal (common — a dup folder's contents
    # also match at every nested level), only report/remove the outer one.
    all_remove_folders = {r for g in dup_folders for r in g["remove"]}
    for g in dup_folders:
        g["remove"] = [
            r for r in g["remove"] if not any(r != other and r.startswith(other + "/") for other in all_remove_folders)
        ]
    dup_folders = [g for g in dup_folders if g["remove"]]

    junk = find_junk(rel_files, junk_patterns)

    # Drop any "duplicate file" hit that lives inside a folder already
    # slated for removal above — it'll go with the folder, no need to
    # double-report or double-delete it.
    removal_folder_set = {r for g in dup_folders for r in g["remove"]}

    def under_removed_folder(relpath: str) -> bool:
        return any(relpath == f or relpath.startswith(f + "/") for f in removal_folder_set)

    for g in dup_files:
        g["remove"] = [r for r in g["remove"] if not under_removed_folder(r)]
    dup_files = [g for g in dup_files if g["remove"]]
    secrets_found = find_secret_like(rel_files, never_delete_patterns)
    dependency_dirs_tracked = find_tracked_dependency_dirs(tracked)
    project_types = detect_project_types(root)
    gi_path, gi_existing, gi_missing = plan_gitignore_update(root, project_types)

    # ---- centralize protection filtering (never-delete patterns + user --protect) ----
    protect_patterns = never_delete_patterns + args.protect
    protected_hits: list = []

    def sanitize(paths):
        kept = []
        for p in paths:
            if matches_any(Path(p).name, protect_patterns) or matches_any(p, protect_patterns):
                protected_hits.append(p)
            else:
                kept.append(p)
        return kept

    for g in dup_files:
        g["remove"] = sanitize(g["remove"])
    dup_files = [g for g in dup_files if g["remove"]]

    for g in dup_folders:
        g["remove"] = sanitize(g["remove"])
    dup_folders = [g for g in dup_folders if g["remove"]]

    junk = sanitize(junk)

    # ---- report ----
    print("\n=== Duplicate files ===")
    if not dup_files:
        print("  none found")
    for g in dup_files:
        reclaim = sum((root / p).stat().st_size for p in g["remove"] if (root / p).exists())
        print(f"  keep: {g['keep']}")
        for r in g["remove"]:
            print(f"    remove: {r}")
        print(f"    (~{human_size(reclaim)} reclaimed)")

    print("\n=== Duplicate folders (byte-identical contents) ===")
    if not dup_folders:
        print("  none found")
    for g in dup_folders:
        print(f"  keep: {g['keep']}/  ({g['file_count']} files)")
        for r in g["remove"]:
            print(f"    remove: {r}/")

    print("\n=== Junk files ===")
    if not junk:
        print("  none found")
    for j in junk:
        print(f"  {j}")

    if protected_hits:
        print("\n=== Protected (matched a pattern but will not be touched) ===")
        for p in sorted(set(protected_hits)):
            print(f"  {p}")

    if secrets_found:
        print("\n=== Secret-looking files (never auto-deleted — only ever gitignored) ===")
        for s in secrets_found:
            status = "TRACKED (already committed!)" if s in tracked_set else "untracked, not gitignored"
            print(f"  {s}  [{status}]")

    if dependency_dirs_tracked:
        print("\n=== Warning: dependency folder(s) tracked by git ===")
        for d in dependency_dirs_tracked:
            print(f"  {d}/ appears to be committed. This is almost always unintentional.")
        print("  Not auto-removed (could be a lot of files). Fix with e.g.:")
        print("    git rm -r --cached <folder> && add it to .gitignore")

    print(f"\n=== .gitignore ({'exists' if gi_path.exists() else 'will be created'}) ===")
    print(f"  detected project type(s): {', '.join(sorted(project_types)) or 'unknown'}")
    if gi_missing:
        for m in gi_missing:
            print(f"  + {m}")
    else:
        print("  already up to date")

    if pre_existing_count:
        print(f"\nNote: {pre_existing_count} pre-existing uncommitted change(s) are also in this repo.")
        print("  `git add -A` will stage those too if you --apply. Commit/stash first to keep them separate.")

    total_removals = sum(len(g["remove"]) for g in dup_files) + sum(len(g["remove"]) for g in dup_folders) + len(junk)

    if not args.apply:
        print(
            f"\nDRY RUN — no changes made. {total_removals} item(s) would be removed, "
            f"{len(gi_missing)} line(s) would be added to .gitignore."
        )
        print("Re-run with --apply to make these changes (a backup is made first),")
        print("or --apply --push to also commit and push.")
        return

    if total_removals == 0 and not gi_missing and not args.format:
        print("\nNothing to change.")
        return

    if not args.yes:
        resp = input(
            f"\nProceed with {total_removals} removal(s) and {len(gi_missing)} .gitignore addition(s)? [y/N] "
        )
        if resp.strip().lower() != "y":
            print("Aborted — no changes made.")
            return

    # ---- backup, then apply ----
    timestamp = time.strftime("%Y%m%d-%H%M%S")
    backup_root = root / ".repo_agent_backup" / timestamp

    remove_files = [r for g in dup_files for r in g["remove"]] + junk
    remove_folders = [r for g in dup_folders for r in g["remove"]]

    if remove_files or remove_folders:
        backup_paths(root, remove_files, remove_folders, backup_root)
        print(f"\nBacked up {len(remove_files)} file(s) and {len(remove_folders)} folder(s) to {backup_root}")

    for rel in remove_files:
        p = root / rel
        if p.exists():
            p.unlink()
    for rel in remove_folders:
        p = root / rel
        if p.exists():
            shutil.rmtree(p)

    pruned = prune_empty_dirs(root)
    if pruned:
        print(f"Removed {len(pruned)} now-empty local folder(s) (not a git-tracked change).")

    if gi_missing:
        lines = list(gi_existing)
        if lines and lines[-1].strip() != "":
            lines.append("")
        lines.append("# Added by repo_agent.py")
        lines += gi_missing
        gi_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"Updated {gi_path}")

    if args.format:
        run_formatters(root, project_types)

    # ---- git add / commit / push ----
    run_git(["add", "-A"], cwd=root)
    staged = run_git(["diff", "--cached", "--name-only"], cwd=root).stdout.strip()
    if not staged:
        print("\nNo staged changes after cleanup — skipping commit.")
        return

    summary_bits = []
    if dup_files:
        summary_bits.append(f"{sum(len(g['remove']) for g in dup_files)} duplicate file(s)")
    if dup_folders:
        summary_bits.append(f"{sum(len(g['remove']) for g in dup_folders)} duplicate folder(s)")
    if junk:
        summary_bits.append(f"{len(junk)} junk file(s)")
    if gi_missing:
        summary_bits.append(f".gitignore +{len(gi_missing)}")
    message = "chore: repo housekeeping — " + ", ".join(summary_bits) if summary_bits else "chore: repo housekeeping"

    run_git(["commit", "-m", message], cwd=root)
    print(f"\nCommitted: {message}")

    if args.push:
        push_result = run_git(["push"], cwd=root, check=False)
        if push_result.returncode != 0:
            print("\nCommit succeeded locally, but push failed:")
            print(push_result.stderr)
            print("You may need to run `git push -u origin <branch>` yourself the first time.")
        else:
            print("Pushed to remote.")
    else:
        print("Committed locally. Re-run with --push (or `git push` yourself) to publish it.")


if __name__ == "__main__":
    main()

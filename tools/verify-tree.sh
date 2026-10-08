#!/usr/bin/env bash
#
# Looks for references to things this tree no longer has.
#
# The measurement in tools/closure.sh answers what the build needs and what gn
# reads and what DEPS names. It cannot answer whether a file that none of those
# three parse points at something that is gone, because nothing reads it. Those
# references do not fail anything when they break; they fail later, in a step
# whose name says nothing about them, which is how a deletion of this size turns
# into a mystery.
#
# So every remaining file that looks like configuration is searched for paths, and
# each path is looked for on disk. What comes back is a list to read, not a
# failure: a reference to a path that gclient fetches resolves once the tree is
# synced, and a reference written as //dir:target is not a path at all.
#
# Usage:
#   tools/verify-tree.sh
#
# Environment:
#   CHARM_VERIFY_LIMIT   references to print per kind, default 25

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIMIT="${CHARM_VERIFY_LIMIT:-25}"

[ -f .gn ] && [ -f BUILD.gn ] || {
  echo "$REPO_DIR does not look like a Chromium checkout." >&2
  exit 1
}

PYTHON="$REPO_DIR/third_party/cpython3/host/bin/python3"
[ -x "$PYTHON" ] || PYTHON="$(command -v python3 || true)"
[ -n "$PYTHON" ] || { echo "python3 not found." >&2; exit 1; }

cd "$REPO_DIR"

# Only files that name paths. A .png does not, and reading 60000 of them to learn
# nothing is the kind of thoroughness that makes a check get skipped.
EXTENSIONS=(".gn" ".gni" ".gyp" ".gypi" ".py" ".sh" ".json" ".yaml" ".yml" ".txt" ".md" ".cfg" ".ini")

printf '=== references from configuration files to paths that are not there ===\n'
"$PYTHON" - "$REPO_DIR" "$LIMIT" "${EXTENSIONS[@]}" <<'PYTHON'
import os
import re
import subprocess
import sys
from collections import Counter

repo_dir, limit = os.path.abspath(sys.argv[1]), int(sys.argv[2])
extensions = tuple(sys.argv[3:])
skip = ("node_modules", "/out/", "/.git/")

files = subprocess.run(
    ["git", "-c", "core.quotePath=false", "ls-files", "-z"],
    cwd=repo_dir, capture_output=True, check=True,
).stdout.split(b"\0")

# //dir, //dir:name and //dir/file. A label's :target is stripped because it is
# not part of the path.
pattern = re.compile(r"//([A-Za-z0-9_.+\-/]+)")

missing = Counter()
scanned = 0
for raw in files:
    if not raw:
        continue
    path = raw.decode("utf-8", "surrogateescape")
    if not path.endswith(extensions) or any(part in path for part in skip):
        continue
    full = os.path.join(repo_dir, path)
    try:
        if os.path.getsize(full) > 512 * 1024:
            continue
        with open(full, encoding="utf-8", errors="replace") as handle:
            text = handle.read()
    except OSError:
        continue
    scanned += 1
    for match in pattern.findall(text):
        target = match.split(":", 1)[0].rstrip("/")
        if not target:
            continue
        candidate = os.path.join(repo_dir, target)
        # A directory is there if anything is in it, which is the case that
        # matters: gn only needs the BUILD file and the sources beneath it.
        if os.path.exists(candidate):
            continue
        if os.path.isdir(os.path.dirname(candidate)) and os.listdir(os.path.dirname(candidate)):
            continue
        missing["%s -> //%s" % (path, target)] += 1

print("scanned %d configuration files" % scanned)
print("%d references to paths that are not present" % sum(missing.values()))
for line, count in missing.most_common(limit):
    print("  %4d  %s" % (count, line))
if len(missing) > limit:
    print("  ... and %d more" % (len(missing) - limit))
PYTHON

printf '\nWrote nothing: this reports, it does not judge. A reference to a path gclient fetches is not a defect.\n'
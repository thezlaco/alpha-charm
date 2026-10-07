#!/usr/bin/env bash
#
# Compiles Charm. Safe to run any number of times.
#
# Reads exactly two things: the source tree produced by tools/materialize.sh and
# config/args.gn. It does not know that patches exist, which is what makes a
# rebuild independent of how the tree was originally created.
#
# Usage:
#   tools/build.sh
#
# Environment:
#   CHARM_WORK_DIR   where the source tree lives        (default /work)
#   CHARM_OUT_DIR    build output, relative to the tree (default out/Charm)

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

WORK_DIR="${CHARM_WORK_DIR:-/work}"
OUT_DIR="${CHARM_OUT_DIR:-out/Charm}"

TREE_DIR="$WORK_DIR/charm-src"
DEPOT_TOOLS="$WORK_DIR/depot_tools"

say() { printf '\n=== %s ===\n' "$1"; }

if [ ! -d "$TREE_DIR/.git" ]; then
  echo "No source tree at $TREE_DIR. Run tools/materialize.sh first." >&2
  exit 1
fi

if [ ! -d "$DEPOT_TOOLS" ]; then
  echo "depot_tools missing at $DEPOT_TOOLS. Run tools/materialize.sh first." >&2
  exit 1
fi

export PATH="$DEPOT_TOOLS:$PATH"

cd "$TREE_DIR"

# Regenerated every run rather than stamped, because it depends on a file that
# changes whenever the configuration is edited. gn is incremental and costs
# seconds, so a stale graph is not worth the special case.
say "gn gen ${OUT_DIR}"
mkdir -p "$OUT_DIR"
cp "$REPO_DIR/config/args.gn" "$OUT_DIR/args.gn"
gn gen "$OUT_DIR"

say "chrome_public_apk"
autoninja -C "$OUT_DIR" chrome_public_apk

APK="$TREE_DIR/$OUT_DIR/apks/ChromePublic.apk"
echo
echo "APK: $APK"
ls -la "$APK"